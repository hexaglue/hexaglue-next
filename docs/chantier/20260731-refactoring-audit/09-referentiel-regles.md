# Référentiel des règles — identification des rôles et conformité hexagonale/DDD

Date : 2026-08-02.
Objet : l'ensemble des règles que le moteur v7 doit mettre en place pour
répondre aux deux questions du produit, sur les deux situations qu'il vise.
Ce document précise et amende le §3 du doc 06 ; il est déclenché par deux
constats de la session « solveur » :

1. **Le nommage est retombé en position structurante.** Mesure sur le corpus
   profil 1 : 39 des 118 verdicts rendus (33 %) reposent sur le seul suffixe,
   dont 3 faux. C'est l'écueil n°1 de l'ancien moteur (doc 06 §2.1),
   reproduit à un étage plus propre.
2. **Les scénarios mono-type sont des questions sans réponse.** 73 des 122
   scénarios récoltés posent au moteur un type unique, isolé de tout
   contexte d'usage, et 33 autres n'en câblent que deux. Une interface seule ne peut pas être classifiée : son rôle est
   une position dans un graphe, pas une propriété de sa déclaration.

Conséquences actées ici : le vocabulaire de nommage (S6) et le nommage de
packages (moitié « conventions » de S5) sortent de la posture par défaut
jusqu'à la fin de M3, où leur apport résiduel sera mesuré ; le corpus est
réorienté vers des scénarios contextuels.

---

## 1. Les deux questions, et leur séparation stricte

Le produit répond à deux questions, dans cet ordre :

- **Q1 — identification** : quel rôle architectural cette classe/interface
  joue-t-elle ? (classification : AGGREGATE_ROOT, DRIVEN_PORT, ...)
- **Q2 — conformité** : ce rôle est-il joué selon les principes
  hexagonaux/DDD ? (findings : pureté, direction des dépendances, ...)

**Règle de séparation : Q1 tolère ce que Q2 condamne.** Un agrégat annoté
`@Entity` et mutable reste un agrégat (Q1) ; le couplage et la mutabilité
sont des findings (Q2). Si l'identification exige la conformité — un VO
« doit » être un record, un port « doit » être dans `ports.out` — alors le
code non conforme devient invisible, et l'audit n'a plus rien à auditer.
C'est la généralisation de D7 (l'annotation JPA n'est jamais une évidence de
kind, elle n'alimente qu'un finding) à toutes les règles.

Corollaires :

- Aucune règle d'identification ne pose une exigence de conformité comme
  condition dure. La conformité peut *corroborer* (l'immutabilité appuie une
  lecture VO), jamais conditionner.
- Q2 ne re-classifie jamais : elle consomme les verdicts de Q1 et le
  registre de connaissance, et produit des findings codés (`HG-XXX-NNN`)
  filtrés par les gates (`ValidationConfig.findingThresholds`).

## 2. Un seul jeu de règles pour deux situations

Les deux situations visées ne sont pas deux modes du moteur :

| Situation | Densité d'ancres | Ce qui change |
|---|---|---|
| **Migration** (entreprise standard : JPA, Spring Data, `@Service`, contrôleurs) | Forte : les frameworks marquent chaque anneau | Les findings Q2 sont nombreux et forment le chemin de remédiation |
| **Hexagonal existant** (cœur sans framework, adapters en périphérie) | Faible au centre, forte au bord : les ancres sont dans les adapters et la config | Les findings Q2 doivent être rares ; l'identification remonte du bord vers le centre |

