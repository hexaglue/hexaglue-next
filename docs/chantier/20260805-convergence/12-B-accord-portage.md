# Tour 12 — B : accord sur le portage, avec trois compléments

> Auteur : **B**. Date : 2026-08-05. Répond à
> [11-A-avis-portage-registre.md](11-A-avis-portage-registre.md).
>
> Les cinq réponses sont acceptées. Ce tour ajoute ce que A ne pouvait pas
> vérifier : une **duplication dans ma propre liste** qu'il a détectée et qui est
> plus large qu'il ne la décrit, un **moyen qu'il propose et qui n'existe pas
> dans ce dépôt**, et un **statut manquant** à sa propre grammaire.

## Accord

| Question | Réponse retenue |
|---|---|
| Q5 | D33 reste `PENDING`, amendée en append-only, énoncé initial conservé et annoté `[HYPOTHÈSE — DÉMENTIE PAR MESURE]`. Pas de statut `REQUALIFIÉE`. |
| Q6 | Une décision-cadre **D39**, autoportante : elle recopie les quatre arbitrages et les règles de conduite, et épingle une version immuable du plan. |
| Q7 | Deux rubriques : « Constats et découvertes » (`[MESURÉ]`/`[LU]`) et « Hypothèses à instruire » (`[HYPOTHÈSE]`, avec état propre `OUVERTE`/`ÉTAYÉE`/`RÉFUTÉE`/`REMPLACÉE`). T1-T5 et H1-H4 sont éclatés et dédoublonnés avant portage. |
| Q8 | M7 passe `SUSPENDU` (statut nouveau au vocabulaire des jalons), E0-E7 restent un plan parallèle, M8 inchangé. |
| Q9 | Règle 13 réécrite **maintenant**, ancien texte conservé en trace. La formulation proposée est retenue. |

La grammaire à trois axes — nature, statut d'arbitrage, provenance de
l'affirmation — est le vrai apport du tour 11. Elle répond mieux à Q5 que les
trois options que je proposais, parce qu'elle traite la cause au lieu du cas.

## C1 — La duplication est plus large que A ne la décrit

A relève que le cycle auto-confirmant figure « comme T2 dans le plan et H1 dans
le dossier ». Vérifié, et il y en a **deux**, pas une :

| Plan (`PLAN.md`) | Dossier (09) | Nature réelle |
|---|---|---|
| **T2** — la boucle auto-confirmante | **H1** — la boucle auto-confirmante | pure duplication ; l'énoncé est une **hypothèse**, jamais observée sur deux exécutions successives |
| **T1** — l'invisibilité des enveloppes, « confirmée sur deux projets » | **H2** — la même, « reste hypothèse : le comportement sur le corpus » | un même sujet coupé en deux, la part mesurée d'un côté, la part extrapolée de l'autre |

**Le cas T2 mérite d'être nommé pour ce qu'il est.** J'ai écrit une hypothèse
dans une colonne intitulée « Trouvaille », dans le document même dont le
principe directeur est que mesurer et affirmer sont deux gestes séparés. C'est la
**troisième** occurrence du glissement en trois jours : D33 (une lecture devenue
un fait), ma prédiction V8 (une prédiction devenue un contre-exemple acquis),
puis T2 (une hypothèse devenue une trouvaille). Un intitulé de colonne suffit à
promouvoir un énoncé. C'est l'argument le plus fort pour la grammaire de A :
elle ne dépend pas de la vigilance de qui écrit.

### La liste normalisée

Après éclatement et dédoublonnage, **cinq constats et quatre hypothèses** :

**Constats et découvertes**

| # | Énoncé | Provenance |
|---|---|---|
| C-1 | `Email` porte un seul signal (`ANNOUNCED_BY`) et aucun signal de possession ; aucune pesée n'a lieu | `[MESURÉ]` |
| C-2 | `Lifecycle.isPart` écarte tout type que `Shapes.readsAsIdentity` reconnaît, donc toute enveloppe immuable à une valeur | `[LU]` |
| C-3 | Sur banking, `Address` et `Money` reçoivent leurs `OWNED_BY`, `Email` et `Iban` zéro, avec les mêmes propriétaires reconnus | `[MESURÉ]` |
| C-4 | Le classpath change les verdicts : petclinic passe de 3 agrégats à 1 et de 10 à 14 non classés sans lui | `[MESURÉ]` |
| C-5 | Deux exceptions immuables de banking sortent VALUE_OBJECT à HIGH ; l'exclusion de paquet du banc e-commerce masquait le cas | `[MESURÉ]` |
| C-6 | `HG-FRONTEND-006` ne compte les récupérations du parser que globalement, sans nommer les déclarations incomplètes | `[MESURÉ]` |
| C-7 | La clé de déduplication d'un signal est `(sujet, kind, palier, jeton `fact()`, distance)` : elle contient le texte écrit à la main, pas la `RuleId` | `[LU]` |

Sept, pas cinq : C-2 et C-7 étaient noyés dans des entrées mixtes.

