# Inventaire consolidé du code mort

Méthode : remontée du pipeline de production depuis les 6 Mojos
(`HexaGlueMojo`, `AuditMojo`, `ValidateMojo`, `GenerateAndAuditMojo`,
`ReactorGenerateMojo`, `ReactorAuditMojo`) → `HexaGlueEngine.create()` →
`DefaultHexaGlueEngine` → `SinglePassClassifier` + `SpoonFrontend` (core/frontend)
+ 4 plugins via `ServiceLoader` (seuls `META-INF/services` du dépôt :
`io.hexaglue.spi.plugin.HexaGluePlugin` × 4). Croisement grep des instanciations
et imports sur tout `src/main`. « Mort » = référencé uniquement par ses propres
tests, son package-info ou lui-même.

Le pipeline vivant réel :

```
Mojos → DefaultHexaGlueEngine
          ├─ CachedSpoonAnalyzer + SpoonFrontend (core/frontend/spoon)   [parsing]
          ├─ GraphBuilder → ApplicationGraph + GraphQuery                [graphe]
          ├─ SinglePassClassifier (+ 31 criteria core/classification)    [classification]
          ├─ NewArchitecturalModelBuilder → ArchitecturalModel           [modèle]
          └─ PluginExecutor → JpaPlugin | RestPlugin | LivingDocPlugin | DddAuditPlugin
```

Tout ce qui suit est hors de ce chemin.

## Confiance HAUTE — suppression sans risque fonctionnel

### hexaglue-core (~9 200 lignes)

