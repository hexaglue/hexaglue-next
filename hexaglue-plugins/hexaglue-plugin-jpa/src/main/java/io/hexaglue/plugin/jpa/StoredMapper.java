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
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.spi.SourceFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The two ways between a domain type and the row that stores it.
 *
 * <p>Written only when every field can be carried across in both directions. A mapper that handled
 * most of a type and quietly dropped the rest would lose data on the way out and rebuild something
 * that is not what was saved — so this either writes the whole conversion or writes nothing and
 * says which field it could not carry.</p>
 */
final class StoredMapper {

    private final DomainType type;
    private final ArchModel model;
    private final Stored stored;
    private final JpaOptions options;

    StoredMapper(DomainType type, ArchModel model, Stored stored, JpaOptions options) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.stored = Objects.requireNonNull(stored, "stored must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Returns the field this type cannot be carried across by, if there is one.
     *
     * @return the first field in the way, empty when the whole type can be mapped
     */
    Optional<Field> unmappable() {
        return DomainAccess.state(type).stream()
                .filter(field -> DomainAccess.accessorOf(type, field).isEmpty() || !carriable(field))
                .findFirst();
    }

    /**
     * A field can be carried when what it holds is something this backend also stores: a plain
     * value, an identity it can unwrap, or a type it has written a mapper for. A collection is not
     * yet one of them — turning a list of rows back into a domain collection needs the domain to
     * say how it takes one, which nothing in the model states.
     */
    private boolean carriable(Field field) {
        if (field.isCollection()) {
            return false;
        }
        Optional<ArchKind> held = kindOf(field.type());
        if (held.isEmpty()) {
            return true;
        }
        return switch (held.orElseThrow()) {
            case IDENTIFIER -> unwrapAccessor(field.type()).isPresent();
            case VALUE_OBJECT -> mappable(field.type());
            default -> false;
        };
    }

    /**
     * Writes the mapper out.
     *
     * @return the source file
     */
    SourceFile render() {
        String name = options.mapperFor(type.id().simpleName());
        TypeName domain = Stored.named(reference());
        TypeName entity = row();
        TypeSpec spec = TypeSpec.classBuilder(name)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.FINAL)
                .addJavadoc(
                        "Between $L and the row that stores it.\n\n<p>Written from the classified"
                                + " model. Anything changed here is lost the next time the sources are"
                                + " read.</p>\n",
                        type.id().simpleName())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(javax.lang.model.element.Modifier.PRIVATE)
                        .build())
                .addMethod(toEntity(domain, entity))
                .addMethod(toDomain(domain, entity))
                .build();
        return SourceFile.of(
                type.id().packageName(),
                name,
                JavaFile.builder(type.id().packageName(), spec)
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build()
                        .toString());
    }

    /**
     * What stores this type: a row of its own for something with a life of its own, part of
     * somebody else's row for a value. Asking {@link Stored} for an entity either way would name a
     * class this backend never wrote.
     */
    private TypeName row() {
        return type.kind() == ArchKind.VALUE_OBJECT ? stored.embeddable(reference()) : stored.entity(reference());
    }

    private MethodSpec toEntity(TypeName domain, TypeName entity) {
        List<CodeBlock> arguments = new ArrayList<>();
        for (Field field : DomainAccess.state(type)) {
            arguments.add(outward(field));
        }
        return MethodSpec.methodBuilder("toEntity")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.STATIC)
                .returns(entity)
                .addParameter(domain, "domain")
                .addStatement("return new $T($L)", entity, CodeBlock.join(arguments, ", "))
                .addJavadoc("@param domain what to store\n@return the row storing it\n")
                .build();
    }

    private MethodSpec toDomain(TypeName domain, TypeName entity) {
        List<CodeBlock> arguments = new ArrayList<>();
        for (Field field : DomainAccess.state(type)) {
            arguments.add(inward(field));
        }
        return MethodSpec.methodBuilder("toDomain")
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.STATIC)
                .returns(domain)
                .addParameter(entity, "row")
                .addStatement("return new $T($L)", domain, CodeBlock.join(arguments, ", "))
                .addJavadoc("@param row what was stored\n@return what the domain makes of it\n")
                .build();
    }

    /** Domain to row: unwrap an identity, hand a value to its own mapper, copy anything else. */
    private CodeBlock outward(Field field) {
        String read = "domain." + DomainAccess.accessorOf(type, field).orElseThrow() + "()";
        Optional<ArchKind> held = kindOf(field.type());
        if (held.filter(ArchKind.IDENTIFIER::equals).isPresent()) {
            return CodeBlock.of("$L.$L()", read, unwrapAccessor(field.type()).orElseThrow());
        }
        if (held.filter(ArchKind.VALUE_OBJECT::equals).isPresent()) {
            return CodeBlock.of("$T.toEntity($L)", mapperFor(field.type()), read);
        }
        return CodeBlock.of("$L", read);
    }

    /** Row to domain: rebuild an identity around its value, a value through its own mapper. */
    private CodeBlock inward(Field field) {
        String read = "row.get" + Character.toUpperCase(field.name().charAt(0))
                + field.name().substring(1) + "()";
        Optional<ArchKind> held = kindOf(field.type());
        if (held.filter(ArchKind.IDENTIFIER::equals).isPresent()) {
            return CodeBlock.of("new $T($L)", Stored.named(field.type()), read);
        }
        if (held.filter(ArchKind.VALUE_OBJECT::equals).isPresent()) {
            return CodeBlock.of("$T.toDomain($L)", mapperFor(field.type()), read);
        }
        return CodeBlock.of("$L", read);
    }

    /** The single component an identity is written around, read from the identity itself. */
    private Optional<String> unwrapAccessor(TypeRef identity) {
        return model.type(TypeId.of(identity.qualifiedName()))
                .flatMap(held -> DomainAccess.state(held).stream()
                        .findFirst()
                        .flatMap(only -> DomainAccess.accessorOf(held, only)));
    }

    /** Whether the value held by a field is itself something a mapper was written for. */
    private boolean mappable(TypeRef value) {
        return model.type(TypeId.of(value.qualifiedName()))
                .filter(DomainType.class::isInstance)
                .map(DomainType.class::cast)
                .filter(DomainAccess::isRebuildable)
                .isPresent();
    }

    private ClassName mapperFor(TypeRef value) {
        return ClassName.get(value.packageName(), options.mapperFor(value.simpleName()));
    }

    private Optional<ArchKind> kindOf(TypeRef held) {
        return model.type(TypeId.of(held.qualifiedName())).map(ArchType::kind);
    }

    private TypeRef reference() {
        return TypeRef.of(type.id().qualifiedName());
    }
}
