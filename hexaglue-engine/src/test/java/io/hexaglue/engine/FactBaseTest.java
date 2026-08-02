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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FactBaseTest {

    private static final TypeId ORDER = TypeId.of("com.acme.Order");
    private static final TypeId CUSTOMER = TypeId.of("com.acme.Customer");

    private static KindEvidence evidence(TypeId subject, ArchKind kind, String fact) {
        return new KindEvidence(
                subject,
                kind,
                Evidence.of(EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, fact, "because the shape says so"),
                0,
                ProofNode.fact(fact));
    }

    @Nested
    @DisplayName("holds a fact")
    class HoldsAFact {

        @Test
        @DisplayName("once, however many times it is derived")
        void onceHoweverManyTimesDerived() {
            FactBase facts = new FactBase();

            assertThat(facts.add(evidence(ORDER, ArchKind.AGGREGATE_ROOT, "record shape")))
                    .isTrue();
            assertThat(facts.add(evidence(ORDER, ArchKind.AGGREGATE_ROOT, "record shape")))
                    .isFalse();
            assertThat(facts.all(KindEvidence.class)).hasSize(1);
        }

        @Test
        @DisplayName("keeping the proof of the route that reached it first")
        void keepingTheFirstProof() {
            FactBase facts = new FactBase();
            KindEvidence first = new KindEvidence(
                    ORDER,
                    ArchKind.AGGREGATE_ROOT,
                    Evidence.of(EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, "shape", "first route"),
                    0,
                    ProofNode.derived(RuleId.of("A"), "shape"));
            KindEvidence second = new KindEvidence(
                    ORDER,
                    ArchKind.AGGREGATE_ROOT,
                    Evidence.of(EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, "shape", "first route"),
                    0,
                    ProofNode.derived(RuleId.of("B"), "shape"));

            facts.add(first);
            facts.add(second);

            assertThat(facts.all(KindEvidence.class))
                    .singleElement()
                    .extracting(fact -> fact.proof().rule())
                    .isEqualTo(java.util.Optional.of(RuleId.of("A")));
        }
    }

    @Nested
    @DisplayName("answers")
    class Answers {

        @Test
        @DisplayName("in subject order, whatever the derivation order")
        void inSubjectOrder() {
            FactBase facts = new FactBase();
            facts.add(evidence(ORDER, ArchKind.AGGREGATE_ROOT, "shape"));
            facts.add(evidence(CUSTOMER, ArchKind.AGGREGATE_ROOT, "shape"));

            assertThat(facts.all(KindEvidence.class))
                    .extracting(KindEvidence::subject)
                    .containsExactly(CUSTOMER, ORDER);
        }

        @Test
        @DisplayName("only what is known about one subject")
        void onlyAboutOneSubject() {
            FactBase facts = new FactBase();
            facts.add(evidence(ORDER, ArchKind.AGGREGATE_ROOT, "shape"));
            facts.add(evidence(CUSTOMER, ArchKind.AGGREGATE_ROOT, "shape"));

            assertThat(facts.about(ORDER, KindEvidence.class))
                    .extracting(KindEvidence::subject)
                    .containsExactly(ORDER);
        }

        @Test
        @DisplayName("nothing about a subject no rule spoke of")
        void nothingAboutAnUnknownSubject() {
            assertThat(new FactBase().about(ORDER, KindEvidence.class)).isEmpty();
        }

        @Test
        @DisplayName("how many facts it holds")
        void howManyFactsItHolds() {
            FactBase facts = new FactBase();
            facts.add(evidence(ORDER, ArchKind.AGGREGATE_ROOT, "shape"));
            facts.add(evidence(ORDER, ArchKind.ENTITY, "other shape"));

            assertThat(facts.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("refuses")
    class Refuses {

        @Test
        @DisplayName("a fact shape no predicate names")
        void aFactShapeNoPredicateNames() {
            assertThatThrownBy(() -> Predicate.of(Fact.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Fact");
        }
    }
}
