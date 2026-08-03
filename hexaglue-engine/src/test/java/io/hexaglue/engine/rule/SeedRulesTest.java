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

import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.FactBase;
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.engine.KnowledgeAssertion;
import io.hexaglue.engine.RuleSet;
import io.hexaglue.engine.Saturation;
import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.config.GenerationConfig;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.config.ModulesConfig;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.model.declaration.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SeedRulesTest {

    private static final TypeId ORDER = TypeId.of("com.acme.Order");
    private static final String JMOLECULES_AGGREGATE = "org.jmolecules.ddd.annotation.AggregateRoot";
    private static final String JMOLECULES_VALUE_OBJECT = "org.jmolecules.ddd.types.ValueObject";
    private static final String JMOLECULES_ENTITY = "org.jmolecules.ddd.types.Entity";
    private static final String JPA_ENTITY = "jakarta.persistence.Entity";
    private static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";

    private static FactBase saturate(CodeModel code, HexaGlueConfig config) {
        return Saturation.saturate(RuleSet.standard(), EngineContext.of(code, KnowledgePacks.embedded(), config));
    }

    private static FactBase saturate(CodeModel code) {
        return saturate(code, HexaGlueConfig.defaults());
    }

    private static CodeModel annotated(String qualifiedName, String... annotations) {
        return CodeModel.builder()
                .addType(TypeNode.builder(TypeId.of(qualifiedName), TypeNature.CLASS)
                        .annotations(List.of(annotations).stream()
                                .map(Annotation::of)
                                .toList())
                        .build())
                .build();
    }

    /** A type implementing an interface, with the closure the frontend hands over. */
    private static CodeModel implementing(String qualifiedName, String supertype, List<TypeId> closure) {
        TypeNode type = TypeNode.builder(TypeId.of(qualifiedName), TypeNature.CLASS)
                .interfaces(List.of(TypeRef.of(supertype)))
                .build();
        CodeModel.Builder code = CodeModel.builder().addType(type).supertypes(type.id(), closure);
        closure.forEach(id -> code.addType(TypeNode.externalStub(id, TypeNature.INTERFACE)));
        return code.build();
    }

    @Nested
    @DisplayName("asserts what the packs recognize")
    class AssertsWhatThePacksRecognize {

        @Test
        @DisplayName("on a type of the perimeter")
        void onATypeOfThePerimeter() {
            FactBase facts = saturate(annotated("com.acme.Order", JPA_ENTITY));

            assertThat(facts.all(KnowledgeAssertion.class))
                    .extracting(assertion -> assertion.finding().fact())
                    .containsExactly(KnowledgeFact.PERSISTENCE_MODEL);
        }

        @Test
        @DisplayName("on a classpath type too, which is how a tool held by a type is recognized")
        void onAClasspathTypeToo() {
            CodeModel code = CodeModel.builder()
                    .addType(TypeNode.builder(ORDER, TypeNature.CLASS).build())
                    .addType(TypeNode.externalStub(TypeId.of(ENTITY_MANAGER), TypeNature.INTERFACE))
                    .build();

            assertThat(saturate(code).about(TypeId.of(ENTITY_MANAGER), KnowledgeAssertion.class))
                    .extracting(assertion -> assertion.finding().fact())
                    .containsExactly(KnowledgeFact.INFRA_DEPENDENCY);
        }

        @Test
        @DisplayName("citing the pack and the symbol, so the verdict can be traced back to them")
        void citingThePackAndTheSymbol() {
            FactBase facts = saturate(annotated("com.acme.Order", JPA_ENTITY));

            assertThat(facts.all(KnowledgeAssertion.class))
                    .singleElement()
                    .extracting(KnowledgeAssertion::render)
                    .isEqualTo("PERSISTENCE_MODEL(com.acme.Order) [jakarta:" + JPA_ENTITY + "]");
        }
    }

    @Nested
    @DisplayName("takes the author's word")
    class TakesTheAuthorsWord {

        @Test
        @DisplayName("when an intent annotation declares the kind")
        void whenAnIntentAnnotationDeclaresTheKind() {
            FactBase facts = saturate(annotated("com.acme.Order", JMOLECULES_AGGREGATE));

            assertThat(facts.about(ORDER, KindEvidence.class)).singleElement().satisfies(evidence -> {
                assertThat(evidence.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
                assertThat(evidence.evidence().tier()).isEqualTo(EvidenceTier.DECLARED_INTENT);
                assertThat(evidence.evidence().force()).isEqualTo(Confidence.EXPLICIT);
                assertThat(evidence.distance()).isZero();
            });
        }

        @Test
        @DisplayName("when an intent interface declares it, and B4's canonical case is one of those")
        void whenAnIntentInterfaceDeclaresIt() {
            CodeModel code = implementing(
                    "com.acme.Money", JMOLECULES_VALUE_OBJECT, List.of(TypeId.of(JMOLECULES_VALUE_OBJECT)));

            assertThat(saturate(code).about(TypeId.of("com.acme.Money"), KindEvidence.class))
                    .extracting(KindEvidence::kind)
                    .containsExactly(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("measuring how far the inherited signal sits, so a tie can be broken later")
        void measuringHowFarTheInheritedSignalSits() {
            // org.jmolecules.ddd.types.AggregateRoot extends Entity: both are declared, and only
            // the distance tells which one the author wrote down.
            CodeModel code = implementing(
                    "com.acme.Order",
                    "org.jmolecules.ddd.types.AggregateRoot",
                    List.of(TypeId.of("org.jmolecules.ddd.types.AggregateRoot"), TypeId.of(JMOLECULES_ENTITY)));

            assertThat(saturate(code).about(ORDER, KindEvidence.class))
                    .extracting(KindEvidence::kind, KindEvidence::distance)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(ArchKind.AGGREGATE_ROOT, 1),
                            org.assertj.core.groups.Tuple.tuple(ArchKind.ENTITY, 2));
        }

        @Test
        @DisplayName("when the configuration declares the kind")
        void whenTheConfigurationDeclaresTheKind() {
            HexaGlueConfig config = new HexaGlueConfig(
                    AnalysisScope.everything(),
                    new ClassificationConfig(Map.of(ORDER, ArchKind.AGGREGATE_ROOT), Map.of()),
                    ValidationConfig.defaults(),
                    GenerationConfig.defaults(),
                    ModulesConfig.defaults());
            CodeModel code = CodeModel.builder()
                    .addType(TypeNode.builder(ORDER, TypeNature.CLASS).build())
                    .build();

            assertThat(saturate(code, config).about(ORDER, KindEvidence.class))
                    .singleElement()
                    .satisfies(evidence -> {
                        assertThat(evidence.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
                        assertThat(evidence.evidence().force()).isEqualTo(Confidence.EXPLICIT);
                        assertThat(evidence.proof().rule()).contains(ConfiguredKind.ID);
                    });
        }

        @Test
        @DisplayName("but never about a type the scope leaves out")
        void neverAboutATypeTheScopeLeavesOut() {
            HexaGlueConfig config = new HexaGlueConfig(
                    new AnalysisScope(Optional.of("com.other"), List.of(), List.of()),
                    new ClassificationConfig(Map.of(ORDER, ArchKind.AGGREGATE_ROOT), Map.of()),
                    ValidationConfig.defaults(),
                    GenerationConfig.defaults(),
                    ModulesConfig.defaults());
            CodeModel code = CodeModel.builder()
                    .addType(TypeNode.builder(ORDER, TypeNature.CLASS)
                            .annotations(List.of(Annotation.of(JMOLECULES_AGGREGATE)))
                            .build())
                    .build();

            assertThat(saturate(code, config).all(KindEvidence.class)).isEmpty();
        }
    }

    @Nested
    @DisplayName("says nothing about a kind")
    class SaysNothingAboutAKind {

        @Test
        @DisplayName("when the only signal is a persistence mapping")
        void whenTheOnlySignalIsAPersistenceMapping() {
            FactBase facts = saturate(annotated("com.acme.Order", JPA_ENTITY));

            assertThat(facts.all(KindEvidence.class)).isEmpty();
            assertThat(facts.all(KnowledgeAssertion.class)).hasSize(1);
        }
    }
}
