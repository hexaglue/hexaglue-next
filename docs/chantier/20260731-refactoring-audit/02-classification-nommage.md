# Moteur de classification et règles de nommage

Périmètre : `hexaglue-core/classification/**`, `builder/**`, `enrichment/**`.
Le classifieur de production est `SinglePassClassifier` (unique appelant :
`DefaultHexaGlueEngine:272`). `ProgressiveClassifier`, `classification/secondary/`,
`enrichment/` et `arch/builder/` sont morts (voir 01-code-mort.md).

## Diagnostic central sur les règles de nommage

Le problème n'est pas l'existence de règles de nommage — c'est un fallback
légitime quand ni annotation ni relation ne tranche. Le problème est leur état :

- **~102 sites de décision par nom** recensés (83 actifs dont 3 inatteignables,
  19 morts), répartis sur 6 couches : criteria domaine, criteria ports, couche
  sémantique (InterfaceFacts/Anchor), builders, IR du graphe (TypeNode/FieldNode),
  et code mort.
- **Dupliqués** : le prédicat « champ d'identité » existe en 10 exemplaires avec
  3 sémantiques incompatibles (`endsWith("Id")` vs `<typeName>Id` vs annotations) ;
  « port de persistance » en 7 listes de suffixes divergentes (13, 13, 9, 9, 6,
  13 via `contains`, 7).
- **Invisibles** : sur ~48 sites actifs, 4 émettent `EvidenceType.NAMING` ;
  `EvidenceType.PACKAGE` n'est jamais émis (`Evidence.fromPackage` sans appelant) ;
  `RepositoryDominantCriteria` émet `RELATIONSHIP` alors que sa décision repose
  sur `isPersistencePort()` (pur nommage).
- **Non configurables** : `ClassificationConfig` ne permet que l'exclusion par
  glob et le forçage nominal. Impossible de désactiver un critère, changer un
  suffixe, ou passer en mode strict. `IdentifiedCriteria.id()` (implémenté par
  25 criteria, documenté « for YAML profiles ») n'est lu par personne.
  `ValidationConfig.allowInferred` est parsé mais jamais consommé.
- **Contradiction documentaire** : `SinglePassClassifier:61` promet
  « derived from RELATIONSHIPS, not from NAMES ».

## Findings CRITICAL

