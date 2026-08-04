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

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;

/**
 * The annotations a generated type carries, named once.
 *
 * <p>This backend does not depend on Jakarta Persistence: it writes code that will be compiled
 * against it. Naming the annotations in one place is what keeps a package rename from being a
 * search across every generator.</p>
 */
final class Jpa {

    private static final String PACKAGE = "jakarta.persistence";

    static final ClassName ENTITY = ClassName.get(PACKAGE, "Entity");
    static final ClassName TABLE = ClassName.get(PACKAGE, "Table");
    static final ClassName COLUMN = ClassName.get(PACKAGE, "Column");
    static final ClassName ID = ClassName.get(PACKAGE, "Id");
    static final ClassName GENERATED_VALUE = ClassName.get(PACKAGE, "GeneratedValue");
    static final ClassName GENERATION_TYPE = ClassName.get(PACKAGE, "GenerationType");
    static final ClassName EMBEDDABLE = ClassName.get(PACKAGE, "Embeddable");
    static final ClassName EMBEDDED = ClassName.get(PACKAGE, "Embedded");
    static final ClassName ONE_TO_MANY = ClassName.get(PACKAGE, "OneToMany");
    static final ClassName MANY_TO_ONE = ClassName.get(PACKAGE, "ManyToOne");
    static final ClassName ELEMENT_COLLECTION = ClassName.get(PACKAGE, "ElementCollection");

    private Jpa() {}

    /** {@code @Table(name = "…")}. */
    static AnnotationSpec table(String name) {
        return AnnotationSpec.builder(TABLE).addMember("name", "$S", name).build();
    }

    /** {@code @Column(name = "…")}. */
    static AnnotationSpec column(String name) {
        return AnnotationSpec.builder(COLUMN).addMember("name", "$S", name).build();
    }

    /**
     * {@code @GeneratedValue(strategy = …)}, or nothing at all when the domain hands the value
     * over: asking the database to generate an identity the domain already built is how a store
     * ends up with two of them.
     */
    static AnnotationSpec generatedValue(IdentityStrategy strategy) {
        return AnnotationSpec.builder(GENERATED_VALUE)
                .addMember("strategy", "$T.$L", GENERATION_TYPE, strategy.name())
                .build();
    }
}
