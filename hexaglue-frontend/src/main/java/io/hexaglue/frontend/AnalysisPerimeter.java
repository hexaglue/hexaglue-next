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
import io.hexaglue.model.finding.IssueCode;
import java.util.List;
import java.util.Optional;
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

    /** A type whose package falls outside the configured perimeter was not read. */
    private static final IssueCode OUT_OF_SCOPE = IssueCode.of("HG-FRONTEND-004");

    /** A type a generator wrote was not read. */
    private static final IssueCode GENERATED_CODE = IssueCode.of("HG-FRONTEND-005");

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
     * Why a parsed type is left out of the analysis: the published code naming the cause, and the
     * words stating it. The reason is worded here, where the decision is taken, so that a cause
     * and its explanation cannot drift apart.
     *
     * @param code the published code of the cause
     * @param reason what to say after "the type was not analyzed:"
     */
    record Exclusion(IssueCode code, String reason) {}

    /**
     * Returns why a parsed type is left out of the analysis, or empty when it belongs to it.
     *
     * @param type the parsed type
     * @return the reason the type is not read, or empty when it is read
     */
    Optional<Exclusion> exclusionOf(CtType<?> type) {
        String packageName = packageOf(type);
        if (!coversPackage(packageName)) {
            return Optional.of(new Exclusion(
                    OUT_OF_SCOPE, "its package " + packageName + " is outside the configured analysis scope"));
        }
        return generationMarker(type)
                .map(marker -> new Exclusion(GENERATED_CODE, "it is generated code, marked by @" + marker));
    }

    private boolean coversPackage(String packageName) {
        boolean included = includedPackages.isEmpty()
                || includedPackages.stream().anyMatch(prefix -> startsWithSegment(packageName, prefix));
        return included && excludedPackages.stream().noneMatch(prefix -> startsWithSegment(packageName, prefix));
    }

    /**
     * Returns the generation marker carried by the type or by any type enclosing it — a nested
     * type of generated code is generated too — or empty when none of them carries one.
     */
    private static Optional<String> generationMarker(CtType<?> type) {
        for (CtType<?> current = type; current != null; current = current.getDeclaringType()) {
            for (CtAnnotation<?> annotation : current.getAnnotations()) {
                String marker = annotation.getAnnotationType().getQualifiedName();
                if (GENERATED_MARKERS.contains(marker)) {
                    return Optional.of(marker);
                }
            }
        }
        return Optional.empty();
    }

    private static String packageOf(CtType<?> type) {
        return type.getPackage() == null ? "" : type.getPackage().getQualifiedName();
    }

    private static boolean startsWithSegment(String packageName, String prefix) {
        return packageName.equals(prefix) || packageName.startsWith(prefix + ".");
    }
}
