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
import java.util.Objects;

/**
 * What a check on the architecture is allowed to read: the verdicts, and who names whom.
 *
 * <p>Judging is a second question, asked after the first one is answered. Identification asks what
 * a type is and tolerates a codebase that never says; conformity asks whether what the types are
 * holds together, and it is allowed to be demanding about the very same code. Keeping the two
 * apart is what lets a finding be strict without making the model refuse to read anything.</p>
 *
 * <p>Nothing here is a classifier. A check reads the verdicts the engine reached and the
 * references the frontend recorded — it never decides what a type is, and never reads a source
 * file.</p>
 *
 * <p>The naming vocabulary is here for one reason: a codebase that opted into one has stated a
 * convention about itself, and conformity may hold it to what it stated. It never invents one —
 * with no vocabulary configured, every check that reads it says nothing.</p>
 *
 * <p>What the installed backends will write is here for a related reason: a hole the core leaves is
 * a thing to report unless this very build fills it. That is a declaration of the project, read
 * before anything runs — never an observation of what a run produced — so the same sources are
 * judged the same way twice.</p>
 *
 * @param model the classified model
 * @param dependencies who names whom, between types and between packages
 * @param vocabulary the naming convention the codebase opted into, empty by default
 * @param backends what the backends this build installed state they will write
 * @since 7.0.0
 */
public record Judgement(
        ArchModel model, Dependencies dependencies, ClassificationConfig vocabulary, Backends backends) {

    /**
     * Validates every component.
     */
    public Judgement {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        Objects.requireNonNull(vocabulary, "vocabulary must not be null");
        Objects.requireNonNull(backends, "backends must not be null");
    }
}
