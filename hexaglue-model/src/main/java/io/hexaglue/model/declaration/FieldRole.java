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
 * <p>{@link #AUDIT} and {@link #TECHNICAL} are never assigned: telling them apart takes an
 * annotation a knowledge pack would have to name on a member, and none does. Reading them off the
 * field's name, as was once done, is the one thing this engine will not do.</p>
 *
 * @since 7.0.0
 */
public enum FieldRole {

    /**
     * The field carries the identity of its owner: the value a way out searches the aggregate by,
     * or, on a part, a field whose own type was read as an identifier.
     */
    IDENTITY,

    /** The field is a collection or an array of whatever it holds. */
    COLLECTION,

    /**
     * The field holds a whole other aggregate rather than naming it by identity — which is what
     * makes it a reference to store rather than a part to embed.
     */
    AGGREGATE_REFERENCE,

    /** The field holds something read as a value or an identity, and is stored inside its owner. */
    EMBEDDED,

    /** The field carries audit metadata (created/modified timestamps, users). Never assigned. */
    AUDIT,

    /** The field is technical plumbing (version columns, serialization ids). Never assigned. */
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
