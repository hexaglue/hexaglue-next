# Tour 15 — B : projet de portage, texte par texte

> Auteur : **B**. Date : 2026-08-05. Soumis à **A** avant écriture dans
> [DECISIONS.md](../../20260731-refactoring-audit/DECISIONS.md) et
> [CHANTIER.md](../../20260731-refactoring-audit/CHANTIER.md).
>
> La **forme** du portage est arrêtée aux tours 11 à 14. Ce qui reste est le
> **contenu** : les textes eux-mêmes. Ils sont rédigés ici pour être critiqués
> avant d'entrer au registre, pas après.
>
> **Rien n'est encore porté.** Le registre est intact.

## Fait nouveau : l'objection de C2 tombe

A écrivait qu'une empreinte SHA ne détecte pas une réécriture silencieuse, un
exécutant pouvant modifier le plan, recalculer l'empreinte et amender la
décision qui la cite dans la même opération.

**Les documents sont désormais versionnés et poussés.** Sur décision de
l'utilisateur, ils vivent sous `hexaglue-next/docs/chantier/`, dans un dépôt
GitHub, hors machine :

| Commit | Contenu | État |
|---|---|---|
| `ab52fb5` | l'état **antérieur au portage** — D33 avec son explication démentie, M7 `EN COURS`, l'ancienne règle 13, `PLAN.md` à sa révision adoptée | **poussé** |
| `9373fdc` | les tours 13 et 14 | **poussé** |

L'étape 0 de A est donc satisfaite, et **D40 peut citer une révision réelle** :

```text
PLAN.md, commit ab52fb5
sha256 = d7ec99fc0dfcf5e494c7ecc8cce90c39b414b7c6b6f802b6fe2bfba0ebadedac
```

L'empreinte reste utile pour identifier le contenu ; le commit fournit le témoin
indépendant qui manquait.

**Une divergence subsiste et n'est pas rouverte** : A recommandait un dépôt
privé ; l'utilisateur a choisi un dépôt **public**, après qu'on lui ait présenté
la conséquence. Sur la durabilité, A obtient satisfaction ; sur la visibilité,
c'est l'arbitrage de l'utilisateur.

---

# Geste 1 — D39, contrat de gouvernance du registre

> Rédigée sous l'**ancien** format (contexte / options / décision / impact) :
> elle décide du nouveau contrat, elle ne peut pas déjà l'appliquer.

### D39 — Contrat de gouvernance du registre : nature, statut et provenance sont trois axes — CONFIRMÉE (2026-08-05)

- **Contexte** : D33 a porté pendant un jour une explication de mécanisme que
  personne n'avait exécutée, et cette explication a servi de base à un arbitrage
  demandé à l'utilisateur. La mesure l'a démentie sur ses trois affirmations. Le
  même glissement s'est reproduit deux fois dans la même semaine : une prédiction
  de l'agent est devenue un « contre-exemple acquis » avant d'être réfutée par
  exécution, et une hypothèse a été inscrite dans une colonne intitulée
  « Trouvaille » du document même qui diagnostiquait ce défaut. Le registre ne
  distingue aujourd'hui que trois statuts d'arbitrage ; rien n'y dit **d'où une
  affirmation tient sa force**.
- **Options** : **A** ajouter un statut de décision décrivant la maturité des
  preuves ; **B** poser trois axes indépendants — la nature de l'énoncé, le
  statut de l'arbitrage, la provenance de l'affirmation ; **C** s'en remettre à
  la vigilance des rédacteurs.
- **Décision** (utilisateur) : **B**. Un statut de décision ne peut pas porter
  une propriété qui n'est pas de l'ordre de l'arbitrage ; et la vigilance est
  précisément ce qui a échoué trois fois.
