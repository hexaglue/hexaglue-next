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

package io.hexaglue.frontend;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import spoon.reflect.declaration.CtElement;

/**
 * Reads the documentation attached to a declaration: the descriptive prose only, joined into a
 * single line, with leading comment markers and block tags removed.
 *
 * <p>Block tags carry contract details ({@code @param}, {@code @throws}) that documentation
 * renderers restate from the declaration itself; keeping them here would duplicate the signature
 * in prose meant to describe intent.</p>
 */
final class Javadocs {

    private Javadocs() {}

    /**
     * Returns the documentation of an element, when it carries any prose.
     *
     * @param element the parsed element
     * @return the cleaned documentation, or empty when there is none
     */
    static Optional<String> of(CtElement element) {
        String raw = element.getDocComment();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String cleaned = Arrays.stream(raw.split("\n", -1))
                .map(String::trim)
                .map(line -> line.startsWith("*") ? line.substring(1).trim() : line)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("@"))
                .collect(Collectors.joining(" "));
        return cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned);
    }
}
