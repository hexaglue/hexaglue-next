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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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

    private Perimeter(List<TypeNode> types) {
        this.types = List.copyOf(types);
        Set<TypeId> collected = new LinkedHashSet<>();
        types.forEach(type -> collected.add(type.id()));
        this.ids = Set.copyOf(collected);
    }

    /**
     * Determines which types of the model the given scope covers.
     *
     * @param model the analyzed code model
     * @param scope the configured analysis scope
     * @return the perimeter, in identity order
     */
    public static Perimeter of(CodeModel model, AnalysisScope scope) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        return new Perimeter(model.types().stream()
                .filter(type -> !type.external())
                .filter(type -> covers(scope, type.id()))
                .toList());
    }

    private static boolean covers(AnalysisScope scope, TypeId id) {
        if (scope.basePackage().isPresent() && !under(scope.basePackage().orElseThrow(), id)) {
            return false;
        }
        if (!scope.includePackages().isEmpty()
                && scope.includePackages().stream().noneMatch(prefix -> under(prefix, id))) {
            return false;
        }
        return scope.excludePackages().stream().noneMatch(prefix -> under(prefix, id));
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
}
