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
 * Writing markdown and diagrams by saying what they are.
 *
 * <p>Documents used to be assembled by appending strings, and the escaping then lived wherever
 * someone remembered it: a type name holding an underscore came out emphasised, one holding a pipe
 * ended its table row early, a label holding a quote stopped a whole diagram from rendering. Here
 * a caller states a heading, a cell, a node, a relation — and the text is made inert once, on the
 * way in.</p>
 *
 * <p>A diagram renders as its own source, with no markdown fence around it: a diagram is not
 * markdown, and a document that wants to show one puts it in a code block itself. That is what
 * lets the same diagram be written into a document, a report, or a file of its own.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.plugin.livingdoc.render;
