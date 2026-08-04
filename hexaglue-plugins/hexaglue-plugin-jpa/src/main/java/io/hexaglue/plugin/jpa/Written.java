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
 * What every type this backend writes says about itself: that it was written, and by what.
 *
 * <p>This is read back on the next run. The analysis leaves generated code outside its perimeter,
 * so an entity or an adapter written here is never classified as if an author had meant it: a
 * generated adapter that did not say so would be read as the architecture instead of as what was
 * made of it, and the second run would not conclude what the first one did.</p>
 *
 * <p>The annotation is the JDK's rather than the persistence provider's, because what is being
 * stated has nothing to do with storage.</p>
 */
final class Written {

    private static final ClassName GENERATED = ClassName.get("javax.annotation.processing", "Generated");

    private Written() {}

    /**
     * Returns the mark a generated type carries.
     *
     * @param plugin the identifier of the backend that wrote it
     * @return the annotation to add to the type
     */
    static AnnotationSpec by(String plugin) {
        return AnnotationSpec.builder(GENERATED)
                .addMember("value", "$S", plugin)
                .build();
    }
}
