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

/**
 * Runs HexaGlue inside a build.
 *
 * <p>This is the only module that sees Maven, and it is where the decisions belonging to a host
 * are taken: which source roots to read, what to say about what was left out, how the
 * configuration reaches the analysis, and what a failed gate does to the build. Everything below
 * it — the reading, the classification, the rendering — is host-agnostic, so a second host would
 * take these decisions again rather than inherit a shape invented for this one.</p>
 *
 * <p>The host writes nothing into the model and reads nothing back out of a rendered line: it
 * hands the frontend where to look, hands the engine what it read, and turns what came back into
 * log lines and an exit condition.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.maven;
