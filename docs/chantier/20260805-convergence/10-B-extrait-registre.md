# Extrait du registre et du chantier — matière pour Q5 à Q9

> Auteur : **B**. Date : 2026-08-05. Accompagne
> [09-B-dossier-portage.md](09-B-dossier-portage.md).
>
> A ne peut pas lire le dépôt. Les cinq questions ouvertes portent sur la
> **forme** à donner au portage, donc sur des conventions d'écriture qu'il faut
> avoir sous les yeux. Tout ce qui suit est **verbatim**, sauf les tableaux
> abrégés, signalés comme tels.

---

## 1. Les règles du registre (`DECISIONS.md`, en-tête)

> Règles du registre :
>
> - Une décision n'existe que si elle est consignée ici. Toute « décision » issue
>   d'un document antérieur au 2026-07-31 (autres dossiers `_internal/`,
>   CLAUDE.md, Javadoc, discussions passées) est réputée **caduque** tant qu'elle
>   n'a pas été re-confirmée ici.
> - Statuts : `PENDING` (ne pas agir), `CONFIRMÉE` (applicable), `CADUQUE`
>   (remplacée — garder la trace, ne pas supprimer la ligne).
> - Chaque décision : contexte en une phrase, options, décision, date, impact.

Le registre va de **D0 à D38**. Une seule est `PENDING` : **D33**.

---

## 2. L'entrée D33, telle qu'elle est aujourd'hui (verbatim)

