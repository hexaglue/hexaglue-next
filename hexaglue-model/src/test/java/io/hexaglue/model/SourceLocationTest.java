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

package io.hexaglue.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SourceLocationTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("keeps file path and line range")
        void keepsFilePathAndLineRange() {
            SourceLocation location = new SourceLocation("src/main/java/com/example/Order.java", 10, 42);

            assertThat(location.filePath()).isEqualTo("src/main/java/com/example/Order.java");
            assertThat(location.lineStart()).isEqualTo(10);
            assertThat(location.lineEnd()).isEqualTo(42);
        }

        @Test
        @DisplayName("null file path is rejected")
        void nullFilePathIsRejected() {
            assertThatNullPointerException().isThrownBy(() -> new SourceLocation(null, 1, 1));
        }

        @Test
        @DisplayName("blank file path is rejected")
        void blankFilePathIsRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> new SourceLocation(" ", 1, 1));
        }

        @Test
        @DisplayName("line numbers below one are rejected")
        void lineNumbersBelowOneAreRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> new SourceLocation("Order.java", 0, 1));
        }

        @Test
        @DisplayName("an end line before the start line is rejected")
        void endLineBeforeStartLineIsRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> new SourceLocation("Order.java", 5, 4));
        }
    }

    @Nested
    @DisplayName("Display")
    class Display {

        @Test
        @DisplayName("file name strips the directory path")
        void fileNameStripsDirectories() {
            SourceLocation location = new SourceLocation("src/main/java/com/example/Order.java", 10, 42);

            assertThat(location.fileName()).isEqualTo("Order.java");
        }

        @Test
        @DisplayName("file name handles windows separators")
        void fileNameHandlesWindowsSeparators() {
            SourceLocation location = new SourceLocation("src\\main\\java\\Order.java", 1, 1);

            assertThat(location.fileName()).isEqualTo("Order.java");
        }

        @Test
        @DisplayName("file name handles a root-level absolute path")
        void fileNameHandlesRootLevelPath() {
            SourceLocation location = new SourceLocation("/Order.java", 1, 1);

            assertThat(location.fileName()).isEqualTo("Order.java");
        }

        @Test
        @DisplayName("display string is file name and start line")
        void displayStringIsFileNameAndStartLine() {
            SourceLocation location = new SourceLocation("src/main/java/com/example/Order.java", 10, 42);

            assertThat(location.toDisplayString()).isEqualTo("Order.java:10");
        }
    }
}
