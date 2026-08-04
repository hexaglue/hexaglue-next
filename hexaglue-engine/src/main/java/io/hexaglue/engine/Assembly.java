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

package io.hexaglue.engine;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.arch.ApplicationService;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.CommandHandler;
import io.hexaglue.model.arch.DrivenAdapter;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivingAdapter;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.model.arch.ModuleTopology;
import io.hexaglue.model.arch.QueryHandler;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UseCase;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;

/**
 * Turns the settled verdicts into the model every consumer reads.
 *
 * <p>One record per type of the perimeter, chosen by the ring the verdict placed it on and filled
 * from what the analysis reached about it — see {@link Links} for the two places that content comes
 * from and why they are kept apart.</p>
 */
final class Assembly {

    private final Links links;
    private final Structures structures;
    private final Fields fields;
    private final DomainAssembly domain;

    private Assembly(EngineContext context, FactBase facts, Verdicts verdicts) {
        this.links = new Links(context, facts, verdicts);
        this.structures = Structures.of(context.code());
        this.fields = new Fields(links);
        this.domain = new DomainAssembly(links);
    }

    /**
     * Builds the model of the whole perimeter.
     *
     * @param context what was analyzed
     * @param facts the facts held once the verdicts had settled
     * @param verdicts the settled verdicts
     * @param topology the build layout the reading placed the types in
     * @return the classified model, one entry per type of the perimeter
     */
    static ArchModel assemble(EngineContext context, FactBase facts, Verdicts verdicts, ModuleTopology topology) {
        Assembly assembly = new Assembly(context, facts, verdicts);
        ArchModel.Builder model = ArchModel.builder().moduleTopology(topology);
        for (TypeNode type : context.perimeter().types()) {
            model.addType(assembly.classified(type, facts, verdicts));
        }
        return model.build();
    }

    private ArchType classified(TypeNode type, FactBase facts, Verdicts verdicts) {
        Classification verdict = verdicts.verdict(type.id())
                .orElseThrow(() -> EngineException.of(
                        EngineException.MISSING_VERDICT,
                        "no verdict was reached on " + type.id().qualifiedName() + ", which the perimeter owes one"));
        ArchKind kind = verdict.kind();
        TypeStructure structure = structures.of(type, fields.of(type, kind));
        if (kind.isDomain()) {
            return domain.of(type, structure, verdict);
        }
        if (kind.isPort()) {
            return port(type, structure, verdict);
        }
        if (kind.isAdapter()) {
            return adapter(type, structure, verdict);
        }
        if (kind.isApplication()) {
            return application(type, structure, verdict);
        }
        return Fallback.of(type, structure, verdict, facts);
    }

    private ArchType port(TypeNode type, TypeStructure structure, Classification verdict) {
        if (verdict.kind() == ArchKind.DRIVEN_PORT) {
            return new DrivenPort(
                    type.id(),
                    structure,
                    verdict,
                    links.roleOf(type.id()),
                    Links.single(links.objects(RelationKind.MANAGES, type.id())).map(Links::reference));
        }
        return new DrivingPort(
                type.id(),
                structure,
                verdict,
                type.methods().stream().map(Assembly::useCase).toList(),
                Links.references(links.namedInPerimeter(type.methods().stream()
                        .flatMap(method -> method.parameters().stream())
                        .map(Parameter::type))),
                Links.references(links.namedInPerimeter(type.methods().stream().map(Method::returnType))));
    }

    /**
     * A method answering with nothing has done something; a method answering with something was
     * asked. Telling the two apart takes no more than the return type, and a method that both
     * changes and answers cannot be told from one that only answers without reading its body.
     */
    private static UseCase useCase(Method method) {
        UseCase.UseCaseType type = "void".equals(method.returnType().qualifiedName())
                ? UseCase.UseCaseType.COMMAND
                : UseCase.UseCaseType.QUERY;
        return new UseCase(method, method.documentation(), type);
    }

    /**
     * An entry point reaches the application through a way in it is handed; a piece of plumbing
     * answers a way out it implements. Which side of the hexagon the adapter is on is therefore
     * also which of the two questions to ask of its declaration.
     */
    private ArchType adapter(TypeNode type, TypeStructure structure, Classification verdict) {
        if (verdict.kind() == ArchKind.DRIVING_ADAPTER) {
            return new DrivingAdapter(
                    type.id(),
                    structure,
                    verdict,
                    Links.references(links.heldBy(type).filter(held -> links.is(held, ArchKind.DRIVING_PORT))));
        }
        return new DrivenAdapter(
                type.id(),
                structure,
                verdict,
                Links.references(links.answeredBy(type).filter(contract -> links.is(contract, ArchKind.DRIVEN_PORT))));
    }

    private static ArchType application(TypeNode type, TypeStructure structure, Classification verdict) {
        return switch (verdict.kind()) {
            case COMMAND_HANDLER -> new CommandHandler(type.id(), structure, verdict);
            case QUERY_HANDLER -> new QueryHandler(type.id(), structure, verdict);
            default -> new ApplicationService(type.id(), structure, verdict);
        };
    }
}
