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

import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.spi.Contribution;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.PluginManifest;
import io.hexaglue.spi.SourceFile;
import java.util.List;
import java.util.Optional;

/**
 * The persistence side of a hexagon, written from the classified model.
 *
 * <p>What gets stored is what the analysis said has a life of its own: an aggregate and its parts
 * become tables, a value becomes part of the row that holds it. Nothing here decides what a type
 * is — that question was answered before this plugin was handed anything.</p>
 *
 * @since 7.0.0
 */
public final class JpaPlugin implements HexaGluePlugin {

    /** The identifier other plugins depend on to run after this one. */
    public static final String ID = "io.hexaglue.jpa";

    /** A verdict this run is not sure enough about to write persistence code from. */
    static final IssueCode TOO_UNSURE = IssueCode.of("HG-JPA-001");

    /** An aggregate or a part whose identity the analysis could not name. */
    static final IssueCode NO_IDENTITY = IssueCode.of("HG-JPA-002");

    /**
     * Creates the plugin. A host discovers backends by loading them as services, which needs a
     * constructor that is public and takes nothing.
     */
    // Written out rather than left implicit: the strict Javadoc of a published module documents
    // every constructor it exposes, and a service is instantiated through this one.
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public JpaPlugin() {
        // Nothing to hold: a contribution is a function of the model it is handed.
    }

    @Override
    public PluginManifest manifest() {
        return new PluginManifest(ID, List.of(), JpaOptions.KEYS);
    }

    @Override
    public void contribute(Contribution contribution) {
        JpaOptions options = JpaOptions.from(contribution.config());
        Stored stored = new Stored(contribution.model(), options);
        contribution.model().all(DomainType.class).forEach(type -> store(type, contribution, stored, options));
    }

    /**
     * Stores one domain type, or says why it was left alone. The two reasons are told apart because
     * they ask different things of an author: one is a verdict to make surer, the other an identity
     * to give the type.
     */
    private void store(DomainType type, Contribution contribution, Stored stored, JpaOptions options) {
        if (!storable(type, options)) {
            return;
        }
        Optional<Field> identity = identityOf(type);
        if (!contribution.isCertainEnough(type)) {
            contribution.report(tooUnsure(type, contribution));
            return;
        }
        if (hasATableOfItsOwn(type) && identity.isEmpty()) {
            contribution.report(noIdentity(type));
            return;
        }
        SourceFile source = new StoredType(type, stored, options).render(identity);
        contribution.emit(options.targetModule().map(source::in).orElse(source));
    }

    /** Only what the store has a shape for: aggregates, their parts, and the values they hold. */
    private static boolean storable(DomainType type, JpaOptions options) {
        if (type instanceof ValueObject) {
            return options.embeddables();
        }
        return hasATableOfItsOwn(type);
    }

    private static boolean hasATableOfItsOwn(DomainType type) {
        return type instanceof AggregateRoot || type instanceof Entity;
    }

    private static Optional<Field> identityOf(DomainType type) {
        if (type instanceof AggregateRoot aggregate) {
            return aggregate.identityField();
        }
        if (type instanceof Entity entity) {
            return entity.identityField();
        }
        return Optional.empty();
    }

    /**
     * Below the threshold, the remediation the engine wrote for this very type travels with the
     * refusal: what makes a verdict surer is a property of the type, not of the backend that
     * declined to act on it.
     */
    private static Diagnostic tooUnsure(ArchType type, Contribution contribution) {
        return Diagnostic.builder(
                        TOO_UNSURE,
                        DiagnosticSeverity.WARNING,
                        "no persistence was written for " + type.qualifiedName() + ": it was read as "
                                + type.kind() + " with " + type.classification().confidence()
                                + " confidence, and this build generates from "
                                + contribution.minConfidence() + " upwards")
                .subject(type.id())
                .remediations(type.classification().remediations())
                .build();
    }

    private static Diagnostic noIdentity(ArchType type) {
        return Diagnostic.builder(
                        NO_IDENTITY,
                        DiagnosticSeverity.WARNING,
                        "no persistence was written for " + type.qualifiedName()
                                + ": nothing in the analysis names the field carrying its identity, and a row"
                                + " cannot be found without one")
                .subject(type.id())
                .build();
    }
}
