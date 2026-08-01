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

package io.hexaglue.model.finding;

import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.RemediationHint;
import java.util.List;
import java.util.Objects;

/**
 * A coded, located, documented verdict of an audit rule on the analyzed architecture. This is the
 * single findings model: the renderers display it and the validation gates threshold on it — the
 * same object, never two divergent readings.
 *
 * <p>A finding justifies itself the way a classification does: with tiered {@link Evidence} and
 * typed {@link RemediationHint}s, so every refusal can print the specific fix for the type at
 * hand.</p>
 *
 * @param code the published issue code
 * @param severity the severity the gates consume
 * @param message the human-readable statement of the problem
 * @param subject the type the finding is about
 * @param relatedTypes the other types involved, in rule order
 * @param locations the source locations of the problem, in rule order
 * @param evidences the evidences supporting the finding, in tier order
 * @param remediations type-specific suggestions to fix the problem, most effective first
 * @since 7.0.0
 */
public record Finding(
        IssueCode code,
        Severity severity,
        String message,
        TypeId subject,
        List<TypeId> relatedTypes,
        List<SourceLocation> locations,
        List<Evidence> evidences,
        List<RemediationHint> remediations) {

    /**
     * Validates the message and copies every collection.
     */
    public Finding {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(relatedTypes, "relatedTypes must not be null");
        Objects.requireNonNull(locations, "locations must not be null");
        Objects.requireNonNull(evidences, "evidences must not be null");
        Objects.requireNonNull(remediations, "remediations must not be null");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        relatedTypes = List.copyOf(relatedTypes);
        locations = List.copyOf(locations);
        evidences = List.copyOf(evidences);
        remediations = List.copyOf(remediations);
    }

    /**
     * Creates a builder for a finding.
     *
     * @param code the published issue code
     * @param severity the severity the gates consume
     * @param message the human-readable statement of the problem
     * @param subject the type the finding is about
     * @return a new builder
     */
    public static Builder builder(IssueCode code, Severity severity, String message, TypeId subject) {
        return new Builder(code, severity, message, subject);
    }

    /**
     * Builder for {@link Finding} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final IssueCode code;
        private final Severity severity;
        private final String message;
        private final TypeId subject;
        private List<TypeId> relatedTypes = List.of();
        private List<SourceLocation> locations = List.of();
        private List<Evidence> evidences = List.of();
        private List<RemediationHint> remediations = List.of();

        private Builder(IssueCode code, Severity severity, String message, TypeId subject) {
            this.code = Objects.requireNonNull(code, "code must not be null");
            this.severity = Objects.requireNonNull(severity, "severity must not be null");
            this.message = Objects.requireNonNull(message, "message must not be null");
            this.subject = Objects.requireNonNull(subject, "subject must not be null");
        }

        /**
         * Sets the other types involved in the finding.
         *
         * @param relatedTypes the related type ids, in rule order
         * @return this builder
         */
        public Builder relatedTypes(List<TypeId> relatedTypes) {
            this.relatedTypes = relatedTypes;
            return this;
        }

        /**
         * Sets the source locations of the problem.
         *
         * @param locations the locations, in rule order
         * @return this builder
         */
        public Builder locations(List<SourceLocation> locations) {
            this.locations = locations;
            return this;
        }

        /**
         * Sets the evidences supporting the finding.
         *
         * @param evidences the evidences, in tier order
         * @return this builder
         */
        public Builder evidences(List<Evidence> evidences) {
            this.evidences = evidences;
            return this;
        }

        /**
         * Sets the remediation hints.
         *
         * @param remediations the hints, most effective first
         * @return this builder
         */
        public Builder remediations(List<RemediationHint> remediations) {
            this.remediations = remediations;
            return this;
        }

        /**
         * Builds the finding.
         *
         * @return a new Finding
         */
        public Finding build() {
            return new Finding(code, severity, message, subject, relatedTypes, locations, evidences, remediations);
        }
    }
}
