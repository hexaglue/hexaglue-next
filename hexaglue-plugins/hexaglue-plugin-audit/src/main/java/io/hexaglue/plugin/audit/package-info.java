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
 * The report an architecture gets about itself.
 *
 * <p>Seven things a reader wants: the verdict, what the verdict is made of, the violations, how
 * far to trust any of it, the measures, the inventory, and what the work would cost. The order is
 * the order they are read in — the number first for whoever only wants the number, the reading it
 * rests on before the detail, the cost last.</p>
 *
 * <p>This plugin decides nothing. The kinds, the violations and the measures all arrive already
 * settled; what happens here is arrangement. That is not modesty about the report — it is the only
 * way the report and the gate can agree, because they are looking at the same answer rather than
 * at two answers that happen to match today.</p>
 *
 * <p>Nothing here fails a build either. Whether a violation stops a build is stated once, in the
 * configuration the engine reads. A report that could also stop one would be a second lever on the
 * same door.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.plugin.audit;
