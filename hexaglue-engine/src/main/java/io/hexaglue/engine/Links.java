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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What the analysis reached about a type, in the two forms the records need it.
 *
 * <p>A record asks for more than a kind, and the extra comes from one of two places, never both.
 * What a rule <em>decided</em> is read back from the tie that rule stated — which aggregate a way
 * out keeps, which value carries an identity, what an owner is made of. What the <em>sources say
 * plainly</em> is read from the declaration: the contracts a type answers, the collaborators it is
 * handed. The line matters: a decision recomputed here would be a second definition free to drift
 * from the first, while a declaration re-read is the same text read twice.</p>
 */
final class Links {

    private final CodeModel code;
    private final Perimeter perimeter;
    private final FactBase facts;
    private final Verdicts verdicts;

    Links(EngineContext context, FactBase facts, Verdicts verdicts) {
        this.code = context.code();
        this.perimeter = context.perimeter();
        this.facts = facts;
        this.verdicts = verdicts;
    }

    CodeModel code() {
        return code;
    }

    /** Answers whether the settled verdicts read the given type as that kind. */
    boolean is(TypeId id, ArchKind kind) {
        return verdicts.kindOf(id).filter(kind::equals).isPresent();
    }

    boolean isAPort(TypeId id) {
        return verdicts.kindOf(id).filter(ArchKind::isPort).isPresent();
    }

    /** The types a link of that shape runs from the given subject to. */
    Stream<TypeId> objects(RelationKind kind, TypeId subject) {
        return facts.about(subject, Relation.class).stream()
                .filter(relation -> relation.kind() == kind)
                .map(Relation::object);
    }

    /** The types a link of that shape runs to the given object from. */
    Stream<TypeId> subjects(RelationKind kind, TypeId object) {
        return facts.all(Relation.class).stream()
                .filter(relation -> relation.kind() == kind)
                .filter(relation -> relation.object().equals(object))
                .map(Relation::subject);
    }

    /** The trade a way out was read to ply, general when no rule named one. */
    DrivenPortType roleOf(TypeId port) {
        return facts.about(port, PortRole.class).stream()
                .map(PortRole::role)
                .findFirst()
                .orElse(DrivenPortType.OTHER);
    }

    /**
     * Returns the types of the perimeter a declaration keeps: its state and whatever it is handed
     * to be built. Both are the same statement — this type needs that one — written in the two
     * places Java offers to write it.
     */
    Stream<TypeId> heldBy(TypeNode type) {
        return namedInPerimeter(Stream.concat(
                Structures.state(type).stream().map(Field::type),
                type.constructors().stream()
                        .map(Constructor::parameters)
                        .flatMap(List::stream)
                        .map(Parameter::type)));
    }

    /**
     * Returns the declarations of the perimeter answering the given contract. The inverse reading of
     * {@link #answeredBy(TypeNode)}, and the only way to reach the code behind a way in: a port
     * states what can be asked of it and nothing of what answering it involves.
     */
    Stream<TypeNode> answering(TypeId contract) {
        return perimeter.types().stream()
                .filter(type -> !type.id().equals(contract))
                .filter(type -> answeredBy(type).anyMatch(contract::equals));
    }

    /** Returns the contracts a declaration answers, named outright or inherited along the way. */
    Stream<TypeId> answeredBy(TypeNode type) {
        return Stream.concat(
                        Stream.concat(type.superClass().stream(), type.interfaces().stream())
                                .map(reference -> TypeId.of(reference.qualifiedName())),
                        code.supertypesOf(type.id()).stream())
                .distinct();
    }

    Stream<TypeId> namedInPerimeter(Stream<TypeRef> references) {
        return references
                .map(TypeRef::unwrapElement)
                .filter(TypeRef.Named.class::isInstance)
                .map(reference -> TypeId.of(reference.qualifiedName()))
                .filter(perimeter::contains)
                .distinct();
    }

    /**
     * Answers with the single subject of a link, and with nothing when there are several. Two
     * answers to a question that admits one are a report to write, not a coin to flip.
     */
    static Optional<TypeId> single(Stream<TypeId> candidates) {
        List<TypeId> found = candidates.distinct().limit(2).toList();
        return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
    }

    static List<TypeRef> references(Stream<TypeId> ids) {
        return ids.distinct().map(Links::reference).toList();
    }

    static TypeRef reference(TypeId id) {
        return TypeRef.of(id.qualifiedName());
    }
}
