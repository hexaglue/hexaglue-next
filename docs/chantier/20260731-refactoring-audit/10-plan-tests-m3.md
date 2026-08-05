# Plan de tests — reste de M3 sous le référentiel (doc 09)

Date : 2026-08-02.
Objet : les tests à mettre en place maintenant que D13/D14 sont tranchées et
que le référentiel des règles ([09-referentiel-regles.md](09-referentiel-regles.md))
fait foi. Ce plan structure le reste de M3 en lots exécutables (15-23), chacun
tests d'abord. L'implémentation démarre en session suivante par le lot 15.

État de départ mesuré (2026-08-02) : corpus profil 1 = 122 scénarios, dont
73 mono-type (26 avec ancre S1/S2, 47 nus) et 33 bi-types ; 20 relus,
18 passants (2 attendent R3/R4) ; `make ci` vert, 645 tests, PIT engine 87 %.

---

## 1. Les couches de test

| Couche | Où | Ce qu'elle prouve | Discipline |
|---|---|---|---|
| **T1 — unitaires de règle** | `hexaglue-engine` (CodeModel synthétique via builders, sans Spoon) | Chaque règle du doc 09 : cas nominal, contre-cas, anti-règle | TDD strict : la règle n'existe pas tant que son test est vert par accident |
| **T2 — propagation** | `hexaglue-engine` (`Classifier` multi-tours) | Une vague alimente la suivante (adapter au tour 1 → port au tour 2 → agrégat au tour 3) ; le remplacement de verdict (une lecture supplantée disparaît, ex. S4 VALUE_OBJECT → R1b AGGREGATE_ROOT) ; stabilité et déterminisme (testkit `Determinism`) | Un test de convergence par interaction de vagues |
| **T3 — acceptation corpus** | `hexaglue-acceptance` (chaîne complète frontend → moteur) | Les scénarios contextuels ; le cliquet par profil (`corpus-floor.properties`) | Chaque lot monte le plancher ; le build échoue en dessous ET au-dessus |
| **T4 — qualité** | réacteur | PIT (engine ≥ 87 %, pas de recul), 0 warning sur build propre | contrôle sur `make ci`, jamais sur un `verify` incrémental |

## 2. Doctrine d'arbitrage du corpus

La règle de relecture qui remplace celle des lots 11-14 :

1. **Un scénario ne peut attendre un kind que si ses sources portent de quoi
   le dériver** : ancre S1/S2, forme S4, ou relation S3 présente dans la
   fixture. Un type nu et isolé attend UNCLASSIFIED — c'est l'anti-règle
   « toute interface n'est pas un port », épinglée par le corpus lui-même.
2. **Aucune attente fondée sur un nom ou un package.** Les 47 mono-type nus
   passent en UNCLASSIFIED relu ; les 26 ancrés sont relus contre leurs
   ancres seules.
3. **Les fixtures des règles S3 utilisent des noms non conventionnels**
   (style profil 3 : `Ledger`, `Checkout`, `Hangar`) : le test prouve la
   relation et reste valide quelle que soit l'issue de la réévaluation du
   nommage en fin de M3. Les fixtures profil 1 gardent leurs noms
   conventionnels — elles prouvent que le moteur n'en a pas besoin.
