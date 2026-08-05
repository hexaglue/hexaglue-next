# Dossier de portage consolidé — tout ce qu'il faut écrire, en un seul endroit

> Auteur : **B**. Date : 2026-08-05. Commit de référence : `hexaglue-next` à
> `13e8efd`.
>
> **Ce document remplace, pour l'exécution, les tours 15 à 21.** Ceux-ci restent
> la trace de comment on y est arrivé ; celui-ci est ce qu'il faut écrire. Tout
> ce qui a été corrigé entre-temps est intégré : les sept corrections du tour 16,
> les quatre réponses du tour 18, les trois amendements arbitrés au tour 21.
>
> **Rien n'est encore porté.** `DECISIONS.md` et `CHANTIER.md` sont intacts.

---

## 0. État de départ

| Élément | État |
|---|---|
| Révision du plan à épingler | `PLAN.md`, commit **`5b4c421`** (révision 6), `sha256 944998954d73fb3650355b65cf1718ddddecc18f5a33e53777ed1bd27c87c613` |
| Révisions antérieures, conservées comme témoins | `ab52fb5` (r3, adoptée), `9a109d3` (r4, trois amendements), `f2450df` (r5, sujets nommés d'E7) |
| Registre | intact, D0 à D38, une seule `PENDING` : D33 |
| Jalons | M7 encore `EN COURS`, M8 `À FAIRE` |
| Règle 13 | encore dans sa version « corpus vert » |
| Code | cinq commits poussés, CI et CodeQL vertes 4/4 ; cinq commits locaux non poussés, `9a109d3` → `5b4c421` |

**Tous les points d'arbitrage sont tranchés** — le dernier, la place de
`E7-MODÈLE-1`, l'a été le 2026-08-05 : voir §8.

---

## 1. Geste 1 — D39, contrat de gouvernance du registre

> À écrire dans `DECISIONS.md`, en tête des décisions confirmées.
> **Rédigée sous l'ancien format** : elle décide du nouveau contrat, elle ne peut
> pas déjà l'appliquer.

### D39 — Contrat de gouvernance du registre : nature, statut et provenance sont trois axes — CONFIRMÉE (2026-08-05)

- **Contexte** : D33 a porté pendant un jour une explication de mécanisme que
  personne n'avait exécutée, et cette explication a servi de base à un arbitrage
  demandé à l'utilisateur ; la mesure l'a démentie sur ses trois affirmations. Le
  même glissement s'est reproduit trois fois de plus dans la même semaine : une
  prédiction est devenue un « contre-exemple acquis » avant d'être réfutée par
  exécution, une hypothèse a été inscrite dans une colonne « Trouvaille » du
  document même qui diagnostiquait ce défaut, et une affectation d'étape a été
  changée sans arbitrage. Le registre ne distingue aujourd'hui que trois statuts
  d'arbitrage ; rien n'y dit **d'où une affirmation tient sa force**.
- **Options** : **A** ajouter un statut de décision décrivant la maturité des
  preuves ; **B** poser trois axes indépendants ; **C** s'en remettre à la
  vigilance des rédacteurs.
- **Décision** (utilisateur) : **B**. Un statut d'arbitrage ne peut pas porter
  une propriété qui n'est pas de l'ordre de l'arbitrage ; et la vigilance est
  précisément ce qui a échoué quatre fois.

#### Ce que le contrat pose

**1. Trois axes.**

| Axe | Valeurs | Ce qu'il gouverne |
|---|---|---|
| Nature | décision, constat, hypothèse, erratum, plan | où l'énoncé doit vivre |
| Statut d'arbitrage | `PENDING`, `CONFIRMÉE`, `CADUQUE` | l'autorisation d'agir |
| Provenance | `[MESURÉ]`, `[LU]`, `[HYPOTHÈSE]` | la force de l'affirmation |

**2. Sémantique stricte des balises.** `[MESURÉ]` désigne ce qu'une exécution a
observé, et renvoie à une commande relançable avec ses entrées épinglées :
révision, racines, **classpath**, configuration, corpus. Le classpath appartient
à l'identité d'une mesure : sans lui, le moteur rend d'autres verdicts. `[LU]`
désigne ce qui est visible dans une révision précise du code, et ne vaut jamais
seul comme preuve d'un comportement exécuté. `[HYPOTHÈSE]` couvre toute
extrapolation causale non exécutée. **Chaque proposition atomique porte sa
propre balise.**

**3. Les attentes normatives et métier ne sont dans aucune des trois.** Une
attente du type « ce type ne devrait pas être un `DOMAIN_EVENT` » n'est ni
mesurée, ni lue, ni hypothétique. **Règle transitoire** : avant la mise en place
de l'oracle en E4a, une telle attente cite une décision `CONFIRMÉE` ou reste
explicitement une question à arbitrer ; **elle ne peut pas être présentée comme
un fait acquis**. E4a définira la provenance et la représentation des attentes
arbitrées avec leurs premiers consommateurs réels — pas avant, pour ne pas figer
une forme sans usage.

**4. Append-only, borné.** La règle vise les **entrées porteuses de preuve ou
d'arbitrage** : décisions, constats, hypothèses, errata, points de reprise. Un
énoncé daté de cette nature ne se réécrit pas ; ce qui est démenti reçoit une
requalification datée, à côté. Les **vues d'état courant** — le tableau des
jalons, un compteur — peuvent évoluer, à condition de conserver la transition
datée et l'ancienne valeur dans l'historique.

**5. Le legs antérieur à D39 n'est pas une voie de contournement.** Une
affirmation antérieure peut rester non balisée pour trace ; **elle ne peut pas
servir de prémisse à une nouvelle décision** tant qu'elle n'a pas été reprise
dans un amendement daté portant sa provenance.

**6. Trois rubriques à `CHANTIER.md`.** « Constats et découvertes » (`[MESURÉ]`
ou `[LU]`, avec preuve, portée et étape), « Hypothèses à instruire », et
« Errata actifs » — index opérationnel consommé par la règle 13, qui relie un
défaut documentaire à sa preuve, son étape correctrice et sa vérification.

**7. Une hypothèse porte deux champs, jamais un.**

| Champ | Valeurs |
|---|---|
| État épistémique | `OUVERTE`, `ÉTAYÉE`, `RÉFUTÉE`, `REMPLACÉE` |
| État d'instruction | `À PRÉPARER`, `PRÊTE`, `EN COURS`, `EXÉCUTÉE`, `BLOQUÉE` |

Un blocage nomme son prérequis et l'étape chargée de le lever. Une fixture
absente ne dit rien de la vérité d'une hypothèse : elle dit que son protocole
n'est pas exécutable. Ces états **n'autorisent aucune correction du comportement
produit** — ils autorisent le travail de mesure prévu par le plan. Une hypothèse
réfutée est **conservée avec son résultat**.

**8. Le registre est versionné.** Les documents du chantier vivent sous
`hexaglue-next/docs/chantier/`. Une décision peut donc citer un commit, et une
réécriture après coup se voit en diff.

- **Impact** : l'en-tête de `DECISIONS.md` gagne les axes et la règle
  append-only ; `CHANTIER.md` gagne trois rubriques ; le contrat s'applique **à
  partir de l'entrée suivante**. Une évolution future de cette grammaire
  remplacera D39 sans rendre caduque aucune décision prise sous elle.

---

## 2. Geste 2 — La grammaire appliquée

**Dans `DECISIONS.md`**, après les trois règles d'en-tête existantes :

> - **Provenance (D39)** : toute affirmation factuelle porte `[MESURÉ]`, `[LU]`
>   ou `[HYPOTHÈSE]`. `[MESURÉ]` renvoie à une commande relançable et à ses
>   entrées — révision, racines, classpath, configuration, corpus. Une attente
>   normative ou métier n'est dans aucune des trois : avant E4a, elle cite une
>   décision `CONFIRMÉE` ou reste une question à arbitrer.
> - **Append-only (D39)** : une entrée porteuse de preuve ou d'arbitrage ne se
>   réécrit pas ; ce qui est démenti reçoit une requalification datée, à côté.
> - **Legs (D39)** : une affirmation antérieure au 2026-08-05 peut rester non
>   balisée pour trace, mais ne peut pas servir de prémisse à une décision
>   nouvelle sans reprise datée portant sa provenance.

**Dans `CHANTIER.md`** : la rubrique « Découvertes en cours de chantier » est
renommée **« Constats et découvertes »**, ses six entrées existantes conservées
telles quelles sans balise rétroactive ; deux rubriques s'ouvrent après elle,
**« Hypothèses à instruire »** et **« Errata actifs »**.

---

## 3. Geste 3 — D33 amendée, append-only

Le texte existant est **conservé intégralement**. Deux blocs l'encadrent.

**En tête de l'entrée, juste après le titre** :

> ⚠ **Explication causale du contexte : `[HYPOTHÈSE — DÉMENTIE PAR MESURE le
> 2026-08-05]`.** Le symptôme mesuré demeure ; le mécanisme qui l'expliquait est
> conservé ci-dessous pour trace, puis requalifié en fin d'entrée.

**En fin d'entrée**, après « Ne pas agir avant arbitrage » :

> #### Requalification du 2026-08-05
>
> - `[MESURÉ]` — sur `case-study-ecommerce`, `Email` sort **`DOMAIN_EVENT` à
>   `HIGH`**. C'est le verdict à l'origine de cette décision, et il est inchangé.
> - `[MESURÉ]` — `Email` porte **un seul signal**,
>   `[S3/HIGH] ANNOUNCED_BY(NotificationSender)`, et **aucun signal de
>   possession**. Commande relançable, entrées épinglées et sortie complète :
>   `docs/chantier/20260805-convergence/MESURE-D33.md`.
> - `[MESURÉ]` — sur le même run, R3b émet **cinq signaux `OWNED_BY` sur
>   `Money`** et **aucun sur `Email`**.
> - `[LU]` — `Lifecycle.isPart` écarte tout type que `Shapes.readsAsIdentity`
>   reconnaît, et `readsAsIdentity = isImmutable && wrapsSingleValue`.
>   `record Email(String value)` n'est donc jamais une partie d'un agrégat.
> - `[LU]` — R7 émet un jeton nommant le **port**, sur des types transportés
>   `.distinct()` : les deux méthodes de `NotificationSender` produisent une
>   seule clé.
>
> **Ce que la mesure invalide** : les trois affirmations du contexte — « deux
> signaux R7 » (il y en a un), « la possession par `Customer` (R3b, un signal) »
> (il y en a zéro), et la pesée entre les deux (**il n'y en a aucune** : le
> verdict est acquis 1-0, sans adversaire).
>
> **Ce qui survit** : deux des trois questions à instruire. « R7 devrait-il se
> taire sur un type que le domaine GARDE ? » et « le rôle EVENT_PUBLISHER
> est-il trop large ? » restent posées, R7 étant désormais **le seul à parler**
> sur ce verdict. La première — « la possession devrait-elle primer la
> publication à palier égal ? » — est **sans objet** : il n'y a pas de signal de
> possession à faire primer.
>
> **Ce qui est ouvert ailleurs** : la cause réelle dépasse ce cas et ne se
> greffe pas rétroactivement au périmètre de D33. Elle est enregistrée en
> constats C-2 et C-3, en hypothèse H-2, et son arbitrage revient au sujet
> `E7-MODÈLE-1` de la file de réévaluation.
>
> **Statut inchangé : `PENDING`.** Les questions survivantes ne sont pas
> tranchées.

---

## 4. Geste 4 — D40, adoption du plan de reprise

> À écrire dans `DECISIONS.md`, **sous la nouvelle grammaire**.

### D40 — Adoption du plan de reprise du chantier — CONFIRMÉE (2026-08-05)

- **Contexte** `[MESURÉ]` : le corpus d'acceptation, qui sert de cliquet de
  clôture à chaque jalon, compte 143 / 6 / 5 scénarios. **122 des 143 du profil 1
  portent encore le nom d'une méthode de test de l'ancien réacteur** ; **77 ne
  posent qu'un type et 120 au plus deux** ; **80 n'attestent que du silence** ;
  et sur les trois profils les attentes ne portent que `HIGH` (122) et
  `EXPLICIT` (50), **aucune `MEDIUM` ni `LOW`** — donc la porte de génération,
  qui compare `confidence >= HIGH`, n'a jamais été exercée comme un refus.
  Commande : `docs/chantier/20260805-convergence/mesures.sh`, sortie datée dans
  `MESURES.md`.
- **Contexte** `[LU]` : `EvidenceTier` traduit S2, S3 et S4 sur la même valeur
  `Confidence.HIGH` ; `Classifier` repart d'une base de faits vide à chaque tour
  et sort quand les verdicts ne bougent plus, là où le doc 07 §4.1 annonce une
  saturation monotone ; sept fichiers de règles sur vingt-huit concluent d'une
  absence ; jqwik, annoncé au doc 07 §7, est absent de tous les `pom.xml`.
- **Décision** (utilisateur, 2026-08-05) : **le plan de reprise est adopté** dans
  la révision épinglée ci-dessous, et **M7b est gelé après son lot 4**.

#### Les quatre arbitrages, recopiés

Ils font foi ici, indépendamment de toute évolution ultérieure du plan.

1. **Ordre des étapes** : E0 gel, E1 séparer palier / confiance / autorisation
   — **ouverte par `E1a`**, la caractérisation du marqueur de génération —, E2
   nommer la sémantique du solveur, E2b produire les fixtures manquantes, E3a
   caractériser, E3b décider les garanties, E4a construire l'étalon, E4b
   calibrer, E5 périmètre des sources, E6 politiques des absences, E7 familles
   de décisions, E8 reprise de M7b. **E5 et E6 ne sont pas regroupées** : la
   première porte la complétude des entrées, la seconde la sémantique des règles
   sur une entrée connue.
2. **E5 est maintenue** : l'axe d'autorisation de E1 traite le droit d'agir, il
   ne rend pas l'observation complète.
3. **Les 122 scénarios transplantés sont requalifiés un par un**, quatre issues
   — conserver et renommer, réécrire en scénario câblé, remplacer par une
   propriété, supprimer. Les survivants perdent le nom des anciennes méthodes de
   test et portent celui de l'invariant.
4. **La relecture des attentes se fait en quatre temps** : passe aveugle sans
   afficher le verdict courant, arbitrage utilisateur, révélation et score,
   enregistrement par une session distincte. L'agent ne fabrique pas seul
   l'oracle qu'il devra satisfaire ; le dénominateur reste exhaustif.

#### Les six règles de conduite

Elles gouvernent le plan de reprise ; les treize règles de `CHANTIER.md`
restent applicables, et **toute contradiction est consignée et arbitrée
explicitement**.

1. Une trouvaille faite dans un lot est enregistrée, jamais tranchée ni
   implémentée dans ce lot.
2. Toute mesure qui étaye une décision est une commande relançable, avec ses
   entrées et son résultat attendu.
3. Une décision de portée générale présente un cas nominal, un contre-exemple et
   son effet sur plusieurs projets.
4. « Débloque le lot » est un impact de calendrier, jamais un argument de
   justesse.
5. **Indépendance de validation.** Une même session ne peut pas à la fois
   proposer ou implémenter une modification d'un artefact qui détermine ce qui
   est correct, puis **valider seule** cette modification. Pour le déplacement
   d'un cliquet et l'enregistrement d'une attente d'oracle, la session de
   validation est distincte de celle qui a produit la modification, l'arbitrage
   de l'utilisateur ne s'y substituant pas ; pour la fermeture d'un erratum,
   l'arbitrage de l'utilisateur suffit.
6. **Caractériser n'est pas garantir** : mesurer une propriété et exiger qu'elle
   soit vraie sont deux gestes séparés, dans deux sessions séparées.

#### Révision adoptée

```text
docs/chantier/20260805-convergence/PLAN.md
commit  5b4c421                        (révision 6)
sha256  944998954d73fb3650355b65cf1718ddddecc18f5a33e53777ed1bd27c87c613
```

La révision `ab52fb5` a été adoptée le même jour, puis amendée sur des clauses
identifiées et sur elles seules : l'articulation des règles, l'indépendance de
validation et l'ouverture d'E1 par `E1a` (`9a109d3`), puis l'ouverture de la
file de sujets nommés d'E7 (`f2450df`). La révision 6 (`5b4c421`) ne fait que
suivre le reclassement des fichiers : **aucun changement normatif**. Les
révisions antérieures sont conservées comme témoins. Une révision ultérieure ne modifie pas cette
décision ; si un volet change, une décision nouvelle remplace explicitement la
clause concernée.

**Régime d'installation** : les quatre amendements du plan ont été arbitrés
**avant** l'entrée en vigueur de la règle 5 révisée. Ce n'est ni une exception
ni une auto-validation — c'est la borne du régime sous lequel la règle a été
installée.

- **Impact** : M7 passe `SUSPENDU` ; la règle 13 est réécrite ; les constats,
  hypothèses et errata sont portés aux trois rubriques nouvelles ; **rien n'est
  corrigé dans le code par cette décision**.

---

## 5. Geste 5 — Constats, hypothèses, errata

### 5.1 Constats et découvertes — ajouts du 2026-08-05

Huit entrées atomiques. C-4 et C-6 sont scindées par rapport au projet du
tour 15, C-7 est bornée.

| # | Énoncé | Provenance | Étape |
|---|---|---|---|
| **C-1** | Sur `case-study-ecommerce`, `Email` sort `DOMAIN_EVENT` à `HIGH` avec **un seul signal** (`ANNOUNCED_BY(NotificationSender)`) et **aucun signal de possession** : aucune pesée n'a lieu | `[MESURÉ]` | E7 |
| **C-2** | `Lifecycle.isPart` écarte tout type que `Shapes.readsAsIdentity` reconnaît, donc **toute enveloppe immuable à une valeur** est invisible à la composition, qu'elle soit une identité ou une valeur | `[LU]` | E7 |
| **C-3** | Sur `case-study-banking`, cinq agrégats reconnus : `Address` reçoit un `OWNED_BY`, `Money` en reçoit cinq, `Email` et `Iban` **zéro** — et sortent tous deux sur un duel IDENTIFIER 100 / VALUE_OBJECT 100 | `[MESURÉ]` | E7 |
| **C-4a** | Sans classpath, `spring-petclinic` passe de 3 agrégats à 1 et de 10 à 14 non classés ; `Owner` cesse d'être un agrégat | `[MESURÉ]` | E4a |
| **C-4b** | `VetRepository extends Repository<…>` s'apparie par FQN direct ; `OwnerRepository extends JpaRepository<…>` exige la fermeture transitive lue en bytecode | `[LU]` | E4a |
| **C-5** | Deux exceptions immuables de banking sortent `VALUE_OBJECT` à `HIGH` par `IMMUTABLE_SHAPE` ; le banc e-commerce excluait le paquet `exception` par configuration | `[MESURÉ]` | E4a |
| **C-6** | `HG-FRONTEND-006` rend un compteur agrégé de récupérations du parser, sans nommer les déclarations incomplètes | `[MESURÉ]` | E4a |
| **C-7** | La clé de déduplication d'un signal est `(sujet, kind, palier, jeton fact(), distance)` : elle contient le texte écrit à la main et **pas la `RuleId`**, ce qui **rend possible** qu'une modification de message déplace une pondération | `[LU]` | E3a |
| **C-8** | jqwik est absent de tous les `pom.xml` du réacteur, alors que le doc 07 §7 l'annonce comme choix cible pour les propriétés du point fixe | `[MESURÉ]` | E3a |

Ce que C-6 disait de trop — « insuffisant pour un oracle relu type par type » —
est un besoin, pas un constat : il appartient à la définition d'E4a.

### 5.2 Hypothèses à instruire — ouvertes le 2026-08-05

| # | Proposition falsifiable | Épistémique | Instruction | Protocole | Étape |
|---|---|---|---|---|---|
| **H-1a** | Un adapter pilotant marqué `@jakarta.annotation.Generated` rend R5 inatteignable, et R5b devient la seule voix sur les ports qu'il servait | `OUVERTE` | `À PRÉPARER` | mêmes sources en deux variantes, seul le marqueur varie ; comparer registre de passage, signaux R5/R5b, verdict et autorisation | **E1a** |
| **H-1b** | Le cycle se referme : HexaGlue régénère l'adapter qu'il vient d'écarter | `OUVERTE` | **`BLOQUÉE`** — demande un générateur d'adapters pilotants | deux exécutions successives sur un projet dont l'adapter pilotant est généré | **E8** |
| **H-2** | L'invisibilité des enveloppes déplace des attentes relues sur les 154 scénarios | `OUVERTE` | `À PRÉPARER` — demande une variante expérimentale du moteur | passer le corpus avec et sans l'exclusion de `readsAsIdentity`, sans toucher au comportement de référence ni aux goldens | E3a |
| **H-3** | Adopter la clé `(ruleFamily, subject, candidateKind, ancre)` déplace le corpus **dans les deux sens** | `OUVERTE` | `À PRÉPARER` — variante expérimentale | comparer les verdicts sous les deux clés | E3a |
| **H-4** | **Il existe deux états initiaux admissibles** — dont éventuellement `Verdicts.none()` — **qui convergent vers des verdicts finaux différents** | `OUVERTE` | `PRÊTE` | comparer les points fixes atteints depuis les seeds admissibles définis par P3a | E3a |
| **H-5** | ~~Masquer les détenteurs de `InventoryUseCases` le rend générable~~ | **`RÉFUTÉE`** | `EXÉCUTÉE` le 2026-08-05 | exclusion de `com.acme.shop.application` : le type reste UNCLASSIFIED, l'implémenteur partageant le paquet des détenteurs | — |

**Note** : H-2 et H-3 demandent une variante expérimentale du moteur. E3a les
enregistre comme **interventions de caractérisation** — isolées, reproductibles,
sans modifier le comportement de référence ni les goldens. Leur sortie est une
mesure destinée à E3b, jamais un correctif anticipé.

**Note** : les propriétés P0a et P0b du plan ont la même instruction bloquée
qu'H-1b, mais leur prérequis est levable — c'est l'objet d'E2b.

### 5.3 Errata actifs — ouverts le 2026-08-05

Forme d'une entrée : identifiant stable, cible (document, révision, section,
énoncé), défaut borné, fondement lié aux constats, impact, correction attendue
en critère observable, étape affectée, statut, historique.

