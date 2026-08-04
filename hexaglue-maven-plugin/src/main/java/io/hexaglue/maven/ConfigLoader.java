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

package io.hexaglue.maven;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ModuleRole;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.config.GenerationConfig;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.config.ModulesConfig;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.maven.project.MavenProject;

/**
 * Reads {@code hexaglue.yaml} into the typed configuration, strictly.
 *
 * <p>Strictly means: an unknown key, a value of the wrong shape, a word outside a vocabulary, the
 * same key stated twice — each of them fails the build with a coded diagnostic. A configuration
 * quietly ignored is worse than none: the user believes a gate is armed, the build says nothing,
 * and the difference only surfaces as trust in a verdict nobody checked.</p>
 *
 * <p>The document mirrors the typed configuration, block by block:</p>
 *
 * <pre>{@code
 * analysis:
 *   basePackage: com.acme
 *   includePackages: [com.acme.domain]
 *   excludePackages: [com.acme.legacy]
 * classification:
 *   explicit:
 *     com.acme.Order: AGGREGATE_ROOT
 *   namingSuffixes:
 *     IDENTIFIER: [Id, Ref]
 * validation:
 *   failOnUnclassified: true
 *   minConfidence: HIGH
 *   failOnAmbiguous: true
 *   allowInferred: false
 *   findings:
 *     HG-DDD-012: BLOCKER
 * generation:
 *   minConfidence: HIGH
 * modules:
 *   shop-domain: DOMAIN
 *   shop-infra: INFRASTRUCTURE
 * }</pre>
 *
 * <p>An absent document is a project that configured nothing, and so is an empty one: both answer
 * the documented defaults. What is refused is a document stating something this cannot honour.</p>
 */
final class ConfigLoader {

    /** A value cannot be honoured: outside a vocabulary, or refused by the model itself. */
    private static final IssueCode VALUE_NOT_HONOURED = IssueCode.of("HG-CONFIG-003");

    /** The document names, in the order the loader looks for them. */
    private static final List<String> DOCUMENT_NAMES = List.of("hexaglue.yaml", "hexaglue.yml");

    private static final String ANALYSIS = "analysis";
    private static final String CLASSIFICATION = "classification";
    private static final String VALIDATION = "validation";
    private static final String GENERATION = "generation";

    /** Where a project states what it asks of each backend, keyed by plugin identifier. */
    private static final String PLUGINS = "plugins";

    /** Where a project states the role each module of its reactor plays, keyed by module name. */
    private static final String MODULES = "modules";

    private static final String BASE_PACKAGE = "basePackage";
    private static final String INCLUDE_PACKAGES = "includePackages";
    private static final String EXCLUDE_PACKAGES = "excludePackages";
    private static final String EXPLICIT = "explicit";
    private static final String NAMING_SUFFIXES = "namingSuffixes";
    private static final String FAIL_ON_UNCLASSIFIED = "failOnUnclassified";
    private static final String MIN_CONFIDENCE = "minConfidence";
    private static final String FAIL_ON_AMBIGUOUS = "failOnAmbiguous";
    private static final String ALLOW_INFERRED = "allowInferred";
    private static final String FINDINGS = "findings";

    private static final List<String> ROOT_KEYS =
            sorted(ANALYSIS, CLASSIFICATION, VALIDATION, GENERATION, MODULES, PLUGINS);
    private static final List<String> ANALYSIS_KEYS = sorted(BASE_PACKAGE, INCLUDE_PACKAGES, EXCLUDE_PACKAGES);
    private static final List<String> CLASSIFICATION_KEYS = sorted(EXPLICIT, NAMING_SUFFIXES);
    private static final List<String> VALIDATION_KEYS =
            sorted(FAIL_ON_UNCLASSIFIED, MIN_CONFIDENCE, FAIL_ON_AMBIGUOUS, ALLOW_INFERRED, FINDINGS);
    private static final List<String> GENERATION_KEYS = sorted(MIN_CONFIDENCE);

    private ConfigLoader() {}

    /**
     * Reads the configuration a project states beside its POM.
     *
     * @param projectDir the directory of the project being built
     * @return what the document states, or the documented defaults when there is none
     * @throws ConfigException when a document is there and cannot be honoured as written
     */
    static Map<String, Map<String, String>> readPluginOptions(Path projectDir) {
        Objects.requireNonNull(projectDir, "projectDir must not be null");
        return readPluginOptions(List.of(projectDir));
    }

    /**
     * Reads what the nearest document asks of each backend.
     *
     * @param directories where to look, nearest first
     * @return what the nearest document asks, or nothing when there is no document
     * @throws ConfigException when that document cannot be honoured as written
     */
    static Map<String, Map<String, String>> readPluginOptions(List<Path> directories) {
        Objects.requireNonNull(directories, "directories must not be null");
        return document(directories)
                .map(found -> pluginOptions(found.origin(), text(found.path(), found.origin())))
                .orElseGet(Map::of);
    }

