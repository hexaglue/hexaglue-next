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

package io.hexaglue.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Annotation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Framework knowledge is stated once, on the root of a hierarchy, and answered from the closure the
 * frontend computed. What this exercises is the distance between the two: an interface extending a
 * vendor interface, itself derived from the symbol a pack knows, must answer — with the aggregate
 * and the identifier its declaration named.
 */
class FrameworkKnowledgeTest {

    private static final String REPOSITORY = "org.springframework.data.repository.Repository";
    private static final String CRUD_REPOSITORY = "org.springframework.data.repository.CrudRepository";
    private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
    private static final String ENTITY = "jakarta.persistence.Entity";
    private static final String AGGREGATE_ROOT = "org.jmolecules.ddd.annotation.AggregateRoot";

    private static final KnowledgePack SPRING_DATA = new KnowledgePack(
            "spring-data",
            "What a Spring Data interface says about a type.",
            List.of(KnowledgeEntry.of(new Selector.Supertype(REPOSITORY), KnowledgeFact.SPRING_DATA_REPOSITORY)));

    private static final KnowledgePack JAKARTA = new KnowledgePack(
            "jakarta",
            "Persistence mapping, and the tools that perform it.",
            List.of(
                    KnowledgeEntry.of(new Selector.Annotated(ENTITY), KnowledgeFact.PERSISTENCE_MODEL),
                    KnowledgeEntry.of(
                            new Selector.Type("jakarta.persistence.EntityManager"), KnowledgeFact.INFRA_DEPENDENCY),
                    KnowledgeEntry.of(new Selector.PackagePrefix("jakarta.validation"), KnowledgeFact.NEUTRAL)));

    private static final KnowledgePack JMOLECULES = new KnowledgePack(
            "jmolecules",
            "Intent, declared by the author.",
            List.of(KnowledgeEntry.declaring(new Selector.Annotated(AGGREGATE_ROOT), ArchKind.AGGREGATE_ROOT)));

    private static final FrameworkKnowledge KNOWLEDGE =
            FrameworkKnowledge.of(List.of(SPRING_DATA, JAKARTA, JMOLECULES));

    private static TypeNode source(String qualifiedName) {
        return TypeNode.builder(TypeId.of(qualifiedName), TypeNature.CLASS).build();
    }

    private static TypeNode annotated(String qualifiedName, String... annotations) {
        return TypeNode.builder(TypeId.of(qualifiedName), TypeNature.CLASS)
                .annotations(List.of(annotations).stream().map(Annotation::of).toList())
                .build();
    }

    /**
     * A repository interface declared over {@code declaredSupertype}, above a chain of stubs — the
     * closure of each one included, exactly as the frontend hands it over.
     */
    private static CodeModel repositoryModel(TypeRef declaredSupertype, List<TypeId> chain) {
        TypeNode repository = TypeNode.builder(TypeId.of("com.acme.OrderRepository"), TypeNature.INTERFACE)
                .interfaces(List.of(declaredSupertype))
                .build();
        CodeModel.Builder model = CodeModel.builder().addType(repository).supertypes(repository.id(), chain);
        for (int rank = 0; rank < chain.size(); rank++) {
            model.addType(TypeNode.externalStub(chain.get(rank), TypeNature.INTERFACE));
            model.supertypes(chain.get(rank), chain.subList(rank + 1, chain.size()));
        }
        return model.build();
    }

    private static CodeModel modelOf(TypeNode... types) {
        CodeModel.Builder model = CodeModel.builder();
        for (TypeNode type : types) {
            model.addType(type);
        }
        return model.build();
    }

    @Nested
    @DisplayName("recognizes a symbol")
    class RecognizesASymbol {

        @Test
        @DisplayName("borne as an annotation, by its qualified name")
        void borneAsAnAnnotation() {
            TypeNode order = annotated("com.acme.Order", ENTITY);

            assertThat(KNOWLEDGE.factsFor(modelOf(order), order))
                    .extracting(KnowledgeFinding::fact)
                    .containsExactly(KnowledgeFact.PERSISTENCE_MODEL);
        }

        @Test
        @DisplayName("inherited, however far up the hierarchy it sits")
        void inheritedHoweverFarUp() {
            CodeModel model = repositoryModel(
                    TypeRef.parameterized(JPA_REPOSITORY, TypeRef.of("com.acme.Order"), TypeRef.of("com.acme.OrderId")),
                    List.of(TypeId.of(JPA_REPOSITORY), TypeId.of(CRUD_REPOSITORY), TypeId.of(REPOSITORY)));
            TypeNode repository =
                    model.type(TypeId.of("com.acme.OrderRepository")).orElseThrow();

            assertThat(KNOWLEDGE.factsFor(model, repository))
                    .extracting(KnowledgeFinding::fact)
                    .containsExactly(KnowledgeFact.SPRING_DATA_REPOSITORY);
        }

