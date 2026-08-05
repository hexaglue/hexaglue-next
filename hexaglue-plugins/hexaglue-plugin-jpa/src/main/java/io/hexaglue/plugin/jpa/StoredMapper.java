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

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.spi.DomainAccess;
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
    private final Stored stored;
    private final Crossing crossing;
    private final JpaOptions options;

    StoredMapper(DomainType type, Stored stored, Crossing crossing, JpaOptions options) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.stored = Objects.requireNonNull(stored, "stored must not be null");
        this.crossing = Objects.requireNonNull(crossing, "crossing must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Returns the field this type cannot be carried across by, if there is one.
     *
     * @return the first field in the way, empty when the whole type can be mapped
     */
    Optional<Field> unmappable() {
        return DomainAccess.state(type).stream()
                .filter(field -> DomainAccess.accessorOf(type, field).isEmpty() || !crossing.crosses(field.type()))
                .findFirst();
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
                .addAnnotation(Written.by(JpaPlugin.ID))
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

    /** Domain to row: read the field the way the type offers it, then carry the value across. */
    private CodeBlock outward(Field field) {
        CodeBlock read =
                CodeBlock.of("domain.$L()", DomainAccess.accessorOf(type, field).orElseThrow());
        return crossing.outward(field.type(), read).orElseThrow();
    }

    /** Row to domain: read the column off the row, then carry the value back. */
    private CodeBlock inward(Field field) {
        CodeBlock read = CodeBlock.of(
                "row.get$L$L()",
                field.name().substring(0, 1).toUpperCase(java.util.Locale.ROOT),
                field.name().substring(1));
        return crossing.inward(field.type(), read).orElseThrow();
    }

    private TypeRef reference() {
        return TypeRef.of(type.id().qualifiedName());
    }
}
