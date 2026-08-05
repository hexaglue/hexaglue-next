# Page blanche — architecture, algorithmes et outillage d'un HexaGlue réécrit aujourd'hui

Date : 2026-08-01.

> **Statut au 2026-08-02** : l'architecture décrite ici est celle **retenue** pour
> le nouveau réacteur (décision D12) ; c'est la référence de revue de chaque
> jalon, §3 pour les contrats et §10 pour les interdits. Seule la conclusion
> §11 (« ne pas réécrire ») est dépassée : la stratégie est la réécriture
> **ancrée** — corpus, exemples et savoir-faire transplantés, carrière
> `hexaglue/` gelée en lecture seule — décrite par
> [08-plan-reecriture-ancree.md](08-plan-reecriture-ancree.md).
>
> **Amendements du registre à connaître avant de lire §2, §6 et §9** :
> `hexaglue-cli` (§2.1) et le CLI autonome (§9) **ne sont pas des livrables de
> la 7.0.0** (D17) — la restitution existe, indépendante de l'hôte ; le SPI
> (§6.1) n'est pas écrit avant **M6** et arrive avec son premier plugin (D18) ;
> la validation reste une politique du moteur (§6.3) et elle est **livrée**
> depuis M5, gates comprises. Les sinks de §6.1 sont étagés, aucun avant son
> consommateur (D25) : `ReportSink` s'appelle `DocumentSink`, `FindingSink`
> est **supprimé** (D24 — les findings viennent du moteur, un plugin n'est
> jamais juge), `SourceSink` et `DiagnosticSink` arrivent à M7 (D28) ; le
> manifeste porte `produces` (D27) mais ni `consumes` ni `minConfidence`,
> différés faute de consommateur (§10.7). En cas d'écart entre ce document et
> le registre, le registre l'emporte.

Nature : **exercice de conception**, pas un plan de réécriture. On repart de
zéro sur la chaîne complète — analyse du code existant → identification et
classification des classes → propagation aux plugins → plugins d'audit, de
validation et de génération — en intégrant les enseignements des documents
01 à 06. Ce document sert d'**étoile polaire** pour arbitrer D6-D11 et
orienter les refactorings ; la conclusion (§11) explique pourquoi la
réécriture totale n'est pas recommandée.

---

## 1. Vision : un compilateur d'architecture, littéralement

HexaGlue se décrit comme un « architecture compiler ». La page blanche prend
la métaphore au sérieux et adopte l'architecture éprouvée des compilateurs :

```
SOURCES Java ──frontend──▶ CODE MODEL ──règles──▶ ARCH MODEL ──backends──▶ artefacts
 + classpath                (base de faits         (IR classifiée            (code généré,
                             syntaxiques)           + preuves)                rapports, gates)
```

Cinq invariants produit, non négociables et outillés :

1. **Déterminisme total** : mêmes sources → mêmes sorties, octet à octet
   (collections ordonnées partout, aucune dépendance à l'ordre d'itération
   ni au classpath ; vérifié par test de re-exécution).
2. **Explicabilité** : toute conclusion porte une preuve consultable —
   `hexaglue explain com.acme.Order` affiche l'arbre de dérivation complet.
3. **Échec bruyant** : jamais de modèle vide silencieux (leçon 04-H6) ;
   diagnostics codés (`HG-xxxx`) avec localisation, documentés publiquement.
4. **Budget de build** : l'analyse doit rester en secondes sur un millier de
   types ; incrémentalité au niveau module (réacteur).
5. **Les frontières sont des modèles de données immuables**, jamais des
   couches d'abstraction spéculatives (leçon 03-C1 : l'abstraction publiée
   « pour une future implémentation » est morte sans avoir servi).

---

## 2. Architecture applicative

### 2.1 Modules