    /**
     * Reads the configuration a project states beside its POM.
     *
     * @param projectDir the directory of the project being built
     * @return what the document states, or the documented defaults when there is none
     * @throws ConfigException when a document is there and cannot be honoured as written
     */
    static HexaGlueConfig read(Path projectDir) {
        Objects.requireNonNull(projectDir, "projectDir must not be null");
        return read(List.of(projectDir));
    }

    /**
     * Reads the nearest configuration a project or the reactor above it states.
     *
     * <p>The nearest document wins <em>whole</em>: a module stating one is read on its own, and
     * nothing of the reactor's leaks into it. Merging two documents would make every value depend
     * on a second file the reader is not looking at, and a gate would then be armed by something
     * the module never says.</p>
     *
     * @param directories where to look, nearest first
     * @return what the nearest document states, or the documented defaults when there is none
     * @throws ConfigException when that document cannot be honoured as written
     */
    static HexaGlueConfig read(List<Path> directories) {
        Objects.requireNonNull(directories, "directories must not be null");
        return document(directories)
                .map(found -> load(found.origin(), text(found.path(), found.origin())))
                .orElseGet(HexaGlueConfig::defaults);
    }

    /**
     * Returns where to look for the configuration of a project: beside it first, then beside each
     * project it inherits from, up to the root of the reactor.
     *
     * <p>A module of a reactor rarely states its own configuration, and having to copy the same
     * document into every module would guarantee they drift apart.</p>
     *
     * @param project the project being built
     * @return the directories to look in, nearest first
     */
    static List<Path> searchPath(MavenProject project) {
        Objects.requireNonNull(project, "project must not be null");
        List<Path> directories = new ArrayList<>();
        for (MavenProject current = project; current != null; current = current.getParent()) {
            Optional.ofNullable(current.getBasedir()).map(File::toPath).ifPresent(directories::add);
        }
        return directories;
    }

