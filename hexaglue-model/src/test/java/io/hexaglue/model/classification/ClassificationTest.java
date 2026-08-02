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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassificationTest {

    private static final ProofNode PROOF = ProofNode.derived(
            RuleId.of("R1"), "AGGREGATE_ROOT(Order)", ProofNode.fact("extends(OrderRepository, ...)"));

    @Test
    @DisplayName("a full verdict carries kind, confidence, basis, evidences, proof and remediations")
    void fullVerdictCarriesEverything() {
        Evidence evidence = Evidence.of(
                EvidenceTier.FRAMEWORK_KNOWLEDGE,
                Confidence.HIGH,
                "SPRING_DATA_REPOSITORY(Order)",
                "extends JpaRepository<Order, OrderId>");
        Classification classification = Classification.builder(
                        ArchKind.AGGREGATE_ROOT, Confidence.HIGH, Basis.INFERRED, PROOF)
                .evidences(List.of(evidence))
                .remediations(List.of(RemediationHint.addAnnotation(
                        TypeId.of("org.jmolecules.ddd.annotation.AggregateRoot"), ArchKind.AGGREGATE_ROOT)))
                .build();

        assertThat(classification.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
        assertThat(classification.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(classification.basis()).isEqualTo(Basis.INFERRED);
        assertThat(classification.evidences()).hasSize(1);
        assertThat(classification.proof().rule()).contains(RuleId.of("R1"));
        assertThat(classification.remediations()).hasSize(1);
        assertThat(classification.isAmbiguous()).isFalse();
        assertThat(classification.direction()).isEmpty();
    }

    @Test
    @DisplayName("a port verdict carries its direction")
    void portVerdictCarriesDirection() {
        Classification port = Classification.builder(ArchKind.DRIVEN_PORT, Confidence.HIGH, Basis.INFERRED, PROOF)
                .direction(PortDirection.DRIVEN)
                .build();

        assertThat(port.direction()).contains(PortDirection.DRIVEN);
    }

    @Test
    @DisplayName("an adapter verdict carries its direction too")
    void adapterVerdictCarriesDirection() {
        Classification adapter = Classification.builder(
                        ArchKind.DRIVING_ADAPTER, Confidence.HIGH, Basis.INFERRED, PROOF)
                .direction(PortDirection.DRIVING)
                .build();

        assertThat(adapter.direction()).contains(PortDirection.DRIVING);
    }

    @Test
    @DisplayName("a direction on a kind that sits on no hexagon boundary is rejected")
    void directionOnNonPortKindIsRejected() {
        Classification.Builder builder = Classification.builder(
                        ArchKind.VALUE_OBJECT, Confidence.HIGH, Basis.INFERRED, PROOF)
                .direction(PortDirection.DRIVING);

        assertThatIllegalArgumentException().isThrownBy(builder::build).withMessageContaining("port");
    }

    @Test
    @DisplayName("an ambiguous verdict keeps its ordered candidates")
    void ambiguousVerdictKeepsCandidates() {
        Evidence structural =
                Evidence.of(EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, "HAS_IDENTITY(id)", "has an identity field");
        Classification ambiguous = Classification.builder(
                        ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED, ProofNode.fact("insufficient margin"))
                .candidates(List.of(
                        new Candidate(ArchKind.ENTITY, 12, List.of(structural)),
                        new Candidate(ArchKind.VALUE_OBJECT, 11, List.of())))
                .build();

        assertThat(ambiguous.isAmbiguous()).isTrue();
        assertThat(ambiguous.candidates())
                .extracting(Candidate::kind)
                .containsExactly(ArchKind.ENTITY, ArchKind.VALUE_OBJECT);
    }

    @Test
    @DisplayName("a negative candidate score is rejected")
    void negativeCandidateScoreIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Candidate(ArchKind.ENTITY, -1, List.of()));
    }

    @Test
    @DisplayName("remediation factories produce explicit impacts")
    void remediationFactoriesProduceExplicitImpacts() {
        RemediationHint annotation = RemediationHint.addAnnotation(
                TypeId.of("org.jmolecules.ddd.annotation.ValueObject"), ArchKind.VALUE_OBJECT);
        RemediationHint configuration =
                RemediationHint.configureExplicit(TypeId.of("com.a.Money"), ArchKind.VALUE_OBJECT);

        assertThat(annotation.action()).isEqualTo(RemediationAction.ADD_ANNOTATION);
        assertThat(annotation.impact().resultingConfidence()).isEqualTo(Confidence.EXPLICIT);
        assertThat(configuration.action()).isEqualTo(RemediationAction.CONFIGURE_EXPLICIT);
        assertThat(configuration.description()).contains("com.a.Money");
        assertThat(RemediationImpact.improved(ArchKind.ENTITY, Confidence.HIGH).resultingConfidence())
                .isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("an annotation hint names the annotation in full and pastes with its import")
    void annotationHintNamesTheAnnotationInFull() {
        RemediationHint hint = RemediationHint.addAnnotation(
                TypeId.of("org.jmolecules.ddd.annotation.AggregateRoot"), ArchKind.AGGREGATE_ROOT);

        assertThat(hint.description()).contains("org.jmolecules.ddd.annotation.AggregateRoot");
        assertThat(hint.codeSnippet()).isPresent();
        assertThat(hint.codeSnippet().orElseThrow())
                .contains("import org.jmolecules.ddd.annotation.AggregateRoot;")
                .contains("@AggregateRoot");
    }

    @Test
    @DisplayName("a nested annotation is imported by its source name")
    void nestedAnnotationIsImportedBySourceName() {
        RemediationHint hint =
                RemediationHint.addAnnotation(TypeId.of("com.acme.Intents$Aggregate"), ArchKind.AGGREGATE_ROOT);

        assertThat(hint.codeSnippet().orElseThrow())
                .contains("import com.acme.Intents.Aggregate;")
                .contains("@Aggregate");
    }

    @Test
    @DisplayName("an annotation named without its package is rejected")
    void unqualifiedAnnotationIsRejected() {
        TypeId unqualified = TypeId.of("Entity");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> RemediationHint.addAnnotation(unqualified, ArchKind.ENTITY))
                .withMessageContaining("qualified");
    }
}
