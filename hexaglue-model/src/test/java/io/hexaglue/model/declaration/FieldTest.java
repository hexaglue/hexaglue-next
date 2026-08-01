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

class FieldTest {

    @Test
    @DisplayName("a full field keeps every component")
    void fullFieldKeepsEveryComponent() {
        Field field = Field.builder("id", TypeRef.of("com.a.OrderId"))
                .modifiers(Set.of(Modifier.PRIVATE, Modifier.FINAL))
                .annotations(List.of(Annotation.of("com.a.Marker")))
                .documentation("The identity.")
                .wrappedType(TypeRef.of("java.util.UUID"))
                .elementType(TypeRef.of("com.a.Item"))
                .roles(Set.of(FieldRole.IDENTITY))
                .sourceLocation(new SourceLocation("Order.java", 4, 4))
                .build();

        assertThat(field.name()).isEqualTo("id");
        assertThat(field.modifiers()).containsExactly(Modifier.PRIVATE, Modifier.FINAL);
        assertThat(field.hasAnnotation("com.a.Marker")).isTrue();
        assertThat(field.documentation()).contains("The identity.");
        assertThat(field.wrappedType()).map(TypeRef::qualifiedName).contains("java.util.UUID");
        assertThat(field.elementType()).map(TypeRef::qualifiedName).contains("com.a.Item");
        assertThat(field.isIdentity()).isTrue();
        assertThat(field.sourceLocation()).map(SourceLocation::lineStart).contains(4);
    }

    @Test
    @DisplayName("a minimal field carries no derived information")
    void minimalFieldCarriesNoDerivedInformation() {
        Field field = Field.of("name", TypeRef.of("java.lang.String"));

        assertThat(field.roles()).isEmpty();
        assertThat(field.wrappedType()).isEmpty();
        assertThat(field.elementType()).isEmpty();
        assertThat(field.isIdentity()).isFalse();
        assertThat(field.isCollection()).isFalse();
        assertThat(field.hasAnnotation("com.a.Marker")).isFalse();
    }

    @Test
    @DisplayName("the collection role answers isCollection")
    void collectionRoleAnswersIsCollection() {
        Field items = Field.builder("items", TypeRef.parameterized("java.util.List", TypeRef.of("com.a.Item")))
                .roles(Set.of(FieldRole.COLLECTION))
                .build();

        assertThat(items.isCollection()).isTrue();
        assertThat(items.hasRole(FieldRole.COLLECTION)).isTrue();
        assertThat(items.hasRole(FieldRole.IDENTITY)).isFalse();
    }

    @Test
    @DisplayName("a blank name is rejected")
    void blankNameIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> Field.of(" ", TypeRef.of("com.a.T")));
    }

    @Test
    @DisplayName("business relevance separates domain roles from plumbing")
    void businessRelevanceSeparatesDomainRolesFromPlumbing() {
        assertThat(FieldRole.IDENTITY.isBusinessRelevant()).isTrue();
        assertThat(FieldRole.EMBEDDED.isBusinessRelevant()).isTrue();
        assertThat(FieldRole.AUDIT.isBusinessRelevant()).isFalse();
        assertThat(FieldRole.TECHNICAL.isBusinessRelevant()).isFalse();
    }
}
