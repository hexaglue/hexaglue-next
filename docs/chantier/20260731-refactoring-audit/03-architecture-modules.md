# Frontières de modules : documenté vs réel

## Graphe de dépendances réel (lu dans les POMs)

| Module | Dépendances compile réelles |
|---|---|
| `hexaglue-syntax-api` | aucune (conforme) |
| `hexaglue-syntax-spoon` | syntax-api, spoon-core |
| `hexaglue-arch` | syntax-api (+ syntax-spoon en test, non utilisé : les tests utilisent un `StubSyntaxProvider` maison) |
| `hexaglue-spi` | **arch** (la doc dit « plugins dépendent de spi+arch » ; en réalité arch est transitif via spi) |
| `hexaglue-core` | spi, **spoon-core (direct !)**, snakeyaml, slf4j, arch, **syntax-spoon (déclaré, jamais importé)** |
| `hexaglue-maven-plugin` | core, spi, snakeyaml, Maven API |
| `hexaglue-plugin-*` | **spi uniquement** — mais compilent contre `io.hexaglue.arch.*` et `io.hexaglue.syntax.*` (used-undeclared) |

Écarts principaux vs l'architecture documentée dans CLAUDE.md :

- **E1** : core importe Spoon directement dans 14 fichiers
  (`frontend/CachedSpoonAnalyzer`, `frontend/spoon/**`, `GraphBuilder:25`,
  `ProgressiveClassifier:33`) — l'abstraction `hexaglue-syntax` est contournée.
- **E2** : core déclare `hexaglue-syntax-spoon` (`pom.xml:56-59`) sans jamais
  l'importer.
- **E5** : les 4 plugins utilisent arch et syntax-api sans les déclarer ; le POM
  parent des plugins gère leurs versions sans qu'aucun enfant ne les déclare
  (problème connu, contourné).
- **E6** : `hexaglue-arch` contient un moteur de classification complet
  (`arch/builder/**`, 1 794 l., mort) — violation du rôle « modèle ».

Points conformes : aucun import de core dans spi/arch/plugins ; pas de cycle
Maven ; syntax-api a bien zéro dépendance externe.

## CRITICAL

### C1 — Deux abstractions de parsing ; celle publiée est morte
`core.frontend.JavaFrontend` (+ `JavaType`/`JavaField`/`JavaMethod`, impl
`SpoonFrontend` + 7 adapters, 3 394 l.) est le parsing **vivant**
(`DefaultHexaGlueEngine.withDefaults():120-126`).
`syntax.SyntaxProvider` (+ `TypeSyntax`…, impl `SpoonSyntaxProvider`, 3 120 l.)
est **mort** : `SpoonSyntaxProvider.builder()` n'apparaît que dans ses tests.
Le module `hexaglue-syntax-spoon` est publié sur Maven Central et embarqué dans
le classpath de chaque utilisateur sans qu'une ligne n'en soit exécutée.
Seuls 4 types de syntax-api sont vivants, comme DTO : `TypeRef` (100 importeurs),
`Modifier` (28), `SourceLocation`, `TypeForm`.

**Option A (recommandée)** : promouvoir `core.frontend` dans `hexaglue-syntax-api`
(interfaces `Java*`), déplacer `core.frontend.spoon` + `CachedSpoonAnalyzer`
dans `hexaglue-syntax-spoon`, supprimer `SyntaxProvider`/`TypeSyntax`/…,
retirer `spoon-core` du POM de core. L'abstraction publiée redevient vraie.
**Option B** : supprimer syntax-spoon + l'abstraction, déplacer les DTO vivants
dans arch. Moins ambitieux, élimine ~3 000 lignes.

### C2 — L'abstraction vivante est court-circuitée par downcast
`GraphBuilder.calculateComplexity:272-280` et
`ProgressiveClassifier.extractCtMethod:452-457` : `instanceof SpoonMethodAdapter`
→ accès direct au `CtMethod`. Une implémentation JDT/JavaParser produirait
silencieusement une complexité vide, sans erreur.
**Refactoring** : `JavaMethod.cyclomaticComplexity()` / `bodyAnalysis()` sur
l'interface, implémentés dans l'adapter Spoon.

