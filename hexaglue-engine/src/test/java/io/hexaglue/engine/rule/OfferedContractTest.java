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
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OfferedContractTest {

    private static final TypeId CONTRACT = TypeId.of("com.acme.Boarding");
    private static final TypeId CORE = TypeId.of("com.acme.Assembly");
    private static final TypeId OTHER = TypeId.of("com.acme.Ferrying");
    private static final TypeId ENTRY_POINT = TypeId.of("com.acme.HangarDoor");
    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";

    private static TypeNode contract() {
        return TypeNode.builder(CONTRACT, TypeNature.INTERFACE).build();
    }

    /** A type of the core writing the contract, and nothing else. */
    private static TypeNode core() {
        return TypeNode.builder(CORE, TypeNature.CLASS)
                .interfaces(List.of(TypeRef.of(CONTRACT.qualifiedName())))
                .build();
    }

    /** Another type of the core, keeping whatever it was handed. */
    private static TypeNode holder(TypeId held) {
        return TypeNode.builder(OTHER, TypeNature.CLASS)
                .fields(List.of(Field.of("target", TypeRef.of(held.qualifiedName()))))
                .build();
    }

    private static TypeNode entryPoint() {
        return TypeNode.builder(ENTRY_POINT, TypeNature.CLASS)
                .annotations(List.of(Annotation.of(REST_CONTROLLER)))
                .fields(List.of(Field.of("target", TypeRef.of(CONTRACT.qualifiedName()))))
                .build();
    }

    private static Verdicts verdicts(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        return Classifier.classify(
                EngineContext.of(code.build(), KnowledgePacks.embedded(), HexaGlueConfig.defaults()));
    }

    @Nested
    @DisplayName("reads a contract the core writes and nobody inside takes")
    class ReadsAContractNobodyInsideTakes {

        @Test
        @DisplayName("as the way in it can only be, before any adapter exists to prove it")
        void theWayInItCanOnlyBe() {
            // The hexagon whose web layer has not been written yet: the one case where a
            // generator has the most to offer, and the one the ring-side reading cannot see.
            Classification verdict =
                    verdicts(contract(), core()).verdict(CONTRACT).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVING_PORT);
            assertThat(verdict.direction()).contains(PortDirection.DRIVING);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("at a lower tier than the ring calling it, so a ring that speaks is heard first")
        void atALowerTierThanTheRingCallingIt() {
            Classification verdict =
                    verdicts(contract(), core()).verdict(CONTRACT).orElseThrow();

            assertThat(verdict.evidences()).allMatch(evidence -> evidence.tier() == EvidenceTier.LOCAL_STRUCTURE);
        }

        @Test
        @DisplayName("and steps aside once an entry point states the same thing more strongly")
        void andStepsAsideOnceAnEntryPointSaysIt() {
            // Both readings agree, so the verdict never turned on which one spoke; what is owed
            // here is that the stronger one is on the record for a reader asking how it is known.
            Classification verdict =
                    verdicts(contract(), core(), entryPoint()).verdict(CONTRACT).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVING_PORT);
            assertThat(verdict.evidences()).anyMatch(evidence -> evidence.tier() == EvidenceTier.GRAPH_RELATION);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a contract another type of the core keeps, which is a seam and not a way in")
        void aboutAContractTheCoreItselfKeeps() {
            assertThat(verdicts(contract(), core(), holder(CONTRACT)).kindOf(CONTRACT))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a contract nothing inside writes, which is a way out or nothing at all")
        void aboutAContractNobodyInsideWrites() {
            assertThat(verdicts(contract()).kindOf(CONTRACT)).contains(ArchKind.UNCLASSIFIED);
        }
    }
}
