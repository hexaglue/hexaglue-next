# Mesures — état du réacteur v7

- **Date** : 2026-08-05
- **Commit mesuré** : `hexaglue-next` à `d5386a2`, arbre propre
- **Relance** : `./mesures.sh [corpus|moteur|echelle|outillage|bancs|all]`

Ce document est la **sortie** de [`mesures.sh`](mesures.sh), pas une
reformulation. Tout chiffre cité ailleurs dans ce répertoire doit se retrouver
ici, et se relancer.

---

## Corpus : scénarios par profil

```
profile1   143 scenarios
profile2   6 scenarios
profile3   5 scenarios
```

## Corpus profil 1 : nombre de types par scénario

```
 77 scenarios a 1 type(s)
 43 scenarios a 2 type(s)
  7 scenarios a 3 type(s)
  6 scenarios a 4 type(s)
  3 scenarios a 5 type(s)
  5 scenarios a 6 type(s)
  1 scenarios a 7 type(s)
  1 scenarios a 9 type(s)
```

**Ce que ça dit** : 120 scénarios sur 143 posent au plus deux types. La thèse du
moteur — « le rôle d'un type est une position dans un graphe » (D13) — est
exercée par 23 scénarios.

## Corpus profil 1 : ce que les attentes affirment

```
scenarios                      : 143
entrees classees               : 134
entrees UNCLASSIFIED           : 135
scenarios 100% UNCLASSIFIED    : 80
  dont un type unique          : 48
```

**Ce que ça dit** : 80 scénarios sur 143 n'attestent **que du silence**. Le
cliquet « 143/143 vert » certifie majoritairement que le moteur ne conclut
rien ; il est presque insensible au mode d'échec qui compte, conclure faux sur
un vrai graphe. C'est cohérent avec le fait que le premier banc réel ait trouvé
quatre défauts qu'aucune des deux suites ne voyait.

## Corpus profil 1 : provenance des scénarios

```
  31  DomainCriteriaTest          9  ClassificationConfigIntegrationTest
  16  DomainClassifierTest        8  ClassificationContractTest
  15  PortClassifierTest          7  PortCriteriaTest
  12  ConflictDetectionTest       6  FlexibleApplicationServiceCriteriaTest
   9  ClassificationIntegrationTest   3  ProgressiveClassifierIntegrationTest
                                  3  GoldenFileTest
   6  PortBoundary                3  ClassificationGoldenFilesTest
   5  DomainLifecycle
   5  ApplicationLayer
   5  AdapterRing

 122 transplantes de la carriere (nom en *Test)
  21 ecrits pour le nouveau moteur
```

