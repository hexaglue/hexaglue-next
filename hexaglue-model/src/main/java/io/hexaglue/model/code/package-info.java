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
 * The code model: an immutable base of syntactic facts built once by the frontend.
 *
 * <p>Nodes are types — including nested types and lightweight stubs for every referenced
 * classpath type — modules, and the declarations they contain. Edges are typed and carry their
 * provenance: which member, parameter or type argument produced them, including edges toward
 * external types ({@code extends JpaRepository<Order, OrderId>} yields an EXTENDS edge to a stub
 * plus TYPE_ARGUMENT edges). The supertype closure, classpath included, is precomputed by the
 * frontend and stored as data. Method-body facts (invocations, instantiations) are present only
 * when that capability was requested.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.model.code;