        @Test
        @DisplayName("as the type itself")
        void whenTheSymbolIsTheTypeItself() {
            TypeNode entityManager = source("jakarta.persistence.EntityManager");

            assertThat(KNOWLEDGE.factsFor(modelOf(entityManager), entityManager))
                    .extracting(KnowledgeFinding::fact)
                    .containsExactly(KnowledgeFact.INFRA_DEPENDENCY);
        }

        @Test
        @DisplayName("by the package it lives in, on a segment boundary")
        void byThePackageItLivesIn() {
            TypeNode constraint = source("jakarta.validation.constraints.NotNull");
            TypeNode lookalike = source("jakarta.validationtools.Checker");

            assertThat(KNOWLEDGE.factsFor(modelOf(constraint), constraint))
                    .extracting(KnowledgeFinding::fact)
                    .containsExactly(KnowledgeFact.NEUTRAL);
            assertThat(KNOWLEDGE.factsFor(modelOf(lookalike), lookalike)).isEmpty();
        }

        @Test
        @DisplayName("never by its simple name")
        void neverByItsSimpleName() {
            TypeNode order = annotated("com.acme.Order", "com.acme.persistence.Entity");

            assertThat(KNOWLEDGE.factsFor(modelOf(order), order)).isEmpty();
        }
    }

    @Nested
    @DisplayName("captures what a declaration names")
    class CapturesWhatADeclarationNames {

        @Test
        @DisplayName("the aggregate and its identifier, from the supertype reference")
        void theAggregateAndItsIdentifier() {
            CodeModel model = repositoryModel(
                    TypeRef.parameterized(JPA_REPOSITORY, TypeRef.of("com.acme.Order"), TypeRef.of("com.acme.OrderId")),
                    List.of(TypeId.of(JPA_REPOSITORY), TypeId.of(CRUD_REPOSITORY), TypeId.of(REPOSITORY)));
            TypeNode repository =
                    model.type(TypeId.of("com.acme.OrderRepository")).orElseThrow();

            KnowledgeFinding finding = KNOWLEDGE.factsFor(model, repository).get(0);

            assertThat(finding.capture("subject")).contains(TypeRef.of("com.acme.Order"));
            assertThat(finding.capture("id")).contains(TypeRef.of("com.acme.OrderId"));
            assertThat(finding.captures()).containsOnlyKeys("subject", "id");
        }

        @Test
        @DisplayName("nothing, when the declaration carries no type arguments")
        void nothingWhenTheDeclarationCarriesNone() {
            TypeNode base = TypeNode.builder(TypeId.of("com.acme.BaseRepository"), TypeNature.INTERFACE)
                    .interfaces(List.of(TypeRef.of(REPOSITORY)))
                    .build();
            TypeNode repository = TypeNode.builder(TypeId.of("com.acme.OrderRepository"), TypeNature.INTERFACE)
                    .interfaces(List.of(TypeRef.of("com.acme.BaseRepository")))
                    .build();
            CodeModel model = CodeModel.builder()
                    .addType(base)
                    .addType(repository)
                    .addType(TypeNode.externalStub(TypeId.of(REPOSITORY), TypeNature.INTERFACE))
                    .supertypes(base.id(), List.of(TypeId.of(REPOSITORY)))
                    .supertypes(repository.id(), List.of(base.id(), TypeId.of(REPOSITORY)))
                    .build();

            KnowledgeFinding finding = KNOWLEDGE.factsFor(model, repository).get(0);

            assertThat(finding.fact()).isEqualTo(KnowledgeFact.SPRING_DATA_REPOSITORY);
            assertThat(finding.captures()).isEmpty();
        }

        @Test
        @DisplayName("a type variable as it stands, leaving the engine to make of it what it can")
        void aTypeVariableAsItStands() {
            CodeModel model = repositoryModel(
                    TypeRef.parameterized(JPA_REPOSITORY, TypeRef.typeVariable("T"), TypeRef.typeVariable("ID")),
                    List.of(TypeId.of(JPA_REPOSITORY), TypeId.of(REPOSITORY)));
            TypeNode repository =
                    model.type(TypeId.of("com.acme.OrderRepository")).orElseThrow();

            KnowledgeFinding finding = KNOWLEDGE.factsFor(model, repository).get(0);

            assertThat(finding.capture("subject")).contains(TypeRef.typeVariable("T"));
        }

