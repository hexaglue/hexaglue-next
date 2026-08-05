# Audit de refactoring HexaGlue — Synthèse et plan

Date : 2026-07-31
Périmètre : `hexaglue/` (tous modules, `src/main` + POMs), ~157 000 lignes Java.
Méthode : 5 analyses parallèles avec lecture du code source réel. Chaque constat est
appuyé par un `fichier:ligne` vérifié. Les documents détaillés :

| Document | Contenu |
|---|---|
| [01-code-mort.md](01-code-mort.md) | Inventaire consolidé du code mort (~20 000 lignes) |
| [02-classification-nommage.md](02-classification-nommage.md) | Moteur de classification et règles de nommage |
| [03-architecture-modules.md](03-architecture-modules.md) | Frontières de modules, graphe de dépendances réel vs documenté |
| [04-core-engine-audit.md](04-core-engine-audit.md) | Core hors classification (engine, graph, audit SPI, plugin executor) |
| [05-plugins.md](05-plugins.md) | Les 4 plugins (JPA, REST, living-doc, audit) |
| [06-classification-metier.md](06-classification-metier.md) | Analyse fonctionnelle (2026-08-01) : classification par évidences hiérarchisées et propagation vers les plugins. Constats vérifiés ; les décisions D6-D11 qu'elle soumettait sont **toutes tranchées** ([DECISIONS.md](DECISIONS.md)), lire le registre avant le doc |
| [07-architecture-page-blanche.md](07-architecture-page-blanche.md) | Exercice page blanche (2026-08-01) : architecture, algorithmes et outillage d'une réimplémentation idéale. Architecture retenue par D12 |
| [08-plan-reecriture-ancree.md](08-plan-reecriture-ancree.md) | Plan de construction exécutoire (D12) : jalons M0-M8, liste de récolte, B1-B15 en tests de régression, gate de parité |
| [09-referentiel-regles.md](09-referentiel-regles.md) | Référentiel des règles (2026-08-02, D13/D14) : Q1 identification / Q2 conformité, ancres et dérivation par vagues, nommage différé jusqu'à fin M3. Précise et amende le §3 du doc 06 |
| [10-plan-tests-m3.md](10-plan-tests-m3.md) | Plan de tests du reste de M3 (2026-08-02) : couches de test, doctrine d'arbitrage du corpus, grammaire des scénarios contextuels, lots 15-23 |

---

## Verdict global

L'intuition de départ est confirmée et dépassée. Les choix contre-productifs ne sont
pas les règles de nommage en elles-mêmes (elles sont un fallback légitime), mais :

1. **~20 000 lignes de code mort publiées sur Maven Central** (~13 % du source de
   production, ~27 % de hexaglue-core). Ce sont les vestiges des refontes v3→v4→v5
   jamais nettoyées : 3 moteurs de classification sur 4 sont morts, l'abstraction de
   parsing `hexaglue-syntax` est morte dans son intégralité, un moteur d'audit complet
   dort dans core, un sous-système entier (~4 000 lignes) dort dans le plugin audit.
   Ce code mort est testé et vert, ce qui donne une fausse impression de santé et
   masque des divergences fonctionnelles avec le code vivant.

2. **~102 sites de décision par nommage, dispersés sur 6 couches, dupliqués et
   invisibles.** Le prédicat « champ d'identité » existe en 10 exemplaires avec
   3 sémantiques incompatibles ; « port de persistance » en 7 listes de suffixes
   divergentes. Sur ~48 sites actifs, 4 seulement émettent une évidence
   `NAMING` traçable ; `EvidenceType.PACKAGE` n'est jamais émis ; le `ReasonTrace`
   soigneusement construit est jeté à la frontière core→arch. Aucune règle n'est
   configurable. Le rapport d'audit ne peut donc pas distinguer « classé par
   relation » (fiable) de « classé par convention de nommage » (à confirmer).

3. **Les frontières de modules documentées ne sont pas tenues.** Core importe Spoon
   directement (14 fichiers) et court-circuite l'abstraction `hexaglue-syntax` ;
   le pivot classification→modèle est une `String` avec 3 switchs divergents ;
   3 `TypeRef`, 4 enums de confiance, 3 `Evidence`, 2 `Severity` coexistent ;
   le SPI d'audit (`ArchitectureQuery`) re-classifie l'architecture par heuristiques
   de nommage alors que le modèle classifié est disponible.

