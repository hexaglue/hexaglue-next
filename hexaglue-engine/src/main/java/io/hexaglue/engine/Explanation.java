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
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RemediationHint;
import io.hexaglue.model.classification.RuleId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What the engine concluded about a type, and why, put into lines a host can hand to whatever it
 * writes to.
 *
 * <p>Lines rather than one block, and lines rather than a format of its own: a build plugin logs
 * one line at a time, a command line prints them joined, a report indents them under a heading.
 * None of them has to read the text back — the structure stays where it was, on the {@link
 * ArchType} and its {@link Classification}, and this rendering is a leaf of the pipeline, never a
 * stage of it.</p>
 *
 * <p>The derivation a verdict rests on stops at the round that produced it: a rule reading the
 * verdicts of the previous round has no proof of its own to cite. What such a reason names instead
 * are the types it leaned on, rendered as {@code involving}, and asking about those in turn is how
 * a reader walks back the chain.</p>
 *
 * @since 7.0.0
 */
public final class Explanation {

    private static final String REASON_INDENT = "  ";
    private static final String DETAIL_INDENT = "    ";
    private static final String DERIVATION_INDENT = "    ";
    private static final String DERIVATION_STEP = "  ";

    private Explanation() {}

    /**
     * Renders the verdict on a type: what it was classified as, how sure the engine is, every
     * reason that carried the decision, the candidates it could not separate and the remediation
     * that would settle it.
     *
     * @param type the classified type
     * @return the lines, in a stable order, never empty
     */
    public static List<String> of(ArchType type) {
        Classification verdict = type.classification();
        List<String> lines = new ArrayList<>();
        lines.add(header(type.id(), verdict));
        if (weighed(verdict)) {
            lines.add(REASON_INDENT + "signals, strongest first: " + Tiers.ranking());
        }
        if (type instanceof UnclassifiedType unclassified) {
            lines.add(REASON_INDENT + fallback(unclassified));
        }
        for (Evidence evidence : verdict.evidences()) {
            appendEvidence(lines, evidence, REASON_INDENT, type);
        }
        for (Candidate candidate : verdict.candidates()) {
            lines.add(REASON_INDENT + "candidate " + candidate.kind() + " (" + Tiers.carrying(candidate.evidences())
                    + ")");
            for (Evidence evidence : candidate.evidences()) {
                appendEvidence(lines, evidence, DETAIL_INDENT, type);
            }
        }
        for (RemediationHint hint : verdict.remediations()) {
            lines.add(REASON_INDENT + "to make it explicit: " + hint.description());
        }
        return List.copyOf(lines);
    }

    /**
     * Renders how a whole run went: the total, a tally per kind with the fallback broken down
     * under it, and one closing line on how much of the result the sources stated themselves.
     *
     * <p>A run that read nothing says so in as many words. A host that printed an empty tally
     * instead would look like it had analysed something and found nothing to say about it.</p>
     *
     * @param outcome the counted run
     * @return the lines, in a stable order, never empty
     */
    public static List<String> of(Outcome outcome) {
        if (outcome.types() == 0) {
            return List.of("no type was analysed");
        }
        List<String> lines = new ArrayList<>();
        lines.add(outcome.types() + " types analysed");
        for (Outcome.Tally<ArchKind> kind : outcome.kinds()) {
            lines.add(REASON_INDENT + kind.count() + " " + kind.subject());
            if (kind.subject() == ArchKind.UNCLASSIFIED) {
                for (Outcome.Tally<UnclassifiedCategory> category : outcome.unclassified()) {
                    lines.add(DETAIL_INDENT + category.count() + " " + category.subject());
                }
            }
        }
        lines.add(outcome.declared() + " declared, " + outcome.inferred() + " inferred, " + outcome.ambiguous()
                + " left ambiguous");
        return List.copyOf(lines);
    }

    /**
     * Renders the verdict and, under it, the tree of rules and facts the decision was derived
     * from — what a reader contesting a verdict needs and a reader accepting it does not.
     *
     * @param type the classified type
     * @return the lines of {@link #of(ArchType)} followed by the derivation
     */
    public static List<String> withDerivation(ArchType type) {
        List<String> lines = new ArrayList<>(of(type));
        lines.add(REASON_INDENT + "derivation:");
        appendProof(lines, type.classification().proof(), DERIVATION_INDENT);
        appendRules(lines, type.classification().proof());
        return List.copyOf(lines);
    }

