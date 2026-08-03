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

import io.hexaglue.acceptance.NamingShift.Outcome;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.testkit.GoldenFiles;
import io.hexaglue.testkit.corpus.Corpus;
import io.hexaglue.testkit.corpus.CorpusExpectations;
import io.hexaglue.testkit.corpus.CorpusProfile;
import io.hexaglue.testkit.corpus.CorpusScenario;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What reading names actually buys, measured rather than assumed.
 *
 * <p>The naming vocabulary was taken out of the default posture on the grounds that a role is a
 * position in a graph, and that a suffix deciding alone classifies types nobody wired to anything.
 * That was a judgement, and it came with a promise to check it: run the whole reviewed corpus
 * twice, once reading names and once not, and for every verdict that moves, ask the reviewer who
 * was right. A name that reaches the reviewed answer where position alone could not is a gain; one
 * that contradicts an answer position had reached is damage.</p>
 *
 * <p>The measurement is committed as a golden rather than asserted as a threshold, because the
 * number is not a rule to hold: it is evidence for a decision recorded elsewhere, and what matters
 * on a later run is that the diff shows what moved. A rule that makes names buy more — or less —
 * should be visible here on the day it lands.</p>
 *
 * <p>What this cannot measure is what nobody reviewed. A verdict that moves on a type no claim
 * speaks about is counted apart and never scored: reading it as a gain because it looks plausible
 * is exactly the reasoning the corpus exists to refuse.</p>
 */
class NamingVocabularyTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");

    @TempDir
    Path workspace;

    /** Line ending fixed rather than taken from the platform: a golden is compared byte for byte. */
    private static final String NEWLINE = "\n";

    private static final String PREAMBLE = """
            Reading names against the reviewed corpus.
            Vocabulary off is the shipped posture; on adds ClassificationConfig.conventional().

            What this measures is the verdict, and only reviewed scenarios are scored. A move
            in the candidates behind a verdict, or in its confidence, is not visible here, and
            neither is anything outside the corpus: the examples of the previous reactor are
            frozen and not wired to this one.
            """;

    @Test
    void namingIsWorthWhatTheCorpusSaysItIsWorth() {
        List<String> report = new ArrayList<>(List.of(PREAMBLE.stripTrailing()));
        for (CorpusProfile profile : CorpusProfile.values()) {
            report.add("");
            report.addAll(renderProfile(profile));
        }

        GoldenFiles.assertMatchesExisting(GOLDEN_DIR, "naming-vocabulary.txt", String.join(NEWLINE, report) + NEWLINE);
    }

    private List<String> renderProfile(CorpusProfile profile) {
        Map<Outcome, Integer> tally = new EnumMap<>(Outcome.class);
        Arrays.stream(Outcome.values()).forEach(outcome -> tally.put(outcome, 0));
        List<String> detail = new ArrayList<>();
        int scenariosMoved = 0;

        for (CorpusScenario scenario : Corpus.of(profile)) {
            CorpusExpectations expectations = CorpusExpectations.of(scenario);
            if (!expectations.isScorable()) {
                continue;
            }
            List<NamingShift> shifts = shiftsOf(scenario, expectations);
            if (shifts.isEmpty()) {
                continue;
            }
            scenariosMoved++;
            detail.add("  " + scenario.id());
            for (NamingShift shift : shifts) {
                tally.merge(shift.outcome(), 1, Integer::sum);
                detail.add("    " + shift.render());
            }
        }

        String counts = Arrays.stream(Outcome.values())
                .map(outcome -> outcome.label() + " " + tally.get(outcome))
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        List<String> lines = new ArrayList<>(List.of(profile.directory() + ": " + counts + " — over " + scenariosMoved
                + " scenario(s) whose reading moved"));
        lines.addAll(detail.isEmpty() ? List.of("  nothing moved") : detail);
        return lines;
    }

    /**
     * Returns the verdicts of one scenario that move when the vocabulary is switched on, in type
     * order. A type absent from one of the two models has no verdict there, which is itself a
     * difference worth reporting: reading a name is how an isolated type stops being silent.
     */
    private List<NamingShift> shiftsOf(CorpusScenario scenario, CorpusExpectations expectations) {
        Path sources = scenario.materialize(workspace.resolve(scenario.id()));
        ArchModel without = AnalysisChain.modelOf(sources, scenario.basePackage(), ClassificationConfig.defaults());
        ArchModel with = AnalysisChain.modelOf(sources, scenario.basePackage(), ClassificationConfig.conventional());

        List<NamingShift> shifts = new ArrayList<>();
        for (String qualifiedName : namesIn(without, with)) {
            String off = CorpusRun.kindIn(without, qualifiedName);
            String on = CorpusRun.kindIn(with, qualifiedName);
            if (!off.equals(on)) {
                shifts.add(NamingShift.of(qualifiedName, off, on, expectations.claims()));
            }
        }
        return shifts;
    }

    /** Sorted, because a report a human compares between runs may not reorder itself. */
    private static NavigableSet<String> namesIn(ArchModel without, ArchModel with) {
        NavigableSet<String> names = new TreeSet<>();
        for (ArchModel model : List.of(without, with)) {
            model.types().stream().map(type -> type.id().qualifiedName()).forEach(names::add);
        }
        return names;
    }
}
