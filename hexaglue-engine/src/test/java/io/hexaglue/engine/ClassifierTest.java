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

package io.hexaglue.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClassifierTest {

    private static final TypeId ORDER = TypeId.of("com.acme.Order");
    private static final TypeId LINE = TypeId.of("com.acme.OrderLine");
    private static final String JMOLECULES_AGGREGATE = "org.jmolecules.ddd.annotation.AggregateRoot";

    private static EngineContext context(CodeModel code) {
        return EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults());
    }

    private static CodeModel sources() {
        return CodeModel.builder()
                .addType(TypeNode.builder(ORDER, TypeNature.CLASS)
                        .annotations(List.of(Annotation.of(JMOLECULES_AGGREGATE)))
                        .build())
                .addType(TypeNode.builder(LINE, TypeNature.RECORD).build())
                .build();
    }

    /** A rule that speaks about a neighbour only once that neighbour has been classified. */
    private static final class Neighbour implements Rule {

        @Override
        public RuleId id() {
            return RuleId.of("TEST-NEIGHBOUR");
        }

        @Override
        public Set<Predicate> writes() {
            return Set.of(Predicate.EVIDENCE);
        }

        @Override
        public void apply(Derivation derivation) {
            if (derivation
                    .kindOf(ORDER)
                    .filter(kind -> kind == ArchKind.AGGREGATE_ROOT)
                    .isEmpty()) {
                return;
            }
            derivation.derive(KindEvidence.derived(
                    LINE,
                    ArchKind.ENTITY,
                    Evidence.of(
                            EvidenceTier.GRAPH_RELATION,
                            Confidence.HIGH,
                            "owned by an aggregate",
                            "com.acme.Order owns it"),
                    0,
                    id(),
                    ProofNode.fact("AGGREGATE_ROOT(com.acme.Order)")));
        }
    }

    @Nested
    @DisplayName("classifies")
    class Classifies {

        @Test
        @DisplayName("every type of the perimeter, with no silent disappearance")
        void everyTypeOfThePerimeter() {
            Verdicts verdicts = Classifier.classify(context(sources()));

            assertThat(verdicts.subjects()).containsExactly(ORDER, LINE);
        }

        @Test
        @DisplayName("on the author's declaration when there is one")
        void onTheAuthorsDeclaration() {
            Classification verdict =
                    Classifier.classify(context(sources())).verdict(ORDER).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
            assertThat(verdict.confidence()).isEqualTo(Confidence.EXPLICIT);
            assertThat(verdict.basis()).isEqualTo(Basis.DECLARED);
        }

        @Test
        @DisplayName("as unclassified when nothing says anything")
        void unclassifiedWhenNothingSaysAnything() {
            Classification verdict =
                    Classifier.classify(context(sources())).verdict(LINE).orElseThrow();

            assertThat(verdict.kind()).isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("to the same verdicts on a second run")
        void identicallyOnASecondRun() {
            assertThat(Classifier.classify(context(sources()))).isEqualTo(Classifier.classify(context(sources())));
        }
    }

    @Nested
    @DisplayName("keeps going")
    class KeepsGoing {

        @Test
        @DisplayName("until a rule reading its neighbour's verdict has had its say")
        void untilANeighbourRuleHasHadItsSay() {
            RuleSet rules = RuleSet.of(Stream.concat(RuleSet.standard().rules().stream(), Stream.of(new Neighbour()))
                    .toList());

            Verdicts verdicts = Classifier.classify(rules, context(sources()));

            assertThat(verdicts.kindOf(LINE)).contains(ArchKind.ENTITY);
        }
    }

    @Nested
    @DisplayName("fails loudly")
    class FailsLoudly {

        @Test
        @DisplayName("when two readings keep undoing each other")
        void whenTwoReadingsKeepUndoingEachOther() {
            // Speaks for a kind only while the type has none: deciding it silences the rule, and
            // losing the evidence brings the type back to unclassified.
            Rule oscillating = new Rule() {
                @Override
                public RuleId id() {
                    return RuleId.of("TEST-OSCILLATING");
                }

                @Override
                public Set<Predicate> writes() {
                    return Set.of(Predicate.EVIDENCE);
                }

                @Override
                public void apply(Derivation derivation) {
                    if (derivation
                            .kindOf(LINE)
                            .filter(kind -> kind != ArchKind.UNCLASSIFIED)
                            .isPresent()) {
                        return;
                    }
                    derivation.derive(KindEvidence.derived(
                            LINE,
                            ArchKind.VALUE_OBJECT,
                            Evidence.of(EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, "is a record", "shape"),
                            0,
                            id(),
                            ProofNode.fact("record com.acme.OrderLine")));
                }
            };

            assertThatThrownBy(() -> Classifier.classify(RuleSet.of(List.of(oscillating)), context(sources())))
                    .isInstanceOf(EngineException.class)
                    .hasMessageContaining("com.acme.OrderLine");
        }
    }
}