```
hexaglue-model           LE CONTRAT. Records + interfaces scellées, zéro dépendance,
                         zéro logique : CodeModel, ArchModel, Evidence/Proof,
                         Finding, Diagnostic, config typée. Publié, stable, versionné.
hexaglue-frontend        Spoon (+ classpath) → CodeModel. SEUL module dépendant de
                         Spoon. Pas d'interface frontend spéculative : la frontière
                         est le CodeModel lui-même.
hexaglue-knowledge       Packs déclaratifs de connaissance frameworks (ressources
                         YAML versionnées, §5). Données, pas code.
hexaglue-engine          Base de faits, moteur de saturation, règles, décision,
                         indexes, API explain. Aucune E/S.
hexaglue-spi             Contrat plugin : consomme ArchModel, émet via sinks typés.
hexaglue-plugins/*       audit, living-doc, jpa, rest (les backends).
hexaglue-maven-plugin    Adaptateur Maven mince. + hexaglue-gradle-plugin,
hexaglue-cli             + CLI autonome (analyze / explain / audit) pour le
                         debug et l'adoption hors build.
hexaglue-testkit         PUBLIÉ : DSL de fixtures Java, harnais golden,
                         corpus de référence à 3 profils (06 §6).
```

Sens des dépendances : `frontend → model ← engine ← spi ← plugins` ;
`maven/gradle/cli → engine`. Personne d'autre ne voit Spoon ; personne ne
voit Maven hors des adaptateurs.

### 2.2 Décisions structurantes (et leurs raisons)

- **Une seule implémentation de frontend.** Le point de substitution est le
  `CodeModel` (données), pas une hiérarchie d'interfaces `Java*`/`*Syntax`.
  Si un jour un frontend javac/JDT existe, il produira le même CodeModel.
- **La validation n'est pas un plugin** : c'est une politique du moteur
  (gates sur statistiques de classification + findings, §6.3). L'audit reste
  un plugin car il produit du contenu ; la validation ne produit qu'un verdict.
- **Un seul modèle de findings** partagé audit/validation : plus jamais de
  sémantique transportée dans des messages texte re-parsés (leçon 05-H2).
- **Le moteur n'écrit rien** : les plugins émettent dans des sinks, l'hôte
  (Maven/CLI) matérialise. Chemins confinés par construction (leçon 04-M5).
- **Auto-application** : HexaGlue s'audite lui-même en CI (dogfooding) et ses
  propres frontières de modules sont tenues par ArchUnit + enforcer.

---

## 3. Le modèle pivot

### 3.1 CodeModel : une base de faits syntaxiques

Graphe de propriétés immuable + relations typées, construit une fois :

- **Nœuds** : types (y compris **imbriqués**, leçon A2, et **stubs externes**
  légers pour tout type du classpath référencé, leçon A1), membres, modules.
- **Arêtes typées avec preuve** (`via=return/param:i/typeArg:i/field`) —
  y compris vers les types externes : `extends JpaRepository<Order,OrderId>`
  produit une arête EXTENDS vers un stub + les TYPE_ARGUMENT.
- **Annotations avec valeurs typées** (`AnnotationValue` scellée : String,
  Class, Enum, Array, Annotation imbriquée — jamais stringifiée, leçon B6).
- **Fermetures transitives précalculées** : hiérarchie de supertypes
  **incluant le classpath** (lue en bytecode, §7) — savoir que
  `MongoRepository` est un `Repository` Spring Data sans en avoir les sources.
