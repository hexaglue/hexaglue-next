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

package io.hexaglue.spi;

import java.util.Objects;

/**
 * A file a plugin wants written: where, relative to a directory the host chooses, and what.
 *
 * <p>The path is relative by construction — no root, no drive, no step upwards, no separator but
 * the forward slash. A plugin that could name an absolute path would make the confinement of the
 * output a matter of good behaviour; here it is a matter of shape, checked once, at the only place
 * a path enters the contract.</p>
 *
 * @param path where the document goes, relative to the host's output directory
 * @param content the whole content of the document
 * @since 7.0.0
 */
public record Document(String path, String content) {

    /**
     * Validates the path.
     */
    public Document {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("document path must not be blank");
        }
        if (path.indexOf('\\') >= 0 || path.indexOf(':') >= 0) {
            throw new IllegalArgumentException("document path must use forward slashes and no drive: " + path);
        }
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("document path must be relative to the output directory: " + path);
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("document path must not step out of the output directory: " + path);
            }
        }
    }
}
