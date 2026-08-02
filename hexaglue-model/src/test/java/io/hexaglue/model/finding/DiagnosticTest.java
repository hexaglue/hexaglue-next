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

package io.hexaglue.model.finding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.RemediationHint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DiagnosticTest {

    private static final IssueCode CODE = IssueCode.of("HG-GEN-001");
    private static final TypeId ORDER = TypeId.of("com.shop.Order");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("a minimal diagnostic carries code, severity and message")
        void minimalDiagnosticCarriesEssentials() {
            Diagnostic diagnostic = Diagnostic.builder(
                            CODE, DiagnosticSeverity.ERROR, "Generation refused: confidence below threshold")
                    .build();

            assertThat(diagnostic.code()).isEqualTo(CODE);
            assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
            assertThat(diagnostic.message()).isEqualTo("Generation refused: confidence below threshold");
            assertThat(diagnostic.subject()).isEmpty();
            assertThat(diagnostic.location()).isEmpty();
            assertThat(diagnostic.remediations()).isEmpty();
        }

        @Test
        @DisplayName("a full diagnostic localizes the problem and suggests the remediation")
        void fullDiagnosticLocalizesAndSuggests() {
            SourceLocation location = new SourceLocation("com/shop/Order.java", 12, 12);
            RemediationHint remediation = RemediationHint.addAnnotation(
                    TypeId.of("org.jmolecules.ddd.annotation.AggregateRoot"), ArchKind.AGGREGATE_ROOT);

            Diagnostic diagnostic = Diagnostic.builder(
                            CODE, DiagnosticSeverity.WARNING, "Generation refused: confidence below threshold")
                    .subject(ORDER)
                    .location(location)
                    .remediations(List.of(remediation))
                    .build();

            assertThat(diagnostic.subject()).contains(ORDER);
            assertThat(diagnostic.location()).contains(location);
            assertThat(diagnostic.remediations()).containsExactly(remediation);
        }

        @Test
        @DisplayName("a blank message is rejected")
        void blankMessageIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Diagnostic.builder(CODE, DiagnosticSeverity.INFO, " ")
                            .build())
                    .withMessageContaining("message must not be blank");
        }
    }
}
