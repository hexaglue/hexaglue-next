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
 * The plugin contract: what a backend reads, what it emits, and how a run survives it.
 *
 * <p>A plugin is a pure function of the classified model. It reads {@code ArchModel} — the
 * verdicts and their provenance — and emits into typed sinks; it never opens a file. Where the
 * output lands is the host's decision, so a plugin names a relative path and nothing more. That
 * is what makes a run replayable, parallelizable and testable without touching a disk.</p>
 *
 * <p>Plugins can declare that another plugin runs first. Resolving that order takes two passes —
 * one to learn which identifiers exist, one to draw the edges between them — because a single
 * pass has to look up a plugin that may not have been read yet. What the order leaves out is
 * stated rather than thrown: a dependency nobody provides, a cycle, two plugins claiming one
 * identifier.</p>
 *
 * <p>A plugin is untrusted code running inside someone's build. Whatever it throws, and whatever
 * it fails to link against, is caught and turned into a coded diagnostic naming the plugin; the
 * other plugins still run, and what depended on the failed one is skipped rather than run against
 * half a result.</p>
 *
 * <p>Beside the contract sits the one reading every backend needs and none should answer on its
 * own: how generated code reaches into a domain type somebody else wrote. Persistence reads a
 * field to put it in a row and a web layer reads the same field to put it in a response — two
 * answers to that would mean one of them is wrong.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.spi;
