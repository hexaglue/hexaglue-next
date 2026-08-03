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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A markdown document, written by calling what it is rather than what it looks like.
 *
 * <p>Every renderer that ever assembled markdown by appending strings ended up escaping in some
 * places and forgetting in others, which is how a type name with a pipe or an underscore quietly
 * broke a table. Here the structure is stated — a heading, a list item, a table cell — and the
 * escaping happens once, where the text enters the document.</p>
 *
 * @since 7.0.0
 */
public final class Markdown {

    private final List<String> lines = new ArrayList<>();

    private Markdown() {}

    /**
     * Starts an empty document.
     *
     * @return a new document
     */
    public static Markdown document() {
        return new Markdown();
    }

    /**
     * Returns the anchor GitHub-flavoured markdown gives a heading, so a link to it can be written
     * without guessing.
     *
     * @param heading the heading text
     * @return the anchor, without its leading hash
     */
    public static String anchorOf(String heading) {
        Objects.requireNonNull(heading, "heading must not be null");
        String slug = heading.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("^-+|-+$", "");
    }

    /**
     * Renders text in bold.
     *
     * @param text the text
     * @return the emphasised text
     */
    public static String bold(String text) {
        return "**" + escape(text) + "**";
    }

    /**
     * Renders text as inline code, where nothing needs escaping because nothing is interpreted.
     *
     * @param text the text
     * @return the text between backticks
     */
    public static String inlineCode(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return "`" + text + "`";
    }

    /**
     * Renders a link.
     *
     * @param text what the reader sees
     * @param target where it goes
     * @return the link
     */
    public static String link(String text, String target) {
        Objects.requireNonNull(target, "target must not be null");
        return "[" + escape(text) + "](" + target + ")";
    }

    /**
     * Escapes the markdown control characters of a run of text.
     *
     * @param text the text
     * @return the text, inert
     */
    static String escape(String text) {
        Objects.requireNonNull(text, "text must not be null");
        StringBuilder escaped = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            if ("\\`*_{}[]()#+-.!|<>".indexOf(character) >= 0) {
                escaped.append('\\');
            }
            escaped.append(character);
        }
        return escaped.toString();
    }

    /**
     * Adds a heading.
     *
     * @param level the heading level, from 1 to 6
     * @param text the heading text
     * @return this document
     */
    public Markdown heading(int level, String text) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("heading level must be between 1 and 6, got: " + level);
        }
        Objects.requireNonNull(text, "text must not be null");
        return line("#".repeat(level) + " " + text).blank();
    }

    /**
     * Adds a paragraph.
     *
     * @param text the paragraph text, already rendered
     * @return this document
     */
    public Markdown paragraph(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return line(text).blank();
    }

    /**
     * Adds one item of a bullet list.
     *
     * @param text the item text, already rendered
     * @return this document
     */
    public Markdown bullet(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return line("- " + text);
    }

    /**
     * Adds a table.
     *
     * @param table the table
     * @return this document
     */
    public Markdown table(Table table) {
        Objects.requireNonNull(table, "table must not be null");
        return line(table.render()).blank();
    }

    /**
     * Adds a fenced code block.
     *
     * @param language the language the fence announces, empty for none
     * @param code the code, verbatim
     * @return this document
     */
    public Markdown code(String language, String code) {
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(code, "code must not be null");
        return line("```" + language).line(code.stripTrailing()).line("```").blank();
    }

    /**
     * Adds a section the reader opens.
     *
     * @param summary what the closed section says
     * @param body the markdown revealed when it opens
     * @return this document
     */
    public Markdown collapsible(String summary, String body) {
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(body, "body must not be null");
        return line("<details>")
                .line("<summary>" + summary + "</summary>")
                .blank()
                .line(body.stripTrailing())
                .blank()
                .line("</details>")
                .blank();
    }

    /**
     * Adds a horizontal rule.
     *
     * @return this document
     */
    public Markdown rule() {
        return line("---").blank();
    }

    /**
     * Adds a blank line, unless the document already ends with one.
     *
     * @return this document
     */
    public Markdown blank() {
        if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) {
            lines.add("");
        }
        return this;
    }

    private Markdown line(String text) {
        Collections.addAll(lines, text.split("\n", -1));
        return this;
    }

    /**
     * Renders the document, ending with exactly one newline.
     *
     * @return the markdown
     */
    public String render() {
        List<String> written = new ArrayList<>(lines);
        while (!written.isEmpty() && written.get(written.size() - 1).isEmpty()) {
            written.remove(written.size() - 1);
        }
        return String.join("\n", written) + "\n";
    }
}
