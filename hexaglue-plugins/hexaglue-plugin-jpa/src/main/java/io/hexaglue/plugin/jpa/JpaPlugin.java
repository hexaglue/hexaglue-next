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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivenPortType;
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

    /** A repository port the analysis did not reach an aggregate for. */
    static final IssueCode NOTHING_KEPT = IssueCode.of("HG-JPA-003");

    /** A domain type the generated code has no way of reading or of rebuilding. */
    static final IssueCode OUT_OF_REACH = IssueCode.of("HG-JPA-004");

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
        if (options.repositories()) {
            contribution.model().all(DrivenPort.class).forEach(port -> serve(port, contribution, stored, options));
        }
    }

    /**
     * Serves one repository port. A port that keeps nothing the analysis could name gets nothing:
     * there is no table to reach for, and guessing one is how a generator writes something that
     * looks right and stores the wrong rows.
     */
    private void serve(DrivenPort port, Contribution contribution, Stored stored, JpaOptions options) {
        if (port.portType() != DrivenPortType.REPOSITORY) {
            return;
        }
        Optional<AggregateRoot> kept = port.managedAggregate()
                .flatMap(reference -> contribution.model().type(TypeId.of(reference.qualifiedName())))
                .filter(AggregateRoot.class::isInstance)
                .map(AggregateRoot.class::cast);
        if (kept.isEmpty()) {
            contribution.report(nothingKept(port));
            return;
        }
        AggregateRoot aggregate = kept.orElseThrow();
        if (!contribution.isCertainEnough(port) || !contribution.isCertainEnough(aggregate)) {
            contribution.report(tooUnsure(port, contribution));
            return;
        }
        if (aggregate.identityField().isEmpty()) {
            contribution.report(noIdentity(aggregate));
            return;
        }
        SourceFile source = new StoredRepository(port, aggregate, stored, options).render();
        contribution.emit(options.targetModule().map(source::in).orElse(source));
    }

    private static Diagnostic nothingKept(DrivenPort port) {
        return Diagnostic.builder(
                        NOTHING_KEPT,
                        DiagnosticSeverity.WARNING,
                        "no repository was written for " + port.qualifiedName()
                                + ": the analysis did not reach which aggregate it keeps, and a store"
                                + " serves one thing rather than anything")
                .subject(port.id())
                .build();
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
        if (options.mappers()) {
            carry(type, contribution, stored, options);
        }
    }

    /**
     * Writes the two ways between a type and its row, or says which field stands in the way. A
     * mapper is all-or-nothing: one that carried most of a type would lose the rest on the way out
     * and rebuild something that is not what was stored.
     */
    private void carry(DomainType type, Contribution contribution, Stored stored, JpaOptions options) {
        StoredMapper mapper = new StoredMapper(type, contribution.model(), stored, options);
        if (!DomainAccess.isRebuildable(type)) {
            contribution.report(
                    outOfReach(type, "nothing in it takes its own state, so a row could not be turned back into one"));
            return;
        }
        Optional<Field> blocking = mapper.unmappable();
        if (blocking.isPresent()) {
            contribution.report(outOfReach(
                    type,
                    "its field " + blocking.orElseThrow().name()
                            + " cannot be carried across, and half a mapper loses what it skips"));
            return;
        }
        SourceFile source = mapper.render();
        contribution.emit(options.targetModule().map(source::in).orElse(source));
    }

    private static Diagnostic outOfReach(ArchType type, String because) {
        return Diagnostic.builder(
                        OUT_OF_REACH,
                        DiagnosticSeverity.WARNING,
                        "no mapper was written for " + type.qualifiedName() + ": " + because)
                .subject(type.id())
                .build();
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
