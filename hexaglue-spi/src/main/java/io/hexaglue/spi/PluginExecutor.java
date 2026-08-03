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
        return run(plugins, model, List.of(), Measurements.none(), options);
    }

    /**
     * Runs every plugin that can run, in dependency order.
     *
     * @param plugins the discovered plugins, in discovery order
     * @param model the classified model they all read
     * @param findings what the checks made of that model
     * @param measurements what was measured about the shape of the codebase
     * @param options the stated options, by plugin identifier
     * @return what the run produced and what it refused
     */
    public static PluginRun run(
            List<HexaGluePlugin> plugins,
            ArchModel model,
            List<Finding> findings,
            Measurements measurements,
            Map<String, Map<String, String>> options) {
        Objects.requireNonNull(plugins, "plugins must not be null");
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        Objects.requireNonNull(measurements, "measurements must not be null");
        Objects.requireNonNull(options, "options must not be null");

        List<Diagnostic> diagnostics = new ArrayList<>();
        Map<String, HexaGluePlugin> byId = new LinkedHashMap<>();
        Schedule schedule = Schedule.of(described(plugins, byId, diagnostics));

        Set<String> skipped = new TreeSet<>();
        schedule.excluded().forEach(exclusion -> skipped.add(exclusion.pluginId()));
        schedule.excluded().forEach(exclusion -> refusalOf(exclusion, schedule).ifPresent(diagnostics::add));
        schedule.duplicates()
                .forEach(pluginId -> diagnostics.add(diagnostic(
                        DUPLICATE_ID,
                        "two plugins claim the identifier " + pluginId + "; the first one read was kept")));

        Map<String, String> writtenBy = new LinkedHashMap<>();
        List<Document> documents = new ArrayList<>();
        List<String> executed = new ArrayList<>();
        for (String pluginId : schedule.order()) {
            if (skipped.contains(pluginId)) {
                continue;
            }
            // The schedule was built from these very manifests, so the plugin is always there.
            HexaGluePlugin plugin = Objects.requireNonNull(byId.get(pluginId), pluginId);
            Optional<Diagnostic> refusal =
                    contribute(plugin, model, findings, measurements, options, documents, writtenBy, diagnostics);
            if (refusal.isEmpty()) {
                executed.add(pluginId);
            } else {
                List<String> dependents = schedule.dependentsOf(pluginId);
                diagnostics.add(alsoSkipping(refusal.get(), dependents));
                skipped.add(pluginId);
                skipped.addAll(dependents);
            }
        }
        return new PluginRun(documents, diagnostics, executed, List.copyOf(skipped));
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
            Map<String, Map<String, String>> options,
            List<Document> documents,
            Map<String, String> writtenBy,
            List<Diagnostic> diagnostics) {
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

        List<Document> emitted = new ArrayList<>();
        try {
            plugin.contribute(new Contribution(
                    model, findings, measurements, PluginConfig.of(manifest.id(), stated), new Sinks(emitted::add)));
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
        collect(manifest.id(), emitted, documents, writtenBy, diagnostics);
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
     * Takes in what a plugin emitted, refusing a document another plugin already claimed — the
     * first writer keeps the path, and both claimants are named.
     */
    private static void collect(
            String pluginId,
            List<Document> emitted,
            List<Document> documents,
            Map<String, String> writtenBy,
            List<Diagnostic> diagnostics) {
        for (Document document : emitted) {
            String first = writtenBy.putIfAbsent(document.path(), pluginId);
            if (first == null) {
                documents.add(document);
            } else {
                diagnostics.add(diagnostic(
                        DOCUMENT_CLAIMED_TWICE,
                        "plugins " + first + " and " + pluginId + " both write " + document.path()
                                + "; the document of " + first + " was kept"));
            }
        }
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
