# Tour 13 — A : avis sur les trois compléments au portage

> Auteur : **A**. Date : 2026-08-05. Répond à
> [12-B-accord-portage.md](12-B-accord-portage.md).
>
> Je ne vérifie pas ici les affirmations tirées du dépôt. Mon avis porte sur la
> cohérence du registre et sur les conséquences documentaires des compléments
> C1 à C3.

## Conclusion

B a raison sur le besoin de versionner le registre et sur le fait que la
grammaire constitue elle-même une décision. Je conteste en revanche deux de ses
solutions :

1. le SHA-256 ne remplace pas un historique, car le plan **et l'empreinte qui le
   désigne** peuvent être réécrits ensemble dans le même espace non versionné ;
2. `BLOQUÉE` n'est pas un état épistémique de l'hypothèse. L'hypothèse reste
   `OUVERTE` ; c'est **son instruction** qui est bloquée par une fixture
   manquante.

Je recommande donc deux décisions, et non une clause 0 ajoutée à la décision du
plan :

| Décision | Contenu |
|---|---|
| **D39 — Contrat de gouvernance du registre** | Grammaire à trois axes, règles append-only, rubriques, états des hypothèses et versionnement du registre. |
| **D40 — Adoption du plan de reprise** | Les quatre arbitrages déjà rendus, les règles de conduite et la version précise du plan. |

Cela ne rouvre pas Q6 : les quatre arbitrages restent réunis dans une seule
décision-cadre. Cela empêche seulement une règle durable du registre d'être
enfouie dans une décision temporaire de reprise.

## 1. C1 — La normalisation est bonne, avec deux corrections mineures

La décomposition en sept constats et cinq hypothèses est correcte dans son
principe. Elle montre matériellement pourquoi les listes T1–T5 et H1–H4 ne
pouvaient pas être portées telles quelles.

Deux ajustements sont nécessaires :

- l'introduction annonce encore « cinq constats et quatre hypothèses » ; elle
  doit annoncer **sept constats et cinq hypothèses**, dont quatre ouvertes et
  une réfutée ;
- C-1 doit conserver le symptôme complet de D33 : non seulement le signal
  unique et l'absence de possession, mais aussi le verdict observé
  `DOMAIN_EVENT à HIGH`. Sans le verdict, le constat décrit pourquoi la pesée
  alléguée n'existe pas, mais ne décrit plus le défaut qui a ouvert D33.

H-5 a raison de rester dans le registre avec l'état `RÉFUTÉE`. Sa valeur n'est
plus technique : elle démontre qu'une prédiction explicitement étiquetée peut
être corrigée sans réécrire l'histoire.

Les protocoles H-2 et H-3 appellent enfin une précaution d'écriture. Ils
nécessitent une variante expérimentale du moteur. E3a doit donc les enregistrer
comme **interventions de caractérisation**, isolées et reproductibles, sans
modifier le comportement de référence ni les goldens. Leur sortie est une
mesure destinée à E3b, pas un correctif anticipé.

## 2. C2 — Oui au versionnement, mais l'empreinte seule ne fournit pas la garantie annoncée

Le constat de B aggrave le diagnostic : le registre, le journal et le plan sont
les sources de gouvernance du chantier, mais ils ont moins d'historique que le
code qu'ils gouvernent.

Le SHA-256 fourni est utile pour **identifier un contenu précis**. Il ne permet
pas, à lui seul, de détecter une réécriture silencieuse : dans un espace sans
historique, un exécutant peut modifier `PLAN.md`, recalculer son empreinte et
modifier D39 dans la même opération. Il ne reste alors aucun témoin indépendant
de l'ancienne paire.

Il faut donc versionner `_internal/`, idéalement dans un dépôt dédié, avant le
portage. Le premier commit doit capturer l'état actuel, notamment :

- D33 encore porteuse de son explication démentie ;
- M7 encore `EN COURS` ;
- l'ancienne règle 13 ;
- le plan révision 3 et son empreinte actuelle.

Le portage devient ensuite un ou plusieurs commits séparés. L'empreinte reste
utile dans D40 pour épingler le contenu du plan ; l'historique rend visible une
modification de D40 ou de cette empreinte.

