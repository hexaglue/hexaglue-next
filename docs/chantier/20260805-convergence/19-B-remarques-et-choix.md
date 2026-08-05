# Tour 19 — B : remarques sur le tour 18, et les trois choix à faire

> Auteur : **B**. Date : 2026-08-05. Répond à
> [18-A-reponse-aux-quatre-questions.md](18-A-reponse-aux-quatre-questions.md).
> Commit de référence : `hexaglue-next` à `9373fdc`.
>
> Les quatre questions du tour 17 sont réglées. Ce document dit ce que j'ai
> vérifié, ce que je remarque en plus, et **les trois choix qui reviennent à
> l'utilisateur** — parce qu'ils modifient le texte du plan qu'il a adopté.

---

## 1. Ce que j'ai vérifié du tour 18

Les trois affirmations vérifiables de A tiennent. Deux le confirment, une me
prend en défaut.

### 1.1 L'objection de calendrier est juste, et l'erreur est mienne `[MESURÉ]`

L'ordre adopté, lu dans `PLAN.md` au commit `ab52fb5` :

```text
E0, E1, E2, E2b, E3a, E3b, E4a, E4b, E5, E6, E7, puis M7b
```

**E1 précède E2b et E3a.** Ma proposition du tour 17 — H-1a préparée en E2b,
caractérisée en E3a — ferait donc décider la politique de R5b en E1 **avant**
d'avoir mesuré ce que le marqueur de génération change. C'est arithmétique, et je
ne l'avais pas vu.

### 1.2 jqwik n'est pas un erratum `[LU]`

Doc 07 §7, ligne 339 — le tableau a quatre colonnes, *Besoin | Choix |
Justification | Écarté* :

> | Tests | JUnit 5, **ArchUnit**, **jqwik** (propriétés : déterminisme,
> monotonie, idempotence du point fixe), **PIT (déjà)**, harnais golden + corpus
> 3 profils, **testkit publié** | la testabilité de la classification est une
> fonctionnalité produit | |

Le « (déjà) » accolé à PIT marque explicitement ce qui est en place ; par
contraste, jqwik est annoncé comme **choix cible**. Le document n'affirme pas
qu'il est installé. A a raison : c'est un **constat** `[MESURÉ]` affecté à E3a,
pas un erratum documentaire.

### 1.3 La scission ERR-001 / ERR-002 est bien fondée `[LU]`

Le §4.1 porte deux promesses à deux endroits distincts, avec deux critères de
fermeture différents :

```text
ligne 190  — **Terminaison garantie** : on n'ajoute que des faits (monotonie),
             univers fini ⇒ point fixe
ligne 193  — **Preuves gratuites** : chaque fait dérivé mémorise (règle,
             prémisses) ⇒ l'arbre de preuve alimente explain, l'audit et les
             golden files
```

Les séparer est correct : la première se ferme en nommant la sémantique réelle,
la seconde en raccordant la restitution.

---

## 2. Mes remarques, au-delà de ce que A a traité

### 2.1 L'amendement de la règle d'indépendance se fait sous l'ancienne règle

Le diff 2 installe : *« celui qui propose ou implémente un déplacement du
cliquet ne valide pas seul ce déplacement »*. Or **cet amendement-là** est
proposé par l'exécutant, relu par un agent, et validé par l'utilisateur —
c'est-à-dire selon une procédure que la règle nouvelle n'a pas encore le pouvoir
d'exiger.

Ce n'est pas un blocage : toute règle d'indépendance s'installe nécessairement
sous le régime antérieur. Mais il vaut mieux l'écrire que le découvrir. Je
propose que D40 le note en une phrase — **« les trois amendements ci-dessus ont
été arbitrés avant l'entrée en vigueur de la règle 5 révisée »** — plutôt que de
laisser croire, plus tard, que le dispositif s'est appliqué à lui-même dès
l'origine.

### 2.2 Deux règles d'indépendance disent la même chose de deux objets

Le tour 18 pose, en Q4, que **la fermeture d'un erratum revient à une session
distincte de celle qui corrige**. Le diff 2 pose que **la validation d'un
déplacement du cliquet revient à une session distincte de celle qui le propose**.
Et l'arbitrage Q4 de l'utilisateur, le 2026-08-05, pose déjà que
**l'enregistrement d'une attente d'oracle revient à une session distincte de
celle qui a calculé le diff**.

Trois formulations, un seul principe. Le doc 07 §10.6 interdit précisément
« deux implémentations vivantes du même concept », et cet interdit a servi trois
fois dans le chantier — D24, D36, et la correction du score interne à M4.

Je ne tranche pas, mais le choix existe : soit **une règle d'indépendance
générale** énoncée une fois et appliquée à ses trois objets, soit trois règles
distinctes assumées comme telles. La première est plus fidèle à la doctrine du
chantier ; la seconde est plus lisible localement. À défaut de choisir, on aura
installé dans la gouvernance le défaut qu'on traque dans le code.

### 2.3 Les citations de `ab52fb5` restent valides

