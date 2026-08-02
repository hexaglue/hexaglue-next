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

package io.hexaglue.model.arch;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Classification;

/**
 * A classified type of the architectural model.
 *
 * <p>Every ArchType carries a stable {@link TypeId} that survives reclassification, its
 * {@link ArchKind}, its structure, and the complete {@link Classification} that explains the
 * verdict. The hierarchy is sealed — domain, ports, application, adapters, categorized fallback —
 * so consumers can match exhaustively.</p>
 *
 * @since 7.0.0
 */
public sealed interface ArchType permits DomainType, PortType, ApplicationType, AdapterType, UnclassifiedType {

    /**
     * Returns the stable identity of this type, independent of its classification.
     *
     * @return the type id
     */
    TypeId id();

    /**
     * Returns the architectural kind of this type.
     *
     * @return the kind
     */
    ArchKind kind();

    /**
     * Returns the structural description of this type.
     *
     * @return the structure
     */
    TypeStructure structure();

    /**
     * Returns the complete verdict on this type: confidence, basis, evidences, candidates, proof
     * and remediation.
     *
     * @return the classification
     */
    Classification classification();

    /**
     * Returns the fully qualified name of this type.
     *
     * @return the qualified name
     */
    default String qualifiedName() {
        return id().qualifiedName();
    }

    /**
     * Returns the simple name of this type.
     *
     * @return the simple name
     */
    default String simpleName() {
        return id().simpleName();
    }

    /**
     * Returns the package of this type.
     *
     * @return the package name
     */
    default String packageName() {
        return id().packageName();
    }
}
