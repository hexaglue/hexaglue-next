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
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a declaration says about each of its fields once the verdicts have settled.
 *
 * <p>A field is written once and read twice: the sources give its name, its type and its
 * annotations, and the analysis adds what it reached about the type that field names — whether it
 * carries the identity of its owner, whether it holds a value, a part or a whole other aggregate.
 * The addition is the same reading the records already do, moved to the one place every consumer
 * finds it, so a generator asking what a field is does not have to re-derive an answer the model
 * already reached.</p>
 *
 * <p>Two roles are deliberately never set. Audit metadata and technical plumbing can only be told
 * apart by an annotation a pack would have to name on a member, and no pack names any: the carrière
 * told them apart by field name, which is exactly the reading this engine refuses. An empty answer
 * says the analysis has no way to know, which is true; a guess from a name would not be.</p>
 */
final class Fields {

    /** The kinds that own an identity of their own; nothing else carries one in a field. */
    private static final Set<ArchKind> OWNS_AN_IDENTITY =
            Set.of(ArchKind.AGGREGATE_ROOT, ArchKind.ENTITY, ArchKind.DOMAIN_EVENT);

    /** The kinds a field is embedded into its owner by holding. */
    private static final Set<ArchKind> EMBEDS = Set.of(ArchKind.VALUE_OBJECT, ArchKind.IDENTIFIER);

    private final Links links;

    Fields(Links links) {
        this.links = Objects.requireNonNull(links, "links must not be null");
    }

    /**
     * Returns the fields of a declaration, each carrying what the analysis reached about it.
     *
     * @param type the analyzed declaration
     * @param kind the verdict reached on it
     * @return its fields, in declaration order
     */
    List<Field> of(TypeNode type, ArchKind kind) {
        Optional<TypeId> tied = kind == ArchKind.AGGREGATE_ROOT
                ? Links.single(links.objects(RelationKind.IDENTIFIED_BY, type.id()))
                : Optional.empty();
        return type.fields().stream().map(field -> read(field, kind, tied)).toList();
    }

    private Field read(Field field, ArchKind owner, Optional<TypeId> tied) {
        TypeId named = TypeId.of(field.type().qualifiedName());
        Field.Builder read = Field.builder(field.name(), field.type())
                .modifiers(field.modifiers())
                .annotations(field.annotations())
                .roles(rolesOf(field, owner, tied, named));
        field.documentation().ifPresent(read::documentation);
        field.sourceLocation().ifPresent(read::sourceLocation);
        elementOf(field.type()).ifPresent(read::elementType);
        wrappedBy(named).ifPresent(read::wrappedType);
        return read.build();
    }

    private Set<FieldRole> rolesOf(Field field, ArchKind owner, Optional<TypeId> tied, TypeId named) {
        Set<FieldRole> roles = EnumSet.noneOf(FieldRole.class);
        if (carriesIdentity(field, owner, tied)) {
            roles.add(FieldRole.IDENTITY);
        }
        if (field.type().isCollectionLike()) {
            roles.add(FieldRole.COLLECTION);
        }
        if (EMBEDS.stream().anyMatch(kind -> links.is(named, kind))) {
            roles.add(FieldRole.EMBEDDED);
        }
        if (links.is(named, ArchKind.AGGREGATE_ROOT)) {
            roles.add(FieldRole.AGGREGATE_REFERENCE);
        }
        return roles;
    }

    /**
     * Which field carries an identity is asked of the two sources that can answer, and of no other.
     * On an aggregate, a way out states which value it is searched by, and the field holding that
     * value is the one — nothing else is, even when it holds an identity of its own kind. On a part,
     * no such tie exists and the verdict on the field's own type is what is left to read.
     *
     * <p>A static field belongs to the class rather than to any of its instances, so it never
     * carries the identity of one.</p>
     */
    private boolean carriesIdentity(Field field, ArchKind owner, Optional<TypeId> tied) {
        if (!OWNS_AN_IDENTITY.contains(owner) || field.modifiers().contains(Modifier.STATIC)) {
            return false;
        }
        if (owner == ArchKind.AGGREGATE_ROOT) {
            return tied.filter(carrier -> carrier.qualifiedName()
                            .equals(field.type().unwrapElement().qualifiedName()))
                    .isPresent();
        }
        return links.is(TypeId.of(field.type().qualifiedName()), ArchKind.IDENTIFIER);
    }

    /**
     * The one thing a container holds, when the field names a container at all. Unwrapping is the
     * model's single reading of what is inside an array, a collection, an optional or a stream, so
     * the shortcut a consumer reads here and the reference the rules walk cannot drift apart.
     */
    private static Optional<TypeRef> elementOf(TypeRef declared) {
        TypeRef element = declared.unwrapElement();
        return element.equals(declared) ? Optional.empty() : Optional.of(element);
    }

    /**
     * The single value the field's own type is written around, when that type was read as something
     * written around one. Storage names that value rather than the wrapper, which is why the model
     * carries it next to the field instead of leaving every consumer to go and look.
     */
    private Optional<TypeRef> wrappedBy(TypeId named) {
        return EMBEDS.stream().anyMatch(kind -> links.is(named, kind))
                ? Structures.wrappedValueOf(links.code().type(named))
                : Optional.empty();
    }
}
