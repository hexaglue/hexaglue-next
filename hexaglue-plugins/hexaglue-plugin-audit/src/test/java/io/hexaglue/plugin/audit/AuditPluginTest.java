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

package io.hexaglue.plugin.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.Stability;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import io.hexaglue.spi.Document;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.Measurements;
import io.hexaglue.spi.PluginExecutor;
import io.hexaglue.spi.PluginRun;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The report says what the run concluded and nothing else. What is checked here is that every
 * section arrives, that the number is explained by its parts, and that a reader is told how far to
 * trust any of it.
 */
class AuditPluginTest {

    private static final TypeId ORDER = TypeId.of("com.shop.domain.Order");
    private static final TypeId TOOLS = TypeId.of("com.shop.util.Tools");

    private static ArchType type(TypeId id, ArchKind kind, Basis basis) {
        Classification verdict = Classification.builder(kind, Confidence.HIGH, basis, ProofNode.fact("by fixture"))
                .build();
        return kind == ArchKind.UNCLASSIFIED
                ? new UnclassifiedType(
                        id,
                        TypeStructure.builder(TypeNature.CLASS).build(),
                        verdict,
                        UnclassifiedType.UnclassifiedCategory.UTILITY,
                        Optional.of("nothing uses it"))
                : new ValueObject(id, TypeStructure.builder(TypeNature.RECORD).build(), verdict);
    }

    private static ArchModel model() {
        return ArchModel.builder()
                .addType(type(ORDER, ArchKind.VALUE_OBJECT, Basis.DECLARED))
                .addType(type(TOOLS, ArchKind.UNCLASSIFIED, Basis.INFERRED))
                .build();
    }

    private static Measurements measurements() {
        return new Measurements(
                List.of(Stability.of("com.shop.domain", 0, 1, 1, 1), Stability.of("com.shop.util", 1, 0, 0, 1)),
                List.of(List.of("com.shop.a", "com.shop.b")));
    }

    private static List<Finding> findings() {
        return List.of(
                Finding.builder(IssueCode.of("HG-DDD-001"), Severity.MAJOR, "Invoice reaches OrderLine directly", ORDER)
                        .build());
    }

    private static PluginRun run(Map<String, String> options) {
        List<HexaGluePlugin> plugins = List.of(new AuditPlugin());
        return PluginExecutor.run(plugins, model(), findings(), measurements(), Map.of(AuditPlugin.ID, options));
    }

    private static String document(PluginRun run, String name) {
        return run.documents().stream()
                .filter(document -> document.path().endsWith(name))
                .map(Document::content)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no document named " + name + " in " + run.documents()));
    }

    @Nested
    @DisplayName("what a run produces")
    class Produced {

        @Test
        @DisplayName("writes the report and its data form under the directory it was given")
        void writesBothForms() {
            PluginRun run = run(Map.of("outputDirectory", "target/report"));

            assertThat(run.executed()).containsExactly(AuditPlugin.ID);
            assertThat(run.diagnostics()).isEmpty();
            assertThat(run.documents())
                    .extracting(Document::path)
                    .containsExactly("target/report/architecture-audit.md", "target/report/architecture-audit.json");
        }

        @Test
        @DisplayName("writes markdown alone when the data form is refused")
        void writesMarkdownAlone() {
            assertThat(run(Map.of("writeJson", "false")).documents())
                    .extracting(Document::path)
                    .containsExactly("audit/architecture-audit.md");
        }
    }

    @Nested
    @DisplayName("the seven sections")
    class Sections {

        @Test
        @DisplayName("all arrive, in the order a reader wants them")
        void allArrive() {
            String report = document(run(Map.of()), AuditReport.NAME);

            assertThat(report.indexOf("## Verdict"))
                    .isLessThan(report.indexOf("## What the score is made of"))
                    .isNotNegative();
            assertThat(report)
                    .contains("## Violations")
                    .contains("## How far to trust this")
                    .contains("## Quality metrics")
                    .contains("## Inventory")
                    .contains("## Package stability")
                    .contains("## What it would take");
        }