Même moteur, mêmes règles, mêmes findings : seules diffèrent la densité
d'ancres que le code offre et la sévérité que les gates appliquent. Il n'y a
pas de « profil » à configurer (D7 l'a déjà refusé pour JPA).

**L'unité d'analyse est l'application, pas la classe.** Le moteur a besoin de
voir au moins un consommateur ou un implémenteur pour prononcer un rôle de
frontière. Un périmètre réduit au domaine seul produira des UNCLASSIFIED
honnêtes avec candidats — c'est la réponse correcte, et `explain` doit dire
« aucun contexte d'usage dans le périmètre ».

## 3. Les ancres (vague 0) — ce qui parle sans contexte

| Ancre | Source | Ce qu'elle établit | État |
|---|---|---|---|
| A1 | S1 — intention déclarée (`classification.explicit`, jMolecules FQN exact) | Le kind, EXPLICIT | Livré (`ConfiguredKind`, `DeclaredKind`, pack jmolecules) |
| A2 | S2 — connaissance des frameworks (packs) | Des **faits techniques**, jamais un kind : `SPRING_DATA_REPOSITORY(subject,id)`, `DRIVING_ENTRYPOINT`, `INFRA_DEPENDENCY`, `APPLICATION_STEREOTYPE`, `PERSISTENCE_MODEL`, `TECHNICAL`, `NEUTRAL` | Livré (`hexaglue-knowledge`, `AssertKnowledge`) ; `GENERATED_CODE` est déclaré mais sans sujet possible (D15) |
| A3 | S4 — forme des valeurs | Candidats VO/IDENTIFIER (record, immutabilité, wrapper mono-valeur) — **candidat, pas verdict** : le wrapper émet les deux lectures | Livré (`LocalShape`) |
| A4 | S5 — **structure** de modules (dépendances inter-modules, `ModuleRole` déclaré par l'hôte) | Module candidat domaine = ne dépend d'aucun module interne ni d'infra | À faire (M5 pour le rôle hôte) |

Sortent des ancres : le nommage de types (S6) et le nommage de packages
(`ports.in`, `.domain.`) — ce sont des conventions, différées (§6). S5 est
donc scindé : sa moitié structurelle (dépendances de modules) reste une
ancre ; sa moitié conventionnelle (mots dans les packages) suit le sort du
nommage.

## 4. Q1 — la dérivation par vagues

Le rôle se lit du bord vers le centre : les frameworks marquent le bord, les
ports sont les trous du mur, l'application les traverse, le domaine est ce
que les ports gèrent. Chaque vague consomme les verdicts des précédentes —
c'est exactement la boucle de `Classifier` (verdicts du tour précédent
disponibles dans `Derivation`).

Les règles ci-dessous remplacent et étendent R1-R8 du doc 06 §3.2. Notation :
`[*]` = mord sur les deux situations, `[M]` = surtout migration, `[H]` =
surtout hexagonal existant.

### Vague 1 — l'anneau extérieur (adapters), depuis les ancres

| Règle | Si... | Alors évidence... | État |
|---|---|---|---|
| **W1-DA** `[*]` | le type porte `DRIVING_ENTRYPOINT` (contrôleur, listener, `@ControllerAdvice`) | DRIVING_ADAPTER, S2 | **livré** (`FrameworkEntryPoint`) |
| **W1-DR** `[*]` | le type porte ≥1 `INFRA_DEPENDENCY` (champ/param `JdbcTemplate`, `EntityManager`, client HTTP...) ou étend un template framework | DRIVEN_ADAPTER, S2 | **livré** (`InfrastructureDependency`) |
| **W1-DR2** `[*]` | le type implémente une interface déjà DRIVEN_PORT (vague 2, tours suivants) et n'est pas lui-même du cœur | DRIVEN_ADAPTER, S3 | **livré** (`PortImplementation`) |

Cas particulier `[M]` : le dépôt Spring Data n'a **pas** d'adapter dans les
sources (le framework le génère). L'absence d'implémentation interne d'un
port est donc un état normal, jamais un motif de silence.

### Vague 2 — la frontière (ports), depuis l'anneau et le cœur

Un port se reconnaît à qui l'implémente et qui le consomme — jamais à son
nom :

| Règle | Si... | Alors évidence... | État |
|---|---|---|---|
| **R1** `[M]` | `I extends` Spring Data `Repository<A,ID>` | I=DRIVEN_PORT S2 ; A=AGGREGATE_ROOT S2 ; ID=IDENTIFIER S2 | **livré** (`RepositorySubject`) |
| **R4** `[*]` | interface I consommée (champ / param de constructeur) par un type du cœur (APPLICATION_SERVICE ou DOMAIN_SERVICE des tours précédents ; en amorçage : par un type non-adapter) ET aucune implémentation interne au cœur | I=DRIVEN_PORT, S3 | **livré** (`ConsumedContract`) |
| **R5** `[*]` | interface I implémentée par un type du cœur ET détenue/appelée par un DRIVING_ADAPTER (structurel : champ/param ; renforcé par `INVOKES` si `METHOD_BODIES`) | I=DRIVING_PORT, S3 | **livré** (`ExposedContract`, D14) |
| **R5b** `[*]` | interface I implémentée par un type du cœur ET **détenue par aucune déclaration du périmètre** (ni champ, ni paramètre de constructeur) : son appelant est donc dehors | I=DRIVING_PORT, **S4** | **livré** (`OfferedContract`, D38) — palier sous R5 : l'anneau qui parle vaut mieux que l'anneau qui manque. Lit un hexagone dont la couche web n'existe pas encore, que R5 ne peut pas voir par construction. Un contrat que le cœur garde pour lui est exclu par la même clause : le côté appelant le détient |
| **R9** `[*]` | port pilotant P dont les signatures **nomment exactement un** AGGREGATE_ROOT (les deux côtés confondus, conteneurs déballés) | relation `CONCERNS(P, A)` — aucun kind dérivé | **livré** (`ExposedAggregate`, D35). **Ne calque PAS la convergence de W2-ROLE** : un port piloté reçoit l'agrégat pour le garder donc converge, un port pilotant reçoit une identité et rend l'agrégat — mesuré sur six ports réels, l'intersection est vide sur les six. Zéro ou deux agrégats = silence, et le backend dit qu'il n'a pas de ressource |
| **W2-X** `[*]` | interface I implémentée **et** consommée par le cœur | I n'est **pas** un port : contrat interne → candidat DOMAIN_SERVICE, sinon silence | **livré** comme condition partagée (`Contracts`), sans dérivation ; la candidature DOMAIN_SERVICE revient à R8 |

**Anti-règle : toute interface n'est pas un port.** Sans consommateur ni
implémenteur dans le périmètre, une interface reste UNCLASSIFIED avec ses
candidats — c'est le verdict correct des scénarios mono-type.

La direction est purement relationnelle : le cœur consomme → DRIVEN ; le
cœur implémente et le bord appelle → DRIVING. Plus aucun départage par
suffixe ou par package (corrige B12/H5 par construction).

**Sous-type du port piloté** (REPOSITORY/GATEWAY/EVENT_PUBLISHER), par la
forme des signatures, pas par les noms de méthodes — **livré**
(`PortSignatures`, fait `PortRole`, relation `MANAGES` sur le sujet) :

- *rôle repository* : les signatures convergent sur un sujet T du périmètre
  (T revient en retour — nu, `Optional<T>`, collection — et en paramètre) ;
- *rôle event-publisher* : méthodes unidirectionnelles (void) prenant des
  types immuables du périmètre ;
- *rôle gateway* : le reste (signatures dominées par des types externes ou
  primitifs).

### Vague 3 — l'application, entre les ports

| Règle | Si... | Alors évidence... | État |
|---|---|---|---|
| **R6** `[*]` | classe C implémente un DRIVING_PORT et/ou consomme ≥1 DRIVEN_PORT | C=APPLICATION_SERVICE, S3 (pivot) | **livré** (`PortPivot`) |
| **R6b** `[M]` | classe C appelée/détenue par un DRIVING_ADAPTER et consommant des DRIVEN_PORT ; `APPLICATION_STEREOTYPE` corrobore, ne décide pas | C=APPLICATION_SERVICE, S3 | **livré** (`AdapterCollaborator`) — détenue par l'anneau est requis, le port consommé **ou** le stéréotype complète |
| **R8** `[*]` | classe sans état (ou sans état mutable), sans dépendance port/infra, opérant sur ≥2 types du domaine, consommée par le cœur, **et dont aucun agrégat n'est fait** (lot 20) | DOMAIN_SERVICE, S3 | **livré** (`DomainCollaboration`) — l'infra est couverte par la garde d'anneau |

COMMAND_HANDLER/QUERY_HANDLER : sans vocabulaire et sans faits de corps, la
distinction n'est atteignable que par la forme du port implémenté (port
mono-méthode ; mutation vs lecture indécidable structurellement). Assumer :
kinds en retrait jusqu'à ce que S6 soit réévalué ou `METHOD_BODIES` présent.

