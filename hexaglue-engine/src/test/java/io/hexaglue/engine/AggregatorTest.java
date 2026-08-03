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
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AggregatorTest {

    private static final TypeId ORDER = TypeId.of("com.acme.Order");

    private static final Perimeter PERIMETER = Perimeter.of(
            CodeModel.builder()
                    .addType(TypeNode.builder(ORDER, TypeNature.RECORD).build())
                    .build(),
            AnalysisScope.everything());

    private static KindEvidence evidence(ArchKind kind, EvidenceTier tier, String fact, int distance) {
        return new KindEvidence(
                ORDER,
                kind,
                Evidence.of(tier, tier.maxConfidence(), fact, "because " + fact),
                distance,
                ProofNode.fact(fact));
    }

    private static Classification decide(KindEvidence... evidences) {
        FactBase facts = new FactBase();
        for (KindEvidence evidence : evidences) {
            facts.add(evidence);
        }
        return Aggregator.decide(facts, PERIMETER).verdict(ORDER).orElseThrow();
    }

    @Nested
    @DisplayName("weighs the evidences")
    class WeighsTheEvidences {

        @Test
        @DisplayName("taking the only kind anything speaks for")
        void takingTheOnlyKindAnythingSpeaksFor() {
            Classification verdict =
                    decide(evidence(ArchKind.AGGREGATE_ROOT, EvidenceTier.FRAMEWORK_KNOWLEDGE, "managed by a repo", 0));

            assertThat(verdict.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
            assertThat(verdict.basis()).isEqualTo(Basis.INFERRED);
            assertThat(verdict.isAmbiguous()).isFalse();
        }

        @Test
        @DisplayName("letting the stronger tier win however many weak signals oppose it")
        void lettingTheStrongerTierWin() {
            Classification verdict = decide(
                    evidence(ArchKind.AGGREGATE_ROOT, EvidenceTier.FRAMEWORK_KNOWLEDGE, "managed by a repo", 0),
                    evidence(ArchKind.VALUE_OBJECT, EvidenceTier.NAMING, "name says value", 0),
                    evidence(ArchKind.VALUE_OBJECT, EvidenceTier.NAMING, "name says immutable", 0),
                    evidence(ArchKind.VALUE_OBJECT, EvidenceTier.TOPOLOGY, "sits in a values package", 0));

            assertThat(verdict.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("counting them at equal tier")
        void countingThemAtEqualTier() {
            Classification verdict = decide(
                    evidence(ArchKind.ENTITY, EvidenceTier.LOCAL_STRUCTURE, "has an identity field", 0),
                    evidence(ArchKind.ENTITY, EvidenceTier.LOCAL_STRUCTURE, "is mutable", 0),
                    evidence(ArchKind.VALUE_OBJECT, EvidenceTier.LOCAL_STRUCTURE, "is a record", 0));

            assertThat(verdict.kind()).isEqualTo(ArchKind.ENTITY);
        }

        @Test
        @DisplayName("keeping the nearer signal when the tally ties")
        void keepingTheNearerSignalWhenTheTallyTies() {
            // jMolecules AggregateRoot extends Entity: implementing the first declares both, and
            // only the distance says which one was written down.
            Classification verdict = decide(
                    evidence(ArchKind.AGGREGATE_ROOT, EvidenceTier.DECLARED_INTENT, "DECLARED_KIND(AGGREGATE_ROOT)", 1),
                    evidence(ArchKind.ENTITY, EvidenceTier.DECLARED_INTENT, "DECLARED_KIND(ENTITY)", 2));

            assertThat(verdict.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
            assertThat(verdict.confidence()).isEqualTo(Confidence.EXPLICIT);
            assertThat(verdict.basis()).isEqualTo(Basis.DECLARED);
        }

        @Test
        @DisplayName("separating the two sides of the hexagon on their structural signal alone")
        void separatingTheTwoSidesOfTheHexagon() {
            // Two directions competing is no longer a collision of hard-coded priorities: the
            // structural signal outranks the name because of where each sits, not because a
            // table said which one to prefer.
            Classification verdict = decide(
                    evidence(ArchKind.DRIVING_PORT, EvidenceTier.GRAPH_RELATION, "called by an entry point", 0),
                    evidence(ArchKind.DRIVEN_PORT, EvidenceTier.NAMING, "name ends in Repository", 0));

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVING_PORT);
            assertThat(verdict.direction()).contains(PortDirection.DRIVING);
        }
    }

    @Nested
    @DisplayName("refuses to decide")
    class RefusesToDecide {

        @Test
        @DisplayName("when nothing separates two candidates, and keeps them both")
        void whenNothingSeparatesTwoCandidates() {
            Classification verdict = decide(
                    evidence(ArchKind.ENTITY, EvidenceTier.LOCAL_STRUCTURE, "has an identity field", 0),
                    evidence(ArchKind.VALUE_OBJECT, EvidenceTier.LOCAL_STRUCTURE, "is a record", 0));

            assertThat(verdict.kind()).isEqualTo(ArchKind.UNCLASSIFIED);
            assertThat(verdict.confidence()).isEqualTo(Confidence.LOW);
            assertThat(verdict.isAmbiguous()).isTrue();
            assertThat(verdict.candidates())
                    .extracting(Candidate::kind)
                    .containsExactly(ArchKind.ENTITY, ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("when no rule said anything at all")
        void whenNoRuleSaidAnything() {
            Classification verdict =
                    Aggregator.decide(new FactBase(), PERIMETER).verdict(ORDER).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.UNCLASSIFIED);
            assertThat(verdict.confidence()).isEqualTo(Confidence.LOW);
            assertThat(verdict.candidates()).isEmpty();
            assertThat(verdict.evidences()).isEmpty();
        }

        @Test
        @DisplayName("about a type it owes no verdict on")
        void aboutATypeItOwesNoVerdictOn() {
            assertThat(Aggregator.decide(new FactBase(), PERIMETER).verdict(TypeId.of("com.acme.Absent")))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("shows its work")
    class ShowsItsWork {

        @Test
        @DisplayName("carrying the evidences that decided, strongest tier first")
        void carryingTheEvidencesThatDecided() {
            Classification verdict = decide(
                    evidence(ArchKind.ENTITY, EvidenceTier.LOCAL_STRUCTURE, "has an identity field", 0),
                    evidence(ArchKind.ENTITY, EvidenceTier.FRAMEWORK_KNOWLEDGE, "managed by a repo", 0));

            assertThat(verdict.evidences())
                    .extracting(Evidence::tier)
                    .containsExactly(EvidenceTier.FRAMEWORK_KNOWLEDGE, EvidenceTier.LOCAL_STRUCTURE);
        }

        @Test
        @DisplayName("rooting the proof of the verdict in the proof of each evidence")
        void rootingTheProofInEachEvidence() {
            Classification verdict =
                    decide(evidence(ArchKind.AGGREGATE_ROOT, EvidenceTier.FRAMEWORK_KNOWLEDGE, "managed by a repo", 0));

            assertThat(verdict.proof().rule()).contains(Aggregator.ID);
            assertThat(verdict.proof().premises())
                    .extracting(ProofNode::conclusion)
                    .containsExactly("managed by a repo");
        }

        @Test
        @DisplayName("saying so when it had nothing to go on")
        void sayingSoWhenItHadNothingToGoOn() {
            Classification verdict =
                    Aggregator.decide(new FactBase(), PERIMETER).verdict(ORDER).orElseThrow();

            assertThat(verdict.proof().conclusion()).contains("no signal");
        }

        @Test
        @DisplayName("stating what carried the decision rather than the number it sorted on")
        void statingWhatCarriedTheDecisionRatherThanTheNumberItSortedOn() {
            Classification verdict = decide(
                    evidence(ArchKind.ENTITY, EvidenceTier.FRAMEWORK_KNOWLEDGE, "managed by a repo", 0),
                    evidence(ArchKind.ENTITY, EvidenceTier.GRAPH_RELATION, "kept by a port", 0));

            assertThat(verdict.proof().conclusion())
                    .isEqualTo("ENTITY(com.acme.Order) [decided on 1 signal at S2, 1 at S3]");
        }

        @Test
        @DisplayName("mentioning how far a signal was found only when it was not found here")
        void mentioningHowFarASignalWasFoundOnlyWhenItWasNotFoundHere() {
            Classification verdict =
                    decide(evidence(ArchKind.ENTITY, EvidenceTier.FRAMEWORK_KNOWLEDGE, "managed by a repo", 2));

            assertThat(verdict.proof().conclusion())
                    .isEqualTo("ENTITY(com.acme.Order) [decided on 1 signal at S2, nearest 2 steps away]");
        }
    }
}
