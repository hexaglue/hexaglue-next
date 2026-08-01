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
 * The architectural model: the classified intermediate representation the plugins consume.
 *
 * <p>The sealed {@link io.hexaglue.model.arch.ArchType} hierarchy covers the hexagon — domain,
 * ports, application — plus the categorized {@code UnclassifiedType} fallback, so pattern matching
 * over a model is exhaustive. Every type carries its {@code TypeStructure} and its complete
 * {@link io.hexaglue.model.classification.Classification}; construction rejects a verdict whose
 * kind does not match the record that carries it.</p>
 *
 * <p>The {@link io.hexaglue.model.arch.ArchModel} container assembles the verdicts, answers
 * provenance queries ({@code classificationOf}, {@code explain}) and exposes the deterministic
 * indexes — domain, ports, composition — and the {@code ModuleTopology} describing the build
 * layout of a multi-module reactor.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.model.arch;
