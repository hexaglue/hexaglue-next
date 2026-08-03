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

import static io.hexaglue.model.code.CodeModelCapability.METHOD_BODIES;

import io.hexaglue.frontend.TypeNodeMapper.MappedType;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.MethodBodyFacts;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Reads Java sources into a {@link CodeModel}.
 *
 * <p>This is the whole frontend API: a request in, a code model out. There is no parser
 * abstraction to implement and no partially built model to observe — the model is complete or the
 * call fails.</p>
 *
 * <p>Parsing is tolerant: a reference the classpath cannot resolve keeps the name the source
 * gives it rather than failing the analysis, because the projects HexaGlue reads rarely hand over
 * a complete classpath. The richer the classpath, the more classpath types become knowable.</p>
 *
 * @since 7.0.0
 */
public final class SpoonFrontend {

    private static final Logger LOG = LoggerFactory.getLogger(SpoonFrontend.class);

    /** The analysis was pointed at a source root it cannot read. */
    private static final IssueCode SOURCE_ROOT_UNREADABLE = IssueCode.of("HG-FRONTEND-001");

    /** The analysis was given a classpath entry that does not exist. */
    private static final IssueCode CLASSPATH_ENTRY_MISSING = IssueCode.of("HG-FRONTEND-002");

    /** The sources could not be parsed. */
    private static final IssueCode PARSING_FAILED = IssueCode.of("HG-FRONTEND-003");

    /** The parser recovered from a source it could not fully read. */
    private static final IssueCode PARSING_RECOVERED = IssueCode.of("HG-FRONTEND-006");

    private SpoonFrontend() {}

    /**
     * Reads the requested sources into a code model, and says what it left out of it.
     *
     * @param request what to read and how
     * @return the code model of the analyzed sources, with the diagnostics of what was not read
     */
    public static FrontendResult analyze(FrontendRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Parsed parsed = parse(request);

        AnalysisPerimeter perimeter = new AnalysisPerimeter(request.scope());
        SourceLocations locations = new SourceLocations(request.sourceRoots());
        TypeNodeMapper mapper = new TypeNodeMapper(locations, request.has(METHOD_BODIES));

        List<CtType<?>> analyzed = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        // The reading as a whole comes before what it left out, type by type.
        if (parsed.recoveries() > 0) {
            diagnostics.add(recovered(parsed.recoveries()));
        }
        for (CtType<?> type : declaredTypes(parsed.model())) {
            perimeter
                    .exclusionOf(type)
                    .ifPresentOrElse(
                            exclusion -> diagnostics.add(leftOut(type, exclusion, locations)),
                            () -> analyzed.add(type));
        }

        List<MappedType> mapped = analyzed.stream().map(mapper::map).toList();
        List<TypeNode> nodes = mapped.stream().map(MappedType::node).toList();
        List<MethodBodyFacts> bodyFacts =
                mapped.stream().flatMap(type -> type.bodyFacts().stream()).toList();
        Edges relations = Edges.from(nodes, bodyFacts);

        List<TypeNode> allNodes = new ArrayList<>(nodes);
        allNodes.addAll(relations.stubs());

        CodeModel.Builder model = CodeModel.builder();
        request.capabilities().forEach(model::capability);
        allNodes.forEach(model::addType);
        relations.all().forEach(model::addEdge);
        bodyFacts.forEach(model::addBodyFacts);
        Supertypes.closures(allNodes, request.classpath()).forEach(model::supertypes);
        LOG.debug(
                "Code model built: {} analyzed types, {} external stubs, {} edges, {} diagnostics",
                nodes.size(),
                relations.stubs().size(),
                relations.all().size(),
                diagnostics.size());
        return new FrontendResult(model.build(), diagnostics);
    }

    /**
     * Words a parser recovery. Tolerant parsing is what makes an incomplete code base analyzable,
     * but a declaration read half-way is indistinguishable from one written that way: the reading
     * is the only place that knows the difference, so it is the only place that can say it.
     */
    private static Diagnostic recovered(int recoveries) {
        return Diagnostic.builder(
                        PARSING_RECOVERED,
                        DiagnosticSeverity.WARNING,
                        "The parser recovered from " + recoveries
                                + " problem(s) while reading the sources; the declarations it could not fully read"
                                + " are incomplete in the model")
                .build();
    }

    /**
     * Words an exclusion as a coded diagnostic. A type left out is not erased from the world — it
     * still becomes an external stub when something analyzed refers to it — so the message says
     * what was not read, never that the type does not exist.
     */
    private static Diagnostic leftOut(CtType<?> type, AnalysisPerimeter.Exclusion exclusion, SourceLocations locations) {
        String name = type.getQualifiedName();
        Diagnostic.Builder diagnostic = Diagnostic.builder(
                        exclusion.code(), DiagnosticSeverity.INFO, name + " was not analyzed: " + exclusion.reason())
                .subject(TypeId.of(name));
        locations.of(type).ifPresent(diagnostic::location);
        return diagnostic.build();
    }

