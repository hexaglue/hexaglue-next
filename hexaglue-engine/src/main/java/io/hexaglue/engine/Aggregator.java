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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Turns the evidences held about a type into the verdict on that type.
 *
 * <p>This is where the engine stops collecting and commits, and it is deliberately not a rule: a
 * rule that could decide would be able to decide first, and first-match is the failure mode this
 * design exists to remove. Every signal is emitted, all of them are weighed together, and the
 * weighing happens once.</p>
 *
 * <p>The weighing is lexicographic by tier. A kind supported at a stronger tier wins over a kind
 * supported at a weaker one, however many weak signals the latter accumulates — which is how a
 * name can inform a verdict without ever being able to overturn a structural fact. At equal tier
 * the signals are counted, and if the count still ties, the nearer signal wins: a type
 * implementing an interface that extends another declares both, and the one it named is the one
 * the author meant.</p>
 *
 * <p>When nothing separates the two best candidates, the engine says so instead of picking. The
 * type is left unclassified and both candidates are kept with their evidences — an honest
 * "I could not tell" that the report can act on, rather than a coin flip nobody can audit.</p>
 *
 * @since 7.0.0
 */
public final class Aggregator {

    /** The published identifier of the decision step, which every verdict's proof names. */
    public static final RuleId ID = RuleId.of("DECISION");

    /**
     * Beyond nine signals at one tier the case is already overwhelming, and counting further
     * would let a pile of weak signals overflow into the weight of a stronger tier.
     */
    private static final int MAX_PER_TIER = 9;

    /**
     * One decimal place per tier, strongest tier on the highest place: a count at a stronger tier
     * outweighs any count below it, and counts at the same tier simply add up.
     */
    private static final Map<EvidenceTier, Integer> TIER_WEIGHT = tierWeights();

    private Aggregator() {}

    private static Map<EvidenceTier, Integer> tierWeights() {
        List<EvidenceTier> tiers = List.of(EvidenceTier.values());
        Map<EvidenceTier, Integer> weights = new EnumMap<>(EvidenceTier.class);
        int weight = 1;
        for (int rank = tiers.size() - 1; rank >= 0; rank--) {
            weights.put(tiers.get(rank), weight);
            weight *= 10;
        }
        return Collections.unmodifiableMap(weights);
    }

    /**
     * Decides the verdict on every type of the perimeter.
     *
     * @param facts the evidences held at this point of the analysis
     * @param perimeter the types owed a verdict
     * @return the verdicts, one per type of the perimeter
     */
    public static Verdicts decide(FactBase facts, Perimeter perimeter) {
        Objects.requireNonNull(facts, "facts must not be null");
        Objects.requireNonNull(perimeter, "perimeter must not be null");
        Map<TypeId, Classification> verdicts = new TreeMap<>();
        for (TypeNode type : perimeter.types()) {
            verdicts.put(type.id(), decideOne(type.id(), facts.about(type.id(), KindEvidence.class)));
        }
        return Verdicts.of(verdicts);
    }

    private static Classification decideOne(TypeId subject, List<KindEvidence> evidences) {
        if (evidences.isEmpty()) {
            return silence(subject);
        }
        List<Contender> contenders = rank(evidences);
        Contender best = contenders.get(0);
        if (contenders.size() > 1 && !separates(best, contenders.get(1))) {
            return ambiguous(subject, contenders);
        }
        return decided(best);
    }

    /**
     * Groups the evidences by the kind they support and orders the groups best first.
     */
    private static List<Contender> rank(List<KindEvidence> evidences) {
        Map<ArchKind, List<KindEvidence>> byKind = new LinkedHashMap<>();
        for (KindEvidence evidence : evidences) {
            byKind.computeIfAbsent(evidence.kind(), kind -> new ArrayList<>()).add(evidence);
        }
        return byKind.entrySet().stream()
                .map(entry -> Contender.of(entry.getKey(), entry.getValue()))
                .sorted(Comparator.<Contender>comparingInt(Contender::score)
                        .reversed()
                        .thenComparingInt(Contender::distance)
                        .thenComparing(contender -> contender.kind().name()))
                .toList();
    }

    /**
     * Answers whether the leader beats the runner-up by the smallest margin that means anything:
     * one signal at the deciding tier, or one inheritance step.
     */
    private static boolean separates(Contender best, Contender runnerUp) {
        return best.score() != runnerUp.score() || best.distance() != runnerUp.distance();
    }

    private static Classification decided(Contender winner) {
        List<Evidence> evidences = winner.evidences();
        Classification.Builder verdict = Classification.builder(
                        winner.kind(), confidenceOf(evidences), basisOf(evidences), winner.proof(ID))
                .evidences(evidences);
        directionOf(winner.kind()).ifPresent(verdict::direction);
        return verdict.build();
    }

