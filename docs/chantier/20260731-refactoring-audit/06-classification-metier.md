# Analyse fonctionnelle — Classification et propagation vers les plugins

Date : 2026-08-01.
Objet : le fonctionnement **métier** de HexaGlue — comment identifier la
structure architecturale et le rôle des classes d'une application Java
(hexagonale + DDD, frameworks d'entreprise standards) à partir du graphe
Spoon, et comment faire parvenir cette information, avec sa provenance,
jusqu'aux plugins d'audit, de validation et de génération.

Cette analyse complète l'audit du 2026-07-31 (qualité du code) ; elle ne le
remplace pas. Méthode : 4 analyses parallèles du code vivant (pipeline de
classification, flux core→arch→plugins, frontend/graphe, exemples/tests),
chaque constat vérifié `fichier:ligne`. Préfixe `core/` =
`hexaglue-core/src/main/java/io/hexaglue/core/`.

---

## 0. Résumé

La promesse produit — « compiler l'intention architecturale du code » — bute
sur trois verrous fonctionnels qui forment un système :

1. **Le nommage ne sert pas de fallback : il est structurant.** Le moteur
   décide par cascade premier-match et par vetos nominaux ; le contrat
   utilisateur observable (exemples, golden tests) exige les suffixes
   `*Id`/`*Repository` et les packages `ports.in/out`. Un domaine correct qui
   ne suit pas ces conventions est mal ou pas classifié.
2. **La matière première rend les signaux forts invisibles.** Le graphe ne
   crée aucune arête vers les types externes : `extends JpaRepository<Order,
   OrderId>` — le signal le plus déterminant qui existe sur du code
   d'entreprise — ne produit rien. Les annotations frameworks sont soit un
   contresens (`@jakarta.persistence.Entity` → ENTITY DDD explicite, B3), soit
   des ancres calculées puis jamais relues.
3. **La provenance meurt à la frontière core→arch.** Évidences, trace,
   candidats en cas de conflit : tout est jeté. En aval, ni `validate`, ni
   l'audit, ni la génération ne peuvent distinguer « classé par relation »
   de « classé par nommage » — alors chaque couche re-devine par nommage,
   d'où les ~102 sites recensés par l'audit.

Cible proposée : un moteur d'**évidences hiérarchisées** (intention déclarée >
connaissance des frameworks > relations > structure > topologie > nommage)
avec **propagation par point fixe** sur le graphe, un **registre de
connaissance des frameworks**, et une **trace complète propagée** consommée
par les trois fonctions aval (portes de confiance pour `validate`, provenance
dans l'audit, seuils de certitude pour la génération).

---

## 1. Le problème métier reformulé

Principe fondateur : on parse le code Java d'une application dans un graphe
Spoon et on identifie la structure architecturale des classes et leur rôle.

La cible réelle est plus large que les exemples actuels :

| Profil d'application | État du support |
|---|---|
| A. Hexagonale « à la HexaGlue » (POJO, suffixes, `ports.in/out`) | Bien couverte (tous les exemples et golden tests) |
| B. Entreprise standard, modèle unique (domaine annoté JPA, Spring Data, `@Service`) | Non couverte : aucune fixture, aucun test ; B3 produit des contresens |
| C. En cours de migration (mélange des deux — les case studies) | Contournée : `case-study-ecommerce/hexagonal/hexaglue.yaml:1-4` **exclut** l'infrastructure de l'analyse |

Et trois consommateurs aux besoins distincts :

- **Audit** : dire la vérité sur l'existant — y compris « ce type est classé
  sur une simple convention de nommage, confirmez-le ».
- **Validation** : des portes objectives (échouer si trop d'inféré, trop
  d'ambigu), aujourd'hui impossibles.
- **Génération** : ne générer que sur du certain ; générer du JPA depuis une
  classification nominale erronée produit du code faux silencieusement.

---

## 2. Constat : pourquoi la classification échoue sur la cible

### 2.1 Le nommage décide au lieu d'informer

- **Cascade premier-match** : pour les ports, un `if/else-if` inline
  (`core/classification/SinglePassClassifier.java:253-342`) court-circuite le
  moteur de criteria ; les 10 criteria de `PortClassifier` ne s'exécutent
  qu'en fallback (`:337`). Le domaine est classifié **sans aucun accès** aux
  résultats des ports : le contexte inter-phases est construit puis ignoré
  (`:459-463`, paramètre jamais lu dans `:472-512`).
- **Le prédicat d'entrée des ports est nominal aux deux tiers** :
  `hasPortAnnotation` = annotation jMolecules OU package `contains("port")`
  OU nom contenant un des 13 motifs
  (`core/classification/semantic/InterfaceFactsIndex.java:186-210`).
- **Conditions dures nominales** : `RecordSingleIdCriteria` exige
  `endsWith("Id")` (`core/classification/domain/criteria/RecordSingleIdCriteria.java:70-72`) —
  un `OrderNumber`/`OrderKey` est refusé, et c'est **testé comme comportement
  attendu** (`DomainCriteriaTest.java:329-343`). Les criteria relationnels
  portent des **vetos nominaux** (`looksLikeDrivenPort`,
  `CommandPatternCriteria.java:131-142`, `InjectedAsDependencyCriteria.java:120-127`)
  qui masquent la collision de priorités B12.
- **Le contrat utilisateur observable est nominal** : tous les exemples et
  toutes les fixtures d'intégration/golden reposent sur `*Id`, `*Repository`,
  `ports.in/out` ; aucun mini-domaine de test entièrement sans suffixe
  n'existe ; aucun test de classification sur projet Spring réaliste
  (rapport exemples, `hexaglue-core/src/test/`).

### 2.2 La matière première rend les signaux forts invisibles

- **Aucune arête vers les types externes.** `GraphBuilder` restreint toutes
  les arêtes aux types du `basePackage` (`knownTypes`,
  `core/graph/builder/GraphBuilder.java:84-89`, conditions `:175-208`).
  Conséquences directes sur du code d'entreprise :
  - `interface OrderRepository extends JpaRepository<Order, OrderId>` ne
    produit **aucune** arête IMPLEMENTS ni TYPE_ARGUMENT — alors que cette
    seule ligne établit avec certitude : port de persistance, `Order` agrégat,
    `OrderId` identifiant.
  - `@Entity`, `@RestController`, `@AggregateRoot` (jMolecules, en JAR) :
    **zéro** arête ANNOTATED_BY.
  - Nuance importante : l'information survit **au niveau des nœuds**
    (`TypeNode.interfaces()` garde le `TypeRef` complet avec ses arguments de
    type ; `TypeNode.annotations()` garde les `AnnotationRef` **avec
    valeurs**, `GraphBuilder.java:370-377`). Elle est locale, non indexée,
    et aucun criteria ne la lit sous cette forme.
- **Les valeurs d'annotations meurent à la frontière core→arch** (B6,
  `core/builder/TypeStructureBuilder.java:326-328`) — même en corrigeant B6,
  le frontend vivant dégrade déjà (annotations imbriquées stringifiées,
  `SpoonAnnotationAdapter.java:76-78`) là où le frontend mort
  (`hexaglue-syntax-spoon`) produisait des `AnnotationValue` typés.
- **Types imbriqués jamais analysés** : `SpoonSemanticModel.types()` repose
  sur `CtModel.getAllTypes()` (top-level uniquement,
  `core/frontend/spoon/SpoonSemanticModel.java:56`). Un `Order.OrderLine`
  n'existe ni comme nœud ni comme arête.
- **Ni membres hérités ni fermeture transitive** : `getMethods()` déclarées
  seulement (`SpoonTypeAdapter.java:122-124`), `subtypesOf/supertypesOf`
  directs seulement (`core/graph/index/GraphIndexes.java:278-301`).
- **Les corps de méthodes sont extraits puis jetés** :
  `CachedSpoonAnalyzer.analyzeMethodBody` capture invocations et accès champs
  (`core/frontend/CachedSpoonAnalyzer.java:181-263`) ; seul l'entier de
  complexité cyclomatique survit (`GraphBuilder.java:272-280`). Aucune arête
  d'appel ni d'instanciation n'existe dans le graphe.
- **Contresens frameworks** : le matching par nom simple des annotations
  « explicites » (B3/C3) classe `@jakarta.persistence.Entity` en ENTITY DDD
  et `@org.springframework.stereotype.Repository` en REPOSITORY DDD, priorité
  100/EXPLICIT — pendant que `AnchorDetector.INFRA_ANNOTATIONS` déclare les
  mêmes symboles « infrastructure » (`core/classification/anchor/AnchorDetector.java:70-83`).
  Et les ancres elles-mêmes (le seul endroit où Spring/Jakarta sont compris)
  sont calculées puis **jamais relues** par le classifieur
  (`SinglePassClassifier.java:241`, aucun criteria ne consulte `AnchorContext`).

### 2.3 La provenance meurt en route

Constats du traçage bout en bout (25 points de perte détaillés, sélection) :

- `ReasonTrace` n'est **jamais lu** ; `DomainClassifier`/`PortClassifier` le
  produisent déjà `null` (`DomainClassifier.java:179-188`,
  `PortClassifier.java:175-184`).
- Les évidences sont jetées au convertisseur : la branche qui les
  transporterait est inatteignable
  (`core/builder/ClassificationTraceConverter.java:120-136`) ;
  `AppliedCriterion.evidence` est toujours vide ; `SemanticContext` et
  `RemediationHint` sont des champs de contrat **toujours vides** (`:80`).
- En cas de conflit, le kind candidat est détruit dès le core
  (`ClassificationResult.java:239-278` : kind/confidence/criteria `null`) ;
  un type en conflit devient indiscernable d'un type inconnu.
- Des types disparaissent **sans verdict** : interfaces sans méthode et
  interfaces d'événement sortent de la boucle sans entrée
  (`SinglePassClassifier.java:296,300`), puis du modèle
  (`NewArchitecturalModelBuilder.java:166-168`).
- `validate` ne valide **jamais les ports** (filtre `target == DOMAIN`,
  `core/engine/DefaultHexaGlueEngine.java:388-389`), n'utilise **jamais la
  confiance** pour décider (`ValidateMojo.java:176`), et `allowInferred` est
  parsé mais sans effet (`ClassificationConfig.validationConfig()` lu nulle
  part). Ses suggestions de remédiation sont trois lignes génériques
  identiques pour tous les types (`ValidateMojo.java:359-364`).
- L'audit ne lit ni confiance ni critère gagnant hors logs
  (`DddAuditPlugin.java:457-520`) : **aucun rapport ne distingue « classé par
  nommage » de « classé par annotation/relation »**. La living-doc affiche un
  badge HIGH/MEDIUM/LOW sans jamais dire pourquoi
  (`DomainContentSelector.java:407-416`).
- Le seul usage fonctionnel de la provenance dans tout le produit est le
  couplage fragile de JpaPlugin à la chaîne `"contained-entity"`
  (`JpaPlugin.java:439-442`).

### 2.4 Diagnostic de causalité

Les trois verrous s'alimentent : (2.2) prive le moteur de signaux forts →
(2.1) le nommage comble le vide et devient décisif → (2.3) l'aval ne voyant
pas la différence, chaque couche re-devine par nommage
(`DefaultArchitectureQuery`, `ApplicationPurityValidator`,
`BoundedContextDetector`, JpaPlugin) → duplication et divergence des règles.

**Corriger (2.1) seul — le `NamingVocabulary` de la Phase 2 — assainit le code
mais ne résout pas le métier** : sans signaux forts (2.2) ni propagation
(2.3), le vocabulaire unifié restera l'arbitre principal.

---

## 3. Cible : classification par évidences hiérarchisées

### 3.1 Hiérarchie des sources de signal

Toute règle de classification devient un **capteur** qui émet des évidences
typées ; plus aucun capteur ne décide seul. Six niveaux, par force
décroissante :

| Niveau | Source | Exemples | Confiance max |
|---|---|---|---|
| S1 | Intention déclarée | `hexaglue.yaml` `classification.explicit` ; annotations/interfaces jMolecules (FQN exact **uniquement**) | EXPLICIT |
| S2 | Connaissance des frameworks | `extends` Spring Data `Repository<A,ID>` ; `@RestController` ; `@Entity` JPA (fait « modèle de persistance », voir 3.4) | HIGH |
| S3 | Relations dans le graphe | implémentée par le cœur / injectée dans le cœur ; sujet des signatures d'un port ; composition depuis un agrégat | HIGH |
| S4 | Structure locale | record, immutabilité, champ d'identité, enum, forme, wrapper mono-composant | HIGH (jamais EXPLICIT) |
| S5 | Topologie | packages `ports.in/domain/...` ; **`ModuleRole`** (aujourd'hui invisible à la classification) | MEDIUM |
| S6 | Nommage (`NamingVocabulary`) | suffixes, verbes command/query | MEDIUM, jamais décisif contre S2-S4, jamais de veto |

Règles du jeu :

1. Chaque évidence porte : type de source, force, justification, localisation
   source, nœuds liés. Toute décision par nom émet NAMING ou PACKAGE (règle
   ArchUnit déjà prévue en Phase 2).
2. La décision est une **pesée lexicographique par niveau avec marge** : un
   niveau supérieur non contredit l'emporte ; à niveau égal, cumul
   d'évidences ; marge insuffisante → AMBIGUOUS **avec candidats ordonnés
   conservés** (jamais de destruction d'information, contrairement à
   `conflictDomain/conflictPort` actuels).
3. Les incompatibilités sont sémantiques et intègrent la direction
   (DRIVING vs DRIVEN départagés par signal structurel, corrige B12/H5) —
   plus de collision de priorités arbitraires.
4. Le kind est porté par la **contribution**, pas par le criteria (corrige
   B4/C1 et B11/C2 par construction).

### 3.2 Propagation par point fixe

Le point faible algorithmique central du `SinglePassClassifier` est dans son
nom : une seule passe, aucun type ne bénéficie de la classification de ses
voisins. Or l'essentiel du DDD est relationnel. Cible :

- **Phase seed** : règles locales S1/S2/S4/S5/S6 (annotations, frameworks,
  structure, packages, noms) → premières évidences.
- **Phase propagation** : règles S3 conditionnées par les classifications
  voisines suffisamment sûres, appliquées **jusqu'à stabilité**. Le processus
  est monotone (on n'ajoute que des évidences, on n'en retire jamais) donc il
  termine ; en pratique 2-3 itérations.

Règles de propagation types (chacune remplace une heuristique locale
actuelle qui re-devine par nommage) :

| Règle | Si... | Alors évidence... | Remplace |
|---|---|---|---|
| R1 | `I extends` Spring Data `Repository<A,ID>` (arguments de type déjà disponibles au niveau nœud) | I=DRIVEN_PORT(REPOSITORY) S2 ; A=AGGREGATE_ROOT S2 ; ID=IDENTIFIER S2 | rien : cas aujourd'hui invisible |
| R2 | A agrégat confirmé, champ d'identité de type T interne | T=IDENTIFIER S3 | `endsWith("Id")` de `RecordSingleIdCriteria` comme condition dure |
| R3 | A agrégat confirmé, champ/collection vers T interne ; T a une identité propre → ENTITY, sinon → VALUE_OBJECT | S3 | `ContainedEntityCriteria`/`EmbeddedValueObjectCriteria` (qui tournent aujourd'hui sans savoir si le conteneur est un agrégat) |
| R4 | interface P injectée dans un CoreAppClass ET (pas d'impl interne \| impl uniquement infra) | P=DRIVEN_PORT S3 ; le kind (REPOSITORY/GATEWAY/...) par signatures | la cascade inline + 7 listes de suffixes |
| R5 | interface P implémentée par le cœur, appelée depuis une ancre driving | P=DRIVING_PORT S3 | `implementedByCore` seul, et le veto `usedByCore` divergent |
| R6 | classe C implémente un DRIVING_PORT et dépend d'un DRIVEN_PORT | C=APPLICATION_SERVICE S3 | pivot actuel (conservé, mais devient traçable) |
| R7 | type immutable retourné/publié par un port EVENT_PUBLISHER | DOMAIN_EVENT S3 | le seul `endsWith("Event")` |
| R8 | classe sans état, opère sur ≥2 types du domaine, injectée dans des services | DOMAIN_SERVICE S3 | rien : `DOMAIN_SERVICE` est aujourd'hui un kind **inatteignable** (aucun criteria vivant ne le produit) |

Note : `ProgressiveClassifier` (mort) visait déjà du multi-passes, mais par
re-analyse des corps de méthodes Spoon ; la cible propage sur le graphe déjà
construit — déterministe, testable, sans coût de parsing additionnel.

### 3.3 FrameworkKnowledge : un registre unique de connaissance des frameworks

Aujourd'hui la connaissance Spring/Jakarta est éparpillée en ≥6 listes
divergentes (`AnchorDetector:44-101`, `LayerClassifier:76-92`,
`CoreAppClassDetector.isExternalType:164-181`,
`InterfaceFactsIndex.isExternalInterface:223-230`, `GENERATED_ANNOTATIONS`,
préfixes infra des plugins). Cible : un registre déclaratif unique,
extensible par configuration, où chaque entrée (FQN exact ou préfixe de
package — **jamais de nom simple**, corrige B3/C3) émet des **faits
techniques** :

| Marqueur | Fait émis | Interprétation |
|---|---|---|
| `org.springframework.data.repository.Repository` et dérivés (`CrudRepository`, `JpaRepository`, `MongoRepository`...) en supertype | SPRING_DATA_REPOSITORY(sujet, id) | R1 |
| `jakarta.persistence.Entity/Embeddable/MappedSuperclass` (+ `javax.`) | PERSISTENCE_MODEL | voir 3.4 |
| `@RestController`, `@Controller`, listeners Kafka/JMS/Rabbit, JAX-RS `@Path` | DRIVING_ENTRYPOINT | ancre driving → DRIVING_ADAPTER (3.5) |
| `@Service` / `@Component` / `@Transactional` | APPLICATION_STEREOTYPE (faible) | appuie R6, jamais décisif |
| `@Configuration`, `@SpringBootApplication`, `@ConfigurationProperties` | TECHNICAL | hors hexagone, catégorisé |
| `jakarta.validation.*`, `org.slf4j.*` | NEUTRE | n'est pas une pollution du domaine |
| types-outils injectés (`JdbcTemplate`, `EntityManager`, `RestTemplate`, `WebClient`, `feign.`...) | INFRA_DEPENDENCY | ancre infra (existant, conservé) |

Le registre sert à la fois la classification (évidences S2), l'audit
(pureté du domaine : les préfixes interdits en dur de
`DomainPurityValidator`/`ApplicationPurityValidator` deviennent ce même
registre, configurable) et la délimitation du périmètre.

### 3.4 La posture face au domaine annoté JPA (profil B)

> **Amendé par D7 (2026-08-02) : les deux profils ci-dessous ne sont PAS
> retenus.** La posture est unique et sans configuration : le kind vient de
> S3/S4 comme si l'annotation n'existait pas, et le couplage produit un
> finding. Ce que le profil `strict` aurait fait se règle par la porte de
> validation (`ValidationConfig.findingThresholds`, p. ex.
> `HG-DDD-0xx: BLOCKER`), pas par une branche du moteur. Le discriminant entre
> « domaine couplé » et « persistance interne à un adapter » est la position
> dans le graphe : référencé par l'hexagone contre référencé seulement par un
> adapter. Le texte ci-dessous est conservé pour la trace du raisonnement.

Point de doctrine à trancher (décision D7 proposée). Principe non négociable :
**une annotation JPA n'est jamais une évidence DDD positive** (c'est
aujourd'hui le contresens B3). Elle établit le fait « type mappé en
persistance ». Deux profils de configuration :

- **`strict`** : les types PERSISTENCE_MODEL sortent de l'hexagone
  (TECHNICAL/OUT_OF_SCOPE), l'audit exige la séparation domaine/persistance.
  C'est la posture actuelle implicite (case studies : exclusion manuelle).
- **`pragmatic`** (défaut proposé) : le kind DDD vient de S3/S4 (le repo
  Spring Data qui gère le type, l'identité, la composition) **comme si
  l'annotation JPA n'existait pas**, et l'audit produit un finding « domaine
  couplé à la persistance » avec la liste des types concernés. C'est ce qui
  rend HexaGlue utile sur le parc existant (spring-petclinic : aujourd'hui
  classification aberrante ; demain : agrégats corrects + un finding honnête).

### 3.5 Un verdict pour chaque type du périmètre

- Plus aucune disparition silencieuse : tout type du `basePackage` reçoit un
  verdict (kind, UNCLASSIFIED catégorisé, ou OUT_OF_SCOPE motivé).
- Les adapters existants sont classifiés `DRIVING_ADAPTER`/`DRIVEN_ADAPTER`
  (les `ElementKind` existent déjà, jamais produits) à partir des ancres —
  décision D8 : le principe « hors périmètre » de CLAUDE.md vient du cas
  génération ; le cas audit/migration a besoin de la couverture totale
  (aujourd'hui `ApplicationPurityValidator` re-classifie par package
  précisément parce que le modèle ne couvre pas ces types, 05-C1).

---

## 4. Prérequis sur la matière première (graphe/frontend)

Par ordre de levier :

- **G1 — Types externes dans le graphe** : nœuds externes légers (flag
  `external`, pas de membres) + arêtes EXTENDS/IMPLEMENTS/ANNOTATED_BY/
  TYPE_ARGUMENT sans le filtre `knownTypes`. Volumétrie bornée par le nombre
  de types externes distincts référencés. Déverrouille S2 en requêtable.
  *Premier incrément possible sans toucher au graphe* : les règles seed S2
  peuvent lire dès aujourd'hui `TypeNode.interfaces()` (arguments de type
  inclus) et `TypeNode.annotations()` (valeurs incluses) — R1 est
  implémentable au niveau nœud.
- **G2 — Valeurs d'annotations bout en bout** : B6 (transport core→arch) est
  déjà au chantier ; y ajouter la non-dégradation côté frontend (s'inspirer
  des `AnnotationValue` typés du frontend `hexaglue-syntax-spoon` **avant sa
  suppression** en Phase 1/D3).
- **G3 — Types imbriqués** : remplacer `getAllTypes()` par un parcours
  incluant les types nested (avec lien DECLARES vers le type englobant).
- **G4 — Fermeture transitive** : index `supertypesOf*`/`interfacesOf*`
  transitifs (les criteria d'héritage refont aujourd'hui la récursion à la
  main, chacun à sa façon).
- **G5 (différé)** — Arêtes d'invocation/instanciation depuis l'analyse de
  corps déjà écrite (`CachedSpoonAnalyzer`) : utile pour R7/R8 et pour
  l'audit comportemental ; à n'activer qu'à la demande (coût).

---

## 5. Propagation aval : le contrat cible

### 5.1 La trace de classification (remplit le contrat existant, aujourd'hui vide)

```
ClassificationTrace {
  kind, confidence,            // UN SEUL enum, avec EXPLICIT (Phase 3)
  basis: DECLARED | INFERRED,  // la distinction que tout l'aval attend
  evidences: [ {source(S1..S6), force, description, sourceLocation?, relatedTypes} ],
  candidates: [ {kind, score, evidences} ],   // non vide si AMBIGUOUS/CONFLICT
  remediation: [ RemediationHint ]            // enfin produits
}
```

Les champs `SemanticContext`, `RemediationHint`, `evaluatedCriteria`,
`Evidence.sourceLocation` existent déjà dans `hexaglue-arch` — le contrat est
bon, il n'a jamais été rempli.

### 5.2 `validate` : des portes objectives

- Valider **aussi les ports** (supprimer le filtre `target == DOMAIN`).
- Portes configurables et effectives : `failOnUnclassified` (existe),
  `minConfidence`, `failOnAmbiguous`, `allowInferred` (réellement branché).
- Messages par type : catégorie + hint spécifique (« ajoutez
  `@AggregateRoot` ou déclarez `classification.explicit` ») au lieu des
  trois suggestions génériques actuelles.

### 5.3 Audit : la provenance devient un livrable

- Inventaire : colonne provenance (déclaré / inféré-relationnel /
  inféré-nominal) par type.
- Nouvelle section « fiabilité de la classification » : taux par basis,
  ambiguïtés avec candidats, remédiations priorisées — le rapport peut enfin
  dire « 12 types classés sur convention de nommage seule, confirmez-les ».
- Violations taguées quand elles reposent sur des types inférés-nominaux
  (une violation sur une classification incertaine n'a pas le même poids).

### 5.4 Génération : seuil de certitude

- `generation.minConfidence` (défaut HIGH) : sous le seuil, pas de code —
  un diagnostic + hint à la place (générer du JPA sur une classification
  nominale douteuse est pire que ne rien générer).
- JPA lit la composition depuis le modèle (`containedEntities`/
  `CompositionIndex`) — suppression du couplage à la chaîne
  `"contained-entity"` ; identifiants comparés par type via `DomainIndex`
  (plus de `endsWith("Id")` résiduel, 05-H4).

### 5.5 SPI : la règle d'or devient outillable

- Identifiants de règles typés exposés dans le SPI (plus de comparaison de
  chaînes libres).
- `ArchitectureQuery` réimplémentée sur le modèle classifié (déjà Phase 4) ;
  garde-fou ArchUnit/PMD étendu aux plugins : tout `endsWith/startsWith/
  contains` sur un nom hors `NamingVocabulary` est un échec de build.

---

## 6. Effets attendus, mesurables

Constituer un **corpus de référence à trois profils** avec golden files
incluant la provenance :

1. Style HexaGlue (fixtures actuelles) — non-régression stricte.
2. Entreprise standard type petclinic (domaine JPA + Spring Data + `@Service`)
   — aujourd'hui : contresens et invisibilité ; cible : agrégats/ports
   corrects via R1-R6 + findings de pureté.
3. Sans conventions (style `sample-starwars` : `Fleets`, `AssembleAFleet`,
   `UUID` nu) — aujourd'hui : dépend du package `contains("port")` ; cible :
   classification par S3.

KPIs par profil : taux de classification, répartition DECLARED/INFERRED,
taux d'ambiguïté, zéro régression sur le profil 1. C'est le critère
d'acceptation fonctionnel du chantier classification.

---

## 7. Décisions soumises par cette analyse — toutes tranchées

Ce tableau n'appelle plus d'arbitrage : il donne l'issue de chaque question,
au 2026-08-02. La formulation qui fait foi est celle de
[DECISIONS.md](DECISIONS.md).

| # | Question | Issue |
|---|---|---|
| D6 | Moteur cible : refonte « évidences + point fixe » vs amélioration incrémentale du SinglePass | **Refonte** (option A), par voie de conséquence de D12 |
| D7 | Posture par défaut face au domaine annoté JPA : `pragmatic` vs `strict` | **Ni l'un ni l'autre : posture unique sans profil** (voir l'amendement du §3.4) |
| D8 | Étendre la classification aux adapters existants (DRIVING/DRIVEN_ADAPTER) | **Oui** ; le « hors périmètre » de CLAUDE.md est amendé (adapter des sources = classifié, adapter généré = sortie) |
| D9 | Enrichir le graphe de nœuds/arêtes externes (G1) | **Oui**, natif dès M2 (livré) |
| D10 | Annotations HexaGlue propres (`io.hexaglue.annotation.*`) ou jMolecules + YAML uniquement | **jMolecules + YAML**, sans annotations propres ni report au backlog |
| D11 | Corpus entreprise (profils 2 et 3 du §6) comme critère d'acceptation | **Oui**, critère d'acceptation exécutable |

---

## 8. Articulation avec le chantier en cours

Rien ici n'invalide les phases décidées ; cette analyse les précise et
propose une suite :

- **Phases 0-1 inchangées et préalables** : B3 (matching nom simple), B4/B11
  (kind porté par le criteria), B6 (valeurs d'annotations), B12 (collision de
  direction) sont des prérequis directs de la cible ; la purge du code mort
  élimine les moteurs concurrents. Point d'attention Phase 1 : récupérer le
  design des `AnnotationValue` typés de `hexaglue-syntax-spoon` avant
  suppression (G2).
- **Phase 2 précisée** : `NamingVocabulary` = le capteur S6 (plafonné
  MEDIUM, sans veto) ; y adjoindre le **FrameworkKnowledge** (§3.3) qui
  absorbe les listes d'`AnchorDetector`/`LayerClassifier`/plugins.
- **Phase 3 précisée** : le typage du pivot transporte la trace complète
  (§5.1), pas seulement le kind.
- **Phase 4 inchangée** : `ArchitectureQuery` sur le modèle = le volet SPI
  de §5.5.
- **Nouvelle phase proposée — « Phase 6 : moteur d'évidences et
  propagation »** (après Phase 3, prérequis graphe G1/G3/G4 réalisables dès
  la fin de Phase 1) : agrégateur par pesée, règles seed + propagation
  R1-R8, point fixe, profils `strict/pragmatic`, corpus §6, puis consommation
  aval (validate §5.2, audit §5.3, génération §5.4). Bloquée par D6-D11.

---

## Annexe A — Faits nouveaux constatés (absents de l'audit du 2026-07-31)

| # | Fait | Localisation |
|---|---|---|
| A1 | Aucune arête vers les types externes (filtre `knownTypes`) ; `extends JpaRepository`, annotations en JAR : zéro arête | `core/graph/builder/GraphBuilder.java:84-89,175-208` |
| A2 | Types imbriqués jamais analysés (`getAllTypes()` top-level) | `core/frontend/spoon/SpoonSemanticModel.java:56` |
| A3 | `validate` ne valide jamais les ports (filtre `target == DOMAIN`) | `core/engine/DefaultHexaGlueEngine.java:388-389` |
| A4 | `AuditMojo.failOnUnclassified` sans effet (transmis mais jamais lu) | `hexaglue-maven-plugin/.../AuditMojo.java:135-136,220` |
| A5 | Types sans verdict : interfaces marker/événement sortent du modèle sans entrée UNCLASSIFIED | `SinglePassClassifier.java:296,300` ; `NewArchitecturalModelBuilder.java:166-168` |
| A6 | `DOMAIN_SERVICE` inatteignable : aucun criteria vivant ne le produit | grep `return ElementKind.` dans `domain/criteria/` |
| A7 | `ClassificationContext` inter-phases construit puis ignoré ; `drivingPorts/drivenPorts` calculés puis jetés | `SinglePassClassifier.java:175-179,459-463,472-512` |
| A8 | `AnchorContext` jamais relu par le classifieur (les annotations frameworks ne servent qu'au périmètre CoreAppClass) | `SinglePassClassifier.java:241` |
| A9 | `methodCallsFrom/To`, `fieldAccessesFrom/To` : API vivante retournant toujours vide (aucun producteur d'arêtes d'appel) | `core/graph/query/DefaultGraphQuery.java:195-224,354-367` |
| A10 | `CachedSpoonAnalyzer` : ~12 traversées AST par corps de méthode, seul l'entier de complexité survit ; caches non synchronisés malgré la Javadoc « thread-safe » ; hit rate attendu ~0 % | `core/frontend/CachedSpoonAnalyzer.java:70,89,104-117,543-550` ; `GraphBuilder.java:272-280` |
| A11 | Constantes d'enum matérialisées en `FieldNode` ordinaires ; composants de record sans marqueur | `SpoonTypeAdapter.java:110-112` |
| A12 | `USES_AS_COLLECTION_ELEMENT` : champs uniquement, `Map` exclue, 1er argument de type seulement | `core/graph/builder/DerivedEdgeComputer.java:125-165` ; `core/frontend/TypeRef.java:43-44,106-111` |
| A13 | Noms de critères en dur dans `SinglePassClassifier` divergents des noms canoniques (`"SemanticDrivingPortCriteria"` vs `"semantic-driving"`) | `SinglePassClassifier.java:358,384` |
| A14 | `ModuleRole` invisible à la classification (`ModuleIndex` construit après) ; `ModuleDescriptor.basePackage` toujours `null` | `DefaultHexaGlueEngine.java:332-362` |
| A15 | Membres hérités non extraits (`getMethods()` déclarées) ; `ConstructorNode.thrownTypes` jamais alimenté ; `HAS_PARAMETER`/`THROWS` jamais émis | `SpoonTypeAdapter.java:122-124` ; `GraphBuilder.java:283-290` |
| A16 | `JavaField.initialValue()` extrait puis jamais consommé | `SpoonFieldAdapter.java:70-76` |
| A17 | Style de packages détecté (`packageOrganizationStyle`, `supportsPortDirection`) puis jamais consulté par la classification | `core/graph/query/GraphQuery.java:198-217` |
