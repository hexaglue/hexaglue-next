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

import io.hexaglue.engine.Classifier;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.Verdicts;
import io.hexaglue.frontend.FrontendRequest;
import io.hexaglue.frontend.SpoonFrontend;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.config.GenerationConfig;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.testkit.corpus.CorpusExpectations;
import io.hexaglue.testkit.corpus.CorpusScenario;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One corpus scenario run through the whole chain: sources on disk, the frontend, the engine, and
 * the verdicts compared to what the scenario is held to mean.
 *
 * <p>This is the only place that wires the frontend to the engine. Neither depends on the other —
 * the boundary between them is the code model — so the harness that needs both lives outside both.
 */
record CorpusRun(CorpusScenario scenario, Verdicts verdicts) {

    /**
     * Materializes the scenario, analyzes it and classifies it.
     *
     * @param scenario the scenario to run
     * @param workspace the directory receiving its sources
     * @return the run, with the verdicts the engine reached
     */
    static CorpusRun of(CorpusScenario scenario, Path workspace) {
        Path sources = scenario.materialize(workspace);
        AnalysisScope scope = new AnalysisScope(Optional.of(scenario.basePackage()), List.of(), List.of());
        CodeModel code = SpoonFrontend.analyze(
                FrontendRequest.builder().sourceRoot(sources).scope(scope).build());
        HexaGlueConfig config = new HexaGlueConfig(
                scope, ClassificationConfig.defaults(), ValidationConfig.defaults(), GenerationConfig.defaults());
        return new CorpusRun(scenario, Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), config)));
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
            String actual = verdicts.kindOf(TypeId.of(claim.qualifiedName()))
                    .map(ArchKind::name)
                    .orElse("NO VERDICT");
            if (!claim.isSatisfiedBy(actual)) {
                unmet.add(claim + " but got " + actual);
            }
        }
        return unmet;
    }
}