Cycle : `OUVERT` → `CORRIGÉ À RELIRE` → `FERMÉ`. L'étape affectée corrige et
passe à `CORRIGÉ À RELIRE` ; **elle ne ferme pas**. Une session distincte
vérifie que le texte fautif est corrigé, que le nouveau texte correspond à la
décision ou à la mesure citée, et qu'aucun passage dépendant ne conserve
l'ancienne affirmation — puis passe à `FERMÉ` en inscrivant la révision relue.
`DIFFÉRÉ (Dxx)` exige une décision citée et ne vaut pas fermeture ; `RÉFUTÉ`
conserve la trace d'un erratum mal fondé.

| ID | Cible | Défaut | Fondement | Affectation | Fermeture attendue |
|---|---|---|---|---|---|
| **ERR-001** | doc 07 §4.1, « Terminaison garantie : on n'ajoute que des faits (monotonie) » | Le solveur n'est pas monotone : `Classifier` repart d'une base vide à chaque tour et la convergence est surveillée par un plafond | `[LU]`, contexte de D40 | **E2** | sémantique réelle nommée dans le code et §4.1 corrigé |
| **ERR-002** | doc 07 §4.1, « Preuves gratuites : chaque fait dérivé mémorise (règle, prémisses) » | La formulation laisse entendre une dérivation complète, alors que la restitution ne recolle pas les verdicts cités en prémisses | `[LU]`, `Explanation.java:46-50` | **E2** | promesse documentaire bornée et explication raccordée |

