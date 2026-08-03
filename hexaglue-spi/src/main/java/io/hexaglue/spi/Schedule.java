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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The order plugins run in, and what falls out of it.
 *
 * <p>Resolving the order takes <strong>two passes</strong>: one to learn which identifiers exist,
 * one to draw the edges between them. A single pass has to resolve a dependency against a plugin
 * it may not have read yet, and there is no answer to give at that point — which is exactly where
 * the order used to break, on nothing more exotic than plugins declared in reverse order.</p>
 *
 * <p>Nothing here throws. A dependency nobody provides and a cycle are each stated as an {@link
 * Exclusion} carrying its reason and the identifiers that explain it, and whatever depended on an
 * excluded plugin is excluded with it — running a plugin against half of what it asked for is
 * worse than not running it. A caller that wants to fail on any of this reads the exclusions; a
 * caller that wants to run what can run reads the order. Two plugins claiming one identifier is a
 * third thing, reported apart: the identifier still runs, through the first plugin that claimed
 * it.</p>
 *
 * <p>Plugins that do not depend on each other run in identifier order, so the same set of plugins
 * always produces the same run.</p>
 *
 * @since 7.0.0
 */
public final class Schedule {

    private final List<String> order;
    private final List<Exclusion> excluded;
    private final List<String> duplicates;
    private final Map<String, Set<String>> dependents;

    private Schedule(
            List<String> order,
            List<Exclusion> excluded,
            List<String> duplicates,
            Map<String, Set<String>> dependents) {
        this.order = List.copyOf(order);
        this.excluded = List.copyOf(excluded);
        this.duplicates = List.copyOf(duplicates);
        this.dependents = Map.copyOf(dependents);
    }

    /**
     * Why a plugin cannot run.
     *
     * @since 7.0.0
     */
    public enum Reason {

        /** A declared dependency is on no classpath. */
        MISSING_DEPENDENCY,

        /** The plugin takes part in a dependency cycle. */
        CYCLE,

        /** Something the plugin depends on is itself excluded. */
        BLOCKED
    }

    /**
     * One plugin kept out of the run.
     *
     * @param pluginId the plugin that cannot run
     * @param reason why it cannot
     * @param involved the identifiers that explain the reason — the missing dependencies, the
     *     other members of the cycle, the excluded dependency — in identifier order
     * @since 7.0.0
     */
    public record Exclusion(String pluginId, Reason reason, List<String> involved) {

        /**
         * Validates the exclusion and copies the identifiers.
         */
        public Exclusion {
            Objects.requireNonNull(pluginId, "pluginId must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(involved, "involved must not be null");
            involved = List.copyOf(involved);
        }
    }

    /**
     * Resolves the order of a set of plugins.
     *
     * @param manifests what the plugins declare, in discovery order
     * @return the schedule
     */
    public static Schedule of(List<PluginManifest> manifests) {
        Objects.requireNonNull(manifests, "manifests must not be null");
        Set<String> duplicates = new TreeSet<>();
        Map<String, PluginManifest> declared = declaredBy(manifests, duplicates);
        Map<String, Exclusion> keptOut = new TreeMap<>();
        Map<String, Set<String>> requirements = requirementsOf(declared, keptOut);
        Map<String, Set<String>> dependents = dependentsOf(declared);

        blockDependents(keptOut, dependents);
        excludeCycles(sort(requirements, keptOut).unresolved(), requirements, keptOut);
        blockDependents(keptOut, dependents);

        return new Schedule(
                sort(requirements, keptOut).order(),
                List.copyOf(keptOut.values()),
                List.copyOf(duplicates),
                dependents);
    }

    /**
     * First pass: learn which identifiers exist. Two plugins can claim the same identifier — a
     * stale copy left on a classpath — and only the first one read answers to it. That is a
     * redundant plugin, not a plugin kept out of the order: the identifier still runs.
     */
    private static Map<String, PluginManifest> declaredBy(List<PluginManifest> manifests, Set<String> duplicates) {
        Map<String, PluginManifest> declared = new LinkedHashMap<>();
        for (PluginManifest manifest : manifests) {
            if (declared.putIfAbsent(manifest.id(), manifest) != null) {
                duplicates.add(manifest.id());
            }
        }
        return declared;
    }

    /**
     * Second pass: draw the edges, now that every identifier is known. A dependency on nothing is
     * an exclusion rather than a dangling node, which is what keeps the graph total.
     */
    private static Map<String, Set<String>> requirementsOf(
            Map<String, PluginManifest> declared, Map<String, Exclusion> excluded) {
        Map<String, Set<String>> requirements = new TreeMap<>();
        for (PluginManifest manifest : declared.values()) {
            Set<String> resolved = new TreeSet<>();
            List<String> missing = new ArrayList<>();
            for (String dependency : manifest.dependsOn()) {
                if (declared.containsKey(dependency)) {
                    resolved.add(dependency);
                } else {
                    missing.add(dependency);
                }
            }
            requirements.put(manifest.id(), resolved);
            if (!missing.isEmpty()) {
                excluded.putIfAbsent(manifest.id(), new Exclusion(manifest.id(), Reason.MISSING_DEPENDENCY, missing));
            }
        }
        return requirements;
    }

