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

import io.hexaglue.model.config.AnalysisScope;
import java.util.List;
import java.util.Set;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;

/**
 * Decides which parsed types are analyzed.
 *
 * <p>Two things keep a type out. Its package may fall outside the configured perimeter — matched
 * on whole package segments, so {@code com.acme} never captures {@code com.acmetools}. Or it may
 * be generated code: re-reading what a generator emitted feeds a tool its own output and teaches
 * it the generator's conventions instead of the author's intent. Generated types are recognized
 * by the fully qualified name of their marker annotation, never by a name pattern.</p>
 *
 * <p>Keeping generated code out here rather than judging it later is deliberate, and it was
 * measured: a build tool hands an analysis every source root it compiles, generated ones included,
 * so an emitted adapter would come back implementing the port its author wrote. The rules would
 * then read that port as one the core implements — a seam rather than a boundary — and the port,
 * along with the service calling it, would lose its reading on the second run over unchanged
 * sources. What a generator wrote must not be able to change what its author's code means.</p>
 *
 * <p>A type left out is not erased from the world: it still becomes an external stub when
 * something inside the perimeter refers to it.</p>
 */
final class AnalysisPerimeter {

    private static final Set<String> GENERATED_MARKERS = Set.of(
            "javax.annotation.Generated",
            "javax.annotation.processing.Generated",
            "jakarta.annotation.Generated",
            "lombok.Generated");

    private final List<String> includedPackages;
    private final List<String> excludedPackages;

    AnalysisPerimeter(AnalysisScope scope) {
        this.includedPackages = scope.includePackages();
        this.excludedPackages = scope.excludePackages();
    }

    /**
     * Returns whether a parsed type belongs to the analysis.
     *
     * @param type the parsed type
     * @return true when the type must be read into the code model
     */
    boolean covers(CtType<?> type) {
        return coversPackage(packageOf(type)) && !isGenerated(type);
    }

    private boolean coversPackage(String packageName) {
        boolean included = includedPackages.isEmpty()
                || includedPackages.stream().anyMatch(prefix -> startsWithSegment(packageName, prefix));
        return included && excludedPackages.stream().noneMatch(prefix -> startsWithSegment(packageName, prefix));
    }

    /**
     * Returns whether the type, or any type enclosing it, carries a generation marker: a nested
     * type of generated code is generated too.
     */
    private static boolean isGenerated(CtType<?> type) {
        for (CtType<?> current = type; current != null; current = current.getDeclaringType()) {
            for (CtAnnotation<?> annotation : current.getAnnotations()) {
                if (GENERATED_MARKERS.contains(annotation.getAnnotationType().getQualifiedName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String packageOf(CtType<?> type) {
        return type.getPackage() == null ? "" : type.getPackage().getQualifiedName();
    }

    private static boolean startsWithSegment(String packageName, String prefix) {
        return packageName.equals(prefix) || packageName.startsWith(prefix + ".");
    }
}
