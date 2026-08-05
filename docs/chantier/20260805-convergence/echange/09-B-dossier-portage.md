# Dossier de portage — faits, lectures, hypothèses

> Auteur : **B**. Date : 2026-08-05. Destiné à **A**, pour avis avant que
> l'utilisateur porte quoi que ce soit dans
> [DECISIONS.md](../../20260731-refactoring-audit/DECISIONS.md) et
> [CHANTIER.md](../../20260731-refactoring-audit/CHANTIER.md).
>
> **Rien n'a encore été porté.** Le registre du chantier est intact et porte
> toujours un D33 dont l'explication est démentie.
>
> Trois statuts, tenus séparés dans tout le document :
> **[MESURÉ]** exécuté, commande relançable · **[LU]** lecture de code, non
> exécutée · **[HYPOTHÈSE]** ni l'un ni l'autre.

---

## 1. Ce qui est à porter

| # | Objet | Origine |
|---|---|---|
| 1 | Les **quatre arbitrages** de l'utilisateur (ordre des étapes, E5 maintenue, requalification des 122 scénarios, relecture en quatre temps) | [PLAN.md](../PLAN.md), tranchés le 2026-08-05 |
| 2 | La **requalification de D33** | [07-B-mesure-d33.md](../MESURE-D33.md) |
| 3 | Les **six résultats de mesure** sur trois projets | [08-B-mesures-multi-projets.md](../MESURE-PROJETS.md) |
| 4 | Les **six règles de conduite** du plan, qui s'ajoutent aux treize de CHANTIER.md | [PLAN.md](../PLAN.md) |
| 5 | Le **gel de M7b après le lot 4** et le renvoi du point de reprise vers le plan | E0 |
| 6 | Cinq **trouvailles enregistrées non tranchées** (T1-T5) | [PLAN.md](../PLAN.md) |

---

## 2. Les faits

### 2.1 État du dépôt et de l'outillage

- **[MESURÉ]** `hexaglue-next` à `d5386a2`, arbre propre, quatre commits non
  poussés depuis `3a62fb8`.
- **[MESURÉ]** Corpus : 143 / 6 / 5 scénarios. 77 des 143 du profil 1 ne posent
  qu'un type, 120 au plus deux, 23 en posent trois ou plus.
- **[MESURÉ]** 122 des 143 portent encore le nom d'une méthode de test de
  l'ancien réacteur (`DomainCriteriaTest-…`, `PortClassifierTest-…`) ; 21 sont
  écrits pour le nouveau moteur.
- **[MESURÉ]** 80 des 143 n'attestent que du silence, dont 48 posent un type
  unique attendu UNCLASSIFIED. Total : 134 entrées classées, 135 UNCLASSIFIED.
- **[MESURÉ]** Sur les trois profils, les attentes ne portent que deux valeurs
  de confiance : **122 `HIGH`, 50 `EXPLICIT`, aucune `MEDIUM`, aucune `LOW`**.
- **[MESURÉ]** jqwik est absent de tous les `pom.xml` du réacteur ; aucun test
  ne nomme le point fixe.
- **[MESURÉ]** 7 fichiers de règles sur 28 concluent au moins une fois d'une
  absence.
- **[MESURÉ]** `RelationKind` compte cinq valeurs : `MANAGES`, `IDENTIFIED_BY`,
  `OWNS`, `ANNOUNCES`, `CONCERNS`.

### 2.2 Le mécanisme de D33

- **[MESURÉ]** Sur `case-study-ecommerce`, `Email` porte **un** signal :
  `[S3/HIGH] ANNOUNCED_BY(NotificationSender)`. Aucun autre. Aucun candidat
  concurrent.
- **[MESURÉ]** Sur le même run, `Money` porte six signaux dont **cinq**
  `OWNED_BY`. R3b fonctionne ; il ne parle jamais d'`Email`.
- **[MESURÉ]** `Customer` sort AGGREGATE_ROOT ; il détient bien
  `private final Email email`.
