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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TableTest {

    @Test
    @DisplayName("renders a header, a separator and one line per row")
    void rendersATable() {
        String rendered = Table.withHeaders("Type", "Kind")
                .row("Order", "AGGREGATE_ROOT")
                .row("OrderId", "IDENTIFIER")
                .render();

        assertThat(rendered).isEqualTo("""
                        | Type | Kind |
                        | --- | --- |
                        | Order | AGGREGATE_ROOT |
                        | OrderId | IDENTIFIER |""");
    }

    @Test
    @DisplayName("renders a header alone when nothing was added")
    void rendersAnEmptyTable() {
        Table table = Table.withHeaders("Type");

        assertThat(table.isEmpty()).isTrue();
        assertThat(table.render()).isEqualTo("| Type |\n| --- |");
    }

    @Test
    @DisplayName("keeps a pipe inside a cell from ending the row")
    void escapesAPipe() {
        String rendered = Table.withHeaders("Signature").row("find(a|b)").render();

        assertThat(rendered).contains("| find(a\\|b) |");
    }

    @Test
    @DisplayName("folds a multi-line value onto the single line a row has")
    void foldsNewlines() {
        String rendered =
                Table.withHeaders("Doc").row("first line\n   second line").render();

        assertThat(rendered).contains("| first line second line |");
    }

    @Test
    @DisplayName("refuses a row that does not match the columns")
    void refusesAMisshapenRow() {
        assertThatThrownBy(() -> Table.withHeaders("Type", "Kind").row("Order"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 cells for 2 columns");
    }

    @Test
    @DisplayName("refuses a table with no column")
    void refusesAColumnlessTable() {
        assertThatThrownBy(Table::withHeaders).isInstanceOf(IllegalArgumentException.class);
    }
}