    /**
     * Returns the nearest document, looking for each spelling in turn in each directory.
     */
    private static Optional<Document> document(List<Path> directories) {
        for (Path directory : directories) {
            Objects.requireNonNull(directory, "a directory to look in must not be null");
            for (String name : DOCUMENT_NAMES) {
                Path path = directory.resolve(name);
                if (Files.isRegularFile(path)) {
                    return Optional.of(new Document(path, name));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * A document found on disk, under the name every diagnostic about it names.
     */
    private record Document(Path path, String origin) {}

    /**
     * Reads what the document asks of each backend.
     *
     * <p>The inner keys are left alone on purpose: only a plugin knows its own vocabulary, and it
     * refuses what it does not declare — with the alternatives named, which nothing here could
     * do.</p>
     *
     * @param origin where the document comes from, named in every diagnostic
     * @param yaml the document text
     * @return the stated options by plugin identifier, empty when the document states none
     * @throws ConfigException when the document cannot be honoured as written
     */
    static Map<String, Map<String, String>> pluginOptions(String origin, String yaml) {
        YamlDocument document = YamlDocument.parse(origin, yaml);
        if (document.isEmpty()) {
            return Map.of();
        }
        document.rejectUnknownKeys("the document", ROOT_KEYS);
        return document.sections(PLUGINS);
    }

    /**
     * Reads a configuration from its YAML text.
     *
     * @param origin where the document comes from, named in every diagnostic
     * @param yaml the document text
     * @return what the document states
     * @throws ConfigException when the document cannot be honoured as written
     */
    static HexaGlueConfig load(String origin, String yaml) {
        YamlDocument document = YamlDocument.parse(origin, yaml);
        if (document.isEmpty()) {
            return HexaGlueConfig.defaults();
        }
        document.rejectUnknownKeys("the document", ROOT_KEYS);
        return new HexaGlueConfig(
                analysis(document.block(ANALYSIS, ANALYSIS_KEYS)),
                classification(document.block(CLASSIFICATION, CLASSIFICATION_KEYS)),
                validation(document.block(VALIDATION, VALIDATION_KEYS)),
                generation(document.block(GENERATION, GENERATION_KEYS)),
                modules(document));
    }

    private static String text(Path document, String origin) {
        try {
            return Files.readString(document, StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new ConfigException(
                    Diagnostic.builder(
                                    YamlDocument.DOCUMENT_UNREADABLE,
                                    DiagnosticSeverity.ERROR,
                                    origin + " cannot be read: " + unreadable.getMessage())
                            .build(),
                    unreadable);
        }
    }

    private static AnalysisScope analysis(YamlDocument block) {
        if (block.isEmpty()) {
            return AnalysisScope.everything();
        }
        return refusedByTheModel(
                block,
                () -> new AnalysisScope(
                        block.text(BASE_PACKAGE), block.texts(INCLUDE_PACKAGES), block.texts(EXCLUDE_PACKAGES)));
    }

    private static ClassificationConfig classification(YamlDocument block) {
        if (block.isEmpty()) {
            return ClassificationConfig.defaults();
        }
        Map<TypeId, ArchKind> explicit = new LinkedHashMap<>();
        block.entries(EXPLICIT)
                .forEach((type, kind) -> explicit.put(
                        refusedByTheModel(block, () -> TypeId.of(type)),
                        word(block, EXPLICIT + "." + type, kind, ArchKind.class)));
        Map<ArchKind, List<String>> suffixes = new EnumMap<>(ArchKind.class);
        block.listEntries(NAMING_SUFFIXES)
                .forEach((kind, stated) -> suffixes.put(word(block, NAMING_SUFFIXES, kind, ArchKind.class), stated));
        return refusedByTheModel(block, () -> new ClassificationConfig(explicit, suffixes));
    }

    private static ValidationConfig validation(YamlDocument block) {
        ValidationConfig defaults = ValidationConfig.defaults();
        if (block.isEmpty()) {
            return defaults;
        }
        Map<IssueCode, Severity> thresholds = new LinkedHashMap<>();
        block.entries(FINDINGS)
                .forEach((code, severity) -> thresholds.put(
                        refusedByTheModel(block, () -> IssueCode.of(code)),
                        word(block, FINDINGS + "." + code, severity, Severity.class)));
        return refusedByTheModel(
                block,
                () -> ValidationConfig.builder()
                        .failOnUnclassified(block.flag(FAIL_ON_UNCLASSIFIED, defaults.failOnUnclassified()))
                        .minConfidence(confidence(block, defaults.minConfidence()))
                        .failOnAmbiguous(block.flag(FAIL_ON_AMBIGUOUS, defaults.failOnAmbiguous()))
                        .allowInferred(block.flag(ALLOW_INFERRED, defaults.allowInferred()))
                        .findingThresholds(thresholds)
                        .build());
    }

    /**
     * Reads the role of each module of the reactor. The keys are module names, so they are read
     * from the document itself rather than from a block of known keys: nothing here can list them,
     * and only the value is held to a vocabulary.
     */
    private static ModulesConfig modules(YamlDocument document) {
        Map<String, ModuleRole> roles = new LinkedHashMap<>();
        document.entries(MODULES)
                .forEach((module, role) ->
                        roles.put(module, word(document, MODULES + "." + module, role, ModuleRole.class)));
        return refusedByTheModel(document, () -> new ModulesConfig(roles));
    }

    private static GenerationConfig generation(YamlDocument block) {
        GenerationConfig defaults = GenerationConfig.defaults();
        if (block.isEmpty()) {
            return defaults;
        }
        return refusedByTheModel(block, () -> new GenerationConfig(confidence(block, defaults.minConfidence())));
    }

    private static Confidence confidence(YamlDocument block, Confidence fallback) {
        return block.text(MIN_CONFIDENCE)
                .map(stated -> word(block, MIN_CONFIDENCE, stated, Confidence.class))
                .orElse(fallback);
    }

    /**
     * Reads a word against the vocabulary it must belong to, naming that vocabulary when it does
     * not: a configuration is written by hand, so a refusal that lists the accepted words is the
     * difference between a fix and a guess.
     */
    private static <E extends Enum<E>> E word(YamlDocument block, String key, String stated, Class<E> vocabulary) {
        return Arrays.stream(vocabulary.getEnumConstants())
                .filter(constant -> constant.name().equals(stated))
                .findFirst()
                .orElseThrow(() -> block.failure(
                        VALUE_NOT_HONOURED,
                        "states '" + key + ": " + stated + "', which is not one of "
                                + Arrays.stream(vocabulary.getEnumConstants())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))));
    }

    /**
     * Runs what the model's own invariants may refuse, and reports that refusal in the model's
     * words: the configuration records are the authority on what they accept, and restating their
     * rules here would give one rule two voices.
     */
    private static <T> T refusedByTheModel(YamlDocument block, Supplier<T> binding) {
        try {
            return binding.get();
        } catch (IllegalArgumentException refused) {
            throw block.failure(
                    VALUE_NOT_HONOURED,
                    "states a configuration the model refuses: "
                            + Optional.ofNullable(refused.getMessage()).orElseGet(refused::toString),
                    refused);
        }
    }

    private static List<String> sorted(String... keys) {
        return Arrays.stream(keys).sorted().toList();
    }
}