- **[MESURÉ]** `CustomerId` sort UNCLASSIFIED sur un duel parfait :
  IDENTIFIER score 100 contre VALUE_OBJECT score 100.
- **[LU]** `Lifecycle.isPart` écarte tout type que `Shapes.readsAsIdentity`
  reconnaît, et `readsAsIdentity = isImmutable && wrapsSingleValue`. Donc
  `record Email(String value)` n'est jamais une partie d'un agrégat.
- **[LU]** R7 (`PublishedEvent`) émet un jeton nommant le **port**, sur des
  types transportés `.distinct()` : deux méthodes du même `NotificationSender`
  produisent une seule clé.

**Conséquence** : les trois affirmations de D33 sont fausses — un signal R7 et
non deux, zéro R3b et non un, **aucune pesée**. Les deux premières de ses trois
questions tombent ; la troisième (« EVENT_PUBLISHER est-il trop large ? ») et la
deuxième (« R7 doit-il se taire sur ce que le domaine garde ? ») tiennent.

### 2.3 Le classpath

- **[MESURÉ]** `spring-petclinic`, mêmes sources, même configuration, seul le
  classpath Spring Data change :

| | sans classpath | avec classpath |
|---|---|---|
| UNCLASSIFIED | 14 | 10 |
| AGGREGATE_ROOT | 1 | 3 |
| VALUE_OBJECT | 1 | 2 |
| DRIVEN_PORT | 2 | 3 |
| `Owner` | UNCLASSIFIED | **AGGREGATE_ROOT** |
| `Pet` | UNCLASSIFIED | **VALUE_OBJECT** |

- **[LU]** Cause : `VetRepository extends Repository<…>` matche par FQN direct ;
  `OwnerRepository extends JpaRepository<…>` exige la fermeture transitive lue
  en bytecode.

### 2.4 Les enveloppes à une valeur, sur un second projet

- **[MESURÉ]** `case-study-banking`, cinq agrégats reconnus, donc la composition
  fonctionne :

| Type | Forme | signaux `OWNED_BY` | Verdict |
|---|---|---|---|
| `Address(street, city, zipCode, country)` | multi-champs | 1 | VALUE_OBJECT |
| `Money(amount, currency)` | multi-champs | 5 | VALUE_OBJECT |
| `Email(String value)` | enveloppe | **0** | **UNCLASSIFIED** (duel 100/100) |
| `Iban(String value)` | enveloppe | **0** | **UNCLASSIFIED** (duel 100/100) |

- **[MESURÉ]** Trois des six `*Id` de banking restent UNCLASSIFIED
  (`CustomerId`, `BeneficiaryId`, `TransactionId`) ; les trois autres sont
  sauvés par un port qui les prend comme clé.
- **[MESURÉ]** `spring-petclinic` ne porte **aucune** enveloppe (identités
  `Integer` nues) : il ne confirme ni n'infirme, il délimite la population
  concernée.

### 2.5 D16 sur le réel

- **[MESURÉ]** Avec classpath, `Pet` sort **VALUE_OBJECT** via
  `OWNED_BY(Owner)`, `Owner` sort AGGREGATE_ROOT. C'est exactement le cas que
  D16 décrit et tranche par « Q1 muet, Q2 le dit », vérifié hors corpus.

### 2.6 Les ports pilotants de banking

- **[MESURÉ]** 5 `DRIVING_PORT`, dont **5 par R5b** (`OFFERED_BY_THE_CORE`) et
  **0 par R5** (`HELD_BY_DRIVING_ADAPTER`).
- **[MESURÉ]** Le registre de passage explique pourquoi : 99 fichiers `.java`
  sous les racines, 48 types dans le `CodeModel`. Les cinq contrôleurs et
  dix-sept DTO de `banking-api` sont écartés,
  `HG-FRONTEND-005 : it is generated code, marked by @jakarta.annotation.Generated`.
