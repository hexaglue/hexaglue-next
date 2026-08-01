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

package io.hexaglue.model.code;

import io.hexaglue.model.TypeId;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A typed edge of the code graph, carrying its provenance: the member, parameter position or type
 * argument position where the relation appears in source. The provenance is what lets a rule — and
 * later an explanation — say <em>why</em> the edge exists, without re-parsing anything.
 *
 * @param source the type the relation starts from
 * @param target the type the relation points to, possibly an external stub
 * @param kind the relation kind
 * @param memberName the member (field, method, constructor) carrying the relation, when any
 * @param parameterIndex the 0-based parameter position, for parameter-borne relations
 * @param typeArgumentIndex the 0-based type-argument position, for TYPE_ARGUMENT relations
 * @since 7.0.0
 */
public record Edge(
        TypeId source,
        TypeId target,
        EdgeKind kind,
        Optional<String> memberName,
        OptionalInt parameterIndex,
        OptionalInt typeArgumentIndex) {

    /**
     * Validates that every component is present.
     */
    public Edge {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(memberName, "memberName must not be null");
        Objects.requireNonNull(parameterIndex, "parameterIndex must not be null");
        Objects.requireNonNull(typeArgumentIndex, "typeArgumentIndex must not be null");
    }

    /**
     * Creates an edge without member provenance, for type-declaration-level relations
     * (extends, implements, permits, annotated-by, declares).
     *
     * @param source the source type
     * @param kind the relation kind
     * @param target the target type
     * @return a new Edge
     */
    public static Edge of(TypeId source, EdgeKind kind, TypeId target) {
        return new Edge(source, target, kind, Optional.empty(), OptionalInt.empty(), OptionalInt.empty());
    }

    /**
     * Returns a compact, deterministic rendering of this edge with its provenance.
     *
     * @return the display string (e.g. {@code com.a.OrderRepository -EXTENDS-> o.s.JpaRepository})
     */
    public String toDisplayString() {
        StringBuilder display = new StringBuilder(64)
                .append(source.qualifiedName())
                .append(" -")
                .append(kind)
                .append("-> ")
                .append(target.qualifiedName());
        memberName.ifPresent(member -> display.append(" @").append(member));
        parameterIndex.ifPresent(
                index -> display.append("[param ").append(index).append("]"));
        typeArgumentIndex.ifPresent(
                index -> display.append("[arg ").append(index).append("]"));
        return display.toString();
    }
}
