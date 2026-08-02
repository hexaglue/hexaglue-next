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
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InfrastructureDependencyTest {

    private static final TypeId SUBJECT = TypeId.of("com.acme.Anything");
    private static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";
    private static final String JDBC_TEMPLATE = "org.springframework.jdbc.core.JdbcTemplate";
    private static final String LOGGER = "org.slf4j.Logger";
    private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
    private static final String SPRING_REPOSITORY = "org.springframework.data.repository.Repository";

    /** The subject, plus the classpath stub every dependency of it points at. */
    private static CodeModel holding(TypeNode subject, String... stubs) {
        CodeModel.Builder code = CodeModel.builder().addType(subject);
        for (String stub : stubs) {
            code.addType(TypeNode.externalStub(TypeId.of(stub), TypeNature.INTERFACE));
        }
        return code.build();
    }

    private static TypeNode withField(String fieldType) {
        return TypeNode.builder(SUBJECT, TypeNature.CLASS)
                .fields(List.of(Field.builder("dependency", TypeRef.of(fieldType))
                        .modifiers(Set.of(Modifier.FINAL))
                        .build()))
                .build();
    }

    private static TypeNode withConstructorParameter(String parameterType) {
        return TypeNode.builder(SUBJECT, TypeNature.CLASS)
                .constructors(List.of(
                        Constructor.of(List.of(new Parameter("dependency", TypeRef.of(parameterType), List.of())))))
                .build();
    }

    private static Classification verdictOf(CodeModel code) {
        return Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .verdict(SUBJECT)
                .orElseThrow();
    }

    @Nested
    @DisplayName("reads a type that reaches outside the hexagon")
    class ReadsATypeThatReachesOutside {

        @Test
        @DisplayName("as the driven adapter it is, when it holds the tool as a field")
        void holdingTheToolAsAField() {
            Classification verdict = verdictOf(holding(withField(ENTITY_MANAGER), ENTITY_MANAGER));

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVEN_ADAPTER);
            assertThat(verdict.direction()).contains(PortDirection.DRIVEN);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("as the driven adapter it is, when the tool is handed to its constructor")
        void takingTheToolAsAConstructorParameter() {
            assertThat(verdictOf(holding(withConstructorParameter(JDBC_TEMPLATE), JDBC_TEMPLATE))
                            .kind())
                    .isEqualTo(ArchKind.DRIVEN_ADAPTER);
        }

        @Test
        @DisplayName("even when the tool is wrapped, because the wrapping changes nothing")
        void evenWhenTheToolIsWrapped() {
            TypeNode subject = TypeNode.builder(SUBJECT, TypeNature.CLASS)
                    .fields(List.of(
                            Field.builder("clients", TypeRef.parameterized("java.util.List", TypeRef.of(JDBC_TEMPLATE)))
                                    .build()))
                    .build();

            assertThat(verdictOf(holding(subject, JDBC_TEMPLATE)).kind()).isEqualTo(ArchKind.DRIVEN_ADAPTER);
        }

        @Test
        @DisplayName("and when it extends the framework's own base class")
        void extendingTheFrameworksBaseClass() {
            TypeNode subject = TypeNode.builder(SUBJECT, TypeNature.CLASS)
                    .superClass(TypeRef.of(JDBC_TEMPLATE))
                    .build();

            assertThat(verdictOf(holding(subject, JDBC_TEMPLATE)).kind()).isEqualTo(ArchKind.DRIVEN_ADAPTER);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a dependency the packs call neutral, which no domain is impure for holding")
        void aboutANeutralDependency() {
            assertThat(verdictOf(holding(withField(LOGGER), LOGGER)).kind()).isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a dependency no pack recognizes")
        void aboutAnUnknownDependency() {
            assertThat(verdictOf(holding(withField("com.acme.Neighbour"), "com.acme.Neighbour"))
                            .kind())
                    .isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an interface reaching for the same tool, which contracts rather than adapts")
        void aboutAnInterface() {
            // An interface can name an infrastructure type as readily as a class can hold one; what
            // makes an adapter is being the implementation, and an interface never is.
            TypeNode subject = TypeNode.builder(SUBJECT, TypeNature.INTERFACE)
                    .interfaces(List.of(TypeRef.of(ENTITY_MANAGER)))
                    .build();

            assertThat(verdictOf(holding(subject, ENTITY_MANAGER)).kind()).isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a Spring Data repository, which is the port and not the adapter of it")
        void aboutASpringDataRepository() {
            // The vendor package is infrastructure, and the repository extends it: read as a class
            // would be, this interface would be a port and an adapter at once, and the two readings
            // would cancel each other into silence. The framework writes the adapter; the author
            // wrote the port.
            TypeId repository = TypeId.of("com.acme.Ledger");
            CodeModel code = CodeModel.builder()
                    .addType(TypeNode.builder(repository, TypeNature.INTERFACE)
                            .interfaces(List.of(TypeRef.parameterized(
                                    JPA_REPOSITORY, TypeRef.of("com.acme.Book"), TypeRef.of("java.util.UUID"))))
                            .build())
                    .addType(TypeNode.builder(TypeId.of("com.acme.Book"), TypeNature.CLASS)
                            .build())
                    .addType(TypeNode.externalStub(TypeId.of(JPA_REPOSITORY), TypeNature.INTERFACE))
                    .addType(TypeNode.externalStub(TypeId.of(SPRING_REPOSITORY), TypeNature.INTERFACE))
                    .supertypes(repository, List.of(TypeId.of(JPA_REPOSITORY), TypeId.of(SPRING_REPOSITORY)))
                    .supertypes(TypeId.of(JPA_REPOSITORY), List.of(TypeId.of(SPRING_REPOSITORY)))
                    .build();

            assertThat(Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                            .kindOf(repository))
                    .contains(ArchKind.DRIVEN_PORT);
        }

        @Test
        @DisplayName("about the infrastructure tool itself, which is nobody's adapter")
        void aboutTheToolItself() {
            CodeModel code = CodeModel.builder()
                    .addType(TypeNode.builder(TypeId.of(JDBC_TEMPLATE), TypeNature.CLASS)
                            .build())
                    .build();

            assertThat(Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                            .kindOf(TypeId.of(JDBC_TEMPLATE)))
                    .contains(ArchKind.UNCLASSIFIED);
        }
    }
}
