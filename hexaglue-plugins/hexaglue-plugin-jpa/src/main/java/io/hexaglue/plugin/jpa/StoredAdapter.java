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
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import io.hexaglue.spi.SourceFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What fills the hole a repository port leaves: the port itself, answered from the store.
 *
 * <p>The adapter is the one generated file that has to answer <em>every</em> question the port
 * asks, because a class implementing an interface implements all of it. So it is written whole or
 * not at all: a method the store has no answer for would have to throw, and a generated method that
 * compiles and refuses at runtime is worse than a file that was never written and said why.</p>
 *
 * <p>Nothing here converts anything itself. The rows come from the interface written for the
 * aggregate, the values cross through the mappers written for them, and this only wires the two
 * together in the order the port asked for.</p>
 */
final class StoredAdapter {

    private static final String STORE = "repository";

    private final DrivenPort port;
    private final AggregateRoot aggregate;
    private final Crossing crossing;
    private final JpaOptions options;

    StoredAdapter(DrivenPort port, AggregateRoot aggregate, Crossing crossing, JpaOptions options) {
        this.port = Objects.requireNonNull(port, "port must not be null");
        this.aggregate = Objects.requireNonNull(aggregate, "aggregate must not be null");
        this.crossing = Objects.requireNonNull(crossing, "crossing must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Returns the questions this port asks that the store has no answer for.
     *
     * @return the methods standing in the way, empty when the whole port can be answered
     */
    List<Method> unanswerable() {
        return port.structure().methods().stream()
                .filter(method -> answerTo(method).isEmpty())
                .toList();
    }

    /**
     * Writes the adapter out.
     *
     * @return the source file
     */
    SourceFile render() {
        String name = options.adapterFor(port.id().simpleName());
        ClassName store = ClassName.get(
                port.id().packageName(), options.repositoryFor(aggregate.id().simpleName()));
        TypeSpec.Builder spec = TypeSpec.classBuilder(name)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.FINAL)
                .addAnnotation(Written.by(JpaPlugin.ID))
                .addAnnotation(Spring.COMPONENT)
                .addSuperinterface(Stored.named(TypeRef.of(port.id().qualifiedName())))
                .addJavadoc(
                        "How $L is answered from the store.\n\n<p>Written from the classified model."
                                + " Anything changed here is lost the next time the sources are read.</p>\n",
                        port.id().simpleName())
                .addField(FieldSpec.builder(
                                store,
                                STORE,
                                javax.lang.model.element.Modifier.PRIVATE,
                                javax.lang.model.element.Modifier.FINAL)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .addParameter(store, STORE)
                        .addStatement("this.$L = $L", STORE, STORE)
                        .addJavadoc("@param $L what holds the rows\n", STORE)
                        .build());
        port.structure().methods().forEach(method -> spec.addMethod(answering(method)));
        return SourceFile.of(
                port.id().packageName(),
                name,
                JavaFile.builder(port.id().packageName(), spec.build())
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build()
                        .toString());
    }

    /** One method per question, answering it exactly as the port declared it. */
    private MethodSpec answering(Method method) {
        MethodSpec.Builder answering = MethodSpec.methodBuilder(method.name())
                .addAnnotation(Override.class)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .returns(Stored.named(method.returnType()));
        method.parameters()
                .forEach(parameter -> answering.addParameter(Stored.named(parameter.type()), parameter.name()));
        return answering.addStatement(body(answerTo(method).orElseThrow())).build();
    }

    /**
     * What the store is asked, and what is made of what it answers. A count and a truth come back
     * as they are; anything holding rows comes back through the mapper written for the aggregate.
     */
    private CodeBlock body(Answered answered) {
        CodeBlock call = CodeBlock.of("$L.$L($L)", STORE, answered.question().name(), answered.arguments());
        ClassName carrier = crossing.mapperFor(kept());
        return switch (answered.question().answer()) {
            case NOTHING -> call;
            case DIRECT -> CodeBlock.of("return $T.toDomain($L)", carrier, call);
            case MAYBE -> CodeBlock.of("return $L.map($T::toDomain)", call, carrier);
            case MANY -> CodeBlock.of("return $L.stream().map($T::toDomain).toList()", call, carrier);
            case TRUTH, COUNT -> CodeBlock.of("return $L", call);
        };
    }

    /**
     * What the store is asked for one question, values included — or nothing at all when one of
     * those values has no way across.
     */
    private Optional<Answered> answerTo(Method method) {
        return StoreQuestion.of(method, aggregate)
                .flatMap(question -> handedOver(method, question).map(arguments -> new Answered(question, arguments)));
    }

    /**
     * What the port hands over, as the store takes it: the aggregate as a row, an identity as the
     * value it is written around, a field value as that field is stored.
     */
    private Optional<CodeBlock> handedOver(Method method, StoreQuestion question) {
        List<Parameter> parameters = method.parameters();
        return switch (question.taking()) {
            case NOTHING -> Optional.of(CodeBlock.of(""));
            case THE_WHOLE ->
                Optional.of(CodeBlock.of(
                        "$T.toEntity($L)",
                        crossing.mapperFor(kept()),
                        parameters.get(0).name()));
            case THE_IDENTITY ->
                crossing.outward(
                        parameters.get(0).type(),
                        CodeBlock.of("$L", parameters.get(0).name()));
            case FIELDS -> byFields(parameters, question.by());
        };
    }

    private Optional<CodeBlock> byFields(List<Parameter> parameters, List<Field> fields) {
        List<CodeBlock> handed = new ArrayList<>();
        for (int index = 0; index < parameters.size(); index++) {
            Parameter parameter = parameters.get(index);
            Optional<CodeBlock> across =
                    crossing.outward(fields.get(index).type(), CodeBlock.of("$L", parameter.name()));
            if (across.isEmpty()) {
                return Optional.empty();
            }
            handed.add(across.orElseThrow());
        }
        return Optional.of(CodeBlock.join(handed, ", "));
    }

    private TypeRef kept() {
        return TypeRef.of(aggregate.id().qualifiedName());
    }

    /** One question of the port, and the values the store is handed for it. */
    private record Answered(StoreQuestion question, CodeBlock arguments) {}
}
