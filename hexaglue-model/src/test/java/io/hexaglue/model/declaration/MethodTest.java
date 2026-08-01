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

package io.hexaglue.model.declaration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.Modifier;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MethodTest {

    @Test
    @DisplayName("a full method keeps every component")
    void fullMethodKeepsEveryComponent() {
        Method method = Method.builder(
                        "findById", TypeRef.parameterized("java.util.Optional", TypeRef.of("com.a.Order")))
                .parameters(List.of(Parameter.of("id", TypeRef.of("com.a.OrderId"))))
                .modifiers(Set.of(Modifier.PUBLIC, Modifier.ABSTRACT))
                .annotations(List.of(Annotation.of("com.a.Marker")))
                .documentation("Finds an order.")
                .thrownExceptions(List.of(TypeRef.of("com.a.NotFoundException")))
                .roles(Set.of(MethodRole.QUERY))
                .cyclomaticComplexity(1)
                .sourceLocation(new SourceLocation("OrderRepository.java", 12, 12))
                .build();

        assertThat(method.name()).isEqualTo("findById");
        assertThat(method.parameters()).hasSize(1);
        assertThat(method.modifiers()).containsExactly(Modifier.PUBLIC, Modifier.ABSTRACT);
        assertThat(method.hasAnnotation("com.a.Marker")).isTrue();
        assertThat(method.documentation()).contains("Finds an order.");
        assertThat(method.thrownExceptions()).extracting(TypeRef::simpleName).containsExactly("NotFoundException");
        assertThat(method.hasRole(MethodRole.QUERY)).isTrue();
        assertThat(method.cyclomaticComplexity()).hasValue(1);
        assertThat(method.sourceLocation()).isPresent();
    }

    @Test
    @DisplayName("the signature renders name and parameter simple names")
    void signatureRendersNameAndParameters() {
        Method method = Method.builder("place", TypeRef.of("void"))
                .parameters(List.of(
                        Parameter.of("id", TypeRef.of("com.a.OrderId")),
                        Parameter.of("note", TypeRef.of("java.lang.String"))))
                .build();

        assertThat(method.signature()).isEqualTo("place(OrderId, String)");
    }

    @Test
    @DisplayName("a minimal method has no roles and no complexity")
    void minimalMethodHasNoRolesAndNoComplexity() {
        Method method = Method.of("touch", TypeRef.of("void"));

        assertThat(method.roles()).isEmpty();
        assertThat(method.cyclomaticComplexity()).isEmpty();
        assertThat(method.hasRole(MethodRole.QUERY)).isFalse();
        assertThat(method.hasAnnotation("com.a.Marker")).isFalse();
    }

    @Test
    @DisplayName("a blank name is rejected")
    void blankNameIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> Method.of(" ", TypeRef.of("void")));
    }

    @Test
    @DisplayName("parameters know their annotations")
    void parametersKnowTheirAnnotations() {
        Parameter parameter = new Parameter(
                "id", TypeRef.of("com.a.OrderId"), List.of(Annotation.of("jakarta.validation.constraints.NotNull")));

        assertThat(parameter.hasAnnotation("jakarta.validation.constraints.NotNull"))
                .isTrue();
        assertThat(parameter.hasAnnotation("com.a.Other")).isFalse();
    }

    @Test
    @DisplayName("method roles classify mutation, access, infrastructure and domain operations")
    void methodRolesClassifyBehaviour() {
        assertThat(MethodRole.COMMAND.isMutation()).isTrue();
        assertThat(MethodRole.GETTER.isAccessor()).isTrue();
        assertThat(MethodRole.FACTORY.isInfrastructure()).isTrue();
        assertThat(MethodRole.VALIDATION.isDomainOperation()).isTrue();
        assertThat(MethodRole.GETTER.isMutation()).isFalse();
        assertThat(MethodRole.COMMAND.isAccessor()).isFalse();
        assertThat(MethodRole.BUSINESS.isInfrastructure()).isFalse();
        assertThat(MethodRole.SETTER.isDomainOperation()).isFalse();
    }
}
