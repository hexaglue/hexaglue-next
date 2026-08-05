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
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.CodeModelCapability;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExposedContractTest {

    private static final TypeId CONTRACT = TypeId.of("com.acme.Boarding");
    private static final TypeId CORE = TypeId.of("com.acme.Assembly");
    private static final TypeId ENTRY_POINT = TypeId.of("com.acme.HangarDoor");
    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";

    private static TypeNode contract() {
        return TypeNode.builder(CONTRACT, TypeNature.INTERFACE).build();
    }

    /** A type of the core writing the contract the outside will reach it through. */
    private static TypeNode core() {
        return TypeNode.builder(CORE, TypeNature.CLASS)
                .interfaces(List.of(TypeRef.of(CONTRACT.qualifiedName())))
                .build();
    }

    /** The entry point the framework calls, keeping whatever it was handed. */
    private static TypeNode entryPoint(TypeId held) {
        return TypeNode.builder(ENTRY_POINT, TypeNature.CLASS)
                .annotations(List.of(Annotation.of(REST_CONTROLLER)))
                .fields(List.of(Field.of("target", TypeRef.of(held.qualifiedName()))))
                .build();
    }

    private static Verdicts verdicts(CodeModel code) {
        return Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()));
    }

    private static CodeModel model(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        return code.build();
    }

    @Nested
    @DisplayName("reads an interface the core writes and the outer ring calls")
    class ReadsAnInterfaceTheCoreWrites {

        @Test
        @DisplayName("as the driving port it is, on the structure of the declarations alone")
        void aContractTheRingCallsIsADrivingPort() {
            // The field edge is what a real model carries for a held collaborator, and it must not
            // be mistaken for the call: only one reading is owed here, the structural one.
            CodeModel code = CodeModel.builder()
                    .addType(contract())
                    .addType(core())
                    .addType(entryPoint(CONTRACT))
                    .addEdge(Edge.of(ENTRY_POINT, EdgeKind.FIELD_TYPE, CONTRACT))
                    .build();

            Classification verdict = verdicts(code).verdict(CONTRACT).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVING_PORT);
            assertThat(verdict.direction()).contains(PortDirection.DRIVING);
            assertThat(verdict.evidences()).hasSize(1);
            assertThat(verdict.evidences()).allMatch(evidence -> evidence.tier() == EvidenceTier.GRAPH_RELATION);
        }

        @Test
        @DisplayName("and says so twice when the bodies were read and the call is there to see")
        void andSaysSoTwiceWhenTheCallIsThereToSee() {
            // The invocation adds to the structural reading, it does not condition it: the same
            // port is reached with or without the capability, and the extra evidence is what a
            // report shows a reader who asks how the engine knows.
            CodeModel code = CodeModel.builder()
                    .addType(contract())
                    .addType(core())
                    .addType(entryPoint(CONTRACT))
                    .capability(CodeModelCapability.METHOD_BODIES)
                    .addEdge(new Edge(
                            ENTRY_POINT,
                            CONTRACT,
                            EdgeKind.INVOKES,
                            Optional.of("board"),
                            OptionalInt.empty(),
                            OptionalInt.empty()))
                    .build();

            Classification verdict = verdicts(code).verdict(CONTRACT).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVING_PORT);
            assertThat(verdict.evidences()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a contract of the core no entry point reaches for")
        void aboutAContractNoEntryPointReachesFor() {
            // The verdict is not the subject here: another rule reads that contract from the
            // absence of a holder. What is owed is that this one, which reads a caller, stays
            // quiet when there is no caller to read.
            assertThat(verdicts(model(contract(), core()))
                            .verdict(CONTRACT)
                            .orElseThrow()
                            .evidences())
                    .noneMatch(evidence -> evidence.tier() == EvidenceTier.GRAPH_RELATION);
        }

        @Test
        @DisplayName("about a contract the entry point holds but nobody inside writes")
        void aboutAContractNobodyInsideWrites() {
            assertThat(verdicts(model(contract(), entryPoint(CONTRACT))).kindOf(CONTRACT))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a contract the entry point bypasses by holding the implementation itself")
        void aboutAContractTheEntryPointBypasses() {
            // Reaching past the port is exactly what a port exists to prevent, and this rule
            // states what it sees: no port was used, so it reads none. That the contract is a
            // way in all the same is another rule's reading, and naming the shortcut is the
            // conformity question, answered elsewhere.
            assertThat(verdicts(model(contract(), core(), entryPoint(CORE)))
                            .verdict(CONTRACT)
                            .orElseThrow()
                            .evidences())
                    .noneMatch(evidence -> evidence.tier() == EvidenceTier.GRAPH_RELATION);
        }
    }
}
