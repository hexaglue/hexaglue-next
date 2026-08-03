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
 * Reads Java sources and their classpath into the code model.
 *
 * <p>The frontend is the only place where a parser is visible. It offers no parser abstraction:
 * the boundary handed to the rest of the reactor is {@link io.hexaglue.model.code.CodeModel}
 * itself, an immutable base of syntactic facts. A second frontend, if one ever exists, produces
 * the same model rather than implementing an interface invented in advance.</p>
 *
 * <p>Everything here is a fact read from a declaration — never an interpretation. Deciding what a
 * type <em>is</em> belongs to the engine.</p>
 *
 * <p>A reading also accounts for itself: {@link io.hexaglue.frontend.FrontendResult} carries the
 * coded diagnostics of what was not read — outside the perimeter, generated, or recovered from a
 * source the parser could not fully read. They explain a narrower reading, which no later stage
 * can distinguish from a smaller code base; they are never verdicts, and no rule consumes them.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.frontend;