    /**
     * What one parse produced: the parsed sources, and how many problems the parser recovered from
     * along the way. The count is the parser's own — measured to react to a source it could not
     * read, and not to a reference it could not resolve, which is the normal condition of an
     * analysis run without a complete classpath.
     */
    private record Parsed(CtModel model, int recoveries) {}

    // The parser signals every kind of setup and reading failure with unchecked exceptions of its
    // own and of its compiler backend. They are caught wholesale and rethrown coded — nothing is
    // swallowed, and no partial model escapes.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static Parsed parse(FrontendRequest request) {
        checkInputs(request);
        Launcher launcher = new Launcher();
        Environment environment = launcher.getEnvironment();
        configure(environment, request);
        for (Path sourceRoot : request.sourceRoots()) {
            launcher.addInputResource(sourceRoot.toAbsolutePath().toString());
        }
        try {
            CtModel model = launcher.buildModel();
            return new Parsed(model, environment.getErrorCount() + environment.getWarningCount());
        } catch (RuntimeException parseFailure) {
            // Converted, not swallowed: the caller gets a coded failure instead of a model
            // silently missing whatever could not be parsed.
            throw new FrontendException(
                    diagnostic(PARSING_FAILED, "Failed to parse the sources: " + parseFailure.getMessage()),
                    parseFailure);
        }
    }

    /**
     * Checks what the caller pointed the analysis at, before any work: a missing source root or an
     * absent classpath entry silently produces a smaller model, and a smaller model reads as a
     * smaller code base rather than as a broken setup.
     */
    private static void checkInputs(FrontendRequest request) {
        for (Path sourceRoot : request.sourceRoots()) {
            if (!Files.isDirectory(sourceRoot)) {
                throw new FrontendException(
                        diagnostic(SOURCE_ROOT_UNREADABLE, "Source root is not a readable directory: " + sourceRoot));
            }
        }
        for (Path entry : request.classpath()) {
            if (!Files.exists(entry)) {
                throw new FrontendException(
                        diagnostic(CLASSPATH_ENTRY_MISSING, "Classpath entry does not exist: " + entry));
            }
        }
    }

    private static Diagnostic diagnostic(IssueCode code, String message) {
        return Diagnostic.builder(code, DiagnosticSeverity.ERROR, message).build();
    }

    private static void configure(Environment environment, FrontendRequest request) {
        // Tolerant resolution: an unresolved reference degrades to its source-level name
        // instead of aborting the analysis of the whole project.
        environment.setNoClasspath(true);
        environment.setComplianceLevel(request.javaVersion());
        environment.setCommentEnabled(true);
        environment.setAutoImports(false);
        environment.setShouldCompile(false);
        environment.setIgnoreDuplicateDeclarations(true);
        if (!request.classpath().isEmpty()) {
            environment.setSourceClasspath(request.classpath().stream()
                    .map(entry -> entry.toAbsolutePath().toString())
                    .toArray(String[]::new));
        }
    }

    /**
     * Returns every type the sources declare, in identity order — before the perimeter decides
     * which of them are read, so that what is left out is reported in that same stable order.
     *
     * <p>Nested types are read like any other: the parser's top-level view hides the domain
     * written inside an aggregate. Anonymous and local classes are left out — they name nothing
     * stable and express no architectural intent, so nothing can be said about them either.</p>
     */
    // The explicit type argument is load-bearing: with a diamond the filter infers the raw
    // CtType and the result no longer carries the wildcard the model mapper expects.
    @SuppressWarnings("PMD.UseDiamondOperator")
    private static List<CtType<?>> declaredTypes(CtModel parsed) {
        return parsed.getElements(new TypeFilter<CtType<?>>(CtType.class)).stream()
                .filter(SpoonFrontend::isNamedDeclaration)
                .sorted(Comparator.comparing(CtType::getQualifiedName))
                .toList();
    }

    /**
     * Returns whether a parsed type is a type declaration of its own. The parser models a type
     * parameter as a type too — {@code T} of {@code Box<T>} — but a type variable declares nothing
     * and has no identity outside the declaration that introduces it.
     */
    private static boolean isNamedDeclaration(CtType<?> type) {
        String qualifiedName = type.getQualifiedName();
        return qualifiedName != null
                && !qualifiedName.isBlank()
                && !(type instanceof CtTypeParameter)
                && !type.isAnonymous()
                && !type.isLocalType();
    }
}
