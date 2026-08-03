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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.IssueCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A plugin is untrusted code running inside a build. Whatever it does — throw, link against a
 * class that is not there, claim an option nobody declared, write where another plugin already
 * wrote — the run must come back with everything the other plugins produced and a coded account
 * of what it refused.
 */
class PluginExecutorTest {

    private static final IssueCode MISSING_DEPENDENCY = IssueCode.of("HG-PLUGIN-001");
    private static final IssueCode DEPENDENCY_CYCLE = IssueCode.of("HG-PLUGIN-002");
    private static final IssueCode PLUGIN_FAILED = IssueCode.of("HG-PLUGIN-003");
    private static final IssueCode OPTION_UNKNOWN = IssueCode.of("HG-PLUGIN-004");
    private static final IssueCode OPTION_MALFORMED = IssueCode.of("HG-PLUGIN-005");
    private static final IssueCode DOCUMENT_CLAIMED_TWICE = IssueCode.of("HG-PLUGIN-006");
    private static final IssueCode DUPLICATE_ID = IssueCode.of("HG-PLUGIN-007");

    private static final ArchModel EMPTY = ArchModel.builder().build();

    private static PluginRun run(List<HexaGluePlugin> plugins) {
        return PluginExecutor.run(plugins, EMPTY, Map.of());
    }

    private static Optional<Diagnostic> coded(PluginRun run, IssueCode code) {
        return run.diagnostics().stream()
                .filter(diagnostic -> diagnostic.code().equals(code))
                .findFirst();
    }

    /**
     * A plugin whose whole behaviour is what the test hands it.
     */
    private record TestPlugin(PluginManifest manifest, Consumer<Sinks> behaviour) implements HexaGluePlugin {

        static TestPlugin writing(String id, String path, String... dependsOn) {
            return new TestPlugin(
                    new PluginManifest(id, List.of(dependsOn), Set.of()),
                    sinks -> sinks.documents().emit(new Document(path, "written by " + id)));
        }

        @Override
        public void contribute(ArchModel model, PluginConfig config, Sinks sinks) {
            behaviour.accept(sinks);
        }
    }

    @Nested
    @DisplayName("a run that goes well")
    class HappyPath {

        @Test
        @DisplayName("collects what every plugin emitted, in execution order")
        void collectsDocumentsInExecutionOrder() {
            PluginRun run = run(List.of(TestPlugin.writing("b", "b.md", "a"), TestPlugin.writing("a", "a.md")));

            assertThat(run.executed()).containsExactly("a", "b");
            assertThat(run.documents()).extracting(Document::path).containsExactly("a.md", "b.md");
            assertThat(run.diagnostics()).isEmpty();
            assertThat(run.skipped()).isEmpty();
        }

        @Test
        @DisplayName("hands each plugin the options declared under its own identifier")
        void handsEachPluginItsOwnOptions() {
            PluginManifest manifest = new PluginManifest("doc", List.of(), Set.of("title"));
            HexaGluePlugin plugin = new TestPlugin(manifest, sinks -> {});
            StringBuilder seen = new StringBuilder();
            HexaGluePlugin reading = new HexaGluePlugin() {
                @Override
                public PluginManifest manifest() {
                    return manifest;
                }

                @Override
                public void contribute(ArchModel model, PluginConfig config, Sinks sinks) {
                    seen.append(config.text("title").orElse("none"));
                }
            };

            PluginExecutor.run(List.of(reading), EMPTY, Map.of("doc", Map.of("title", "Architecture")));

            assertThat(seen).hasToString("Architecture");
            assertThat(plugin.manifest().id()).isEqualTo("doc");
        }
    }

    @Nested
    @DisplayName("a plugin that misbehaves")
    class Isolation {

        @Test
        @DisplayName("survives a plugin that throws, and runs the others")
        void survivesAThrowingPlugin() {
            HexaGluePlugin exploding = new TestPlugin(new PluginManifest("boom", List.of(), Set.of()), sinks -> {
                throw new IllegalStateException("no model for me");
            });

            PluginRun run = run(List.of(exploding, TestPlugin.writing("quiet", "quiet.md")));

            assertThat(run.executed()).containsExactly("quiet");
            assertThat(run.documents()).extracting(Document::path).containsExactly("quiet.md");
            assertThat(coded(run, PLUGIN_FAILED).orElseThrow().message())
                    .contains("boom")
                    .contains("no model for me");
        }

        @Test
        @DisplayName("survives a plugin that fails to link")
        void survivesALinkageError() {
            HexaGluePlugin unlinkable = new TestPlugin(new PluginManifest("stale", List.of(), Set.of()), sinks -> {
                throw new NoSuchMethodError("io.hexaglue.model.arch.ArchModel.gone()");
            });

            PluginRun run = run(List.of(unlinkable, TestPlugin.writing("quiet", "quiet.md")));

            assertThat(run.executed()).containsExactly("quiet");
            assertThat(coded(run, PLUGIN_FAILED).orElseThrow().message()).contains("stale");
        }

