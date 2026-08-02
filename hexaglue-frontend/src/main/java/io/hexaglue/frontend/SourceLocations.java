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

import io.hexaglue.model.SourceLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

/**
 * Turns parser positions into model locations, with paths made relative to the source root they
 * were read from and separators normalized to {@code /}.
 *
 * <p>Relative paths are what make an analysis reproducible: the same sources analyzed from two
 * checkouts must render identical locations in reports and golden files.</p>
 */
final class SourceLocations {

    private final List<Path> sourceRoots;

    SourceLocations(List<Path> sourceRoots) {
        this.sourceRoots = sourceRoots.stream().map(SourceLocations::canonical).toList();
    }

    /**
     * Returns the location of an element, when the parser knows one.
     *
     * @param element the parsed element
     * @return the location, or empty when the element has no valid position
     */
    Optional<SourceLocation> of(CtElement element) {
        SourcePosition position = element.getPosition();
        if (position == null || !position.isValidPosition() || position.getFile() == null) {
            return Optional.empty();
        }
        Path file = canonical(position.getFile().toPath());
        int lineStart = position.getLine();
        int lineEnd = Math.max(lineStart, position.getEndLine());
        return Optional.of(new SourceLocation(display(file), lineStart, lineEnd));
    }

    private String display(Path file) {
        for (Path root : sourceRoots) {
            if (file.startsWith(root)) {
                return separated(root.relativize(file));
            }
        }
        return separated(file);
    }

    private static String separated(Path path) {
        return path.toString().replace('\\', '/');
    }

    /**
     * Resolves a path to its real location when the file system allows it, so that a root given
     * through a symbolic link still matches the path the parser reports. Falls back to plain
     * normalization for a path that does not exist.
     */
    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException unresolvable) {
            return path.toAbsolutePath().normalize();
        }
    }
}
