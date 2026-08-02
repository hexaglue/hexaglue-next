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
 * The typed configuration contract: the shapes a strict loader binds, nothing more.
 *
 * <p>{@link io.hexaglue.model.config.HexaGlueConfig} composes the four blocks — the
 * {@link io.hexaglue.model.config.AnalysisScope perimeter of the analysis}, the
 * {@link io.hexaglue.model.config.ClassificationConfig declared kinds}, the
 * {@link io.hexaglue.model.config.ValidationConfig validation gates} and the
 * {@link io.hexaglue.model.config.GenerationConfig generation threshold}. Every record validates
 * its own coherence at construction; none carries behavior. Reading YAML into these shapes —
 * strictly, unknown key equals error — is the loader's job, not the model's.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.model.config;
