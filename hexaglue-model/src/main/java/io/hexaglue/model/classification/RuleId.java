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

package io.hexaglue.model.classification;

import java.util.Objects;

/**
 * The published identifier of an inference rule. Rules expose their id as a constant; consumers
 * compare ids, never free-form strings.
 *
 * @param value the rule identifier (e.g. {@code R1})
 * @since 7.0.0
 */
public record RuleId(String value) {

    /**
     * Validates the identifier.
     */
    public RuleId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    /**
     * Creates a rule id.
     *
     * @param value the rule identifier
     * @return a new RuleId
     */
    public static RuleId of(String value) {
        return new RuleId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