Classes abstraites (arbitré au lot 19) : lues comme n'importe quelle classe.
Une classe abstraite qui détient des ports joue le rôle applicatif autant que
la sous-classe qui la complète ; retenir la lecture dirait quelque chose sur la
factorisation du code, pas sur le rôle.

### Vague 4 — le domaine, depuis son cycle de vie

| Règle | Si... | Alors évidence... | État |
|---|---|---|---|
| **R1** `[M]` | (capture Spring Data) | A=AGGREGATE_ROOT, ID=IDENTIFIER, S2 | **livré** |
| **R1b** `[H]` | T est le **sujet des signatures** d'un DRIVEN_PORT à rôle repository (vague 2) | T=AGGREGATE_ROOT, S3 | **livré** (`ManagedAggregate`) — lit la relation `MANAGES`, la seule source des deux vagues |
| **R2** `[*]` | A agrégat confirmé ; son champ dont le type est un wrapper du périmètre sert de clé aux méthodes du port qui **retrouvent au plus un** A (`A` ou `Optional<A>` — une méthode qui rend `List<A>` filtre, elle n'élit pas) | ce type=IDENTIFIER, S3 — résout le duel S4 IDENTIFIER/VALUE_OBJECT | **livré** (`LookupIdentity`) — exactement une clé après départage, sinon silence ; à plusieurs candidates, une clé déjà identité d'un **autre** agrégat s'écarte (D32 — départage seulement, jamais de veto sur une clé unique) ; émet aussi `IDENTIFIED_BY` |
| **R3a** `[*]` | T interne possédé par composition (champ / élément de collection) depuis un agrégat/entité, et T porte un champ IDENTIFIER | T=ENTITY, S3 | **livré** (`OwnedEntity`) |
| **R3b** `[*]` | même possession, sans identité propre | T=VALUE_OBJECT, S3 — **même si T est mutable** (la mutabilité devient un finding, jamais un obstacle) | **livré** (`OwnedValue`) |
| **R7** `[*]` | type immuable publié par un port à rôle event-publisher, ou retourné par une méthode d'un agrégat | DOMAIN_EVENT, S3 | **livré** (`PublishedEvent`) — « retourné » = **sans être gardé** dans l'état, sinon tout accesseur annoncerait |

Ce que la composition **ne dit pas** (livré comme condition partagée, `Lifecycle`) :
un contrat n'est la partie de personne (tenir un port est une question de couche,
pas une composition), et une valeur écrite comme une identité — immuable autour
d'une seule chose — est laissée entière à R2 : la composition ne sait pas
distinguer l'identité de l'agrégat d'une valeur voisine, les deux étant des
champs. Corollaire arbitré au lot 20 : **une partie d'agrégat n'est jamais un
DOMAIN_SERVICE** (R8), le même champ étant une composition vu d'un bout et une
collaboration vu de l'autre.