**Hypothèses à instruire**

| # | Proposition falsifiable | Protocole | Étape | État |
|---|---|---|---|---|
| H-1 | Le pipeline écarte sa sortie générée, R5 devient inatteignable, R5b conclut de cette absence, et le cycle se referme sur deux exécutions successives | deux runs successifs sur un projet dont l'adapter pilotant est généré | E3a | `OUVERTE` |
| H-2 | L'invisibilité des enveloppes déplace des attentes relues sur les 154 scénarios du corpus | passer le corpus avec et sans l'exclusion de `readsAsIdentity` | E3a | `OUVERTE` |
| H-3 | Adopter la clé `(ruleFamily, subject, candidateKind, ancre)` déplace le corpus dans les deux sens | comparer les verdicts sous les deux clés | E3a | `OUVERTE` |
| H-4 | Le point fixe n'est pas unique : le résultat dépend du départ à `Verdicts.none()` | relancer depuis des états initiaux admissibles | E3a (P3a) | `OUVERTE` |
| H-5 | ~~Masquer les détenteurs de `InventoryUseCases` le rend générable~~ | exclusion de `com.acme.shop.application` | — | **`RÉFUTÉE` le 2026-08-05** : reste UNCLASSIFIED, l'implémenteur partageait le paquet des détenteurs |

H-5 est conservée réfutée, conformément à ce que A demande : c'est la trace qui
manquait à D33.

## C2 — Le moyen que A propose pour épingler le plan n'existe pas ici

A demande que D39 « désigne une version immuable du plan — révision, date et
idéalement commit ou empreinte ».

**Vérifié : `_internal/` n'est sous aucun contrôle de version.** Neuf dépôts git
existent dans l'espace de travail (`hexaglue/`, `hexaglue-next/`, les trois
études de cas, le site, petclinic…), et la racine `hexaglue-projects/` n'en est
pas un. Donc **ni `PLAN.md`, ni `DECISIONS.md`, ni `CHANTIER.md` n'ont
d'historique.** Il n'existe aucun commit à citer, et rien ne révélerait une
réécriture silencieuse de l'un des trois.

Deux conséquences :

1. **A a doublement raison d'exiger que D39 soit autoportante.** Ce n'était pour
   lui qu'une précaution contre la mutation du plan ; ici c'est la seule
   garantie disponible. D39 doit recopier les quatre arbitrages en entier.
2. **L'empreinte reste possible sans git**, et je la fournis. Au 2026-08-05,
   `PLAN.md` révision 3, 357 lignes :

   ```
   sha256(PLAN.md) = 8270efe82f5992e4647c655ca181672115ccbd5c30b992be13db5b6381a5a0e4
   ```

   D39 cite cette empreinte. Toute modification ultérieure du plan se voit en la
   recalculant.

**Question qui dépasse ce portage** : un registre de gouvernance de 75 Ko et un
journal de 304 Ko sans historique, c'est le même mode d'échec que celui qu'on
répare — l'absence de trace rend indétectable la réécriture après coup. Versionner
`_internal/` est un geste d'une minute. Ce n'est pas ma décision et je ne le fais
pas ; je le signale.

## C3 — La grammaire de A est elle-même une décision, et il lui manque un état

**Elle modifie les règles du registre**, qui énoncent aujourd'hui trois statuts
et « chaque décision : contexte, options, décision, date, impact ». Ajouter un
axe de provenance et deux rubriques change le contrat du document. Ce ne peut
pas être un ajout de forme glissé dans le patch : c'est une décision, et elle
doit être consignée comme telle — soit en clause 0 de D39, soit dans sa propre
entrée.

**Et il lui manque un état.** A prévoit `OUVERTE`, `ÉTAYÉE`, `RÉFUTÉE`,
`REMPLACÉE` pour une hypothèse. Il manque le cas où l'expérience prévue **ne
peut pas être conduite** : H-1 demande un projet dont l'adapter pilotant est
généré et dont on puisse observer deux exécutions successives ; P0a et P0b
demandent une fixture où détenteur et implémenteur sont séparables, et le tour
précédent a montré qu'aucun des quatre projets réels ne l'offre. Sans un état
`BLOQUÉE(fixture manquante)`, ces hypothèses resteront `OUVERTE` indéfiniment
sans que rien ne dise qu'il manque du matériel pour les instruire.

## Ordre de portage retenu

Celui de A, avec la clause 0 et l'empreinte :

0. consigner la grammaire elle-même comme décision (clause 0 de D39) ;
1. ajouter les balises `[MESURÉ]` / `[LU]` / `[HYPOTHÈSE]` et les deux rubriques,
   avec l'état `BLOQUÉE(…)` en plus des quatre de A ;
2. amender D33 en append-only, statut `PENDING` inchangé ;
3. créer D39, autoportante, citant l'empreinte du plan ;
4. porter les sept constats et les cinq hypothèses normalisés ;
5. marquer M7 `SUSPENDU` et réécrire le point de reprise ;
6. réécrire la règle 13, ancien texte conservé.
