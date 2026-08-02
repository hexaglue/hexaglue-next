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

package io.hexaglue.engine.rule;

import io.hexaglue.engine.Derivation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.stream.Stream;

/**
 * What the methods of a declaration are about: the types of the perimeter they name.
 *
 * <p>Two rules ask this and mean different things by it — one looks for the subject a way out
 * revolves around, the other for the domain types a behaviour spans — but the reading itself is the
 * same, and the two places it matters most are the containers and the boundary. A method returning
 * {@code Optional<Fleet>} or {@code List<Fleet>} is about {@code Fleet}, so containers are
 * unwrapped; a method returning a {@code String} is about nothing the analysis owns, so anything
 * outside the perimeter is dropped rather than counted as a subject.</p>
 *
 * @since 7.0.0
 */
final class Signatures {

    private Signatures() {}

    /**
     * Returns the types of the perimeter the declaration answers with, in declaration order.
     *
     * @param derivation the derivation the rule is running under
     * @param type the declaration to read
     * @return the returned types, duplicates kept
     */
    static List<TypeId> returned(Derivation derivation, TypeNode type) {
        return namedInPerimeter(derivation, type.methods().stream().map(Method::returnType));
    }

    /**
     * Returns the types of the perimeter the declaration is handed, in declaration order.
     *
     * @param derivation the derivation the rule is running under
     * @param type the declaration to read
     * @return the parameter types, duplicates kept
     */
    static List<TypeId> taken(Derivation derivation, TypeNode type) {
        return namedInPerimeter(
                derivation,
                type.methods().stream()
                        .flatMap(method -> method.parameters().stream())
                        .map(Parameter::type));
    }

    /**
     * Returns every type of the perimeter the declaration's methods mention, both sides together.
     *
     * @param derivation the derivation the rule is running under
     * @param type the declaration to read
     * @return the mentioned types, duplicates kept
     */
    static List<TypeId> mentioned(Derivation derivation, TypeNode type) {
        return Stream.concat(returned(derivation, type).stream(), taken(derivation, type).stream())
                .toList();
    }

    /**
     * Returns the types of the perimeter the given references name, one container level unwrapped.
     *
     * @param derivation the derivation the rule is running under
     * @param references the references to read
     * @return the named types of the perimeter, in reference order
     */
    static List<TypeId> namedInPerimeter(Derivation derivation, Stream<TypeRef> references) {
        return references
                .map(TypeRef::unwrapElement)
                .filter(TypeRef.Named.class::isInstance)
                .map(reference -> TypeId.of(reference.qualifiedName()))
                .filter(id -> derivation.perimeter().contains(id))
                .toList();
    }
}
