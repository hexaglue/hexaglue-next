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
 * What the frameworks mean, stated as data.
 *
 * <p>A pack maps a framework symbol — an annotation, a supertype, a type, a package — to the
 * technical fact it carries: that a type is mapped to storage, that an interface is a Spring Data
 * repository, that an annotation declares an author's intent. Stating it once, as versioned
 * resources, is what keeps the same knowledge from being re-listed, and re-diverging, in every
 * component that needs it.</p>
 *
 * <p>A symbol is always named in full — by qualified name or package prefix, never by simple name.
 * Matching {@code Entity} on its simple name is what once made a persistence mapping look like a
 * DDD entity; here it cannot be written down. Inheritance is read from the supertype closure the
 * frontend computes, so knowledge is stated on the root of a hierarchy ({@code Repository}) and
 * every vendor interface derived from it answers to it, sources or not.</p>
 *
 * <p>Nothing here decides anything: a pack states facts, the engine weighs them.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.knowledge;