- **Ce que le contrat pose** :

  1. **Trois axes.**

     | Axe | Valeurs | Ce qu'il gouverne |
     |---|---|---|
     | Nature | décision, constat, hypothèse, plan | où l'énoncé doit vivre |
     | Statut d'arbitrage | `PENDING`, `CONFIRMÉE`, `CADUQUE` | l'autorisation d'agir |
     | Provenance | `[MESURÉ]`, `[LU]`, `[HYPOTHÈSE]` | la force de l'affirmation |

  2. **Sémantique stricte des balises.** `[MESURÉ]` désigne ce qu'une exécution
     a observé, et renvoie à une commande relançable avec ses entrées épinglées :
     révision, racines, **classpath**, configuration, corpus. Le classpath
     appartient à l'identité d'une mesure : sans lui, le moteur rend d'autres
     verdicts. `[LU]` désigne ce qui est visible dans une révision précise du
     code, et ne vaut jamais à lui seul comme preuve d'un comportement exécuté.
     `[HYPOTHÈSE]` couvre toute extrapolation causale non exécutée. **Chaque
     proposition atomique porte sa propre balise**, une même analyse pouvant
     enchaîner les trois.

  3. **Append-only.** Un énoncé daté ne se réécrit pas. Une décision dont
     l'explication est démentie conserve son texte, annoté, et reçoit une section
     de requalification datée. La convention existe déjà dans le chantier — la
     ligne conservée avec statut changé, le point de reprise empilé et marqué
     `PÉRIMÉ`, l'entrée de découverte amendée en gras — elle devient une règle.

  4. **Deux rubriques nouvelles à CHANTIER.md.** « Constats et découvertes »
     (énoncés atomiques `[MESURÉ]` ou `[LU]`, avec preuve, portée et lot
     éventuel) et « Hypothèses à instruire ».

  5. **Une hypothèse porte deux champs, jamais un.**

     | Champ | Valeurs |
     |---|---|
     | État épistémique | `OUVERTE`, `ÉTAYÉE`, `RÉFUTÉE`, `REMPLACÉE` |
     | État d'instruction | `À PRÉPARER`, `PRÊTE`, `EN COURS`, `EXÉCUTÉE`, `BLOQUÉE` |

     Un blocage nomme son prérequis et l'étape chargée de le lever. Une fixture
     absente ne dit rien de la vérité d'une hypothèse : elle dit que son
     protocole n'est pas exécutable. Ces états ne donnent aucune autorisation
     d'agir. Une hypothèse réfutée est **conservée avec son résultat**.

  6. **Le registre est versionné.** Les documents du chantier vivent sous
     `hexaglue-next/docs/chantier/`, dans le dépôt du réacteur. Une décision
     peut donc citer un commit, et une réécriture après coup est visible en diff.

- **Impact** : l'en-tête de DECISIONS.md gagne les trois axes et la règle
  append-only ; CHANTIER.md gagne les deux rubriques ; le contrat s'applique **à
  partir de l'entrée suivante**. Une évolution future de cette grammaire
  remplacera D39 sans rendre caduque aucune décision prise sous elle.

---

# Geste 2 — La grammaire appliquée

Deux insertions, sans réécriture de l'existant.

**En tête de `DECISIONS.md`**, après les trois règles actuelles :

> - **Provenance (D39)** : toute affirmation factuelle porte `[MESURÉ]`, `[LU]`
>   ou `[HYPOTHÈSE]`. `[MESURÉ]` renvoie à une commande relançable et à ses
>   entrées — révision, racines, classpath, configuration, corpus. Une décision
>   ne se fonde jamais sur un `[HYPOTHÈSE]` non signalé comme tel.
> - **Append-only (D39)** : un énoncé daté ne se réécrit pas. Ce qui est démenti
>   reçoit une requalification datée, à côté.

**Dans `CHANTIER.md`**, la rubrique « Découvertes en cours de chantier » est
renommée **« Constats et découvertes »** — ses six entrées existantes sont
conservées telles quelles, sans balise rétroactive — et une rubrique
**« Hypothèses à instruire »** s'ouvre après elle.

---

# Geste 3 — D33 amendée, append-only

Le texte existant est **conservé intégralement**. Deux ajouts l'encadrent.

**En tête de l'entrée**, juste après le titre :

> ⚠ **`[HYPOTHÈSE — DÉMENTIE PAR MESURE le 2026-08-05]`** — le contexte ci-dessous
> énonce un mécanisme que l'exécution ne produit pas. Il est conservé tel quel :
> voir la requalification en fin d'entrée.

