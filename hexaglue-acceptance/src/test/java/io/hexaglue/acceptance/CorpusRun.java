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

package io.hexaglue.acceptance;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.testkit.corpus.CorpusExpectations;
import io.hexaglue.testkit.corpus.CorpusScenario;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One corpus scenario run through the whole chain, and what the scenario is held to mean compared
 * to what came out.
 *
 * <p>The comparison is made against the model rather than against the verdicts alone, so the
 * scoreboard and the golden files score the same thing: a scenario cannot pass here while its
 * snapshot says something else.</p>
 */
record CorpusRun(CorpusScenario scenario, ArchModel model) {

    /**
     * Materializes the scenario, analyzes it and classifies it.
     *
     * @param scenario the scenario to run
     * @param workspace the directory receiving its sources
     * @return the run, with the model the chain produced
     */
    static CorpusRun of(CorpusScenario scenario, Path workspace) {
        return new CorpusRun(scenario, AnalysisChain.modelOf(scenario.materialize(workspace), scenario.basePackage()));
    }

    /**
     * Returns the claims the run did not satisfy, rendered for a failure message.
     *
     * @return the unmet claims, empty when the scenario holds
     */
    List<String> unmetClaims() {
        List<String> unmet = new ArrayList<>();
        for (CorpusExpectations.Claim claim :
                CorpusExpectations.profile1(scenario.id()).claims()) {
            String actual = model.type(TypeId.of(claim.qualifiedName()))
                    .map(ArchType::kind)
                    .map(ArchKind::name)
                    .orElse("NO VERDICT");
            if (!claim.isSatisfiedBy(actual)) {
                unmet.add(claim + " but got " + actual);
            }
        }
        return unmet;
    }
}
