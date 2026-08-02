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
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.engine.RuleSet;
import io.hexaglue.engine.Saturation;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LocalShapeTest {

    private static final TypeId SUBJECT = TypeId.of("com.acme.Subject");

    private static Field field(String name, String type, Modifier... modifiers) {
        return Field.builder(name, TypeRef.of(type))
                .modifiers(Set.of(modifiers))
                .build();
    }

    private static Field finalField(String name, String type) {
        return field(name, type, Modifier.FINAL);
    }

    private static CodeModel declaring(TypeNature nature, List<Field> fields, Annotation... annotations) {
        return CodeModel.builder()
                .addType(TypeNode.builder(SUBJECT, nature)
                        .fields(fields)
                        .annotations(List.of(annotations))
                        .build())
                .build();
    }

    private static List<ArchKind> shapesOf(TypeNature nature, List<Field> fields) {
        return Saturation.saturate(
                        RuleSet.standard(),
                        EngineContext.of(
                                declaring(nature, fields), KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .about(SUBJECT, KindEvidence.class)
                .stream()
                .filter(evidence -> evidence.evidence().tier() == EvidenceTier.LOCAL_STRUCTURE)
                .map(KindEvidence::kind)
                .toList();
    }

    private static Classification verdictOf(TypeNature nature, List<Field> fields, Annotation... annotations) {
        return Classifier.classify(EngineContext.of(
                        declaring(nature, fields, annotations), KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .verdict(SUBJECT)
                .orElseThrow();
    }

    @Nested
    @DisplayName("reads a shape that cannot change")
    class ReadsAShapeThatCannotChange {

        @Test
        @DisplayName("as a value object, when the declaration is a record")
        void aRecordIsAValueObject() {
            assertThat(shapesOf(
                            TypeNature.RECORD,
                            List.of(finalField("amount", "long"), finalField("currency", "java.lang.String"))))
                    .containsExactly(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("as a value object, when every field of a class is final")
        void anAllFinalClassIsAValueObject() {
            assertThat(shapesOf(
                            TypeNature.CLASS,
                            List.of(finalField("amount", "long"), finalField("currency", "java.lang.String"))))
                    .containsExactly(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("as a value object, when the declaration is an enum")
        void anEnumIsAValueObject() {
            assertThat(shapesOf(TypeNature.ENUM, List.of())).containsExactly(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("and says nothing when one field can still change")
        void aMutableClassSaysNothing() {
            assertThat(shapesOf(TypeNature.CLASS, List.of(finalField("id", "long"), field("name", "java.lang.String"))))
                    .isEmpty();
        }

        @Test
        @DisplayName("and says nothing about a class holding no state at all")
        void aStatelessClassSaysNothing() {
            // Vacuous immutability is not a signal: a class with no field is as much a service,
            // a utility or a marker as it is a value.
            assertThat(shapesOf(TypeNature.CLASS, List.of())).isEmpty();
        }

        @Test
        @DisplayName("and says nothing about an interface, which holds no state to read")
        void anInterfaceSaysNothing() {
            assertThat(shapesOf(TypeNature.INTERFACE, List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("reads a single wrapped value")
    class ReadsASingleWrappedValue {

        @Test
        @DisplayName("as an identifier, and as a value object, because it is honestly both")
        void aWrapperIsBoth() {
            assertThat(shapesOf(TypeNature.RECORD, List.of(finalField("value", "java.util.UUID"))))
                    .containsExactlyInAnyOrder(ArchKind.IDENTIFIER, ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("leaving the verdict undecided, with both readings kept")
        void leavingTheVerdictUndecided() {
            // Nothing structural separates OrderId from Email. Saying so beats guessing; the
            // naming vocabulary and the repository declarations break the tie later.
            Classification verdict = verdictOf(TypeNature.RECORD, List.of(finalField("value", "java.util.UUID")));

            assertThat(verdict.kind()).isEqualTo(ArchKind.UNCLASSIFIED);
            assertThat(verdict.confidence()).isEqualTo(Confidence.LOW);
            assertThat(verdict.candidates())
                    .extracting(Candidate::kind)
                    .containsExactlyInAnyOrder(ArchKind.IDENTIFIER, ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("but not when the value can still change, which no identity does")
        void notWhenTheValueCanChange() {
            assertThat(shapesOf(TypeNature.CLASS, List.of(field("value", "java.util.UUID"))))
                    .isEmpty();
        }

        @Test
        @DisplayName("but not when the single field is a collection, which wraps nothing")
        void notWhenTheFieldIsACollection() {
            Field lines = Field.builder("lines", TypeRef.parameterized("java.util.List", TypeRef.of("com.acme.Line")))
                    .modifiers(Set.of(Modifier.FINAL))
                    .build();

            assertThat(shapesOf(TypeNature.RECORD, List.of(lines))).containsExactly(ArchKind.VALUE_OBJECT);
        }
    }

    @Nested
    @DisplayName("never outranks what the author declared")
    class NeverOutranksTheAuthor {

        @Test
        @DisplayName("a wrapper the author called a value object stays a value object")
        void aDeclaredValueObjectStaysOne() {
            Classification verdict = verdictOf(
                    TypeNature.RECORD,
                    List.of(finalField("value", "java.util.UUID")),
                    Annotation.of("org.jmolecules.ddd.annotation.ValueObject"));

            assertThat(verdict.kind()).isEqualTo(ArchKind.VALUE_OBJECT);
            assertThat(verdict.confidence()).isEqualTo(Confidence.EXPLICIT);
        }
    }
}
