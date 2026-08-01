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

package io.hexaglue.model.arch;

import java.util.Objects;

/**
 * A business invariant an aggregate protects, detected from its validation methods.
 *
 * @param name the invariant name, typically the guarding method name
 * @param description the human-readable statement of the invariant
 * @since 7.0.0
 */
public record Invariant(String name, String description) {

    /**
     * Validates that both texts are non-blank.
     */
    public Invariant {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (name.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("name and description must not be blank");
        }
    }

    /**
     * Creates an invariant.
     *
     * @param name the invariant name
     * @param description the human-readable statement
     * @return a new Invariant
     */
    public static Invariant of(String name, String description) {
        return new Invariant(name, description);
    }
}
