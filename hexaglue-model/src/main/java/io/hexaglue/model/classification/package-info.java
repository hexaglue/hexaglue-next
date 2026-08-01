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
 * The classification contract: how a verdict carries its complete provenance.
 *
 * <p>A {@link io.hexaglue.model.classification.Classification} states a kind with one confidence
 * scale, whether it was declared or inferred, the tiered evidences (S1 declared intent down to S6
 * naming) that supported it, the losing candidates when the decision was ambiguous, the proof tree
 * of the derivation and the remediation hints that would strengthen it. Nothing downstream ever
 * needs to re-guess why a type is what it is.</p>
 *
 * <p>The evidence tiers enforce the doctrine by construction: an evidence's force can never exceed
 * the ceiling of its tier — naming can inform, never decide.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.model.classification;