### C3 — Second moteur de classification dans arch, mort et divergent
`io.hexaglue.arch.builder.**` (19 fichiers, 1 794 l.) : aucun import externe.
Divergence installée : le mort supporte `@io.hexaglue.ddd.Entity`
(`ExplicitEntityCriterion:43-44`), le vivant non (`ExplicitEntityCriteria:26`).
Un correctif appliqué au mauvais fichier passe au vert sans effet.
`ClassificationContext` d'arch importe `SyntaxProvider` : le modèle classifie.
**Refactoring** : suppression ; porter d'abord dans core les règles meilleures.

## HIGH

### H1 — Valeurs d'annotations jetées à la frontière core→arch (= B6)
`GraphBuilder.toAnnotationRefs:370-377` conserve `a.values()` ;
`TypeStructureBuilder.mapAnnotation:327-329` appelle `Annotation.of(qualifiedName)`
sans les values. Pour tout ArchType, `annotation.values()` est vide ; la
hiérarchie `arch.model.AnnotationValue` est inatteignable ; aucun plugin ne peut
lire `@Column(name=…)`. Cause racine probable de H2.
**Correctif** : `Annotation.of(ref.qualifiedName(), ref.values())` + test.

### H2 — `plugin-jpa/extraction` : 1 036 lignes écrites contre l'abstraction morte
`JpaAnnotationExtractor` (492 l.) : zéro appelant, opère sur `TypeSyntax` que
rien ne produit. Voir 01-code-mort.md.

### H3 — Pivot classification→modèle en `String` avec 3 switchs divergents (= M3 de 02)
`ClassificationResult.kind` (String) dispatché par : `NewArchitecturalModelBuilder:207-220`
(connaît `EXTERNALIZED_EVENT`, `REPOSITORY`, `GATEWAY`, `NOTIFICATION`…),
`DefaultHexaGlueEngine.toElementKind:444-457` (ne les connaît pas → un
DRIVEN_PORT devient `Optional.empty()` côté `PrimaryClassificationResult`),
`ClassificationTraceConverter.mapKind:83-93` (`valueOf` + catch → UNCLASSIFIED).
**Refactoring** : typer en `ElementKind`/`ArchKind` ; les switchs deviennent
exhaustifs et vérifiés à la compilation.

### H4 — Confiance : 4 enums, 2 chemins de sortie divergents
`ClassificationTraceConverter:100-104` écrase EXPLICIT→HIGH ;
`DefaultHexaGlueEngine:468-473` préserve EXPLICIT (via `CertaintyLevel`).
ValidateMojo peut distinguer explicite/inféré, les plugins non.
Enums concurrentes : `core.classification.ConfidenceLevel`,
`arch.ConfidenceLevel`, `arch.model.ir.ConfidenceLevel`,
`arch.model.classification.CertaintyLevel` (+ mapper local dans living-doc).
**Refactoring** : un seul enum avec EXPLICIT dans arch.

### H5 — `ValidateMojo` : chargeur YAML et rendu de rapport dupliqués et divergents
`ValidateMojo:224-308` duplique `MojoConfigLoader.loadClassificationConfig:65-165` ;
les copies ont déjà divergé sur `allowInferred` et `ValidationConfig`
(`ValidateMojo:175` lit `validationConfig().failOnUnclassified()` sur une config
construite sans `validationConfig(...)`). `ValidateMojo:310-487` (~180 l. de
rendu console/markdown) duplique `ValidationReportGenerator` (380 l., mort).
**Refactoring** : déléguer à `MojoConfigLoader` + un générateur de rapport
unique dans core. Le Mojo passe de 509 à ~150 lignes.

## MEDIUM

### M1 — 5 représentations vivantes d'un « type Java », 3 `TypeRef`
Chaîne réelle : `CtType` → `JavaType` (frontend) → `TypeNode` (graphe) →
`TypeStructure`/`ArchType` (modèle) → `CodeUnit` (audit). `JavaType`→`TypeNode`
est une recopie au même niveau d'abstraction (seul ajout : `NodeId`) ;
`CodeUnit` est une projection appauvrie recalculée par le plugin audit
(9 mappers copiés-collés, métriques en dur à 0/100.0).
3 `TypeRef` en production (`core.frontend`, `syntax`, `arch.model.ir`) forcent
des FQN inline dans les plugins (`DerivedMethodSpec:290`, `AdapterMethodSpec:330`…).
`TypeStructureBuilder.mapTypeRef:294-321` recalcule `isPrimitive` par switch de
chaînes alors que Spoon connaît l'information.
**Refactoring** : fusionner JavaType/TypeNode ; unifier les 3 TypeRef (celui de
syntax, le plus riche) ; supprimer `arch.model.core.TypeInfo` (mort).

