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

package io.hexaglue.model.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.Modifier;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TypeNodeTest {

    @Test
    @DisplayName("a full source node keeps every component")
    void fullSourceNodeKeepsEveryComponent() {
        TypeNode node = TypeNode.builder(TypeId.of("com.a.Order"), TypeNature.CLASS)
                .modifiers(Set.of(Modifier.PUBLIC, Modifier.FINAL))
                .superClass(TypeRef.of("com.a.BaseEntity"))
                .interfaces(List.of(TypeRef.of("java.io.Serializable")))
                .permittedSubtypes(List.of(TypeRef.of("com.a.SpecialOrder")))
                .annotations(List.of(Annotation.of("jakarta.persistence.Entity")))
                .fields(List.of(Field.of("id", TypeRef.of("com.a.OrderId"))))
                .methods(List.of(Method.of("total", TypeRef.of("java.math.BigDecimal"))))
                .constructors(List.of(Constructor.noArg()))
                .documentation("An order.")
                .sourceLocation(new SourceLocation("Order.java", 1, 50))
                .moduleName("shop-domain")
                .build();

        assertThat(node.external()).isFalse();
        assertThat(node.modifiers()).containsExactly(Modifier.PUBLIC, Modifier.FINAL);
        assertThat(node.superClass()).map(TypeRef::simpleName).contains("BaseEntity");
        assertThat(node.interfaces()).hasSize(1);
        assertThat(node.permittedSubtypes()).hasSize(1);
        assertThat(node.hasAnnotation("jakarta.persistence.Entity")).isTrue();
        assertThat(node.fields()).hasSize(1);
        assertThat(node.methods()).hasSize(1);
        assertThat(node.constructors()).hasSize(1);
        assertThat(node.documentation()).contains("An order.");
        assertThat(node.sourceLocation()).isPresent();
        assertThat(node.moduleName()).contains("shop-domain");
    }

    @Test
    @DisplayName("a nested type knows its enclosing type")
    void nestedTypeKnowsEnclosingType() {
        TypeNode nested = TypeNode.builder(TypeId.of("com.a.Order$Line"), TypeNature.CLASS)
                .enclosingType(TypeId.of("com.a.Order"))
                .build();

        assertThat(nested.isNested()).isTrue();
        assertThat(nested.enclosingType()).contains(TypeId.of("com.a.Order"));
    }

    @Test
    @DisplayName("an external stub carries no members")
    void externalStubCarriesNoMembers() {
        TypeNode stub = TypeNode.externalStub(
                TypeId.of("org.springframework.data.jpa.repository.JpaRepository"), TypeNature.INTERFACE);

        assertThat(stub.external()).isTrue();
        assertThat(stub.fields()).isEmpty();
        assertThat(stub.methods()).isEmpty();
        assertThat(stub.constructors()).isEmpty();
        assertThat(stub.isNested()).isFalse();
    }

    @Test
    @DisplayName("an external node with members is rejected")
    void externalNodeWithMembersIsRejected() {
        Field field = Field.of("x", TypeRef.of("int"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TypeNode(
                        TypeId.of("com.a.External"),
                        TypeNature.CLASS,
                        Set.of(),
                        true,
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(field),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()))
                .withMessageContaining("external stub");
    }
}
