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

package io.hexaglue.plugin.livingdoc.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraphTest {

    @Test
    @DisplayName("draws grouped nodes and the arrows between them")
    void drawsAGraph() {
        String rendered = Graph.flowing(Graph.Direction.LEFT_TO_RIGHT)
                .group("core", "Domain")
                .node("com.acme.Order", "Order", Graph.Shape.BOX)
                .endGroup()
                .node("com.acme.OrderApi", "Order API", Graph.Shape.CIRCLE)
                .arrow("com.acme.OrderApi", "com.acme.Order", "drives")
                .render();

        assertThat(rendered).isEqualTo("""
                        graph LR
                            subgraph core["Domain"]
                                com_acme_Order["Order"]
                            end
                            com_acme_OrderApi(("Order API"))
                            com_acme_OrderApi -->|"drives"| com_acme_Order""");
    }

    @Test
    @DisplayName("names a node and an arrow to it the same way")
    void agreesWithItselfOnIdentifiers() {
        String rendered = Graph.flowing(Graph.Direction.TOP_TO_BOTTOM)
                .node("com.acme-shop.Order", "Order", Graph.Shape.BOX)
                .arrow("com.acme-shop.Order", "com.acme-shop.Order")
                .render();

        assertThat(rendered)
                .contains("com_acme_shop_Order[\"Order\"]")
                .contains("com_acme_shop_Order --> com_acme_shop_Order");
    }

    @Test
    @DisplayName("keeps a quote inside an arrow label from ending it")
    void escapesAnArrowLabel() {
        String rendered = Graph.flowing(Graph.Direction.LEFT_TO_RIGHT)
                .arrow("a", "b", "says \"hello\"")
                .render();

        assertThat(rendered).contains("-->|\"says &quot;hello&quot;\"|");
    }

    @Test
    @DisplayName("keeps a quote inside a node label from ending it")
    void escapesANodeLabel() {
        String rendered = Graph.flowing(Graph.Direction.LEFT_TO_RIGHT)
                .node("a", "the \"main\" one", Graph.Shape.ROUNDED)
                .render();

        assertThat(rendered).contains("a(\"the &quot;main&quot; one\")");
    }

    @Test
    @DisplayName("refuses to render with a group left open")
    void refusesAnOpenGroup() {
        Graph graph = Graph.flowing(Graph.Direction.LEFT_TO_RIGHT).group("core", "Domain");

        assertThatThrownBy(graph::render)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("core");
    }

    @Test
    @DisplayName("refuses to close a group that was never opened")
    void refusesAnUnopenedGroup() {
        assertThatThrownBy(() -> Graph.flowing(Graph.Direction.LEFT_TO_RIGHT).endGroup())
                .isInstanceOf(IllegalStateException.class);
    }
}
