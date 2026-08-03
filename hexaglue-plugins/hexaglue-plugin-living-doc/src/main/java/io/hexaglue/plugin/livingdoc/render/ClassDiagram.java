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
import java.util.List;
import java.util.Objects;

/**
 * A Mermaid class diagram: the types, what they hold, and how they relate.
 *
 * <p>The relations are named after what they mean rather than after the arrow that draws them —
 * a composition, an ownership, an announcement — so a caller states the architectural fact and
 * never picks glyphs.</p>
 *
 * @since 7.0.0
 */
public final class ClassDiagram {

    /**
     * What one type says about another.
     *
     * @since 7.0.0
     */
    public enum Relation {

        /** The first type is made of the second, whose lifetime it governs. */
        COMPOSITION("*--"),

        /** The first type holds the second, which lives on without it. */
        AGGREGATION("o--"),

        /** The first type knows the second. */
        ASSOCIATION("-->"),

        /** The first type uses the second without holding it. */
        DEPENDENCY(".."),

        /** The first type is a kind of the second. */
        INHERITANCE("--|>");

        private final String arrow;

        Relation(String arrow) {
            this.arrow = arrow;
        }
    }

    private final List<String> lines = new ArrayList<>(List.of("classDiagram"));

    private ClassDiagram() {}

    /**
     * Starts an empty diagram.
     *
     * @return a new diagram
     */
    public static ClassDiagram empty() {
        return new ClassDiagram();
    }

    /**
     * Adds a type with what it holds.
     *
     * @param name the type name
     * @param stereotype what kind of type it is, empty for none
     * @param members the members to show, in the order they should read
     * @return this diagram
     */
    public ClassDiagram type(String name, String stereotype, List<String> members) {
        Objects.requireNonNull(stereotype, "stereotype must not be null");
        Objects.requireNonNull(members, "members must not be null");
        lines.add("    class " + Mermaid.identifier(name) + " {");
        if (!stereotype.isBlank()) {
            lines.add("        <<" + Mermaid.label(stereotype) + ">>");
        }
        members.forEach(member -> lines.add("        " + Mermaid.label(member)));
        lines.add("    }");
        return this;
    }

    /**
     * Relates two types.
     *
     * @param from the type the relation starts at
     * @param relation what it says
     * @param to the type it reaches
     * @return this diagram
     */
    public ClassDiagram relate(String from, Relation relation, String to) {
        Objects.requireNonNull(relation, "relation must not be null");
        lines.add("    " + Mermaid.identifier(from) + " " + relation.arrow + " " + Mermaid.identifier(to));
        return this;
    }

    /**
     * Relates two types, saying what the relation is.
     *
     * @param from the type the relation starts at
     * @param relation what it says
     * @param to the type it reaches
     * @param label what the reader sees on the line
     * @return this diagram
     */
    public ClassDiagram relate(String from, Relation relation, String to, String label) {
        Objects.requireNonNull(relation, "relation must not be null");
        lines.add("    " + Mermaid.identifier(from) + " " + relation.arrow + " " + Mermaid.identifier(to) + " : "
                + Mermaid.label(label));
        return this;
    }

    /**
     * Attaches a note to a type.
     *
     * @param type the type the note is about
     * @param text what the note says
     * @return this diagram
     */
    public ClassDiagram note(String type, String text) {
        lines.add("    note for " + Mermaid.identifier(type) + " \"" + Mermaid.label(text) + "\"");
        return this;
    }

    /**
     * Renders the diagram.
     *
     * @return the Mermaid source, without a fence and without a trailing newline
     */
    public String render() {
        return String.join("\n", lines);
    }
}