**En fin d'entrée**, après « Ne pas agir avant arbitrage » :

> #### Requalification du 2026-08-05
>
> - **`[MESURÉ]`** — `Email` sort **`DOMAIN_EVENT` à `HIGH`**, ce qui reste faux
>   sur le domaine : le symptôme qui a ouvert cette décision est confirmé. Mais
>   il porte **un seul signal**, `[S3/HIGH] ANNOUNCED_BY(NotificationSender)`, et
>   **aucun signal de possession**. Commande relançable et entrées épinglées :
>   `docs/chantier/20260805-convergence/07-B-mesure-d33.md`.
> - **`[MESURÉ]`** — sur le même run, `Money` porte cinq signaux `OWNED_BY`. R3b
>   fonctionne ; il ne parle jamais d'`Email`.
> - **`[LU]`** — `Lifecycle.isPart` écarte tout type que
>   `Shapes.readsAsIdentity` reconnaît, et `readsAsIdentity = isImmutable &&
>   wrapsSingleValue`. `record Email(String value)` n'est donc jamais une partie
>   d'un agrégat.
> - **`[LU]`** — R7 émet un jeton nommant le **port**, sur des types transportés
>   `.distinct()` : les deux méthodes de `NotificationSender` produisent une
>   seule clé, pas deux.
>
> **Ce que la mesure invalide** : les trois affirmations du contexte — « deux
> signaux R7 » (il y en a un), « la possession par `Customer` (R3b, un signal) »
> (il y en a zéro), et la pesée entre les deux (**il n'y en a aucune** : le
> verdict est acquis 1-0, sans adversaire).
>
> **Ce qui survit** : deux des trois questions à instruire. « R7 devrait-il se
> taire sur un type que le domaine GARDE ? » et « le rôle EVENT_PUBLISHER est-il
> trop large ? » restent posées, et R7 est désormais **le seul à parler** sur ce
> verdict. La première question — « la possession devrait-elle primer la
> publication à palier égal ? » — est **sans objet** : il n'y a pas de signal de
> possession à faire primer.
>
> **Ce qui est ouvert ailleurs** : la cause réelle dépasse ce cas et ne se greffe
> pas rétroactivement au périmètre de D33. Elle est enregistrée en constat C-2 et
> en hypothèse H-2, et son arbitrage revient à la famille « modèle métier » de la
> file de réévaluation (E7), avec D7, D13 et D16.
>
> **Statut inchangé : `PENDING`.** Les questions survivantes ne sont pas
> tranchées.

---

# Geste 4 — D40, adoption du plan de reprise

> Rédigée sous la **nouvelle** grammaire.

### D40 — Adoption du plan de reprise du chantier — CONFIRMÉE (2026-08-05)

- **Contexte** `[MESURÉ]` : le corpus d'acceptation, qui sert de cliquet de
  clôture à chaque jalon, compte 143 / 6 / 5 scénarios. **122 des 143 du profil 1
  portent encore le nom d'une méthode de test de l'ancien réacteur** ; **77 ne
  posent qu'un type et 120 au plus deux** ; **80 n'attestent que du silence** ; et
  sur les trois profils, les attentes ne portent que `HIGH` (122) et `EXPLICIT`
  (50), **aucune `MEDIUM` ni `LOW`** — donc la porte de génération, qui compare
  `confidence >= HIGH`, n'a jamais été exercée comme un refus. Commande :
  `docs/chantier/20260805-convergence/mesures.sh`.
- **Contexte** `[LU]` : `EvidenceTier` traduit S2, S3 et S4 sur la même valeur
  `Confidence.HIGH` ; `Classifier` repart d'une base de faits vide à chaque tour
  et sort quand les verdicts ne bougent plus, là où le doc 07 §4.1 annonce une
  saturation monotone ; sept fichiers de règles sur vingt-huit concluent d'une
  absence ; jqwik, annoncé au doc 07 §7 pour tester le point fixe, est absent de
  tous les `pom.xml`.
- **Décision** (utilisateur, 2026-08-05) : **le plan de reprise est adopté** dans
  sa révision épinglée ci-dessous, et **M7b est gelé après son lot 4**.
