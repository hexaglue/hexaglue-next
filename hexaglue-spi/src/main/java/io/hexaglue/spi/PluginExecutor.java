/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.spi;

import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.config.GenerationConfig;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Runs the plugins over one model and comes back with everything they produced.
 *
 * <p>A plugin is untrusted code inside someone's build: it can throw, it can have been compiled
 * against another version of the model, it can be handed an option its author never wrote. None of
 * that may cost the build the contributions of the other plugins, so each one runs inside its own
 * isolation and every refusal comes back coded, naming the plugin.</p>
 *
 * @since 7.0.0
 */
public final class PluginExecutor {

    /** A declared dependency is on no classpath. */
    private static final IssueCode MISSING_DEPENDENCY = IssueCode.of("HG-PLUGIN-001");

    /** The plugins wait on each other in a circle. */
    private static final IssueCode DEPENDENCY_CYCLE = IssueCode.of("HG-PLUGIN-002");

    /** The plugin threw, or failed to link. */
    private static final IssueCode PLUGIN_FAILED = IssueCode.of("HG-PLUGIN-003");

    /** An option was stated that the plugin never declared. */
    private static final IssueCode OPTION_UNKNOWN = IssueCode.of("HG-PLUGIN-004");

    /** Two plugins want to write the same document. */
    private static final IssueCode DOCUMENT_CLAIMED_TWICE = IssueCode.of("HG-PLUGIN-006");

    /** Two plugins claim the same identifier. */
    private static final IssueCode DUPLICATE_ID = IssueCode.of("HG-PLUGIN-007");

    /** Two plugins want to generate the same type. */
    private static final IssueCode SOURCE_CLAIMED_TWICE = IssueCode.of("HG-PLUGIN-008");

    private PluginExecutor() {}

    /**
     * Runs every plugin that can run, in dependency order.
     *
     * @param plugins the discovered plugins, in discovery order
     * @param model the classified model they all read
     * @param options the stated options, by plugin identifier
     * @return what the run produced and what it refused
     */
    public static PluginRun run(
            List<HexaGluePlugin> plugins, ArchModel model, Map<String, Map<String, String>> options) {
        return run(
                plugins,
                model,
                List.of(),
                Measurements.none(),
                GenerationConfig.defaults().minConfidence(),
                options);
    }

    /**
     * Runs every plugin that can run, in dependency order.
     *
     * @param plugins the discovered plugins, in discovery order
     * @param model the classified model they all read
     * @param findings what the checks made of that model
     * @param measurements what was measured about the shape of the codebase
     * @param minConfidence the weakest verdict this run accepts as grounds for generating
     * @param options the stated options, by plugin identifier
     * @return what the run produced and what it refused
     */
    public static PluginRun run(
            List<HexaGluePlugin> plugins,
            ArchModel model,
            List<Finding> findings,
            Measurements measurements,
            Confidence minConfidence,
            Map<String, Map<String, String>> options) {
        Objects.requireNonNull(plugins, "plugins must not be null");
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        Objects.requireNonNull(measurements, "measurements must not be null");
        Objects.requireNonNull(minConfidence, "minConfidence must not be null");
        Objects.requireNonNull(options, "options must not be null");

        Outputs outputs = new Outputs();
        Map<String, HexaGluePlugin> byId = new LinkedHashMap<>();
        Schedule schedule = Schedule.of(described(plugins, byId, outputs.diagnostics));

        Set<String> skipped = new TreeSet<>();
        schedule.excluded().forEach(exclusion -> skipped.add(exclusion.pluginId()));
        schedule.excluded().forEach(exclusion -> refusalOf(exclusion, schedule).ifPresent(outputs.diagnostics::add));
        schedule.duplicates()
                .forEach(pluginId -> outputs.diagnostics.add(diagnostic(
                        DUPLICATE_ID,
                        "two plugins claim the identifier " + pluginId + "; the first one read was kept")));

        List<String> executed = new ArrayList<>();
        for (String pluginId : schedule.order()) {
            if (skipped.contains(pluginId)) {
                continue;
            }
            // The schedule was built from these very manifests, so the plugin is always there.
            HexaGluePlugin plugin = Objects.requireNonNull(byId.get(pluginId), pluginId);
            Optional<Diagnostic> refusal =
                    contribute(plugin, model, findings, measurements, minConfidence, options, outputs);
            if (refusal.isEmpty()) {
                executed.add(pluginId);
            } else {
                List<String> dependents = schedule.dependentsOf(pluginId);
                outputs.diagnostics.add(alsoSkipping(refusal.get(), dependents));
                skipped.add(pluginId);
                skipped.addAll(dependents);
            }
        }
        return new PluginRun(outputs.documents, outputs.sources, outputs.diagnostics, executed, List.copyOf(skipped));
    }

    /**
     * What a run accumulates across its plugins, and who claimed what first.
     *
     * <p>Two backends writing the same thing is arbitrated the same way whatever the thing is: the
     * first writer keeps it, both claimants are named, and the run carries on. What differs is only
     * what the collision is called.</p>
     */
    private static final class Outputs {

        private final List<Document> documents = new ArrayList<>();
        private final List<SourceFile> sources = new ArrayList<>();
        private final List<Diagnostic> diagnostics = new ArrayList<>();
        private final Map<String, String> documentClaims = new LinkedHashMap<>();
        private final Map<String, String> sourceClaims = new LinkedHashMap<>();

        void takeDocuments(String pluginId, List<Document> emitted) {
            for (Document document : emitted) {
                take(pluginId, document.path(), documentClaims, () -> documents.add(document), DOCUMENT_CLAIMED_TWICE);
            }
        }

