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

/**
 * Declaration-level records shared by the code model and the architectural model: fields, methods,
 * constructors, parameters and annotations with fully typed values.
 *
 * <p>There is a single implementation of each concept. The frontend builds these records from
 * source with the semantic roles left empty; the engine fills {@code FieldRole} and
 * {@code MethodRole} when it assembles the architectural model. Annotation values are always
 * typed ({@link io.hexaglue.model.declaration.AnnotationValue}), never stringified.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.model.declaration;
