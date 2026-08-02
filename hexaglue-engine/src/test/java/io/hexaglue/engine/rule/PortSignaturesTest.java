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
import static org.assertj.core.api.Assertions.tuple;

import io.hexaglue.engine.Classifier;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.FactBase;
import io.hexaglue.engine.PortRole;
import io.hexaglue.engine.Relation;
import io.hexaglue.engine.RelationKind;
import io.hexaglue.engine.RuleSet;
import io.hexaglue.engine.Saturation;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortSignaturesTest {

    private static final TypeId PORT = TypeId.of("com.acme.Ledger");
    private static final TypeId SUBJECT = TypeId.of("com.acme.Fleet");
    private static final TypeId OTHER = TypeId.of("com.acme.Manifest");
    private static final String SECONDARY_PORT = "org.jmolecules.architecture.hexagonal.SecondaryPort";
    private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
    private static final String SPRING_REPOSITORY = "org.springframework.data.repository.Repository";
    private static final TypeRef VOID = TypeRef.of("void");

    /** A way out the author declared, so the role is read on a port nothing else is deciding. */
    private static TypeNode port(Method... methods) {
        return TypeNode.builder(PORT, TypeNature.INTERFACE)
                .annotations(List.of(Annotation.of(SECONDARY_PORT)))
                .methods(List.of(methods))
                .build();
    }

    private static Method method(String name, TypeRef returnType, TypeRef... parameters) {
        List<Parameter> taken = List.of(parameters).stream()
                .map(type -> Parameter.of("argument" + type.simpleName(), type))
                .toList();
        return Method.builder(name, returnType).parameters(taken).build();
    }

    /** A type whose state cannot change, which is how something worth announcing is written. */
    private static TypeNode immutable(TypeId id) {
        return TypeNode.builder(id, TypeNature.RECORD)
                .fields(List.of(Field.builder("reference", TypeRef.of("java.lang.String"))
                        .modifiers(Set.of(Modifier.FINAL))
                        .build()))
                .build();
    }

    private static TypeNode mutable(TypeId id) {
        return TypeNode.builder(id, TypeNature.CLASS)
                .fields(List.of(Field.of("reference", TypeRef.of("java.lang.String"))))
                .build();
    }

    /** The facts as they stand once the verdicts have settled, which is what a consumer reads. */
    private static FactBase settled(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        EngineContext context = EngineContext.of(code.build(), KnowledgePacks.embedded(), HexaGlueConfig.defaults());
        return Saturation.saturate(RuleSet.standard(), context.withVerdicts(Classifier.classify(context)));
    }

    private static List<DrivenPortType> rolesOf(FactBase facts, TypeId port) {
        return facts.about(port, PortRole.class).stream().map(PortRole::role).toList();
    }

    @Nested
    @DisplayName("reads the trade a way out plies")
    class ReadsTheTrade {

        @Test
        @DisplayName("as storage, when the signatures converge on one type of the perimeter")
        void storageWhenTheSignaturesConverge() {
            TypeNode ledger = port(
                    method("locate", TypeRef.parameterized("java.util.Optional", TypeRef.of(SUBJECT.qualifiedName()))),
                    method("keep", VOID, TypeRef.of(SUBJECT.qualifiedName())));

            FactBase facts = settled(ledger, mutable(SUBJECT));

            assertThat(rolesOf(facts, PORT)).containsExactly(DrivenPortType.REPOSITORY);
            assertThat(facts.about(PORT, PortRole.class))
                    .extracting(PortRole::render)
                    .containsExactly("PORT_ROLE(com.acme.Ledger) = REPOSITORY");
            assertThat(facts.about(PORT, Relation.class))
                    .extracting(Relation::kind, Relation::object)
                    .containsExactly(tuple(RelationKind.MANAGES, SUBJECT));
        }

        @Test
        @DisplayName("as storage, when a pack already knows it stores and declares nothing itself")
        void storageWhenAPackAlreadyKnows() {
            TypeId repository = TypeId.of("com.acme.HangarBooks");
            CodeModel code = CodeModel.builder()
                    .addType(TypeNode.builder(repository, TypeNature.INTERFACE)
                            .interfaces(List.of(TypeRef.parameterized(
                                    JPA_REPOSITORY, TypeRef.of(SUBJECT.qualifiedName()), TypeRef.of("java.util.UUID"))))
                            .build())
                    .addType(mutable(SUBJECT))
                    .addType(TypeNode.externalStub(TypeId.of(JPA_REPOSITORY), TypeNature.INTERFACE))
                    .addType(TypeNode.externalStub(TypeId.of(SPRING_REPOSITORY), TypeNature.INTERFACE))
                    .supertypes(repository, List.of(TypeId.of(JPA_REPOSITORY), TypeId.of(SPRING_REPOSITORY)))
                    .supertypes(TypeId.of(JPA_REPOSITORY), List.of(TypeId.of(SPRING_REPOSITORY)))
                    .build();
            EngineContext context = EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults());

            FactBase facts =
                    Saturation.saturate(RuleSet.standard(), context.withVerdicts(Classifier.classify(context)));

            assertThat(rolesOf(facts, repository)).containsExactly(DrivenPortType.REPOSITORY);
        }

        @Test
        @DisplayName("as publication, when every method only ever goes out carrying a value")
        void publicationWhenEveryMethodOnlyGoesOut() {
            TypeNode ledger = port(method("announce", VOID, TypeRef.of(SUBJECT.qualifiedName())));

            assertThat(rolesOf(settled(ledger, immutable(SUBJECT)), PORT))
                    .containsExactly(DrivenPortType.EVENT_PUBLISHER);
        }

        @Test
        @DisplayName("as a call to a service, when the signatures are about types the perimeter does not hold")
        void aCallToAServiceWhenTheTypesAreForeign() {
            // The same string goes in and comes back, which is convergence in shape and nothing at
            // all in meaning: a subject is a type of the analyzed application, not of the JDK.
            TypeNode ledger = port(method("quote", TypeRef.of("java.lang.String"), TypeRef.of("java.lang.String")));

            assertThat(rolesOf(settled(ledger), PORT)).containsExactly(DrivenPortType.GATEWAY);
        }

        @Test
        @DisplayName("as a call to a service, when a method carrying a value answers back")
        void aCallToAServiceWhenAMethodAnswersBack() {
            // Announcing is one-way by definition. A method that hands something over and waits
            // for an answer is asking a question, whatever it is handing over.
            TypeNode ledger =
                    port(method("submit", TypeRef.of("java.lang.String"), TypeRef.of(SUBJECT.qualifiedName())));

            assertThat(rolesOf(settled(ledger, immutable(SUBJECT)), PORT)).containsExactly(DrivenPortType.GATEWAY);
        }

        @Test
        @DisplayName("as a call to a service, when a one-way method carries something that can change")
        void aCallToAServiceWhenTheValueCanChange() {
            // Announcing is telling the outside what happened, and what happened cannot be edited
            // afterwards. A mutable argument is a request being handed over, not an event.
            TypeNode ledger = port(method("submit", VOID, TypeRef.of(SUBJECT.qualifiedName())));

            assertThat(rolesOf(settled(ledger, mutable(SUBJECT)), PORT)).containsExactly(DrivenPortType.GATEWAY);
        }

        @Test
        @DisplayName("as a call to a service, when two subjects share the signatures and neither converges")
        void aCallToAServiceWhenTwoSubjectsShareTheSignatures() {
            TypeNode ledger = port(
                    method("locate", TypeRef.of(SUBJECT.qualifiedName()), TypeRef.of(SUBJECT.qualifiedName())),
                    method("resolve", TypeRef.of(OTHER.qualifiedName()), TypeRef.of(OTHER.qualifiedName())));

            FactBase facts = settled(ledger, mutable(SUBJECT), mutable(OTHER));

            assertThat(rolesOf(facts, PORT)).containsExactly(DrivenPortType.GATEWAY);
            assertThat(facts.about(PORT, Relation.class)).isEmpty();
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about an interface no rule established as a way out")
        void aboutAnInterfaceThatIsNotAPort() {
            TypeNode plain = TypeNode.builder(PORT, TypeNature.INTERFACE)
                    .methods(List.of(method("keep", VOID, TypeRef.of(SUBJECT.qualifiedName()))))
                    .build();

            assertThat(rolesOf(settled(plain, immutable(SUBJECT)), PORT)).isEmpty();
        }

        @Test
        @DisplayName("about a way in, whose trade is the caller's business and not the hexagon's")
        void aboutAWayIn() {
            TypeNode inbound = TypeNode.builder(PORT, TypeNature.INTERFACE)
                    .annotations(List.of(Annotation.of("org.jmolecules.architecture.hexagonal.PrimaryPort")))
                    .methods(List.of(method("keep", VOID, TypeRef.of(SUBJECT.qualifiedName()))))
                    .build();

            assertThat(rolesOf(settled(inbound, immutable(SUBJECT)), PORT)).isEmpty();
        }
    }
}
