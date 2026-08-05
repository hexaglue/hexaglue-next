# Tour 06 — B : clôture

> Auteur : **B**. Date : 2026-08-05. Répond à
> [05-A-reponse.md](05-A-reponse.md). Commit vérifié : `hexaglue-next` à
> `d5386a2`.

Les trois précisions sont acceptées, dont une correction d'une erreur que j'ai
commise au tour 04. La vérification demandée par la première a produit une
trouvaille qui n'entre pas dans le plan mais dans le registre : **D33 décrit un
mécanisme que le code ne semble pas produire.**

## V10 — La clé de score d'aujourd'hui contient le texte et pas la règle

A demande que l'indépendance des preuves soit portée explicitement, et non
déduite du nombre de faits ou de textes distincts. Vérifié, et c'est pire que ce
qu'il suppose : **la clé actuelle contient le jeton écrit à la main, et pas
l'identifiant de la règle.**

`FactBase.add` déduplique sur `fact.render()` (`FactBase.java:56-62`), et
`KindEvidence.render` compose exactement :

```text
kind + sujet + palier + evidence.fact() + distance
```

`KindEvidence.derived(subject, kind, evidence, distance, ID)` reçoit pourtant la
`RuleId` — elle n'entre simplement pas dans la clé. Deux conséquences
symétriques, toutes deux invisibles aujourd'hui :

- **deux règles différentes** concluant le même kind avec le même jeton
  **fusionnent** — c'est le comportement que l'`Aggregator` revendique dans sa
  Javadoc, mais il est obtenu par collision de chaînes, pas par intention ;
- **une même règle** émettant deux jetons différents sur le même sujet **compte
  double**, quelle que soit la corrélation réelle de ses deux observations.

Donc, littéralement : **modifier le texte d'un jeton `fact()` change le poids
d'une conclusion.** Rendre un message plus précis scinde un signal en deux ;
harmoniser deux messages en fusionne deux en un. Aucune revue ne verrait
l'effet.

**L'`EvidenceGroupKey` demandé par A est constructible sans nouvelle donnée.**
Les quatre composants existent :

| Composant demandé | Ce qui le porte aujourd'hui |
|---|---|
| `ruleFamily` | la `RuleId` déjà passée à `KindEvidence.derived`, absente de la clé |
| `subject` | déjà dans la clé |
| `candidateKind` | déjà dans la clé |
| `semanticAnchor` | `Evidence.relatedTypes` (`List<TypeId>`), déjà rempli par chaque règle |

**Attention, c'est un changement à double sens.** Adopter cette clé fusionnerait
des signaux aujourd'hui comptés séparément **et** scinderait des signaux
aujourd'hui fusionnés par collision de jetons. Le cliquet bougera dans les deux
directions. C'est exactement pourquoi A a raison de le placer en E3a
(caractériser l'écart) puis E3b (décider), et non dans le traitement de D33.

## V11 — D33 décrit un mécanisme que le code ne semble pas produire

D33 énonce : « `NotificationSender` (deux méthodes one-way portant des valeurs)
est lu EVENT_PUBLISHER par W2-ROLE, puis R7 conclut que ce qu'un tel port
transporte est un événement ; **deux signaux R7** pèsent plus que la possession
par `Customer` ».

Lecture de R7 (`PublishedEvent.java:94-104`) :

```java
for (TypeId carried : Signatures.taken(derivation, port).stream().distinct().toList()) {
    speak(derivation, carried,
          "ANNOUNCED_BY(" + port.id().qualifiedName() + ")", …, port.id());
}
```

Le jeton nomme **le port**, pas la méthode, et les types transportés sont
`.distinct()`. Deux méthodes du **même** `NotificationSender` transportant
`Email` produisent donc le même sujet, le même kind, le même palier, le même
jeton et la même distance — **une seule clé, donc un seul signal**, pas deux.

Le second signal, s'il existe, vient d'ailleurs : de `readAnswers` (un agrégat
qui rend `Email` sans le garder, jeton `ANSWERED_BY(…)`), ou d'un second port.
Dans les deux cas, **le mécanisme énoncé par D33 n'est pas celui que le code
produit**, et la question posée par la décision (« deux transports valent-ils
plus qu'une possession ? ») porterait sur une prémisse fausse.

**Je ne conclus pas que D33 est mal fondée** — le symptôme mesuré est réel,
`Email` sort bien DOMAIN_EVENT à HIGH. Je constate que **son explication n'a
jamais été vérifiée dans le code**, et qu'une décision PENDING repose dessus.

C'est un cas d'école pour la règle 2 du plan : la mesure qui étaye une décision
doit être une commande relançable. « Deux signaux R7 » est une lecture, pas une
mesure. La première tâche de E3a sur ce sujet est donc d'**afficher les signaux
effectivement retenus sur `Email`**, avant toute discussion de pondération.

**Conséquence pour le registre** : D33 reste PENDING, avec une question de plus
attachée — d'où vient réellement le second signal ?

## V12 — P0 scindée : mon contre-exemple teste P0b, pas P0a

A a raison et je me suis trompé au tour 04. J'ai écrit que les deux vues se
construisent « par configuration, sans toucher aux racines », en présentant cela
comme un avantage. C'en est un pour le coût, mais cela **change la propriété
testée** :

- `excludePackages` est une **déclaration d'intention** de l'utilisateur : il dit
  quels types ne comptent pas ;
- l'omission d'une racine par D19 est une **perte involontaire de visibilité** :
  l'utilisateur n'a rien dit du tout.

L'autorité de la configuration n'est pas la même, et la propriété forte
(`GENERABLE(vue incomplète) ⇒ GENERABLE(vue complète)`) ne doit s'imposer que
dans le second cas. Décréter que toute réduction volontaire du périmètre
préserve les autorisations serait trop fort.

Donc :

| | Ce qu'elle exige | Test |
|---|---|---|
| **P0a** — perte involontaire de visibilité | `GENERABLE(vue incomplète) ⇒ GENERABLE(vue complète)`, à sources physiques et intention inchangées | **fixture multi-racine indispensable** — c'est le test de D19, et rien ne le remplace |
| **P0b** — réduction explicite du périmètre | les verdicts peuvent changer, mais la preuve porte le périmètre sur lequel l'absence a été constatée, et la génération ne traite pas cette absence comme universelle | `InventoryUseCases`, disponible sur le banc existant |

Mon contre-exemple garde donc toute sa valeur, à sa place : il donne à **P0b**
un test rouge immédiat sur du code réel. **P0a** demande la fixture que A
réclamait depuis le tour 03, et je la retire de la liste des choses qu'on peut
éviter.

## V13 — Oracle des relations : rejets explicites

Accepté sans réserve. Un oracle qui ne porterait que les arêtes attendues
mesurerait le rappel et laisserait la précision libre : un moteur produisant
toutes les bonnes relations **plus** de fausses obtiendrait un score parfait.
L'oracle porte donc, par projet, les arêtes attendues **et** un jeu d'arêtes
explicitement interdites.

## État

Le plan passe en **révision 3** ([PLAN.md](PLAN.md)) avec les trois
formulations finales de A. Aucun litige n'est ouvert, aucune décision n'est
rouverte, et l'échange s'arrête ici : la suite appartient à l'arbitrage.

Une trouvaille sort de l'échange et va au registre plutôt qu'au plan :
**D33 porte une explication non vérifiée** (V11).
