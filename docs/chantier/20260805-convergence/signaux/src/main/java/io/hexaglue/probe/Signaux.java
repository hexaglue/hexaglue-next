package io.hexaglue.probe;

import io.hexaglue.engine.Analysis;
import io.hexaglue.engine.AnalysisResult;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.frontend.FrontendRequest;
import io.hexaglue.frontend.FrontendResult;
import io.hexaglue.frontend.SpoonFrontend;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.code.CodeModelCapability;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.HexaGlueConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Affiche les signaux que le moteur retient sur un ou plusieurs types.
 *
 * <p>Le rapport d'audit ne publie que le verdict ; les évidences qui l'ont porté ne sortent
 * nulle part. Ce harnais les affiche, pour que la pondération d'une décision se lise sur des
 * signaux mesurés plutôt que sur un récit.</p>
 *
 * <p>Usage :
 * {@code Signaux <racine-de-sources> <basePackage> <exclusions,séparées,par,virgule> <type...>}</p>
 */
public final class Signaux {

    private Signaux() {}

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("usage: Signaux <sourceRoot> <basePackage> <exclusions|-> <type...>");
            System.exit(2);
        }
        List<Path> sourceRoots = Arrays.stream(args[0].split(":"))
                .filter(root -> !root.isBlank())
                .map(Path::of)
                .toList();
        String basePackage = args[1];
        List<String> exclusions = "-".equals(args[2]) ? List.of() : Arrays.asList(args[2].split(","));
        List<String> subjects = Arrays.asList(args).subList(3, args.length);

        AnalysisScope scope = new AnalysisScope(Optional.of(basePackage), List.of(), exclusions);
        FrontendRequest.Builder builder =
                FrontendRequest.builder().scope(scope).capability(CodeModelCapability.METHOD_BODIES);
        sourceRoots.forEach(builder::sourceRoot);
        List<Path> classpath = classpath();
        classpath.forEach(builder::classpathEntry);
        FrontendRequest request = builder.build();

        FrontendResult read = SpoonFrontend.analyze(request);
        HexaGlueConfig config = new HexaGlueConfig(
                scope,
                HexaGlueConfig.defaults().classification(),
                HexaGlueConfig.defaults().validation(),
                HexaGlueConfig.defaults().generation(),
                HexaGlueConfig.defaults().modules());
        AnalysisResult analysis =
                Analysis.analyze(EngineContext.of(read.code(), KnowledgePacks.embedded(), config));

        System.out.println("racines     : " + sourceRoots.size() + " — " + sourceRoots);
        System.out.println("classpath   : "
                + (classpath.isEmpty() ? "(AUCUN — les supertypes externes ne se ferment pas)" : classpath.size() + " entrées"));
        System.out.println("basePackage : " + basePackage);
        System.out.println("exclusions  : " + (exclusions.isEmpty() ? "(aucune)" : String.join(", ", exclusions)));
        System.out.println("fichiers .java sous les racines : " + sources(sourceRoots));
        System.out.println("types dans le CodeModel        : "
                + read.code().types().stream().filter(node -> !node.external()).count());
        System.out.println("types dans l'ArchModel         : " + analysis.model().types().size());
        report("diagnostics du frontend", read.diagnostics());
        report("diagnostics du moteur", analysis.diagnostics());
        if (subjects.contains("*")) {
            analysis.model().types().stream()
                    .map(type -> type.id().qualifiedName())
                    .sorted()
                    .forEach(name -> report(analysis, name));
            wrappers(analysis);
        } else {
            for (String subject : subjects) {
                report(analysis, subject);
            }
        }
    }

    /**
     * Isole la question ouverte par la mesure sur le banc e-commerce : un type immuable
     * enveloppant une seule valeur est exclu des parties d'un agrégat, donc invisible à la règle
     * qui lit les valeurs possédées. On liste ces types et ce que le moteur en a fait.
     */
    private static void wrappers(AnalysisResult analysis) {
        System.out.println();
        System.out.println("== enveloppes à une valeur (candidates à l'invisibilité de composition) ==");
        boolean any = false;
        for (ArchType type : analysis.model().types()) {
            List<Evidence> owned = type.classification().evidences().stream()
                    .filter(evidence -> evidence.fact().startsWith("OWNED_BY("))
                    .toList();
            boolean wrapper = type.classification().evidences().stream()
                    .anyMatch(evidence -> evidence.fact().startsWith("SINGLE_VALUE_WRAPPER("))
                    || type.classification().candidates().stream()
                            .flatMap(candidate -> candidate.evidences().stream())
                            .anyMatch(evidence -> evidence.fact().startsWith("SINGLE_VALUE_WRAPPER("));
            if (wrapper) {
                any = true;
                System.out.println("  " + type.id().simpleName() + " → " + type.classification().kind()
                        + "   signaux OWNED_BY : " + owned.size());
            }
        }
        if (!any) {
            System.out.println("  aucune");
        }
    }

    /**
     * Le classpath décide de la fermeture transitive des supertypes : sans lui,
     * {@code extends JpaRepository} ne se relie pas à {@code Repository} et la connaissance des
     * frameworks reste muette. La production le passe toujours ; une mesure qui l'omet ne mesure
     * pas le même moteur.
     *
     * @return les entrées déclarées par {@code -Dsignaux.classpath}, séparées par {@code :}
     */
    /**
     * Le dénominateur d'une mesure de couverture : ce que le build contient, et non ce que le
     * modèle a bien voulu retenir. L'écart entre les trois compteurs est le registre de passage.
     */
    private static long sources(List<Path> roots) {
        long total = 0;
        for (Path root : roots) {
            try (var found = java.nio.file.Files.walk(root)) {
                total += found.filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                        .count();
            } catch (java.io.IOException ignored) {
                System.out.println("  racine illisible : " + root);
            }
        }
        return total;
    }

    private static void report(String title, List<io.hexaglue.model.finding.Diagnostic> diagnostics) {
        System.out.println(title + " : " + diagnostics.size());
        diagnostics.forEach(diagnostic ->
                System.out.println("  [" + diagnostic.severity() + "] " + diagnostic.code() + " "
                        + diagnostic.message()));
    }

    private static List<Path> classpath() {
        String declared = System.getProperty("signaux.classpath", "");
        if (declared.isBlank()) {
            return List.of();
        }
        List<Path> entries = new ArrayList<>();
        for (String entry : declared.split(":")) {
            if (!entry.isBlank()) {
                entries.add(Path.of(entry.trim()));
            }
        }
        return entries;
    }

    private static void report(AnalysisResult analysis, String qualifiedName) {
        System.out.println();
        System.out.println("== " + qualifiedName + " ==");
        Optional<ArchType> type = analysis.model().type(TypeId.of(qualifiedName));
        if (type.isEmpty()) {
            System.out.println("  absent du modèle");
            return;
        }
        Classification verdict = type.get().classification();
        System.out.println("  verdict    : " + verdict.kind() + " (" + verdict.confidence() + ", " + verdict.basis()
                + ")");
        System.out.println("  signaux retenus : " + verdict.evidences().size());
        for (Evidence evidence : verdict.evidences()) {
            System.out.println("    " + line(evidence));
        }
        for (Candidate candidate : verdict.candidates()) {
            System.out.println("  candidat " + candidate.kind() + " (score " + candidate.score() + ", "
                    + candidate.evidences().size() + " signaux)");
            for (Evidence evidence : candidate.evidences()) {
                System.out.println("    " + line(evidence));
            }
        }
    }

    /**
     * Une ligne par signal : le palier, la force, le jeton qui sert de clé de déduplication, et
     * les types sur lesquels le signal s'appuie — c'est-à-dire l'ancre que la clé de corrélation
     * proposée utiliserait.
     */
    private static String line(Evidence evidence) {
        List<String> anchors = new ArrayList<>();
        evidence.relatedTypes().forEach(related -> anchors.add(related.simpleName()));
        return "[" + evidence.tier().code() + "/" + evidence.force() + "] " + evidence.fact()
                + "   ancre=" + (anchors.isEmpty() ? "(aucune)" : String.join("+", anchors));
    }
}
