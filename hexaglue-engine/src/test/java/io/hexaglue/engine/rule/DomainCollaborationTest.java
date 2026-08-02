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
import io.hexaglue.model.classification.EvidenceTier;
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

class DomainCollaborationTest {

    private static final TypeId SERVICE = TypeId.of("com.acme.Dispatch");
    private static final TypeId FLEET = TypeId.of("com.acme.Fleet");
    private static final TypeId MANIFEST = TypeId.of("com.acme.Manifest");
    private static final TypeId CALLER = TypeId.of("com.acme.Checkout");
    private static final TypeId WAY_OUT = TypeId.of("com.acme.Ledger");
    private static final String SECONDARY_PORT = "org.jmolecules.architecture.hexagonal.SecondaryPort";

    /** A value of the domain: two fields that cannot change, so no identity reading competes. */
    private static TypeNode value(TypeId id) {
        return TypeNode.builder(id, TypeNature.RECORD)
                .fields(List.of(
                        Field.builder("reference", TypeRef.of("java.lang.String"))
                                .modifiers(Set.of(Modifier.FINAL))
                                .build(),
                        Field.builder("count", TypeRef.of("int"))
                                .modifiers(Set.of(Modifier.FINAL))
                                .build()))
                .build();
    }

    private static Method over(TypeId... types) {
        List<Parameter> taken = List.of(types).stream()
                .map(type -> Parameter.of("argument" + type.simpleName(), TypeRef.of(type.qualifiedName())))
                .toList();
        return Method.builder("plan", TypeRef.of("void")).parameters(taken).build();
    }

    private static TypeNode service(List<Field> state, Method... methods) {
        return TypeNode.builder(SERVICE, TypeNature.CLASS)
                .fields(state)
                .methods(List.of(methods))
                .build();
    }

    private static TypeNode caller(TypeId held) {
        return TypeNode.builder(CALLER, TypeNature.CLASS)
                .fields(List.of(Field.of("collaborator", TypeRef.of(held.qualifiedName()))))
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
    @DisplayName("reads behaviour the domain owns but no type of it can hold")
    class ReadsBehaviourTheDomainOwns {

        @Test
        @DisplayName("as the domain service it is: no state, several domain types, and a caller inside")
        void noStateSeveralDomainTypesAndACallerInside() {
            // Four conditions, and every one of them earns its place: without state it is not an
            // entity, without ports it is not the application, over one type alone the behaviour
            // belongs to that type, and with no caller its position is not observable.
            Classification verdict = verdicts(
                            value(FLEET), value(MANIFEST), service(List.of(), over(FLEET, MANIFEST)), caller(SERVICE))
                    .verdict(SERVICE)
                    .orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.DOMAIN_SERVICE);
            assertThat(verdict.evidences()).allMatch(evidence -> evidence.tier() == EvidenceTier.GRAPH_RELATION);
            assertThat(verdict.evidences().get(0).justification()).contains("Fleet and Manifest");
        }

        @Test
        @DisplayName("even when it keeps something, as long as none of it can change")
        void evenWhenItKeepsSomethingThatCannotChange() {
            // What it keeps is a value of the domain rather than a way out, which is the whole
            // difference: the condition is about reaching outside, not about holding anything.
            List<Field> settings = List.of(Field.builder("reference", TypeRef.of(MANIFEST.qualifiedName()))
                    .modifiers(Set.of(Modifier.FINAL))
                    .build());

            assertThat(verdicts(
                                    value(FLEET),
                                    value(MANIFEST),
                                    service(settings, over(FLEET, MANIFEST)),
                                    caller(SERVICE))
                            .kindOf(SERVICE))
                    .contains(ArchKind.DOMAIN_SERVICE);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a type whose state can change, which remembers rather than computes")
        void aboutATypeWhoseStateCanChange() {
            List<Field> state = List.of(Field.of("lastPlan", TypeRef.of("java.lang.String")));

            assertThat(verdicts(value(FLEET), value(MANIFEST), service(state, over(FLEET, MANIFEST)), caller(SERVICE))
                            .kindOf(SERVICE))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a type working on a single domain type, whose behaviour belongs to that type")
        void aboutATypeWorkingOnASingleDomainType() {
            // Two types in the signatures, one of them the domain's: what the second one is
            // matters, and counting the mentions rather than reading them would classify anything
            // taking two arguments.
            assertThat(verdicts(value(FLEET), value(MANIFEST), service(List.of(), over(FLEET, CALLER)), caller(SERVICE))
                            .kindOf(SERVICE))
                    .contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a type calling a way out, which is the application layer and not the domain")
        void aboutATypeCallingAWayOut() {
            TypeNode wayOut = TypeNode.builder(WAY_OUT, TypeNature.INTERFACE)
                    .annotations(List.of(Annotation.of(SECONDARY_PORT)))
                    .build();
            List<Field> held = List.of(Field.builder("ledger", TypeRef.of(WAY_OUT.qualifiedName()))
                    .modifiers(Set.of(Modifier.FINAL))
                    .build());

            assertThat(verdicts(
                                    wayOut,
                                    value(FLEET),
                                    value(MANIFEST),
                                    service(held, over(FLEET, MANIFEST)),
                                    caller(SERVICE))
                            .kindOf(SERVICE))
                    .contains(ArchKind.APPLICATION_SERVICE);
        }

        @Test
        @DisplayName("about a type nobody inside the perimeter calls")
        void aboutATypeNobodyCalls() {
            assertThat(verdicts(value(FLEET), value(MANIFEST), service(List.of(), over(FLEET, MANIFEST)))
                            .kindOf(SERVICE))
                    .contains(ArchKind.UNCLASSIFIED);
        }
    }
}
