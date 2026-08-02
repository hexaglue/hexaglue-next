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
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdapterCollaboratorTest {

    private static final TypeId WAY_OUT = TypeId.of("com.acme.Ledger");
    private static final TypeId REACHED = TypeId.of("com.acme.Checkout");
    private static final TypeId ENTRY_POINT = TypeId.of("com.acme.HangarDoor");
    private static final TypeId STORE = TypeId.of("com.acme.HangarBooks");
    private static final String SECONDARY_PORT = "org.jmolecules.architecture.hexagonal.SecondaryPort";
    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    private static final String SERVICE = "org.springframework.stereotype.Service";
    private static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";
    private static final String JPA_ENTITY = "jakarta.persistence.Entity";
    private static final TypeId NEIGHBOUR = TypeId.of("com.acme.Manifest");

    private static TypeNode wayOut() {
        return TypeNode.builder(WAY_OUT, TypeNature.INTERFACE)
                .annotations(List.of(Annotation.of(SECONDARY_PORT)))
                .build();
    }

    /** A type the framework calls, keeping the one it delegates to. */
    private static TypeNode entryPoint() {
        return holder(ENTRY_POINT, REACHED, REST_CONTROLLER);
    }

    /** A type the outer ring owns on the other side, keeping the same collaborator. */
    private static TypeNode store() {
        return TypeNode.builder(STORE, TypeNature.CLASS)
                .fields(List.of(
                        Field.of("store", TypeRef.of(ENTITY_MANAGER)),
                        Field.of("collaborator", TypeRef.of(REACHED.qualifiedName()))))
                .build();
    }

    private static TypeNode holder(TypeId id, TypeId held, String... annotations) {
        return TypeNode.builder(id, TypeNature.CLASS)
                .annotations(List.of(annotations).stream().map(Annotation::of).toList())
                .fields(List.of(Field.of("collaborator", TypeRef.of(held.qualifiedName()))))
                .build();
    }

    private static TypeNode reached(String... annotations) {
        return TypeNode.builder(REACHED, TypeNature.CLASS)
                .annotations(List.of(annotations).stream().map(Annotation::of).toList())
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

    /** The reached type, keeping the way out as well as being kept by the entry point. */
    private static TypeNode reachedCallingTheWayOut(String... annotations) {
        List<Annotation> carried = new ArrayList<>();
        for (String annotation : annotations) {
            carried.add(Annotation.of(annotation));
        }
        return TypeNode.builder(REACHED, TypeNature.CLASS)
                .annotations(carried)
                .fields(List.of(Field.of("ledger", TypeRef.of(WAY_OUT.qualifiedName()))))
                .build();
    }

    @Nested
    @DisplayName("reads the type an entry point delegates to")
    class ReadsTheTypeAnEntryPointDelegatesTo {

        @Test
        @DisplayName("as the application service it is, when it calls a way out of its own")
        void whenItCallsAWayOutOfItsOwn() {
            // Both sides agree: the ring reaches it from outside and it reaches storage from
            // inside. Each is stated on its own, so a report can show either half.
            Classification verdict = verdicts(wayOut(), reachedCallingTheWayOut(), entryPoint())
                    .verdict(REACHED)
                    .orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.APPLICATION_SERVICE);
            assertThat(verdict.evidences()).hasSize(2);
        }

        @Test
        @DisplayName("as the application service it is, when the framework's own word completes the case")
        void whenTheFrameworksWordCompletesTheCase() {
            // A stereotype decides nothing on its own; between an entry point and the rest of the
            // application it is the last thing needed, and no more than that.
            Classification verdict =
                    verdicts(reached(SERVICE), entryPoint()).verdict(REACHED).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.APPLICATION_SERVICE);
            assertThat(verdict.evidences().get(0).justification()).contains("HangarDoor", SERVICE);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a stereotype nobody reaches for, which is a habit and not a position")
        void aboutAStereotypeNobodyReachesFor() {
            assertThat(verdicts(reached(SERVICE)).kindOf(REACHED)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a type only the far side of the ring keeps, which is reached outward")
        void aboutATypeOnlyTheFarSideKeeps() {
            assertThat(verdicts(reached(SERVICE), store()).kindOf(REACHED)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a type an entry point keeps that reaches nothing outside")
        void aboutATypeThatReachesNothingOutside() {
            // It has a collaborator of its own, and that collaborator is no way out: what the
            // rule looks for is the reach, not the fact of holding something.
            TypeNode neighbour = TypeNode.builder(NEIGHBOUR, TypeNature.CLASS).build();

            assertThat(verdicts(holder(REACHED, NEIGHBOUR), neighbour, entryPoint())
                            .kindOf(REACHED))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a type an entry point keeps whose only annotation is about persistence")
        void aboutATypeAnnotatedForPersistence() {
            // A framework symbol is not a stereotype because a pack knows it: only the entries
            // that speak for application work complete the case, and a persistence annotation
            // never speaks for a kind at all.
            assertThat(verdicts(reached(JPA_ENTITY), entryPoint()).kindOf(REACHED))
                    .contains(ArchKind.UNCLASSIFIED);
        }
    }
}
