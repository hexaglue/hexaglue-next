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
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import io.hexaglue.spi.SourceFile;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The Spring Data interface a repository port is served by.
 *
 * <p>Extending {@code JpaRepository} is most of it: finding by identity, saving, deleting and
 * counting all come with it. What has to be written are the questions this port asks that the
 * inherited ones do not answer.</p>
 *
 * <p>Those are derived from <strong>shape, never from spelling</strong>. Spring Data reads the name
 * of a method to build its query, so a name is written here — but which method deserves one, and
 * what it asks about, is settled by matching the parameters of the port against the fields of the
 * aggregate and by reading the return type. The carrière did the opposite: it parsed the port's own
 * method names, so {@code findAllActive} became {@code findByActiveTrue} and a domain that named
 * things differently got nothing.</p>
 */
final class StoredRepository {

    private static final ClassName JPA_REPOSITORY =
            ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");
    private static final ClassName OPTIONAL = ClassName.get("java.util", "Optional");
    private static final ClassName LIST = ClassName.get("java.util", "List");

    private final DrivenPort port;
    private final AggregateRoot aggregate;
    private final Stored stored;
    private final JpaOptions options;

    StoredRepository(DrivenPort port, AggregateRoot aggregate, Stored stored, JpaOptions options) {
        this.port = Objects.requireNonNull(port, "port must not be null");
        this.aggregate = Objects.requireNonNull(aggregate, "aggregate must not be null");
        this.stored = Objects.requireNonNull(stored, "stored must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Writes the repository out.
     *
     * @return the source file
     */
    SourceFile render() {
        String name = options.repositoryFor(aggregate.id().simpleName());
        TypeSpec.Builder spec = TypeSpec.interfaceBuilder(name)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .addAnnotation(Written.by(JpaPlugin.ID))
                .addSuperinterface(ParameterizedTypeName.get(
                        JPA_REPOSITORY,
                        stored.entity(reference()),
                        identityType().box()))
                .addJavadoc(
                        "How $L is stored and found, serving $L.\n\n<p>Written from the classified"
                                + " model. Anything changed here is lost the next time the sources are"
                                + " read.</p>\n",
                        aggregate.id().simpleName(),
                        port.id().simpleName());
        queries().forEach(spec::addMethod);
        return SourceFile.of(
                port.id().packageName(),
                name,
                JavaFile.builder(port.id().packageName(), spec.build())
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build()
                        .toString());
    }

    /**
     * One query per question the port asks that the inherited ones do not answer, in the order the
     * port asks them, without two methods of the same name.
     */
    private List<MethodSpec> queries() {
        Set<String> written = new LinkedHashSet<>();
        List<MethodSpec> queries = new ArrayList<>();
        for (Method method : port.structure().methods()) {
            query(method).filter(query -> written.add(query.name())).ifPresent(queries::add);
        }
        return queries;
    }

    /**
     * A question is answerable when every value it takes is a field of the aggregate that is not
     * its identity — asking by identity is what {@code findById} already does. What the answer
     * looks like says which question it is: a truth is an existence, a count is a count, anything
     * else is a search.
     */
    private Optional<MethodSpec> query(Method method) {
        List<Field> matched = new ArrayList<>();
        for (Parameter parameter : method.parameters()) {
            Optional<Field> field = fieldHolding(parameter.type());
            if (field.isEmpty()) {
                return Optional.empty();
            }
            matched.add(field.orElseThrow());
        }
        if (matched.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(spring(method, matched));
    }

    private MethodSpec spring(Method method, List<Field> by) {
        String suffix = by.stream().map(StoredRepository::capitalised).reduce("", (all, one) -> all + "And" + one);
        String verb = verbFor(method.returnType());
        MethodSpec.Builder query = MethodSpec.methodBuilder(verb + "By" + suffix.substring("And".length()))
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.ABSTRACT)
                .returns(answerFor(method.returnType(), verb))
                .addJavadoc("@return what $L asks for\n", method.name());
        by.forEach(field -> query.addParameter(
                ParameterSpec.builder(stored.typeOf(field), field.name()).build()));
        return query.build();
    }

    private static String verbFor(TypeRef answer) {
        return switch (answer.qualifiedName()) {
            case "boolean", "java.lang.Boolean" -> "exists";
            case "long", "int", "java.lang.Long", "java.lang.Integer" -> "count";
            default -> "find";
        };
    }

    /**
     * The store answers with what it holds, not with what the domain declared: a port asking for an
     * aggregate is served rows of the entity generated for it, and turning those back into the
     * domain is the adapter's business.
     */
    private TypeName answerFor(TypeRef answer, String verb) {
        if ("exists".equals(verb)) {
            return TypeName.BOOLEAN;
        }
        if ("count".equals(verb)) {
            return TypeName.LONG;
        }
        TypeName entity = stored.entity(reference());
        if (answer.isCollectionLike() || answer.isStreamLike()) {
            return ParameterizedTypeName.get(LIST, entity);
        }
        return ParameterizedTypeName.get(OPTIONAL, entity);
    }

    /** The field of the aggregate a value of this type would be matched against, if any. */
    private Optional<Field> fieldHolding(TypeRef type) {
        return aggregate.structure().fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .filter(field -> !field.isIdentity())
                .filter(field -> field.type().qualifiedName().equals(type.qualifiedName()))
                .findFirst();
    }

    /** What the rows are keyed by: the value the identity is written around. */
    private TypeName identityType() {
        return aggregate.identityField().map(stored::typeOf).orElseGet(() -> ClassName.get("java.lang", "Object"));
    }

    private TypeRef reference() {
        return TypeRef.of(aggregate.id().qualifiedName());
    }

    private static String capitalised(Field field) {
        String name = field.name();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