**Ce que ça dit** : 85 % des scénarios du profil dominant portent encore le nom
d'une méthode de test de l'ancien réacteur. Beaucoup d'attentes ont été
ré-arbitrées (d'où les 80 silences ci-dessus), mais **la forme des questions
n'a pas été requalifiée**.

## Corpus : valeurs de confiance attendues

```
    122 HIGH
     50 EXPLICIT
```

**Ce que ça dit** : sur les 154 goldens des trois profils, **aucune attente ne
porte MEDIUM ni LOW**. Le seuil de génération par défaut est `HIGH`
(`GenerationConfig.java:45`) et se compare par `isAtLeast`
(`Contribution.java:106`) : **la porte de génération ne refuse rien dans tout le
corpus d'acceptation**. Elle n'y est jamais exercée comme un refus.

---

## Moteur : règles

```
fichiers de regles : 28
```

## Moteur : règles raisonnant sur une absence

```
   1 occurrence(s)  AdapterCollaborator.java
   2 occurrence(s)  ConsumedContract.java
   1 occurrence(s)  DomainCollaboration.java
   2 occurrence(s)  ExposedContract.java
   1 occurrence(s)  OfferedContract.java
   1 occurrence(s)  PortSignatures.java
   2 occurrence(s)  Shapes.java

7 fichiers sur 28 concluent au moins une fois d une absence
```

**Ce que ça dit** : la négation par absence (« personne ne détient », « rien
n'implémente ») est **localisée à 7 sites**. Toute remédiation qui la borne est
un chantier fini, pas une refonte.

## Moteur : forme de la boucle

```
Saturation.java:40    private static final int MAX_ROUNDS = 32;
Saturation.java:55    FactBase facts = new FactBase();
Classifier.java:41    private static final int MAX_ROUNDS = 8;
Classifier.java:68    for (int round = 1; round <= MAX_ROUNDS; round++) {
```

**Ce que ça dit** : deux boucles imbriquées de sémantiques différentes. La
boucle **interne** (`Saturation`) est monotone : la base ne fait que grossir. La
boucle **externe** (`Classifier`) repart d'une base vide à chaque tour, avec les
verdicts du tour précédent en entrée. C'est une itération de Kleene sur un
opérateur non monotone. Le doc 07 §4.1 n'annonce que la première (« on n'ajoute
que des faits (monotonie) […] terminaison garantie »).

À noter, contre une lecture trop sévère : **au point fixe, les verdicts cités
en prémisse sont les verdicts finaux** (`Classifier.java:71`, sortie quand
`next.equals(verdicts)`). Le résultat est donc auto-cohérent, et l'arbre de
preuve est sain — il est **non recollé**, pas faux. `Explanation` l'énonce
lui-même (`Explanation.java:46-50`) : une raison nomme les types sur lesquels
elle s'est appuyée (`involving`), à charge pour le lecteur de les ré-interroger.

Ce qui manque n'est pas la monotonie, c'est **l'énoncé et le test des garanties
réellement offertes** : pas de plus petit point fixe, pas d'unicité, résultat
dépendant du départ à `Verdicts.none()`, terminaison assurée par le seul
plafond.

---

## Échelle : palier d'évidence vers confiance

```
  DECLARED_INTENT("S1", Confidence.EXPLICIT)
  FRAMEWORK_KNOWLEDGE("S2", Confidence.HIGH)
  GRAPH_RELATION("S3", Confidence.HIGH)
  LOCAL_STRUCTURE("S4", Confidence.HIGH)
  TOPOLOGY("S5", Confidence.MEDIUM)
  NAMING("S6", Confidence.MEDIUM);
```

## Échelle : seuil de génération et ce qu'il compare

```
GenerationConfig.java:45      return new GenerationConfig(Confidence.HIGH);
Contribution.java:106         return type.classification().confidence().isAtLeast(minConfidence);
```

**Ce que ça dit** : six paliers ordonnés s'écrasent sur **trois valeurs**, dont
deux seulement apparaissent dans le corpus. `Confidence.HIGH` ne signifie pas
« verdict solide », il signifie « ni topologie ni nommage ». Un verdict lu d'une
présence (R5, S3) et un verdict lu d'une absence (R5b, S4) sortent **tous deux à
HIGH** : l'ordre des paliers ne survit pas à la traduction, et le seuil qui lit
la confiance ne peut pas les départager.

C'est le mécanisme exact derrière D38. Personne n'a choisi `HIGH` pour franchir
le seuil : `OfferedContract.java:92-93` prend `maxConfidence()` de son palier.
L'échelle est simplement incapable d'exprimer la distinction que le palier
encode.

---

## Outillage : jqwik et propriétés du point fixe

```
absent de tous les pom.xml du reacteur
aucun test nommant le point fixe
```

**Ce que ça dit** : le doc 07 §7 liste jqwik nommément, pour « propriétés :
déterminisme, monotonie, idempotence du point fixe » — exactement les propriétés
en cause ci-dessus. L'outil n'a jamais été installé, et aucune revue de clôture
de jalon ne l'a relevé.

---

## Bancs : projets observés contre projets disponibles

```
ecommerce-hexagonal      -> case-study-ecommerce/hexagonal/src/main/java

case-study-banking
case-study-ecommerce
case-study-lombok
spring-petclinic
```

**Ce que ça dit** : un seul banc, sur l'étude de cas écrite par le projet
lui-même. Le README de `_probes` annonce mesurer « ce que le réacteur fait d'un
domaine que personne n'a écrit pour lui » : `spring-petclinic` est le seul des
quatre à répondre à cette description, et il n'est pas branché. D32, D38 et
l'amendement de D35 viennent tous du banc unique.
