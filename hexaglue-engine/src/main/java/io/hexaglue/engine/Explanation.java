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

import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RemediationHint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
        if (type instanceof UnclassifiedType unclassified) {
            lines.add(REASON_INDENT + fallback(unclassified));
        }
        for (Evidence evidence : verdict.evidences()) {
            appendEvidence(lines, evidence, REASON_INDENT);
        }
        for (Candidate candidate : verdict.candidates()) {
            lines.add(REASON_INDENT + "candidate " + candidate.kind() + " (score " + candidate.score() + ")");
            for (Evidence evidence : candidate.evidences()) {
                appendEvidence(lines, evidence, DETAIL_INDENT);
            }
        }
        for (RemediationHint hint : verdict.remediations()) {
            lines.add(REASON_INDENT + "to make it explicit: " + hint.description());
        }
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
        return List.copyOf(lines);
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

    private static void appendEvidence(List<String> lines, Evidence evidence, String indent) {
        lines.add(indent + "[" + evidence.tier().code() + "] " + evidence.justification());
        evidence.sourceLocation().ifPresent(location -> lines.add(indent + DERIVATION_STEP + "at " + at(location)));
        if (!evidence.relatedTypes().isEmpty()) {
            lines.add(indent + DERIVATION_STEP + "involving "
                    + evidence.relatedTypes().stream()
                            .map(TypeId::qualifiedName)
                            .collect(Collectors.joining(", ")));
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
