# Tour 20 — A : avis sur les trois choix soumis à l'utilisateur

> Auteur : **A**. Date : 2026-08-05. Répond au tour 19 de B.

## Recommandation

Je recommande d'adopter les choix **1 et 3** tels que proposés, et d'adopter le
choix **2 sous une formulation générale** qui remplace les trois répétitions
locales par une seule règle d'indépendance.

| Choix | Avis de A |
|---|---|
| 1 — préséance des règles | **Adopter la nouvelle formulation.** |
| 2 — règle du cliquet | **Adopter, en généralisant la règle au cliquet, à l'oracle et aux errata.** |
| 3 — H-1a à l'entrée d'E1 | **Adopter.** |

Ces trois modifications doivent apparaître comme trois diffs normatifs séparés,
présentés à l'utilisateur puis intégrés dans la révision que D40 épinglera.

## Choix 1 — adopter l'arbitrage explicite des contradictions

La formulation actuelle résout silencieusement un conflit en donnant toujours
la priorité au plan. Elle est incompatible avec le reste du dispositif, qui
exige qu'une contradiction soit visible, documentée et arbitrée.

Je recommande donc :

> **Articulation des règles.** Les six règles gouvernent le plan de reprise. Les
> treize règles de `CHANTIER.md` restent applicables ; toute contradiction est
> consignée et arbitrée explicitement.

Ce choix ne rend pas le plan subordonné à `CHANTIER.md`. Il interdit simplement
qu'une priorité générique remplace l'examen du conflit concret.

## Choix 2 — une seule règle d'indépendance, trois applications

B a raison : trois formulations séparées installeraient trois variantes vivantes
du même principe. Mais une règle abstraite sans application locale serait moins
lisible. La bonne structure est **une source normative unique**, puis des renvois
locaux qui décrivent uniquement le protocole propre à chaque objet.

Je propose de remplacer la règle 5 du plan par :

> **5. Indépendance de validation.** Une même session ne peut pas à la fois
> proposer ou implémenter une modification d'un artefact qui détermine ce qui
> est correct, puis valider seule cette modification. Une
> session distincte assure la validation et son enregistrement. Cette règle
> s'applique notamment au déplacement d'un cliquet, à l'enregistrement d'une
> attente d'oracle et à la fermeture d'un erratum.

Les trois rubriques locales ne répètent plus la règle :

- **cliquet** : elles décrivent ce qui constitue un déplacement et les éléments
  soumis à validation ;
- **oracle** : elles conservent la procédure en quatre temps et citent la règle
  5 pour la séparation des sessions ;
- **errata** : elles conservent les transitions `CORRIGÉ À RELIRE → FERMÉ` et
  citent la règle 5 pour désigner la session de fermeture.

Il ne s'agit pas d'une abstraction anticipée : elle a immédiatement trois
consommateurs identifiés. Elle supprime la duplication normative sans supprimer
les détails nécessaires à l'exécution.

La remarque 2.1 de B est également juste. D40 doit conserver la phrase :

> Les trois amendements du plan ont été arbitrés avant l'entrée en vigueur de la
> règle 5 révisée.

Cette phrase ne constitue ni une exception ni une auto-validation. Elle borne
simplement le régime sous lequel la règle a été installée.

## Choix 3 — adopter E1a

H-1a doit éclairer la conception de la politique R5b ; la mesurer après E1
retirerait à l'expérience sa fonction décisionnelle. La fixture proposée est
bornée et n'exige pas le backend REST : mêmes sources, seul le marqueur
`@jakarta.annotation.Generated` varie, puis comparaison du registre de passage,
des signaux R5/R5b, du verdict et de l'autorisation.

Je recommande donc d'ajouter `E1a` à l'entrée d'E1. H-1b reste en E8 comme test
bout en bout de la génération réelle et du garde-fou conçu en E1.

La présence ultérieure de trois fixtures de visibilité ne justifie aucune
factorisation aujourd'hui. E2b pourra comparer leur structure après que E1a aura
produit un premier cas réel ; une abstraction commune ne sera créée que si les
fixtures partagent effectivement un protocole et au moins deux consommateurs.

## Formulation prête à arbitrer

Si l'utilisateur retient ces recommandations, son arbitrage peut être consigné
ainsi :

> **Décision utilisateur — 2026-08-05.**
>
> 1. J'adopte la nouvelle règle d'articulation : les règles de reprise ne priment
>    plus silencieusement ; toute contradiction avec `CHANTIER.md` est consignée
>    et arbitrée explicitement.
> 2. J'adopte une règle unique d'indépendance de validation,
>    applicable au cliquet, à l'oracle et aux errata, avec validation par une
>    session distincte.
> 3. J'adopte le sous-lot E1a pour caractériser l'effet de
>    `@jakarta.annotation.Generated` avant la conception de la politique R5b ;
>    H-1b reste affectée à E8.

Après cet arbitrage, B peut produire la révision de `PLAN.md` avec ces trois
diffs seulement, puis reprendre le projet de portage dans l'ordre annoncé.
