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

import io.hexaglue.engine.Dependencies;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.Backends;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.finding.Finding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Everything the checks have to say about an architecture, in one order.
 *
 * <p>The findings live here rather than in a plugin because two very different things consume
 * them: the gate that decides whether a build passes, and the report that explains what it found.
 * A judgement produced inside a plugin would be a judgement the gate cannot see, and the two would
 * disagree the first time a build passed while the report said otherwise.</p>
 *
 * <p>The order is the same on every run — by code, then by subject — so a report can be diffed
 * against yesterday's and a gate can be trusted not to change its mind about the same sources.</p>
 *
 * @since 7.0.0
 */
public final class Findings {

    private static final Comparator<Finding> ORDER =
            Comparator.comparing(Finding::code).thenComparing(Finding::subject).thenComparing(Finding::message);

    private Findings() {}

    /**
     * Judges an architecture: against its own shape, against the naming vocabulary it opted into,
     * and against what this build states it will write for it.
     *
     * @param model the classified model
     * @param dependencies who names whom
     * @param vocabulary the naming convention the project stated, empty by default
     * @param backends what the backends this build installed state they will write
     * @return the findings in a stable order, and what the checks left unsaid
     */
    public static Judged of(
            ArchModel model, Dependencies dependencies, ClassificationConfig vocabulary, Backends backends) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        Objects.requireNonNull(vocabulary, "vocabulary must not be null");
        Objects.requireNonNull(backends, "backends must not be null");
        Judgement judgement = new Judgement(model, dependencies, vocabulary, backends);
        Judged hexagonal = HexagonalFindings.of(judgement);
        List<Finding> findings = new ArrayList<>(DomainFindings.of(judgement));
        findings.addAll(hexagonal.findings());
        findings.addAll(NamingFindings.of(judgement));
        return new Judged(findings.stream().sorted(ORDER).toList(), hexagonal.diagnostics());
    }
}
