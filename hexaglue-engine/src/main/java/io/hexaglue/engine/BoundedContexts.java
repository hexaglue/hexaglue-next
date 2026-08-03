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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.config.AnalysisScope;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The parts a codebase is divided into, read from where its types live.
 *
 * <p>A context is the first package segment <strong>after the root of the codebase</strong> —
 * never a segment at a fixed depth. {@code com.acme.orders.domain.Order} and
 * {@code io.shop.orders.domain.Order} both sit in {@code orders}, although one root is two
 * segments deep and the other three. Counting from the left instead gives {@code acme} and
 * {@code shop}: one context for the whole codebase, which is the same as none.</p>
 *
 * <p>The root is the configured base package when there is one. When there is not, it is the
 * longest package prefix every analysed type shares — which is the same answer for a codebase
 * that has a root, and an honest one for a codebase that has several.</p>
 *
 * <p>There is one of these, and every rule and every plugin that needs contexts reads it. Two
 * implementations of this question is how a report came to disagree with the audit it was part
 * of.</p>
 *
 * @since 7.0.0
 */
public final class BoundedContexts {

    private final String root;
    private final Map<TypeId, String> byType;
    private final List<String> names;

    private BoundedContexts(String root, SortedMap<TypeId, String> byType) {
        this.root = root;
        // Not Map.copyOf: its iteration order is unspecified, and the types of a context are
        // rendered in the order this map hands them over.
        this.byType = Collections.unmodifiableSortedMap(new TreeMap<>(byType));
        this.names = List.copyOf(new TreeSet<>(byType.values()));
    }

    /**
     * Reads the contexts of an analysis.
     *
     * @param perimeter the types the analysis is about
     * @param scope where the analysis says the codebase starts
     * @return the contexts
     */
    public static BoundedContexts of(Perimeter perimeter, AnalysisScope scope) {
        Objects.requireNonNull(perimeter, "perimeter must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        List<String> packages = perimeter.types().stream()
                .map(node -> node.id().packageName())
                .distinct()
                .sorted()
                .toList();
        String root = scope.basePackage().orElseGet(() -> commonPrefixOf(packages));
        SortedMap<TypeId, String> byType = new TreeMap<>();
        perimeter
                .types()
                .forEach(node ->
                        contextOf(root, node.id().packageName()).ifPresent(context -> byType.put(node.id(), context)));
        return new BoundedContexts(root, byType);
    }

    /**
     * The longest run of segments every package shares. A codebase whose packages have nothing in
     * common has no root, and then every first segment is a context.
     */
    private static String commonPrefixOf(List<String> packages) {
        if (packages.isEmpty()) {
            return "";
        }
        List<String> prefix = List.of(packages.get(0).split("\\."));
        for (String name : packages) {
            List<String> segments = List.of(name.split("\\."));
            int shared = 0;
            while (shared < prefix.size()
                    && shared < segments.size()
                    && prefix.get(shared).equals(segments.get(shared))) {
                shared++;
            }
            prefix = prefix.subList(0, shared);
        }
        return String.join(".", prefix);
    }

    private static Optional<String> contextOf(String root, String packageName) {
        String remainder = remainderOf(root, packageName);
        if (remainder.isEmpty()) {
            return Optional.empty();
        }
        int separator = remainder.indexOf('.');
        return Optional.of(separator < 0 ? remainder : remainder.substring(0, separator));
    }

    private static String remainderOf(String root, String packageName) {
        if (root.isEmpty()) {
            return packageName;
        }
        if (packageName.equals(root)) {
            return "";
        }
        return packageName.startsWith(root + ".") ? packageName.substring(root.length() + 1) : "";
    }

    /**
     * Returns where the codebase starts, as this reading understood it.
     *
     * @return the root package, empty when the analysed packages share nothing
     */
    public String root() {
        return root;
    }

    /**
     * Returns the contexts, in name order.
     *
     * @return the context names
     */
    public List<String> names() {
        return names;
    }

    /**
     * Returns the context a type belongs to.
     *
     * @param type the type
     * @return its context, empty when the type sits directly at the root
     */
    public Optional<String> of(TypeId type) {
        return Optional.ofNullable(byType.get(type));
    }

    /**
     * Returns the types of one context, in identity order.
     *
     * @param context the context name
     * @return its types
     */
    public List<TypeId> typesOf(String context) {
        return byType.entrySet().stream()
                .filter(entry -> entry.getValue().equals(context))
                .map(Map.Entry::getKey)
                .toList();
    }
}
