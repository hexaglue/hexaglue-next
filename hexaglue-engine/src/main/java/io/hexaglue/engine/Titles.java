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

import io.hexaglue.engine.rule.Catalogue;
import io.hexaglue.model.classification.RuleId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What each step a proof can name actually does, in one phrase.
 *
 * <p>A rule identifier is a cross-reference, not an explanation: it lets a reader look the rule up
 * in the reference and lets a consumer compare it without parsing text, and both of those are
 * reasons to keep it. Neither is a reason to make it the only thing a reader gets. {@code R1} says
 * nothing to someone reading their own build log for the first time.</p>
 *
 * <p>The phrases are not held here — each rule carries its own through {@link Rule#title()}, so
 * the sentence and the logic are corrected in the same edit. This is only where they are looked up
 * by identifier, because a proof carries identifiers and not the rules that produced them.</p>
 */
final class Titles {

    private static final Map<RuleId, String> BY_ID = index();

    private Titles() {}

    private static Map<RuleId, String> index() {
        Map<RuleId, String> titles = new LinkedHashMap<>();
        titles.put(Aggregator.ID, Aggregator.TITLE);
        for (Rule rule : Catalogue.all()) {
            titles.put(rule.id(), rule.title());
        }
        return Map.copyOf(titles);
    }

    /**
     * Returns what the step named by an identifier does.
     *
     * @param id the identifier a proof named
     * @return its title, or empty when nothing in the engine claims that identifier
     */
    static Optional<String> of(RuleId id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