C'est ici que la séparation Q1/Q2 paie sur la migration : le `Customer`
annoté JPA, mutable, aux setters partout, est reconnu AGGREGATE_ROOT par R1
(Spring Data le gère) — puis Q2 émet « domaine couplé à la persistance »,
« agrégat anémique », etc. L'ancien moteur l'aurait laissé invisible ou
contresens (B3).

### Ce qui reste UNCLASSIFIED, et comment

Tout type du périmètre sans verdict reçoit une catégorie (`UnclassifiedType`
la porte déjà) : `TECHNICAL` vient du fait S2 du même nom, `AMBIGUOUS` de
l'agrégateur (candidats sans marge), `UNKNOWN` du vide — avec pour raison
« aucun contexte d'usage dans le périmètre » et pour remédiation : élargir le
périmètre, câbler le type, ou le déclarer (S1). Aucun changement de contrat
modèle nécessaire.

**Le code généré n'est pas de ceux-là (D15).** Il n'entre pas dans le modèle :
le frontend l'écarte à la lecture, donc le moteur n'en voit aucun et n'a rien
à catégoriser. La raison est mesurée — un adapter généré remis dans le
périmètre implémente le port que son auteur a écrit, ce qui fait tomber le port
puis le service qui l'appelle, et la seconde exécution sur des sources
inchangées ne rend plus le même modèle. Reconnaître le code généré est une
question de **périmètre de lecture**, réglée avant qu'un fait soit énoncé. Le
fait S2 `GENERATED_CODE` et les entrées de pack correspondantes restent
déclarés mais sans sujet possible : ils sont le prix d'une option, si M6 décide
que l'inventaire du code écarté fait partie du rapport. Dire *combien* a été
écarté relèvera alors du canal de diagnostics du frontend (M5), pas d'un
verdict.