    /**
     * Reverse index: who declared a dependency on whom.
     */
    private static Map<String, Set<String>> dependentsOf(Map<String, PluginManifest> declared) {
        Map<String, Set<String>> dependents = new TreeMap<>();
        for (PluginManifest manifest : declared.values()) {
            for (String dependency : manifest.dependsOn()) {
                dependents.computeIfAbsent(dependency, key -> new TreeSet<>()).add(manifest.id());
            }
        }
        Map<String, Set<String>> copy = new TreeMap<>();
        dependents.forEach((dependency, ids) -> copy.put(dependency, Set.copyOf(ids)));
        return copy;
    }

    /**
     * What the sort produced: the plugins it could place, and the ones it never could.
     */
    private record Sorted(List<String> order, List<String> unresolved) {}

    /**
     * Peels the plugins whose requirements are all satisfied, taking the ready ones in identifier
     * order. What never becomes ready either takes part in a cycle or waits on one.
     */
    private static Sorted sort(Map<String, Set<String>> requirements, Map<String, Exclusion> excluded) {
        Set<String> pending = new TreeSet<>(requirements.keySet());
        pending.removeAll(excluded.keySet());
        List<String> order = new ArrayList<>();
        boolean progressing = true;
        while (progressing) {
            progressing = false;
            for (String pluginId : List.copyOf(pending)) {
                if (satisfied(requirements.getOrDefault(pluginId, Set.of()), order, excluded)) {
                    order.add(pluginId);
                    pending.remove(pluginId);
                    progressing = true;
                }
            }
        }
        return new Sorted(order, List.copyOf(pending));
    }

    /**
     * A requirement is satisfied once the plugin carrying it has run, and settled once that plugin
     * is known not to run at all — a plugin waiting on an excluded one is itself excluded, so it
     * never reaches the order anyway.
     */
    private static boolean satisfied(Set<String> required, List<String> order, Map<String, Exclusion> excluded) {
        return required.stream().allMatch(dependency -> order.contains(dependency) || excluded.containsKey(dependency));
    }

    /**
     * Among the plugins the sort could not place, the ones actually in a cycle are those a
     * dependency path leads back to. The others are merely waiting on one, and saying so keeps the
     * diagnostic honest.
     */
    private static void excludeCycles(
            List<String> unresolved, Map<String, Set<String>> requirements, Map<String, Exclusion> excluded) {
        for (String pluginId : unresolved) {
            if (reachesItself(pluginId, requirements)) {
                excluded.putIfAbsent(
                        pluginId,
                        new Exclusion(
                                pluginId,
                                Reason.CYCLE,
                                unresolved.stream()
                                        .filter(other -> !other.equals(pluginId))
                                        .filter(other -> reachesItself(other, requirements))
                                        .toList()));
            }
        }
    }

    private static boolean reachesItself(String pluginId, Map<String, Set<String>> requirements) {
        Deque<String> pending = new ArrayDeque<>(requirements.getOrDefault(pluginId, Set.of()));
        Set<String> seen = new TreeSet<>();
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (current.equals(pluginId)) {
                return true;
            }
            if (seen.add(current)) {
                pending.addAll(requirements.getOrDefault(current, Set.of()));
            }
        }
        return false;
    }

    /**
     * An excluded plugin takes down what declared it, transitively.
     */
    private static void blockDependents(Map<String, Exclusion> excluded, Map<String, Set<String>> dependents) {
        Deque<String> causes = new ArrayDeque<>(excluded.keySet());
        while (!causes.isEmpty()) {
            String cause = causes.removeFirst();
            for (String dependent : dependents.getOrDefault(cause, Set.of())) {
                if (!excluded.containsKey(dependent)) {
                    excluded.put(dependent, new Exclusion(dependent, Reason.BLOCKED, List.of(cause)));
                    causes.addLast(dependent);
                }
            }
        }
    }

    /**
     * Returns the plugins that can run, dependencies first.
     *
     * @return the identifiers in execution order
     */
    public List<String> order() {
        return order;
    }

    /**
     * Returns what cannot run and why, in identifier order.
     *
     * @return the exclusions
     */
    public List<Exclusion> excluded() {
        return excluded;
    }

    /**
     * Returns the identifiers more than one plugin claimed, in identifier order. Each of them
     * still runs — through the first plugin that claimed it.
     *
     * @return the duplicated identifiers
     */
    public List<String> duplicates() {
        return duplicates;
    }

    /**
     * Returns every plugin that would run after a given one because it depends on it, directly or
     * through others.
     *
     * @param pluginId the plugin to start from
     * @return the identifiers of its dependents in identifier order, empty if nothing depends on it
     */
    public List<String> dependentsOf(String pluginId) {
        Objects.requireNonNull(pluginId, "pluginId must not be null");
        Set<String> reached = new TreeSet<>();
        Deque<String> pending = new ArrayDeque<>(dependents.getOrDefault(pluginId, Set.of()));
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (reached.add(current)) {
                pending.addAll(dependents.getOrDefault(current, Set.of()));
            }
        }
        reached.remove(pluginId);
        return List.copyOf(reached);
    }
}