| Bloc | LOC | Preuve |
|---|---:|---|
| `classification/ProgressiveClassifier` | 649 | Seuls appelants : tests + `PerformanceRegressionTest`. Le moteur n'instancie que `SinglePassClassifier` (`DefaultHexaGlueEngine.java:272`) |
| `classification/secondary/` (WeightedMultiSignalClassifier, SecondaryClassifierExecutor) | 567 | Aucun `ServiceLoader.load(HexaglueClassifier)` dans le dépôt ; aucune inscription `META-INF/services` |
| `enrichment/` (EnrichmentEngine, BehavioralPatternEnricher, EnrichedSnapshot…) | 729 | `EnrichmentEngine` référencé uniquement par package-info + tests ; aucune implémentation d'`EnrichmentPlugin` n'existe |
| `analysis/` (RelationAnalyzer, CascadeInference, MappedByDetector, AnalysisBudget, PublicApiPrioritizer) | 1 484 | RelationAnalyzer : tests seulement. AnalysisBudget/PublicApiPrioritizer : consommés uniquement par ProgressiveClassifier (mort). Sémantique JPA (CascadeType, FetchType) dans core alors que le plugin JPA a son propre modèle de relations |
| `audit/rules/` (12-13 AuditRule, dont Naming*SuffixRule, Layering*, Complexity*, Dependency*) | 1 875 | Aucun `META-INF/services/io.hexaglue.spi.audit.AuditRule` (core n'a pas de `src/main/resources`) ; une seule règle a un test. Contient le bug `String.formatted` ×14 |
| `audit/report/AuditReportGenerator` | 441 | Test uniquement. Doublon du rendu du plugin audit ; JSON échappé à la main incomplet |
| `audit/metrics/` (MetricsCalculator + 5 records ; Lakos/Coupling gardés s'ils servent DefaultArchitectureQuery) | 735 | `MetricsCalculator` : test uniquement. `LakosMetricsCalculator.collectTransitiveDependencies` diverge de la version vivante (suit les arêtes DECLARES → CCD gonflé) |
| `audit/Layer` + `audit/LayerClassifier` | 289 | `Layer` : zéro référence, y compris tests (seule occurrence : sa propre Javadoc). 3e implémentation de la classification en couches |
| `style/` (StyleDetector, ArchitectureStyle, DetectedStyle) | 577 | Doublon mort de `graph/style/StyleDetector` (vivant, utilisé par `GraphBuilder:64`). Deux classes `StyleDetector` dans le même module |
| `graph/model/edges/` (TypedEdge, DependencyEdge, InheritanceEdge, ImplementsEdge, MethodCallEdge, FieldAccessEdge) | 1 051 | Consommés seulement par 4 méthodes `GraphQuery` qu'aucun plugin n'appelle ; les conversions fabriquent des données fausses (`invocationCount` toujours 1, parsing de `EdgeProof.via()` par `contains("static=true")`) |
| `classification/report/ValidationReportGenerator` | 380 | `ValidateMojo` écrit son propre markdown (509 l.) au lieu de l'utiliser |
| `frontend/TypeNameResolver` | ~40 | Zéro référence |

À déplacer (pas supprimer) : `graph/testing/TestGraphBuilder` (418 l.) → `src/test`
ou artefact test-jar. Javadoc dit « for tests », utilisé par 4 classes de test,
publié aujourd'hui dans le JAR Maven Central ; son `parseTypeRef` ne gère qu'un
argument générique.

### hexaglue-arch (~2 600 lignes)

| Bloc | LOC | Preuve |
|---|---:|---|
| `builder/` (DomainClassifier, PortClassifier, ClassificationContext, 12 `*Criterion`) | 1 794 | Zéro import hors de hexaglue-arch. 2e hiérarchie de critères complète, en violation de la règle « le modèle ne classifie jamais » (importe `SyntaxProvider`). Divergence installée : le mort supporte `@io.hexaglue.ddd.Entity`, le vivant non |
| `adapters/` (DrivenAdapter, DrivingAdapter, AdapterType) + `ports/PortClassification` | ~470 | Zéro import externe. `DrivenAdapter` porte un champ `TypeSyntax` (abstraction morte) et déclare `ElementKind.DRIVEN_ADAPTER` alors que `ArchKind` exclut explicitement les adaptateurs |
| Vestiges v4 racine : `UnclassifiedType` (doublon de `model/UnclassifiedType` — les deux en prod !), `RelationType` (doublon de `model/graph/RelationType`), `Cardinality` (doublon de `model/ir/Cardinality`), `MemberId`, `DuplicateElementException` | ~350 | Zéro import externe pour chacun |
| `model/core/TypeInfo` + `TypeKind` | ~150 | Servent uniquement le SPI classification mort |
| `model/query/ModelQuery` | ~100 | Zéro référence |
| `ArchitecturalModel.relationships` (RelationshipStore) | — | Structurellement toujours vide : `addRelation`/`addManages`/`addImplements` n'ont aucun appelant en `src/main`. À supprimer du record ou à alimenter |

### hexaglue-syntax (~2 800 lignes)

| Bloc | LOC | Preuve |
|---|---:|---|
| Module `hexaglue-syntax-spoon` entier | 1 174 | Zéro import de `io.hexaglue.syntax.spoon` hors du module. Le parsing de prod est `core/frontend/spoon/SpoonFrontend`. Déclaré en dépendance compile de core (`hexaglue-core/pom.xml:56-59`) sans aucun import correspondant |
| Abstraction de parsing de `syntax-api` : `SyntaxProvider`, `TypeSyntax`, `FieldSyntax`, `MethodSyntax`, `ConstructorSyntax`, `AnnotationSyntax`, `ParameterSyntax`, `MethodBodySyntax`, `TypeParameterSyntax`, `SyntaxMetadata`, `SyntaxCapabilities`, `UnsupportedCapabilityException`, `AnnotationValue` | ~1 600 | Seul consommateur : `arch/builder` (mort). Aucun code de prod ne construit de `TypeSyntax` (les factories passent `null`). Restent VIVANTS : `TypeRef` (100 importeurs), `Modifier` (28), `SourceLocation`, `TypeForm` — le rôle réel du module est « types partagés », pas « abstraction de parseur » |

### hexaglue-spi (~1 300 lignes)

| Bloc | LOC | Preuve |
|---|---:|---|
| `classification/` (HexaglueClassifier, ClassificationContext, SecondaryClassificationResult, ClassificationException) | 466 | SPI sans aucun chargeur ni implémentation. Conserver `PrimaryClassificationResult` (consommé par ValidateMojo) |
| `enrichment/` (EnrichmentPlugin…) | 531 | Aucune implémentation dans le dépôt |
| `core/ResolutionConfig` | 302 | Zéro référence main. **Décision requise** : SPI publié → dépréciation avant retrait ? |
| `generation/ArtifactWriter` | ~130 | Redélègue 13 méthodes vers `CodeWriter` sans rien ajouter (sa Javadoc l'admet : « interchangeable terms ») |

### hexaglue-plugins (~5 000 lignes)

| Bloc | LOC | Preuve |
|---|---:|---|
| plugin-audit — sous-système recommandations/dette/CI/historique/inventaire/zones : `ExecutiveSummaryBuilder` (419), `CIIntegrationGenerator` (404), `RecommendationEngine` (341), `ArchitectureAnalysis` (317), `ComponentInventory` (280), `AuditHistoryComparator` (260), `RecommendationGenerator` (254), `Recommendation` (216), `InventoryBuilder` (207), `ZoneCategory` (170), `DebtEstimator` (157), `ZoneAnalyzer` (154), `PackageZoneMetrics` (154), `ExecutiveSummary` (150), `AuditComparison` (133), `CIPlatform` (98), `CIConfiguration` (87), `DebtEstimation` (81), `RecommendationPriority` (56), `AuditTrend` (49) | ~3 990 | Aucun de ces fichiers n'est atteignable depuis `DddAuditPlugin.execute` → `executeDomainAudit` → `ReportDataBuilder` → renderers. Ironique : `ZoneAnalyzer` documente et applique parfaitement la règle d'or — c'est le meilleur code du plugin, et il est mort |
| plugin-audit — `DddAuditPlugin.audit(AuditContext)` | ~40 | Méthode contractuelle SPI jamais appelée, corps dupliqué à 90 % de `executeDomainAudit`. Le SPI `AuditPlugin` déclare `audit()` abstraite puis interdit le chemin qui l'appelle (`execute()` par défaut lève UnsupportedOperationException) → corriger le SPI |
| plugin-jpa — `extraction/` (JpaAnnotationExtractor 492, IdentityInfo 159, RelationInfo 191, PropertyInfo 194) | 1 036 | `JpaAnnotationExtractor` : zéro appelant (ni main ni test) ; opère sur `TypeSyntax` que rien ne produit. Si la lecture de `@Id`/`@Column` est souhaitée, réécrire sur `arch.model.Field.annotations()` après correction de B6 |
| living-doc — `mermaid/ClassDiagramBuilder` + `ClassBuilder` (DSL) | ~350 | La DSL testée est morte ; le rendu réel (`DiagramRenderer`, 625 l.) est en StringBuilder brut et n'utilise que `MermaidBuilder.sanitizeId`. À inverser : migrer le rendu sur la DSL, pas supprimer la DSL |

### API morte dans du code vivant (méthodes à retirer)

- `TypeNode.hasGatewaySuffix/hasUseCaseSuffix/hasEventSuffix` ; `FieldNode.hasCommonIdType` ;
  `JavaField.looksLikeIdentity/hasCommonIdType` ; `GraphQuery.repositories()/identifiers()`
  (aucun appelant `src/main` une fois ProgressiveClassifier supprimé).
- `AnchorDetector.isDrivingPackage`, `PortKindClassifier.isCrudMethod/isEventPublishMethod`
  (publics, jamais appelés).
- `DefaultArchitectureQuery` : 16 des 22 méthodes SPI sans consommateur
  (voir 04-core-engine-audit.md).
- `Evidence.fromPackage` : aucun appelant (aucune évidence PACKAGE émise).
- `GraphIndexes.isUsedInRepositorySignature` (test uniquement, heuristique
  `contains("Repository")` sur NodeId).
- `DiagramGenerator.generateAggregateDiagrams` (retourne toujours `List.of()`).

## Confiance MOYENNE — décision utilisateur requise

| Bloc | LOC | Question |
|---|---:|---|
| Module `hexaglue-testing` (HexaGlueTestHarness) | 295 | Jamais consommé dans le dépôt (hors build/coverage/pom.xml). Est-ce une API de test publiée pour les utilisateurs finaux ? |
| `spi/core/ResolutionConfig` | 302 | SPI publié sur Maven Central : retrait direct ou cycle de dépréciation ? |
| `syntax-api` (module) | — | Ne pas supprimer le module : `TypeRef`/`Modifier`/`SourceLocation` sont pleinement vivants. Supprimer seulement l'abstraction de parsing listée ci-dessus, ou la remplacer par la promotion de `core.frontend` (voir 03-architecture-modules.md) |

## Total

≈ 20 900 lignes `src/main` (~13 % du source de production), plus les tests
associés (plusieurs milliers de lignes) qui maintiennent ce code au vert.

Impact de la purge : élimine mécaniquement ~30 règles de nommage mortes, la 2e
hiérarchie de critères, le 2e moteur d'audit, la 2e abstraction de parsing,
1 des 2 `EvidenceType`, 1 des 4 enums de confiance, les 3 fuites de collections
mutables recensées (toutes dans `arch/builder`), et le site du bug B14.

## Ordre d'exécution

1. Blocs sans aucune dépendance entrante : plugins (audit dead subsystem,
   jpa/extraction), core (enrichment, style, secondary, analysis, edges,
   ValidationReportGenerator), spi (enrichment, classification sauf
   PrimaryClassificationResult), arch (adapters, ports, vestiges v4, model/core).
2. `ProgressiveClassifier` (libère AnalysisBudget/PublicApiPrioritizer et les
   méthodes de nommage de TypeNode/FieldNode).
3. `core/audit/{rules,report,metrics partiels,Layer,LayerClassifier}`.
4. `arch/builder` (libère `SyntaxProvider`) puis abstraction `syntax-api` +
   module `syntax-spoon` (à coordonner avec la phase 4 du README).
5. Après chaque lot : `make test` + `make integration` (examples/) —
   la compilation et les tests d'intégration sont le filet de sécurité.