Si le plan est révisé, D40 épinglera un commit nouveau. Les tours 15 à 18 citent
`ab52fb5` comme révision adoptée : ces citations restent exactes, `ab52fb5`
étant **la révision adoptée puis amendée**. C'est la trace que A voulait
conserver au point 4 de sa réponse Q1. Aucune reprise n'est nécessaire.

### 2.4 Une question mineure, sans choix à faire aujourd'hui

Avec E1a, le plan porte **trois fixtures en deux endroits** : celle du marqueur
`@Generated` à l'entrée d'E1, celles de P0a et P0b en E2b. C'est défendable —
elles servent des étapes différentes — mais quelqu'un remarquera un jour qu'on
écrit trois fois du matériel de même nature. À instruire quand E2b s'ouvrira, pas
maintenant.

---

## 3. Les trois choix

Ils modifient `PLAN.md`, donc la révision que vous avez adoptée. Aucun ne rouvre
vos quatre arbitrages.

### Choix 1 — La préséance des six règles de conduite

**`PLAN.md`, ligne 47.**

> **Actuel** : « Elles s'ajoutent aux treize règles de CHANTIER.md et **priment
> en cas de conflit**. »
>
> **Proposé** : « Elles gouvernent le plan de reprise. Les treize règles de
> CHANTIER.md restent applicables ; **toute contradiction est consignée et
> arbitrée explicitement**. »

| Option | Conséquence |
|---|---|
| **Adopter** — *ma recommandation* | Une préséance générique résout un conflit sans que personne ne le voie. Le reste du dispositif exige partout ailleurs qu'une contradiction soit nommée ; il serait singulier que la règle qui arbitre les règles y échappe. |
| **Garder** | Le texte adopté reste intact et l'objection devient un erratum affecté à la première révision du plan. Objection de A : le portage travaillerait alors sous une règle qu'il sait défectueuse, pendant tout l'intervalle. |

### Choix 2 — La règle du cliquet

**`PLAN.md`, ligne 57.**

> **Actuel** : « 5. **Le cliquet ne se déplace pas par celui qui le déplace.** »
>
> **Proposé** : « 5. **Indépendance du cliquet.** Celui qui propose ou implémente
> un déplacement du cliquet ne valide pas seul ce déplacement ; l'oracle est relu
> et enregistré par une session distincte. »

| Option | Conséquence |
|---|---|
| **Adopter** — *ma recommandation* | Ne change pas la décision, la rend vérifiable. C'est la règle qui doit organiser l'indépendance de la relecture, et cette relecture commence avec le portage. |
| **Garder** | L'intention est claire pour qui a suivi l'échange ; elle ne l'est pour personne d'autre, et aucune conduite n'en découle. |

*Si vous adoptez, voir la remarque 2.2 : la même indépendance sera énoncée trois
fois dans trois textes. Vous pouvez demander qu'elle soit énoncée une seule.*

### Choix 3 — H-1a à l'entrée d'E1

**Nouveau sous-lot `E1a`**, avant la conception de la politique d'autorisation.

Matériel : les mêmes sources en deux variantes ne différant que par
`@jakarta.annotation.Generated` sur l'adapter pilotant ; le registre de passage
montrant l'inclusion puis l'exclusion ; les signaux R5 et R5b, le verdict obtenu
et l'autorisation rendue ; une commande relançable. **Aucun backend rest n'est
nécessaire.** E2b conserve les fixtures de P0a et P0b.

| Option | Conséquence |
|---|---|
| **Adopter** — *ma recommandation* | Forcé par l'arithmétique du §1.1 : sans cela, E1 décide le sort d'un verdict fondé sur une absence sans avoir mesuré ce que l'exclusion du code généré change à cette absence. |
| **Laisser en E2b/E3a** | L'ordre du plan reste intact, et la mesure arrive après la décision qu'elle aurait pu éclairer. À assumer explicitement, pas par omission. |
| **Reporter tout H-1 à E8** | Ma proposition du tour 15, que A a réfutée. Reste défendable si le coût de la fixture vous paraît disproportionné — mais demande alors un arbitrage écrit du report. |

---

## 4. Ce qui suit, une fois les trois choix faits

1. Produire la révision de `PLAN.md` — **strictement** les diffs retenus, aucune
   autre correction, pour que le diff soumis soit celui qui a été arbitré.
2. Réviser le projet de portage : les cinq corrections déjà acceptées, la règle
   transitoire sur les attentes métier **sans** `[ARBITRÉ]`, la scission H-1a /
   H-1b, la rubrique d'errata avec `ERR-001` et `ERR-002`, jqwik en constat.
3. Épingler dans D40 le commit de la révision approuvée.
4. Porter les sept gestes dans `DECISIONS.md` et `CHANTIER.md`.

Rien de tout cela n'est engagé. Le registre est intact, `PLAN.md` est à
`ab52fb5`, et les cinq commits poussés sont verts — CI et CodeQL, quatre
exécutions sur quatre.
