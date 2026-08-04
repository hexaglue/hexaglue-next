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

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.spi.SourceFile;
import java.util.List;
import java.util.Objects;

/**
 * A domain type as the store holds it.
 *
 * <p>An entity and an embeddable are the same writing exercise with one difference — what has a
 * life of its own carries an identity and a table, what does not is written into the row of
 * whatever holds it — so they are one class here rather than two that would drift.</p>
 *
 * <p>The generated type is a JPA one, which is to say it has a no-argument constructor and gives
 * access to its state. That is not a design opinion about the domain: it is what the persistence
 * provider requires of the thing it instantiates, and the reason the domain is not asked to look
 * like this itself.</p>
 */
final class StoredType {

    private final DomainType type;
    private final Stored stored;
    private final JpaOptions options;

    StoredType(DomainType type, Stored stored, JpaOptions options) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.stored = Objects.requireNonNull(stored, "stored must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Writes the type out, ready to be handed to the run.
     *
     * @param identity the field carrying the identity, absent for an embeddable
     * @return the source file
     */
    SourceFile render(java.util.Optional<Field> identity) {
        String simpleName = type.id().simpleName();
        String generatedName = identity.isPresent() ? options.entityFor(simpleName) : options.embeddableFor(simpleName);
        TypeSpec.Builder spec = TypeSpec.classBuilder(generatedName)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addAnnotation(Written.by(JpaPlugin.ID))
                .addJavadoc(
                        "How $L is stored.\n\n<p>Written from the classified model. Anything changed here is"
                                + " lost the next time the sources are read.</p>\n",
                        simpleName);
        if (identity.isPresent()) {
            spec.addAnnotation(Jpa.ENTITY).addAnnotation(Jpa.table(SqlNames.table(simpleName, options.tablePrefix())));
        } else {
            spec.addAnnotation(Jpa.EMBEDDABLE);
        }
        spec.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(javax.lang.model.element.Modifier.PROTECTED)
                .addJavadoc("For the persistence provider.\n")
                .build());
        for (Field field : state()) {
            spec.addField(fieldOf(field, identity.filter(field::equals).isPresent()));
            spec.addMethod(getterOf(field));
        }
        spec.addMethod(fullConstructor());
        return SourceFile.of(
                type.id().packageName(),
                generatedName,
                JavaFile.builder(type.id().packageName(), spec.build())
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build()
                        .toString());
    }

    /** What belongs to an instance. A constant belongs to the class and is stored nowhere. */
    private List<Field> state() {
        return type.structure().fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .toList();
    }

    private FieldSpec fieldOf(Field field, boolean isIdentity) {
        FieldSpec.Builder spec =
                FieldSpec.builder(stored.typeOf(field), field.name(), javax.lang.model.element.Modifier.PRIVATE);
        if (isIdentity) {
            spec.addAnnotation(Jpa.ID);
            if (options.identity() != IdentityStrategy.ASSIGNED) {
                spec.addAnnotation(Jpa.generatedValue(options.identity()));
            }
            spec.addAnnotation(Jpa.column(SqlNames.column(field.name())));
            return spec.build();
        }
        if (field.isCollection()) {
            spec.addAnnotation(stored.isRelation(field) ? Jpa.ONE_TO_MANY : Jpa.ELEMENT_COLLECTION);
            return spec.build();
        }
        if (stored.isEmbedded(field)) {
            spec.addAnnotation(Jpa.EMBEDDED);
            return spec.build();
        }
        if (stored.isOneOfAClosedSet(field)) {
            spec.addAnnotation(Jpa.enumerated());
            spec.addAnnotation(Jpa.column(SqlNames.column(field.name())));
            return spec.build();
        }
        if (stored.isRelation(field)) {
            spec.addAnnotation(Jpa.MANY_TO_ONE);
            return spec.build();
        }
        spec.addAnnotation(Jpa.column(SqlNames.column(field.name())));
        return spec.build();
    }

    /**
     * The one way anything but the provider builds one of these. There are no writers, so a row is
     * either read back by the provider or built whole — which is what lets the generated mapper
     * turn a domain object into a row without ever leaving it half filled.
     */
    private MethodSpec fullConstructor() {
        MethodSpec.Builder full = MethodSpec.constructorBuilder()
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addJavadoc("Builds a row from what the domain holds.\n");
        for (Field field : state()) {
            full.addParameter(stored.typeOf(field), field.name())
                    .addStatement("this.$L = $L", field.name(), field.name())
                    .addJavadoc("@param $L the stored $L\n", field.name(), field.name());
        }
        return full.build();
    }

    /**
     * A reader per field and no writers: the store fills these through the provider, and code that
     * could set them would be code changing a domain object through its shadow.
     */
    private MethodSpec getterOf(Field field) {
        TypeName type = stored.typeOf(field);
        String name = field.name();
        return MethodSpec.methodBuilder("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1))
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .returns(type)
                .addStatement("return $L", name)
                .addJavadoc("@return the stored $L\n", name)
                .build();
    }
}
