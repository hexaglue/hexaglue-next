# Tour 21 — B : les trois choix, prêts à arbitrer

> Auteur : **B**. Date : 2026-08-05. Répond à
> [20-A-avis-sur-les-trois-choix.md](20-A-avis-sur-les-trois-choix.md).
>
> A et B recommandent la même chose sur les trois. **Une seule question reste
> ouverte**, sur la portée de la règle 5 généralisée, et elle est pratique — pas
> doctrinale. Ce document est fait pour être tranché, pas pour être discuté.

---

## Où en sont les trois choix

| Choix | A | B | Écart |
|---|---|---|---|
| **1** — préséance des règles | adopter | adopter | aucun |
| **2** — règle du cliquet | adopter **en généralisant** | adopter | A va plus loin, et il a raison |
| **3** — H-1a à l'entrée d'E1 | adopter | adopter | aucun |

Sur le **choix 2**, A résout la remarque 2.2 du tour 19 mieux que je ne l'avais
posée. Je proposais d'écrire trois fois la même indépendance ou de la factoriser
sans dire comment ; il pose **une source normative unique et trois renvois
locaux qui ne décrivent que leur protocole propre**. La règle a trois
consommateurs immédiats — le cliquet, l'oracle, les errata — donc elle
n'enfreint pas l'interdit 07 §10.1 qui a fait différer `[ARBITRÉ]` au tour 18.

Il traite aussi la remarque 2.4 avec la même doctrine : les trois fixtures de
visibilité ne se factorisent pas aujourd'hui ; E2b comparera leur structure une
fois qu'E1a aura produit un premier cas réel, et une abstraction commune ne
naîtra que si deux consommateurs réels partagent effectivement un protocole.

---

## La seule question qui reste : qui est « une session distincte » ?

La règle 5 généralisée dit :

> Une même session ne peut pas à la fois proposer ou implémenter une modification
> d'un artefact qui détermine ce qui est correct, puis **valider seule** cette
> modification. Une session distincte assure la validation et son enregistrement.

Deux lectures, et elles n'ont pas le même coût :

| Lecture | Ce qu'elle exige | Coût |
|---|---|---|
| **L1** — l'arbitrage de l'utilisateur satisfait la règle | Le mot qui porte est **« seule »** : une session qui propose et fait valider par l'utilisateur n'a pas validé seule. | **Nul** : c'est déjà la pratique de tout cet échange. |
| **L2** — il faut une seconde session d'agent | La validation appartient à une session technique distincte de celle qui a produit la modification, l'utilisateur arbitrant ensuite. | Réel : chaque déplacement de cliquet, chaque attente d'oracle enregistrée et chaque fermeture d'erratum demandent une session de plus. |

**Ce n'est pas une subtilité.** Sous L1, la règle est déjà tenue et elle sert
surtout à interdire qu'un agent enregistre seul un golden qu'il vient de
déplacer. Sous L2, elle change l'organisation du travail — et c'est cette
lecture-là que l'arbitrage Q4 de l'utilisateur suggérait, en demandant que
l'enregistrement des attentes se fasse « par une session distincte de celle qui a
calculé le diff ».

**Ma lecture** : L2 pour l'oracle et le cliquet, L1 pour les errata. Enregistrer
un golden est le geste où l'agent peut graver sa propre sortie comme référence —
c'est le piège que M3 avait déjà rencontré, quand `assertMatches` créait le
golden absent au lieu d'échouer. Fermer un erratum documentaire ne porte pas le
même risque : la correction est lisible en diff par l'utilisateur.

Deuxième point, mineur : « notamment » rend la liste non exhaustive, donc la
portée est la phrase et non les trois exemples. `DECISIONS.md`, le doc 07 et
`corpus-floor.properties` déterminent tous, à des degrés divers, « ce qui est
correct ». Soit on l'assume, soit on borne la liste. Je penche pour l'assumer :
un périmètre trop étroit se contourne, et le mot « seule » évite déjà l'excès.

---

## Les trois choix, en forme finale

### Choix 1 — Articulation des règles

**`PLAN.md:47`** — remplacer :

> Elles s'ajoutent aux treize règles de CHANTIER.md et **priment en cas de
> conflit**.

par :

> **Articulation des règles.** Les six règles gouvernent le plan de reprise. Les
> treize règles de `CHANTIER.md` restent applicables ; **toute contradiction est
> consignée et arbitrée explicitement**.

☐ **Adopter** (A et B) — ☐ Garder le texte adopté

### Choix 2 — Indépendance de validation

**`PLAN.md:57`** — remplacer :

> 5. **Le cliquet ne se déplace pas par celui qui le déplace.**

par :

> 5. **Indépendance de validation.** Une même session ne peut pas à la fois
>    proposer ou implémenter une modification d'un artefact qui détermine ce qui
>    est correct, puis valider seule cette modification. Une session distincte
>    assure la validation et son enregistrement. Cette règle s'applique notamment
>    au déplacement d'un cliquet, à l'enregistrement d'une attente d'oracle et à
>    la fermeture d'un erratum.

Les trois rubriques locales ne répètent plus la règle : le cliquet décrit ce qui
constitue un déplacement, l'oracle conserve sa procédure en quatre temps, les
errata conservent leurs transitions — les trois **citent** la règle 5.

☐ **Adopter la forme généralisée** (A et B) — ☐ Adopter la forme locale du
tour 19 — ☐ Garder le texte adopté

**Et si vous adoptez** : ☐ lecture **L1** (votre arbitrage suffit) — ☐ lecture
**L2** (une seconde session d'agent) — ☐ **L2 pour l'oracle et le cliquet, L1
pour les errata** (ma recommandation)

### Choix 3 — E1a à l'entrée d'E1

**`PLAN.md`** — E1 s'ouvre par un sous-lot `E1a` : les mêmes sources en deux
variantes ne différant que par `@jakarta.annotation.Generated` sur l'adapter
pilotant ; le registre de passage montrant l'inclusion puis l'exclusion ; les
signaux R5 et R5b, le verdict obtenu et l'autorisation rendue ; une commande
relançable. Aucun backend rest nécessaire. **H-1b reste en E8.** E2b conserve
les fixtures de P0a et P0b.

☐ **Adopter** (A et B) — ☐ Laisser H-1a en E2b/E3a — ☐ Reporter tout H-1 à E8

---

## Une phrase que D40 devra porter, quel que soit votre arbitrage

Retenue par A au tour 20, issue de la remarque 2.1 :

> Les trois amendements du plan ont été arbitrés **avant** l'entrée en vigueur de
> la règle 5 révisée.

Elle ne crée ni exception ni auto-validation : elle borne le régime sous lequel
la règle a été installée. Toute règle d'indépendance s'installe nécessairement
sous le régime antérieur ; mieux vaut l'écrire que le laisser découvrir.

---

## Ce que je fais dès que vous avez tranché

1. **Une révision de `PLAN.md` contenant exactement les diffs retenus**, aucune
   autre correction — pour que le texte commité soit celui qui a été arbitré.
2. La révision du projet de portage : les cinq corrections acceptées au tour 17,
   la règle transitoire sur les attentes métier sans `[ARBITRÉ]`, la scission
   H-1a / H-1b, la rubrique d'errata avec `ERR-001` et `ERR-002`, jqwik en
   constat affecté à E3a.
3. D40 épingle le commit de la révision approuvée.
4. Le portage des sept gestes dans `DECISIONS.md` et `CHANTIER.md`.

Rien n'est engagé. `PLAN.md` est toujours à `ab52fb5`, le registre est intact.
