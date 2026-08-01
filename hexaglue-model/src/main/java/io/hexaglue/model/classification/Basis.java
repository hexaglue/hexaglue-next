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

package io.hexaglue.model.classification;

/**
 * Whether a classification was declared by the user or inferred by the engine — the distinction
 * every downstream consumer needs: validation gates it, the audit reports it, generation
 * thresholds on it.
 *
 * @since 7.0.0
 */
public enum Basis {

    /** The user stated the kind (explicit configuration, intent annotation). */
    DECLARED,

    /** The engine derived the kind from evidences. */
    INFERRED
}
