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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.engine.Explanation;
import io.hexaglue.engine.Outcome;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.testkit.GoldenFiles;
import io.hexaglue.testkit.corpus.Corpus;
import io.hexaglue.testkit.corpus.CorpusProfile;
import io.hexaglue.testkit.corpus.CorpusScenario;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * What the engine says about its own conclusions, read against the whole corpus.
 *
 * <p>Every rule of the engine builds a proof, every verdict carries one, and until this test
 * nothing read them: the snapshots pin the model, not the derivation behind it. A proof nobody
 * renders is a proof nobody can find wrong, so the restitution of all three profiles is committed
 * as a golden — a rule that changes what a verdict rests on shows up here as a diff, on the day it
 * lands rather than the day a user asks why.</p>
 *
 * <p>The golden pins the exact words; the invariants below hold for every type of every scenario
 * and say what the words must never do, whatever they end up being.</p>
 */
class ExplanationTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");

    /** Line ending fixed rather than taken from the platform: a golden is compared byte for byte. */
    private static final String NEWLINE = "\n";

    /** {@code com.acme.Order: AGGREGATE_ROOT (HIGH, inferred)} — the shape every verdict opens with. */
    private static final Pattern HEADER =
            Pattern.compile("^\\S+: [A-Z_]+ \\((EXPLICIT|HIGH|MEDIUM|LOW), (declared|inferred)\\)$");

    @TempDir
    Path workspace;

    @ParameterizedTest
    @EnumSource(CorpusProfile.class)
    void everyVerdictOfTheCorpusStatesWhatItRestsOn(CorpusProfile profile) {
        List<String> report = new ArrayList<>();
        for (CorpusScenario scenario : Corpus.of(profile)) {
            report.add(scenario.id());
            ArchModel model = modelOf(scenario);
            Explanation.of(Outcome.of(model)).forEach(line -> report.add("  " + line));
            for (ArchType type : model.types()) {
                Explanation.withDerivation(type).forEach(line -> report.add("  " + line));
            }
            report.add("");
        }

        GoldenFiles.assertMatchesExisting(
                GOLDEN_DIR, "explanation-" + profile.directory() + ".txt", String.join(NEWLINE, report) + NEWLINE);
    }

    @ParameterizedTest
    @EnumSource(CorpusProfile.class)
    void everyExplanationOpensOnTheVerdictItIsAbout(CorpusProfile profile) {
        for (CorpusScenario scenario : Corpus.of(profile)) {
            for (ArchType type : modelOf(scenario).types()) {
                List<String> lines = Explanation.of(type);

                assertThat(lines.get(0))
                        .as("%s in %s", type.id().qualifiedName(), scenario.id())
                        .matches(HEADER)
                        .startsWith(type.id().qualifiedName() + ": ");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CorpusProfile.class)
    void noExplanationEverPrintsAnEmptyOrRaggedLine(CorpusProfile profile) {
        for (CorpusScenario scenario : Corpus.of(profile)) {
            for (ArchType type : modelOf(scenario).types()) {
                assertThat(Explanation.withDerivation(type))
                        .as("%s in %s", type.id().qualifiedName(), scenario.id())
                        .isNotEmpty()
                        .allSatisfy(line -> assertThat(line)
                                .isNotBlank()
                                .isEqualTo(line.stripTrailing())
                                .doesNotContain("\n"));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(CorpusProfile.class)
    void askingForTheDerivationNeverTakesAwayWhatTheVerdictAlreadySaid(CorpusProfile profile) {
        for (CorpusScenario scenario : Corpus.of(profile)) {
            for (ArchType type : modelOf(scenario).types()) {
                assertThat(Explanation.withDerivation(type))
                        .as("%s in %s", type.id().qualifiedName(), scenario.id())
                        .containsSubsequence(Explanation.of(type));
            }
        }
    }

    @Test
    void twoRunsOverTheSameSourcesExplainThemInTheSameBytes() {
        for (CorpusScenario scenario : Corpus.of(CorpusProfile.PROFILE_2)) {
            assertThat(render(modelOf(scenario, "second")))
                    .as(scenario.id())
                    .isEqualTo(render(modelOf(scenario, "first")));
        }
    }

    private static String render(ArchModel model) {
        List<String> lines = new ArrayList<>(Explanation.of(Outcome.of(model)));
        model.types().forEach(type -> lines.addAll(Explanation.withDerivation(type)));
        return String.join(NEWLINE, lines);
    }

    private ArchModel modelOf(CorpusScenario scenario) {
        return modelOf(scenario, "once");
    }

    /**
     * Materializes the scenario under a run-specific directory, so that two runs of the same
     * scenario read two distinct trees and a path can never be what makes them agree.
     */
    private ArchModel modelOf(CorpusScenario scenario, String run) {
        Path sources = scenario.materialize(workspace.resolve(run).resolve(scenario.id()));
        return AnalysisChain.modelOf(sources, scenario.basePackage());
    }
}
