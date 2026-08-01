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

package io.hexaglue.model.declaration;

/**
 * Semantic role of a field in the domain model, assigned by the classification engine — the
 * frontend leaves roles empty.
 *
 * @since 7.0.0
 */
public enum FieldRole {

    /** The field carries the identity of its owner (e.g. {@code OrderId id}). */
    IDENTITY,

    /** The field is a collection of owned or referenced elements. */
    COLLECTION,

    /** The field references another aggregate by its identifier. */
    AGGREGATE_REFERENCE,

    /** The field embeds a value object. */
    EMBEDDED,

    /** The field carries audit metadata (created/modified timestamps, users). */
    AUDIT,

    /** The field is technical plumbing (version columns, serialization ids). */
    TECHNICAL;

    /**
     * Returns whether this role carries business meaning, as opposed to audit or technical
     * plumbing.
     *
     * @return true for IDENTITY, COLLECTION, AGGREGATE_REFERENCE and EMBEDDED
     */
    public boolean isBusinessRelevant() {
        return switch (this) {
            case IDENTITY, COLLECTION, AGGREGATE_REFERENCE, EMBEDDED -> true;
            case AUDIT, TECHNICAL -> false;
        };
    }
}