- **[MESURÉ]** Ajouter les deux racines omises (`banking-app`,
  `banking-persistence`) **ne déplace aucun** des cinq verdicts.
- **[MESURÉ]** Sans classpath, le frontend signale
  `HG-FRONTEND-006 : the parser recovered from 38 problem(s)` — un compteur
  agrégé, sans la liste des déclarations incomplètes.

### 2.7 Un faux positif que la configuration masquait

- **[MESURÉ]** `InsufficientFundsException` et `TransferRejectedException`
  sortent **VALUE_OBJECT à HIGH** via `IMMUTABLE_SHAPE`.
  `AccountNotFoundException` y échappe parce qu'elle est mutable.
- **[MESURÉ]** Le banc e-commerce ne pouvait pas le voir : son `hexaglue.yaml`
  de sondage exclut `com.acme.shop.exception`.

### 2.8 Une prédiction que j'ai faite et qui est fausse

- **[MESURÉ]** J'avais annoncé au tour 04 que masquer les deux services
  détenteurs ferait basculer `InventoryUseCases` en DRIVING_PORT générable, et
  j'en avais fait le contre-exemple rouge de P0b. **Exécuté : c'est faux.** Avec
  `com.acme.shop.application` exclu, `InventoryUseCases` reste UNCLASSIFIED,
  zéro signal.
- **[LU]** Raison : `InventoryApplicationService` **implémente** le port et vit
  dans le paquet de ses deux détenteurs. L'exclusion retire l'implémenteur avec
  eux, donc R5b perd sa précondition (`implementersInTheCore` non vide).
- **Conséquence** : **P0b n'a pas de contre-exemple disponible**. Comme P0a,
  elle demande une fixture où détenteur et implémenteur sont séparables. Ta
  position du tour 05 était la bonne ; ma V8 était une prédiction que je n'avais
  pas exécutée. `PLAN.md` et l'état partagé sont corrigés.

---

## 3. Les hypothèses

Aucune n'est mesurée. Elles sont énoncées pour être instruites, pas pour être
portées comme des faits.

### H1 — La boucle auto-confirmante

> HexaGlue génère un adapter pilotant → le run suivant l'écarte parce qu'il est
> marqué généré (D15) → R5 ne peut plus mordre → R5b conclut « personne dedans
> ne le détient » → le port reste générable → HexaGlue régénère l'adapter.

**Ce qui l'étaye** : sur banking, 5 ports pilotants sur 5 viennent de R5b, et
les contrôleurs sont effectivement écartés comme code généré **[MESURÉ]**.
**Ce qui manque** : personne n'a observé le cycle complet sur deux exécutions
successives. Le mécanisme est lu, l'enchaînement est supposé.

### H2 — L'invisibilité des enveloppes est une propriété du jeu de règles

Confirmée sur deux projets **[MESURÉ]** et lisible dans `Lifecycle.isPart`
**[LU]**, donc indépendante du projet analysé. Reste **[HYPOTHÈSE]** :
qu'elle se comporte de même sur le corpus des 154 scénarios, où elle pourrait
déplacer des attentes relues.

### H3 — La clé de corrélation est un changement à double sens

La clé de score est aujourd'hui `(sujet, kind, palier, jeton, distance)`, sans
la `RuleId` **[LU]**. Adopter `(ruleFamily, subject, candidateKind, ancre)`
fusionnerait des signaux aujourd'hui séparés **et** scinderait des signaux
aujourd'hui fusionnés par collision de jetons. **[HYPOTHÈSE]** : l'ampleur du
déplacement sur le corpus, dans les deux sens, est inconnue.

### H4 — La sémantique du solveur est un point fixe auto-cohérent non unique

`Classifier` repart d'une base vide à chaque tour et sort quand
`next.equals(verdicts)` **[LU]**. **[HYPOTHÈSE]** : le résultat dépend du départ
à `Verdicts.none()` et il peut exister plusieurs points fixes. C'est P3a, non
caractérisée.

