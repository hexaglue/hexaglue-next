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

package io.hexaglue.engine.rule;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.engine.Classifier;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.Verdicts;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What the outer ring hands the boundary, and what the boundary hands back.
 *
 * <p>These fixtures pin no single rule but the order the readings arrive in — and, just as much,
 * the reading that has to disappear once a later round knows better.</p>
 */
class WavePropagationTest {

    private static final TypeId CONTRACT = TypeId.of("com.acme.Ledger");
    private static final TypeId RING = TypeId.of("com.acme.HangarBooks");
    private static final TypeId CORE = TypeId.of("com.acme.Checkout");
    private static final TypeId SUBJECT = TypeId.of("com.acme.Fleet");
    private static final TypeId TAG = TypeId.of("com.acme.FleetTag");
    private static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";
    private static final TypeRef CONTRACT_REF = TypeRef.of(CONTRACT.qualifiedName());
    private static final TypeRef SUBJECT_REF = TypeRef.of(SUBJECT.qualifiedName());
    private static final TypeRef TAG_REF = TypeRef.of(TAG.qualifiedName());
    private static final TypeRef TEXT = TypeRef.of("java.lang.String");

    private static TypeNode contract() {
        return TypeNode.builder(CONTRACT, TypeNature.INTERFACE).build();
    }

    /** The same contract with the signatures of storage: it takes the subject and answers with it. */
    private static TypeNode storage() {
        return TypeNode.builder(CONTRACT, TypeNature.INTERFACE)
                .methods(List.of(
                        Method.builder("locate", TypeRef.parameterized("java.util.Optional", SUBJECT_REF))
                                .parameters(List.of(Parameter.of("key", TAG_REF)))
                                .build(),
                        Method.builder("keep", TypeRef.of("void"))
                                .parameters(List.of(Parameter.of("subject", SUBJECT_REF)))
                                .build()))
                .build();
    }

    /** State, and none of it can change: on its own the shape reads this as a value. */
    private static TypeNode subject() {
        return TypeNode.builder(SUBJECT, TypeNature.CLASS)
                .fields(List.of(finalField("tag", TAG_REF), finalField("reference", TEXT)))
                .build();
    }

    /** The same subject, pointing at another of its own kind the way a tree does. */
    private static TypeNode subjectKeepingItsOwnKind() {
        return TypeNode.builder(SUBJECT, TypeNature.CLASS)
                .fields(List.of(finalField("tag", TAG_REF), finalField("next", SUBJECT_REF)))
                .build();
    }

    private static TypeNode tag() {
        return TypeNode.builder(TAG, TypeNature.RECORD)
                .fields(List.of(finalField("value", TEXT)))
                .build();
    }

    private static Field finalField(String name, TypeRef type) {
        return Field.builder(name, type).modifiers(Set.of(Modifier.FINAL)).build();
    }

    /** Holds a way out — so the first wave places it — and fulfils the contract. */
    private static TypeNode ringFulfillingTheContract() {
        return TypeNode.builder(RING, TypeNature.CLASS)
                .interfaces(List.of(CONTRACT_REF))
                .fields(List.of(Field.of("store", TypeRef.of(ENTITY_MANAGER))))
                .build();
    }

    /** Holds a way out and the contract, so it is the contract's only holder — and no core. */
    private static TypeNode ringHoldingTheContract() {
        return TypeNode.builder(RING, TypeNature.CLASS)
                .fields(List.of(Field.of("store", TypeRef.of(ENTITY_MANAGER)), Field.of("collaborator", CONTRACT_REF)))
                .build();
    }

    private static TypeNode coreHoldingTheContract() {
        return TypeNode.builder(CORE, TypeNature.CLASS)
                .fields(List.of(Field.of("collaborator", CONTRACT_REF)))
                .build();
    }

    private static Verdicts verdicts(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        code.addType(TypeNode.externalStub(TypeId.of(ENTITY_MANAGER), TypeNature.INTERFACE));
        return Classifier.classify(
                EngineContext.of(code.build(), KnowledgePacks.embedded(), HexaGlueConfig.defaults()));
    }

    @Nested
    @DisplayName("carries a reading from one wave to the next")
    class CarriesAReadingFromOneWaveToTheNext {

