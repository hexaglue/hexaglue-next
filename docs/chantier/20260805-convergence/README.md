# Reprise du chantier — plan, mesures et outillage

Ouvert le 2026-08-05, après l'arrêt du chantier au milieu de M7b. Ce répertoire
portait d'abord un échange entre deux analyses ; il porte désormais **le plan de
reprise et ce qui sert à l'exécuter**.

## Pourquoi le chantier s'est arrêté

Il a été conduit en pensant **migration d'abord**. L'architecture cible est
saine ; ce qui a été transplanté sans être requalifié, c'est **la définition de
ce qui est correct** — le corpus d'acceptation, hérité des tests unitaires de
l'ancien réacteur, devenu le cliquet de clôture de chaque jalon. Trois dettes se
sont installées derrière ce mauvais étalon : une échelle de confiance écrasée,
une sémantique de solveur jamais énoncée, et des conclusions tirées d'absences
qui autorisent la génération.

Les chiffres qui l'établissent sont dans [MESURES.md](MESURES.md), tous
relançables.

## Ce que contient ce répertoire

| Fichier | Rôle | Vivant ? |
|---|---|---|
| [PLAN.md](PLAN.md) | **Le plan de reprise**, E0 à E8. C'est la révision épinglée par la décision qui l'adopte. | oui — se lit avant tout lot |
| [PORTAGE.md](PORTAGE.md) | Ce qui a été écrit au registre le 2026-08-05, et pourquoi. | non — porté, gardé comme référence |
| [MESURES.md](MESURES.md) | La sortie datée de `mesures.sh`, avec le commit mesuré. | oui — à recalculer quand le code bouge |
| [mesures.sh](mesures.sh) | Les mesures du corpus, du moteur, de l'échelle, de l'outillage et des bancs. | oui |
| [MESURE-D33.md](MESURE-D33.md) | Le mécanisme énoncé par D33 n'existe pas : un signal, aucune possession, aucune pesée. | oui — cité par le registre |
| [MESURE-PROJETS.md](MESURE-PROJETS.md) | petclinic et banking : le classpath change les verdicts, les enveloppes sont invisibles à la composition, R5b est la seule voix sur un projet qui génère. | oui — cité par le registre |
| [signaux/](signaux/) | Le harnais qui affiche les signaux retenus sur un type. | oui |
| [echange/](echange/) | Les vingt tours de l'échange qui a produit le plan. | non — trace |

## Par où entrer

- **Vous ouvrez un lot** → [PLAN.md](PLAN.md), puis
  [CHANTIER.md](../20260731-refactoring-audit/CHANTIER.md) pour l'état et les
  règles de conduite.
- **Vous voulez savoir ce que le registre dit et d'où ça vient** →
  [PORTAGE.md](PORTAGE.md), puis le registre lui-même.
- **Vous doutez d'un chiffre** → [mesures.sh](mesures.sh), et relancez-le.
- **Vous voulez savoir pourquoi le moteur dit ce qu'il dit d'un type** →
  [signaux/](signaux/), avec son classpath.
- **Vous cherchez pourquoi une décision a été prise ainsi** →
  [echange/](echange/).

## Les règles qui gouvernent le travail ici

Elles sont dans [PLAN.md](PLAN.md) et s'ajoutent aux treize de `CHANTIER.md` ;
les trois qui reviennent le plus souvent :

1. **Une trouvaille faite dans un lot est enregistrée, jamais tranchée ni
   implémentée dans ce lot.**
2. **Toute mesure qui étaye une décision est une commande relançable**, avec ses
   entrées — révision, racines, **classpath**, configuration, corpus.
3. **Caractériser n'est pas garantir** : mesurer une propriété et exiger qu'elle
   soit vraie sont deux gestes séparés, dans deux sessions séparées.

## État au 2026-08-05

**Le plan est adopté** et **le portage au registre est fait** : D39 pose le
contrat de gouvernance, D40 adopte le plan, D33 est requalifiée en append-only et
reste `PENDING`, M7 est `SUSPENDU`, la règle 13 est réécrite, et les constats,
hypothèses et errata sont dans leurs trois rubriques.

**Prochaine action du chantier : `E1a`** — caractériser ce que le marqueur de
génération change, avant qu'E1 décide le traitement d'un verdict fondé sur
l'absence seule.

**Code** : cinq commits poussés, CI et CodeQL vertes quatre fois sur quatre ;
1 401 tests, 12/12 en intégration, cliquet 143/143 + 6/6 + 5/5.

## Ce que ce répertoire ne fait pas

- Il ne remplace pas [le dossier d'audit](../20260731-refactoring-audit/) :
  l'audit, le registre des décisions et le journal y restent.
- Il ne contient aucun code de production, et le harnais `signaux/` est hors du
  réacteur.