    private static Classification ambiguous(TypeId subject, List<Contender> contenders) {
        List<Candidate> candidates =
                contenders.stream().map(Contender::candidate).toList();
        String tied = candidates.stream()
                .map(candidate -> candidate.kind().toString())
                .reduce((left, right) -> left + " and " + right)
                .orElseThrow();
        ProofNode proof = new ProofNode(
                ArchKind.UNCLASSIFIED + "(" + subject.qualifiedName() + ") [AMBIGUOUS between " + tied + "]",
                Optional.of(ID),
                contenders.stream()
                        .flatMap(contender -> contender.proofs().stream())
                        .toList());
        return Classification.builder(ArchKind.UNCLASSIFIED, Confidence.LOW, Basis.INFERRED, proof)
                .candidates(candidates)
                .build();
    }

    private static Classification silence(TypeId subject) {
        return Classification.builder(
                        ArchKind.UNCLASSIFIED,
                        Confidence.LOW,
                        Basis.INFERRED,
                        ProofNode.fact("no signal about " + subject.qualifiedName()))
                .build();
    }

    /**
     * Returns the strongest force the winning signals carry. Each force is already capped by its
     * tier, so a verdict reached on naming alone can never claim more than the tier allows.
     */
    private static Confidence confidenceOf(List<Evidence> evidences) {
        return evidences.stream()
                .map(Evidence::force)
                .min(Comparator.naturalOrder())
                .orElse(Confidence.LOW);
    }

    private static Basis basisOf(List<Evidence> evidences) {
        boolean declared = evidences.stream().anyMatch(evidence -> evidence.tier() == EvidenceTier.DECLARED_INTENT);
        return declared ? Basis.DECLARED : Basis.INFERRED;
    }

    private static Optional<PortDirection> directionOf(ArchKind kind) {
        return switch (kind) {
            case DRIVING_PORT, DRIVING_ADAPTER -> Optional.of(PortDirection.DRIVING);
            case DRIVEN_PORT, DRIVEN_ADAPTER -> Optional.of(PortDirection.DRIVEN);
            default -> Optional.empty();
        };
    }

    /**
     * One kind in the running, with everything said for it.
     *
     * @param kind the candidate kind
     * @param signals the signals supporting it, strongest tier first
     * @param score the lexicographic weight of its tier profile
     * @param distance how near the nearest of its signals was found
     */
    private record Contender(ArchKind kind, List<KindEvidence> signals, int score, int distance) {

        static Contender of(ArchKind kind, List<KindEvidence> signals) {
            List<KindEvidence> ordered = signals.stream()
                    .sorted(Comparator.comparing(
                                    (KindEvidence signal) -> signal.evidence().tier())
                            .thenComparingInt(KindEvidence::distance)
                            .thenComparing(KindEvidence::render))
                    .toList();
            return new Contender(kind, ordered, score(ordered), nearest(ordered));
        }

        private static int score(List<KindEvidence> signals) {
            Map<EvidenceTier, Integer> counts = new EnumMap<>(EvidenceTier.class);
            for (KindEvidence signal : signals) {
                counts.merge(signal.evidence().tier(), 1, Integer::sum);
            }
            int score = 0;
            for (Map.Entry<EvidenceTier, Integer> counted : counts.entrySet()) {
                score += Math.min(counted.getValue(), MAX_PER_TIER) * TIER_WEIGHT.getOrDefault(counted.getKey(), 0);
            }
            return score;
        }

        private static int nearest(List<KindEvidence> signals) {
            return signals.stream().mapToInt(KindEvidence::distance).min().orElseThrow();
        }

        List<Evidence> evidences() {
            return signals.stream().map(KindEvidence::evidence).toList();
        }

        List<ProofNode> proofs() {
            return signals.stream().map(KindEvidence::proof).toList();
        }

        /**
         * States the decision in the terms it can be argued with: which tiers spoke and how many
         * times, and how far away the nearest signal sat when it did not sit on the type itself.
         * The score is left out on purpose — it orders candidates, it does not explain one.
         */
        ProofNode proof(RuleId rule) {
            String reach = distance == 0 ? "" : ", nearest " + distance + " steps away";
            return new ProofNode(
                    kind + "(" + signals.get(0).subject().qualifiedName() + ") [decided on "
                            + Tiers.carrying(evidences()) + reach + "]",
                    Optional.of(rule),
                    proofs());
        }

        Candidate candidate() {
            return new Candidate(kind, score, evidences());
        }
    }
}