**jqwik n'est pas un erratum** : le doc 07 §7 l'annonce en colonne « Choix »,
à côté de « PIT (déjà) » qui marque ce qui est en place. Le document n'est pas
faux, la réalisation est incomplète — c'est le constat C-8, affecté à E3a.

---

## 6. Geste 6 — M7 `SUSPENDU` et point de reprise

**Ligne du tableau des jalons**, en remplacement de la ligne M7 :

| Jalon | Contenu | Statut | Notes |
|---|---|---|---|
| M7 | jpa + rest (seuil de certitude) | **SUSPENDU** (2026-08-05) | `EN COURS` jusqu'au 2026-08-05, `SUSPENDU` à cette date par D40. M7a clos ; M7b lots 1-4 faits et poussés, **lots conservés, reprise interdite** avant les portes définies par D40. Le vocabulaire des statuts gagne `SUSPENDU` : `EN COURS` laissait entendre que le lot suivant pouvait s'ouvrir. |

**Point de reprise**, en tête, l'actuel étant empilé dessous et marqué `PÉRIMÉ` :

> ### Point de reprise (au 2026-08-05, **M7 SUSPENDU — plan de reprise adopté**)
>
> Le chantier ne reprend pas à M7b. **D40** adopte un plan de reprise couvrant la
> séquence E0 à E8 et ses sous-étapes, qui répare l'étalon et le moteur avant que
> la trajectoire produit continue. Le plan est
> `docs/chantier/20260805-convergence/PLAN.md`, commit `5b4c421`.
>
> **Le motif, tel qu'adopté par D40** : le chantier a été conduit en pensant
> migration d'abord, et ce qui a été transplanté sans requalification est la
> définition de ce qui est correct. Les mesures qui l'établissent sont au
> contexte de D40 et dans
> `docs/chantier/20260805-convergence/MESURES.md`.
>
> **Prochaine action : `E1a`** — caractériser ce que le marqueur de génération
> change, avant qu'E1 décide le traitement d'un verdict fondé sur l'absence
> seule. Ni E5 ni E7 ne s'ouvrent avant leur tour ; **aucune décision de la file
> de réévaluation ne se rouvre**.
>
> **État du code** `[MESURÉ]` : cinq commits poussés, `5a53135` → `9373fdc`,
> arbre propre ; **1 401 tests**, `make ci` vert, **12/12 en intégration**,
> cliquet 143/143 + 6/6 + 5/5. Commandes : `make ci`, `make integration`.
>
> **Le registre porte une décision `PENDING` : D33**, dont l'explication a été
> démentie par la mesure et requalifiée le 2026-08-05. Ne pas agir dessus.

