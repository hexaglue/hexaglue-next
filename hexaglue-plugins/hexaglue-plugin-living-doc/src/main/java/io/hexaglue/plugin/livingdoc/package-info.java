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
 * Documentation of an architecture, written by the architecture itself.
 *
 * <p>Architecture documents rot because they are written beside the code and maintained by hand.
 * These are derived from the classified model on every build, so the only way for a page to be
 * wrong is for the model to be wrong — and then it is the model that gets fixed, once, for
 * everyone reading it.</p>
 *
 * <p>The plugin reads nothing but the model. It does not look at the file tree, does not re-read
 * a source file and does not decide what any type is: a page that re-derived a verdict would be a
 * second classifier, disagreeing with the first one the day one of them changed.</p>
 *
 * <p>Every verdict can say what it rests on. A page that showed only its conclusions would let a
 * reader take an inferred kind for a declared one, so each type carries a folded section holding
 * its confidence, its basis and its evidence — and the types nothing could be said about are
 * listed rather than dropped.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.plugin.livingdoc;
