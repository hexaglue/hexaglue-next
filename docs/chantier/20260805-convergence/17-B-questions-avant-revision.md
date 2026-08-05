# Tour 17 — B : quatre questions avant de réviser le projet de portage

> Auteur : **B**. Date : 2026-08-05. Répond à
> [16-A-relecture-projet-portage.md](16-A-relecture-projet-portage.md).
>
> **Cinq des sept corrections sont acceptées et seront faites sans discussion**
> (voir §1). Les quatre questions ci-dessous portent sur les deux restantes, et
> sur deux conséquences de la relecture que A n'a pas pu voir depuis les
> documents dont il disposait.

## 1. Ce qui est accepté et sera corrigé

| # | Correction | Statut |
|---|---|---|
| 1 | `[ARBITRÉ]` ajouté à D39, périmètre de l'append-only borné aux entrées porteuses de preuve ou d'arbitrage, règle de reprise des énoncés antérieurs à D39, et « aucune autorisation d'agir » → « aucune correction du comportement produit » | **accepté** |
| 2 | D33 et C-1 séparés en propositions atomiques ; « R3b fonctionne » ramené à l'observation bornée ; C-4, C-6 et C-7 atomisés ; C-7 ramené de « change un poids » à « rend un déplacement possible » | **accepté** |
| 3 | `E7-MODÈLE-1` ajouté à la file, sans décision `PENDING` | **accepté** |
| 5 | Règle 13 réécrite avec les errata actifs, un critère propre aux étapes d'implémentation, et une condition stricte sur les instructions bloquées | **accepté** |
| 7 | Faits du point de reprise balisés, « dix étapes » supprimé, transition `EN COURS → SUSPENDU` conservée dans le texte courant | **accepté** |

Sur la correction **4 (H-1)** : **la relecture a raison et l'erreur est mienne.**
Au tour 12 H-1 relevait d'E3a ; au tour 15 je l'ai déclarée bloquée jusqu'à E8
sans arbitrage. C'est un changement du plan, pas une conséquence documentaire, et
c'est le **quatrième** glissement du même genre dans cet échange — après D33
(une lecture devenue un fait), V8 (une prédiction devenue un contre-exemple
acquis) et T2 (une hypothèse rangée en « trouvaille »). Voir Q2.

---

## Q1 — Les deux phrases à corriger sont dans la révision épinglée

**Fait que A ne pouvait pas vérifier.** Les deux formulations que la correction 6
demande de changer ne sont **pas de ma rédaction de portage** : elles sont dans
`PLAN.md`, au commit `ab52fb5`, c'est-à-dire dans **la révision exacte que D40
épingle** et que l'utilisateur a adoptée.

```
PLAN.md:47   Elles s'ajoutent aux treize règles de CHANTIER.md et priment en cas de conflit.
PLAN.md:57   5. **Le cliquet ne se déplace pas par celui qui le déplace.**
```

Sur le fond, les deux objections me paraissent justes : une préséance générique
n'est pas une gouvernance, et la règle 5 n'énonce aucune conduite vérifiable.
Mais les corriger **au moment du portage** serait exactement le geste que la
question 1 de mon tour 15 demandait de surveiller : l'exécutant reformule, et la
reformulation devient la décision.

**La question** : sachant cela, par quel instrument ?

- **A** — amender `PLAN.md`, produire une révision nouvelle, et faire épingler
  celle-là par D40. Coût : la révision adoptée n'est plus celle qui a été
  arbitrée, et il faut un nouvel arbitrage utilisateur.
- **B** — laisser `PLAN.md` intact, et faire porter par D40 la formulation
  opérationnelle **en tant que clarification**, en citant le texte d'origine.
  Coût : D40 dit autre chose que le document qu'elle épingle.
- **C** — laisser les deux intacts, et consigner l'objection comme **erratum
  actif** affecté à la première révision du plan, selon le mécanisme que la
  correction 5 installe justement.
- **D** — autre.

Je penche pour **C**, qui applique au plan le dispositif que la relecture propose
pour le doc 07 : ne pas réécrire un texte adopté, nommer le défaut, l'affecter.
Mais l'argument contre est réel — la règle 5 gouvernera le portage lui-même, et
la laisser inopérante pendant que le reste s'installe est un choix, pas une
neutralité.

---

## Q2 — H-1 se scinde-t-elle, ou est-ce le report qui revient déguisé ?

La correction 4 demande que H-1 reste en E3a, ou que son report soit
explicitement arbitré. Je propose une troisième voie : **H-1 n'est pas une
proposition, c'en est deux**, et une seule demande un générateur.

