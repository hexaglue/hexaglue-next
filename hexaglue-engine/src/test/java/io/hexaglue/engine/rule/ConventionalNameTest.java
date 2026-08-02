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
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.config.GenerationConfig;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConventionalNameTest {

    private static CodeModel wrapper(String qualifiedName) {
        return CodeModel.builder()
                .addType(TypeNode.builder(TypeId.of(qualifiedName), TypeNature.RECORD)
                        .fields(List.of(Field.builder("value", TypeRef.of("java.util.UUID"))
                                .modifiers(Set.of(Modifier.FINAL))
                                .build()))
                        .build())
                .build();
    }

    private static CodeModel bare(String qualifiedName, TypeNature nature) {
        return CodeModel.builder()
                .addType(TypeNode.builder(TypeId.of(qualifiedName), nature).build())
                .build();
    }

    private static HexaGlueConfig with(ClassificationConfig classification) {
        return new HexaGlueConfig(
                AnalysisScope.everything(), classification, ValidationConfig.defaults(), GenerationConfig.defaults());
    }

    private static List<ArchKind> namesOf(CodeModel code, HexaGlueConfig config) {
        return Saturation.saturate(RuleSet.standard(), EngineContext.of(code, KnowledgePacks.embedded(), config))
                .all(KindEvidence.class)
                .stream()
                .filter(evidence -> evidence.evidence().tier() == EvidenceTier.NAMING)
                .map(KindEvidence::kind)
                .toList();
    }

    private static Classification verdictOf(CodeModel code, String qualifiedName) {
        return Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .verdict(TypeId.of(qualifiedName))
                .orElseThrow();
    }

    @Nested
    @DisplayName("reads the vocabulary the user configured")
    class ReadsTheConfiguredVocabulary {

        @Test
        @DisplayName("recognizing a suffix the code base uses for a kind")
        void recognizingASuffix() {
            assertThat(namesOf(bare("com.acme.OrderRepository", TypeNature.INTERFACE), HexaGlueConfig.defaults()))
                    .containsExactly(ArchKind.DRIVEN_PORT);
        }

        @Test
        @DisplayName("reading the longest spelling only, so one name weighs once")
        void readingTheLongestSpellingOnly() {
            assertThat(namesOf(bare("com.acme.OrderApplicationService", TypeNature.CLASS), HexaGlueConfig.defaults()))
                    .containsExactly(ArchKind.APPLICATION_SERVICE);
        }

        @Test
        @DisplayName("and a convention the user removed really does stop applying")
        void aRemovedConventionStopsApplying() {
            HexaGlueConfig silent = with(ClassificationConfig.silent());

            assertThat(namesOf(bare("com.acme.OrderRepository", TypeNature.INTERFACE), silent))
                    .isEmpty();
        }

        @Test
        @DisplayName("and a convention the user invented is understood")
        void anInventedConventionIsUnderstood() {
            HexaGlueConfig own = with(new ClassificationConfig(Map.of(), Map.of(ArchKind.IDENTIFIER, List.of("Ref"))));

            assertThat(namesOf(bare("com.acme.OrderRef", TypeNature.RECORD), own))
                    .containsExactly(ArchKind.IDENTIFIER);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a type whose whole name is the convention")
        void aboutATypeNamedAfterTheConventionItself() {
            assertThat(namesOf(bare("com.acme.Event", TypeNature.RECORD), HexaGlueConfig.defaults()))
                    .isEmpty();
        }

        @Test
        @DisplayName("about a name that merely looks like a suffix in another case")
        void aboutANameThatOnlyLooksLikeASuffix() {
            // Grid ends in "id", not in "Id": a convention is a word, not a letter sequence.
            assertThat(namesOf(bare("com.acme.Grid", TypeNature.CLASS), HexaGlueConfig.defaults()))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("settles a tie, and only a tie")
    class SettlesATieAndOnlyATie {

        @Test
        @DisplayName("naming an identifier the shape alone could not tell from a value object")
        void namingAnIdentifierTheShapeCouldNotTell() {
            // The shape says both IDENTIFIER and VALUE_OBJECT; the name breaks the deadlock.
            Classification verdict = verdictOf(wrapper("com.acme.OrderId"), "com.acme.OrderId");

            assertThat(verdict.kind()).isEqualTo(ArchKind.IDENTIFIER);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("leaving a wrapper nobody named as undecided as it was")
        void leavingAnUnnamedWrapperUndecided() {
            assertThat(verdictOf(wrapper("com.acme.Email"), "com.acme.Email").kind())
                    .isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("and never overturning what a framework said")
        void neverOverturningAFrameworkSignal() {
            // A repository declares its identifier; a value-object-sounding name cannot undo it.
            CodeModel code = CodeModel.builder()
                    .addType(TypeNode.builder(TypeId.of("com.acme.OrderRepository"), TypeNature.INTERFACE)
                            .interfaces(List.of(TypeRef.parameterized(
                                    "org.springframework.data.repository.CrudRepository",
                                    TypeRef.of("com.acme.Order"),
                                    TypeRef.of("com.acme.OrderService"))))
                            .build())
                    .addType(TypeNode.builder(TypeId.of("com.acme.Order"), TypeNature.CLASS)
                            .build())
                    .addType(TypeNode.builder(TypeId.of("com.acme.OrderService"), TypeNature.RECORD)
                            .fields(List.of(Field.builder("value", TypeRef.of("java.util.UUID"))
                                    .modifiers(Set.of(Modifier.FINAL))
                                    .build()))
                            .build())
                    .addType(TypeNode.externalStub(
                            TypeId.of("org.springframework.data.repository.CrudRepository"), TypeNature.INTERFACE))
                    .addType(TypeNode.externalStub(
                            TypeId.of("org.springframework.data.repository.Repository"), TypeNature.INTERFACE))
                    .supertypes(
                            TypeId.of("com.acme.OrderRepository"),
                            List.of(
                                    TypeId.of("org.springframework.data.repository.CrudRepository"),
                                    TypeId.of("org.springframework.data.repository.Repository")))
                    .supertypes(
                            TypeId.of("org.springframework.data.repository.CrudRepository"),
                            List.of(TypeId.of("org.springframework.data.repository.Repository")))
                    .build();

            assertThat(verdictOf(code, "com.acme.OrderService").kind()).isEqualTo(ArchKind.IDENTIFIER);
        }
    }
}
