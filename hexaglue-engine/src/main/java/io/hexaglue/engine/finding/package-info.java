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
 * The second question: not what the types are, but whether what they are holds together.
 *
 * <p>Identification is generous on purpose — it has to read codebases that never declare anything,
 * and a reading that refused everything it was not sure of would read nothing at all. Conformity
 * is allowed to be strict about that very same code, and the two answers are both true: an
 * aggregate the engine recognised can still be an aggregate nothing stores.</p>
 *
 * <p>These checks sit in the engine rather than in a plugin because the verdict has two consumers
 * that must never disagree: the gate that fails a build, and the report that explains it. One
 * produced inside a plugin would be invisible to the other.</p>
 *
 * <p>A check reads the classified model and the references the frontend recorded. It classifies
 * nothing, reads no source, and touches nothing on disk.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.engine.finding;
