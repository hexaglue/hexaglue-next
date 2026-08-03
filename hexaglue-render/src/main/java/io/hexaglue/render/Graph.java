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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * A Mermaid flow graph: boxes, the groups they sit in, and the arrows between them.
 *
 * <p>Nodes are named by whatever the caller has at hand — a qualified type name, a package, a
 * module — and the diagram turns that into an identifier its syntax accepts. Labels are escaped
 * on every path, arrow labels included: an unescaped one is how a diagram silently stopped
 * rendering.</p>
 *
 * @since 7.0.0
 */
public final class Graph {

    /**
     * Which way the graph flows.
     *
     * @since 7.0.0
     */
    public enum Direction {

        /** Left to right. */
        LEFT_TO_RIGHT("LR"),

        /** Top to bottom. */
        TOP_TO_BOTTOM("TB");

        private final String code;

        Direction(String code) {
            this.code = code;
        }
    }

    /**
     * What a node looks like.
     *
     * @since 7.0.0
     */
    public enum Shape {

        /** A rectangle: the default thing. */
        BOX("[\"", "\"]"),

        /** A rounded rectangle: something softer than a component. */
        ROUNDED("(\"", "\")"),

        /** A circle: an actor or an edge of the system. */
        CIRCLE("((\"", "\"))"),

        /** A diamond: a decision or a boundary. */
        DIAMOND("{\"", "\"}");

        private final String open;
        private final String close;

        Shape(String open, String close) {
            this.open = open;
            this.close = close;
        }
    }

    private final List<String> lines = new ArrayList<>();
    private final Deque<String> open = new ArrayDeque<>();
    private int depth = 1;

    private Graph(Direction direction) {
        lines.add("graph " + direction.code);
    }

    /**
     * Starts a graph flowing in the given direction.
     *
     * @param direction which way it flows
     * @return a new graph
     */
    public static Graph flowing(Direction direction) {
        Objects.requireNonNull(direction, "direction must not be null");
        return new Graph(direction);
    }

    /**
     * Opens a group of nodes.
     *
     * @param name the group name
     * @param label what the reader sees
     * @return this graph
     */
    public Graph group(String name, String label) {
        line("subgraph " + Mermaid.identifier(name) + "[\"" + Mermaid.label(label) + "\"]");
        open.push(name);
        depth++;
        return this;
    }

    /**
     * Closes the group most recently opened.
     *
     * @return this graph
     * @throws IllegalStateException if no group is open
     */
    public Graph endGroup() {
        if (open.isEmpty()) {
            throw new IllegalStateException("no group is open");
        }
        open.pop();
        depth--;
        return line("end");
    }

    /**
     * Adds a node.
     *
     * @param name the node name, which arrows refer to
     * @param label what the reader sees
     * @param shape what it looks like
     * @return this graph
     */
    public Graph node(String name, String label, Shape shape) {
        Objects.requireNonNull(shape, "shape must not be null");
        return line(Mermaid.identifier(name) + shape.open + Mermaid.label(label) + shape.close);
    }

    /**
     * Adds an arrow between two nodes.
     *
     * @param from the node it leaves
     * @param to the node it reaches
     * @return this graph
     */
    public Graph arrow(String from, String to) {
        return line(Mermaid.identifier(from) + " --> " + Mermaid.identifier(to));
    }

    /**
     * Adds an arrow carrying a label.
     *
     * @param from the node it leaves
     * @param to the node it reaches
     * @param label what the arrow says
     * @return this graph
     */
    public Graph arrow(String from, String to, String label) {
        return line(Mermaid.identifier(from) + " -->|\"" + Mermaid.label(label) + "\"| " + Mermaid.identifier(to));
    }

    private Graph line(String text) {
        lines.add("    ".repeat(depth) + text);
        return this;
    }

    /**
     * Renders the diagram.
     *
     * @return the Mermaid source, without a fence and without a trailing newline
     * @throws IllegalStateException if a group was left open
     */
    public String render() {
        if (!open.isEmpty()) {
            throw new IllegalStateException("group left open: " + open.peek());
        }
        return String.join("\n", lines);
    }
}
