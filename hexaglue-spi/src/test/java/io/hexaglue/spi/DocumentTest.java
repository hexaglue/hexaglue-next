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

package io.hexaglue.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A plugin names where its output goes; the host decides where that is. The path a plugin hands
 * over is therefore relative by construction — a plugin that could write outside the directory
 * the host chose would make the confinement a matter of good behaviour rather than of shape.
 */
class DocumentTest {

    @Test
    @DisplayName("accepts a relative path with directories")
    void acceptsARelativePath() {
        Document document = new Document("architecture/domain-model.md", "# Domain");

        assertThat(document.path()).isEqualTo("architecture/domain-model.md");
        assertThat(document.content()).isEqualTo("# Domain");
    }

    @Test
    @DisplayName("accepts an empty document")
    void acceptsAnEmptyDocument() {
        assertThat(new Document("empty.md", "").content()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/etc/passwd",
                "../outside.md",
                "docs/../../outside.md",
                "docs//double.md",
                "docs/",
                "C:\\windows\\system32",
                "docs\\windows.md",
                " "
            })
    @DisplayName("refuses any path that could leave the directory the host chose")
    void refusesAnEscapingPath(String path) {
        assertThatThrownBy(() -> new Document(path, "content")).isInstanceOf(IllegalArgumentException.class);
    }
}
