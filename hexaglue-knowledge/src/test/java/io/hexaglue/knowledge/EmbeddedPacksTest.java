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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Annotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The packs shipped with the tool are its published claims about the frameworks it reads. Each one
 * is exercised on the smallest fixture that can carry the claim — the point being that a symbol
 * removed, mistyped or re-pointed at another fact fails here rather than in a user's report.
 */
class EmbeddedPacksTest {

    private static final FrameworkKnowledge KNOWLEDGE = KnowledgePacks.embedded();

    private static List<KnowledgeFinding> factsOnTypeAnnotatedWith(String... annotations) {
        TypeNode type = TypeNode.builder(TypeId.of("com.acme.Subject"), TypeNature.CLASS)
                .annotations(List.of(annotations).stream().map(Annotation::of).toList())
                .build();
        return KNOWLEDGE.factsFor(CodeModel.builder().addType(type).build(), type);
    }

    private static List<KnowledgeFinding> factsOnType(String qualifiedName) {
        TypeNode type =
                TypeNode.builder(TypeId.of(qualifiedName), TypeNature.CLASS).build();
        return KNOWLEDGE.factsFor(CodeModel.builder().addType(type).build(), type);
    }

    private static List<KnowledgeFact> factsOf(List<KnowledgeFinding> findings) {
        return findings.stream().map(KnowledgeFinding::fact).toList();
    }

    @Nested
    @DisplayName("every shipped pack")
    class EveryShippedPack {

        @Test
        @DisplayName("loads, which is what proves it against the strict reader")
        void loads() {
            assertThat(KNOWLEDGE.packs())
                    .extracting(KnowledgePack::id)
                    .containsExactly("jmolecules", "spring", "jakarta", "platform");
        }

        @Test
        @DisplayName("claims no symbol a sibling pack already claims")
        void claimsNoSymbolASiblingAlreadyClaims() {
            Map<String, String> claimedBy = new LinkedHashMap<>();
            for (KnowledgePack pack : KNOWLEDGE.packs()) {
                for (KnowledgeEntry entry : pack.entries()) {
                    String previous = claimedBy.putIfAbsent(entry.selector().symbol(), pack.id());
                    assertThat(previous)
                            .withFailMessage(
                                    "%s is claimed by both %s and %s",
                                    entry.selector().symbol(), previous, pack.id())
                            .isNull();
                }
            }
        }

        @Test
        @DisplayName("declares a kind only where an author declared an intent")
        void declaresAKindOnlyForIntent() {
            assertThat(KNOWLEDGE.packs())
                    .filteredOn(pack -> !"jmolecules".equals(pack.id()))
                    .flatExtracting(KnowledgePack::entries)
                    .extracting(KnowledgeEntry::declaredKind)
                    .allSatisfy(kind -> assertThat(kind).isEmpty());
        }

        @Test
        @DisplayName("is refused when its resource does not exist")
        void isRefusedWhenMissing() {
            assertThatThrownBy(() -> PackLoader.loadResource("io/hexaglue/knowledge/packs/nowhere.yaml"))
                    .isInstanceOf(KnowledgeException.class)
                    .satisfies(failure -> assertThat(((KnowledgeException) failure)
                                    .diagnostic()
                                    .code()
                                    .value())
                            .isEqualTo("HG-KNOWLEDGE-001"));
        }
    }

    @Nested
    @DisplayName("jMolecules states what the author declared")
    class JMoleculesStatesIntent {

        @Test
        @DisplayName("an aggregate root, an entity, a value object and an identifier")
        void theDddVocabulary() {
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.ddd.annotation.AggregateRoot"))
                    .singleElement()
                    .satisfies(finding -> {
                        assertThat(finding.fact()).isEqualTo(KnowledgeFact.DECLARED_KIND);
                        assertThat(finding.declaredKind()).contains(ArchKind.AGGREGATE_ROOT);
                    });
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.ddd.annotation.Entity"))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.ENTITY));
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.ddd.annotation.ValueObject"))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.VALUE_OBJECT));
        }

