# Plan de construction — Réécriture ancrée (v7)

Date : 2026-08-01. Statut : **exécutoire** (décision D12 confirmée).
Ce plan remplace les phases 0-5 du chantier. Spécification : docs
[06](06-classification-metier.md) (cible fonctionnelle) et
[07](07-architecture-page-blanche.md) (architecture). Le présent document
décrit *comment* construire : jalons, récolte, tests de régression, gate de
parité, risques.

---

## 1. Principes d'ancrage

Ce qui distingue cette réécriture d'une feuille blanche nue — et qui
neutralise les modes d'échec classiques des réécritures :

1. **La spécification existe** : docs 06 + 07, amendés par le registre.
   Les questions de conception se tranchent au registre au fur et à mesure
   qu'elles se posent, sur mesure du code plutôt qu'à l'avance : D7/D8/D10 le
   2026-08-02, D13-D15 pendant M3, D17 à l'ouverture de M4, D18-D22 pendant M5.
   **Une seule reste en attente : D16** (à trancher au plus tard à M6). En cas
   d'écart entre un doc et le registre, le registre l'emporte.
2. **L'acceptation est exécutable** : corpus à 3 profils (D11) + exemples
   d'intégration + golden files. Le « fini » n'est pas une opinion.
3. **L'ancien réacteur `hexaglue/` est une carrière en lecture seule** :
   référence de comportement et source de récolte, jamais modifié.
4. **Toute transplantation arrive avec ses tests** (transplantés ou écrits)
   dans le même lot — on ne récolte pas du code nu.
5. **B1-B15 deviennent des tests de régression** du nouveau code (§5) :
   la connaissance des bugs est capturée, pas re-corrigée en place.
6. **Gate de parité avant toute release** (§6) : la 7.0.0 ne sort que si le
   nouveau moteur fait au moins aussi bien que l'ancien sur l'existant.
7. **Périmètre gelé** : fonctionnalités actuelles + cible 06/07, rien
   d'autre avant la 7.0.0. Toute idée nouvelle va au backlog post-7.0.0
   (anti « second système »).

## 2. Emplacement et cycle de vie

- Nouveau réacteur : **`hexaglue-next/`**, à côté de `hexaglue/` dans
  `hexaglue-projects/` (défaut retenu par D12 ; déplaçable en dépôt dédié
  sans impact sur le plan). Dépôt git propre, conventions existantes
  reprises (Palantir format, commits sémantiques une ligne, pas
  d'attribution).
- À la bascule (M8) : le réacteur v7 prend l'identité publiée (groupId,
  site, doc), l'ancien code est archivé (branche `legacy/v6`), la carrière
  reste consultable jusqu'à la 7.1 au moins.
- Le gel des releases (D5) et le hook `block-release-commands.sh`
  s'appliquent aux **deux** réacteurs jusqu'au gate de parité.

## 3. Jalons M0-M8