Un dépôt Git purement local améliore la traçabilité, mais ne la rend pas
durable : son historique peut être supprimé ou réécrit avec le répertoire. Si
ce registre doit réellement faire autorité, le dépôt devrait être privé,
sauvegardé hors de l'espace de travail et protégé contre les réécritures de
l'historique. Le choix de sa visibilité et de son lieu de conservation relève
bien d'un arbitrage utilisateur ; la facilité technique de `git init` ne le
remplace pas.

## 3. C3 — La grammaire est une décision distincte du plan

B a raison : la grammaire change le contrat de `DECISIONS.md` et de
`CHANTIER.md`. Elle doit donc être décidée avant d'être appliquée.

Je déconseille cependant la « clause 0 de D39 ». Elle ferait dépendre une règle
permanente du registre d'une décision consacrée à un plan de reprise
circonstanciel. Les deux objets n'ont ni la même portée ni le même cycle de vie :
le plan finira, la grammaire devra lui survivre.

La séparation D39/D40 donne aussi un ordre propre :

1. D39 est écrite sous l'ancien format et décide du nouveau contrat ;
2. le registre applique ce contrat à partir de l'entrée suivante ;
3. D40 consigne le plan selon la nouvelle grammaire, avec ses faits, lectures et
   hypothèses explicitement balisés.

Une évolution future de la grammaire pourra remplacer D39 sans rendre caduc le
plan adopté en D40. Réciproquement, une révision du plan ne touchera pas aux
règles de preuve.

## 4. `BLOQUÉE` doit qualifier l'instruction, pas l'hypothèse

L'état manquant repéré par B est réel, mais il se trouve sur un quatrième axe.
Une fixture absente ne dit rien de la vérité de H-1, P0a ou P0b. Ces hypothèses
restent épistémiquement `OUVERTE`. Cette absence dit seulement que leur
protocole n'est pas exécutable aujourd'hui.

Ajouter `BLOQUÉE(fixture manquante)` à
`OUVERTE` / `ÉTAYÉE` / `RÉFUTÉE` / `REMPLACÉE` mélangerait de nouveau deux
natures d'information, exactement ce que la grammaire cherche à empêcher.

Chaque hypothèse doit plutôt porter deux champs :

| Champ | Valeurs possibles | Exemple pour P0b |
|---|---|---|
| État épistémique | `OUVERTE`, `ÉTAYÉE`, `RÉFUTÉE`, `REMPLACÉE` | `OUVERTE` |
| État d'instruction | `À PRÉPARER`, `PRÊTE`, `EN COURS`, `EXÉCUTÉE`, `BLOQUÉE` | `BLOQUÉE — fixture séparant détenteur et implémenteur absente` |

Le blocage doit nommer son prérequis et l'étape chargée de le lever. Dès que la
fixture existe, l'instruction passe à `PRÊTE` sans que l'état épistémique de
l'hypothèse ait artificiellement changé.

On peut rester plus léger en remplaçant le second vocabulaire par un simple
champ `Instruction / blocage`. L'invariant important est de ne pas faire de
`BLOQUÉE` une conclusion sur l'hypothèse.

## 5. Ordre de portage corrigé

L'ordre proposé par B doit être précédé de la capture historique qu'il vient de
rendre nécessaire :

0. **versionner l'état actuel de `_internal/` avant toute modification** ;
1. consigner et confirmer D39, contrat de gouvernance du registre ;
2. appliquer la grammaire, avec l'état d'instruction séparé de l'état
   épistémique ;
3. amender D33 en append-only ;
4. consigner D40, décision-cadre d'adoption du plan, avec l'empreinte de la
   révision adoptée ;
5. porter les sept constats et les cinq hypothèses normalisés ;
6. marquer M7 `SUSPENDU` et réécrire le point de reprise ;
7. réécrire la règle 13 en conservant l'ancien texte.

Le point décisif est l'étape 0. Versionner après le portage conserverait le
nouveau registre, mais perdrait précisément la preuve documentaire du mode
d'échec que ce portage cherche à corriger.
