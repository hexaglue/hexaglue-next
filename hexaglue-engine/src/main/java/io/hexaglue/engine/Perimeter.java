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
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The types the engine owes a verdict on.
 *
 * <p>Two things put a type outside it. A classpath stub is not the user's code: the engine reads
 * what it says about the types that reference it, and says nothing about it. And a type the
 * configured scope excludes was excluded on purpose.</p>
 *
 * <p>What the perimeter does <em>not</em> restrict is knowledge: a pack recognizes symbols
 * wherever they are, because knowing that an injected field is an {@code EntityManager} is how
 * the holder of that field gets classified.</p>
 *
 * @since 7.0.0
 */
public final class Perimeter {

    private final List<TypeNode> types;
    private final Set<TypeId> ids;
    private final List<Exclusion> excluded;

    private Perimeter(List<TypeNode> types, List<Exclusion> excluded) {
        this.types = List.copyOf(types);
        Set<TypeId> collected = new LinkedHashSet<>();
        types.forEach(type -> collected.add(type.id()));
        this.ids = Set.copyOf(collected);
        this.excluded = List.copyOf(excluded);
    }

    /**
     * Why a type the sources declare is owed no verdict.
     *
     * <p>Only the user's own code is accounted for: a classpath stub was never a candidate, and
     * counting it would drown the ones that were.</p>
     *
     * @param type the type left out
     * @param reason what to say after "the type was read but not classified:"
     * @since 7.0.0
     */
    public record Exclusion(TypeId type, String reason) {

        /**
         * Validates that an exclusion names what it is about.
         */
        public Exclusion {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Determines which types of the model the given scope covers, and why the others do not.
     *
     * @param model the analyzed code model
     * @param scope the configured analysis scope
     * @return the perimeter, in identity order
     */
    public static Perimeter of(CodeModel model, AnalysisScope scope) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        List<TypeNode> covered = new ArrayList<>();
        List<Exclusion> excluded = new ArrayList<>();
        for (TypeNode type : model.types()) {
            if (type.external()) {
                continue;
            }
            exclusionOf(scope, type.id())
                    .ifPresentOrElse(reason -> excluded.add(new Exclusion(type.id(), reason)), () -> covered.add(type));
        }
        return new Perimeter(covered, excluded);
    }

    /**
     * Returns why the scope owes a type no verdict, or empty when it does.
     *
     * <p>The scope is read in the order a user states it: the base package first, then what was
     * asked for, then what was sent away.</p>
     */
    private static Optional<String> exclusionOf(AnalysisScope scope, TypeId id) {
        if (scope.basePackage().isPresent() && !under(scope.basePackage().orElseThrow(), id)) {
            return Optional.of("it is outside the configured base package "
                    + scope.basePackage().orElseThrow());
        }
        if (!scope.includePackages().isEmpty()
                && scope.includePackages().stream().noneMatch(prefix -> under(prefix, id))) {
            return Optional.of("its package is none of those the analysis includes: " + scope.includePackages());
        }
        return scope.excludePackages().stream()
                .filter(prefix -> under(prefix, id))
                .findFirst()
                .map(prefix -> "its package is excluded from the analysis by " + prefix);
    }

    /**
     * Answers whether a type sits under a package prefix, on a segment boundary: {@code com.acme}
     * covers {@code com.acme.Order}, never {@code com.acmetools.Order}.
     */
    private static boolean under(String prefix, TypeId id) {
        String packageName = id.packageName();
        return packageName.equals(prefix) || packageName.startsWith(prefix + ".");
    }

    /**
     * Returns the types owed a verdict, in identity order.
     *
     * @return the immutable list of types
     */
    public List<TypeNode> types() {
        return types;
    }

    /**
     * Returns whether the given type is owed a verdict.
     *
     * @param id the type id
     * @return true when the type is inside the perimeter
     */
    public boolean contains(TypeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return ids.contains(id);
    }

    /**
     * Returns the analyzed types the scope leaves without a verdict, in identity order.
     *
     * @return the immutable list of exclusions
     */
    public List<Exclusion> excluded() {
        return excluded;
    }
}
