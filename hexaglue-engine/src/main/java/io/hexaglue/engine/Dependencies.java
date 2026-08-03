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

package io.hexaglue.engine;

import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.TypeNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Which package depends on which, and what that says about each of them.
 *
 * <p>The edges are the ones the frontend already recorded: a supertype, a field type, a signature,
 * an invocation. Deriving a second set of dependencies by walking type structures again would be a
 * second answer to the same question, and the two would disagree the day one of them changed.</p>
 *
 * <p>Two edge kinds are deliberately left out. {@code DECLARES} joins a type to its nested types,
 * which are in its own package by construction — a coupling of a package with itself is not a
 * dependency. {@code PERMITS} is the sealed-hierarchy mirror of {@code EXTENDS}: counting both
 * would make every sealed type spread across two packages look like a cycle, when only one arrow
 * is really there.</p>
 *
 * <p>Only packages of the perimeter are counted. A package's dependency on the JDK or on a
 * framework says nothing about the architecture being analysed, and counting it would drown the
 * measure that does.</p>
 *
 * <p>The graph is built once and never changes, so everything derived from it — the cycles, the
 * measures — is computed once and kept.</p>
 *
 * @since 7.0.0
 */
public final class Dependencies {

    private static final Set<EdgeKind> COUPLING = Set.of(
            EdgeKind.EXTENDS,
            EdgeKind.IMPLEMENTS,
            EdgeKind.ANNOTATED_BY,
            EdgeKind.FIELD_TYPE,
            EdgeKind.RETURN_TYPE,
            EdgeKind.PARAMETER_TYPE,
            EdgeKind.THROWS_TYPE,
            EdgeKind.TYPE_ARGUMENT,
            EdgeKind.INVOKES);

    private final Map<String, Set<String>> outgoing;
    private final Map<String, Set<String>> incoming;
    private final Map<String, Stability> stabilities;
    private final List<List<String>> cycles;

    private Dependencies(
            Map<String, Set<String>> dependencies, Map<String, Integer> types, Map<String, Integer> abstractTypes) {
        this.outgoing = dependencies;
        this.incoming = reverse(dependencies);
        this.cycles = cyclesOf(dependencies);
        this.stabilities = measure(dependencies, this.incoming, types, abstractTypes);
    }

    /**
     * Builds the package graph of an analysis.
     *
     * @param code the code model holding the edges
     * @param perimeter the types the analysis is about
     * @return the graph
     */
    public static Dependencies of(CodeModel code, Perimeter perimeter) {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(perimeter, "perimeter must not be null");

        Map<String, Integer> types = new TreeMap<>();
        Map<String, Integer> abstractTypes = new TreeMap<>();
        for (TypeNode node : perimeter.types()) {
            types.merge(node.id().packageName(), 1, Integer::sum);
            abstractTypes.merge(node.id().packageName(), isAbstract(node) ? 1 : 0, Integer::sum);
        }

        Map<String, Set<String>> dependencies = new TreeMap<>();
        types.keySet().forEach(name -> dependencies.put(name, new TreeSet<>()));
        for (Edge edge : code.edges()) {
            if (!COUPLING.contains(edge.kind())
                    || !perimeter.contains(edge.source())
                    || !perimeter.contains(edge.target())) {
                continue;
            }
            String from = edge.source().packageName();
            String to = edge.target().packageName();
            if (!from.equals(to)) {
                dependencies.computeIfAbsent(from, name -> new TreeSet<>()).add(to);
            }
        }
        return new Dependencies(dependencies, types, abstractTypes);
    }

    private static boolean isAbstract(TypeNode node) {
        return node.nature() == TypeNature.INTERFACE || node.modifiers().contains(Modifier.ABSTRACT);
    }

    private static Map<String, Set<String>> reverse(Map<String, Set<String>> dependencies) {
        Map<String, Set<String>> dependents = new TreeMap<>();
        dependencies.keySet().forEach(name -> dependents.put(name, new TreeSet<>()));
        dependencies.forEach((from, targets) -> targets.forEach(
                to -> dependents.computeIfAbsent(to, name -> new TreeSet<>()).add(from)));
        return dependents;
    }

