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
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConsumedContractTest {

    private static final TypeId CONTRACT = TypeId.of("com.acme.Ledger");
    private static final TypeId CONSUMER = TypeId.of("com.acme.Checkout");
    private static final TypeId IMPLEMENTER = TypeId.of("com.acme.LedgerBook");
    private static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";

    private static TypeNode contract() {
        return TypeNode.builder(CONTRACT, TypeNature.INTERFACE).build();
    }

    /**
     * A type of the core keeping the contract as a collaborator. It descends from something
     * unrelated, because most types do and none of that is the relation being read.
     */
    private static TypeNode holder(TypeId id, TypeRef held) {
        return TypeNode.builder(id, TypeNature.CLASS)
                .superClass(TypeRef.of("com.acme.Anything"))
                .fields(List.of(Field.builder("collaborator", held)
                        .modifiers(Set.of(Modifier.FINAL))
                        .build()))
                .build();
    }

    /** A type the outer ring owns: it holds a way out, so W1-DR places it there. */
    private static TypeNode adapter(TypeId id, TypeRef alsoHeld) {
        return TypeNode.builder(id, TypeNature.CLASS)
                .fields(List.of(Field.of("store", TypeRef.of(ENTITY_MANAGER)), Field.of("collaborator", alsoHeld)))
                .build();
    }

    private static TypeNode implementer(TypeId id) {
        return TypeNode.builder(id, TypeNature.CLASS)
                .interfaces(List.of(TypeRef.of(CONTRACT.qualifiedName())))
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
        return code.addType(TypeNode.externalStub(TypeId.of(ENTITY_MANAGER), TypeNature.INTERFACE))
                .addType(TypeNode.externalStub(TypeId.of("com.acme.Anything"), TypeNature.CLASS))
                .build();
    }

    @Nested
    @DisplayName("reads an interface the core holds and nothing inside implements")
    class ReadsAnInterfaceTheCoreHolds {

        @Test
        @DisplayName("as the driven port it is, without reading a single character of its name")
        void aContractTheCoreCallsIsADrivenPort() {
            Classification verdict = verdicts(model(contract(), holder(CONSUMER, TypeRef.of(CONTRACT.qualifiedName()))))
                    .verdict(CONTRACT)
                    .orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVEN_PORT);
            assertThat(verdict.direction()).contains(PortDirection.DRIVEN);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
            assertThat(verdict.evidences()).allMatch(evidence -> evidence.tier() == EvidenceTier.GRAPH_RELATION);
        }

        @Test
        @DisplayName("when the contract is handed to the consumer's constructor rather than kept")
        void whenTheContractIsHandedToTheConstructor() {
            TypeNode consumer = TypeNode.builder(CONSUMER, TypeNature.CLASS)
                    .constructors(List.of(Constructor.of(
                            List.of(Parameter.of("collaborator", TypeRef.of(CONTRACT.qualifiedName()))))))
                    .build();

            assertThat(verdicts(model(contract(), consumer)).kindOf(CONTRACT)).contains(ArchKind.DRIVEN_PORT);
        }

        @Test
        @DisplayName("even when the consumer wraps it, because the wrapping is not the relation")
        void evenWhenTheConsumerWrapsIt() {
            TypeRef wrapped = TypeRef.parameterized("java.util.List", TypeRef.of(CONTRACT.qualifiedName()));

            assertThat(verdicts(model(contract(), holder(CONSUMER, wrapped))).kindOf(CONTRACT))
                    .contains(ArchKind.DRIVEN_PORT);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about an interface the core implements as well as holds: that is an internal contract")
        void aboutAnInterfaceTheCoreAlsoImplements() {
            // The anti-rule of the wave. A hole in the wall is a contract whose other side lies
            // outside the hexagon; an interface the core both writes and calls is a contract it
            // has with itself, and reading it as a boundary would invent a frontier where the
            // code has none.
            CodeModel code =
                    model(contract(), holder(CONSUMER, TypeRef.of(CONTRACT.qualifiedName())), implementer(IMPLEMENTER));

            assertThat(verdicts(code).kindOf(CONTRACT)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an interface only the outer ring holds, which reaches nothing from the core")
        void aboutAnInterfaceOnlyAnAdapterHolds() {
            CodeModel code = model(contract(), adapter(CONSUMER, TypeRef.of(CONTRACT.qualifiedName())));

            assertThat(verdicts(code).kindOf(CONSUMER)).contains(ArchKind.DRIVEN_ADAPTER);
            assertThat(verdicts(code).kindOf(CONTRACT)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an interface the core also implements through something it inherits")
        void aboutAnInterfaceImplementedThroughASupertype() {
            // The implementation need not name the contract itself. A class descending from a base
            // that fulfils it fulfils it too, and the seam is exactly as internal either way.
            TypeNode descendant = TypeNode.builder(IMPLEMENTER, TypeNature.CLASS)
                    .superClass(TypeRef.of("com.acme.AbstractLedger"))
                    .build();
            CodeModel code = CodeModel.builder()
                    .addType(contract())
                    .addType(holder(CONSUMER, TypeRef.of(CONTRACT.qualifiedName())))
                    .addType(descendant)
                    .addType(TypeNode.externalStub(TypeId.of("com.acme.AbstractLedger"), TypeNature.CLASS))
                    .addType(TypeNode.externalStub(TypeId.of("com.acme.Anything"), TypeNature.CLASS))
                    .supertypes(IMPLEMENTER, List.of(TypeId.of("com.acme.AbstractLedger"), CONTRACT))
                    .build();

            assertThat(verdicts(code).kindOf(CONTRACT)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an interface nobody in the perimeter holds, next to a type holding other things")
        void aboutAnInterfaceNobodyHolds() {
            CodeModel code = model(contract(), holder(CONSUMER, TypeRef.of("java.lang.String")));

            assertThat(verdicts(code).kindOf(CONTRACT)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an interface named only in a constant, which nobody collaborates with")
        void aboutAnInterfaceNamedInAConstant() {
            // A constant belongs to the type that declares it, not to what its instances work
            // with. Reading it as a collaboration would make every registry of defaults a caller.
            TypeNode declarer = TypeNode.builder(CONSUMER, TypeNature.CLASS)
                    .fields(List.of(Field.builder("fallback", TypeRef.of(CONTRACT.qualifiedName()))
                            .modifiers(Set.of(Modifier.STATIC, Modifier.FINAL))
                            .build()))
                    .build();

            assertThat(verdicts(model(contract(), declarer)).kindOf(CONTRACT)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a class the core holds, because a port is a contract and a class is not")
        void aboutAClassTheCoreHolds() {
            TypeId collaborator = TypeId.of("com.acme.Tally");
            CodeModel code = model(
                    TypeNode.builder(collaborator, TypeNature.CLASS).build(),
                    holder(CONSUMER, TypeRef.of(collaborator.qualifiedName())));

            assertThat(verdicts(code).kindOf(collaborator)).contains(ArchKind.UNCLASSIFIED);
        }
    }
}