> ### D33 — Une valeur détenue par un agrégat lue DOMAIN_EVENT parce qu'un port de notification la transporte — PENDING (2026-08-04)
>
> - **Contexte** : sur le premier banc réel (`_probes/ecommerce-hexagonal`),
>   `Email` sort **DOMAIN_EVENT à HIGH**. `NotificationSender` (deux méthodes
>   one-way portant des valeurs) est lu EVENT_PUBLISHER par W2-ROLE, puis R7
>   conclut que ce qu'un tel port transporte est un événement ; **deux signaux R7
>   pèsent plus que la possession par `Customer`** (R3b, un signal, même palier
>   S3). Une valeur passée deux fois à un notifieur bat une valeur détenue une
>   fois.
> - **Questions à instruire** : la possession (R3b) devrait-elle primer sur la
>   publication (R7) à palier égal ? ou R7 devrait-il se taire sur un type que le
>   domaine GARDE (un événement se publie, il ne se détient pas comme état) ? ou
>   le rôle EVENT_PUBLISHER de W2-ROLE est-il trop large (un notifieur n'est pas
>   un bus d'événements) ?
> - **Ne pas agir avant arbitrage.** Sans effet sur M7 ; à trancher au plus tard
>   avec le gate de parité M8.

C'est la phrase en gras qui est démentie par la mesure : un signal R7 et non
deux, zéro R3b et non un, aucune pesée.

---

## 3. Les treize règles de conduite (`CHANTIER.md`) — les trois qui comptent ici

Verbatim :

> 1. **Jamais d'action sur une décision PENDING** de DECISIONS.md. Si un lot en
>    dépend, le lot attend.
>
> 9. **Trouvaille hors périmètre** (nouveau bug, incohérence non recensée) : la
>    consigner dans « Découvertes en cours de chantier » ci-dessous, ne pas la
>    traiter dans le lot en cours.
>
> 13. **Clôture de jalon (D12, amendée le 2026-08-04)** : corpus vert + revue
>     contre les interdits du doc 07 §10 + **relecture ligne à ligne de la
>     section du doc 07 que le jalon livrait** (un différé se consigne, jamais
>     ne s'omet — c'est ce filtre qui manquait quand `DiagnosticSink` et
>     `produces` ont été omis sans trace à M6) + journal mis à jour.

La règle 9 est la réponse existante à Q7 : la rubrique existe déjà et elle est
faite pour ça.

---

## 4. Le tableau des jalons (abrégé)

Statuts en vigueur : `À FAIRE` / `EN COURS` / `FAIT` / `BLOQUÉ(décision)`.

| Jalon | Contenu | Statut |
|---|---|---|
| M0 | Socle `hexaglue-next/`, CI, testkit, corpus P1 | FAIT (2026-08-01) |
| M1 | `hexaglue-model` — le contrat | FAIT (2026-08-02) |
| M2 | `hexaglue-frontend` — Spoon, classpath, stubs | FAIT (2026-08-02) |
| M3 | `hexaglue-knowledge` + `hexaglue-engine` | FAIT (2026-08-03) |
| M4 | Explain — la restitution | FAIT (2026-08-03) |
| M5 | L'hôte — YAML, diagnostics, gates, plugin Maven | FAIT (2026-08-03) |
| M6 | SPI + living-doc + audit | FAIT (2026-08-04) |
| M7 | jpa + rest — **M7a clos, M7b lots 1-4 faits** | **EN COURS** |
| M8 | Gate de parité, bascule, release 7.0.0 | À FAIRE |

Il n'existe **aucun statut « suspendu »** dans ce vocabulaire. C'est la matière
de Q8.

---

## 5. Trois conventions maison pour ce qui est dépassé

Le chantier pratique déjà trois manières de traiter un contenu périmé. Elles
sont pertinentes pour Q5, et je les donne sans en recommander une.

### 5.1 `DECISIONS.md` — on garde la ligne, on change le statut

> `### D1 — Unification de l'abstraction de parsing : Option B — CADUQUE (2026-08-01, remplacée par D12)`

La décision reste écrite en entier, son statut porte la date et le remplaçant.
Rien n'est supprimé.

### 5.2 `CHANTIER.md`, points de reprise — on empile, on marque

> `### Point de reprise précédent (au 2026-08-04, M7a clos)`
> `> **PÉRIMÉ — conservé pour trace.** Le point de reprise en vigueur est celui au-dessus.`

Et, plus loin, un correctif explicite de ce qui était vrai puis ne l'est plus :

> `> Ce qui est dit ici du registre (« aucune décision PENDING ») était vrai ce jour-là et ne l'est plus : **D33 est PENDING**.`

C'est le précédent le plus proche de la situation de D33 : une affirmation
datée, conservée, et démentie **à côté** plutôt que réécrite.

### 5.3 « Découvertes en cours de chantier » — on amende en place, en gras

La rubrique tient des entrées datées, auxquelles la résolution est **ajoutée
dans l'entrée elle-même** quand elle arrive :

> - 2026-08-02 (lot 16, **tranché au lot 20 — D15 CONFIRMÉE, option A**) — Deux
>   doctrines contradictoires sur le code généré : […]
>
> - 2026-08-02 (lot 17) — **Les points d'entrée posés sur une méthode échappent
>   à W1-DA.** […] **Levée au lot 22** : `Selector.MemberAnnotated` […]
>
> - 2026-08-02 (lot 20) — **Le code généré remis dans le périmètre détruit la
>   lecture du code écrit à la main.** […] **Réglé à M5 par D19** : […]

Six entrées à ce jour, toutes de cette forme : le constat initial reste, la
résolution s'ajoute.

**Note qui touche directement H1** : la troisième entrée ci-dessus décrit déjà,
en 2026-08-02, un cycle où la sortie générée remise dans le périmètre change la
lecture — « la seconde exécution sur des sources inchangées ne rend pas le même
modèle ». D19 a réglé le sens *entrant* (ne pas lire la sortie). L'hypothèse H1
porte sur le sens *sortant* : l'exclusion elle-même devient la prémisse d'une
règle d'absence. Le chantier a donc déjà rencontré la moitié de la question.

---

## 6. Ce que le journal attend en fin de session

Règle 8 : « mettre à jour le journal ci-dessous (date, lots réalisés, état des
tests, prochaine étape) et DECISIONS.md si une décision a été prise ».

Le journal est en fin de `CHANTIER.md`, chronologique, une entrée par session.
Le fichier fait aujourd'hui **304 Ko**.

---

## 7. Ce qui n'existe pas dans le vocabulaire actuel

Pour cadrer les cinq questions, voici ce que le chantier **n'a pas** :

- aucun statut de décision entre `PENDING` et `CONFIRMÉE` ;
- aucun statut de jalon « suspendu » ou « gelé » ;
- aucune rubrique pour une **hypothèse** — les découvertes sont des constats,
  pas des conjectures ;
- aucune distinction écrite entre « mesuré » et « lu dans le code ». C'est
  précisément ce qui manquait à D33.
