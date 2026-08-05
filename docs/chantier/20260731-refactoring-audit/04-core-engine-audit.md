# hexaglue-core hors classification : engine, graph, audit SPI, plugin executor

Le code mort de ce périmètre (audit/rules, audit/report, audit/metrics partiel,
Layer/LayerClassifier, style/, analysis/, graph/model/edges, graph/testing) est
inventorié dans 01-code-mort.md. Ici : les défauts du code vivant.

## CRITICAL

### C1 — Principe des dépendances stables inversé (= B1)
`audit/DefaultArchitectureQuery.java:650-672` : `calculateStability` retourne en
réalité l'**instabilité** I = Ce/(Ca+Ce), et la condition de violation est
`fromStability > toStability` — l'inverse du SDP. La règle morte
`DependencyStableRule:138-141` implémente la condition correcte.
Impact : évaluée par arête REFERENCES, elle génère des centaines de fausses
violations ; `HealthScoreCalculator:130-138` (plugin audit) fait tomber le score
à 0. Les rapports du case study e-commerce sont faussés sur cet axe.
**Correctif** : renommer en `calculateInstability`, inverser la condition,
corriger `StabilityViolation.stabilityDelta()` et la Javadoc SPI, test
A(I=0)→B(I=1) = violation / inverse = OK, agréger par paire de packages.

### C2 — `String.formatted` sur le mauvais littéral ×14 (= B14)
Motif `"litéral A avec %s" + "litéral B".formatted(args)` : la précédence fait
que `.formatted` ne s'applique qu'au second littéral. Occurrences dans
`audit/rules/` : NamingRepositorySuffixRule:78-82, NamingControllerSuffixRule:83-87,
NamingDtoSuffixRule:76-80/92-96, LayeringPresentationNoDomainRule (3),
LayeringApplicationNoPresentationRule (3), ComplexityCyclomaticMaxRule:96-99,
ComplexityMethodLengthMaxRule:106-109, DocumentationComplexMethodsRule:78-81,
DocumentationPublicApiRule (2). Code mort aujourd'hui — mais toute réactivation
livre des messages corrompus. Interdire le motif par règle PMD.

### C3 — NPE latent dans le tri topologique des plugins (= B7)
`plugin/PluginExecutor.java:276-285` : `outEdges.get(depId)` est null si le
plugin B (dépendant de A) est itéré avant A — l'initialisation des maps se fait
au fil de l'itération, dont l'ordre vient du `ServiceLoader` (classpath), donc
non déterministe. **Correctif** : deux passes (init puis arêtes) + test avec
deux plugins déclarés dans l'ordre inverse.

## HIGH

### H1 — `DefaultArchitectureQuery` : god class 912 l., 16 des 22 méthodes SPI mortes
Consommées par les plugins : `findDependencyCycles`, `analyzeAllPackageCoupling`,
`allTypeDependencies`, `findBoundedContexts`, `findImplementors`,
`findStabilityViolations`. Les 16 autres (Lakos ×3, `findAggregates`,
`findLayerViolations`, `findPortDirection`…) : aucun appelant.
**Refactoring** : réduire le SPI aux 6 méthodes (ISP) ; éclater en
`CycleDetector`, `PackageCouplingAnalyzer`, `LakosMetricsProvider`,
`BoundedContextResolver`, `ImplementorResolver`.

### H2 — Le SPI d'audit re-classifie par nommage, contre la règle d'or
`DefaultArchitectureQuery` opère sur le graphe syntaxique brut alors que
`ArchitecturalModel` est construit juste avant :
- `:536-552` agrégat = « référencé par un type dont le nom finit par Repository » ;
- `:621-630` `inferLayer` par `pkg.contains("domain")` etc. — 3e implémentation
  de la couche, en `String` magiques ;
- `:572-587` value object = « c'est un record » (commentaire d'aveu) ;
- `:845-858` repository d'un agrégat par `startsWith||contains` + `findFirst()`
  non déterministe (`OrderLineRepository` peut gagner sur `OrderRepository`).
Conséquence : un projet dont les repositories ne suivent pas le suffixe n'a
aucun agrégat détecté par l'audit, alors que la classification les connaît.
**Refactoring** : construire avec `(graph, architecturalModel)` ; réimplémenter
sur DomainIndex/PortIndex/TypeRegistry ; le graphe ne sert plus qu'aux métriques
structurelles.

### H3 — `findPortDirection` retourne toujours `Optional.empty()`
`DefaultArchitectureQuery:42-53` : l'unique constructeur fige
`portDirections = Map.of()`. La Javadoc SPI promet « rather than inferring it
from naming ». Contrat mort-né : supprimer ou alimenter depuis `portIndex()`.

### H4 — Détection de cycles exponentielle et dupliquée (= B10)
`findCyclesHelper:96-110` : `visited` alimenté mais jamais consulté ; pas de
déduplication ; même défaut dans les variantes package et bounded context. La
version correcte (élagage + dédup) est dans le code mort
(`DependencyNoCyclesRule:179-191`). `findDependencyCycles` est appelé 2× par
audit et chaque doublon pénalise le score.
**Refactoring** : un `CycleDetector<T>` unique (ou Tarjan, linéaire et sans
doublon).

### H5 — Filtre inopérant `simpleName().startsWith("java.")`
`DefaultArchitectureQuery:562-569` : un simpleName ne contient jamais de point,
le filtre ne filtre rien. Corriger en `qualifiedName()` ou supprimer (H2).

