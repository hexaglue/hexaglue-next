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

import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.RemediationAction;
import io.hexaglue.model.classification.RemediationHint;
import io.hexaglue.model.classification.RemediationImpact;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Optional;

/**
 * Why a type of the perimeter has no place in the hexagon, said in terms its author can act on.
 *
 * <p>"Unclassified" on its own is a shrug. Three things can be behind it and they do not call for
 * the same answer: the type is plumbing a framework already owns and belongs to no ring, which is
 * not a gap and needs nothing; two readings were left standing with nothing to separate them; or
 * nothing anywhere said anything about it. The last two are the author's to settle, and the same
 * move settles both — say outright what the type is.</p>
 *
 * <p>Generated code is none of these. It never reaches this point: recognizing it is a question of
 * what is read, settled before any fact is stated, so the engine sees none of it and has none to
 * categorize.</p>
 */
final class Fallback {

    private Fallback() {}

    /**
     * Categorizes a type left without a kind.
     *
     * @param type the analyzed declaration
     * @param structure its structure as the model holds it
     * @param verdict the verdict reached on it, always unclassified here
     * @param facts the facts held once the verdicts had settled
     * @return the categorized fallback, with the reason and what would settle it
     */
    static UnclassifiedType of(TypeNode type, TypeStructure structure, Classification verdict, FactBase facts) {
        if (claimedAsPlumbing(type, facts)) {
            return new UnclassifiedType(
                    type.id(),
                    structure,
                    verdict,
                    UnclassifiedCategory.TECHNICAL,
                    Optional.of("a framework claims it as plumbing, which belongs to no ring of the hexagon"));
        }
        if (verdict.isAmbiguous()) {
            return new UnclassifiedType(
                    type.id(),
                    structure,
                    settledBy(type, verdict, "reads as " + tied(verdict) + " alike, and nothing separates them"),
                    UnclassifiedCategory.AMBIGUOUS,
                    Optional.of("two readings were left standing with nothing between them: " + tied(verdict)));
        }
        return new UnclassifiedType(
                type.id(),
                structure,
                settledBy(type, verdict, "is used nowhere the analysis can see"),
                UnclassifiedCategory.UNKNOWN,
                Optional.of("nothing in the perimeter uses it, so there was no context to read it in"));
    }

    private static boolean claimedAsPlumbing(TypeNode type, FactBase facts) {
        return facts.about(type.id(), KnowledgeAssertion.class).stream()
                .anyMatch(assertion -> assertion.finding().fact() == KnowledgeFact.TECHNICAL);
    }

    private static String tied(Classification verdict) {
        return verdict.candidates().stream()
                .map(Candidate::kind)
                .map(ArchKind::toString)
                .reduce((left, right) -> left + " and " + right)
                .orElseThrow();
    }

    /**
     * Attaches the one move that settles a type whatever its neighbours do or do not say. Wiring
     * the type up and widening what is analyzed settle it too, but both depend on a codebase the
     * engine is not looking at; declaring the kind is read on the declaration itself.
     */
    private static Classification settledBy(TypeNode type, Classification verdict, String because) {
        RemediationHint declare = new RemediationHint(
                RemediationAction.CONFIGURE_EXPLICIT,
                declaring(type.id(), because),
                new RemediationImpact(
                        ArchKind.UNCLASSIFIED,
                        verdict.confidence(),
                        "Would be read from the declaration instead of from the neighbours it has none of"),
                Optional.empty());
        return Classification.builder(verdict.kind(), verdict.confidence(), verdict.basis(), verdict.proof())
                .evidences(verdict.evidences())
                .candidates(verdict.candidates())
                .remediations(List.of(declare))
                .build();
    }

    private static String declaring(TypeId subject, String because) {
        return subject.qualifiedName() + " " + because
                + ": declare its kind in the classification configuration, or give it the context that would"
                + " settle it";
    }
}