### M2 — Vestiges v4 dans arch (adapters/, ports/, doublons racine)
Voir 01-code-mort.md : `DrivenAdapter` (champ `TypeSyntax`, `ElementKind.DRIVEN_ADAPTER`
contredit `ArchKind`), `PortClassification` (doublon de `DrivenPortType` +
2 `PortKind`), `UnclassifiedType` ×2 dans le même module (les deux en prod),
`RelationType` ×2, `Cardinality` ×2.

### M3 — `ArchitecturalModel.relationships` toujours vide
`addRelation`/`addManages`/`addImplements` : aucun appelant. Les vraies
relations passent par `RelationshipGraph`, dont seul le dérivé
`CompositionIndex` est attaché au modèle — le graphe est jeté.
**Refactoring** : supprimer `RelationshipStore` du record, ou attacher
`RelationshipGraph`.

### M4 — Mojos : pas de classe de base, 6 copies de `formatDiagnostic`
6 Mojos étendent `AbstractMojo` sans parent commun : `formatDiagnostic` ×6,
parsing `maven.compiler.release` ×5 (fallback `"21"` alors que le projet cible
Java 17), boucle de log des diagnostics ×5.
**Refactoring** : `AbstractHexaGlueMojo` + `EngineConfigFactory`.

### M5 — `HexaGlueLifecycleParticipant` : versions en dur, bloc delombok dupliqué
`MAPSTRUCT_VERSION="1.6.3"`, `EXEC_PLUGIN_VERSION="3.6.3"`,
`DEPENDENCY_PLUGIN_VERSION="3.10.0"` non surchargeables ; bloc delombok
copié-collé (`:365-384` ≡ `:439-457`).
**Refactoring** : `@Parameter(property=…)` + extraction
`buildDelombokExecution(...)`.

## LOW

- `NewArchitecturalModelBuilder` : préfixe `New` vestige de migration ; CLAUDE.md
  documente `ArchitecturalModelBuilder`. Renommer.
- `<doclint>none</doclint>` masque des `@link` cassés sur l'API publiée
  (`HexaGluePlugin:101` → `PluginContext#ir()` supprimée en 4.0.0).
- SpotBugs en `failOnError=false` : rapports générés, jamais bloquants.
- `DefaultHexaGlueEngine.deriveStrategy:479-502` : stratégie inférée par
  `contains()` sur le **nom du critère** — un renommage de classe change le
  rapport de validation. → `ClassificationCriteria.strategy()` déclaré.
- `JavaType.identityField()/hasIdField()` : heuristique métier dans la couche
  frontend, sans appelant externe.

## Architecture cible

```
hexaglue-syntax-api    ZÉRO dépendance externe
  JavaFrontend, JavaType/Field/Method/…, TypeRef (unique), Modifier,
  SourceLocation, TypeForm          (= core.frontend promu ; SyntaxProvider supprimé)
        ▲
hexaglue-syntax-spoon  SEULE dépendance à Spoon du reactor
  SpoonFrontend, Spoon*Adapter, CachedSpoonAnalyzer
        ▲ (implémentation runtime, jamais importée par core)
hexaglue-arch          MODÈLE PUR — ni classification ni parsing
  ArchType scellé, TypeStructure, TypeId, ArchKind (unique), Confidence (unique),
  ClassificationTrace, Evidence, indexes, RelationshipGraph, ClassificationReport
        ▲
hexaglue-spi           CONTRAT PLUGIN (arch + syntax-api)
  HexaGluePlugin, PluginContext, CodeWriter, PluginConfig, ArchitectureQuery (6 méthodes)
        ▲
hexaglue-core          MOTEUR (syntax-api, PAS spoon)
  graph/ classification/ builder/ engine/ plugin/ query/
        ▲
hexaglue-maven-plugin  ADAPTATEUR MAVEN, zéro logique métier
hexaglue-plugins/*     déclarent spi + arch explicitement
```

Invariants à outiller : enforcer `bannedDependencies` (spoon hors syntax-spoon),
ArchUnit (`arch` sans `*Classifier`/`*Criterion`), `dependency:analyze-only`
`failOnWarning`, doclint `reference` sur spi/arch.

Séquencement : (1) suppressions pures (01-code-mort) ; (2) correctifs H1/H4/H5 ;
(3) typage H3 ; (4) unification du parsing C1/C2 en dernier — `arch/builder`
(seul consommateur de `SyntaxProvider`) doit disparaître avant.