        @Test
        @DisplayName("the verdict says the number, the grade and what it counted")
        void statesTheVerdict() {
            String report = document(run(Map.of()), AuditReport.NAME);

            assertThat(report).contains("/100").contains("grade").contains("1 violation over 2 analysed types");
        }

        @Test
        @DisplayName("the inventory carries the provenance of every verdict")
        void carriesProvenance() {
            String report = document(run(Map.of()), AuditReport.NAME);

            assertThat(report).contains("| `Order` | VALUE_OBJECT | HIGH | DECLARED |");
        }

        @Test
        @DisplayName("the reliability section says how much was stated and how much deduced")
        void saysHowFarToTrustIt() {
            String report = document(run(Map.of()), AuditReport.NAME);

            assertThat(report)
                    .contains("| Stated by the sources | 1 |")
                    .contains("| Deduced by the engine | 1 |")
                    .contains("| Not classified at all | 1 |");
        }

        @Test
        @DisplayName("the stability section carries what was measured, not something recomputed")
        void carriesTheMeasures() {
            String report = document(run(Map.of()), AuditReport.NAME);

            assertThat(report).contains("| `com.shop.domain` | 1 | 0 | 0.00 | 1.00 | 0.00 |");
        }

        @Test
        @DisplayName("draws the knots when asked, and stays silent when refused")
        void drawsKnotsOnlyWhenAsked() {
            assertThat(document(run(Map.of()), AuditReport.NAME))
                    .contains("```mermaid")
                    .contains("com_shop_a");
            assertThat(document(run(Map.of("generateDiagrams", "false")), AuditReport.NAME))
                    .doesNotContain("```mermaid");
        }
    }

    @Nested
    @DisplayName("the data form")
    class Data {

        @Test
        @DisplayName("carries the verdict, the violations, the inventory and the packages")
        void carriesEverything() {
            String json = document(run(Map.of()), JsonReport.NAME);

            assertThat(json)
                    .contains("\"grade\"")
                    .contains("\"HG-DDD-001\"")
                    .contains("\"com.shop.domain.Order\"")
                    .contains("\"instability\"");
        }

        @Test
        @DisplayName("is byte-identical between two runs on the same conclusions")
        void isStable() {
            assertThat(document(run(Map.of()), JsonReport.NAME)).isEqualTo(document(run(Map.of()), JsonReport.NAME));
        }
    }

    @Nested
    @DisplayName("the score")
    class Scoring {

        @Test
        @DisplayName("gives an untouched codebase full marks rather than a failing grade")
        void scoresAnEmptyCodebaseWell() {
            Score score = Score.of(ArchModel.builder().build(), List.of(), Measurements.none());

            assertThat(score.overall()).isEqualTo(100);
            assertThat(score.grade()).isEqualTo("A");
        }

        @Test
        @DisplayName("counts a type nothing could name against the reading, not against the architecture")
        void separatesReadingFromSoundness() {
            Score score = Score.of(model(), List.of(), Measurements.none());

            assertThat(score.readable()).isEqualTo(50);
            assertThat(score.sound()).isEqualTo(100);
        }

        @Test
        @DisplayName("counts a serious violation against soundness")
        void countsViolations() {
            Score score = Score.of(model(), findings(), Measurements.none());

            assertThat(score.sound()).isZero();
        }
    }

    @Nested
    @DisplayName("options")
    class Options {

        @Test
        @DisplayName("declares every option it reads, so a typo is refused rather than ignored")
        void declaresWhatItReads() {
            PluginRun refused = run(Map.of("writeJsonn", "false"));

            assertThat(refused.executed()).isEmpty();
            assertThat(refused.diagnostics())
                    .singleElement()
                    .satisfies(diagnostic -> assertThat(diagnostic.message()).contains("writeJsonn"));
        }
    }
}
