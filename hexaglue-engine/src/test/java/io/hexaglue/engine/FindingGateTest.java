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

package io.hexaglue.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A finding fails a build only where the configuration armed its code. Anything else would mean a
 * tool deciding on its own that somebody's architecture is not allowed to compile.
 */
class FindingGateTest {

    private static final IssueCode ARMED = IssueCode.of("HG-DDD-001");
    private static final IssueCode UNARMED = IssueCode.of("HG-HEX-004");
    private static final TypeId SUBJECT = TypeId.of("com.shop.Order");

    private static ArchModel model() {
        return model(Confidence.EXPLICIT, Basis.DECLARED);
    }

    private static ArchModel model(Confidence confidence, Basis basis) {
        ArchType type = new ValueObject(
                SUBJECT,
                TypeStructure.builder(TypeNature.CLASS).build(),
                Classification.builder(ArchKind.VALUE_OBJECT, confidence, basis, ProofNode.fact("by fixture"))
                        .build());
        return ArchModel.builder().addType(type).build();
    }

    private static Finding finding(IssueCode code, Severity severity) {
        return Finding.builder(code, severity, "the aggregate is reachable from outside", SUBJECT)
                .build();
    }

    private static ValidationConfig arming(Map<IssueCode, Severity> thresholds) {
        return ValidationConfig.builder().findingThresholds(thresholds).build();
    }

    @Nested
    @DisplayName("a code the configuration armed")
    class Armed {

        @Test
        @DisplayName("refuses the build when a finding reaches the stated severity")
        void refusesAtTheThreshold() {
            Validation validation = Validation.of(
                    model(), List.of(finding(ARMED, Severity.CRITICAL)), arming(Map.of(ARMED, Severity.CRITICAL)));

            assertThat(validation.passed()).isFalse();
            assertThat(validation.refusals()).singleElement().satisfies(refusal -> {
                assertThat(refusal.gate()).isEqualTo(Gate.FINDING);
                assertThat(refusal.subject().id()).isEqualTo(SUBJECT);
                assertThat(refusal.reason())
                        .contains("HG-DDD-001")
                        .contains("CRITICAL")
                        .contains("reachable from outside");
            });
        }

        @Test
        @DisplayName("refuses the build when a finding is worse than the stated severity")
        void refusesAboveTheThreshold() {
            Validation validation = Validation.of(
                    model(), List.of(finding(ARMED, Severity.BLOCKER)), arming(Map.of(ARMED, Severity.MAJOR)));

            assertThat(validation.passed()).isFalse();
        }

        @Test
        @DisplayName("lets the build pass when the finding is milder than what was armed")
        void passesBelowTheThreshold() {
            Validation validation = Validation.of(
                    model(), List.of(finding(ARMED, Severity.MINOR)), arming(Map.of(ARMED, Severity.CRITICAL)));

            assertThat(validation.passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("a code nobody armed")
    class Unarmed {

        @Test
        @DisplayName("says nothing, whatever it found")
        void staysSilent() {
            Validation validation = Validation.of(
                    model(), List.of(finding(UNARMED, Severity.BLOCKER)), arming(Map.of(ARMED, Severity.MINOR)));

            assertThat(validation.passed()).isTrue();
        }

        @Test
        @DisplayName("says nothing on the default configuration, which arms nothing")
        void staysSilentByDefault() {
            Validation validation =
                    Validation.of(model(), List.of(finding(ARMED, Severity.BLOCKER)), ValidationConfig.defaults());

            assertThat(validation.passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("what the two gates say together")
    class Together {

        @Test
        @DisplayName("reports what the reading could not settle before what the architecture got wrong")
        void reportsTheReadingFirst() {
            Validation validation = Validation.of(
                    model(Confidence.MEDIUM, Basis.INFERRED),
                    List.of(finding(ARMED, Severity.BLOCKER)),
                    ValidationConfig.builder()
                            .minConfidence(Confidence.HIGH)
                            .allowInferred(false)
                            .findingThresholds(Map.of(ARMED, Severity.MAJOR))
                            .build());

            assertThat(validation.refusals())
                    .extracting(Validation.Refusal::gate)
                    .containsExactly(Gate.CONFIDENCE, Gate.INFERRED, Gate.FINDING);
        }

        @Test
        @DisplayName("says nothing about a finding whose subject the model does not hold")
        void ignoresAFindingAboutSomethingElse() {
            Finding elsewhere = Finding.builder(
                            ARMED, Severity.BLOCKER, "about a type nobody analysed", TypeId.of("com.other.Thing"))
                    .build();

            Validation validation = Validation.of(model(), List.of(elsewhere), arming(Map.of(ARMED, Severity.MINOR)));

            assertThat(validation.passed()).isTrue();
        }
    }
}
