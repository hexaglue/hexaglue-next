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

package io.hexaglue.render;

import java.util.Objects;

/**
 * What every Mermaid diagram has to get right about the text it is handed.
 *
 * <p>A diagram distinguishes two kinds of text and treats them differently: an <em>identifier</em>
 * is a name the syntax itself uses, so it may only hold letters, digits and underscores; a
 * <em>label</em> is what the reader sees, so it may hold anything as long as it cannot close the
 * quotes around it. Callers state the type name and the sentence they mean — the diagram builders
 * turn them into both, which is why nothing here is public.</p>
 *
 * @since 7.0.0
 */
final class Mermaid {

    private Mermaid() {}

    /**
     * Turns a name into an identifier the diagram syntax accepts. Two names that differ only by
     * characters the syntax refuses collapse onto the same identifier, which is what makes a node
     * and an arrow to it agree.
     */
    static String identifier(String name) {
        Objects.requireNonNull(name, "name must not be null");
        String sanitized = name.replaceAll("[^a-zA-Z0-9]", "_");
        return sanitized.isEmpty() ? "_" : sanitized;
    }

    /**
     * Makes text safe inside the quotes of a label — including the newlines and the quotes that
     * would end it early.
     */
    static String label(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return text.replace("\"", "&quot;").replaceAll("\\s*\\R\\s*", " ");
    }
}