### H6 — `DefaultHexaGlueEngine.analyze()` : 140 l., échec silencieux
Deux « Step 5 » et un « Step 4.5 » après un « Step 5 » ; Javadoc décrivant une
étape IrExporter supprimée en 4.0.0 ; `catch (Exception)` global qui retourne un
**modèle vide valide** (un échec Spoon devient « projet sans types », 0 fichier
généré, build vert) ; `EngineResult.pluginResult` est un null explicite.
**Refactoring** : extraire une méthode nommée par étape ; exceptions typées par
étape (Frontend/Classification/PluginExecution) ; `EngineResult.failed()`
explicite que les Mojos doivent tester ; `Optional<PluginExecutionResult>`.

### H7 — `PluginExecutor` : pas d'isolation des `Error`
`discoverPlugins:236-246` : l'itération `ServiceLoader` lève
`ServiceConfigurationError` (un Error) — un seul plugin mal packagé casse la
découverte de tous. `executePlugin:336-371` : `catch (Exception)` laisse passer
`NoClassDefFoundError` (cas réel : dépendance optionnelle absente, ex. springdoc
pour REST) sans le diagnostic « Plugin X failed ».
**Refactoring** : itérer via `loader.stream()` avec catch par `Provider` ;
`catch (Exception | LinkageError)` dans executePlugin.

### H8 — Complexités quadratiques dans les chemins chauds
- `ApplicationGraph.containsEdge:212-214` : scan linéaire des arêtes, appelé
  par `DerivedEdgeComputer` pour chaque arête candidate → O(E²) à chaque
  compilation. → index `Set<EdgeSignature>` ou `edgesFrom(from)`.
- `DefaultArchitectureQuery` : `findAggregates()` recalculé à chaque appel par
  3 méthodes ; Lakos en O(V×(V+E)) sans mémoïsation. → caches lazy (la classe
  est construite sur un graphe figé).

## MEDIUM

- **M1** `PluginExecutor.extractGeneratedFiles:455-462` : dispatch `instanceof`
  sur les implémentations de `CodeWriter` — toute nouvelle implémentation
  retourne silencieusement `List.of()`. → interface `GeneratedFileTracker`.
- **M2** `PluginExecutor` : 3 constructeurs télescopiques (5/6/7 params), seuls
  1-2 params validés, pas de copie défensive. → record `PluginExecutionContext`
  avec constructeur compact validant.
- **M3** `resolvePluginOutputOverride:408-414` retourne `null` (règle projet :
  Optional). Propagé jusqu'à `MultiModuleCodeWriter`.
- **M4** `reportsDirectory` (ajouté en 6.0.0 pour fiabiliser l'emplacement des
  rapports) ignoré en mode multi-module : `MultiModuleCodeWriter:82` reconstruit
  un `FileSystemCodeWriter` 1-arg qui re-dérive par `getParent().getParent()` —
  le comportement que le champ visait à éliminer.
- **M5** `FileSystemCodeWriter` : constructeur 1-arg à NPE
  (`outputDirectory.getParent().getParent()` sans garde alors que
  `PluginExecutor.deriveReportsDirectory` fait le même calcul avec gardes) ;
  triplets write/exists/delete dupliqués ×3 ; **aucune validation de chemin** :
  un plugin peut écrire hors de `target/` via `../../..` → `safeResolve` avec
  normalize + confinement.
- **M6** `ApplicationGraph` : `setMetadata` public « internal use only » (un
  commentaire ne remplace pas un modificateur), `indexes()` expose
  `indexNode`/`indexEdge` mutables (l'invariant G-1 peut être corrompu),
  `addNode`/`addEdge` publics malgré l'invariant « append-only via GraphBuilder ».
  → builder + vue lecture seule `GraphIndexView`.
- **M7** `EngineConfig` : record à 15 composants ; 4 witheurs et 3 factories
  réénumèrent les 15 arguments positionnels (deux Path adjacents, deux boolean
  adjacents : une inversion compile) ; le constructeur compact fait des I/O
  disque (`Files.exists`) → non instanciable en test pur. → Builder + validation
  explicite déplacée.
- **M8** `deriveStrategy:479-502` : stratégie de classification inférée par
  `contains()` sur le nom de classe du critère ; `toElementKind:439-459` switch
  sur String avec `default -> null`. → porter la stratégie sur l'interface
  criteria, kinds typés.
- **M9** Bounded contexts : segment 3 en dur (= B8), sans utiliser
  `graph.metadata().basePackage()` disponible. → calcul relatif au basePackage,
  stratégie configurable (package / module Maven via ModuleIndex / annotation).

## LOW

- Imports wildcard généralisés (`DefaultArchitectureQuery:31`,
  `ApplicationGraph`, `GraphIndexes` avec import redondant, `GraphBuilder`…) —
  risqué avec deux classes homonymes `StyleDetector` → Checkstyle
  `AvoidStarImport`.
- FQN inline massifs dans `DefaultHexaGlueEngine` (`io.hexaglue.arch.ElementKind.X`
  ×11 dans `toElementKind`).
- `PluginCyclicDependencyException` sans les identifiants des plugins impliqués.
- `Collectors.toMap` sans merge sur les ids de plugins → `IllegalStateException:
  Duplicate key` brute si deux versions du même artefact sur le classpath.
- `buildModuleIndex` ignore silencieusement les types sans SourceRef ou hors
  module (aucun log) — diagnostic multi-module difficile.
- MojoConfigLoader:222-228/303-309 : `hexaglue.yaml` malformé → simple warn puis
  `Map.of()` — le build continue avec une config vide. À transformer en erreur
  ou warning bloquant selon un flag strict.