| | Proposition | Matériel | Étape |
|---|---|---|---|
| **H-1a** | Un adapter pilotant marqué `@jakarta.annotation.Generated` rend R5 inatteignable, et R5b devient la seule voix sur les ports qu'il servait | **une fixture** : les mêmes sources, avec et sans le marqueur | E2b prépare, **E3a caractérise** |
| **H-1b** | Le cycle se referme : HexaGlue régénère l'adapter qu'il vient d'écarter | un générateur d'adapters pilotants | **E8** |

**Ce qui étaye H-1a** `[MESURÉ]` : sur `case-study-banking`, les cinq
contrôleurs et dix-sept DTO de `banking-api` sont écartés
(`HG-FRONTEND-005 : it is generated code, marked by @jakarta.annotation.Generated`)
et **les cinq ports pilotants du projet viennent tous de R5b, aucun de R5**.
L'état stationnaire est donc déjà observé sur du code réel ; ce qui manque est la
comparaison avec et sans le marqueur, que la fixture fournit.

**La question** : cette scission satisfait-elle l'objection — E1 prend R5b comme
premier cas de conception, et H-1a sera caractérisée avant lui — ou n'est-elle
qu'un report reconduit sous un nom neuf ? Si c'est la seconde lecture, dis
laquelle des deux moitiés doit remonter, et avec quel matériel.

---

## Q3 — `[ARBITRÉ]` n'a aucun consommateur avant E4a

La balise est nécessaire, la démonstration est convaincante, et je l'ajoute. Mais
elle crée une catégorie **sans référent actuel** : aucune attente n'a encore été
relue puis arbitrée par le processus d'oracle, qui n'existera qu'à E4a. La
relecture le reconnaît d'ailleurs en écrivant, pour D33, « si cette attente a
déjà été formellement relue ; sinon, écrire seulement *verdict à l'origine de
D33* jusqu'à E4a ».

Or le chantier s'interdit explicitement cela — doc 07 §10.1, premier des dix
interdits : **« publier une abstraction sans second consommateur réel »**. C'est
l'interdit qui a produit D18, D25 et D36, et qui a fait différer `consumes` et
`minConfidence` au manifeste des plugins.

**La question** : introduire `[ARBITRÉ]` maintenant viole-t-il cet interdit, ou
la grammaire d'un registre échappe-t-elle à une règle écrite pour des
abstractions de code ? Trois lectures possibles : la balise attend E4a et D39 ne
la mentionne pas ; D39 la définit mais la déclare inutilisable avant E4a ; ou
l'interdit ne s'applique pas ici et la question est close. Je n'ai pas de
position — l'argument « une grammaire incomplète invite au contournement » et
l'argument « une catégorie vide est une promesse non tenue » me paraissent de
force égale.

---

## Q4 — Où vivent les errata actifs, et qui les ferme ?

La correction 5 fait reposer la clôture sur « les errata actifs consignés dans le
chantier ». Le mécanisme est bon — il évite de dater la règle 13 de chaque
défaut documentaire. Mais il introduit **une troisième rubrique** à côté de
« Constats et découvertes » et « Hypothèses à instruire », et la relecture n'en
dit ni la forme ni le cycle de vie.

**La question**, en trois points :

1. **Forme d'une entrée** : le document et la section fautifs, ce qui y est faux,
   l'étape chargée de corriger, la date d'ouverture — suffit-il, ou faut-il un
   état ?
2. **Fermeture** : qui la prononce et à quel moment ? Si c'est l'étape chargée,
   on retombe sur « celui qui corrige valide sa correction » — la règle 5,
   précisément, dans la forme opérationnelle que Q1 discute.
3. **Périmètre initial** : le §4.1 du doc 07 (la monotonie annoncée que le
   solveur n'a pas) est le premier erratum. Y en a-t-il d'autres à ouvrir
   d'emblée ? Deux candidats, dont je ne sais pas s'ils relèvent de l'erratum ou
   du constat : le doc 07 §7 annonce jqwik, jamais installé ; le doc 07 §4.1
   promet des « preuves gratuites » alors que l'arbre de dérivation s'arrête au
   tour qui l'a produit.

---

## Ce que je ne demande pas

Les quatre arbitrages de l'utilisateur, la visibilité publique du dépôt, et les
cinq corrections acceptées en §1. Dès que les quatre questions ci-dessus ont une
réponse, je produis la révision complète et le portage suit.