4. **L'ancien moteur reste une observation, jamais un oracle** (lot 11) ;
   toute divergence assumée est inscrite scénario par scénario (fichier
   d'écarts, doc 08 §6.1).
5. Piège connu au branchement des goldens : `GoldenFiles.assertMatches` CRÉE
   le golden absent — le mode corpus exige un garde « golden existant »
   (lot 21).

Claims à confiance (`= KIND@CONFIDENCE`) : optionnelles ; avec le nommage hors
défaut, les verdicts visés sont S2/S3/S4 donc HIGH — l'extension perd son
urgence. À réexaminer au lot 18 si une montée de confiance devient un critère.

## 3. La grammaire des scénarios contextuels

Chaque règle du doc 09 reçoit : **(a)** un scénario où elle est l'évidence
décisive, **(b)** un contre-scénario où elle doit se taire, **(c)** sa place
dans un scénario intégral. Matrice minimale :

| Règle | Scénario nominal (a) | Contre-cas (b) |
|---|---|---|
| W1-DA | classe `@RestController`/`@KafkaListener` → DRIVING_ADAPTER | `@Service` seul ne fait pas un adapter |
| W1-DR | classe à champ `JdbcTemplate`/`EntityManager` → DRIVEN_ADAPTER | dépendance `NEUTRAL` (slf4j, validation) ne fait rien |
| W1-DR2 | classe implémentant un port piloté établi → DRIVEN_ADAPTER (tour suivant) | l'implémenteur situé dans le cœur → fausse frontière (finding M6, pas de kind adapter) |
| R4 | interface consommée par une classe du cœur, sans impl interne → DRIVEN_PORT | interface consommée par le seul adapter → pas un port |
| R5 (D14) | interface implémentée par le cœur + détenue par un DRIVING_ADAPTER → DRIVING_PORT | implémentée par le cœur mais détenue par personne → silence |
| W2-X | interface implémentée ET consommée par le cœur → jamais un port (candidat DOMAIN_SERVICE) | — |
| rôle de port | signatures convergeant sur un sujet du périmètre → REPOSITORY ; void unidirectionnel sur immuables → EVENT_PUBLISHER ; le reste → GATEWAY | — |
| R6/R6b | pivot (implémente driving port et/ou consomme driven ports ; ou appelée par un adapter) → APPLICATION_SERVICE | classe sans port ni appelant → silence |
| R8 | classe sans état, sur ≥2 types du domaine, consommée par le cœur → DOMAIN_SERVICE | classe à état mutable → jamais R8 |
| R1b | sujet des signatures d'un port-repository → AGGREGATE_ROOT | sujet externe (UUID nu) → pas de verdict sur le sujet, le lien reste |
| R2 | clé de recherche du port + champ du sujet → IDENTIFIER (résout le duel S4 `OrderId`/`Email`) | wrapper jamais utilisé en clé → reste candidat |
| R3a/R3b | possédé par un agrégat : avec identité → ENTITY, sans → VALUE_OBJECT **même mutable** (la mutabilité = finding M6) | type partagé sans possesseur → candidats S4 conservés |
| R7 | immuable publié par un port event-publisher ou retourné par une méthode d'agrégat → DOMAIN_EVENT | interface marqueur → silence (déjà épinglé par les 3 ex-faux) |

**Trois scénarios intégraux** (mini-applications complètes, toutes vagues) :

1. **Profil 1 — hexagonal propre** : à récolter des golden files multi-types
   de la carrière (`createBankingExample`, `createMinimalExample`,
   `createApplicationServiceExample`) et des `examples/`.
2. **Profil 2 — entreprise JPA** (petclinic-like) : entités JPA + Spring Data
   + `@Service` + contrôleurs. Épingle D7 (PERSISTENCE_MODEL sans effet de
   kind) et D8 (adapters classifiés) ; les findings eux-mêmes sont M6.
3. **Profil 3 — sans conventions** (starwars-like : `Fleets`,
   `AssembleAFleet`, `UUID` nu) : la preuve que la classification est
   purement relationnelle.

## 4. Les lots (15-23)

Numérotation dans la continuité du journal M3 (dernier : lot 14).

> **Avancement au 2026-08-03** : lots 15 à 23 **TOUS FAITS** (le détail de chacun
> est au journal de [CHANTIER.md](CHANTIER.md), une entrée par lot). Ce plan est
> soldé ; reste la clôture du jalon M3 (revue 07 §10 + feu vert utilisateur).
> Le plancher du cliquet est passé de 20/18 à **143/143 sur le profil 1**, et
> compte désormais par profil : **6/6 sur le profil 2**, **5/5 sur le profil 3**.
> La liste des échecs hérités est vide ; le plancher ne monte plus que par ce
> qu'un lot écrit. Les 154 scénarios s'exécutent aussi en goldens depuis le
> lot 21, plus aucun skipped.

### Lot 15 — reprise bornée D13
- **Code** : `ClassificationConfig.defaults()` sans suffixes ; le vocabulaire
  conventionnel survit dans une fabrique nommée dédiée (opt-in, et intrant du
  harnais du lot 23). `ConventionalName` et `Aggregator.decideOne` inchangés.
- **Tests** : config (défauts muets, préset nommé), S6 opt-in ; ré-arbitrage
  des 2 attentes du lot 14 vers UNCLASSIFIED.
- **Sortie** : `make ci` vert ; plancher recompté (attendu inchangé 20/18 :
  les 18 passants tiennent sur S1/S4, les 2 ré-arbitrés passent par le
  silence). Toute surprise ici est une découverte à consigner.

### Lot 16 — ré-arbitrage du corpus par la doctrine (§2)
- 47 mono-type nus → UNCLASSIFIED relus ; 26 mono-type ancrés relus contre
  leurs ancres seules ; 33 bi-types relus au cas par cas.
- Instruire la découverte du lot 14 : `OrderJpaRepository` hors périmètre
  dans `generatedInterfacesShouldBeSkippedFromPortClassification` (frontend
  ou filtre de portée).
- **Sortie** : plancher en forte hausse (~100 relus) ; zéro code moteur ;
  les échecs restants = la liste mesurée de ce que les lots 17-20 doivent
  faire passer.

### Lot 17 — vague W1 : adapters (D8)
- **T1** : W1-DA, W1-DR (+ contre-cas pack : `NEUTRAL`, `TECHNICAL`).
- **T3** : scénarios contextuels adapters ; premiers verdicts
  DRIVING_ADAPTER/DRIVEN_ADAPTER du moteur.
- **Sortie** : plancher monté ; PIT tenu.

### Lot 18 — vague W2 : ports par position (R4, R5/D14, W2-X, rôle)
- **T1** : R4, R5, W2-X, lecture du rôle par forme des signatures
  (REPOSITORY/GATEWAY/EVENT_PUBLISHER → `DrivenPortType`).
- **T2** : propagation W1→W2 (le port reconnu au tour suivant l'adapter) ;
  W1-DR2 (adapter par implémentation du port).
- **T3** : le scénario relu en attente de R4 passe ; scénarios R4/R5 en
  nommage non conventionnel (§2.3).
- **Sortie** : plancher monté ; les interfaces des scénarios contextuels ont
  leur direction sans un seul suffixe lu.

### Lot 19 — vague W3 : application (R6, R6b, R8)
- **T1** : pivot R6, R6b (appelée par un adapter), R8 (+ contre-cas état
  mutable). Statut des classes abstraites à arbitrer au lot, attente à
  l'appui.
- **T2** : propagation W2→W3.
- **Sortie** : plancher monté.

### Lot 20 — vague W4 : domaine par cycle de vie (R1b, R2, R3a/R3b, R7)
- **T1** : les cinq règles, dont VO mutable toléré (Q1 tolère ce que Q2
  condamne) et résolution du duel S4 par R2.
- **T2** : remplacement de verdict (S4 VALUE_OBJECT → R1b AGGREGATE_ROOT) ;
  convergence sur le scénario intégral profil 1.
- **T3** : les scénarios relus en attente de R2/R3 passent ; scénario
  intégral profil 1 vert de bout en bout.
- **Sortie** : plancher monté ; COMMAND/QUERY_HANDLER restent en retrait
  (assumé, doc 09 §4 vague 3).

### Lot 21 — assemblage `ArchModel` + `AnalysisRunner` + goldens
- Construction des records `ArchType` depuis (`CodeModel`, `FactBase`,
  `Verdicts`) ; catégorisation des UNCLASSIFIED : `TECHNICAL` depuis le fait S2
  du même nom, `AMBIGUOUS` depuis l'agrégateur (candidats sans marge),
  `UNKNOWN` pour le vide, raison « aucun contexte d'usage » pour les nus.
  **Pas de catégorie pour le code généré** : D15 (confirmée au lot 20) l'écarte
  du modèle, le moteur n'en voit aucun.
- `AnalysisRunner` dans `hexaglue-acceptance` (ServiceLoader) ; goldens
  `ArchModelSnapshots` **avec garde « golden existant »** (piège §2.5).
- **Sortie** : les 143 scénarios s'exécutent (plus aucun skipped) ; le
  scoreboard mesure tout le profil 1.

### Lot 22 — corpus profils 2 et 3 (D11) — FAIT
- Fixtures petclinic-like et starwars-like (§3) ; clés `profile2.*` /
  `profile3.*` au plancher ; épingles D7/D8 sur le profil 2.
- **Sortie** : le critère d'acceptation exécutable de D11 existe sur les
  trois profils. Six scénarios `Clinic-` (6/6) et cinq `Armada-` (5/5), le
  corpus paramétré par `CorpusProfile`, le plancher et les goldens comptés par
  profil. La limite du lot 17 (point d'entrée annoté sur une méthode) est levée
  au passage par `Selector.MemberAnnotated`.

### Lot 23 — harnais de réévaluation du nommage (clause de D13) — FAIT
- Runner comparatif : vocabulaire éteint vs allumé (préset du lot 15) sur les
  trois profils + exemples ; rapport par scénario : *gain* (le nom rejoint
  l'arbitrage humain) / *dégât* (il le contredit).
- **Sortie** : `NamingVocabularyTest` (154 scénarios passés deux fois) + le
  rapport commité en golden `naming-vocabulary.txt` ; `NamingShiftTest` exerce
  les cinq issues pour que « gain 0 » ne puisse pas être un angle mort du
  harnais. **Issue consignée au registre (D13) : le vocabulaire reste
  opt-in** — 0 gain partout, 55 dégâts sur le profil 1, **rien sur les
  profils 2 et 3**, où le vocabulaire ne fait que répéter ce que la position
  disait déjà. Écart au plan : les `examples/` ne sont pas couverts (carrière
  gelée, non branchée au nouveau réacteur) ; la mesure porte sur les verdicts,
  pas sur les candidats ni la confiance.

## 5. Hors de ce plan (assumé)

- **Q2 (findings HG-DEP/PURE/PORT/AGG/VAL/APP)** : M6 — le référentiel doc 09
  §5 en fixe déjà la matière ; M3 ne teste que ce que Q1 leur fournit.
- **S5 structurel (modules)** : M5 — le frontend n'alimente pas les modules
  avant l'hôte (écart assumé M2).
- **SAGA, COMMAND/QUERY_HANDLER complets** : en retrait jusqu'à réévaluation
  du nommage ou capacité `METHOD_BODIES`.
- **Extraction du domaine depuis l'entité JPA** : hors 7.0.0 (D7).