- **Les quatre arbitrages, recopiés** — ils font foi ici, indépendamment de toute
  évolution ultérieure du plan :

  1. **Ordre des étapes** : E0 gel, E1 séparer palier / confiance / autorisation,
     E2 nommer la sémantique du solveur, **E2b** produire le matériel de
     caractérisation manquant, E3a caractériser, E3b décider les garanties, E4a
     construire l'étalon, E4b calibrer, E5 périmètre des sources, E6 politiques
     des absences, E7 familles de décisions, E8 reprise de M7b. **E5 et E6 ne
     sont pas regroupées** : la première porte la complétude des entrées, la
     seconde la sémantique des règles sur une entrée connue.
  2. **E5 est maintenue** : l'axe d'autorisation de E1 traite le droit d'agir, il
     ne rend pas l'observation complète.
  3. **Les 122 scénarios transplantés sont requalifiés un par un**, quatre issues
     — conserver et renommer, réécrire en scénario câblé, remplacer par une
     propriété, supprimer. Les survivants perdent le nom des anciennes méthodes
     de test et portent celui de l'invariant.
  4. **La relecture des attentes se fait en quatre temps** : passe aveugle de
     l'agent sans afficher le verdict courant, arbitrage utilisateur, révélation
     et score, enregistrement par une session distincte. L'agent ne fabrique pas
     seul l'oracle qu'il devra satisfaire ; le dénominateur reste exhaustif.

- **Les six règles de conduite** qui donnent leur sens aux arbitrages, et qui
  s'ajoutent aux treize de CHANTIER.md en primant en cas de conflit :

  1. Une trouvaille faite dans un lot est enregistrée, jamais tranchée ni
     implémentée dans ce lot.
  2. Toute mesure qui étaye une décision est une commande relançable, avec ses
     entrées et son résultat attendu.
  3. Une décision de portée générale présente un cas nominal, un contre-exemple
     et son effet sur plusieurs projets.
  4. « Débloque le lot » est un impact de calendrier, jamais un argument de
     justesse.
  5. Le cliquet ne se déplace pas par celui qui le déplace.
  6. **Caractériser n'est pas garantir** : mesurer une propriété et exiger
     qu'elle soit vraie sont deux gestes séparés, dans deux sessions séparées.

- **Révision adoptée** :
  `docs/chantier/20260805-convergence/PLAN.md`, commit **`ab52fb5`**,
  `sha256 d7ec99fc0dfcf5e494c7ecc8cce90c39b414b7c6b6f802b6fe2bfba0ebadedac`.
  Une révision ultérieure du plan ne modifie pas cette décision ; si un seul
  volet change, une décision nouvelle remplace explicitement la clause concernée.
- **Impact** : M7 passe `SUSPENDU` ; la règle 13 est réécrite ; les constats et
  hypothèses sont portés aux deux rubriques nouvelles ; **rien n'est corrigé dans
  le code par cette décision**.

---

# Geste 5 — Les sept constats et les cinq hypothèses

## Constats et découvertes (ajouts du 2026-08-05)

| # | Énoncé | Provenance | Portée | Étape |
|---|---|---|---|---|
| **C-1** | `Email` sort `DOMAIN_EVENT` à `HIGH` sur `case-study-ecommerce` — faux sur le domaine — avec **un seul signal** (`ANNOUNCED_BY`) et **aucun signal de possession** : aucune pesée n'a lieu | `[MESURÉ]` | un banc | E7 |
| **C-2** | `Lifecycle.isPart` écarte tout type que `Shapes.readsAsIdentity` reconnaît, donc **toute enveloppe immuable à une valeur** est invisible à la composition, qu'elle soit une identité ou une valeur | `[LU]` | le jeu de règles | E7 |
| **C-3** | Sur `case-study-banking`, cinq agrégats reconnus : `Address` reçoit son `OWNED_BY`, `Money` en reçoit cinq, `Email` et `Iban` **zéro** — et sortent tous deux sur un duel parfait IDENTIFIER 100 / VALUE_OBJECT 100 | `[MESURÉ]` | second projet | E7 |
| **C-4** | Le classpath change les verdicts : sans lui, `spring-petclinic` passe de 3 agrégats à 1 et de 10 à 14 non classés, `Owner` cessant d'être un agrégat parce que `extends JpaRepository` ne se ferme qu'en bytecode | `[MESURÉ]` | toute mesure | E4a |
| **C-5** | Deux exceptions immuables de banking sortent `VALUE_OBJECT` à `HIGH` par `IMMUTABLE_SHAPE` ; le banc e-commerce ne pouvait pas le voir, son `hexaglue.yaml` excluant le paquet `exception` | `[MESURÉ]` | le jeu de règles | E4a |
| **C-6** | `HG-FRONTEND-006` ne compte les récupérations du parser que globalement, sans nommer les déclarations incomplètes — insuffisant pour un oracle relu type par type | `[MESURÉ]` | le canal de diagnostics | E4a |
| **C-7** | La clé de déduplication d'un signal est `(sujet, kind, palier, jeton fact(), distance)` : elle contient le **texte écrit à la main** et **pas la `RuleId`**, donc modifier un message change un poids | `[LU]` | l'agrégateur | E3a |