| Jalon | Contenu | Récolte principale | Critère de sortie |
|---|---|---|---|
| **M0 — Socle et harnais** | Réacteur `hexaglue-next/`, POM parent, CI, qualité (ArchUnit auto-application, enforcer `bannedDependencies`, Error Prone + NullAway, PIT, formatage), squelette `hexaglue-testkit`, portage du harnais golden, import du corpus profil 1 (fixtures actuelles) | `Makefile`, config CI/qualité, `GoldenFileTest` + `ArchModelSnapshotSerializer`, fixtures inline de `hexaglue-core/src/test` | Build vert ; corpus P1 exécutable (rouge autorisé) ; garde-fous actifs dès le premier commit |
| **M1 — `hexaglue-model`** | CodeModel, ArchModel, `Classification` avec trace complète (07 §3.2), `Finding`/`Diagnostic` codés, config typée stricte | Records de `hexaglue-arch` (ArchType scellé, TypeStructure, indexes) ; design des `AnnotationValue` typés de `hexaglue-syntax-spoon` | Zéro dépendance ; doclint strict ; revue contre 07 §3 |
| **M2 — `hexaglue-frontend`** | Spoon + classpath → CodeModel ; G1-G4 natifs : stubs externes, types imbriqués, valeurs d'annotations typées, fermetures transitives (ClassGraph) ; delombok ; **une** traversée AST par corps (capacité optionnelle) | Adapters Spoon actuels (référence), corpus `hexaglue-benchmarks` | Fixtures dédiées vertes : `extends JpaRepository<A,ID>` visible avec arguments, nested présents, `@Table(name=…)` lisible |
| **M3 — `hexaglue-knowledge` + `hexaglue-engine`** | Base de faits, solveur semi-naïf, strates S0-S4, agrégateur lexicographique, packs jMolecules/Spring/Jakarta, règles seed + propagation R1-R8 (les criteria actuels servent de spécification) ; précédé des **amendements du contrat M1** qu'imposent D7/D8/D10 (adapters dans `ArchKind`, porteur `classification.explicit`, remédiation en FQN) | Listes `AnchorDetector`/`LayerClassifier` → packs ; logique des criteria → règles (jamais copiées telles quelles) | Corpus P1 en **parité** ; P2/P3 verts (nouveaux golden) ; déterminisme prouvé (double run) |
| **M4 — Explain : la restitution** | Rendu du verdict, de ses raisons et de l'arbre de dérivation ; bilan agrégé d'un run. Indépendant de l'hôte : le plugin Maven (M5), le rapport d'audit (M6) et un CLI éventuel consomment le même rendu | — | Restitution golden sur les trois profils ; invariants sur tout le corpus ; sortie stable octet à octet |
| **M5 — L'hôte** | Chargement de configuration en YAML strict (clé inconnue = erreur), gates de validation (politique moteur : `minConfidence`, `failOnAmbiguous`, `allowInferred` effectif, ports inclus), `hexaglue-maven-plugin` mince. **C'est ici que se prennent les décisions d'hôte** : racines de sources (D19), canal de diagnostics (D20), chargement de configuration — et le CLI, s'il est décidé, les reprend au lieu de les préempter (D17). **Le SPI et l'exécution des plugins sont sortis du jalon (D18)** : aucun consommateur avant M6 | `MojoConfigLoader`/`ValidateMojo` comme spécification des clés et des gates | Les `test-param-*` d'hôte portés et verts, écart par écart : **six portés, trois écarts assumés** consignés au journal de clôture et repris au gate de parité (§6) ; chaque gate démontrée par un test |
| **M6 — SPI + living-doc + audit** | **Le SPI en tête de jalon, écrit contre son premier plugin (D18)** : sinks typés, manifest, DAG deux passes, isolation `LinkageError`. **Puis l'analyse réacteur et ce qui en dépend (D21 amendée)** : nom de module dans la requête frontend, canal de rôles en configuration, `ModuleTopology` assemblée, règle S5 structurelle — la règle arrive avec son producteur et son consommateur. Puis module de rendu commun (DSL Mermaid/markdown), findings codés, renderers (JSON via Jackson), règles d'audit transplantées **corrigées** (B1, B7, B8, B9, B10 par construction), section « fiabilité de la classification », provenance dans l'inventaire | `CodeWriter`/`PluginContext` et le `PluginExecutor` (spécification, jamais transplanté) ; `MermaidBuilder` + markdown de living-doc ; contenu des règles et `ReportData` de l'audit | Rapports sur les exemples comparés à l'ancien (chaque écart expliqué) ; `case-study-banking` rejoué |
| **M7 — jpa + rest** | Stratégies de génération transplantées, B2/B15 corrigés et testés, seuil de certitude (`generation.minConfidence`), composition lue du modèle, routage module via sinks | Stratégies JPA/REST, `NamingConventions.toColumnName` + mots réservés SQL, savoir-faire MapStruct/Lombok | Sorties générées golden-diffées sur `examples/` ; compilation des exemples verte |
| **M8 — Parité et bascule** | Gate de parité complet (§6), migration des exemples et tutoriels, pipeline doc-metadata, mise à jour du site (JSON), release **7.0.0** (levée de D5 = décision explicite), archivage de l'ancien réacteur | `scripts/extract-doc-metadata.js` et suite, `RELEASING.md` | Gate §6 vert ; 7.0.0 publiée |

Ordres de grandeur (indicatifs, à ré-étalonner après M3, qui est le cœur) :
M0+M1 ≈ 1 semaine ; M2 ≈ 1 semaine ; M3 ≈ 2-3 semaines ; M4 ≈ 2-3 jours ;
M5 ≈ 1 semaine ; M6 ≈ 2 semaines (le SPI y est entré, D18) ; M7 ≈ 2 semaines ;
M8 ≈ 1 semaine.

