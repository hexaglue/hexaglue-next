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

package io.hexaglue.plugin.livingdoc;

import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.plugin.livingdoc.render.Markdown;
import io.hexaglue.plugin.livingdoc.render.Table;

/**
 * Why a type reads the way it does.
 *
 * <p>Documentation generated from a classification is only worth what the classification is worth,
 * and a reader who cannot see what a verdict rests on has no way to tell a declared kind from a
 * guess. So every type can say it: how sure the engine is, whether the author declared it or the
 * engine inferred it, and the evidence behind it, tier by tier.</p>
 *
 * <p>It is folded away by default in the page, because the answer matters more often than the
 * reason — but it is never absent.</p>
 */
final class Provenance {

    private Provenance() {}

    /**
     * Writes the folded section stating what a verdict rests on.
     */
    static void writeTo(Markdown document, ArchType type) {
        Classification classification = type.classification();
        Markdown section = Markdown.document()
                .paragraph(Markdown.bold("Confidence") + ": " + classification.confidence() + " — "
                        + Markdown.bold("basis") + ": " + classification.basis());
        if (classification.evidences().isEmpty()) {
            section.paragraph("No evidence was recorded for this verdict.");
        } else {
            Table evidences = Table.withHeaders("Tier", "Fact", "Why");
            classification
                    .evidences()
                    .forEach(evidence -> evidences.row(
                            evidence.tier().code(), Markdown.inlineCode(evidence.fact()), evidence.justification()));
            section.table(evidences);
        }
        classification
                .remediations()
                .forEach(hint -> section.bullet(Markdown.bold("To make it certain") + ": " + hint.description()));
        document.collapsible("What this verdict rests on", section.render());
    }
}
