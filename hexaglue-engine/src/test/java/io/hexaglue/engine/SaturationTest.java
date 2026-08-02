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
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SaturationTest {

    private static final TypeId ORDER = TypeId.of("com.acme.Order");

    private static EngineContext context() {
        CodeModel code = CodeModel.builder()
                .addType(TypeNode.builder(ORDER, TypeNature.RECORD).build())
                .build();
        return EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults());
    }

    private static KindEvidence evidence(ArchKind kind, String fact) {
        return new KindEvidence(
                ORDER,
                kind,
                Evidence.of(EvidenceTier.LOCAL_STRUCTURE, Confidence.HIGH, fact, "because the shape says so"),
                0,
                ProofNode.fact(fact));
    }

    /** A rule that derives one fixed evidence and counts how many rounds ran it. */
    private static final class CountingRule implements Rule {

        private final RuleId id;
        private final Set<Predicate> reads;
        private final ArchKind kind;
        private final AtomicInteger runs = new AtomicInteger();

        CountingRule(String id, Set<Predicate> reads, ArchKind kind) {
            this.id = RuleId.of(id);
            this.reads = reads;
            this.kind = kind;
        }

        @Override
        public RuleId id() {
            return id;
        }

        @Override
        public Set<Predicate> reads() {
            return reads;
        }

        @Override
        public Set<Predicate> writes() {
            return Set.of(Predicate.EVIDENCE);
        }

        @Override
        public void apply(Derivation derivation) {
            runs.incrementAndGet();
            derivation.derive(evidence(kind, "shape of " + kind));
        }

        int runs() {
            return runs.get();
        }
    }

    @Nested
    @DisplayName("runs the rules")
    class RunsTheRules {

        @Test
        @DisplayName("until no rule has anything left to add")
        void untilNothingIsLeftToAdd() {
            CountingRule seed = new CountingRule("A-SEED", Set.of(), ArchKind.AGGREGATE_ROOT);
            CountingRule propagation = new CountingRule("B-PROPAGATION", Set.of(Predicate.EVIDENCE), ArchKind.ENTITY);

            FactBase facts = Saturation.saturate(RuleSet.of(List.of(seed, propagation)), context());

            assertThat(facts.all(KindEvidence.class))
                    .extracting(KindEvidence::kind)
                    .containsExactlyInAnyOrder(ArchKind.AGGREGATE_ROOT, ArchKind.ENTITY);
        }

        @Test
        @DisplayName("again only when a predicate they read received something new")
        void againOnlyWhenAnInputChanged() {
            CountingRule seed = new CountingRule("A-SEED", Set.of(), ArchKind.AGGREGATE_ROOT);
            CountingRule propagation = new CountingRule("B-PROPAGATION", Set.of(Predicate.EVIDENCE), ArchKind.ENTITY);

            Saturation.saturate(RuleSet.of(List.of(seed, propagation)), context());

            // Round 1 runs both; round 2 runs only the reader, whose input grew, and it adds
            // nothing new, so there is no round 3. A rule reading nothing never runs twice.
            assertThat(seed.runs()).isEqualTo(1);
            assertThat(propagation.runs()).isEqualTo(2);
        }

        @Test
        @DisplayName("in identifier order, whatever order they were registered in")
        void inIdentifierOrder() {
            Rule first = new CountingRule("A-SEED", Set.of(), ArchKind.AGGREGATE_ROOT);
            Rule second = new CountingRule("B-SEED", Set.of(), ArchKind.ENTITY);

            assertThat(RuleSet.of(List.of(second, first)).rules())
                    .extracting(Rule::id)
                    .containsExactly(RuleId.of("A-SEED"), RuleId.of("B-SEED"));
        }

        @Test
        @DisplayName("reaching the same fact base every time")
        void reachingTheSameFactBaseEveryTime() {
            RuleSet rules = RuleSet.of(List.of(
                    new CountingRule("A-SEED", Set.of(), ArchKind.AGGREGATE_ROOT),
                    new CountingRule("B-PROPAGATION", Set.of(Predicate.EVIDENCE), ArchKind.ENTITY)));

            List<String> first = Saturation.saturate(rules, context()).all(KindEvidence.class).stream()
                    .map(Fact::render)
                    .toList();
            List<String> second = Saturation.saturate(rules, context()).all(KindEvidence.class).stream()
                    .map(Fact::render)
                    .toList();

            assertThat(second).isEqualTo(first);
        }
    }

    @Nested
    @DisplayName("fails loudly")
    class FailsLoudly {

        @Test
        @DisplayName("when a rule derives a predicate it never declared writing")
        void whenARuleDerivesAnUndeclaredPredicate() {
            Rule liar = new Rule() {
                @Override
                public RuleId id() {
                    return RuleId.of("LIAR");
                }

                @Override
                public Set<Predicate> writes() {
                    return Set.of(Predicate.KNOWLEDGE);
                }

                @Override
                public void apply(Derivation derivation) {
                    derivation.derive(evidence(ArchKind.ENTITY, "shape"));
                }
            };

            assertThatThrownBy(() -> Saturation.saturate(RuleSet.of(List.of(liar)), context()))
                    .isInstanceOf(EngineException.class)
                    .hasMessageContaining("LIAR")
                    .hasMessageContaining("EVIDENCE");
        }

        @Test
        @DisplayName("when a rule keeps inventing facts instead of converging")
        void whenARuleNeverConverges() {
            Rule endless = new Rule() {
                private int derivations;

                @Override
                public RuleId id() {
                    return RuleId.of("ENDLESS");
                }

                @Override
                public Set<Predicate> reads() {
                    return Set.of(Predicate.EVIDENCE);
                }

                @Override
                public Set<Predicate> writes() {
                    return Set.of(Predicate.EVIDENCE);
                }

                @Override
                public void apply(Derivation derivation) {
                    derivations++;
                    derivation.derive(evidence(ArchKind.ENTITY, "shape " + derivations));
                }
            };

            assertThatThrownBy(() -> Saturation.saturate(RuleSet.of(List.of(endless)), context()))
                    .isInstanceOf(EngineException.class)
                    .hasMessageContaining("ENDLESS");
        }

        @Test
        @DisplayName("when two rules claim the same identifier")
        void whenTwoRulesClaimTheSameIdentifier() {
            Rule one = new CountingRule("SAME", Set.of(), ArchKind.ENTITY);
            Rule other = new CountingRule("SAME", Set.of(), ArchKind.VALUE_OBJECT);

            assertThatThrownBy(() -> RuleSet.of(List.of(one, other)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SAME");
        }
    }
}