---

## 7. Geste 7 — La règle 13 réécrite

L'ancien texte est **conservé** juste en dessous, sous la mention « *texte en
vigueur jusqu'au 2026-08-05, remplacé par D40* ».

> **13. Clôture d'une étape ou d'un jalon (D12, amendée le 2026-08-04, réécrite
> par D40 le 2026-08-05).** La clôture se fait contre les critères de sortie
> déclarés **avant** l'exécution.
>
> — Une étape de **caractérisation** est clôturable lorsque ses mesures sont
> relançables, ses résultats consignés — y compris les contre-exemples rouges —
> et qu'aucune correction implicite n'a été introduite. **Une instruction
> `BLOQUÉE` ne satisfait le critère que si son différé a été explicitement
> arbitré et si le résultat absent n'est pas une précondition d'une étape
> aval.**
>
> — Une étape d'**implémentation** est clôturable lorsque le comportement
> attendu est démontré par les tests déclarés, les régressions applicables
> vertes, et la documentation et les diagnostics mis en cohérence.
>
> — Une étape de **conformité** est clôturable lorsque ses portes approuvées
> sont vertes.
>
> Toute mesure précise la révision, les racines, le **classpath**, la
> configuration, la commande et le corpus. La clôture comprend la relecture des
> sections applicables du plan, du document 07 **et des errata actifs** ; une
> contradiction connue est corrigée par l'étape qui en a la charge ou fait
> l'objet d'un différé explicite, elle n'est jamais masquée par la relecture du
> texte historique. **Une étape ne clôt pas si un erratum de son périmètre reste
> `OUVERT` ou `CORRIGÉ À RELIRE`**, sauf différé autorisé par décision citée.
> **Un corpus n'est une porte que si la décision qui l'institue comme oracle est
> citée.**

