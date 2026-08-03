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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MarkdownTest {

    @Nested
    @DisplayName("structure")
    class Structure {

        @Test
        @DisplayName("writes headings, paragraphs and bullets in the order they were stated")
        void writesADocument() {
            String rendered = Markdown.document()
                    .heading(1, "Architecture")
                    .paragraph("What the model says.")
                    .heading(2, "Aggregates")
                    .bullet("Order")
                    .bullet("Customer")
                    .render();

            assertThat(rendered).isEqualTo("""
                            # Architecture

                            What the model says.

                            ## Aggregates

                            - Order
                            - Customer
                            """);
        }

        @Test
        @DisplayName("refuses a heading level markdown does not have")
        void refusesAnImpossibleHeading() {
            assertThatThrownBy(() -> Markdown.document().heading(7, "Too deep"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("never lets two blank lines pile up")
        void collapsesBlankLines() {
            String rendered = Markdown.document()
                    .paragraph("One")
                    .blank()
                    .blank()
                    .paragraph("Two")
                    .render();

            assertThat(rendered).isEqualTo("One\n\nTwo\n");
        }

        @Test
        @DisplayName("ends with exactly one newline")
        void endsWithOneNewline() {
            assertThat(Markdown.document().heading(1, "Title").render()).endsWith("# Title\n");
        }

        @Test
        @DisplayName("fences a code block with the language it announces")
        void fencesCode() {
            String rendered =
                    Markdown.document().code("mermaid", "graph LR\n    a --> b").render();

            assertThat(rendered).isEqualTo("""
                            ```mermaid
                            graph LR
                                a --> b
                            ```
                            """);
        }

        @Test
        @DisplayName("separates two parts with a rule, and renders a table it was given")
        void separatesAndTabulates() {
            String rendered = Markdown.document()
                    .paragraph("Before")
                    .rule()
                    .table(Table.withHeaders("Type").row("Order"))
                    .render();

            assertThat(rendered).isEqualTo("""
                            Before

                            ---

                            | Type |
                            | --- |
                            | Order |
                            """);
        }

        @Test
        @DisplayName("writes a section the reader opens")
        void writesACollapsibleSection() {
            String rendered =
                    Markdown.document().collapsible("Details", "- one\n- two").render();

            assertThat(rendered).isEqualTo("""
                            <details>
                            <summary>Details</summary>

                            - one
                            - two

                            </details>
                            """);
        }
    }

    @Nested
    @DisplayName("text that must not be read as markup")
    class Escaping {

        @Test
        @DisplayName("neutralises the control characters of emphasised text")
        void escapesBold() {
            assertThat(Markdown.bold("Order_Line")).isEqualTo("**Order\\_Line**");
        }

        @Test
        @DisplayName("leaves inline code alone, because nothing in it is interpreted")
        void leavesInlineCodeAlone() {
            assertThat(Markdown.inlineCode("List<Order_Line>")).isEqualTo("`List<Order_Line>`");
        }

        @Test
        @DisplayName("escapes the text of a link but not its target")
        void escapesLinkText() {
            assertThat(Markdown.link("Order_Line", "#order-line")).isEqualTo("[Order\\_Line](#order-line)");
        }
    }

    @Nested
    @DisplayName("anchors")
    class Anchors {

        @Test
        @DisplayName("gives a heading the anchor markdown will give it")
        void anchorsAHeading() {
            assertThat(Markdown.anchorOf("Domain Model")).isEqualTo("domain-model");
            assertThat(Markdown.anchorOf("Ports & Adapters")).isEqualTo("ports-adapters");
        }

        @Test
        @DisplayName("leaves no hyphen dangling at either end")
        void trimsHyphens() {
            assertThat(Markdown.anchorOf("(Order)")).isEqualTo("order");
        }
    }
}
