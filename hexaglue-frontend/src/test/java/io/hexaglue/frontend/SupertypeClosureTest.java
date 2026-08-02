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

package io.hexaglue.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Knowing that {@code JpaRepository} is a Spring Data {@code Repository} without ever seeing its
 * sources is what lets framework knowledge be stated once, on the root of a hierarchy, instead of
 * being re-listed for every derived interface a vendor ships.
 */
class SupertypeClosureTest {

    private static final String REPOSITORY = "org.springframework.data.repository.Repository";
    private static final String CRUD_REPOSITORY = "org.springframework.data.repository.CrudRepository";
    private static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";

    @TempDir
    Path sources;

    @TempDir
    Path work;

    private Path springDataClasspath() {
        return CompiledClasspath.of(
                work,
                Map.of(
                        "org/springframework/data/repository/Repository.java",
                        "package org.springframework.data.repository; public interface Repository<T, ID> {}",
                        "org/springframework/data/repository/CrudRepository.java",
                        "package org.springframework.data.repository; public interface CrudRepository<T, ID> extends Repository<T, ID> {}",
                        "org/springframework/data/jpa/repository/JpaRepository.java",
                        """
                        package org.springframework.data.jpa.repository;
                        import org.springframework.data.repository.CrudRepository;
                        public interface JpaRepository<T, ID> extends CrudRepository<T, ID> {}
                        """));
    }

    private CodeModel analyzeWithSpringData() {
        return SpoonFrontend.analyze(FrontendRequest.builder()
                .sourceRoot(sources)
                .classpathEntry(springDataClasspath())
                .build());
    }

    @Nested
    @DisplayName("from the classpath")
    class FromTheClasspath {

        @Test
        @DisplayName("walks a framework hierarchy read from bytecode")
        void walksAFrameworkHierarchyFromBytecode() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "com/acme/OrderRepository.java", """
                    package com.acme;
                    import org.springframework.data.jpa.repository.JpaRepository;
                    public interface OrderRepository extends JpaRepository<Order, Long> {}
                    """);

            CodeModel model = analyzeWithSpringData();

            assertThat(model.supertypesOf(TypeId.of("com.acme.OrderRepository")))
                    .containsExactly(TypeId.of(JPA_REPOSITORY), TypeId.of(CRUD_REPOSITORY), TypeId.of(REPOSITORY));
        }

        @Test
        @DisplayName("gives a classpath stub its own closure")
        void givesAStubItsOwnClosure() {
            SourceFixtures.write(sources, "com/acme/OrderRepository.java", """
                    package com.acme;
                    import org.springframework.data.jpa.repository.JpaRepository;
                    public interface OrderRepository extends JpaRepository<String, Long> {}
                    """);

            CodeModel model = analyzeWithSpringData();

            assertThat(model.supertypesOf(TypeId.of(JPA_REPOSITORY)))
                    .containsExactly(TypeId.of(CRUD_REPOSITORY), TypeId.of(REPOSITORY));
        }

        @Test
        @DisplayName("knows nothing of a hierarchy the classpath does not carry")
        void knowsNothingWithoutClasspath() {
            SourceFixtures.write(sources, "com/acme/OrderRepository.java", """
                    package com.acme;
                    import org.springframework.data.jpa.repository.JpaRepository;
                    public interface OrderRepository extends JpaRepository<String, Long> {}
                    """);

            CodeModel model = SpoonFrontend.analyze(FrontendRequest.of(sources));

            assertThat(model.supertypesOf(TypeId.of("com.acme.OrderRepository")))
                    .containsExactly(TypeId.of(JPA_REPOSITORY));
            assertThat(model.supertypesOf(TypeId.of(JPA_REPOSITORY))).isEmpty();
        }
    }

    @Nested
    @DisplayName("from the analyzed sources")
    class FromTheSources {

        @Test
        @DisplayName("walks classes and interfaces together, nearest first")
        void walksClassesAndInterfacesNearestFirst() {
            SourceFixtures.write(sources, "com/acme/Auditable.java", "package com.acme; public interface Auditable {}");
            SourceFixtures.write(
                    sources,
                    "com/acme/Base.java",
                    "package com.acme; public abstract class Base implements Auditable {}");
            SourceFixtures.write(
                    sources, "com/acme/Order.java", "package com.acme; public class Order extends Base {}");

            CodeModel model = SpoonFrontend.analyze(FrontendRequest.of(sources));

            assertThat(model.supertypesOf(TypeId.of("com.acme.Order")))
                    .containsExactly(TypeId.of("com.acme.Base"), TypeId.of("com.acme.Auditable"));
        }

        @Test
        @DisplayName("leaves the implicit root of every hierarchy out")
        void leavesTheImplicitRootOut() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

            CodeModel model = SpoonFrontend.analyze(FrontendRequest.of(sources));

            assertThat(model.supertypesOf(TypeId.of("com.acme.Order"))).isEmpty();
        }

        @Test
        @DisplayName("survives a cycle in the declared hierarchy without looping")
        void survivesADeclaredCycle() {
            SourceFixtures.write(sources, "com/acme/A.java", "package com.acme; public interface A extends B {}");
            SourceFixtures.write(sources, "com/acme/B.java", "package com.acme; public interface B extends A {}");

            CodeModel model = SpoonFrontend.analyze(FrontendRequest.of(sources));

            assertThat(model.supertypesOf(TypeId.of("com.acme.A")))
                    .containsExactly(TypeId.of("com.acme.B"), TypeId.of("com.acme.A"));
        }

        @Test
        @DisplayName("renders the same closure on repeated runs")
        void rendersTheSameClosureOnRepeatedRuns() {
            SourceFixtures.write(sources, "com/acme/Marker.java", "package com.acme; public interface Marker {}");
            SourceFixtures.write(sources, "com/acme/Audited.java", "package com.acme; public interface Audited {}");
            SourceFixtures.write(
                    sources,
                    "com/acme/Order.java",
                    "package com.acme; public class Order implements Marker, Audited {}");

            List<TypeId> first =
                    SpoonFrontend.analyze(FrontendRequest.of(sources)).supertypesOf(TypeId.of("com.acme.Order"));
            List<TypeId> second =
                    SpoonFrontend.analyze(FrontendRequest.of(sources)).supertypesOf(TypeId.of("com.acme.Order"));

            assertThat(first)
                    .containsExactly(TypeId.of("com.acme.Audited"), TypeId.of("com.acme.Marker"))
                    .isEqualTo(second);
        }
    }
}