---

## 8. Le point tranché — `E7-MODÈLE-1` vit dans le plan

**Arbitrage de l'utilisateur, 2026-08-05** : le sujet est écrit dans `PLAN.md`,
section E7, et non dans `CHANTIER.md` où il se perdrait. La file de réévaluation
appartient au plan.

C'est le **quatrième amendement** du plan, commit `f2450df`, révision 5 — la révision 6 qui suit ne fait que reclasser les fichiers. Il
ouvre une sous-section « Sujets nommés de la file » qui pose ce qu'est un
sujet — une question normative identifiée avant d'avoir son matériel, sans
numéro de décision et sans autorisation — puis inscrit `E7-MODÈLE-1` avec sa
famille, sa question, ce qui la fonde, ce qu'elle n'est pas, et l'interdiction
de l'instruire avant l'ouverture de la famille « modèle métier ».

**Plus rien ne reste à trancher avant d'écrire.**

## 9. Ordre d'exécution

1. Écrire **D39** dans `DECISIONS.md`, sous l'ancien format.
2. Appliquer la grammaire — en-tête de `DECISIONS.md`, trois rubriques de
   `CHANTIER.md`.
3. Amender **D33**, statut `PENDING` inchangé.
4. Écrire **D40**, épinglant `5b4c421`.
5. Porter les **huit constats**, les **six hypothèses** et les **deux errata**
   dans `CHANTIER.md`. `E7-MODÈLE-1` est déjà écrit dans `PLAN.md` (§8).
6. Passer **M7 `SUSPENDU`** et réécrire le point de reprise.
7. Réécrire la **règle 13**, ancien texte conservé.

Un commit par geste, ou un commit unique — à votre convenance ; le diff sera
lisible dans les deux cas puisque le registre est désormais versionné.
