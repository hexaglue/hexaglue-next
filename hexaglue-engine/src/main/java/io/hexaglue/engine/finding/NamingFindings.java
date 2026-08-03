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

package io.hexaglue.engine.finding;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Whether a codebase keeps the naming convention it said it keeps.
 *
 * <p>This is the one check that would be indefensible as an opinion of the tool and is perfectly
 * defensible as a reading of the project's own configuration. Nothing here knows that an event
 * should be named in the past tense or that a repository should end in {@code Repository}: it
 * reads the vocabulary the project opted into and holds it to that, kind by kind. With no
 * vocabulary configured — the default — it says nothing at all, on any codebase.</p>
 *
 * <p>What this deliberately does not carry over is the shipped list of past-tense endings the
 * previous engine matched event names against. A convention the tool holds and the project never
 * stated is an opinion wearing the clothes of a rule.</p>
 *
 * @since 7.0.0
 */
public final class NamingFindings {

    /** A type does not follow the naming vocabulary the project opted into. */
    static final IssueCode OFF_VOCABULARY = IssueCode.of("HG-NAME-001");

    private NamingFindings() {}

    /**
     * Runs the naming check.
     *
     * @param judgement what the check may read
     * @return the findings, empty whenever no vocabulary is configured
     */
    static List<Finding> of(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (ArchType type : judgement.model().types()) {
            List<String> expected = judgement.vocabulary().suffixesFor(type.kind());
            if (expected.isEmpty() || endsWithOneOf(type.id(), expected)) {
                continue;
            }
            findings.add(Finding.builder(
                            OFF_VOCABULARY,
                            Severity.MINOR,
                            type.id().simpleName() + " is a " + type.kind() + ", and this project states that a "
                                    + type.kind() + " is named with " + String.join(" or ", expected)
                                    + ". Either the name or the vocabulary is out of date, and a reader following "
                                    + "the convention will read this one wrong.",
                            type.id())
                    .evidences(List.of(new Evidence(
                            EvidenceTier.NAMING,
                            Confidence.MEDIUM,
                            "off-vocabulary(" + type.kind() + ")",
                            "the configured vocabulary expects " + String.join(" or ", expected),
                            Optional.empty(),
                            List.of())))
                    .build());
        }
        return findings;
    }

    private static boolean endsWithOneOf(TypeId id, List<String> suffixes) {
        return suffixes.stream().anyMatch(suffix -> id.simpleName().endsWith(suffix));
    }
}
