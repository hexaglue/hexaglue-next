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

import io.hexaglue.engine.Analysis;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.frontend.FrontendRequest;
import io.hexaglue.frontend.SpoonFrontend;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.config.GenerationConfig;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.testkit.ArchModelSnapshots;
import io.hexaglue.testkit.corpus.AnalysisRunner;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The whole chain in one place: sources on disk, the frontend, the engine, the classified model.
 *
 * <p>Neither the frontend nor the engine depends on the other — the boundary between them is the
 * code model — so the one thing that needs both lives outside both. The testkit ships the corpus
 * and its harness but no engine, and finds this one through {@link java.util.ServiceLoader}: that
 * is the whole reason the corpus can be published without publishing an analysis with it. The
 * loader builds what it discovers, which the implicit public constructor of this stateless class
 * gives it.</p>
 */
public final class AnalysisChain implements AnalysisRunner {

    /**
     * Runs the chain and answers with the model.
     *
     * @param sources the root of the Java sources to read
     * @param basePackage the package the analysis is scoped to
     * @return the classified model
     */
    static ArchModel modelOf(Path sources, String basePackage) {
        AnalysisScope scope = new AnalysisScope(Optional.of(basePackage), List.of(), List.of());
        CodeModel code = SpoonFrontend.analyze(
                FrontendRequest.builder().sourceRoot(sources).scope(scope).build());
        HexaGlueConfig config = new HexaGlueConfig(
                scope, ClassificationConfig.defaults(), ValidationConfig.defaults(), GenerationConfig.defaults());
        return Analysis.analyze(EngineContext.of(code, KnowledgePacks.embedded(), config));
    }

    @Override
    public String analyze(Path sourceRoot, String basePackage) {
        return ArchModelSnapshots.serialize(modelOf(sourceRoot, basePackage));
    }
}