        @Test
        @DisplayName("from the superclass before the interfaces, and never twice")
        void fromTheSuperclassFirst() {
            TypeNode repository = TypeNode.builder(TypeId.of("com.acme.OrderRepository"), TypeNature.CLASS)
                    .superClass(TypeRef.parameterized(
                            CRUD_REPOSITORY, TypeRef.of("com.acme.Order"), TypeRef.of("com.acme.OrderId")))
                    .interfaces(List.of(TypeRef.parameterized(
                            JPA_REPOSITORY, TypeRef.of("com.acme.Other"), TypeRef.of("com.acme.OtherId"))))
                    .build();
            CodeModel model = CodeModel.builder()
                    .addType(repository)
                    .addType(TypeNode.externalStub(TypeId.of(CRUD_REPOSITORY), TypeNature.INTERFACE))
                    .addType(TypeNode.externalStub(TypeId.of(JPA_REPOSITORY), TypeNature.INTERFACE))
                    .addType(TypeNode.externalStub(TypeId.of(REPOSITORY), TypeNature.INTERFACE))
                    .supertypes(TypeId.of(CRUD_REPOSITORY), List.of(TypeId.of(REPOSITORY)))
                    .supertypes(TypeId.of(JPA_REPOSITORY), List.of(TypeId.of(CRUD_REPOSITORY), TypeId.of(REPOSITORY)))
                    .supertypes(
                            repository.id(),
                            List.of(TypeId.of(CRUD_REPOSITORY), TypeId.of(JPA_REPOSITORY), TypeId.of(REPOSITORY)))
                    .build();

            List<KnowledgeFinding> findings = KNOWLEDGE.factsFor(model, repository);

            assertThat(findings).hasSize(1);
            assertThat(findings.get(0).capture("subject")).contains(TypeRef.of("com.acme.Order"));
        }
    }

    @Nested
    @DisplayName("answers the same way every time")
    class AnswersTheSameWayEveryTime {

        @Test
        @DisplayName("in pack order, then in the order each pack states its entries")
        void inPackThenEntryOrder() {
            TypeNode order = annotated("com.acme.Order", ENTITY, AGGREGATE_ROOT);

            assertThat(KNOWLEDGE.factsFor(modelOf(order), order))
                    .extracting(KnowledgeFinding::packId, KnowledgeFinding::fact)
                    .containsExactly(
                            tuple("jakarta", KnowledgeFact.PERSISTENCE_MODEL),
                            tuple("jmolecules", KnowledgeFact.DECLARED_KIND));
        }

        @Test
        @DisplayName("carrying the declared kind of an intent entry")
        void carryingTheDeclaredKind() {
            TypeNode order = annotated("com.acme.Order", AGGREGATE_ROOT);

            KnowledgeFinding finding = KNOWLEDGE.factsFor(modelOf(order), order).get(0);

            assertThat(finding.declaredKind()).contains(ArchKind.AGGREGATE_ROOT);
            assertThat(finding.symbol()).isEqualTo(AGGREGATE_ROOT);
            assertThat(finding.entry().selector()).isEqualTo(new Selector.Annotated(AGGREGATE_ROOT));
        }

        @Test
        @DisplayName("saying nothing about a type no pack knows")
        void sayingNothingAboutAnUnknownType() {
            TypeNode plain = source("com.acme.Order");

            assertThat(KNOWLEDGE.factsFor(modelOf(plain), plain)).isEmpty();
        }
    }

    @Nested
    @DisplayName("holds its packs")
    class HoldsItsPacks {

        @Test
        @DisplayName("in the order they were given")
        void inTheOrderTheyWereGiven() {
            assertThat(KNOWLEDGE.packs())
                    .extracting(KnowledgePack::id)
                    .containsExactly("spring-data", "jakarta", "jmolecules");
        }

        @Test
        @DisplayName("refusing two packs under one identity")
        void refusingTwoPacksUnderOneIdentity() {
            assertThatThrownBy(() -> FrameworkKnowledge.of(List.of(JAKARTA, JAKARTA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("jakarta");
        }

        @Test
        @DisplayName("refusing to know nothing at all")
        void refusingToKnowNothingAtAll() {
            assertThatThrownBy(() -> FrameworkKnowledge.of(List.of())).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("a finding")
    class AFinding {

        @Test
        @DisplayName("refuses a capture the fact never named")
        void refusesACaptureTheFactNeverNamed() {
            KnowledgeEntry entry = KnowledgeEntry.of(new Selector.Annotated(ENTITY), KnowledgeFact.PERSISTENCE_MODEL);

            assertThatThrownBy(() ->
                            new KnowledgeFinding("jakarta", entry, Map.of("subject", TypeRef.of("com.acme.Order"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subject");
        }

        @Test
        @DisplayName("refuses to be attributed to no pack")
        void refusesToBeAttributedToNoPack() {
            KnowledgeEntry entry = KnowledgeEntry.of(new Selector.Annotated(ENTITY), KnowledgeFact.PERSISTENCE_MODEL);

            assertThatThrownBy(() -> new KnowledgeFinding(" ", entry, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