        @Test
        @DisplayName("the ring first, then the contract it fulfils, then the ring again for the reason")
        void theRingThenTheContractThenTheRingAgain() {
            // Round one places the adapter on the tool it holds. Only then does the contract stop
            // counting as implemented from the inside, which is what lets the next round read it
            // as a way out — and the round after hands that back to the adapter as a second reason
            // for standing where it stands.
            Verdicts settled = verdicts(contract(), ringFulfillingTheContract(), coreHoldingTheContract());

            assertThat(settled.kindOf(CONTRACT)).contains(ArchKind.DRIVEN_PORT);

            Classification onTheRing = settled.verdict(RING).orElseThrow();
            assertThat(onTheRing.kind()).isEqualTo(ArchKind.DRIVEN_ADAPTER);
            assertThat(onTheRing.evidences())
                    .extracting(Evidence::tier)
                    .containsExactlyInAnyOrder(EvidenceTier.FRAMEWORK_KNOWLEDGE, EvidenceTier.GRAPH_RELATION);
        }

        @Test
        @DisplayName("and on to the layer that calls the contract, once the contract is a way out")
        void andOnToTheLayerThatCallsTheContract() {
            // The third hop of the same chain: a tool places the ring, the ring frees the contract
            // to be read as a port, and the port makes its caller the application layer. Not one
            // of the three readings could have been made on the round before it.
            Verdicts settled = verdicts(contract(), ringFulfillingTheContract(), coreHoldingTheContract());

            assertThat(settled.kindOf(CORE)).contains(ArchKind.APPLICATION_SERVICE);
        }
    }

    @Nested
    @DisplayName("carries the reading on into the domain")
    class CarriesTheReadingOnIntoTheDomain {

        @Test
        @DisplayName("the ring, the way out, the layer between, then what the way out keeps")
        void theRingTheWayOutTheLayerThenWhatItKeeps() {
            // The whole chain on one fixture: a tool places the adapter, the adapter frees the
            // contract to read as a way out, the way out makes its caller the application layer and
            // its subject the aggregate, and the key that subject is searched by is its identity.
            Verdicts settled =
                    verdicts(storage(), ringFulfillingTheContract(), coreHoldingTheContract(), subject(), tag());

            assertThat(settled.kindOf(RING)).contains(ArchKind.DRIVEN_ADAPTER);
            assertThat(settled.kindOf(CONTRACT)).contains(ArchKind.DRIVEN_PORT);
            assertThat(settled.kindOf(CORE)).contains(ArchKind.APPLICATION_SERVICE);
            assertThat(settled.kindOf(SUBJECT)).contains(ArchKind.AGGREGATE_ROOT);
            assertThat(settled.kindOf(TAG)).contains(ArchKind.IDENTIFIER);
        }

        @Test
        @DisplayName("without turning back on the aggregate, which is not made of itself")
        void withoutTurningBackOnTheAggregate() {
            // A tree-shaped aggregate points at another of its own kind, and reading that field as
            // composition would have the aggregate be one of its own parts — a second reading of
            // the same declaration, as strong as the first, and the verdict would collapse.
            Verdicts settled = verdicts(
                    storage(),
                    ringFulfillingTheContract(),
                    coreHoldingTheContract(),
                    subjectKeepingItsOwnKind(),
                    tag());

            assertThat(settled.kindOf(SUBJECT)).contains(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("replacing the reading the shape had made, once a way out knows better")
        void replacingTheReadingTheShapeHadMade() {
            // On its own the subject is a value: state, and none of it can change. The round that
            // reads the way out storing it says something stronger about the same declaration, and
            // the earlier reading has to give way rather than sit beside it.
            Verdicts alone = verdicts(subject(), tag());
            assertThat(alone.kindOf(SUBJECT)).contains(ArchKind.VALUE_OBJECT);

            Verdicts settled =
                    verdicts(storage(), ringFulfillingTheContract(), coreHoldingTheContract(), subject(), tag());

            assertThat(settled.kindOf(SUBJECT)).contains(ArchKind.AGGREGATE_ROOT);
        }
    }

    @Nested
    @DisplayName("withdraws a reading a later round contradicts")
    class WithdrawsAReadingALaterRoundContradicts {

        @Test
        @DisplayName("the contract read as a way out while its only holder was still unplaced")
        void theContractReadAsAWayOutTooEarly() {
            // Nothing is decided on the first round, so the holder counts as core and the contract
            // reads as a port. The round after, the holder is on the ring and the reading has no
            // ground left: it must vanish rather than sit next to the truer one.
            Verdicts settled = verdicts(contract(), ringHoldingTheContract());

            assertThat(settled.kindOf(RING)).contains(ArchKind.DRIVEN_ADAPTER);
            assertThat(settled.kindOf(CONTRACT)).contains(ArchKind.UNCLASSIFIED);
        }
    }
}