        void takeSources(String pluginId, List<SourceFile> emitted) {
            for (SourceFile source : emitted) {
                take(pluginId, source.qualifiedName(), sourceClaims, () -> sources.add(source), SOURCE_CLAIMED_TWICE);
            }
        }

        private void take(
                String pluginId, String claimed, Map<String, String> claims, Runnable keep, IssueCode collision) {
            String first = claims.putIfAbsent(claimed, pluginId);
            if (first == null) {
                keep.run();
            } else {
                diagnostics.add(diagnostic(
                        collision,
                        "plugins " + first + " and " + pluginId + " both write " + claimed + "; the one of " + first
                                + " was kept"));
            }
        }
    }

    /**
     * Asks every plugin what it declares, indexing the first plugin to claim each identifier.
     * Describing itself is the first thing a plugin does, and the first thing it can fail at.
     */
    // Isolating a plugin is exactly catching everything it can raise: a backend compiled against
    // another version of the model raises errors no narrower type covers.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static List<PluginManifest> described(
            List<HexaGluePlugin> plugins, Map<String, HexaGluePlugin> byId, List<Diagnostic> diagnostics) {
        List<PluginManifest> manifests = new ArrayList<>();
        for (HexaGluePlugin plugin : plugins) {
            PluginManifest manifest;
            try {
                manifest = plugin.manifest();
            } catch (Exception | LinkageError failure) {
                diagnostics.add(
                        diagnostic(PLUGIN_FAILED, "a plugin failed to describe itself and was skipped: " + failure));
                continue;
            }
            manifests.add(manifest);
            byId.putIfAbsent(manifest.id(), plugin);
        }
        return manifests;
    }

    /**
     * Runs one plugin inside its own isolation. Returns the diagnostic that cost it its
     * contribution, or null when it contributed.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static Optional<Diagnostic> contribute(
            HexaGluePlugin plugin,
            ArchModel model,
            List<Finding> findings,
            Measurements measurements,
            Confidence minConfidence,
            Map<String, Map<String, String>> options,
            Outputs outputs) {
        PluginManifest manifest = plugin.manifest();
        Map<String, String> stated = options.getOrDefault(manifest.id(), Map.of());
        List<String> undeclared = stated.keySet().stream()
                .filter(key -> !manifest.options().contains(key))
                .sorted()
                .toList();
        if (!undeclared.isEmpty()) {
            return Optional.of(diagnostic(
                    OPTION_UNKNOWN,
                    "plugin " + manifest.id() + " was given options it does not declare: "
                            + String.join(", ", undeclared) + ". It answers to: "
                            + String.join(", ", manifest.options())));
        }

        // Held aside until the plugin comes back: a backend that fails halfway through leaves
        // nothing behind, so a failed contribution is a contribution that did not happen.
        List<Document> documents = new ArrayList<>();
        List<SourceFile> sources = new ArrayList<>();
        List<Diagnostic> reported = new ArrayList<>();
        try {
            plugin.contribute(new Contribution(
                    model,
                    findings,
                    measurements,
                    PluginConfig.of(manifest.id(), stated),
                    minConfidence,
                    new Sinks(documents::add, sources::add, reported::add)));
        } catch (PluginConfigException malformed) {
            return Optional.of(malformed.diagnostic());
        }
        // A plugin is third-party code, possibly compiled against another version of the model:
        // whatever it throws and whatever it fails to link against costs its own contribution,
        // never the contributions of the others.
        catch (Exception | LinkageError failure) {
            return Optional.of(
                    diagnostic(PLUGIN_FAILED, "plugin " + manifest.id() + " failed and was skipped: " + failure));
        }
        outputs.takeDocuments(manifest.id(), documents);
        outputs.takeSources(manifest.id(), sources);
        outputs.diagnostics.addAll(reported);
        return Optional.empty();
    }

    /**
     * Says, on the diagnostic that carries the cause, which plugins went down with it — so the
     * account of a run reads as one refusal with its consequences rather than as a list of
     * unrelated failures.
     */
    private static Diagnostic alsoSkipping(Diagnostic cause, List<String> dependents) {
        if (dependents.isEmpty()) {
            return cause;
        }
        return Diagnostic.builder(
                        cause.code(),
                        cause.severity(),
                        cause.message() + ". Skipped with it: " + String.join(", ", dependents))
                .build();
    }

    /**
     * A plugin excluded because something it depends on is excluded gets no diagnostic of its own:
     * the diagnostic of the cause names it. One refusal, one code, its consequences stated with it.
     */
    private static Optional<Diagnostic> refusalOf(Schedule.Exclusion exclusion, Schedule schedule) {
        String involved = String.join(", ", exclusion.involved());
        List<String> dependents = schedule.dependentsOf(exclusion.pluginId());
        return switch (exclusion.reason()) {
            case MISSING_DEPENDENCY ->
                Optional.of(alsoSkipping(
                        diagnostic(
                                MISSING_DEPENDENCY,
                                "plugin " + exclusion.pluginId() + " depends on " + involved
                                        + ", which no plugin provides"),
                        dependents));
            case CYCLE ->
                Optional.of(diagnostic(
                        DEPENDENCY_CYCLE,
                        "plugin " + exclusion.pluginId() + " takes part in a dependency cycle with " + involved));
            case BLOCKED -> Optional.empty();
        };
    }

    private static Diagnostic diagnostic(IssueCode code, String message) {
        return Diagnostic.builder(code, DiagnosticSeverity.WARNING, message).build();
    }
}