---

## 4. Ce sur quoi je te demande un avis

Le portage n'est pas mécanique : il touche le registre, c'est-à-dire l'artefact
de gouvernance dont la faiblesse a déclenché tout cet échange. Cinq questions
d'écriture, où je n'ai pas de position tranchée.

### Q5 — Quelle forme donner à la requalification de D33 ?

Le registre connaît trois statuts : `PENDING`, `CONFIRMÉE`, `CADUQUE`. Aucun ne
décrit « le constat tient, l'explication est démentie, deux des trois questions
tombent ».

- **A** — D33 reste `PENDING`, son texte est réécrit sur la mesure, avec la
  trace de ce qui a été démenti.
- **B** — D33 passe `CADUQUE` et une décision neuve porte la question réelle.
- **C** — un statut nouveau, du type `REQUALIFIÉE`, à créer.

Ce qui me gêne en A : réécrire une décision datée efface la démonstration que le
registre a porté un mécanisme faux pendant un jour. Ce qui me gêne en B : la
question posée n'a pas changé de sujet, seulement de cause.

### Q6 — Les quatre arbitrages deviennent-ils des décisions numérotées ?

Le registre va de D0 à D38.

- **A** — une décision unique, D39, « plan de reprise adopté », qui renvoie au
  plan.
- **B** — quatre décisions, D39 à D42, une par arbitrage.
- **C** — aucune décision : les arbitrages vivent dans le plan, et CHANTIER.md
  y renvoie.

Argument pour B : la règle du registre dit qu'une décision n'existe que si elle
y est consignée. Argument pour A : quatre décisions qui renvoient toutes au même
document en font un registre de formulaires.

### Q7 — Où atterrissent T1 à T5, et les hypothèses H1 à H4 ?

Le registre est fait pour des décisions, pas pour des constats en attente
d'instruction. CHANTIER.md a une rubrique « Découvertes en cours de chantier ».

- **A** — T1-T5 en découvertes, H1-H4 nulle part (ce sont des questions de E3a).
- **B** — T1-T5 en découvertes **et** H1-H4 aussi, pour qu'une hypothèse ne se
  transforme pas en fait par citation, comme D33.
- **C** — tout en décisions `PENDING`.

Je penche pour B, précisément à cause de D33 : ce qui a manqué n'était pas un
statut mais **la mention que l'explication n'avait pas été exécutée**.

### Q8 — Les étapes E0-E7 remplacent-elles le tableau des jalons ?

CHANTIER.md porte un tableau M0-M8, dont M7 est `EN COURS` et M8 `À FAIRE`. Le
plan introduit dix étapes qui ne sont pas des jalons : elles n'ajoutent aucune
fonctionnalité, elles réparent l'étalon et le moteur.

- **A** — un jalon nouveau s'intercale, entre M7 et M8, portant E0-E7.
- **B** — le tableau reste, et le plan est un document parallèle vers lequel le
  point de reprise renvoie.
- **C** — le tableau est refondu : M7 repasse `SUSPENDU`, et E0-E7 deviennent la
  feuille de route jusqu'à sa reprise.

### Q9 — Que devient la règle 13 de clôture de jalon ?

Elle demande « corpus vert + revue contre les interdits du doc 07 §10 +
relecture ligne à ligne de la section livrée ». Or le corpus vert est
précisément le cliquet qu'on vient de disqualifier, et le doc 07 §4.1 énonce un
faux sur la monotonie.

Faut-il suspendre la règle 13 jusqu'à E4b, la réécrire tout de suite, ou la
garder en notant qu'elle mesure moins qu'elle ne prétend ?

---

## 5. Ce que je ne te demande pas

Les quatre arbitrages de l'utilisateur sont pris et ne se rediscutent pas. Les
six résultats de mesure sont exécutés et relançables. Ce dossier ne rouvre ni le
plan, ni les litiges, ni les décisions du registre.
