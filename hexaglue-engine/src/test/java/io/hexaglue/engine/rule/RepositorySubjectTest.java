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
import io.hexaglue.engine.FactBase;
import io.hexaglue.engine.Relation;
import io.hexaglue.engine.RuleSet;
import io.hexaglue.engine.Saturation;
import io.hexaglue.engine.Verdicts;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RepositorySubjectTest {

    private static final TypeId REPOSITORY = TypeId.of("com.acme.OrderRepository");
    private static final TypeId ORDER = TypeId.of("com.acme.Order");
    private static final TypeId ORDER_ID = TypeId.of("com.acme.OrderId");
    private static final TypeId UUID = TypeId.of("java.util.UUID");
    private static final String SPRING_REPOSITORY = "org.springframework.data.repository.Repository";
    private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";

    /** A repository declared over the given arguments, above the closure the frontend computes. */
    private static CodeModel sources(TypeRef... arguments) {
        TypeNode repository = TypeNode.builder(REPOSITORY, TypeNature.INTERFACE)
                .interfaces(List.of(TypeRef.parameterized(JPA_REPOSITORY, arguments)))
                .build();
        return CodeModel.builder()
                .addType(repository)
                .addType(TypeNode.builder(ORDER, TypeNature.CLASS).build())
                .addType(TypeNode.builder(ORDER_ID, TypeNature.RECORD).build())
                .addType(TypeNode.externalStub(TypeId.of(JPA_REPOSITORY), TypeNature.INTERFACE))
                .addType(TypeNode.externalStub(TypeId.of(SPRING_REPOSITORY), TypeNature.INTERFACE))
                .supertypes(REPOSITORY, List.of(TypeId.of(JPA_REPOSITORY), TypeId.of(SPRING_REPOSITORY)))
                .supertypes(TypeId.of(JPA_REPOSITORY), List.of(TypeId.of(SPRING_REPOSITORY)))
                .build();
    }

    private static EngineContext context(CodeModel code) {
        return EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults());
    }

    private static Verdicts classify(TypeRef... arguments) {
        return Classifier.classify(context(sources(arguments)));
    }

    private static List<Relation> relations(TypeRef... arguments) {
        FactBase facts = Saturation.saturate(RuleSet.standard(), context(sources(arguments)));
        return facts.all(Relation.class);
    }

    @Nested
    @DisplayName("reads a repository declaration")
    class ReadsARepositoryDeclaration {

        @Test
        @DisplayName("as the driven port it is")
        void theDrivenPortItIs() {
            Classification verdict = classify(TypeRef.of(ORDER.qualifiedName()), TypeRef.of(ORDER_ID.qualifiedName()))
                    .verdict(REPOSITORY)
                    .orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVEN_PORT);
            assertThat(verdict.direction()).contains(PortDirection.DRIVEN);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
            assertThat(verdict.basis()).isEqualTo(Basis.INFERRED);
        }

        @Test
        @DisplayName("as the aggregate the first argument names")
        void theAggregateTheFirstArgumentNames() {
            assertThat(classify(TypeRef.of(ORDER.qualifiedName()), TypeRef.of(ORDER_ID.qualifiedName()))
                            .kindOf(ORDER))
                    .contains(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("as the identifier the second argument names, with no suffix involved")
        void theIdentifierTheSecondArgumentNames() {
            assertThat(classify(TypeRef.of(ORDER.qualifiedName()), TypeRef.of(ORDER_ID.qualifiedName()))
                            .kindOf(ORDER_ID))
                    .contains(ArchKind.IDENTIFIER);
        }

        @Test
        @DisplayName("through a vendor interface the packs never named")
        void throughAVendorInterfaceThePacksNeverNamed() {
            // The packs state Repository; JpaRepository is reached through the closure, and the
            // arguments are read off the declaration that was actually written.
            assertThat(classify(TypeRef.of(ORDER.qualifiedName()), TypeRef.of(ORDER_ID.qualifiedName()))
                            .kindOf(ORDER))
                    .contains(ArchKind.AGGREGATE_ROOT);
        }
    }

    @Nested
    @DisplayName("ties the types together")
    class TiesTheTypesTogether {

        @Test
        @DisplayName("naming the aggregate the port manages and what identifies it")
        void namingWhatThePortManagesAndWhatIdentifiesIt() {
            assertThat(relations(TypeRef.of(ORDER.qualifiedName()), TypeRef.of(ORDER_ID.qualifiedName())))
                    .extracting(Relation::render)
                    .containsExactlyInAnyOrder(
                            "MANAGES(com.acme.OrderRepository, com.acme.Order)",
                            "IDENTIFIED_BY(com.acme.Order, com.acme.OrderId)");
        }

        @Test
        @DisplayName("even when the identity is a type of the JDK, which gets no verdict of its own")
        void evenWhenTheIdentityIsAJdkType() {
            List<Relation> relations = relations(TypeRef.of(ORDER.qualifiedName()), TypeRef.of(UUID.qualifiedName()));

            assertThat(relations)
                    .extracting(Relation::render)
                    .contains("IDENTIFIED_BY(com.acme.Order, java.util.UUID)");
            assertThat(classify(TypeRef.of(ORDER.qualifiedName()), TypeRef.of(UUID.qualifiedName()))
                            .verdict(UUID))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about an argument that is a type variable, which names no type at all")
        void aboutATypeVariable() {
            assertThat(relations(TypeRef.typeVariable("T"), TypeRef.of(ORDER_ID.qualifiedName())))
                    .isEmpty();
        }

        @Test
        @DisplayName("about the aggregate when the declaration carries no arguments")
        void aboutTheAggregateWithoutArguments() {
            TypeNode repository = TypeNode.builder(REPOSITORY, TypeNature.INTERFACE)
                    .interfaces(List.of(TypeRef.of(JPA_REPOSITORY)))
                    .build();
            CodeModel code = CodeModel.builder()
                    .addType(repository)
                    .addType(TypeNode.builder(ORDER, TypeNature.CLASS).build())
                    .addType(TypeNode.externalStub(TypeId.of(JPA_REPOSITORY), TypeNature.INTERFACE))
                    .addType(TypeNode.externalStub(TypeId.of(SPRING_REPOSITORY), TypeNature.INTERFACE))
                    .supertypes(REPOSITORY, List.of(TypeId.of(JPA_REPOSITORY), TypeId.of(SPRING_REPOSITORY)))
                    .build();

            Verdicts verdicts = Classifier.classify(context(code));

            assertThat(verdicts.kindOf(REPOSITORY)).contains(ArchKind.DRIVEN_PORT);
            assertThat(verdicts.kindOf(ORDER)).contains(ArchKind.UNCLASSIFIED);
        }
    }
}
