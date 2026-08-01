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
 * Base vocabulary of the HexaGlue contract, shared by the code model, the architectural model and
 * every downstream consumer: type identity, type references, source locations and the Java
 * declaration vocabulary.
 *
 * <p>Everything in this module is an immutable value: records validate their invariants at
 * construction and defensively copy their collections. Absence is expressed with
 * {@link java.util.Optional}, never {@code null}. Collections preserve a deterministic iteration
 * order so that identical models render identical outputs, byte for byte.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.model;
