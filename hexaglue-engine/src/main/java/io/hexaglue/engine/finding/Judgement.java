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
 * @param model the classified model
 * @param dependencies who names whom, between types and between packages
 * @since 7.0.0
 */
public record Judgement(ArchModel model, Dependencies dependencies) {

    /**
     * Validates both components.
     */
    public Judgement {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
    }
}