Découpage interne de M3 (pour éviter l'enlisement) : identifiants + agrégats
d'abord (R1, R2), puis composition (R3), puis ports (R4, R5), puis
application/services/événements (R6-R8) — le corpus s'allume kind par kind.

## 4. Liste de récolte (carrière `hexaglue/`)

Trois modes : **T** = transplantation (code repris et adapté, avec ses
tests) ; **S** = spécification (le code sert de référence de comportement,
réécrit) ; **R** = ressource (copie directe).

| Actif | Source (carrière) | Destination | Mode |
|---|---|---|---|
| Records du modèle (ArchType scellé, TypeStructure, Field/FieldRole, indexes) | `hexaglue-arch/` | `hexaglue-model` | T |
| `AnnotationValue` typés | `hexaglue-syntax-api`/`-spoon` (mort mais bien conçu) | `hexaglue-model` | T |
| Adapters Spoon | `core/frontend/spoon/` | `hexaglue-frontend` | S |
| Analyse de corps (invocations, complexité) | `core/frontend/CachedSpoonAnalyzer` | `hexaglue-frontend` (1 traversée) | S |
| Logique des criteria (26 vivants) | `core/classification/**` | règles S2/S4 du moteur | S |
| Listes d'annotations frameworks | `AnchorDetector`, `LayerClassifier` | packs `hexaglue-knowledge` | S |
| Harnais golden + sérialiseur | `hexaglue-core/src/test` (`GoldenFileTest`) | `hexaglue-testkit` | T |
| Fixtures de classification (~8 700 l.) | `hexaglue-core/src/test/classification/**` | corpus profil 1 | T |
| Exemples et projets d'intégration | `examples/` (58 projets), `build/integration-tests` | corpus + tests d'intégration v7 | T |
| DSL Mermaid + markdown | living-doc (`MermaidBuilder`, `markdown/`) | module de rendu commun | T |
| Contenu des règles d'audit + `ReportData` | plugin audit | règles findings M6 | S |
| Stratégies de génération JPA/REST, `NamingConventions` (mots réservés SQL), savoir MapStruct/Lombok | plugins jpa/rest | plugins v7 M7 | T |
| Clés de config + sémantique des goals | `MojoConfigLoader`, Mojos | config typée + maven-plugin v7 | S |
| Makefile, CI, config qualité, PIT | racine `hexaglue/` | `hexaglue-next/` | R |
| Pipeline doc-metadata | `scripts/*.js`, `docs/generated-metadata` | M8 | T |
| Case studies (projets externes) | `case-study-*` | rejeu au gate de parité | R |

**Non-récolte (interdits de transplantation)** : `SinglePassClassifier` et sa
cascade, `ClassificationTraceConverter`, les heuristiques de
`DefaultArchitectureQuery`, `PluginExecutor` (dispatch `instanceof`),
`JsonReportRenderer` manuel, tout l'inventaire du doc
[01-code-mort.md](01-code-mort.md), `arch/builder/**`,
`SyntaxProvider`/`TypeSyntax` (hors `AnnotationValue`),
`ProgressiveClassifier` (seule l'idée multi-passes survit, dans le solveur).

## 5. B1-B15 → tests de régression du nouveau code

| Bug (carrière) | Traitement en v7 | Jalon |
|---|---|---|
| B1 SDP inversé | Implémentation correcte + test canonique A(I=0)→B(I=1) | M6 |
| B2 mots réservés SQL | `toColumnName` transplanté + test paramétré des 22 mots | M7 |
| B3 annotation par nom simple | Impossible par construction (packs FQN/préfixe) + test `@jakarta.persistence.Entity` ≠ évidence DDD | M3 |
| B4 kind perdu (jMolecules) | Le fait porte le kind + test `implements ValueObject` → VALUE_OBJECT | M3 |
| B5 exception avalée | Échec bruyant + test « échec de construction ⇒ diagnostic codé » | M3 |
| B6 valeurs d'annotations jetées | G2 natif + test `@Table(name)` lisible dans ArchModel | M2 |
| B7 NPE tri topologique | Deux passes + test plugins déclarés en ordre inverse | M6 (avec le SPI, D18) |
| B8/B9 bounded contexts | Implémentation unique relative au basePackage + test multi-contextes | M6 |
| B10 cycles exponentiels | Tarjan + test cycles dédupliqués | M6 |
| B11 état mutable criteria | Sans objet (règles pures) + test de ré-entrance du solveur | M3 |
| B12 collision de priorités | Pesée avec direction + test `CreateOrderUseCase` injecté → DRIVING sans conflit | M3 |
| B13 `hasPortAnnotation` mensonger | Faits décomposés + test `PortfolioService`/package `reporting` non-ports | M3 |
| B14 `formatted` sur mauvais littéral | Règle Error Prone/PMD au socle | M0 |
| B15 défaut `idStrategy` | Config typée stricte + test valeur invalide → erreur claire | M7 |

## 6. Gate de parité (critère de bascule M8)

1. **Corpus profil 1** : classifications identiques à l'ancien moteur, ou
   écarts **améliorants justifiés un par un** (fichier d'écarts versionné).
2. **`examples/` + `test-param-*`** : tous verts ; sorties générées
   équivalentes (golden diff ; écarts justifiés — p. ex. B2 corrigé change
   légitimement un nom de colonne). **Trois écarts sont déjà connus et
   attendus**, constatés à la clôture de M5 : `validation-report-path` (la
   validation rend un verdict et des logs, pas un fichier — le rapport est le
   produit de l'audit), `classification-exclude` (la carrière excluait des
   types par motif `*.shared.*` ; `AnalysisScope` refuse les globs et n'exclut
   que par préfixe de package, donc **aucune exclusion type par type en v7** —
   c'est celui des trois qui peut valoir une fonctionnalité, à trancher ici),
   et `skip-validation` (sans objet : un seul goal).
3. **Case studies rejoués** : rapports cohérents ; les écarts dus aux bugs
   corrigés (B1 : score dependency ; B8 : bounded contexts) sont **attendus
   et documentés** — ils serviront à régénérer les pages du site.
4. **Performance** : corpus large des benchmarks ≤ 1,5× le temps de
   l'ancien moteur (seuil CI).
5. **Déterminisme** : double exécution → sorties identiques octet à octet.
6. **Doctrine** : les interdits du doc 07 §10 sont vérifiés outillés
   (ArchUnit/PMD/enforcer), pas à la main.

## 7. Risques et parades

| Risque | Parade |
|---|---|
| Second système (le périmètre enfle) | Périmètre gelé (§1.7) ; backlog post-7.0.0 ; revue de jalon = conformité 06/07, pas d'ajouts |
| Enlisement sur M3 (le cœur) | Découpage kind par kind (§3) ; corpus incrémental ; ré-étalonnage du plan après M3 |
| Perte de savoir enfoui dans le vieux code | Récolte **avec tests** ; carrière conservée en lecture seule jusqu'à 7.1 ; docs 01-05 comme carte du savoir |
| Longue période sans démontrable | Jalons courts, chacun se termine par quelque chose d'exécutable ; le corpus d'acceptation est le démontrable permanent, et la restitution de M4 le rend lisible sans hôte |
| Site et case studies couplés au format des rapports | Contrat de sortie doc-metadata stabilisé seulement à M8 ; pages du site retouchées après le gate (écarts B1/B8 attendus) |
| Deux réacteurs qui divergent | Interdiction de modifier la carrière (règle de conduite 10) ; toute trouvaille sur l'ancien code → Découvertes de CHANTIER.md |

## 8. Règles de conduite spécifiques

Complètent les règles 1-9 de [CHANTIER.md](CHANTIER.md) (qui restent en
vigueur pour le nouveau réacteur) :

10. L'ancien réacteur `hexaglue/` est en **lecture seule** (référence et
    carrière). Aucune modification, sauf demande explicite de l'utilisateur.
11. Toute transplantation arrive **avec ses tests** dans le même lot.
12. Périmètre gelé jusqu'à M8 : fonctionnalités actuelles + cible 06/07.
    Toute idée nouvelle → backlog post-7.0.0.
13. Chaque jalon se clôt corpus vert + revue contre les interdits du
    doc 07 §10 ; le journal de CHANTIER.md consigne l'état du jalon.