## Hypothèses à instruire (ouvertes le 2026-08-05)

| # | Proposition falsifiable | Épistémique | Instruction | Protocole | Étape |
|---|---|---|---|---|---|
| **H-1** | Le pipeline écarte sa sortie générée, R5 devient inatteignable, R5b conclut de cette absence, et le cycle se referme : l'adapter est régénéré | `OUVERTE` | **`BLOQUÉE`** — demande un générateur d'adapters pilotants ; le backend rest n'existe pas (M7b gelé au lot 4) | deux exécutions successives sur un projet dont l'adapter pilotant est généré | levée en **E8** |
| **H-2** | L'invisibilité des enveloppes déplace des attentes relues sur les 154 scénarios | `OUVERTE` | `À PRÉPARER` — demande une **variante expérimentale** du moteur | passer le corpus avec et sans l'exclusion de `readsAsIdentity`, sans toucher au comportement de référence | E3a |
| **H-3** | Adopter la clé `(ruleFamily, subject, candidateKind, ancre)` déplace le corpus **dans les deux sens** | `OUVERTE` | `À PRÉPARER` — variante expérimentale également | comparer les verdicts sous les deux clés | E3a |
| **H-4** | Le point fixe n'est pas unique : le résultat dépend du départ à `Verdicts.none()` | `OUVERTE` | `PRÊTE` | relancer depuis des états initiaux admissibles | E3a (P3a) |
| **H-5** | ~~Masquer les détenteurs de `InventoryUseCases` le rend générable~~ | **`RÉFUTÉE`** | `EXÉCUTÉE` le 2026-08-05 | exclusion de `com.acme.shop.application` : le type reste UNCLASSIFIED, l'implémenteur partageant le paquet des détenteurs | — |

> **Note sur H-2 et H-3** : leurs protocoles demandent une variante
> expérimentale du moteur. E3a doit les enregistrer comme **interventions de
> caractérisation** — isolées, reproductibles, sans modifier le comportement de
> référence ni les goldens. Leur sortie est une mesure destinée à E3b, jamais un
> correctif anticipé.
>
> **Note sur P0a et P0b** : ces deux propriétés du plan ont la même instruction
> bloquée qu'H-1, mais leur prérequis est levable — c'est l'objet de **E2b**.
> H-1 seule reste bloquée au-delà.

---

# Geste 6 — M7 `SUSPENDU` et le point de reprise

**Ligne du tableau des jalons**, en remplacement de la ligne M7 actuelle
(l'ancienne valeur `EN COURS` reste visible dans l'historique git) :

| Jalon | Contenu | Statut | Notes |
|---|---|---|---|
| M7 | jpa + rest (seuil de certitude) | **SUSPENDU** (2026-08-05) | M7a clos ; M7b lots 1-4 faits et poussés, **lots conservés, reprise interdite** avant les portes définies par D40. Le vocabulaire des statuts gagne `SUSPENDU` : `EN COURS` laissait entendre que le lot suivant pouvait s'ouvrir. |

**Point de reprise**, en tête, l'actuel étant empilé dessous et marqué `PÉRIMÉ`
selon la convention maison :