### C1 — `ImplementsJMoleculesInterfaceCriteria` classe tout en AGGREGATE_ROOT
`domain/criteria/ImplementsJMoleculesInterfaceCriteria.java:56-95`.
`evaluate()` calcule le bon kind depuis `INTERFACE_TO_KIND` puis le jette :
`MatchResult` (record `matched/confidence/justification/evidence`) ne porte pas
de kind, et le builder utilise `criteria.targetKind()` qui retourne toujours
`AGGREGATE_ROOT` (le constructeur privé paramétré n'est jamais appelé).
Un `implements org.jmolecules.ddd.types.ValueObject` → AGGREGATE_ROOT prio 100
EXPLICIT → échec `AggregateRootBuilder` → avalé (B5) → UNCLASSIFIED silencieux.
Aucun test ne couvre cette classe.
**Refactoring** : faire porter le kind par `MatchResult<K>` et retirer
`targetKind()` comme source de vérité ; ou éclater en 4 criteria mono-kind.

### C2 — État mutable partagé dans `InheritedClassificationCriteria`
`domain/criteria/InheritedClassificationCriteria.java:65-81`. Champ
`private ElementKind matchedKind` muté pendant `evaluate()` sur une instance
singleton partagée pour tous les types. Non thread-safe, valeur périmée après
`noMatch()`, contrat `targetKind()` (documenté constant) violé. Même cause
racine que C1 ; même correctif.

### C3 — Annotations « explicites » matchées par nom simple
`AbstractExplicitAnnotationCriteria.java:84-88` et
`AbstractExplicitPortAnnotationCriteria.java:94-98` : fallback
`annotation.simpleName().equals(...)`. Conséquence : `@jakarta.persistence.Entity`
→ ENTITY DDD, `@org.springframework.stereotype.Repository` → repository DDD,
avec priorité 100 / EXPLICIT — alors que `AnchorDetector.INFRA_ANNOTATIONS`
liste ces mêmes annotations comme infrastructure. Le même symbole produit deux
verdicts opposés selon la couche. Aussi : `ExplicitIdentifierCriteria:66-68`,
`RepositoryDominantCriteria:172-176`.
**Refactoring** : match par nom simple opt-in (réservé aux annotations
HexaGlue), et registre unique `AnnotationVocabulary` partagé avec AnchorDetector.

### C4 — `InterfaceFacts.hasPortAnnotation` est un mensonge
`semantic/InterfaceFactsIndex.java:155-210`. Le booléen nommé « hasPortAnnotation »
teste : annotation OU `packageName.contains("port")` OU nom `contains()` parmi
13 motifs. `contains` non ancré : `PaymentAdapterFactory`, `ClientRegistry`,
`PortfolioService`, packages `reporting`/`export`/`support` matchent. Ce booléen
alimente `isDrivenPortCandidate()`, porte d'entrée de la génération de code.
**Refactoring** : décomposer en `hasExplicitPortAnnotation` /
`isInPortPackage` / `matchesPortNamingConvention`, chacun avec son Evidence
typée ; `contains` → `endsWith` ; ancrer le test de package (`.port.`/`.ports.`).

### C5 — Logique des criteria sémantiques dupliquée inline dans `SinglePassClassifier`
`SinglePassClassifier.java:288-338` recopie en ligne (avec `continue`) la logique
de `SemanticDrivenPortCriteria`/`SemanticDrivingPortCriteria`, rendant
`SemanticDrivenPortCriteria.evaluate()` inatteignable en production. Pire :
la branche inline utilise `isDrivingPortCandidate()` (= implémenté ET non
consommé par le core) tandis que le criteria fallback teste `implementedByCore()`
seul — deux définitions contradictoires de « driving port » dans le même package,
la doc du modèle affirmant l'inverse du fallback. Les noms de criteria et
priorités (85/80) sont en dur dans le classifieur.
**Refactoring** : supprimer les branches inline, transformer les 5 conditions en
5 `PortClassificationCriteria` avec priorités déclarées ; `SinglePassClassifier`
passe de 617 à ~250 lignes.

## Findings HIGH

### H1 — 10 définitions du prédicat « champ d'identité », 3 sémantiques
`RepositoryDominantCriteria:178-197` = `InheritedClassificationCriteria:254-277`
(copies) ; `EmbeddedValueObjectCriteria:187-199` = `ContainedEntityCriteria:148-158`
= `DomainRecordValueObjectCriteria:133-143` (copies) ;
`SignatureBasedDrivenPortCriteria:154-157` = `SignatureBasedGatewayCriteria:138-141`
(copies) ; + `FieldRoleDetector:138-187`, `FieldNode:126-132`, `JavaField:44-47`.
`endsWith("Id")` accepte `productId` dans `OrderLine` ; `<typeName>Id` le rejette.
Le même champ est « identité » pour un critère et pas pour l'autre.
**Refactoring** : une `IdentityFieldPolicy` unique injectée partout ; supprimer
`FieldNode.looksLikeIdentity()` et `JavaField.looksLikeIdentity()`.

### H2 — 7 listes de suffixes « port de persistance », dont 2 paires copiées-collées
Listes divergentes dans `RepositoryDominantCriteria` (13 suffixes),
`InheritedClassificationCriteria` (copie), `CommandPatternCriteria` (9),
`QueryPatternCriteria` (copie), `SignatureBasedDrivenPortCriteria` (6),
`InterfaceFactsIndex` (13 via `contains`), `DomainServiceBuilder` (7).
`CommandPatternCriteria` et `QueryPatternCriteria` partagent 5 méthodes privées
identiques au caractère près, y compris deux regex recompilées à chaque appel
(`String.matches` dans les chemins chauds). Asymétrie non documentée : en cas
d'égalité command/query, Command gagne (`>` vs `>=`).
**Refactoring** : `NamingVocabulary` immuable unique, surchargeable via
`hexaglue.yaml` ; fusion en un `CqrsPatternCriteria` paramétré par direction ;
`Pattern.compile` en constantes.

### H3 — Conventions DDD fuitées dans l'IR du graphe
`TypeNode.java:162-195` (`hasRepositorySuffix`, `hasGatewaySuffix`,
`hasUseCaseSuffix`, `hasIdSuffix`, `hasEventSuffix`), `FieldNode:126-141`,
`JavaField:44-57`, `GraphQuery:88-100` (`repositories()`, `identifiers()`).
Le graphe censé être une IR syntaxique neutre embarque du vocabulaire DDD ;
la plupart de ces méthodes n'ont d'ailleurs aucun appelant vivant.
**Refactoring** : supprimer ; le vocabulaire vit dans `NamingVocabulary`.

### H4 — Exception avalée dans `NewArchitecturalModelBuilder.buildAggregateRoot`
`builder/NewArchitecturalModelBuilder.java:223-232` : catch `IllegalStateException`
→ fallback unclassified, commentaire « with a note » mais aucune note, aucun log.
Masque C1. **Refactoring** : `AggregateRootBuilder.tryBuild()` retournant un
résultat explicite + Diagnostic warning + RemediationHint dans le rapport.

### H5 — Collision de priorités inter-direction → conflit dur systématique
`CommandPatternCriteria` (DRIVING, 75), `QueryPatternCriteria` (DRIVING, 75),
`InjectedAsDependencyCriteria` (DRIVEN, 75) + `CompatibilityPolicy.noneCompatible`
+ `DefaultDecisionPolicy:52-59` → une interface use-case injectée comme champ
produit un conflit dur → `unclassifiedPort` → le type disparaît du modèle.
Les gardes-fous par nommage (`looksLikeDrivenPort`…) existent pour masquer cette
collision : la duplication de règles de nommage est un symptôme de ce défaut.
Aussi : `PortDirection` transportée en `Map<String,Object>` non typée alors que
`targetDirection()` existe en typé.
**Refactoring** : direction intégrée à la décision (deux directions opposées =
incompatibles par définition, départagées par signal structurel) ; échelle de
priorités documentée sans collision inter-direction ; suppression de
`Contribution.metadata`.

### H6 — Évidence et traçabilité non exploitées
(a) `EvidenceType.NAMING` émis 4 fois sur ~48 décisions par nom ; (b)
`EvidenceType.PACKAGE` jamais émis ; (c) `ClassificationTraceConverter.convert()`
ne lit jamais `reasonTrace` — toute la traçabilité construite par
`SinglePassClassifier` est perdue au passage core→arch ; (d) branche morte
`ClassificationTraceConverter:120-136` : le `noneMatch` est toujours vrai
(noms `-evidence` vs noms de criteria), donc `evaluatedCriteria` ne contient
toujours qu'un élément, vidant `ClassificationTrace.explain()` de son sens.
**Refactoring** : Evidence obligatoire et typée dans `MatchResult.match(...)` ;
règle ArchUnit « endsWith/startsWith/matches sur un nom ⇒ Evidence NAMING » ;
propager `ReasonTrace` ; collecter aussi les criteria non matchés.

### H7 — Aucune configurabilité
`spi/core/ClassificationConfig.java:52-53` : seulement excludePatterns +
explicitClassifications. Proposition :

```yaml
hexaglue:
  classification:
    profile: strict          # strict | balanced | permissive
    criteria:
      domain.naming.domainEvent: { enabled: false }
      domain.structural.repositoryDominant: { priority: 85 }
    naming:
      persistencePortSuffixes: [Repository, Dao]
      commandVerbs: [create, place, cancel]
```

Clé = `IdentifiedCriteria.id()` (existe déjà sur 25 criteria) ; le
`ContributionBuilder` de `CriteriaEngine` accepte déjà une `effectivePriority`
— seul le câblage manque. Supprimer ou implémenter `allowInferred`.

## Findings MEDIUM

- **M1** `SinglePassClassifier` god class (617 l., 6 responsabilités,
  `classifyPorts` 90 l.) ; `createPortAwareContext` ignore 2 de ses 3 paramètres.
  Extraire `SemanticIndexBuilder`, `ClassificationResultFactory`,
  `TypeClassificationFilter`.
- **M2** `AbstractExplicitAnnotationCriteria` / `AbstractExplicitPortAnnotationCriteria` :
  classes de base dupliquées au caractère près → une seule classe générique
  `<K extends Enum<K>>`.
- **M3** Pivot stringly-typed : `ClassificationResult.kind` est une `String` ;
  switch sur chaînes avec `default` silencieux ; 6 champs nullables dans le
  record. → type scellé + kinds typés (détail : README phase 3).
- **M4** Parsing de `NodeId` par manipulation de chaîne dupliqué 5 fois avec
  variantes (`>` vs `>=`, `endsWith` faux : `com.acme.Order` matche
  `type:com.acme.SubOrder`). → `NodeId.qualifiedName()` ; remplacer les scans
  O(n²) de `AggregateRootBuilder:230-234` par une Map.
- **M5** Conversion de confiance dupliquée et lossy (EXPLICIT|HIGH → HIGH) dans
  `ClassificationTraceConverter:95-105` et `ClassificationReportBuilder:177-186` ;
  3 enums `ConfidenceLevel` + 2 `EvidenceType`. → un seul enum avec EXPLICIT.
- **M6** Classification métier dans les builders (invisible à l'audit) :
  `AggregateRootBuilder:256-262` (association repo↔agrégat par `contains`),
  `:58-59` (invariants par regex `validate|check|ensure|verify`),
  `DomainServiceBuilder:65-66` (ports injectés par 7 suffixes),
  `EntityBuilder:164-179` = `DomainEventBuilder:175-196` (même heuristique
  strip-`Id` dupliquée), `UnclassifiedCategoryDetector:49-62` (Utils/Helper…).
  → résolveurs explicites dans `classification/` produisant des Evidence,
  consommés par les builders via `BuilderContext`.
- **M7** Docs contredisant le code : `DomainEnumCriteria` (« in domain packages »
  mais classe tous les enums), `DomainClassifier:94` (« priority >= 70 » mais
  contient 68 et 65), `IdentifiedCriteria` (« for YAML profiles », jamais lu),
  `NewArchitecturalModelBuilder:229` (« with a note », aucune note).

## Findings LOW

- **L1** Listes de types externes incohérentes : `CoreAppClassDetector:164-181`
  (13 préfixes) vs `InterfaceFactsIndex:223-230` (5) → `ExternalTypePredicate`
  unique.
- **L2** Détection de code de test dupliquée et divergente
  (`InterfaceFactsIndex:215-218` : 4 marqueurs vs
  `UnclassifiedCategoryDetector:52-62` : 10) alors que `ModuleSourceSet` porte
  déjà l'information prod/test.
- **L3** Regex recompilées dans les chemins chauds (`String.matches` dans
  Command/QueryPatternCriteria) — `AggregateRootBuilder` montre le bon pattern.
- **L4** Tie-break command/query non documenté ni testé.
- **L5** `AnchorDetector.classify:146-188` : le signal package (faible, documenté
  comme tel) évalué avant les dépendances de champ (signal fort) — inverser.

## Inventaire des règles de nommage (synthèse)

Comptage par couche (détail complet conservé dans les rapports d'analyse) :

| Couche | Sites | Exemples | Statut |
|---|---:|---|---|
| Criteria domaine | 20 | 13 suffixes repository ×2, `endsWith("Id")` record → IDENTIFIER (prio 80), `endsWith("Event")` → DOMAIN_EVENT (68), match annotations par nom simple (100) | actifs |
| Criteria ports | 20 | verbes command/query regex ×2, suffixes UseCase/Command/Query/Handler ×4 listes, packages `.ports.in` | actifs |
| Sémantique/ancres | 12 | `contains("port")` package, 13 motifs `contains`, préfixes infra `.infrastructure.`/`.adapters.`, 15 préfixes CRUD → REPOSITORY, 7 verbes → EVENT_PUBLISHER | actifs (2 jamais appelés) |
| Builders | 22 | `<typeName>Id`, champs audit (createdat…), rôles de méthodes (get/set/of/from…), Utils/Helper → UTILITY, `aggregateId` | actifs |
| IR graphe | 9 | `hasRepositorySuffix`, `looksLikeIdentity` | majorité morte |
| Code mort | 19 | poids arbitraires WeightedMultiSignal (Order/Account/Invoice +3…), arch/builder criteria | morts |

Constat transverse : **aucune n'est configurable**, et la sévérité réelle vient
des règles à confiance EXPLICIT/priorité 100 déclenchées par nom simple (C3),
pas des heuristiques de fallback à priorité basse.
