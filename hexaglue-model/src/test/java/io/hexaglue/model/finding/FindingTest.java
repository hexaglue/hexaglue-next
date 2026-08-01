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
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.RemediationHint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FindingTest {

    private static final IssueCode CODE = IssueCode.of("HG-DDD-012");
    private static final TypeId ORDER = TypeId.of("com.shop.Order");
    private static final TypeId CUSTOMER = TypeId.of("com.shop.Customer");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("a minimal finding carries code, severity, message and subject")
        void minimalFindingCarriesEssentials() {
            Finding finding = Finding.builder(CODE, Severity.MAJOR, "Aggregate without repository", ORDER)
                    .build();

            assertThat(finding.code()).isEqualTo(CODE);
            assertThat(finding.severity()).isEqualTo(Severity.MAJOR);
            assertThat(finding.message()).isEqualTo("Aggregate without repository");
            assertThat(finding.subject()).isEqualTo(ORDER);
            assertThat(finding.relatedTypes()).isEmpty();
            assertThat(finding.locations()).isEmpty();
            assertThat(finding.evidences()).isEmpty();
            assertThat(finding.remediations()).isEmpty();
        }

        @Test
        @DisplayName("a full finding carries its provenance and remediation")
        void fullFindingCarriesProvenanceAndRemediation() {
            Evidence evidence = Evidence.of(
                    EvidenceTier.LOCAL_STRUCTURE,
                    Confidence.HIGH,
                    "no repository signature uses com.shop.Order",
                    "an aggregate root is expected to be persisted through a driven port");
            RemediationHint remediation = RemediationHint.addAnnotation("AggregateRoot", ArchKind.AGGREGATE_ROOT);
            SourceLocation location = new SourceLocation("com/shop/Order.java", 12, 12);

            Finding finding = Finding.builder(CODE, Severity.CRITICAL, "Aggregate without repository", ORDER)
                    .relatedTypes(List.of(CUSTOMER))
                    .locations(List.of(location))
                    .evidences(List.of(evidence))
                    .remediations(List.of(remediation))
                    .build();

            assertThat(finding.relatedTypes()).containsExactly(CUSTOMER);
            assertThat(finding.locations()).containsExactly(location);
            assertThat(finding.evidences()).containsExactly(evidence);
            assertThat(finding.remediations()).containsExactly(remediation);
        }

        @Test
        @DisplayName("a blank message is rejected")
        void blankMessageIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            Finding.builder(CODE, Severity.MAJOR, "  ", ORDER).build())
                    .withMessageContaining("message must not be blank");
        }
    }
}
