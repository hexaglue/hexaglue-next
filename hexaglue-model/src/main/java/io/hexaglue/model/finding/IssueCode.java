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

package io.hexaglue.model.finding;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The published identifier of an issue HexaGlue can report — an audit finding or a tool
 * diagnostic. One code type serves both: the catalogue documented publicly is unique, and the
 * validation gates threshold on the same codes the audit displays.
 *
 * <p>The canonical form is {@code HG-CATEGORY-NNN}: the {@code HG} prefix, an uppercase category
 * (for example {@code DDD}, {@code GEN}), and a three-digit number. Anything else is rejected at
 * construction.</p>
 *
 * @param value the canonical code, for example {@code HG-DDD-012}
 * @since 7.0.0
 */
// The record-generated equals/hashCode compare the single component that compareTo orders on,
// so the Comparable consistency contract holds without explicit overrides.
@SuppressWarnings("PMD.OverrideBothEqualsAndHashCodeOnComparable")
public record IssueCode(String value) implements Comparable<IssueCode> {

    private static final Pattern CANONICAL_FORM = Pattern.compile("HG-[A-Z]+-[0-9]{3}");

    /**
     * Validates the canonical form.
     */
    public IssueCode {
        Objects.requireNonNull(value, "value must not be null");
        if (!CANONICAL_FORM.matcher(value).matches()) {
            throw new IllegalArgumentException("issue code must match HG-CATEGORY-NNN, got: " + value);
        }
    }

    /**
     * Creates an issue code from its canonical form.
     *
     * @param value the canonical code, for example {@code HG-DDD-012}
     * @return a new IssueCode
     */
    public static IssueCode of(String value) {
        return new IssueCode(value);
    }

    /**
     * Returns the category segment of the code.
     *
     * @return the category, for example {@code DDD}
     */
    public String category() {
        return value.substring(3, value.lastIndexOf('-'));
    }

    /**
     * Returns the numeric segment of the code.
     *
     * @return the number, for example {@code 12} for {@code HG-DDD-012}
     */
    public int number() {
        return Integer.parseInt(value.substring(value.lastIndexOf('-') + 1));
    }

    @Override
    public int compareTo(IssueCode other) {
        return value.compareTo(other.value);
    }
}