    /**
     * A knot of packages that reach each other is one fact, not one fact per path through it —
     * which is what a strongly connected component gives and what walking every path does not.
     */
    private static List<List<String>> cyclesOf(Map<String, Set<String>> dependencies) {
        Graph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
        dependencies.keySet().forEach(graph::addVertex);
        dependencies.forEach((from, targets) -> targets.forEach(to -> graph.addEdge(from, to)));
        List<List<String>> cycles = new ArrayList<>();
        new GabowStrongConnectivityInspector<>(graph)
                .stronglyConnectedSets().stream()
                        .filter(component -> component.size() > 1)
                        .map(component -> List.copyOf(new TreeSet<>(component)))
                        .sorted((left, right) -> left.get(0).compareTo(right.get(0)))
                        .forEach(cycles::add);
        return List.copyOf(cycles);
    }

    private static Map<String, Stability> measure(
            Map<String, Set<String>> dependencies,
            Map<String, Set<String>> dependents,
            Map<String, Integer> types,
            Map<String, Integer> abstractTypes) {
        Map<String, Stability> stabilities = new LinkedHashMap<>();
        dependencies.forEach((name, targets) -> stabilities.put(
                name,
                Stability.of(
                        name,
                        targets.size(),
                        dependents.getOrDefault(name, Set.of()).size(),
                        abstractTypes.getOrDefault(name, 0),
                        types.getOrDefault(name, 0))));
        return stabilities;
    }

    /**
     * Returns every package of the perimeter, in name order.
     *
     * @return the package names
     */
    public List<String> packages() {
        return List.copyOf(outgoing.keySet());
    }

    /**
     * Returns the packages one package depends on.
     *
     * @param packageName the package
     * @return the packages it depends on, in name order, empty for a package nobody analysed
     */
    public List<String> dependenciesOf(String packageName) {
        return List.copyOf(outgoing.getOrDefault(packageName, Set.of()));
    }

    /**
     * Returns the packages that depend on one package.
     *
     * @param packageName the package
     * @return the packages depending on it, in name order, empty for a package nobody analysed
     */
    public List<String> dependentsOf(String packageName) {
        return List.copyOf(incoming.getOrDefault(packageName, Set.of()));
    }

    /**
     * Returns the knots of packages that depend on each other, each knot once.
     *
     * @return the cycles, packages in name order and cycles in the order of their first package
     */
    public List<List<String>> cycles() {
        return List.copyOf(cycles);
    }

    /**
     * Returns how settled and how abstract every package is, in name order.
     *
     * @return the measures
     */
    public List<Stability> stabilities() {
        return List.copyOf(stabilities.values());
    }

    /**
     * Returns the measure of one package.
     *
     * @param packageName the package
     * @return the measure, or a package nobody analysed measured as empty
     */
    public Stability stabilityOf(String packageName) {
        return stabilities.getOrDefault(packageName, Stability.of(packageName, 0, 0, 0, 0));
    }

    /**
     * Returns the dependencies that point the wrong way: from a package that is hard to change
     * towards one that is not.
     *
     * <p>Depending on something less settled than yourself means every reason that thing has to
     * change becomes a reason for you to change — and you are the one nobody can afford to change.
     * The direction is what the whole check is about, so the canonical case is worth stating: a
     * package nothing depends on, depending on a package everything depends on, is fine; the
     * reverse is the violation.</p>
     *
     * @return the offending dependencies as pairs, in the order of the depending package
     */
    public List<Violation> unstableDependencies() {
        List<Violation> violations = new ArrayList<>();
        outgoing.forEach((from, targets) -> targets.forEach(to -> {
            Stability source = stabilityOf(from);
            Stability target = stabilityOf(to);
            if (target.instability() > source.instability()) {
                violations.add(new Violation(source, target));
            }
        }));
        return List.copyOf(violations);
    }

    /**
     * A dependency running from a settled package towards a less settled one.
     *
     * @param from the package that depends, and that is the harder of the two to change
     * @param on the package it depends on, which is freer to change than it is
     * @since 7.0.0
     */
    public record Violation(Stability from, Stability on) {

        /**
         * Validates both measures.
         */
        public Violation {
            Objects.requireNonNull(from, "from must not be null");
            Objects.requireNonNull(on, "on must not be null");
        }
    }
}