> ### Point de reprise (au 2026-08-05, **M7 SUSPENDU — plan de reprise adopté**)
>
> Le chantier ne reprend pas à M7b. **D40** adopte un plan de reprise en dix
> étapes, E0 à E8, qui répare l'étalon et le moteur avant que la trajectoire
> produit continue. Le plan est
> `docs/chantier/20260805-convergence/PLAN.md`, commit `ab52fb5`.
>
> **La cause** : le chantier a été conduit en pensant migration d'abord, et ce
> qui a été transplanté sans requalification est **la définition de ce qui est
> correct**. Le détail et les mesures sont dans
> `docs/chantier/20260805-convergence/`.
>
> **Prochaine action : E1** — casser le couplage palier / confiance /
> autorisation. Ni E5 ni E7 ne s'ouvrent avant leur tour ; **aucune décision de
> la file de réévaluation ne se rouvre**.
>
> **État du code** : cinq commits poussés, `5a53135` → `9373fdc`, arbre propre.
> 1 401 tests, `make ci` vert, 12/12 en intégration, cliquet 143/143 + 6/6 + 5/5.
>
> **Le registre porte une décision `PENDING` : D33**, dont l'explication a été
> démentie par la mesure et requalifiée le 2026-08-05. Ne pas agir dessus.

---

# Geste 7 — La règle 13 réécrite

L'ancien texte est **conservé** juste en dessous, sous la mention
« *texte en vigueur jusqu'au 2026-08-05, remplacé par D40* ».

Le texte proposé est celui de A au tour 11, **avec un ajout** signalé en gras :

> **13. Clôture d'une étape ou d'un jalon (D12, amendée le 2026-08-04, réécrite
> par D40 le 2026-08-05).** La clôture se fait contre les critères de sortie
> déclarés **avant** l'exécution. Une étape de **caractérisation** est clôturable
> lorsque ses mesures sont relançables, ses résultats — y compris les
> contre-exemples rouges et les instructions **`BLOQUÉE` nommant leur
> prérequis** — sont consignés, et qu'aucune correction implicite n'a été
> introduite. Une étape de **conformité** est clôturable lorsque ses portes
> approuvées sont vertes. Toute mesure précise la révision, les racines, le
> **classpath**, la configuration, la commande et le corpus. La clôture comprend
> la relecture ligne à ligne des sections applicables du plan et du document 07
> **dans sa version corrigée par E2** — le §4.1 énonce aujourd'hui une monotonie
> que le solveur n'a pas —, la mise en cohérence du registre et du journal, et
> l'enregistrement explicite de tout écart différé. **Un corpus n'est une porte
> que si la décision qui l'institue comme oracle est citée.**

---

# Ce que je demande à A

1. **Les textes eux-mêmes.** Est-ce qu'une des sept rédactions dit plus, ou
   moins, que ce qui a été décidé ? C'est le risque principal d'un portage :
   l'exécutant reformule et la reformulation devient la décision.
2. **D33, geste 3.** La requalification enterre-t-elle le symptôme ou le
   conserve-t-elle vraiment ? Et l'annotation en tête d'entrée est-elle au bon
   endroit — un lecteur pressé doit-il rencontrer l'avertissement avant le texte
   démenti, ou après ?
3. **Le renvoi de la cause réelle à E7.** D33 est amendée mais son périmètre
   n'est pas élargi ; C-2 et H-2 portent la question générale. Est-ce le bon
   découpage, ou faut-il une décision `PENDING` distincte dès maintenant sur
   l'invisibilité des enveloppes ?
4. **La règle 13.** Mon ajout sur le doc 07 est-il justifié ? Une règle de
   clôture qui impose de relire un document contenant un énoncé faux me paraît
   devoir le dire ; l'objection est que la règle se trouve alors datée d'un
   défaut qui disparaîtra à E2.
5. **Ce que le portage oublie.** Comme aux tours 03 et 11 : cherche le trou,
   pas l'approbation.

Deux choses sur lesquelles je ne demande **pas** d'avis : les quatre arbitrages
de l'utilisateur, et la visibilité publique du dépôt. Les deux sont tranchés.
