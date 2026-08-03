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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DependenciesTest {

    private final CodeModel.Builder code = CodeModel.builder();

    private DependenciesTest type(String qualifiedName, TypeNature nature, Modifier... modifiers) {
        code.addType(TypeNode.builder(TypeId.of(qualifiedName), nature)
                .modifiers(Set.of(modifiers))
                .build());
        return this;
    }

    private DependenciesTest type(String qualifiedName) {
        return type(qualifiedName, TypeNature.CLASS);
    }

    private DependenciesTest edge(String from, String to, EdgeKind kind) {
        code.addEdge(Edge.of(TypeId.of(from), kind, TypeId.of(to)));
        return this;
    }

    private Dependencies graph() {
        CodeModel model = code.build();
        return Dependencies.of(model, Perimeter.of(model, AnalysisScope.everything()));
    }

    @Nested
    @DisplayName("what counts as a dependency")
    class Edges {

        @Test
        @DisplayName("folds the couplings between types into couplings between packages")
        void foldsTypeCouplingsIntoPackages() {
            Dependencies dependencies = type("com.acme.app.Service")
                    .type("com.acme.domain.Order")
                    .edge("com.acme.app.Service", "com.acme.domain.Order", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.dependenciesOf("com.acme.app")).containsExactly("com.acme.domain");
            assertThat(dependencies.dependentsOf("com.acme.domain")).containsExactly("com.acme.app");
        }

        @Test
        @DisplayName("ignores a type coupled to another of its own package")
        void ignoresCouplingsInsideAPackage() {
            Dependencies dependencies = type("com.acme.domain.Order")
                    .type("com.acme.domain.OrderLine")
                    .edge("com.acme.domain.Order", "com.acme.domain.OrderLine", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.dependenciesOf("com.acme.domain")).isEmpty();
        }

        @Test
        @DisplayName("ignores a coupling to a type nobody analysed")
        void ignoresCouplingsOutsideThePerimeter() {
            Dependencies dependencies = type("com.acme.domain.Order")
                    .edge("com.acme.domain.Order", "java.util.List", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.dependenciesOf("com.acme.domain")).isEmpty();
        }

        @Test
        @DisplayName("counts a sealed hierarchy once, not once in each direction")
        void countsASealedHierarchyOnce() {
            Dependencies dependencies = type("com.acme.domain.Payment", TypeNature.INTERFACE)
                    .type("com.acme.card.CardPayment")
                    .edge("com.acme.domain.Payment", "com.acme.card.CardPayment", EdgeKind.PERMITS)
                    .edge("com.acme.card.CardPayment", "com.acme.domain.Payment", EdgeKind.IMPLEMENTS)
                    .graph();

            assertThat(dependencies.dependenciesOf("com.acme.domain")).isEmpty();
            assertThat(dependencies.dependenciesOf("com.acme.card")).containsExactly("com.acme.domain");
            assertThat(dependencies.cycles()).isEmpty();
        }

        @Test
        @DisplayName("does not read a nested type as a coupling")
        void ignoresNesting() {
            Dependencies dependencies = type("com.acme.domain.Order")
                    .type("com.acme.domain.Order.Line")
                    .edge("com.acme.domain.Order", "com.acme.domain.Order.Line", EdgeKind.DECLARES)
                    .graph();

            assertThat(dependencies.dependenciesOf("com.acme.domain")).isEmpty();
        }
    }

    @Nested
    @DisplayName("cycles")
    class Knots {

        @Test
        @DisplayName("reports a knot of packages once, however many ways round it there are")
        void reportsAKnotOnce() {
            Dependencies dependencies = type("com.acme.a.A")
                    .type("com.acme.b.B")
                    .type("com.acme.c.C")
                    .edge("com.acme.a.A", "com.acme.b.B", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.b.B", "com.acme.c.C", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.c.C", "com.acme.a.A", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.b.B", "com.acme.a.A", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.cycles())
                    .singleElement()
                    .satisfies(cycle -> assertThat(cycle).containsExactly("com.acme.a", "com.acme.b", "com.acme.c"));
        }

        @Test
        @DisplayName("says nothing about packages that depend one way only")
        void saysNothingAboutAnAcyclicGraph() {
            Dependencies dependencies = type("com.acme.a.A")
                    .type("com.acme.b.B")
                    .edge("com.acme.a.A", "com.acme.b.B", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.cycles()).isEmpty();
        }

        @Test
        @DisplayName("reports two separate knots separately")
        void reportsTwoKnots() {
            Dependencies dependencies = type("com.acme.a.A")
                    .type("com.acme.b.B")
                    .type("com.acme.x.X")
                    .type("com.acme.y.Y")
                    .edge("com.acme.a.A", "com.acme.b.B", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.b.B", "com.acme.a.A", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.x.X", "com.acme.y.Y", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.y.Y", "com.acme.x.X", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.cycles()).hasSize(2);
            assertThat(dependencies.cycles().get(0)).containsExactly("com.acme.a", "com.acme.b");
            assertThat(dependencies.cycles().get(1)).containsExactly("com.acme.x", "com.acme.y");
        }
    }

    @Nested
    @DisplayName("how settled a package is")
    class Measures {

        @Test
        @DisplayName("measures a package everything depends on and that depends on nothing")
        void measuresAStablePackage() {
            Dependencies dependencies = type("com.acme.domain.Order", TypeNature.INTERFACE)
                    .type("com.acme.app.Service")
                    .edge("com.acme.app.Service", "com.acme.domain.Order", EdgeKind.FIELD_TYPE)
                    .graph();

            Stability domain = dependencies.stabilityOf("com.acme.domain");
            assertThat(domain.efferent()).isZero();
            assertThat(domain.afferent()).isEqualTo(1);
            assertThat(domain.instability()).isEqualTo(0.0);
            assertThat(domain.abstractness()).isEqualTo(1.0);
            assertThat(domain.distance()).isCloseTo(0.0, within(0.0001));
        }

        @Test
        @DisplayName("measures a package that depends on everything and that nothing depends on")
        void measuresAnUnstablePackage() {
            Dependencies dependencies = type("com.acme.domain.Order", TypeNature.INTERFACE)
                    .type("com.acme.app.Service")
                    .edge("com.acme.app.Service", "com.acme.domain.Order", EdgeKind.FIELD_TYPE)
                    .graph();

            Stability app = dependencies.stabilityOf("com.acme.app");
            assertThat(app.instability()).isEqualTo(1.0);
            assertThat(app.abstractness()).isEqualTo(0.0);
            assertThat(app.distance()).isCloseTo(0.0, within(0.0001));
        }

        @Test
        @DisplayName("counts an abstract class as abstract, like an interface")
        void countsAbstractClasses() {
            Dependencies dependencies = type("com.acme.domain.Base", TypeNature.CLASS, Modifier.ABSTRACT)
                    .type("com.acme.domain.Order")
                    .graph();

            assertThat(dependencies.stabilityOf("com.acme.domain").abstractness())
                    .isCloseTo(0.5, within(0.0001));
        }

        @Test
        @DisplayName("places a concrete package nothing can change away from without breaking at the far end")
        void measuresDistance() {
            Dependencies dependencies = type("com.acme.domain.Order")
                    .type("com.acme.app.Service")
                    .edge("com.acme.app.Service", "com.acme.domain.Order", EdgeKind.FIELD_TYPE)
                    .graph();

            Stability domain = dependencies.stabilityOf("com.acme.domain");
            assertThat(domain.abstractness()).isEqualTo(0.0);
            assertThat(domain.instability()).isEqualTo(0.0);
            assertThat(domain.distance()).isCloseTo(1.0, within(0.0001));
        }
    }

    @Nested
    @DisplayName("dependencies that point the wrong way")
    class Direction {

        @Test
        @DisplayName("reports the settled package depending on the freer one, and nothing else")
        void reportsTheCanonicalViolation() {
            // core is depended on three times and depends once: I = 1/4. leaf is depended on once
            // and depends once: I = 1/2. So core -> leaf runs from the harder thing to change
            // towards the easier one, and it is the only dependency of the graph that does.
            Dependencies dependencies = type("com.acme.core.Core")
                    .type("com.acme.leaf.Leaf")
                    .type("com.acme.util.Util")
                    .type("com.acme.a.A")
                    .type("com.acme.b.B")
                    .type("com.acme.c.C")
                    .edge("com.acme.a.A", "com.acme.core.Core", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.b.B", "com.acme.core.Core", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.c.C", "com.acme.core.Core", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.core.Core", "com.acme.leaf.Leaf", EdgeKind.FIELD_TYPE)
                    .edge("com.acme.leaf.Leaf", "com.acme.util.Util", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.stabilityOf("com.acme.core").instability()).isCloseTo(0.25, within(0.0001));
            assertThat(dependencies.stabilityOf("com.acme.leaf").instability()).isCloseTo(0.5, within(0.0001));
            assertThat(dependencies.unstableDependencies()).singleElement().satisfies(violation -> {
                assertThat(violation.from().packageName()).isEqualTo("com.acme.core");
                assertThat(violation.on().packageName()).isEqualTo("com.acme.leaf");
            });
        }

        @Test
        @DisplayName("says nothing when every dependency runs towards what is harder to change")
        void saysNothingWhenTheDirectionIsRight() {
            Dependencies dependencies = type("com.acme.domain.Order", TypeNature.INTERFACE)
                    .type("com.acme.app.Service")
                    .edge("com.acme.app.Service", "com.acme.domain.Order", EdgeKind.FIELD_TYPE)
                    .graph();

            assertThat(dependencies.unstableDependencies()).isEmpty();
        }
    }
}
