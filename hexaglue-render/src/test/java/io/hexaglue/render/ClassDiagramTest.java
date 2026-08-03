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

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassDiagramTest {

    @Test
    @DisplayName("draws a type with its stereotype and its members")
    void drawsAType() {
        String rendered = ClassDiagram.empty()
                .type("com.acme.Order", "AggregateRoot", List.of("+OrderId id", "+List~Line~ lines"))
                .render();

        assertThat(rendered).isEqualTo("""
                        classDiagram
                            class com_acme_Order {
                                <<AggregateRoot>>
                                +OrderId id
                                +List~Line~ lines
                            }""");
    }

    @Test
    @DisplayName("draws a type without a stereotype when it has none")
    void drawsATypeWithoutStereotype() {
        String rendered = ClassDiagram.empty().type("Order", "", List.of()).render();

        assertThat(rendered).isEqualTo("classDiagram\n    class Order {\n    }");
    }

    @Test
    @DisplayName("says what a relation means rather than which arrow draws it")
    void drawsRelations() {
        String rendered = ClassDiagram.empty()
                .relate("Order", ClassDiagram.Relation.COMPOSITION, "Line")
                .relate("Order", ClassDiagram.Relation.ASSOCIATION, "Customer", "placed by")
                .relate("SqlOrders", ClassDiagram.Relation.INHERITANCE, "Orders")
                .render();

        assertThat(rendered).isEqualTo("""
                        classDiagram
                            Order *-- Line
                            Order --> Customer : placed by
                            SqlOrders --|> Orders""");
    }

    @Test
    @DisplayName("attaches a note to a type")
    void attachesANote() {
        String rendered =
                ClassDiagram.empty().note("Order", "identified by OrderId").render();

        assertThat(rendered).isEqualTo("classDiagram\n    note for Order \"identified by OrderId\"");
    }

    @Test
    @DisplayName("keeps a quote inside a member or a note from ending it")
    void escapesText() {
        String rendered = ClassDiagram.empty()
                .type("Order", "", List.of("+String label = \"none\""))
                .note("Order", "the \"main\" aggregate")
                .render();

        assertThat(rendered).contains("+String label = &quot;none&quot;").contains("the &quot;main&quot; aggregate");
    }
}