## 5. Q2 — le référentiel de conformité

Mêmes règles pour les deux situations ; seuls les seuils diffèrent
(`findingThresholds`). Familles :

| Famille | Findings types | S'appuie sur |
|---|---|---|
| **HG-DEP** — direction des dépendances | domaine → application/adapter/infra interdit ; application → adapter interdit ; adapter → adapter interdit | verdicts Q1 + arêtes du graphe |
| **HG-PURE** — pureté du cœur | type du domaine portant `PERSISTENCE_MODEL` (D7) ; type du cœur portant `INFRA_DEPENDENCY` ; annotation framework non-`NEUTRAL` sur le domaine | faits S2 (le registre remplace les préfixes en dur des validators de la carrière) |
| **HG-PORT** — discipline des ports | port qui n'est pas une interface ; driven port implémenté par le cœur (fausse frontière) ; signatures de port exposant des types framework (`Page`, `ResponseEntity`, `EntityManager`) ; driving adapter court-circuitant les ports | verdicts + signatures + registre |
| **HG-AGG** — discipline des agrégats | référence directe inter-agrégats (au lieu de l'identifiant) ; entité possédée par deux agrégats ; repository sur une non-racine ; agrégat sans identité | verdicts + composition |
| **HG-VAL** — discipline des valeurs | VO/IDENTIFIER mutable ; VO à sémantique d'identité | verdicts + structure |
| **HG-APP** — discipline applicative | service applicatif avec état ; domaine anémique (agrégat sans comportement, logique dans les services — nécessite au moins la forme des méthodes, complet avec `METHOD_BODIES`) | verdicts + structure |

Sur la **migration**, ces findings *sont* le livrable : l'inventaire des
écarts, priorisé par sévérité, avec provenance — le chemin de remédiation
vers l'hexagone. Sur l'**existant hexagonal**, les mêmes findings doivent
sortir vides ; chaque exception est une régression d'architecture que
`validate` peut bloquer.

Points de style assumés configurables (pas de règle dure) : ports dans le
domaine vs dans l'application ; adapters driving appelant le domaine en
lecture. À instruire quand M6 écrira les findings.

## 6. Le nommage : différé, puis mesuré

Ce qu'il resterait à S6 une fois le référentiel en place :

1. départager IDENTIFIER/VALUE_OBJECT d'un wrapper **non possédé** (R2 le
   fait mieux dès qu'un agrégat existe) ;
2. étiqueter COMMAND_HANDLER/QUERY_HANDLER ;
3. conclure sur les types **définitivement isolés** — précisément là où le
   silence est la réponse honnête.

Protocole de réévaluation, fin M3 : corpus 3 profils + exemples réels,
vocabulaire éteint vs allumé ; pour chaque écart, compter *gain* (le nom
aurait rejoint l'arbitrage humain) et *dégât* (il l'aurait contredit — les 3
faux mesurés sur profil 1 sont des dégâts). Le vocabulaire ne revient en
posture par défaut que si le gain domine nettement sur les profils 2-3. Le
code (`ConventionalName`, `ClassificationConfig.namingSuffixes`) reste livré
et testé : c'est la posture par défaut qui change, pas la capacité.

**Mesuré au lot 23 (2026-08-03) — le vocabulaire reste opt-in.** Harnais
`NamingVocabularyTest`, rapport commité en golden : **0 gain** sur les trois
profils ; **55 dégâts sur le profil 1**, tous de la forme
`UNCLASSIFIED → un kind` ; **aucun mouvement sur les profils 2 et 3**, ceux-là
mêmes que la clause désignait. Les trois résidus listés ci-dessus sont couverts
par la mesure : S6 tranche bien le duel du wrapper non possédé (1) et atteint
bien COMMAND/QUERY_HANDLER (2), mais dans les deux cas contre l'arbitrage relu ;
et (3) est exactement la population des 55. Formulation qui fait foi :
[DECISIONS.md](DECISIONS.md), D13.

Le nommage de packages suit le même sort : la moitié conventionnelle de S5
est différée avec S6, la moitié structurelle (modules) reste au plan.

## 7. Conséquences sur le corpus

1. **Les scénarios mono-type deviennent des fixtures de silence honnête** :
   attente `UNCLASSIFIED`, qui épinglent l'anti-règle « toute interface
   n'est pas un port ». Les deux scénarios relus au lot 14
   (`interfaceWithoutMarkersIsUnclassified`, `shouldHandleInterfaceTypes`)
   sont ré-arbitrés dans ce sens — retour à l'attente de la carrière, pour
   la bonne raison cette fois. Le plancher du cliquet est recompté.
2. **Le poids du corpus bascule sur des scénarios contextuels** :
   mini-applications câblées (port + consommateur + sujet + adapter), à
   récolter dans les golden files multi-types de la carrière
   (`createBankingExample`, ...), les `examples/` et les étapes des case
   studies — plus les profils 2 et 3 du doc 06 §6 (D11).
3. Les brouillons mono-type restants se relisent vite : la plupart
   deviennent `UNCLASSIFIED` attendu.
4. Optionnel, au moment des lots R4/R5 : claims porteuses de confiance
   (`= DRIVEN_PORT@HIGH`) pour mesurer la montée MEDIUM→HIGH quand une règle
   relationnelle prend le relais d'un signal faible.

## 8. Conséquences sur le registre

- **D13 — dissoute par retrait de sa prémisse.** La question « que répond le
  moteur quand seul un signal faible parle » supposait le nommage en posture
  par défaut. Réponse retenue : il n'y a plus de signal faible par défaut ;
  quand rien ne parle, UNCLASSIFIED catégorisé avec candidats et
  remédiation. Reprise (bornée comme prévu au registre) : retirer les
  suffixes de `ClassificationConfig.defaults()` (le vocabulaire devient
  opt-in), ré-arbitrer les deux attentes, recompter le plancher.
  `Aggregator.decideOne` est inchangé (le chemin du silence existe).
- **D14 — absorbée par R5** : signal structurel (option A) comme base,
  `INVOKES` en renfort quand `METHOD_BODIES` est présent. Les deux niveaux
  sont des évidences S3 du même port, la capacité en ajoute, elle ne
  conditionne pas.
- Le plan de M3 se ré-étalonne sur les vagues : W1 (adapters) → R4/R5 (ports)
  → R6/R8 (application) → R1b/R2/R3/R7 (domaine), chaque vague avec ses
  scénarios contextuels au corpus.
