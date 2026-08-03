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
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RemediationHint;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.ValidationConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The gates are a policy of the engine, not a plugin: they produce a verdict and nothing else. Each
 * one is armed on its own, so what a type fails is reported gate by gate — turning one off never
 * changes what the others say.
 */
class ValidationTest {

    private static TypeStructure structure(TypeId id) {
        CodeModel code = CodeModel.builder()
                .addType(TypeNode.builder(id, TypeNature.CLASS).build())
                .build();
        return Structures.of(code).of(code.type(id).orElseThrow());
    }

    private static Classification.Builder verdict(ArchKind kind, Confidence confidence, Basis basis) {
        return Classification.builder(kind, confidence, basis, ProofNode.fact("because " + kind));
    }

    private static ArchType value(String name, Confidence confidence, Basis basis) {
        TypeId id = TypeId.of(name);
        return new ValueObject(id, structure(id), verdict(ArchKind.VALUE_OBJECT, confidence, basis).build());
    }

    private static ArchType port(String name, Confidence confidence) {
        TypeId id = TypeId.of(name);
        return new DrivenPort(
                id,
                structure(id),
                verdict(ArchKind.DRIVEN_PORT, confidence, Basis.INFERRED)
                        .direction(PortDirection.DRIVEN)
                        .build(),
                DrivenPortType.REPOSITORY,
                Optional.empty());
    }

    private static ArchType silent(String name, List<RemediationHint> remediations) {
        TypeId id = TypeId.of(name);
        return new UnclassifiedType(
                id,
                structure(id),
                verdict(ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED)
                        .remediations(remediations)
                        .build(),
                UnclassifiedCategory.UNKNOWN,
                Optional.of("nothing in the sources says what it is"));
    }

    private static ArchType tied(String name) {
        TypeId id = TypeId.of(name);
        Evidence shape =
                Evidence.of(EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, "IMMUTABLE(" + name + ")", "it never changes");
        return new UnclassifiedType(
                id,
                structure(id),
                verdict(ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED)
                        .candidates(List.of(
                                new Candidate(ArchKind.ENTITY, 1000, List.of(shape)),
                                new Candidate(ArchKind.VALUE_OBJECT, 1000, List.of(shape))))
                        .build(),
                UnclassifiedCategory.AMBIGUOUS,
                Optional.empty());
    }

    private static ArchModel model(ArchType... types) {
        ArchModel.Builder builder = ArchModel.builder();
        for (ArchType type : types) {
            builder.addType(type);
        }
        return builder.build();
    }

    private static List<String> subjectsOf(Validation validation) {
        return validation.refusals().stream()
                .map(refusal -> refusal.subject().id().qualifiedName())
                .toList();
    }

    @Nested
    @DisplayName("with nothing armed")
    class Permissive {

        @Test
        @DisplayName("accepts a model nothing could be said about")
        void acceptsWhateverWhenNoGateIsArmed() {
            ArchModel model = model(silent("com.acme.Thing", List.of()), tied("com.acme.Tied"));

            Validation validation = Validation.of(model, ValidationConfig.defaults());

            assertThat(validation.passed()).isTrue();
            assertThat(validation.refusals()).isEmpty();
        }
    }

    @Nested
    @DisplayName("gate by gate")
    class OneAtATime {

        @Test
        @DisplayName("refuses every type that reached no kind, and says which")
        void refusesUnclassified() {
            ArchModel model =
                    model(value("com.acme.Money", Confidence.HIGH, Basis.INFERRED), silent("com.acme.Thing", List.of()));

            Validation validation = Validation.of(
                    model, ValidationConfig.builder().failOnUnclassified(true).build());

            assertThat(validation.passed()).isFalse();
            assertThat(subjectsOf(validation)).containsExactly("com.acme.Thing");
            assertThat(validation.refusals().get(0).gate()).isEqualTo(Gate.UNCLASSIFIED);
        }

        @Test
        @DisplayName("refuses a verdict below the confidence floor")
        void refusesBelowTheConfidenceFloor() {
            ArchModel model = model(
                    value("com.acme.Money", Confidence.HIGH, Basis.INFERRED),
                    value("com.acme.Guess", Confidence.MEDIUM, Basis.INFERRED));

            Validation validation = Validation.of(
                    model, ValidationConfig.builder().minConfidence(Confidence.HIGH).build());

            assertThat(subjectsOf(validation)).containsExactly("com.acme.Guess");
            assertThat(validation.refusals().get(0).reason()).contains("MEDIUM").contains("HIGH");
        }

        @Test
        @DisplayName("holds ports to the confidence floor like everything else")
        void holdsPortsToTheFloor() {
            ArchModel model = model(port("com.acme.Orders", Confidence.MEDIUM));

            Validation validation = Validation.of(
                    model, ValidationConfig.builder().minConfidence(Confidence.HIGH).build());

            assertThat(subjectsOf(validation)).containsExactly("com.acme.Orders");
        }

        @Test
        @DisplayName("refuses a decision that kept candidates")
        void refusesAmbiguous() {
            ArchModel model = model(tied("com.acme.Tied"), value("com.acme.Money", Confidence.HIGH, Basis.INFERRED));

            Validation validation =
                    Validation.of(model, ValidationConfig.builder().failOnAmbiguous(true).build());

            assertThat(subjectsOf(validation)).containsExactly("com.acme.Tied");
            assertThat(validation.refusals().get(0).gate()).isEqualTo(Gate.AMBIGUOUS);
        }

        @Test
        @DisplayName("refuses a deduced kind when the sources must state every one of them")
        void refusesInferredWhenDeclarationIsRequired() {
            ArchModel model = model(
                    value("com.acme.Declared", Confidence.EXPLICIT, Basis.DECLARED),
                    value("com.acme.Deduced", Confidence.HIGH, Basis.INFERRED));

            Validation validation =
                    Validation.of(model, ValidationConfig.builder().allowInferred(false).build());

            assertThat(subjectsOf(validation)).containsExactly("com.acme.Deduced");
            assertThat(validation.refusals().get(0).gate()).isEqualTo(Gate.INFERRED);
        }
    }

    @Nested
    @DisplayName("with several gates armed")
    class Together {

        @Test
        @DisplayName("reports a type once per gate it fails, in a stable order")
        void reportsOneRefusalPerGate() {
            ArchModel model = model(tied("com.acme.Tied"));

            Validation validation = Validation.of(
                    model,
                    ValidationConfig.builder()
                            .failOnUnclassified(true)
                            .failOnAmbiguous(true)
                            .minConfidence(Confidence.HIGH)
                            .build());

            assertThat(validation.refusals())
                    .extracting(Validation.Refusal::gate)
                    .containsExactly(Gate.UNCLASSIFIED, Gate.CONFIDENCE, Gate.AMBIGUOUS);
        }

        @Test
        @DisplayName("carries the remediation of the type it refused, not a generic suggestion")
        void carriesTheRemediationOfTheType() {
            RemediationHint hint = RemediationHint.configureExplicit(TypeId.of("com.acme.Thing"), ArchKind.ENTITY);
            ArchModel model = model(silent("com.acme.Thing", List.of(hint)));

            Validation validation = Validation.of(
                    model, ValidationConfig.builder().failOnUnclassified(true).build());

            assertThat(validation.refusals().get(0).subject().classification().remediations())
                    .containsExactly(hint);
        }
    }
}
