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
import java.util.Optional;

/**
 * A Java type a plugin wants written: which module it belongs in, what it is called, and what it
 * says.
 *
 * <p>A generator names a package and a type, never a path. Where those land is the host's business
 * — under a generated-sources root it also has to register for compilation — and a plugin that
 * could name a path would make the confinement of the output a matter of good behaviour rather
 * than of shape. The path is derived here, once, from names that are checked to be Java
 * identifiers.</p>
 *
 * <p>The module is what a generated adapter is routed by: persistence code belongs beside the other
 * persistence code, not beside the domain it was read from. It is optional because most projects
 * are one module, and a plugin that states none is saying "wherever this run writes" rather than
 * "the module I could not work out".</p>
 *
 * @param module the module the type belongs in, empty when the plugin states none
 * @param packageName the package the type is declared in
 * @param typeName the simple name of the type
 * @param content the whole content of the source file
 * @since 7.0.0
 */
public record SourceFile(Optional<String> module, String packageName, String typeName, String content) {

    /**
     * Validates the names.
     */
    public SourceFile {
        Objects.requireNonNull(module, "module must not be null");
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(typeName, "typeName must not be null");
        Objects.requireNonNull(content, "content must not be null");
        module.ifPresent(named -> {
            if (named.isBlank()) {
                throw new IllegalArgumentException("module must not be blank when stated");
            }
        });
        requireIdentifiers(packageName, "package name");
        requireIdentifier(typeName, "type name");
    }

    /**
     * Creates a source file that states no module.
     *
     * @param packageName the package the type is declared in
     * @param typeName the simple name of the type
     * @param content the whole content of the source file
     * @return a new source file
     */
    public static SourceFile of(String packageName, String typeName, String content) {
        return new SourceFile(Optional.empty(), packageName, typeName, content);
    }

    /**
     * Returns the same source file, routed to a module.
     *
     * @param module the module the type belongs in
     * @return a new source file, this one unchanged
     */
    public SourceFile in(String module) {
        return new SourceFile(Optional.of(module), packageName, typeName, content);
    }

    /**
     * Returns where this file goes, relative to whichever source root the host writes to.
     *
     * @return the relative path, forward slashes, ending in {@code .java}
     */
    public String path() {
        return packageName.replace('.', '/') + '/' + typeName + ".java";
    }

    /**
     * Returns the fully qualified name of the type.
     *
     * @return the qualified name
     */
    public String qualifiedName() {
        return packageName + '.' + typeName;
    }

    private static void requireIdentifiers(String dotted, String what) {
        for (String segment : dotted.split("\\.", -1)) {
            requireIdentifier(segment, what);
        }
    }

    /**
     * A name that is not a Java identifier is refused here rather than where it would become a
     * path: {@code ..} and {@code /} are not special cases to screen out, they are simply not
     * identifiers, and neither is anything else that could walk out of the output directory.
     */
    private static void requireIdentifier(String name, String what) {
        if (name.isBlank()) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            throw new IllegalArgumentException(what + " is not a Java identifier: " + name);
        }
        for (int index = 1; index < name.length(); index++) {
            if (!Character.isJavaIdentifierPart(name.charAt(index))) {
                throw new IllegalArgumentException(what + " is not a Java identifier: " + name);
            }
        }
    }
}
