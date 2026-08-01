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

package io.hexaglue.model.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.TypeId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClassificationContractTest {

    @Nested
    @DisplayName("One confidence scale")
    class OneConfidenceScale {

        @Test
        @DisplayName("the scale orders EXPLICIT above HIGH above MEDIUM above LOW")
        void scaleOrdersConfidences() {
            assertThat(Confidence.EXPLICIT.isAtLeast(Confidence.HIGH)).isTrue();
            assertThat(Confidence.HIGH.isAtLeast(Confidence.HIGH)).isTrue();
            assertThat(Confidence.MEDIUM.isAtLeast(Confidence.HIGH)).isFalse();
            assertThat(Confidence.LOW.isAtLeast(Confidence.MEDIUM)).isFalse();
        }

        @Test
        @DisplayName("only EXPLICIT and HIGH are reliable")
        void onlyExplicitAndHighAreReliable() {
            assertThat(Confidence.EXPLICIT.isReliable()).isTrue();
            assertThat(Confidence.HIGH.isReliable()).isTrue();
            assertThat(Confidence.MEDIUM.isReliable()).isFalse();
            assertThat(Confidence.LOW.isReliable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Evidence tiers")
    class EvidenceTiers {

        @Test
        @DisplayName("tiers are ordered S1 to S6 with their confidence ceilings")
        void tiersAreOrderedWithCeilings() {
            assertThat(EvidenceTier.values())
                    .extracting(EvidenceTier::code)
                    .containsExactly("S1", "S2", "S3", "S4", "S5", "S6");
            assertThat(EvidenceTier.DECLARED_INTENT.maxConfidence()).isEqualTo(Confidence.EXPLICIT);
            assertThat(EvidenceTier.FRAMEWORK_KNOWLEDGE.maxConfidence()).isEqualTo(Confidence.HIGH);
            assertThat(EvidenceTier.GRAPH_RELATION.maxConfidence()).isEqualTo(Confidence.HIGH);
            assertThat(EvidenceTier.LOCAL_STRUCTURE.maxConfidence()).isEqualTo(Confidence.HIGH);
            assertThat(EvidenceTier.TOPOLOGY.maxConfidence()).isEqualTo(Confidence.MEDIUM);
            assertThat(EvidenceTier.NAMING.maxConfidence()).isEqualTo(Confidence.MEDIUM);
        }

        @Test
        @DisplayName("a naming evidence can never claim HIGH confidence")
        void namingEvidenceCanNeverClaimHigh() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Evidence.of(
                            EvidenceTier.NAMING, Confidence.HIGH, "SUFFIX(Repository)", "name ends with Repository"))
                    .withMessageContaining("ceiling");
        }

        @Test
        @DisplayName("a structural evidence can never claim EXPLICIT confidence")
        void structuralEvidenceCanNeverClaimExplicit() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Evidence.of(
                            EvidenceTier.LOCAL_STRUCTURE, Confidence.EXPLICIT, "RECORD", "type is a record"));
        }

        @Test
        @DisplayName("an evidence within its ceiling carries its facts")
        void evidenceWithinCeilingCarriesFacts() {
            Evidence evidence = new Evidence(
                    EvidenceTier.FRAMEWORK_KNOWLEDGE,
                    Confidence.HIGH,
                    "SPRING_DATA_REPOSITORY(Order, OrderId)",
                    "extends JpaRepository<Order, OrderId>",
                    java.util.Optional.empty(),
                    List.of(TypeId.of("com.a.Order"), TypeId.of("com.a.OrderId")));

            assertThat(evidence.tier()).isEqualTo(EvidenceTier.FRAMEWORK_KNOWLEDGE);
            assertThat(evidence.relatedTypes()).hasSize(2);
        }

        @Test
        @DisplayName("a blank fact is rejected")
        void blankFactIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Evidence.of(EvidenceTier.NAMING, Confidence.MEDIUM, " ", "why"));
        }
    }

    @Nested
    @DisplayName("Proof trees")
    class ProofTrees {

        @Test
        @DisplayName("a derivation remembers its rule and premises")
        void derivationRemembersRuleAndPremises() {
            ProofNode fact = ProofNode.fact("extends(OrderRepository, JpaRepository<Order, OrderId>)");
            ProofNode derived = ProofNode.derived(RuleId.of("R1"), "AGGREGATE_ROOT(Order)", fact);

            assertThat(derived.rule()).contains(RuleId.of("R1"));
            assertThat(derived.premises()).containsExactly(fact);
            assertThat(fact.rule()).isEmpty();
            assertThat(fact.premises()).isEmpty();
        }

        @Test
        @DisplayName("a base fact cannot have premises")
        void baseFactCannotHavePremises() {
            ProofNode fact = ProofNode.fact("annotated(Order, Entity)");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ProofNode("conclusion", java.util.Optional.empty(), List.of(fact)));
        }

        @Test
        @DisplayName("rule ids render as their value")
        void ruleIdsRenderAsTheirValue() {
            assertThat(RuleId.of("R4")).hasToString("R4");
            assertThatIllegalArgumentException().isThrownBy(() -> RuleId.of(" "));
        }
    }
}
