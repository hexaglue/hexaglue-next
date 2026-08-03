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

package io.hexaglue.plugin.livingdoc;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import io.hexaglue.plugin.livingdoc.render.Markdown;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * How a reader sees what the model calls things.
 *
 * <p>A document shows simple names, because a page of qualified names is unreadable; it links on
 * the anchor of the qualified one, because two packages may hold the same simple name. Nothing
 * here decides anything about a type — it only decides how to show it.</p>
 */
final class Names {

    /** Everything up to the last dot of a qualified name, generic arguments included. */
    private static final Pattern QUALIFIER = Pattern.compile("\\b(?:[a-zA-Z_$][\\w$]*\\.)+");

    private Names() {}

    /**
     * Returns what a type is called on a page.
     */
    static String displayOf(TypeId id) {
        return id.simpleName();
    }

    /**
     * Returns the anchor of the section documenting a type.
     */
    static String anchorOf(TypeId id) {
        return Markdown.anchorOf(id.toString());
    }

    /**
     * Returns a link to the section documenting a type, inside the same document.
     */
    static String linkTo(TypeId id) {
        return Markdown.link(displayOf(id), "#" + anchorOf(id));
    }

    /**
     * Returns a link to the section documenting a type, in another document.
     */
    static String linkTo(TypeId id, String document) {
        return Markdown.link(displayOf(id), document + "#" + anchorOf(id));
    }

    /**
     * Returns how a type reference reads, generics included and packages dropped.
     *
     * <p>A page of qualified names is a page nobody reads: {@code List<OrderLine>} carries the
     * whole meaning of {@code java.util.List<com.shop.domain.OrderLine>} to a reader who already
     * knows which codebase they are in. The qualified name stays where it disambiguates — under
     * the heading of the type, and in the anchor a link targets.</p>
     */
    static String displayOf(TypeRef type) {
        return QUALIFIER.matcher(type.toDisplayString()).replaceAll("");
    }

    /**
     * Returns how a field reads: its type then its name.
     */
    static String declarationOf(Field field) {
        return displayOf(field.type()) + " " + field.name();
    }

    /**
     * Returns how a method reads: what it gives back, what it is called, what it takes.
     */
    static String signatureOf(Method method) {
        String parameters =
                method.parameters().stream().map(Names::declarationOf).collect(Collectors.joining(", "));
        return displayOf(method.returnType()) + " " + method.name() + "(" + parameters + ")";
    }

    private static String declarationOf(Parameter parameter) {
        return displayOf(parameter.type()) + " " + parameter.name();
    }
}