        @Test
        @DisplayName("a repository, which is a port and not a layer")
        void aRepositoryIsAPort() {
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.ddd.annotation.Repository"))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.DRIVEN_PORT));
        }

        @Test
        @DisplayName("both sides of the hexagon, ports and adapters alike")
        void bothSidesOfTheHexagon() {
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.architecture.hexagonal.PrimaryPort"))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.DRIVING_PORT));
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.architecture.hexagonal.SecondaryPort"))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.DRIVEN_PORT));
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.architecture.hexagonal.PrimaryAdapter"))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.DRIVING_ADAPTER));
            assertThat(factsOnTypeAnnotatedWith("org.jmolecules.architecture.hexagonal.SecondaryAdapter"))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.DRIVEN_ADAPTER));
        }

        @Test
        @DisplayName("the same vocabulary when it is carried by an interface instead of an annotation")
        void theSameVocabularyThroughInterfaces() {
            TypeNode identifier = TypeNode.builder(TypeId.of("com.acme.OrderId"), TypeNature.RECORD)
                    .interfaces(List.of(TypeRef.of("org.jmolecules.ddd.types.Identifier")))
                    .build();
            CodeModel model = CodeModel.builder()
                    .addType(identifier)
                    .addType(TypeNode.externalStub(
                            TypeId.of("org.jmolecules.ddd.types.Identifier"), TypeNature.INTERFACE))
                    .supertypes(identifier.id(), List.of(TypeId.of("org.jmolecules.ddd.types.Identifier")))
                    .build();

            assertThat(KNOWLEDGE.factsFor(model, identifier))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.declaredKind()).contains(ArchKind.IDENTIFIER));
        }
    }

    @Nested
    @DisplayName("Spring states what its symbols do")
    class SpringStatesWhatItsSymbolsDo {

        @Test
        @DisplayName("a repository interface, with the aggregate and the identifier it manages")
        void aRepositoryInterfaceWithItsSubject() {
            String jpaRepository = "org.springframework.data.jpa.repository.JpaRepository";
            String repository = "org.springframework.data.repository.Repository";
            TypeNode orders = TypeNode.builder(TypeId.of("com.acme.OrderRepository"), TypeNature.INTERFACE)
                    .interfaces(List.of(TypeRef.parameterized(
                            jpaRepository, TypeRef.of("com.acme.Order"), TypeRef.of("com.acme.OrderId"))))
                    .build();
            CodeModel model = CodeModel.builder()
                    .addType(orders)
                    .addType(TypeNode.externalStub(TypeId.of(jpaRepository), TypeNature.INTERFACE))
                    .addType(TypeNode.externalStub(TypeId.of(repository), TypeNature.INTERFACE))
                    .supertypes(TypeId.of(jpaRepository), List.of(TypeId.of(repository)))
                    .supertypes(orders.id(), List.of(TypeId.of(jpaRepository), TypeId.of(repository)))
                    .build();

            assertThat(KNOWLEDGE.factsFor(model, orders)).singleElement().satisfies(finding -> {
                assertThat(finding.fact()).isEqualTo(KnowledgeFact.SPRING_DATA_REPOSITORY);
                assertThat(finding.capture("subject")).contains(TypeRef.of("com.acme.Order"));
                assertThat(finding.capture("id")).contains(TypeRef.of("com.acme.OrderId"));
            });
        }

        @Test
        @DisplayName("an entry point the framework calls from outside")
        void anEntryPoint() {
            assertThat(factsOf(factsOnTypeAnnotatedWith("org.springframework.web.bind.annotation.RestController")))
                    .containsExactly(KnowledgeFact.DRIVING_ENTRYPOINT);
            assertThat(factsOf(factsOnTypeAnnotatedWith("org.springframework.stereotype.Controller")))
                    .containsExactly(KnowledgeFact.DRIVING_ENTRYPOINT);
        }

        @Test
        @DisplayName("a stereotype that supports a reading without deciding it")
        void aStereotype() {
            assertThat(factsOf(factsOnTypeAnnotatedWith("org.springframework.stereotype.Service")))
                    .containsExactly(KnowledgeFact.APPLICATION_STEREOTYPE);
        }

        @Test
        @DisplayName("plumbing, which belongs to no ring of the hexagon")
        void plumbing() {
            assertThat(factsOf(factsOnTypeAnnotatedWith("org.springframework.context.annotation.Configuration")))
                    .containsExactly(KnowledgeFact.TECHNICAL);
        }

        @Test
        @DisplayName("a tool that reaches outside, whether named exactly or by its package")
        void aToolThatReachesOutside() {
            assertThat(factsOf(factsOnType("org.springframework.jdbc.core.JdbcTemplate")))
                    .containsExactly(KnowledgeFact.INFRA_DEPENDENCY);
            assertThat(factsOf(factsOnType("org.springframework.data.jpa.repository.support.SimpleJpaRepository")))
                    .containsExactly(KnowledgeFact.INFRA_DEPENDENCY);
        }
    }

    @Nested
    @DisplayName("Jakarta states persistence without ever stating a kind")
    class JakartaStatesPersistence {

        @Test
        @DisplayName("a mapped type carries a persistence fact, and nothing that could become a verdict")
        void aMappedTypeCarriesNoVerdict() {
            List<KnowledgeFinding> findings = factsOnTypeAnnotatedWith("jakarta.persistence.Entity");

            assertThat(factsOf(findings)).containsExactly(KnowledgeFact.PERSISTENCE_MODEL);
            assertThat(findings)
                    .allSatisfy(finding -> assertThat(finding.declaredKind()).isEmpty());
        }

        @Test
        @DisplayName("the same, under the names the previous decade used")
        void theSameUnderTheOldNames() {
            assertThat(factsOf(factsOnTypeAnnotatedWith("javax.persistence.Entity")))
                    .containsExactly(KnowledgeFact.PERSISTENCE_MODEL);
        }

        @Test
        @DisplayName("a JAX-RS resource, which is an entry point")
        void aJaxRsResource() {
            assertThat(factsOf(factsOnTypeAnnotatedWith("jakarta.ws.rs.Path")))
                    .containsExactly(KnowledgeFact.DRIVING_ENTRYPOINT);
        }

        @Test
        @DisplayName("validation, which pollutes no domain")
        void validationPollutesNoDomain() {
            assertThat(factsOf(factsOnType("jakarta.validation.constraints.NotNull")))
                    .containsExactly(KnowledgeFact.NEUTRAL);
        }

        @Test
        @DisplayName("the persistence tool itself")
        void thePersistenceTool() {
            assertThat(factsOf(factsOnType("jakarta.persistence.EntityManager")))
                    .containsExactly(KnowledgeFact.INFRA_DEPENDENCY);
        }
    }

    @Nested
    @DisplayName("the platform pack states what the rest of the classpath is")
    class ThePlatformPack {

        @Test
        @DisplayName("generated code, which is nobody's intent")
        void generatedCode() {
            assertThat(factsOf(factsOnTypeAnnotatedWith("lombok.Generated")))
                    .containsExactly(KnowledgeFact.GENERATED_CODE);
            assertThat(factsOf(factsOnTypeAnnotatedWith("javax.annotation.processing.Generated")))
                    .containsExactly(KnowledgeFact.GENERATED_CODE);
        }

        @Test
        @DisplayName("logging, which pollutes no domain")
        void loggingPollutesNoDomain() {
            assertThat(factsOf(factsOnType("org.slf4j.Logger"))).containsExactly(KnowledgeFact.NEUTRAL);
        }

        @Test
        @DisplayName("an HTTP client that ships on its own")
        void anHttpClientThatShipsOnItsOwn() {
            assertThat(factsOf(factsOnType("feign.Client"))).containsExactly(KnowledgeFact.INFRA_DEPENDENCY);
        }
    }
}
