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

    private final List<Root> sourceRoots;

    SourceLocations(List<Path> sourceRoots) {
        this.sourceRoots = sourceRoots.stream()
                .map(root -> new Root(root, canonical(root)))
                .toList();
    }

    /**
     * Returns the location of an element, when the parser knows one.
     *
     * @param element the parsed element
     * @return the location, or empty when the element has no valid position
     */
    Optional<SourceLocation> of(CtElement element) {
        return file(element).map(file -> {
            SourcePosition position = element.getPosition();
            int lineStart = position.getLine();
            int lineEnd = Math.max(lineStart, position.getEndLine());
            return new SourceLocation(display(file), lineStart, lineEnd);
        });
    }

    /**
     * Returns the source root an element was read from, as the caller stated it.
     *
     * <p>The path is handed back unchanged rather than resolved, because it is the key everything
     * the caller knows about that root is filed under.</p>
     *
     * @param element the parsed element
     * @return the root, or empty when the element has no valid position or comes from none of them
     */
    Optional<Path> rootOf(CtElement element) {
        return file(element)
                .flatMap(file -> sourceRoots.stream()
                        .filter(root -> file.startsWith(root.canonical()))
                        .findFirst()
                        .map(Root::declared));
    }

    private Optional<Path> file(CtElement element) {
        SourcePosition position = element.getPosition();
        if (position == null || !position.isValidPosition() || position.getFile() == null) {
            return Optional.empty();
        }
        return Optional.of(canonical(position.getFile().toPath()));
    }

    private String display(Path file) {
        for (Root root : sourceRoots) {
            if (file.startsWith(root.canonical())) {
                return separated(root.canonical().relativize(file));
            }
        }
        return separated(file);
    }

    /**
     * A source root under the two names it answers to: the one the caller stated, and the one the
     * file system resolves it to.
     */
    private record Root(Path declared, Path canonical) {}

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