- **Faits de corps de méthode optionnels** (invocations, instanciations)
  derrière un flag de capacité : extraits en **une seule traversée AST**
  (contre ~12 aujourd'hui, leçon A10), activés seulement si une règle ou un
  plugin les demande.
- Un **unique** `TypeRef` récursif (arguments de type, tableaux, wildcard
  distingué d'une variable de type).

### 3.2 ArchModel : l'IR classifiée

Le socle conceptuel actuel est bon et il est conservé : `ArchType` scellé
(Domain/Port/Application/Unclassified), `TypeStructure`, indexes
(DomainIndex, PortIndex, CompositionIndex, ModuleTopology). Ce qui change :

```
Classification {
  kind, direction?,
  confidence,                    // UN enum : EXPLICIT | HIGH | MEDIUM | LOW
  basis: DECLARED | INFERRED,
  evidences: [ {tier(S1..S6), fact, why, sourceLocation, relatedTypes} ],
  candidates: [ {kind, score, evidences} ],   // conservés si AMBIGUOUS
  proof: ProofNode                            // arbre (règle, prémisses)
}
```

Tout type du périmètre a un verdict (leçon A5) ; les adapters existants sont
classifiés (`DRIVING_ADAPTER`/`DRIVEN_ADAPTER`) pour servir l'audit (06 §3.5).

### 3.3 Findings et diagnostics

`Finding { code (HG-DDD-012…), severity, subject, locations, evidences,
remediation }` — produit par les règles d'audit, consommé par les renderers
**et** par les gates de validation. `Diagnostic` pour les problèmes de
l'outil lui-même. Les deux codés, localisés, documentés.

---

## 4. Algorithmes

### 4.1 Classification = inférence à saturation (sémantique Datalog, moteur maison)

Le problème « classer des nœuds d'un graphe selon des règles dépendant du
classement des voisins » est un problème d'**inférence de faits jusqu'à
point fixe**. C'est la sémantique Datalog — sans embarquer de moteur tiers :

- **Faits typés** : `extends(T,S)`, `annotated(T,A,values)`,
  `injectedField(C,P)`, `signatureUses(P,T,via)`, `moduleRole(M,R)`, puis
  dérivés : `persistenceModel(T)`, `springDataRepository(I,subject,id)`,
  `evidence(T,kind,tier,weight,why)`, `classified(T,kind,conf)`.
- **Règles = classes Java** implémentant `Rule { emit(FactBase, Sink) }`,
  pures, déclarant leurs prédicats d'entrée/sortie. Pas de DSL externe, pas
  de scripting : débogables, typées, testables unitairement.
- **Évaluation semi-naïve** : à chaque itération, seules les règles dont un
  prédicat d'entrée a reçu de nouveaux faits sont ré-exécutées.
- **Stratification** :
  - S0 : faits syntaxiques (frontend).
  - S1 : interprétation frameworks (packs §5) → faits techniques.
  - S2 : hypothèses locales — chaque règle émet des évidences par palier
    S1..S6 (hiérarchie du doc 06 §3.1).
  - S3 : **décision** — agrégateur déterministe (pas une règle) : pesée
    lexicographique par palier, marge minimale, sinon AMBIGUOUS avec
    candidats conservés ; direction et compatibilités sémantiques intégrées.
  - S4 : **propagation** — règles conditionnées par les kinds décidés
    (repository ⇒ sujet agrégat ⇒ champ id identifiant ⇒ composition
    entités/VO ⇒ ports par usage… : les R1-R8 du doc 06 §3.2). On boucle
    S2→S4 jusqu'à stabilité.
- **Terminaison garantie** : on n'ajoute que des faits (monotonie), univers
  fini ⇒ point fixe ; en pratique 2-3 itérations. Complexité linéaire en
  |faits| par itération grâce au semi-naïf.
- **Preuves gratuites** : chaque fait dérivé mémorise (règle, prémisses) ⇒
  l'arbre de preuve alimente `explain`, l'audit et les golden files.
- **Déterminisme** : base de faits ordonnée, règles ordonnées par identifiant,
  aucune structure à ordre d'itération non spécifié.

Prior art assumé : CodeQL et jQAssistant valident le motif « faits + règles +
requêtes » pour l'analyse d'architecture ; on n'en retient que le motif, pas
les dépendances (§4.4).

### 4.2 Ce que ça corrige par construction

Premier-match et vetos nominaux disparaissent (les évidences coexistent et
sont pesées) ; les conflits ne détruisent plus d'information ; le kind est
porté par le fait, pas par la règle (B4/B11 impossibles) ; la collision de
priorités inter-direction (B12) devient une pesée avec départage structurel ;
le nommage est un palier plafonné, jamais décisif seul (06 §3.1).

### 4.3 Algorithmes du graphe (audit)

- **Cycles** : Tarjan (SCC), linéaire, cycles dédupliqués par construction
  (leçon B10 : le DFS exponentiel maison) — via JGraphT, pas réécrit.
- **Métriques de Martin** (Ce/Ca/I/A/D) calculées une fois par package,
  mémoïsées sur graphe figé (leçon 04-H8) ; sens du SDP testé sur un cas
  canonique (leçon B1).
- **Composition / ownership des agrégats** : accessibilité BFS depuis les
  racines confirmées, dans le sous-graphe domaine, avec règle d'ownership
  exclusif (un type possédé par 2 agrégats ⇒ finding, pas un choix
  silencieux).
- **Bounded contexts** : relatifs au `basePackage` (leçon B8), stratégie
  configurable (package / module / annotation), **une seule implémentation**
  consommée par tous les plugins (leçon B9).

### 4.4 Ce qu'on n'utilise pas, et pourquoi

| Écarté | Raison |
|---|---|
| Drools / moteurs de règles génériques | ordre d'activation difficile à rendre déterministe, dépendance lourde, preuves pauvres |
| Neo4j / jQAssistant embarqué | base graphe persistante disproportionnée pour un plugin de build |
| Soufflé / Datalog natifs | binaire C++ hors JVM ; le sous-ensemble utile tient en ~500 lignes de Java |
| ML / LLM dans le chemin de build | non déterministe, inexplicable dans un gate ; au plus une couche *advisory* hors verdict, jamais dans `validate` |

---

## 5. La connaissance des frameworks : des packs de données

Aujourd'hui : ≥6 listes de FQN codées en dur et divergentes. Page blanche :
des **packs déclaratifs** embarqués (ressources versionnées de
`hexaglue-knowledge`), étendus par l'utilisateur pour ses frameworks internes.

```yaml
pack: spring-data
rules:
  - supertype: org.springframework.data.repository.Repository   # fermeture classpath
    emits: SPRING_DATA_REPOSITORY(subject: $T0, id: $T1)
  - annotation: jakarta.persistence.Entity
    emits: PERSISTENCE_MODEL
  - annotation: org.springframework.web.bind.annotation.RestController
    emits: DRIVING_ENTRYPOINT
  - package-prefix: jakarta.validation
    emits: NEUTRAL          # n'est pas une pollution du domaine
```

- Matching par **FQN exact ou préfixe de package, jamais par nom simple**
  (leçon B3/C3) ; `supertype` s'applique à la fermeture transitive incluant
  le classpath (§3.1).
- Les packs servent trois consommateurs avec la même donnée : la
  classification (évidences S2), l'audit de pureté (préfixes interdits
  configurables, leçon 05-H3) et la délimitation du périmètre.
- Doctrine JPA (06 §3.4, amendée par D7) : `PERSISTENCE_MODEL` n'est **jamais**
  une évidence de kind, ni positive ni négative. Il n'y a pas de profil : le
  fait alimente un finding, et la sévérité doctrinale se règle par la porte de
  validation.
- Chaque pack est testé par contrat (fixtures minimales dans le testkit).

---

## 6. Le SPI plugins

### 6.1 Contrat

```java
interface HexaGluePlugin {
    PluginManifest manifest();   // id, consumes, produces, minConfidence requis
    void contribute(ArchModel model, PluginConfig config, Sinks sinks);
}
```

- **Sinks typés** : `SourceSink` (fichiers Java, chemins confinés, routage
  multi-module intégré — leçon 05-H9), `ReportSink`, `FindingSink`,
  `DiagnosticSink`. Le plugin n'écrit jamais lui-même (leçon 05-M5).
- `contribute` est une fonction pure du modèle : parallélisable (modèle
  immuable), rejouable, testable sans E/S.
- Exécution : tri topologique des dépendances en deux passes (leçon B7),
  isolation `Exception | LinkageError` par plugin (leçon 04-H7).
- L'accès à la provenance est typé : `model.classificationOf(typeId)`,
  `model.explain(typeId)` ; identifiants de règles = constantes publiées
  (plus jamais `"contained-entity"` en littéral, leçon 05-H5).

### 6.2 Plugin audit

Des règles d'audit → `Finding`s codés ; les renderers (console, Markdown,
HTML, JSON via Jackson ; diagrammes via la DSL Mermaid commune) ne font que
de la présentation depuis le même `ReportData`. L'inventaire affiche la
**provenance** de chaque classification ; une section « fiabilité » agrège
DECLARED/INFERRED/AMBIGUOUS et les remédiations (06 §5.3). L'audit ne
re-classifie jamais : tout ce dont il a besoin est dans le modèle, sinon le
modèle est étendu (règle d'or outillée par ArchUnit sur les plugins).

### 6.3 Validation = politique du moteur

```yaml
validation:
  failOnUnclassified: true
  minConfidence: HIGH          # gate sur la confiance, ports inclus
  failOnAmbiguous: true
  allowInferred: true          # false = tout doit être DECLARED
  findings:
    HG-DDD-012: BLOCKER        # seuils par code de finding
```

Le verdict s'appuie sur les statistiques de classification **et** les
findings — un seul mécanisme, consommé par `validate` (gate) et affiché par
l'audit (rapport). Chaque refus imprime la remédiation spécifique du type
concerné (leçon : les 3 suggestions génériques de ValidateMojo).

### 6.4 Génération

Seuil de certitude par plugin (`generation.minConfidence`, défaut HIGH) :
sous le seuil, diagnostic + remédiation au lieu de code faux. Le plugin JPA
lit la composition (`CompositionIndex`) et les identifiants (`DomainIndex`)
depuis le modèle — zéro re-dérivation nominale (leçons 05-H4/H5).

---

## 7. Choix d'outillage

| Besoin | Choix | Justification | Écarté |
|---|---|---|---|
| Parsing source | **Spoon 11** (déjà utilisé), classpath renseigné, delombok en amont | modèle source complet, positions, annotations avec valeurs ; le remplacer n'apporterait rien | javac Tree API (modèle pauvre), JDT (lourd), JavaParser (résolution faible) |
| Types externes du classpath | **ClassGraph** (ou ASM nu) | fermeture de supertypes des dépendances sans leurs sources, en millisecondes | parser les JARs avec Spoon (coût) |
| Algorithmes de graphe | structure maison immuable + **JGraphT** pour SCC/topologie | Tarjan éprouvé plutôt que DFS maison (B10) | Neo4j, TinkerPop |
| Moteur d'inférence | **maison** (~500 l., semi-naïf, preuves) | déterminisme, zéro dépendance, arbre de preuve natif | Drools, Soufflé (§4.4) |
| Codegen Java | **com.palantir.javapoet** | fork maintenu de JavaPoet (archivé par Square) ; API identique | templates texte pour du Java |
| Config | **snakeyaml-engine** + binding typé strict (clé inconnue = erreur) | un YAML malformé ou une clé inconnue ne doit jamais passer silencieusement (leçon MojoConfigLoader) | SnakeYAML 1.x permissif |
| JSON (rapports, golden) | **Jackson** (sortie canonique ordonnée) | échappement correct (leçon : JsonReportRenderer manuel, 834 l.) | sérialiseur maison |
| HTML/Markdown/Mermaid | DSL interne commune (promue de living-doc) ; JTE si le HTML se complexifie | peu de dépendances, échappement centralisé (leçon 05-H6) | FreeMarker, Velocity |
| Tests | JUnit 5, **ArchUnit** (auto-application), **jqwik** (propriétés : déterminisme, monotonie, idempotence du point fixe), PIT (déjà), harnais golden + corpus 3 profils, **testkit publié** | la testabilité de la classification est une fonctionnalité produit | |
| Qualité interne | Error Prone + NullAway, Palantir format (déjà), enforcer bannedDependencies | attraper les nullités et imports interdits à la compilation | |

---

## 8. Performance et incrémentalité

- **Une traversée AST par corps de méthode** (fusion des ~12 passes
  actuelles, A10), et uniquement si la capacité « body facts » est demandée.
- **Cache par module** : hash (source-set + version des packs + config) →
  ArchModel sérialisé réutilisé dans le réacteur ; un module inchangé ne se
  re-analyse pas.
- Le moteur travaille sur des index précalculés ; aucune requête quadratique
  sur les listes d'arêtes (leçon 04-H8 : `containsEdge` O(E)).
- Benchmarks JMH conservés (corpus small/medium/large), avec **seuils
  absolus en CI** (aujourd'hui : aucun seuil).

---

## 9. L'expérience utilisateur qui change la donne

- `hexaglue explain com.acme.Order` : « AGGREGATE_ROOT (HIGH, INFERRED) —
  parce que OrderRepository extends JpaRepository<Order, OrderId> [S2,
  spring-data] ; champ identité OrderId [S3, R2] ; … ». L'arbre de preuve
  rend le moteur auditable par l'utilisateur — c'est la réponse définitive à
  l'opacité des conventions de nommage.
- Diagnostics et findings codés, documentés sur le site, chacun avec sa
  remédiation.
- CLI autonome pour essayer HexaGlue sur un projet **sans toucher au POM**
  (adoption), et pour scripter l'audit en CI hors Maven.
- Un seul rapport multi-format (console/MD/HTML/JSON) depuis le même modèle
  de findings : plus de divergences entre renderers.

---

## 10. Ce que la page blanche interdit (anti-leçons gravées)

1. Publier une abstraction sans second consommateur réel (03-C1).
2. Un pivot `String` entre deux étages du pipeline (03-H3).
3. `catch (Exception)` global rendant un modèle vide « valide » (04-H6).
4. Matcher une annotation par nom simple (B3).
5. Un plugin qui re-dérive une information du modèle (05, partout) — tenu
   par ArchUnit/PMD, pas par la discipline.
6. Deux implémentations vivantes du même concept (confiance, TypeRef,
   Mermaid, bounded contexts…).
7. Du code mort publié sur Maven Central « pour plus tard » (01).
8. Une décision de classification sans évidence tracée (02-H6).
9. Une règle de nommage hors du vocabulaire configurable (02-H2).
10. Un échec de construction avalé sans diagnostic (B5).

---

## 11. Du réel vers la cible : pourquoi ne pas réécrire

L'actif conservable est substantiel — le socle conceptuel (`ArchType` scellé,
indexes, pipeline unidirectionnel) est sain (verdict de l'audit), et une
partie du chemin est déjà décidée (phases 0-5). Correspondance :

| Composant cible (ce document) | Actif existant | Chemin |
|---|---|---|
| `hexaglue-model` (contrat pur) | `hexaglue-arch` | Phase 3 (typage pivot) + trace remplie (06 §5.1) |
| `hexaglue-frontend` sans abstraction spéculative | `core/frontend` + D1 option B | Phase 4 amendée ; + stubs externes, nested, valeurs typées (G1-G4) |
| Base de faits / graphe | `ApplicationGraph` + indexes | extension, pas remplacement |
| Moteur à saturation (§4.1) | `SinglePassClassifier` + criteria | remplacé ; les criteria deviennent des règles S2/S4 — **c'est la décision D6, option A** |
| Packs de connaissance (§5) | listes `AnchorDetector`/`LayerClassifier`/plugins | Phase 2 élargie (FrameworkKnowledge, 06 §3.3) |
| Findings unifiés + gates (§6.3) | `Finding`s du plugin audit + ValidateMojo | consolidation, D6-D11 |
| Sinks + commons rendu (§6.1) | `CodeWriter` + DSL Mermaid living-doc | Phase 5 (`hexaglue-plugin-commons`) |
| Testkit + corpus 3 profils | fixtures inline + golden actuels | D11 |

Une réécriture repartirait de zéro sur les 15 bugs déjà localisés, le corpus
de tests (~8 700 lignes sur la seule classification) et l'écosystème publié —
pour un gain principalement esthétique sur les modules périphériques. La page
blanche vaut comme **cible de convergence** : chaque phase du chantier doit
rapprocher le code d'un des composants ci-dessus, et tout écart nouveau par
rapport aux interdits du §10 est un défaut de revue.
