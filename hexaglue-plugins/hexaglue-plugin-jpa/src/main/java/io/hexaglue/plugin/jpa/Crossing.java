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

package io.hexaglue.plugin.jpa;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainType;
import java.util.Objects;
import java.util.Optional;

/**
 * How one value crosses between the domain and the store, in either direction.
 *
 * <p>Both the mapper and the adapter ask this: the mapper of every field it carries, the adapter of
 * every value a port hands it. Asking it in one place is what keeps the two from disagreeing about
 * the same value — an adapter passing an identity the mapper stored differently would look right
 * and match no row.</p>
 *
 * <p>What can cross is settled by the verdict on what the value holds: a plain value goes over
 * untouched, an identity as the single value it is written around, a domain value through the
 * mapper written for it. Anything with a life of its own does not cross here — it is named by its
 * identity, which is the reference a store keeps — and neither does a collection, which would need
 * the domain to say how it takes one.</p>
 */
final class Crossing {

    /** Stands in for an expression when the question is whether there is a way across at all. */
    private static final CodeBlock ANY_VALUE = CodeBlock.of("");

    private final ArchModel model;
    private final JpaOptions options;

    Crossing(ArchModel model, JpaOptions options) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Returns what the given domain expression becomes on its way to the store.
     *
     * @param held what the expression holds
     * @param domain the expression, as the generated code reads it
     * @return the stored form, or empty when nothing carries this value across
     */
    Optional<CodeBlock> outward(TypeRef held, CodeBlock domain) {
        if (isContainer(held)) {
            return Optional.empty();
        }
        Optional<ArchKind> kind = kindOf(held);
        if (kind.isEmpty()) {
            return Optional.of(domain);
        }
        return switch (kind.orElseThrow()) {
            case IDENTIFIER -> unwrapAccessor(held).map(accessor -> CodeBlock.of("$L.$L()", domain, accessor));
            case VALUE_OBJECT ->
                isOneOfAClosedSet(held)
                        ? Optional.of(domain)
                        : carrier(held).map(mapper -> CodeBlock.of("$T.toEntity($L)", mapper, domain));
            default -> Optional.empty();
        };
    }

    /**
     * Returns what the given stored expression becomes on its way back to the domain.
     *
     * @param held what the domain holds there
     * @param row the expression, as the generated code reads it off the row
     * @return the domain form, or empty when nothing carries this value across
     */
    Optional<CodeBlock> inward(TypeRef held, CodeBlock row) {
        if (isContainer(held)) {
            return Optional.empty();
        }
        Optional<ArchKind> kind = kindOf(held);
        if (kind.isEmpty()) {
            return Optional.of(row);
        }
        return switch (kind.orElseThrow()) {
            case IDENTIFIER -> unwrapAccessor(held).map(ignored -> CodeBlock.of("new $T($L)", Stored.named(held), row));
            case VALUE_OBJECT ->
                isOneOfAClosedSet(held)
                        ? Optional.of(row)
                        : carrier(held).map(mapper -> CodeBlock.of("$T.toDomain($L)", mapper, row));
            default -> Optional.empty();
        };
    }

    /**
     * Answers whether a value of the given type has a way across at all.
     *
     * @param held what the value holds
     * @return true when it can be carried in both directions
     */
    boolean crosses(TypeRef held) {
        return outward(held, ANY_VALUE).isPresent();
    }

    /**
     * Returns the mapper written for a type, whether or not one was.
     *
     * @param type the domain type
     * @return the name the generated mapper goes by
     */
    ClassName mapperFor(TypeRef type) {
        return ClassName.get(type.packageName(), options.mapperFor(type.simpleName()));
    }

    /**
     * Whether the value is one of a closed set the type lists: it crosses as itself, having no
     * state to take apart and nothing for a mapper to carry.
     */
    private boolean isOneOfAClosedSet(TypeRef type) {
        return model.type(TypeId.of(type.qualifiedName()))
                .filter(declared -> declared.structure().nature() == TypeNature.ENUM)
                .isPresent();
    }

    /** The mapper of a type, when that type is one this backend can rebuild from a row. */
    private Optional<ClassName> carrier(TypeRef type) {
        return model.type(TypeId.of(type.qualifiedName()))
                .filter(DomainType.class::isInstance)
                .filter(DomainAccess::isRebuildable)
                .map(rebuildable -> mapperFor(type));
    }

    /** The single component an identity is written around, read from the identity itself. */
    private Optional<String> unwrapAccessor(TypeRef identity) {
        return model.type(TypeId.of(identity.qualifiedName()))
                .flatMap(held -> DomainAccess.state(held).stream()
                        .findFirst()
                        .flatMap(only -> DomainAccess.accessorOf(held, only)));
    }

    private static boolean isContainer(TypeRef type) {
        return type.isCollectionLike() || type.isOptionalLike() || type.isStreamLike() || type.isMapLike();
    }

    private Optional<ArchKind> kindOf(TypeRef held) {
        return model.type(TypeId.of(held.qualifiedName())).map(ArchType::kind);
    }
}
