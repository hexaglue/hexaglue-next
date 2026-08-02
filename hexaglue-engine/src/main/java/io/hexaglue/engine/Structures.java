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
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * How a declaration is carried over into the classified model.
 *
 * <p>Nothing here is a reading: the structure of a type in the model is the declaration the sources
 * hold, member for member. The one thing the code model states the other way round is nesting — a
 * type names the one enclosing it, and the model names the ones it encloses — so the map is turned
 * over once for the whole run rather than searched per type.</p>
 */
final class Structures {

    private final Map<TypeId, List<TypeRef>> nestedByEnclosing;

    private Structures(Map<TypeId, List<TypeRef>> nestedByEnclosing) {
        this.nestedByEnclosing = nestedByEnclosing;
    }

    /**
     * Turns the enclosing links of the code model over, once.
     *
     * @param code the analyzed sources
     * @return the reader of structures for that code model
     */
    static Structures of(CodeModel code) {
        Map<TypeId, List<TypeRef>> nested = new TreeMap<>();
        for (TypeNode type : code.types()) {
            type.enclosingType()
                    .ifPresent(enclosing -> nested.computeIfAbsent(enclosing, key -> new ArrayList<>())
                            .add(TypeRef.of(type.id().qualifiedName())));
        }
        return new Structures(nested);
    }

    /**
     * Returns the declaration of the given type as the model holds it.
     *
     * @param type the analyzed declaration
     * @return its structure
     */
    TypeStructure of(TypeNode type) {
        TypeStructure.Builder structure = TypeStructure.builder(type.nature())
                .modifiers(type.modifiers())
                .interfaces(type.interfaces())
                .permittedSubtypes(type.permittedSubtypes())
                .fields(type.fields())
                .methods(type.methods())
                .constructors(type.constructors())
                .annotations(type.annotations())
                .nestedTypes(nestedByEnclosing.getOrDefault(type.id(), List.of()));
        type.documentation().ifPresent(structure::documentation);
        type.superClass().ifPresent(structure::superClass);
        type.sourceLocation().ifPresent(structure::sourceLocation);
        return structure.build();
    }

    /**
     * Returns the single value a type is written around, when it is written around exactly one.
     *
     * <p>An identity carries a value the storage side has to name, and an aggregate identified by
     * one is stored under that value rather than under the wrapper. Nothing is read here: which
     * types are identities was settled long before, and this only names what one of them holds.</p>
     *
     * @param type the declaration to read, absent when the type is not in the sources
     * @return the wrapped value, empty when the type holds none or holds several
     */
    static Optional<TypeRef> wrappedValueOf(Optional<TypeNode> type) {
        return type.map(Structures::state)
                .filter(state -> state.size() == 1)
                .map(state -> state.get(0).type())
                .filter(value -> !value.isCollectionLike() && !value.isMapLike() && !value.isOptionalLike());
    }

    /**
     * Returns the fields carrying the state of a declaration; a static field belongs to the class,
     * not to any of its instances.
     *
     * @param type the declaration to read
     * @return its state fields, in declaration order
     */
    static List<Field> state(TypeNode type) {
        return type.fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .toList();
    }
}
