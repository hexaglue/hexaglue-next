# Convergence — reprise du chantier de refactoring

Ouvert le 2026-08-05, après l'arrêt du chantier demandé par l'utilisateur au
milieu de M7b (voir le point de reprise en tête de
[CHANTIER.md](../20260731-refactoring-audit/CHANTIER.md)).

Ce répertoire est l'espace d'échange entre deux analyses qui ne se recouvrent
pas, et dont il faut tirer **un** plan. Il ne contient aucun code, ne pilote
aucun lot, et rien de ce qui s'y écrit ne s'implémente : la sortie est une
proposition, l'arbitrage reste à l'utilisateur.

## Le sujet

Le chantier a été conduit en pensant migration d'abord, réécriture ensuite. La
conception cible est saine sur le papier ; ce qui a été transplanté sans être
requalifié, c'est **la définition de ce qui est correct** — le corpus
d'acceptation, hérité des tests unitaires de l'ancien réacteur, devenu le
cliquet de clôture de chaque jalon. Trois dettes techniques se sont installées
derrière ce mauvais étalon. Le détail est dans
[00-etat-du-desaccord.md](00-etat-du-desaccord.md).

## Participants

| Sigle | Qui | Ce qu'il peut faire | Ce qu'il doit donc |
|---|---|---|---|
| **A** | analyse sur documents | lit CHANTIER.md, DECISIONS.md, les docs 01-10 | énoncer des affirmations **falsifiables** et signaler ce qu'il n'a pas pu vérifier |
| **B** | agent avec accès au dépôt | lit le code, exécute, mesure | fournir pour chaque fait une **référence `fichier:ligne` ou une commande** |

L'asymétrie est le sujet, pas un problème : A raisonne sur ce que le chantier
dit de lui-même, B sur ce que le code fait. Les deux se sont déjà contredits
utilement.

## Règles de l'échange

1. **Un tour = un fichier**, numéroté, un seul auteur :
   `NN-<sigle>-<slug>.md`. On n'édite pas le fichier d'un autre tour ; on en
   écrit un nouveau.
2. **Toute affirmation factuelle porte sa preuve** : une référence
   `fichier:ligne`, ou une section de [`mesures.sh`](mesures.sh). Une
   affirmation non vérifiée se marque `[non vérifié]` — c'est licite, ce n'est
   pas disqualifiant, mais ça doit se voir.
3. **La mesure appartient à celui qui décide.** Un chiffre ne vaut que s'il se
   relance. Tout chiffre nouveau entre dans `mesures.sh` dans le même tour que
   l'affirmation qu'il soutient.
4. **[00-etat-du-desaccord.md](00-etat-du-desaccord.md) est l'état partagé**,
   pas l'historique. Il se met à jour à chaque tour : ce qui est acquis, ce qui
   reste en litige, ce que la mesure a tranché. Un lecteur pressé ne lit que
   lui.
5. **Une trouvaille ne se tranche pas dans le tour qui la fait.** C'est la
   règle dont l'absence a produit les dérives du chantier ; elle vaut d'abord
   ici.
6. **`PLAN.md` est la sortie**, et il n'existe qu'une fois le désaccord vidé.
   Il propose un ordre de travail à l'utilisateur, qui décide seul ce qui
   s'exécute et dans quel jalon.

## Ce que ce répertoire ne fait pas

- Il ne rouvre pas les décisions du registre. La file de réévaluation par
  familles est une **proposition** de `PLAN.md`, appliquée ailleurs.
- Il ne modifie ni `hexaglue-next/`, ni `hexaglue/` (gelée), ni le registre.
- Il ne remplace pas [CHANTIER.md](../20260731-refactoring-audit/CHANTIER.md) :
  quand un plan sort d'ici, il s'inscrit là-bas, et ce répertoire se clôt.

## Index

| Fichier | Auteur | Contenu |
|---|---|---|
| [00-etat-du-desaccord.md](00-etat-du-desaccord.md) | commun | état partagé, tenu à jour |
| [PLAN.md](PLAN.md) | commun | **la sortie** : huit étapes, proposées à l'arbitrage |
| [01-A-arbitrage.md](01-A-arbitrage.md) | A | arbitrage des quatre litiges, résidu sur L1 |
| [02-B-verifications.md](02-B-verifications.md) | B | vérifications sur le tour 01 |
| [03-A-relecture-critique.md](03-A-relecture-critique.md) | A | relecture critique du plan, six corrections demandées |
| [04-B-reponse.md](04-B-reponse.md) | B | corrections acceptées, trois `[non vérifié]` levés |
| [05-A-reponse.md](05-A-reponse.md) | A | désaccord clos, trois formulations finales |
| [06-B-cloture.md](06-B-cloture.md) | B | clôture ; une trouvaille part au registre (D33) |
| [07-B-mesure-d33.md](07-B-mesure-d33.md) | B | **mesure** : le mécanisme énoncé par D33 n'existe pas |
| [08-B-mesures-multi-projets.md](08-B-mesures-multi-projets.md) | B | **mesure** : petclinic + banking ; six résultats, dont le classpath et la boucle auto-confirmante de R5b |
| [09-B-dossier-portage.md](09-B-dossier-portage.md) | B | **à soumettre à A** : faits, lectures, hypothèses, et cinq questions d'écriture du registre |
| [10-B-extrait-registre.md](10-B-extrait-registre.md) | B | la structure réelle du registre et du chantier, verbatim — matière de Q5 à Q9 |
| [11-A-avis-portage-registre.md](11-A-avis-portage-registre.md) | A | avis sur les cinq questions ; grammaire à trois axes |
| [12-B-accord-portage.md](12-B-accord-portage.md) | B | accord ; duplication normalisée, `_internal/` non versionné, état manquant |
| [signaux/](signaux/) | B | harnais affichant les signaux retenus sur un type, hors du réacteur |
| [MESURES.md](MESURES.md) | B | sortie datée de `mesures.sh`, avec le commit mesuré |
| [mesures.sh](mesures.sh) | B | les mesures, relançables |

**État au 2026-08-05.** Douze tours, en deux temps. **Le plan** : désaccord vidé
en six tours, `PLAN.md` en **révision 3**, quatre arbitrages pris par
l'utilisateur. **Le portage** : deux campagnes de mesure (07, 08) puis quatre
tours sur la forme à donner au registre (09 à 12), clos par un accord complet.

Ce qui attend est **l'exécution du portage** dans
[DECISIONS.md](../20260731-refactoring-audit/DECISIONS.md) et
[CHANTIER.md](../20260731-refactoring-audit/CHANTIER.md), en six gestes listés
en fin de [12-B-accord-portage.md](12-B-accord-portage.md). Ni A ni B ne doivent
de tour.

Trois choses sortent de l'échange et vont au registre plutôt qu'au plan : **D33
porte une explication démentie par la mesure** (07), **`_internal/` n'est sous
aucun contrôle de version** (12, C2), et **la grammaire `[MESURÉ]` / `[LU]` /
`[HYPOTHÈSE]` est elle-même une décision** puisqu'elle change les règles du
registre (12, C3).

Les deux documents d'ouverture vivent encore dans le dossier d'audit :
[20-analyse-refactoring.md](../20260731-refactoring-audit/20-analyse-refactoring.md)
(A, premier tour) et
[20-analyse-refactoring-v2.md](../20260731-refactoring-audit/20-analyse-refactoring-v2.md)
(A, second tour, après la réponse de B). La réponse de B au premier tour n'a pas
été consignée ; elle est reprise dans l'état du désaccord.
