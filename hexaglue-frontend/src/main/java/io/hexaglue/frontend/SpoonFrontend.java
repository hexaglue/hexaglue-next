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
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.MethodBodyFacts;
import io.hexaglue.model.code.TypeNode;
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

    private SpoonFrontend() {}

    /**
     * Reads the requested sources into a code model.
     *
     * @param request what to read and how
     * @return the code model of the analyzed sources
     */
    public static CodeModel analyze(FrontendRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        CtModel parsed = parse(request);

        AnalysisPerimeter perimeter = new AnalysisPerimeter(request.scope());
        TypeNodeMapper mapper =
                new TypeNodeMapper(new SourceLocations(request.sourceRoots()), request.has(METHOD_BODIES));
        List<MappedType> mapped =
                analyzedTypes(parsed, perimeter).stream().map(mapper::map).toList();

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
                "Code model built: {} analyzed types, {} external stubs, {} edges",
                nodes.size(),
                relations.stubs().size(),
                relations.all().size());
        return model.build();
    }

    private static CtModel parse(FrontendRequest request) {
        Launcher launcher = new Launcher();
        configure(launcher.getEnvironment(), request);
        for (Path sourceRoot : request.sourceRoots()) {
            launcher.addInputResource(sourceRoot.toAbsolutePath().toString());
        }
        return launcher.buildModel();
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
     * Returns the types to read, in identity order.
     *
     * <p>Nested types are read like any other: the parser's top-level view hides the domain
     * written inside an aggregate. Anonymous and local classes are left out — they name nothing
     * stable and express no architectural intent.</p>
     */
    // The explicit type argument is load-bearing: with a diamond the filter infers the raw
    // CtType and the result no longer carries the wildcard the model mapper expects.
    @SuppressWarnings("PMD.UseDiamondOperator")
    private static List<CtType<?>> analyzedTypes(CtModel parsed, AnalysisPerimeter perimeter) {
        return parsed.getElements(new TypeFilter<CtType<?>>(CtType.class)).stream()
                .filter(SpoonFrontend::isNamedDeclaration)
                .filter(perimeter::covers)
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
