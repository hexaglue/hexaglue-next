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
import io.hexaglue.model.classification.RemediationHint;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A coded report on the tool's own condition: an analysis that failed, a generation refused
 * under the confidence threshold, a degraded step. Where a {@link Finding} judges the analyzed
 * architecture, a diagnostic explains HexaGlue itself — loudly, never as a silently empty result.
 *
 * <p>When a diagnostic replaces an output (refused generation), it carries the subject and the
 * typed remediations that would unlock it.</p>
 *
 * @param code the published issue code
 * @param severity the compiler-like severity
 * @param message the human-readable statement of what happened
 * @param subject the type concerned, when the problem is about one
 * @param location the source location, when one localizes the problem
 * @param remediations typed suggestions to unlock the tool, most effective first
 * @since 7.0.0
 */
public record Diagnostic(
        IssueCode code,
        DiagnosticSeverity severity,
        String message,
        Optional<TypeId> subject,
        Optional<SourceLocation> location,
        List<RemediationHint> remediations) {

    /**
     * Validates the message and copies the remediations.
     */
    public Diagnostic {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(remediations, "remediations must not be null");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        remediations = List.copyOf(remediations);
    }

    /**
     * Creates a builder for a diagnostic.
     *
     * @param code the published issue code
     * @param severity the compiler-like severity
     * @param message the human-readable statement of what happened
     * @return a new builder
     */
    public static Builder builder(IssueCode code, DiagnosticSeverity severity, String message) {
        return new Builder(code, severity, message);
    }

    /**
     * Builder for {@link Diagnostic} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final IssueCode code;
        private final DiagnosticSeverity severity;
        private final String message;
        private Optional<TypeId> subject = Optional.empty();
        private Optional<SourceLocation> location = Optional.empty();
        private List<RemediationHint> remediations = List.of();

        private Builder(IssueCode code, DiagnosticSeverity severity, String message) {
            this.code = Objects.requireNonNull(code, "code must not be null");
            this.severity = Objects.requireNonNull(severity, "severity must not be null");
            this.message = Objects.requireNonNull(message, "message must not be null");
        }

        /**
         * Sets the type the diagnostic is about.
         *
         * @param subject the subject type id
         * @return this builder
         */
        public Builder subject(TypeId subject) {
            this.subject = Optional.of(subject);
            return this;
        }

        /**
         * Sets the source location of the problem.
         *
         * @param location the location
         * @return this builder
         */
        public Builder location(SourceLocation location) {
            this.location = Optional.of(location);
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
         * Builds the diagnostic.
         *
         * @return a new Diagnostic
         */
        public Diagnostic build() {
            return new Diagnostic(code, severity, message, subject, location, remediations);
        }
    }
}
