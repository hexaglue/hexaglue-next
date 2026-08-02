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
 * The inference rules, one class each.
 *
 * <p>Each rule is a small, testable statement about one way a type can reveal what it is. None of
 * them decides: they emit signals for kinds, and the decision weighs the signals once no rule has
 * anything left to say. A rule that could decide alone would be the first-match cascade the engine
 * exists to replace.</p>
 *
 * <p>The seed rules read the context — the packs, the configuration, the shape of a declaration —
 * and the propagation rules read what the other rules concluded, which is how a verdict on one
 * type informs the verdict on its neighbours.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.engine.rule;