4. **Une quinzaine de bugs fonctionnels confirmés**, dont plusieurs faussent les
   rapports d'audit en production (voir tableau ci-dessous).

La robustesse du code vivant est par ailleurs bonne (copies défensives
systématiques, pas d'état statique mutable), et le socle conceptuel
(pipeline unidirectionnel, `ArchType` scellé, indexes) est sain. Le problème est
l'accumulation, pas la conception d'origine.

---

## Bugs confirmés (à corriger avant tout refactoring)

| # | Bug | Localisation | Impact |
|---|---|---|---|
| B1 | Principe des dépendances stables **inversé** (`fromStability > toStability` au lieu de `<`) | `core/audit/DefaultArchitectureQuery.java:650-672` | Des centaines de fausses violations sur tout projet sain ; le score « dependency » des rapports d'audit tombe à 0. La version correcte existe... dans le code mort (`DependencyStableRule:139`) |
| B2 | Mots réservés SQL non échappés : `PropertyFieldSpec:138` utilise `JpaModelUtils.toSnakeCase` au lieu de `NamingConventions.toColumnName` | `plugin-jpa/model/PropertyFieldSpec.java:138` | Un champ domaine `order`, `value`, `key`, `group` génère `@Column(name="order")` → échec DDL PostgreSQL/MySQL/H2. Le mécanisme anti-mot-réservé existe mais n'est pas branché |
| B3 | Annotations matchées par **nom simple** : `@jakarta.persistence.Entity`, `@org.springframework.stereotype.Repository` déclenchent une classification DDD priorité 100/EXPLICIT | `AbstractExplicitAnnotationCriteria.java:84-88`, `AbstractExplicitPortAnnotationCriteria.java:94-98` | Sur un projet Spring/JPA, les entités JPA sont classées entités DDD avec la confiance maximale, court-circuitant toute la chaîne sémantique |
| B4 | `ImplementsJMoleculesInterfaceCriteria` classe **tout** en AGGREGATE_ROOT (le kind calculé est perdu car `MatchResult` ne porte pas de kind) | `ImplementsJMoleculesInterfaceCriteria.java:56-95` | Un `implements ValueObject` jMolecules → AGGREGATE_ROOT → échec du builder → retombe silencieusement en UNCLASSIFIED (exception avalée, voir B5) |
| B5 | Exception avalée sans note ni log dans `buildAggregateRoot` | `builder/NewArchitecturalModelBuilder.java:223-232` | Masque B4 et toute erreur de construction d'agrégat |
| B6 | Valeurs d'annotations **jetées** à la frontière core→arch (`Annotation.of(qualifiedName)` sans `values()`) | `builder/TypeStructureBuilder.java:327-329` | Aucun plugin ne peut lire `@Column(name=…)`, `@Table`, `@GeneratedValue(strategy=…)` ; la hiérarchie `AnnotationValue` d'arch est inatteignable |
| B7 | NPE latent dans le tri topologique des plugins (maps initialisées au fil de l'itération) | `plugin/PluginExecutor.java:276-285` | Build cassé de façon intermittente selon l'ordre du classpath |
| B8 | Bounded contexts : 3e segment de package codé en dur, sans tenir compte du `basePackage` | `DefaultArchitectureQuery.java:288-325` | Pour `basePackage=com.acme.erp`, un seul bounded context détecté pour tout le projet ; l'inventaire des rapports est faux |
| B9 | Bounded contexts recalculés différemment par living-doc (`BoundedContextDetector`) | `plugin-living-doc/content/BoundedContextDetector.java:146-156` | Deux documents générés par le même build décrivent une topologie différente |
| B10 | Détection de cycles : DFS sans élagage `visited`, exponentiel, cycles dupliqués | `DefaultArchitectureQuery.java:96-110` | Le même cycle est pénalisé plusieurs fois dans le score de santé ; la version correcte existe dans le code mort (`DependencyNoCyclesRule:179-191`) |
| B11 | Champ mutable partagé `matchedKind` dans un critère singleton | `InheritedClassificationCriteria.java:65-81` | Non thread-safe, valeur périmée après `noMatch()`, contrat `targetKind()` violé |
| B12 | Collision de priorités (75) entre criteria DRIVING et DRIVEN → conflit dur systématique | `PortClassifier.java:93-108` + `DefaultDecisionPolicy.java:52-59` | `CreateOrderUseCase` injecté comme champ → COMMAND vs REPOSITORY à égalité → le type disparaît du modèle. Les gardes-fous par nommage prolifèrent pour masquer ce défaut |
| B13 | `hasPortAnnotation` mensonger : `contains()` sur noms + `packageName.contains("port")` | `semantic/InterfaceFactsIndex.java:155-210` | `reporting`, `export`, `support`, `PortfolioService` matchent ; porte d'entrée de la génération de code |
| B14 | `String.formatted` appliqué au mauvais littéral, ×14 | `audit/rules/Naming*SuffixRule` et al. | Messages corrompus (code mort aujourd'hui, piège si réactivé) |
| B15 | `JpaConfig.idStrategy` : défaut réel `ASSIGNED`, documenté `IDENTITY` ; `valueOf` non protégé | `plugin-jpa/JpaConfig.java:57,96` | Config utilisateur invalide → message JDK brut sans contexte |

---

## Plan de refactoring recommandé

> **CADUC depuis le 2026-08-01 (décision D12), conservé pour trace.** Ce plan en
> phases 0-5 supposait une migration en place de `hexaglue/`. La stratégie retenue
> est la **réécriture ancrée** : le plan exécutoire est
> [08-plan-reecriture-ancree.md](08-plan-reecriture-ancree.md) (jalons M0-M8) et
> l'état d'avancement est dans [CHANTIER.md](CHANTIER.md). Le contenu ci-dessous
> reste utile comme intrant : les bugs deviennent des tests de régression,
> l'inventaire du code mort une liste d'exclusions de récolte, les garde-fous le
> jalon M0.

### Phase 0 — Correctifs de bugs (1-2 jours, indépendant du reste)

B1, B2, B5, B6, B7 sont des patchs de quelques lignes chacun, avec test de
non-régression. B3 (opt-in du match par nom simple) et B4 (kind porté par
`MatchResult`) demandent une retouche du contrat des criteria (~1 jour).

### Phase 1 — Purge du code mort (2-3 jours, risque quasi nul)

Suppression des ~20 000 lignes inventoriées dans [01-code-mort.md](01-code-mort.md)
et de leurs tests. Gains mécaniques : élimine 1 des 3 moteurs de classification
morts et ses ~30 règles de nommage, le 2e moteur d'audit, la 2e hiérarchie de
critères, la 2e abstraction de parsing, et la moitié des enums dupliquées.
Trois décisions préalables : `hexaglue-testing` (API publiée pour les
utilisateurs ?), `spi/core/ResolutionConfig` (contrat SPI publié ?),
`graph/testing/TestGraphBuilder` (à déplacer en test-jar, pas à supprimer).

### Phase 2 — Unification du vocabulaire de nommage (~1 semaine)

C'est la réponse directe à la question des conventions de nommage.
Détail dans [02-classification-nommage.md](02-classification-nommage.md) :

1. Un module `classification/naming/` avec un `NamingVocabulary` immuable unique
   (suffixes de ports, verbes command/query, marqueurs de packages, noms d'audit),
   surchargeable via `hexaglue.yaml`.
2. Une seule `IdentityFieldPolicy` injectée partout (remplace les 10 définitions).
3. Toute décision par nom émet obligatoirement `Evidence(NAMING)` ou
   `Evidence(PACKAGE)` ; règle ArchUnit pour l'imposer.
4. `ReasonTrace` propagé jusqu'à `ClassificationTrace` (aujourd'hui jeté).
5. Échelle de priorités documentée, sans collision inter-direction (B12).
6. Configuration par critère via `IdentifiedCriteria.id()` (l'infrastructure
   existe déjà, seul le câblage manque) : enable/disable, priorité, profils
   strict/balanced/permissive.

Bénéfice produit : l'audit peut alors afficher « classé par convention de
nommage, ajoutez `@AggregateRoot` pour fiabiliser » — les règles de nommage
deviennent un capteur assumé au lieu d'un mécanisme caché.

### Phase 3 — Typage du pivot classification→modèle (3-5 jours)

- `ClassificationResult.kind` : `String` → type scellé (`DomainClassification` /
  `PortClassification` / `Unclassified` / `Conflicted`) avec `ElementKind`/`PortKind`
  typés. Les 3 switchs sur chaînes divergents deviennent exhaustifs et vérifiés
  à la compilation.
- Un seul `ConfidenceLevel` (avec `EXPLICIT`, dans arch) et un seul
  `Evidence`/`EvidenceType` ; suppression des mappers lossy qui écrasent
  EXPLICIT→HIGH.

### Phase 4 — Frontières de modules (1-2 semaines)

Détail dans [03-architecture-modules.md](03-architecture-modules.md) :

- Promouvoir `core.frontend` (les interfaces `Java*`) dans `hexaglue-syntax-api`,
  déplacer `core.frontend.spoon` + `CachedSpoonAnalyzer` dans
  `hexaglue-syntax-spoon`, supprimer l'abstraction morte `SyntaxProvider`/`TypeSyntax`.
  Retirer `spoon-core` du POM de core : le build échoue si un `import spoon.`
  subsiste.
- Remplacer les downcasts `instanceof SpoonMethodAdapter` par des méthodes
  d'interface (`JavaMethod.cyclomaticComplexity()`).
- Unifier les 3 `TypeRef` en un seul.
- Réimplémenter `DefaultArchitectureQuery` sur `ArchitecturalModel`
  (DomainIndex/PortIndex) au lieu des heuristiques de nommage ; réduire le SPI
  `ArchitectureQuery` aux 6 méthodes réellement consommées (sur 22).
- Les plugins déclarent explicitement `hexaglue-arch` (used-undeclared aujourd'hui).

### Phase 5 — Découpage des god classes et module commun (1-2 semaines)

- Créer `hexaglue-plugin-commons` (Naming, JavaTypes, DSL Mermaid, markdown,
  ModuleTargetResolver, PluginConfigs) — élimine ~25 duplications inter-plugins.
- Découper : `SinglePassClassifier` (617→~120 lignes en passant par le
  PortClassifier au lieu de la logique inline), `DefaultArchitectureQuery` (912),
  `DddAuditPlugin` (1 339, dont 18 méthodes copiées-collées → −350 lignes),
  `MapperSpecBuilder` (1 365 → 4 détecteurs testables), `ReportDataBuilder`
  (1 156 → 1 builder par section), Mojos (base `AbstractHexaGlueMojo`,
  `ValidateMojo` 509→~150).

### Garde-fous permanents (à installer dès la phase 1)

| Garde-fou | Outil |
|---|---|
| Core ne connaît pas Spoon | retrait de la dépendance + enforcer `bannedDependencies` |
| Arch ne classifie pas | ArchUnit : interdit `*Classifier`/`*Criterion` dans `io.hexaglue.arch.**` |
| Criteria par nommage → Evidence NAMING | ArchUnit sur les appels `endsWith`/`startsWith`/`matches` |
| Pas de dépendance used-undeclared | `maven-dependency-plugin:analyze-only` `failOnWarning` |
| Piège `"a" + "b".formatted(...)` | règle PMD custom |
| Javadoc API publique valide | `doclint=reference` sur spi et arch (aujourd'hui `none`) |
| SpotBugs bloquant | `failOnError=true` (aujourd'hui `false`) |

### Trous de tests à combler (chemin de production)

- `core/classification/anchor/` (4 classes, étape 1 du classifieur) : **aucun test**.
- `core/graph/query/DefaultGraphQuery` : aucun test direct.
- `core/frontend/spoon/adapters/` (9 classes) : testées seulement indirectement.
- `plugin-rest/model/` : 18 classes sans test.
- Cas B1-B15 : chaque correctif doit arriver avec son test de non-régression.
