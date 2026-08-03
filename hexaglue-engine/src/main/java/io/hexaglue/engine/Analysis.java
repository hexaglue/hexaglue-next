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

import io.hexaglue.engine.finding.Findings;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The whole engine in one call: a code model in, the classified model out — with what it read
 * and did not classify said rather than left to be noticed.
 *
 * <p>Three steps, in this order and no other. The verdicts settle first, because a rule reading a
 * neighbour's kind has to read the final one. The facts are then derived once more against those
 * settled verdicts, because the fact base of each round is thrown away and only the last one
 * describes the codebase as the verdicts finally read it. The records are built from both.</p>
 *
 * @since 7.0.0
 */
public final class Analysis {

    /** A type was read and left without a verdict, the configured scope not covering it. */
    private static final IssueCode NOT_CLASSIFIED = IssueCode.of("HG-ENGINE-003");

    private Analysis() {}

    /**
     * Analyzes the given context with the standard rules.
     *
     * @param context what the rules may read
     * @return the classified model, and what was read without being classified
     * @throws EngineException when the verdicts have not settled within the round ceiling
     */
    public static AnalysisResult analyze(EngineContext context) {
        return analyze(RuleSet.standard(), context);
    }

    /**
     * Analyzes the given context with the given rules.
     *
     * @param rules the rules to run
     * @param context what the rules may read
     * @return the classified model, and what was read without being classified
     * @throws EngineException when the verdicts have not settled within the round ceiling
     */
    public static AnalysisResult analyze(RuleSet rules, EngineContext context) {
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Verdicts verdicts = Classifier.classify(rules, context);
        FactBase facts = Saturation.saturate(rules, context.withVerdicts(verdicts));
        Modules modules = Modules.read(context.code(), context.config().modules(), facts);
        ArchModel model = Assembly.assemble(context, facts, verdicts, modules.topology());
        List<Finding> findings = Findings.of(
                model,
                Dependencies.of(context.code(), context.perimeter()),
                context.config().classification());
        List<Diagnostic> diagnostics = new ArrayList<>(modules.diagnostics());
        diagnostics.addAll(leftUnclassified(context));
        return new AnalysisResult(model, findings, diagnostics);
    }

    /**
     * Words what the scope read without classifying. The message says the type was not classified,
     * never that it does not exist: it was read, and what it says about its neighbours counted.
     */
    private static List<Diagnostic> leftUnclassified(EngineContext context) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (Perimeter.Exclusion exclusion : context.perimeter().excluded()) {
            diagnostics.add(Diagnostic.builder(
                            NOT_CLASSIFIED,
                            DiagnosticSeverity.INFO,
                            exclusion.type().qualifiedName() + " was read but not classified: " + exclusion.reason())
                    .subject(exclusion.type())
                    .build());
        }
        return diagnostics;
    }
}