        @Test
        @DisplayName("survives a plugin that cannot even describe itself")
        void survivesAPluginThatCannotDescribeItself() {
            HexaGluePlugin mute = new HexaGluePlugin() {
                @Override
                public PluginManifest manifest() {
                    throw new NoClassDefFoundError("io/hexaglue/spi/Gone");
                }

                @Override
                public void contribute(ArchModel model, PluginConfig config, Sinks sinks) {
                    throw new IllegalStateException("never reached");
                }
            };

            PluginRun run = run(List.of(mute, TestPlugin.writing("quiet", "quiet.md")));

            assertThat(run.executed()).containsExactly("quiet");
            assertThat(coded(run, PLUGIN_FAILED).orElseThrow().message()).contains("describe itself");
        }

        @Test
        @DisplayName("skips what depended on a plugin that failed")
        void skipsDependentsOfAFailedPlugin() {
            HexaGluePlugin exploding = new TestPlugin(new PluginManifest("base", List.of(), Set.of()), sinks -> {
                throw new IllegalStateException("gone");
            });

            PluginRun run = run(List.of(exploding, TestPlugin.writing("derived", "derived.md", "base")));

            assertThat(run.executed()).isEmpty();
            assertThat(run.skipped()).containsExactly("base", "derived");
            assertThat(run.documents()).isEmpty();
            assertThat(coded(run, PLUGIN_FAILED).orElseThrow().message()).contains("derived");
        }
    }

    @Nested
    @DisplayName("what the schedule refuses")
    class ScheduleRefusals {

        @Test
        @DisplayName("reports a dependency nobody provides, and runs the rest")
        void reportsAMissingDependency() {
            PluginRun run =
                    run(List.of(TestPlugin.writing("lonely", "lonely.md", "absent"), TestPlugin.writing("a", "a.md")));

            assertThat(run.executed()).containsExactly("a");
            assertThat(run.skipped()).containsExactly("lonely");
            assertThat(coded(run, MISSING_DEPENDENCY).orElseThrow().message())
                    .contains("lonely")
                    .contains("absent");
        }

        @Test
        @DisplayName("reports a dependency cycle by naming its members")
        void reportsACycle() {
            PluginRun run = run(List.of(TestPlugin.writing("b", "b.md", "c"), TestPlugin.writing("c", "c.md", "b")));

            assertThat(run.executed()).isEmpty();
            assertThat(coded(run, DEPENDENCY_CYCLE).orElseThrow().message())
                    .contains("b")
                    .contains("c");
        }

        @Test
        @DisplayName("reports two plugins claiming the same identifier")
        void reportsADuplicateIdentifier() {
            PluginRun run = run(List.of(TestPlugin.writing("twin", "one.md"), TestPlugin.writing("twin", "two.md")));

            assertThat(run.executed()).containsExactly("twin");
            assertThat(run.documents()).extracting(Document::path).containsExactly("one.md");
            assertThat(coded(run, DUPLICATE_ID).orElseThrow().message()).contains("twin");
        }
    }

    @Nested
    @DisplayName("options")
    class Options {

        @Test
        @DisplayName("refuses an option the plugin never declared, before running it")
        void refusesAnUndeclaredOption() {
            PluginManifest manifest = new PluginManifest("doc", List.of(), Set.of("title"));
            HexaGluePlugin plugin =
                    new TestPlugin(manifest, sinks -> sinks.documents().emit(new Document("doc.md", "x")));

            PluginRun run = PluginExecutor.run(List.of(plugin), EMPTY, Map.of("doc", Map.of("titel", "typo")));

            assertThat(run.executed()).isEmpty();
            assertThat(run.documents()).isEmpty();
            Diagnostic refusal = coded(run, OPTION_UNKNOWN).orElseThrow();
            assertThat(refusal.message()).contains("titel").contains("title");
        }

        @Test
        @DisplayName("reports a value whose shape the plugin could not read")
        void reportsAMalformedValue() {
            PluginManifest manifest = new PluginManifest("doc", List.of(), Set.of("depth"));
            HexaGluePlugin plugin = new HexaGluePlugin() {
                @Override
                public PluginManifest manifest() {
                    return manifest;
                }

                @Override
                public void contribute(ArchModel model, PluginConfig config, Sinks sinks) {
                    config.number("depth", 3);
                }
            };

            PluginRun run = PluginExecutor.run(List.of(plugin), EMPTY, Map.of("doc", Map.of("depth", "deep")));

            assertThat(run.executed()).isEmpty();
            Diagnostic refusal = coded(run, OPTION_MALFORMED).orElseThrow();
            assertThat(refusal.message()).contains("depth").contains("deep");
        }
    }

    @Nested
    @DisplayName("two plugins writing the same place")
    class DocumentConflicts {

        @Test
        @DisplayName("keeps the first document and names both claimants")
        void keepsTheFirstDocument() {
            PluginRun run = run(List.of(TestPlugin.writing("a", "shared.md"), TestPlugin.writing("b", "shared.md")));

            assertThat(run.documents())
                    .singleElement()
                    .satisfies(document -> assertThat(document.content()).isEqualTo("written by a"));
            assertThat(coded(run, DOCUMENT_CLAIMED_TWICE).orElseThrow().message())
                    .contains("shared.md")
                    .contains("a")
                    .contains("b");
        }
    }
}
