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
 * Findings and diagnostics: everything HexaGlue reports, coded, located and documented.
 *
 * <p>A {@link io.hexaglue.model.finding.Finding} is an audit rule's verdict on the analyzed
 * architecture — the single model both the renderers and the validation gates consume. A
 * {@link io.hexaglue.model.finding.Diagnostic} reports the tool's own condition — a failed
 * analysis or a refused generation is announced, never returned as a silently empty result.
 * Both carry a published {@link io.hexaglue.model.finding.IssueCode} from the same documented
 * catalogue, and justify themselves with the evidences and typed remediations of
 * {@link io.hexaglue.model.classification}.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.model.finding;
