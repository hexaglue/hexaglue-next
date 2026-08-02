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

package io.hexaglue.frontend;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.TypeNode;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Computes, for every type of the model, the types it inherits from — transitively, across the
 * source hierarchy and the classpath alike.
 *
 * <p>This is what lets framework knowledge be stated once. {@code JpaRepository},
 * {@code MongoRepository} and every vendor interface derived from Spring Data's
 * {@code Repository} answer to a single fact about their common root, without HexaGlue shipping a
 * list of every derived name in existence — and without the sources of any of them.</p>
 *
 * <p>The implicit root of every Java hierarchy is left out: {@code java.lang.Object} tells nothing
 * apart. A declared cycle, which the language forbids but a partial parse can still produce, is
 * walked once and closed.</p>
 */
final class Supertypes {

    private static final TypeId OBJECT = TypeId.of("java.lang.Object");

    private Supertypes() {}

    /**
     * Computes the transitive supertypes of every node, nearest first.
     *
     * @param nodes the nodes of the model, analyzed types and external stubs alike
     * @param classpath the classpath entries carrying the compiled hierarchies
     * @return the closure by type identity, in identity order, without the empty ones
     */
    static Map<TypeId, List<TypeId>> closures(List<TypeNode> nodes, List<Path> classpath) {
        Map<TypeId, List<TypeId>> declared = declaredSupertypes(nodes);
        if (classpath.isEmpty()) {
            return closures(nodes, declared, type -> List.of());
        }
        try (ScanResult compiled = scan(classpath)) {
            return closures(nodes, declared, type -> compiledSupertypes(compiled, type));
        }
    }

    private static Map<TypeId, List<TypeId>> closures(
            List<TypeNode> nodes, Map<TypeId, List<TypeId>> declared, UnaryLookup compiled) {
        SortedMap<TypeId, List<TypeId>> closures = new TreeMap<>();
        for (TypeNode node : nodes) {
            List<TypeId> closure = walk(node.id(), declared, compiled);
            if (!closure.isEmpty()) {
                closures.put(node.id(), closure);
            }
        }
        return closures;
    }

    /** Reads what a type directly inherits from outside the analyzed sources. */
    @FunctionalInterface
    private interface UnaryLookup {
        List<TypeId> directSupertypesOf(TypeId type);
    }

    private static ScanResult scan(List<Path> classpath) {
        ClassGraph scanner = new ClassGraph().enableClassInfo();
        String entries = classpath.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        return scanner.overrideClasspath(entries).scan();
    }

    /**
     * Indexes the supertypes each node declares, so that a source hierarchy is walked from the
     * model rather than re-read from the parser.
     */
    private static Map<TypeId, List<TypeId>> declaredSupertypes(List<TypeNode> nodes) {
        SortedMap<TypeId, List<TypeId>> declared = new TreeMap<>();
        for (TypeNode node : nodes) {
            List<TypeId> direct = new ArrayList<>();
            node.superClass().flatMap(Supertypes::identityOf).ifPresent(direct::add);
            node.interfaces().stream()
                    .map(Supertypes::identityOf)
                    .flatMap(Optional::stream)
                    .forEach(direct::add);
            if (!direct.isEmpty()) {
                declared.put(node.id(), List.copyOf(direct));
            }
        }
        return declared;
    }

    private static List<TypeId> walk(TypeId start, Map<TypeId, List<TypeId>> declared, UnaryLookup compiled) {
        Set<TypeId> visited = new LinkedHashSet<>();
        Deque<TypeId> pending = new ArrayDeque<>(directSupertypes(start, declared, compiled));
        while (!pending.isEmpty()) {
            TypeId current = pending.removeFirst();
            if (OBJECT.equals(current) || !visited.add(current)) {
                continue;
            }
            pending.addAll(directSupertypes(current, declared, compiled));
        }
        return List.copyOf(visited);
    }

    /**
     * Returns what a type directly inherits from: what the analyzed sources declare when the type
     * is one of them, what the bytecode says otherwise.
     */
    private static List<TypeId> directSupertypes(
            TypeId type, Map<TypeId, List<TypeId>> declared, UnaryLookup compiled) {
        List<TypeId> fromSources = declared.get(type);
        return fromSources != null ? fromSources : compiled.directSupertypesOf(type);
    }

    private static List<TypeId> compiledSupertypes(ScanResult compiled, TypeId type) {
        ClassInfo info = compiled.getClassInfo(type.qualifiedName());
        if (info == null) {
            return List.of();
        }
        List<TypeId> direct = new ArrayList<>();
        ClassInfo superClass = info.getSuperclass();
        if (superClass != null) {
            direct.add(TypeId.of(superClass.getName()));
        }
        info.getInterfaces().directOnly().stream()
                .map(ClassInfo::getName)
                .sorted()
                .map(TypeId::of)
                .forEach(direct::add);
        return direct;
    }

    private static Optional<TypeId> identityOf(TypeRef reference) {
        return reference instanceof TypeRef.Named named
                ? Optional.of(TypeId.of(named.qualifiedName()))
                : Optional.empty();
    }
}
