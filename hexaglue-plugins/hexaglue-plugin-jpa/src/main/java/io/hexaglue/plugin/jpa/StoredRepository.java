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
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.spi.SourceFile;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
                        Spring.JPA_REPOSITORY,
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
            StoreQuestion.of(method, aggregate)
                    .filter(StoreQuestion::declared)
                    .filter(question -> written.add(question.name()))
                    .map(question -> declare(question, method))
                    .ifPresent(queries::add);
        }
        return queries;
    }

    /**
     * Spring Data builds its query from the name of the method, so a name is written here — from
     * the fields of the entity this backend has just generated, never from a name of the user's
     * code. Writing a name a framework imposes is not reading one to conclude an architecture.
     */
    private MethodSpec declare(StoreQuestion question, Method asked) {
        MethodSpec.Builder query = MethodSpec.methodBuilder(question.name())
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.ABSTRACT)
                .returns(answerFor(question.answer()))
                .addJavadoc("@return what $L asks for\n", asked.name());
        question.by()
                .forEach(field -> query.addParameter(ParameterSpec.builder(stored.typeOf(field), field.name())
                        .build()));
        return query.build();
    }

    /**
     * The store answers with what it holds, not with what the domain declared: a port asking for an
     * aggregate is served rows of the entity generated for it, and turning those back into the
     * domain is the adapter's business.
     */
    private TypeName answerFor(StoreQuestion.Answer answer) {
        TypeName entity = stored.entity(reference());
        return switch (answer) {
            case TRUTH -> TypeName.BOOLEAN;
            case COUNT -> TypeName.LONG;
            case MANY -> ParameterizedTypeName.get(LIST, entity);
            default -> ParameterizedTypeName.get(OPTIONAL, entity);
        };
    }

    /** What the rows are keyed by: the value the identity is written around. */
    private TypeName identityType() {
        return aggregate.identityField().map(stored::typeOf).orElseGet(() -> ClassName.get("java.lang", "Object"));
    }

    private TypeRef reference() {
        return TypeRef.of(aggregate.id().qualifiedName());
    }
}