    /**
     * Says what each rule named in the tree does, once per rule, in the order the tree first named
     * it.
     *
     * <p>Under the tree rather than inside it: a proof that fires the same rule four times would
     * repeat the sentence four times, and a title on every node would bury the derivation it is
     * supposed to make readable. A proof that derived nothing names no rule and gets no block.</p>
     */
    private static void appendRules(List<String> lines, ProofNode proof) {
        Map<RuleId, String> cited = new LinkedHashMap<>();
        collectRules(proof, cited);
        if (cited.isEmpty()) {
            return;
        }
        lines.add(REASON_INDENT + "rules cited:");
        cited.forEach((id, title) -> lines.add(DERIVATION_INDENT + id + ": " + title));
    }

    private static void collectRules(ProofNode node, Map<RuleId, String> cited) {
        node.rule().ifPresent(id -> Titles.of(id).ifPresent(title -> cited.putIfAbsent(id, title)));
        node.premises().forEach(premise -> collectRules(premise, cited));
    }

    /**
     * Answers whether a verdict actually had a pecking order to apply.
     *
     * <p>Reasons of one kind alone were never weighed against anything, and printing the ranking
     * over them would explain a comparison that never happened. Two kinds or more — across the
     * reasons that decided and the candidates that could not be separated — and the order is what
     * the reader needs to make sense of what follows.</p>
     */
    private static boolean weighed(Classification verdict) {
        Set<EvidenceTier> tiers = EnumSet.noneOf(EvidenceTier.class);
        verdict.evidences().forEach(evidence -> tiers.add(evidence.tier()));
        verdict.candidates().stream()
                .flatMap(candidate -> candidate.evidences().stream())
                .forEach(evidence -> tiers.add(evidence.tier()));
        return tiers.size() > 1;
    }

    private static String header(TypeId id, Classification verdict) {
        return id.qualifiedName() + ": " + verdict.kind() + " (" + verdict.confidence() + ", "
                + verdict.basis().name().toLowerCase(Locale.ROOT) + ")";
    }

    /**
     * Renders why a type reached no kind: the category always, and the sentence the fallback wrote
     * when it had one to write.
     */
    private static String fallback(UnclassifiedType unclassified) {
        return unclassified
                .reason()
                .map(reason -> unclassified.category() + ": " + reason)
                .orElseGet(() -> unclassified.category().toString());
    }

    /**
     * Renders one reason, and under it only what the reader does not already have.
     *
     * <p>A reason whose location is the declaration being explained points at the line the reader
     * started from, and a reason that names its own subject among the types it involves sends them
     * back to where they are. Both are dropped: what is left under a reason is a place to look
     * next, which is the only thing worth an extra line.</p>
     */
    private static void appendEvidence(List<String> lines, Evidence evidence, String indent, ArchType subject) {
        lines.add(indent + "[" + Tiers.named(evidence.tier()) + "] " + evidence.justification());
        evidence.sourceLocation()
                .filter(location ->
                        !location.equals(subject.structure().sourceLocation().orElse(null)))
                .ifPresent(location -> lines.add(indent + DERIVATION_STEP + "at " + at(location)));
        List<String> elsewhere = evidence.relatedTypes().stream()
                .filter(related -> !related.equals(subject.id()))
                .map(TypeId::qualifiedName)
                .toList();
        if (!elsewhere.isEmpty()) {
            lines.add(indent + DERIVATION_STEP + "involving " + String.join(", ", elsewhere));
        }
    }

    private static String at(SourceLocation location) {
        return location.filePath() + ":" + location.lineStart();
    }

    private static void appendProof(List<String> lines, ProofNode node, String indent) {
        lines.add(indent + node.rule().map(rule -> "[" + rule + "] ").orElse("") + node.conclusion());
        for (ProofNode premise : node.premises()) {
            appendProof(lines, premise, indent + DERIVATION_STEP);
        }
    }
}
