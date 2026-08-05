# Chantier de refactoring HexaGlue — État et règles de conduite

> Ce fichier est LA source de vérité du chantier. Toute session de travail sur le
> refactoring DOIT commencer par sa lecture (avec [DECISIONS.md](DECISIONS.md))
> et se terminer par la mise à jour du journal ci-dessous.

## Sources de vérité (ordre de priorité)

1. **Le code source actuel** (`hexaglue/`) : seul état factuel.
2. **L'audit du 2026-07-31** (ce dossier : README + 01 à 05) : constats vérifiés
   `fichier:ligne`. Complété par [06-classification-metier.md](06-classification-metier.md)
   (analyse fonctionnelle), [07-architecture-page-blanche.md](07-architecture-page-blanche.md)
   (architecture cible, retenue par D12) et
   [08-plan-reecriture-ancree.md](08-plan-reecriture-ancree.md) (plan
   exécutoire de la réécriture). Toutes les décisions du registre sont
   tranchées : D7, D8 et D10, dernières en attente, le 2026-08-02.
3. **[DECISIONS.md](DECISIONS.md)** : seules les décisions qui y sont consignées
   avec le statut CONFIRMÉE font foi.

**Ne font PAS foi** (faits erronés, décisions caduques — confirmé par
l'utilisateur le 2026-07-31) :
- les sections architecture de `CLAUDE.md` (écarts documentés dans
  [03-architecture-modules.md](03-architecture-modules.md)) ;
- les documents `_internal/` antérieurs au 2026-07-31 ;
- les Javadoc/READMEs des modules quand ils contredisent le code (nombreux cas
  recensés dans l'audit : promesses non tenues, liens morts, contrats faux).

En cas de doute : lire le code, pas la doc.

## Règles de conduite (invariantes, toutes sessions)

1. **Jamais d'action sur une décision PENDING** de DECISIONS.md. Si un lot en
   dépend, le lot attend.
2. **Un commit = un lot homogène.** Ne jamais mélanger suppression de code mort
   et changement de comportement dans le même commit. Les corrections de bugs
   (B1-B15) sont des commits séparés, un par bug.
3. **Test rouge d'abord** pour tout correctif B1-B15 (TDD, règle projet).
4. **Aucune nouvelle règle de nommage** ne doit être introduite hors du
   vocabulaire de nommage du nouveau moteur (capteur S6, jalon M3). D'ici
   là : aucune nouvelle occurrence de `endsWith`/`startsWith`/`contains`/
   regex sur des noms de types/méthodes/packages.
5. **Vérification par lot** : `mvn test -pl <modules impactés>` pendant le
   travail, `make test` complet avant chaque commit, `make integration` avant de
   clore une phase.
6. **Pas de release ni deploy pendant le chantier** : `make release`,
   `mvn deploy`, `mvn release:*` et `git push --force` sont bloqués par la
   configuration (`.claude/settings.json` + hook `block-release-commands.sh`).
   `make release-check` (dry-run) reste autorisé.
7. **Références de chantier** (IDs B1-B15, phases, findings) : uniquement dans
   ce dossier `_internal/`. Jamais dans le code, les commentaires, les commits
   ni les PRs (règles projet existantes).
8. **Fin de session** : mettre à jour le journal ci-dessous (date, lots
   réalisés, état des tests, prochaine étape) et DECISIONS.md si une décision a
   été prise.
9. **Trouvaille hors périmètre** (nouveau bug, incohérence non recensée) : la
   consigner dans « Découvertes en cours de chantier » ci-dessous, ne pas la
   traiter dans le lot en cours.
10. **Carrière en lecture seule (D12)** : aucune modification sous
    `hexaglue/` (référence de comportement et carrière de récolte), sauf
    demande explicite de l'utilisateur. Le travail se fait dans
    `hexaglue-next/`.
11. **Transplantation avec tests (D12)** : tout code récolté depuis la
    carrière arrive avec ses tests dans le même lot.
12. **Périmètre gelé jusqu'à M8 (D12)** : fonctionnalités actuelles + cible
    des docs 06/07 ; toute idée nouvelle va au backlog post-7.0.0.
13. **Clôture de jalon (D12, amendée le 2026-08-04)** : corpus vert + revue
    contre les interdits du doc 07 §10 + **relecture ligne à ligne de la
    section du doc 07 que le jalon livrait** (un différé se consigne, jamais
    ne s'omet — c'est ce filtre qui manquait quand `DiagnosticSink` et
    `produces` ont été omis sans trace à M6) + journal mis à jour.

## Plan et état — jalons M0-M8 (réécriture ancrée, D12)

> **Réorientation du 2026-08-01 (D12)** : stratégie « réécriture ancrée ».
> Le plan exécutoire est [08-plan-reecriture-ancree.md](08-plan-reecriture-ancree.md) ;
> l'ancien plan en phases 0-5 (ci-dessous, conservé pour trace) est caduc —
> son contenu est recyclé : bugs → tests de régression, inventaires → liste
> de récolte, garde-fous → jalon M0.
> Statuts : À FAIRE / EN COURS / FAIT / BLOQUÉ(décision).

| Jalon | Contenu | Statut | Notes |
|---|---|---|---|
| M0 | Socle `hexaglue-next/` : réacteur, CI, qualité, testkit + corpus P1 | FAIT (2026-08-01) | Build vert, 122 scénarios corpus exécutables (skipped sans moteur) |
| M1 | `hexaglue-model` (contrat pur, trace de classification complète) | FAIT (2026-08-02) | 9 commits (8 lots f03c062…0c8784a + corrections 5ef73c1). Contrat complet : CodeModel, Classification tracée, ArchType, ArchModel+indexes, Finding/Diagnostic, config typée, sérialiseur snapshot testkit. Revue 07 §3/§10 : conforme ; écart assumé = pas d'adapters dans ArchKind — D8 confirmée le 2026-08-02, à combler au premier lot de M3 |
| M2 | `hexaglue-frontend` (Spoon+classpath, stubs externes, nested, valeurs typées) | FAIT (2026-08-02) | 10 commits (af2f7bf…ccd0c20). G1-G4 natifs ; corps de méthode sous capacité ; échec bruyant codé HG-FRONTEND-001/002/003. Écarts assumés : modules non alimentés (pas d'hôte avant M5), pas de canal de diagnostic sur le résultat |
| M3 | `hexaglue-knowledge` + `hexaglue-engine` (solveur à saturation, packs, règles du référentiel doc 09) | FAIT (2026-08-03) | **Toutes les décisions sont tranchées** (D13 dissoute, D14 « A étagé », D15 = le frontend garde la main sur le code généré, le 2026-08-02) ; le référentiel des règles est [09-referentiel-regles.md](09-referentiel-regles.md), le plan de tests du reste de M3 est [10-plan-tests-m3.md](10-plan-tests-m3.md) (lots 15-23). FAITS : amendements du contrat M1 (lots 1-3), `hexaglue-knowledge` (4-6), machinerie moteur + S1 + agrégateur + boucle + R1 (7-10), cliquet corpus (11), capteurs S4/S6 (12-14), vocabulaire de nommage rendu opt-in (15), corpus profil 1 arbitré de bout en bout — 114/122 (16), vague W1 adapters — 119/127 (17), vague W2 ports par position + rôle par signatures — 125/133 (18), vague W3 application — 132/138 (19), vague W4 domaine par cycle de vie — 143/143 (20), assemblage `ArchModel` + corpus branché sur le moteur, 143 goldens et plus aucun skipped (21), corpus profils 2-3 et sélecteur « annotation sur un membre » — 143/143, 6/6, 5/5 (22), harnais de réévaluation du nommage : **issue de D13 mesurée, le vocabulaire reste opt-in** (23). **Les lots 15-23 sont tous faits ; reste la clôture du jalon** (revue 07 §10 + feu vert). S5 structurel reporté à M5 (modules non alimentés avant l'hôte) |
| M4 | Explain : la restitution (sans hôte) | FAIT (2026-08-03) | **Le CLI est sorti du jalon (D17)** : c'est un hôte, et toutes les décisions d'hôte sont à M5. Livré : `Explanation` (verdict, raisons, arbre de dérivation) et `Outcome` (bilan agrégé d'un run) dans `hexaglue-engine`, plus le cliquet — un golden de restitution par profil et quatre invariants sur les 154 scénarios. La restitution est indépendante de l'hôte : logs du plugin Maven (M5), rapport d'audit (M6), CLI éventuel |
| M5 | L'hôte : YAML strict, diagnostics, gates validate, maven-plugin | FAIT (2026-08-03) | **Jalon redéfini à son ouverture (D18)** : le SPI, les sinks, le DAG deux passes et l'isolation `LinkageError` sortent du jalon — aucun consommateur avant M6 — et passent en tête de M6, écrits contre leur premier plugin. Les quatre décisions d'hôte sont prises : D18 (périmètre), D19 (racine de sources déclarée seule), D20 (le frontend rend un résultat porteur), D21 (multi-module, amendée en cours de jalon). Six commits ; 951 tests, 7 cas d'intégration sur des builds réels, cliquet inchangé. Deux décisions de plus en cours de jalon : **D21 amendée** (topologie et S5 → M6, faute de substrat) et **D22** (le moteur rend ses propres diagnostics). Trois `test-param-*` non portés, écarts assumés au journal |
| M6 | SPI + living-doc + audit (findings codés, provenance) | **FAIT (2026-08-04)** — neuf lots, vingt commits ; 1 163 tests, 10/10 en intégration, cliquet inchangé. Clôture au journal : 14 `test-param-*` arbitrés, `case-study-banking` rejoué et comparé, revue des dix interdits (trois violations trouvées et corrigées) | **Jalon cadré à son ouverture par quatre décisions** : D25 (le SPI est écrit contre living-doc, le plus petit consommateur), D24 (les règles de findings et le substrat de graphe vivent au moteur — `findingThresholds` trouve son sujet et B9 devient vrai par construction), D23 (le rapport publié fait foi : sept sections, quatre sorties de la carrière au backlog), **D16 tranchée** (option C : Q1 muet, Q2 le dit). Neuf lots ; B1, B7, B8, B9, B10 en tests de régression ; l'analyse réacteur au lot 8 (D21 amendée) |
| M7 | jpa + rest (seuil de certitude) | **EN COURS** (ouvert 2026-08-04) | **Scindé : M7a = jpa, M7b = rest** (le premier consommateur arbitre, comme D25). Cadré par **D27** (`produces` au manifeste, le jugement le lit), **D28** (`SourceSink` + `DiagnosticSink`, seuil typé dans la `Contribution`, goal `generate`) et **D29** (l'assemblage remplit `Field`). **M7a CLOS** — huit lots au journal. **M7b ouvert le 2026-08-05**, cadré par **D34** (les corps lus en production, `UseCase.type()` véridique), **D35** (ce dont un port pilotant parle = un lien du moteur), **D36** (la lecture du domaine extraite hors de jpa) et **D37** (handler d'exceptions et câblage de beans hors périmètre) ; huit lots au journal |
| M8 | Gate de parité, bascule, release 7.0.0 | À FAIRE | Levée de D5 = décision explicite |

### Point de reprise (au 2026-08-05, **ARRÊT DEMANDÉ — revue des dérives avant tout autre lot**)

> **⚠ NE PAS ENCHAÎNER SUR LE LOT 5.** L'utilisateur a arrêté la session du
> 2026-08-05 en énonçant que **le chantier a connu des dérives**, qu'il faut les
> résorber et repartir sur de meilleures bases. **La prochaine session s'ouvre
> par cette revue**, pas par la suite de M7b. Le plan des neuf lots, le point de
> reprise détaillé ci-dessous et le journal restent valides comme **état des
> lieux**, pas comme feuille de route à reprendre telle quelle.
>
> **Ce qui est acquis et vert** : quatre commits `5a53135` → `d5386a2`, **non
> poussés**, arbre propre ; 1 401 tests après `make ci` clean, 12/12 en
> intégration, `make verify` vert.
>
> **Observations de la session, à confirmer ou infirmer par l'utilisateur** —
> elles ne sont PAS des constats établis, seulement ce que l'agent a remarqué et
> qui pourrait correspondre à ce qu'il nomme dérives :
>
> 1. **Un jalon qui ne livre pas ce qu'il nomme.** M7b s'appelle « rest » et
>    quatre lots plus tard aucune ligne du backend rest n'existe : D34, D38, D35
>    et D36 sont du moteur, du modèle et du SPI. Chaque lot était justifié
>    séparément ; l'enchaînement, lui, éloigne du livrable.
> 2. **Le registre décidé et exécuté dans la même session.** L'agent rédige la
>    décision, l'utilisateur valide sur cette rédaction, l'agent l'implémente —
>    et l'amende quand la mesure le contredit (D35, « prennent et rendent » →
>    « nomment »). Le registre perd sa fonction de contrôle s'il est écrit par
>    celui qu'il contrôle.
> 3. **Le cliquet re-enregistré par celui qui le déplace.** Six scénarios en
>    verdicts, quinze goldens en liens, des attentes qu'un humain avait
>    « relues et vouchées » ré-arbitrées, un scénario renommé — le tout dans le
>    lot qui les bouge, par l'agent. Le frein est devenu une formalité.
> 4. **Les documents cibles amendés pour suivre le code.** R5b et R9 ajoutés au
>    doc 09, écarts consignés contre 07 §6.4. La cible se réécrit au rythme de
>    l'implémentation.
> 5. **La doctrine tirée d'un seul projet réel.** D32, D38 et l'amendement de
>    D35 viennent tous de `_probes/ecommerce-hexagonal`. Un banc unique est une
>    base étroite pour des règles de moteur.
>
> **À faire à l'ouverture de la prochaine session** : écouter d'abord le
> diagnostic de l'utilisateur, ne rien présumer de cette liste, et ne reprendre
> aucun lot avant que la conduite du chantier soit re-cadrée.

### Point de reprise détaillé (au 2026-08-05, M7b lots 1-4 faits, lot 5 non ouvert)

> **M7b est ouvert et quatre lots sont faits** (journal du 2026-08-05, en fin de
> fichier : l'instruction du backend rest de la carrière, la mesure sur les 27
> cas d'usage du banc, les décisions, le plan des neuf lots).
>
> **Cadré par D34-D38** : les corps de méthode sont lus en production et
> `UseCase.type()` est véridique (un cas d'usage qui **remet un type possédé** à
> un port piloté change quelque chose — aucun nom lu) ; **un port pilotant se lit
> aussi par l'intérieur** (answered par le cœur, détenu par personne — D38, née
> de la mesure du lot 1) ; ce dont un port pilotant parle devient un lien du
> moteur (exactement un agrégat, sinon silence) ; la lecture du domaine de D30
> sort de jpa vers un commun (§10.1 satisfait : le second consommateur existe) ;
> le `@ControllerAdvice` global et la classe `@Configuration` de câblage partent
> au backlog.
>
> **Quatre commits, `5a53135` → `d5386a2`** (non poussés). Arbre propre.
> **1 401 tests** (`make ci` clean), aucun skipped, `make verify` **vert**
> (0 violation Checkstyle), **12/12 en intégration**. **Cliquet déplacé de 6 scénarios sur 154 en verdicts** (tous
> relus et arbitrés dans le lot qui les a bougés) **et de 15 goldens en liens au
> lot 3** (15 lignes ajoutées, zéro supprimée — aucun verdict) ; un scénario
> renommé (`PortBoundary-contractNoRingCallsIsStillAWayIn`) parce que son nom
> énonçait la doctrine que D38 retourne. **Banc : 0 → 6 ports pilotants, chacun
> avec le bon agrégat pour sujet (6/6), non classés 10 → 4, score 62 → 65**,
> 25 types générés inchangés. **Mesure D13 : damage 55/47 scénarios → 53/45** —
> deux scénarios de moins dépendent des noms.
>
> **Prochaine action : lot 5 de M7b** — le socle de rest : options typées
> strictes, le nommage écrit **vers** HTTP (kebab, pluriel — un nom émis, jamais
> lu, comme `SqlNames`), et le manifeste qui déclare `produces` =
> `PortFamily.driving()` : le **second déclarant**, qui solde le §10.1 assumé à
> la clôture de M7a. Puis endpoints (lot 6), DTO (lot 7 — c'est là que la
> partition de `Crossing` s'extrait, cf. journal du lot 4), contrôleurs (lot 8),
> l'hôte et le banc (lot 9).
>
> **ATTENTION — le registre porte une décision PENDING (D33)** : une valeur
> détenue par un agrégat lue DOMAIN_EVENT parce qu'un notifieur la transporte.
> Ne pas agir dessus sans arbitrage. D31, D32 et D34-D38 sont confirmées.
>
> **Deux défauts du banc restent ouverts** : D33, et le **défaut 4** — le mapper
> exigeant que le constructeur présente l'état dans l'ordre des champs. Ce
> dernier est **désormais traitable** : son appariement sûr demandait « ce que le
> constructeur affecte », que le modèle porte sous `METHOD_BODIES` — la capacité
> que le lot 1 vient d'allumer en production.
>
> **Piège d'outillage à retenir** : `mvn test -pl hexaglue-acceptance` **sans
> `-am`** résout le moteur depuis le dépôt local et mesure donc un moteur périmé.
> Toute mesure de corpus se prend avec `-am` ou depuis la racine.
>
> État de sortie de M7a (2026-08-04) : treize commits, `bf11931` → `3a62fb8`,
> tous poussés. **1 392 tests**, `make ci` vert, **12/12 en intégration**,
> cliquet 143/143 + 6/6 + 5/5, **36 warnings dont zéro de compilation**. Module
> jpa : **96 % de mutants tués**.
>
> **Ce qu'un lecteur de la doc publique croit désormais** (README racine + un par
> backend) : quatre goals dont `generate` écrit et ne juge pas ; tout ce qui est
> généré porte la marque de génération ; un backend déclare la famille de ports
> qu'il remplit, et l'audit ne signale plus ceux-là en le disant ; jpa garde un
> enum tel quel, par nom et jamais par rang. Toute évolution de M7b qui contredit
> l'une de ces phrases corrige la phrase dans le même lot.

### Point de reprise précédent (au 2026-08-04, M7a clos)

> **PÉRIMÉ — conservé pour trace.** Le point de reprise en vigueur est celui
> au-dessus.
>
> **M7a est CLOS** : huit lots, la génération JPA marche de bout en bout sur de
> vraies sources, et **l'exemple est généré ET compilé** (critère de sortie
> tenu). La clôture sous règle 13 amendée est au journal, en fin de fichier :
> relecture ligne à ligne de 07 §6.4 (**deux écarts consignés** — le seuil est
> celui du projet et non du plugin ; jpa n'appelle ni `CompositionIndex` ni
> `DomainIndex`, il lit les liens de l'assemblage), revue des dix interdits
> (**un §10.1 assumé** : `PortFamily.Driving` attend son déclarant à M7b) et
> warnings **comptés** — 36, dont **zéro de compilation** après avoir soldé le
> seul qui l'était.
>
> **Le premier banc sur projet réel** (`_probes/`, hors réacteur) a trouvé quatre
> défauts qu'aucune suite du réacteur ne voyait. **Deux sont corrigés** : l'enum
> non compilable (`e07c52d`) et l'identité perdue sur cinq agrégats sur six
> (`7b208d8`, **D32** au registre — la forme de la réponse départage, puis le
> point fixe). Mesure : identifiants lus IDENTIFIER **1/6 → 5/6**, types générés
> **13 → 25**. **Deux restent ouverts** : **D33 (PENDING)** — une valeur détenue
> lue DOMAIN_EVENT parce qu'un notifieur la transporte — et le **défaut 4**, le
> mapper exigeant que le constructeur présente l'état dans l'ordre des champs
> (correctif non évident : apparier par type seul écrirait un mapper qui compile
> et se trompe de colonne).
>
> **Ensuite : ouvrir M7b (rest)** — même méthode que M7a : instruire le
> code de la carrière d'abord, arbitrer ensuite. Ce que M7b reprend sans les
> rouvrir : D27/D28/D30/D31, `SourceSink`, le seuil, `@Generated`. Ce qu'il
> apporte : le second déclarant de `produces` (`PortFamily.Driving`), et la
> question de l'extraction d'un nommage commun (la carrière porte une seconde
> `NamingConventions` dans rest — §10.1 : le second consommateur existera).
>
> **ATTENTION — le registre porte une décision PENDING (D33)** : une valeur
> détenue par un agrégat lue DOMAIN_EVENT parce qu'un notifieur la transporte.
> Ne pas agir dessus sans arbitrage. D31 et D32 sont confirmées.
>
> État de fin de session (2026-08-04, troisième session du jour) : **treize
> commits, `bf11931` → `3a62fb8`, tous poussés** ; arbre propre, local et
> distant identiques. **1 392 tests**, aucun skipped, `make ci` **vert**,
> **12/12 en intégration**, cliquet inchangé (143/143 + 6/6 + 5/5), **36
> warnings dont zéro de compilation**. Module jpa : **96 % de mutants tués**.
>
> **Ce qu'un lecteur de la doc publique croit désormais** (README racine + un par
> backend, mis à jour en fin de session) : quatre goals dont `generate` écrit et
> ne juge pas ; tout ce qui est généré porte la marque de génération ; un
> backend déclare la famille de ports qu'il remplit, et l'audit ne signale plus
> ceux-là en le disant ; jpa garde un enum tel quel, par nom et jamais par rang.
> Toute évolution de M7b qui contredit l'une de ces phrases corrige la phrase
> dans le même lot.

### Point de reprise précédent (au 2026-08-04, M7a lot 1 à faire)

> **PÉRIMÉ — conservé pour trace.** Le point de reprise en vigueur est celui
> au-dessus. Ce qui est dit ici du registre (« aucune décision PENDING ») était
> vrai ce jour-là et ne l'est plus : **D33 est PENDING**.
>
> **À lire en premier pour reprendre M7** : le journal du 2026-08-04
> ci-dessous, « Jalon M7 : ouverture » (l'analyse cible/SPI, les décisions,
> le plan des huit lots de M7a). M7 est cadré par **D27** (`produces` au
> manifeste, le jugement le lit), **D28** (`SourceSink` + `DiagnosticSink`,
> seuil typé dans la `Contribution`, goal `generate`) et **D29**
> (l'assemblage remplit `Field`). Périmètre scindé : **M7a = jpa,
> M7b = rest**. **Le registre ne porte plus aucune décision PENDING.**
>
> **M6 est CLOS** : neuf lots, vingt commits, plus trois de documentation.
> Les points ci-dessous décrivent son état de sortie, qui reste le socle.
>
> - **Vingt-trois commits poussés le 2026-08-04, `949a1b5` → `ed989a0`.** Arbre
>   propre, local et distant identiques, aucune divergence — le dépôt public
>   porte tout M6 et sa documentation.
> - **1 163 tests**, aucun skipped ; `verify -Pquality` vert **sur tout le
>   réacteur** ; **10/10 en intégration** ; cliquet inchangé — 143/143, 6/6, 5/5.
> - **D26 est au registre** (2026-08-04) : la lecture S5 établit une propriété de
>   module, jamais un kind. Le registre ne porte toujours aucune décision PENDING.
> - **Des trois questions ouvertes par la clôture, une est tranchée** :
>   `HG-HEX-002` sur un projet dont les adapters sont générés = **D27**.
>   Restent la portée de la mesure D13 (gate M8) et le goal `validate` resté
>   mono-module (avant M8 — D27 y touche : `validate` lira les manifestes,
>   le gate et le rapport doivent juger pareil).
> - **Le réacteur compte quatre modules de plus** : `hexaglue-spi`,
>   `hexaglue-render`, `hexaglue-plugins/hexaglue-plugin-audit`,
>   `hexaglue-plugins/hexaglue-plugin-living-doc`. Deux dépendances nouvelles :
>   JGraphT (moteur, composantes fortement connexes) et Jackson (audit, JSON).
> - **M7 est ouvert (2026-08-04)** — instruction du code d'abord, arbitrage
>   utilisateur ensuite, comme M4-M6. **Lot 1 de M7a FAIT** (`43460fd`) :
>   l'assemblage remplit `Field`, 38 goldens relus, 1 178 tests, cliquet
>   inchangé, `Fields` à 30/30 mutants tués. **Lot 2 FAIT** (`37fb5a3`) :
>   `SourceFile`/`SourceSink`/`DiagnosticSink` au SPI, seuil typé dans la
>   `Contribution` et branché sur le YAML, `HG-PLUGIN-008` ; 1 208 tests,
>   aucun survivant PIT sur le code du lot. **Les onze warnings préexistants
>   sont soldés** (`aa85f99`, hors jalon) : `make ci` n'en émet plus aucun.
>   **Lot 3 FAIT** (`ec37dab`) : module `hexaglue-plugin-jpa`, `SqlNames`
>   (B2 corrigé par construction, 23 mots réservés) et `JpaOptions`
>   (B15 : le code avait raison, sa Javadoc non) ; `PluginConfig.choice`
>   au SPI ; 1 281 tests, zéro warning, 100 % de mutants tués sur le lot.
>   **Lot 4 FAIT** (`1b19d3b`) : le plugin est enregistré et écrit entités et
>   embeddables (javapoet 0.18.0) ; le seuil de D28 a son premier consommateur
>   (`HG-JPA-001`/`002`, le plugin continue) ; 1 300 tests, **100 % de mutants
>   tués sur le module jpa**. **Lot 5 FAIT** (`8874576`) : le dépôt Spring Data,
>   ses requêtes dérivées **de la forme et non du nom** (B2 du générateur),
>   `HG-JPA-003` ; 1 312 tests. **Les adapters sont partis au lot 6**, avec les
>   mappers dont ils dépendent — sans quoi le code généré ne compilerait pas
>   entre les deux lots. **Lot 6 FAIT, PARTIEL** (`bf11931`) : les mappers,
>   avec deux décisions utilisateur (lire le domaine **par la forme**, le
>   reconstruire **par son constructeur**) et `HG-JPA-004` ; 1 317 tests, mais
>   **83 % de mutation seulement** sur le module jpa, en retrait des lots 4-5.
>   **Les adapters de ports ne sont pas faits** et passent au lot 7.
>   **Prochaine action : lot 7** — adapters de ports, `produces` au manifeste
>   et D27 côté juge, **plus une passe de couverture sur `StoredMapper` et
>   `DomainAccess`** pour ramener le module au niveau des autres.
>
> **Fin de session du 2026-08-04 (seconde session du jour).** Sept commits,
> `ed989a0` → `bf11931`, **non poussés** ; arbre propre. Le registre porte
> quatre décisions de plus — **D27, D28, D29** (ouverture de M7) et **D30**
> (lot 6) — et **aucune PENDING**. État mesuré : `make ci` vert,
> **1 317 tests**, aucun skipped, **zéro warning** (les onze préexistants ont
> été soldés hors jalon), **10/10 en intégration**, cliquet inchangé
> (143/143, 6/6, 5/5). Le réacteur compte un module de plus,
> `hexaglue-plugins/hexaglue-plugin-jpa`, et une dépendance de plus,
> javapoet 0.18.0.
> - **Ce qu'un lecteur de la doc publique croit désormais** (README racine +
>   un README par backend) : trois goals, `hexaglue.yaml` strict et hérité, le
>   code généré écarté et ses conséquences énoncées, seize codes de findings
>   documentés. Toute évolution de M7 qui contredit l'une de ces phrases doit
>   corriger la phrase dans le même lot.
> - **Écarts déjà décidés à porter au gate de parité M8** (en plus des trois de
>   M5) : les quatre sorties d'audit écartées par D23 (générateur de CI,
>   comparateur d'historique, charts radar/quadrant, renderer HTML) ; la
>   correspondance **non 1:1** des règles hexagonales (tableau au journal) ; la
>   liste de terminaisons au passé de `ddd:event-naming`, remplacée par
>   HG-NAME-001 qui lit le vocabulaire opt-in ; les paramètres
>   `audit-error-on-*`, remplacés par `validation.findings` au moteur ; **la
>   détection du rôle d'un module par le suffixe de son `artifactId`**, remplacée
>   par une déclaration (`modules:`) ; **le goal `reactor-audit` de la carrière**,
>   dont l'équivalent v7 s'appelle `reactor-report` et exécute les backends
>   installés, quels qu'ils soient.
> - **Deux points de conception à relire** : la composition du score de l'audit
>   (quatre parts, moyenne simple, grade A-E) et le fait que
>   `Finding.remediations` ne sait exprimer qu'un correctif de classification.

> **À lire en premier : [09-referentiel-regles.md](09-referentiel-regles.md)
> (le référentiel des règles, formulation qui fait foi).**
> D13 est dissoute (le nommage reste opt-in, issue mesurée au lot 23), D14 est
> « A étagé », **D15 CONFIRMÉE** (le code généré n'entre pas dans le modèle) et
> **D17 CONFIRMÉE** (le CLI n'est pas un livrable de la 7.0.0 ; la restitution
> est indépendante de l'hôte). **Une seule décision PENDING au registre : D16**
> (une partie dont l'identité est un type de plateforme nu — ENTITY ou
> VALUE_OBJECT ?), à trancher au plus tard à M6 ; **rien à M5 n'en dépend**.
> **M3, M4 et M5 sont CLOS** (revues de clôture au journal). La prochaine
> session ouvre **M6 — SPI + living-doc + audit** : le SPI y entre en tête de
> jalon (D18), écrit contre son premier plugin, et la topologie de modules avec
> lui (D21 amendée). Les décisions d'hôte sont prises : D19 (racines), D20
> (le frontend dit ce qu'il écarte), D22 (le moteur dit ce qu'il n'a pas
> classé).

- **Où** : neuf commits cette session, **poussés sur `origin/main`**, suivis des
  deux PR Dependabot mergées en squash. M4 proprement dit : `c9bba94` (rendu du
  verdict), `69affda` (bilan agrégé d'un run), `16a829c` (ne dire sous une
  raison que ce que l'en-tête n'a pas dit), `bc19f11` (cliquet : golden de
  restitution par profil + invariants sur tout le corpus). Puis la lisibilité
  des logs, demandée après coup : `2c2f4fb` (le score interne remplacé par ce
  qu'il encode), `c32e408` (paliers nommés, hiérarchie dite, collision `S<n>`
  cassée), `09ca88d` (chaque règle dit ce qu'elle conclut), plus `8149672` et
  `fbd7ff3` (README du dépôt public). Arbre propre, local et distant
  identiques. `make ci` vert (**898 tests, aucun skipped**), **aucun warning de
  compilation** sur build propre (vérifié sur le log complet, pas sur sa fin :
  un `tail` avait d'abord masqué la question). Dépendances à jour :
  snakeyaml-engine 3.0.1, classgraph 4.8.186.
- **Ce que M4 a livré, et pourquoi sous cette forme** : `Explanation` et
  `Outcome` dans `hexaglue-engine` — le verdict, ses raisons, l'arbre de
  dérivation, et le bilan d'un run — rendus en `List<String>` pour que le
  plugin Maven (M5), le rapport d'audit (M6) et un CLI éventuel consomment le
  même. **Le rendu est une feuille du pipeline, jamais un étage** : personne ne
  relit le texte, la structure reste sur `ArchType`/`Classification`/`Outcome`.
- **Ce que M4 a réparé sans le chercher** : l'arbre de preuve était une sortie
  que **rien ne lisait** — construit par chaque règle depuis M3, porté par
  chaque verdict, absent de tous les goldens. Il est désormais gravé, trois
  goldens de restitution (2 924 lignes) plus quatre invariants sur tous les
  types de tous les scénarios.
- **Limite connue de l'arbre, à connaître avant de le lire** : la chaîne
  s'arrête au tour qui l'a produit. Une règle qui lit les verdicts du tour
  précédent n'a aucune preuve à citer (`Classifier` jette la base de faits de
  chaque tour), donc son nœud est une feuille — sur `Clinic-theWholeClinic`,
  6 nœuds de règle sur 10. Ce que ces raisons nomment à la place, ce sont les
  types sur lesquels elles s'appuient (`involving`) : c'est le modèle qui est
  navigable, pas l'arbre qui prétend être complet. Restaurer les prémisses
  inter-tours est de la chirurgie sur `Classifier` et sur toutes les règles,
  non engagée.
- **L'issue de D13 est mesurée et consignée** : le vocabulaire **reste
  opt-in**. 0 gain partout ; 55 dégâts sur le profil 1 (fixtures mono-type
  héritées), **rien du tout sur les profils 2 et 3** — là où il a de quoi
  parler, le nom répète ce que la position disait déjà. Le harnais
  (`NamingVocabularyTest` + golden `naming-vocabulary.txt`) reste au réacteur.
- **Décisions** : **D16 PENDING** (ouverte au lot 22 par la mesure du profil 2 :
  une partie dont l'identité est un type de plateforme nu se lit VALUE_OBJECT ;
  trois options instruites au registre, recommandation C). Règle de conduite 1 :
  ne pas agir dessus. **D15 confirmée le 2026-08-02** — le code
  généré n'entre pas dans le modèle ; c'est un périmètre de lecture, pas une
  classification anticipée. **D13 dissoute** — le rôle est une
  position dans un graphe ; S6 et la moitié conventionnelle de S5 sortent des
  défauts, réévaluation **mesurée** en fin de M3 (lot 23, issue à consigner
  au registre). **D14 = A étagé** — port driving reconnu structurellement,
  `INVOKES` en renfort sous capacité, jamais une condition.
- **Doctrine du doc 09, en deux lignes** : Q1 (identification) tolère ce que
  Q2 (conformité) condamne ; un seul jeu de règles pour migration et
  hexagonal existant, dérivation par vagues bord → centre (W1 adapters →
  W2 ports par position → W3 application → W4 domaine par cycle de vie).
- **Ce qui existe dans `hexaglue-engine`** (à lire avant d'écrire une règle) :
  `Fact` scellé (`KnowledgeAssertion`, `KindEvidence`, `Relation`, `PortRole`)
  indexé par `Predicate` dans `FactBase` ; `Rule` déclare `reads()`/`writes()`
  et reçoit une `Derivation` (accès au `CodeModel`, aux packs, à la config, au
  périmètre, aux faits, aux **verdicts du tour précédent**) ; `Saturation`
  boucle en semi-naïf **à l'intérieur** d'un tour ; `Aggregator` décide ;
  `Classifier` rejoue tours et décisions jusqu'à stabilité. Catalogue des
  règles : `io.hexaglue.engine.rule.Catalogue.all()`.
- **Quatre utilitaires partagés entre règles** (lots 18-20), à réutiliser plutôt
  qu'à redécouvrir : `Contracts` (implémenteurs, détenteurs, collaborateurs et
  supertypes dans les deux sens, cœur vs anneau, et l'anti-règle W2-X en
  Javadoc), `Shapes` (immuabilité, état, et **la lecture unique de la forme d'une
  identité**, `readsAsIdentity`), `Signatures` (les types du périmètre que les
  méthodes nomment, conteneurs déballés) et `Lifecycle` (les ties de stockage,
  la possession et « porte une identité » ; un contrat et une valeur en forme
  d'identité ne sont jamais des parties).
  « Détenir » ignore les champs statiques ; « du cœur » = pas un adapter au
  tour précédent ; « réclamé par le cœur » = un verdict ni UNCLASSIFIED ni
  adapter ; « partie » = classe/record/enum du modèle, ni le possesseur
  lui-même, ni un contrat, ni une valeur en forme d'identité.
- **Comment lire les faits au point fixe** (ce que fait `Analysis`, et ce que
  font les tests du rôle) : `Saturation.saturate(RuleSet.standard(),
  context.withVerdicts(Classifier.classify(context)))`. `Classifier` ne rend
  que les verdicts — la base de faits de chaque tour est jetée.
- **Ce qui existe depuis le lot 21** : `Analysis.analyze(EngineContext)` rend
  l'`ArchModel` complet. La ligne qui gouverne son remplissage est dans la
  Javadoc de `Links` — *une décision de règle se relit dans le lien que cette
  règle a énoncé, une déclaration se relit sur la déclaration* — et c'est
  pourquoi `RelationKind` compte quatre liens (`MANAGES`, `IDENTIFIED_BY`,
  `OWNS`, `ANNOUNCES`). Toute nouvelle règle dont un record a besoin du résultat
  énonce son lien plutôt que de laisser l'assemblage le redériver.
- **Posture de nommage (lot 15)** : `ClassificationConfig.defaults()` ne porte
  plus de suffixes ; le vocabulaire vit dans `conventional()` /
  `conventionalNamingSuffixes()`, opt-in, et sert d'intrant au harnais du
  lot 23. `ConventionalName` (S6) et `Aggregator` sont inchangés : le capteur
  lit une liste vide et se tait.
- **Mesure en place (lots 11, 16-22)** : cliquet (`hexaglue-acceptance`,
  `corpus-floor.properties`), **compté par profil** — **143/143 (profil 1),
  6/6 (profil 2), 5/5 (profil 3)**, et 154 goldens. Un total unique laisserait
  un gain sur les sources écrites dans notre vocabulaire payer une perte sur
  celles qui n'en ont aucun. Le corpus profil 1 hérité est intégralement
  arbitré **et intégralement vert** : il prouve surtout l'anti-règle (une
  interface isolée n'est pas un port) et les ancres S1/S2 ; s'y ajoutent
  vingt-et-un scénarios écrits en vocabulaire non conventionnel — cinq
  d'adapters (`AdapterRing-`), six de frontière (`PortBoundary-`), cinq
  d'application (`ApplicationLayer-`) et cinq de domaine (`DomainLifecycle-`) —
  puis six d'entreprise (`Clinic-`, profil 2) et cinq sans vocabulaire
  (`Armada-`, profil 3). Le plancher ne monte plus que par ce qu'un lot
  **écrit**, et **ré-arbitrer ce qu'une vague déplace fait partie du lot**
  (deux attentes au lot 19, aucune aux lots 20 et 22).
- **Ajouter un profil** : déclarer sa valeur dans `CorpusProfile`, poser
  `corpus/<profil>/scenarios.txt` + les scénarios, `golden/<profil>/`, et les
  deux clés au plancher. Les invariants structurels (`CorpusTest`) et les
  goldens (`CorpusGoldenTest`) s'y appliquent d'eux-mêmes.
- **Prochaine action : M6 — SPI + living-doc + audit.** Le SPI (sinks, manifest,
  DAG deux passes, isolation `LinkageError`) y entre **en tête de jalon**, écrit
  contre le premier plugin qui le consomme et arbitré par lui (D18) ; B7 y
  devient un test de régression. La topologie de modules et S5 arrivent avec
  l'analyse réacteur (D21 amendée). **Ce qu'un plugin trouvera en place** :
  `ArchModel` complet, `Explanation`/`Outcome` pour le rendu, `Validation` pour
  les portes, et les trois canaux de diagnostics (frontend, moteur, hôte) déjà
  codés — un plugin qui veut dire quelque chose sur l'outil lui-même a un
  vocabulaire, pas à en inventer un.
- **Les trois dettes de M5 sont soldées** : racines de sources (D19 — la racine
  déclarée seule, delombok substitué, jamais `target/`), canal de diagnostics du
  frontend (D20) et du moteur (D22), chargement YAML strict (`ConfigLoader`,
  clé inconnue = erreur codée).
- **Ce qu'un hôte a déjà sous la main** (livré à M4, à ne pas réécrire) :
  `Explanation.of(archType)` / `withDerivation(archType)` / `of(outcome)` et
  `Outcome.of(archModel)`, dans `hexaglue-engine`. Le rendu sort en
  `List<String>` — `forEach(log::info)` côté Maven — et la structure reste
  lisible séparément. Un hôte qui reformaterait le texte ou qui le relirait
  pour en tirer une sévérité rouvrirait le pivot `String` (07 §10.2).
- **Comment mesurer sans rien graver** (procédé du lot 14, réutilisable) :
  marquer temporairement les brouillons `status: reviewed`, lancer
  `mvn -pl hexaglue-acceptance -am test`, lire la liste des échecs dans le
  message du cliquet, puis `git checkout` du répertoire du corpus. C'est ce qui
  a débusqué le défaut du capteur S4 (une classe mutable à un champ lue comme
  un identifiant).
- **Rappel de doctrine pour le solveur** : `PERSISTENCE_MODEL` ne contribue à
  aucune décision de kind (D7) ; il n'alimente qu'un finding. Les fixtures
  des règles S3 utilisent des noms non conventionnels (doc 10 §2.3) pour que
  les tests restent valides quelle que soit l'issue du lot 23.
- **Ce que le solveur consomme, sans avoir à relire `hexaglue-knowledge`** :
  `KnowledgePacks.embedded()` → `FrameworkKnowledge` ;
  `factsFor(CodeModel, TypeNode)` → `List<KnowledgeFinding>` en ordre de pack
  puis d'entrée ; un finding porte `fact()`, `packId()`, `symbol()`,
  `declaredKind()` et `capture("subject"|"id")` ; `KnowledgeFact.tier()` donne
  déjà le palier d'évidence (S1 pour `DECLARED_KIND`, S2 pour le reste), donc
  le moteur n'a aucun palier à coder en dur.
- **Rappels d'exécution** : le corpus profil 1 tourne deux fois dans
  `hexaglue-acceptance` — le cliquet (`CorpusScoreboardTest`, les claims relues)
  et les goldens (`Profile1GoldenTest`, le modèle entier), les deux sur le même
  `AnalysisChain` enregistré par ServiceLoader ; **enregistrer un golden est une
  exécution qui se déclare** (`-Dhexaglue.golden.regenerate=true`), sinon un
  golden absent est un échec ; le contrôle « zéro warning » se fait sur
  `make ci`, jamais sur un `verify` incrémental ; règles de conduite 10-13
  (carrière en lecture seule, transplantation avec tests, périmètre gelé,
  clôture de jalon).

### Ancien plan en phases (caduc depuis D12, conservé pour trace)

| Phase | Contenu | Statut | Notes |
|---|---|---|---|
| 0 | Correction des bugs B1-B15 | À FAIRE | Ordre suggéré : B1, B2, B7, B6, B5, B4, B3, puis le reste |
| 1 | Purge du code mort (~20 900 lignes, [01-code-mort.md](01-code-mort.md)) | À FAIRE | Débloquée par D2 (hexaglue-testing supprimé) et D3 (retrait direct). Inclut hexaglue-testing et hexaglue-syntax-spoon (D1 option B) |
| 2 | NamingVocabulary + IdentityFieldPolicy + évidences + config YAML | À FAIRE | Après phase 1. Cadrage fonctionnel précisé par [06-classification-metier.md](06-classification-metier.md) (§8) : NamingVocabulary = capteur S6, + FrameworkKnowledge |
| 3 | Typage du pivot classification→modèle | À FAIRE | Après phase 2. Le pivot doit transporter la trace complète (06-classification-metier.md, §5.1) |
| 4 | Frontières de modules (ArchitectureQuery sur le modèle, deps plugins, DTO vers arch) | À FAIRE | D1 = Option B : PAS de promotion de core.frontend ; la « cible » du doc 03 (option A) est amendée par DECISIONS.md. Spoon confiné à core/frontend (ArchUnit), downcasts C2 résorbés via méthodes d'interface |
| 5 | God classes + hexaglue-plugin-commons | À FAIRE | En dernier |
| G | Garde-fous build (enforcer, ArchUnit, dependency:analyze, PMD, doclint, SpotBugs bloquant) | À FAIRE | À installer au fil des phases, chaque garde-fou dès que son invariant devient vrai |

## Points en attente (hors décisions formelles)

- Les rapports d'audit du case study e-commerce (hexaglue-site) sont faussés par
  B1 (score dependency) et B8 (bounded contexts) : à régénérer après le jalon
  M8 (gate de parité — écarts B1/B8 attendus et documentés), et les textes des
  étapes step-0 à step-6 à relire en conséquence.
- Versions : reactor en 6.1.1-SNAPSHOT / plugins 3.1.1 au moment de l'audit.
  Cible : 7.0.0 (D4) ; la 6.1.1 ne sera jamais releasée.
- ~~**Canal de diagnostics du frontend**~~ — **soldé à M5** (D20 pour la
  lecture, D22 pour le périmètre de verdict) : les trois causes sont codées
  (hors périmètre HG-FRONTEND-004, code généré 005, récupération au parsing
  006) et une quatrième l'a rejointe (lu mais non classé, HG-ENGINE-003). La
  réserve tient toujours : c'est un diagnostic, jamais un verdict — si M6
  réclame un **inventaire du code écarté dans le rapport d'audit**, c'est D15
  qui se rouvre, et son prix est conservé (entrées de pack `GENERATED_CODE`).
- **Trois écarts de M5 à porter au gate de parité (M8)**, détaillés au journal
  de clôture : pas de rapport de validation en fichier, pas d'exclusion type
  par type (les globs de `classification-exclude` n'ont pas d'équivalent), pas
  de `skip-validation` séparé. Le deuxième est le seul qui puisse valoir une
  fonctionnalité plutôt qu'un écart.
- **Lecture hiérarchique de `hexaglue.yaml`** (document du module, puis racine
  du réacteur) : la carrière la faisait, M5 ne cherche le document qu'à côté du
  POM analysé. Suffisant en mono-module ; à reprendre avec les goals réacteur
  (M6/M7).
- **Défaut de forme du moteur** (constaté à M4, non corrigé) : les
  justifications des règles disent « is a AGGREGATE_ROOT » là où l'anglais veut
  « an ». Le corriger touche les règles et déplacerait les trois goldens de
  restitution — à faire dans un lot qui déplace déjà ces goldens.
- ~~**`Field.elementType` / `wrappedType` / `roles` jamais alimentés**~~ —
  **tranché à l'ouverture de M7 (D29)** : le moteur les remplit à
  l'assemblage, rôles relus des liens énoncés (doctrine `Links`), jamais du
  frontend. Porté par le lot 1 de M7a ; les 154 goldens y bougent une fois.

## Découvertes en cours de chantier

- 2026-08-01 — 17 faits nouveaux (absents de l'audit du 31/07) recensés avec
  fichier:ligne dans l'Annexe A de
  [06-classification-metier.md](06-classification-metier.md). Les plus notables :
  aucune arête du graphe vers les types externes (A1, `GraphBuilder:84-89`),
  types imbriqués jamais analysés (A2, `SpoonSemanticModel:56`), le goal
  `validate` ne valide jamais les ports (A3, `DefaultHexaGlueEngine:388-389`),
  `AuditMojo.failOnUnclassified` sans effet (A4), types sans verdict absents
  du modèle (A5), `DOMAIN_SERVICE` inatteignable (A6).
- 2026-08-02 (lot 16, **tranché au lot 20 — D15 CONFIRMÉE, option A**) — Deux
  doctrines contradictoires sur le code généré : le frontend livré (M2,
  `AnalysisPerimeter.covers`) l'écarte du modèle, le doc 09 prévoyait un
  UNCLASSIFIED catégorisé alimenté par le fait S2 `GENERATED_CODE`. **Le
  frontend garde la main** : reconnaître le code généré est une question de
  périmètre de lecture, réglée avant qu'un fait soit énoncé. L'option « verdict
  plein » a été écartée **par la mesure** (voir ci-dessous). Les quatre entrées
  de pack et `KnowledgeFact.GENERATED_CODE` sont conservées, commentées comme
  sans sujet possible : c'est le prix de l'option si M6 réclame l'inventaire.
  Le doc 09 est amendé, les trois scénarios de corpus deviennent la référence.
- 2026-08-02 (lot 20) — **Le code généré remis dans le périmètre détruit la
  lecture du code écrit à la main.** Sonde jetable sur le moteur livré : port +
  service à la main, puis la sortie du plugin JPA ajoutée au périmètre.
  L'adapter généré implémente le port ; R4 exige « rien du cœur ne l'implémente »
  donc le port tombe en UNCLASSIFIED, et R6 n'ayant plus de port sur quoi
  pivoter, le service applicatif tombe avec lui — l'adapter généré étant lu à sa
  place comme la couche applicative. **La seconde exécution sur des sources
  inchangées ne rend pas le même modèle.** **Réglé à M5 par D19** : un outil de build
  passe à l'analyse toutes ses racines de compilation,
  `target/generated-sources` compris, donc l'hôte ne lui en passe qu'une — la
  racine déclarée du projet — et le filtre `@Generated` du frontend redevient
  ce qu'il doit être, un filet pour du code généré commité dans les sources. Corollaire vérifié : aucune règle ne lit hors périmètre
  (toutes passent par `derivation.perimeter().types()`, seul `AssertKnowledge`
  balaie `code().types()` sans qu'aucun verdict n'en découle), et les stubs
  externes ne portent ni membres ni annotations (`TypeNode.externalStub`).
- 2026-08-03 (lot 21) — **`Field.elementType` n'est jamais alimenté par le
  frontend.** Un champ `List<Hull>` sort donc du snapshot en
  `cardinality: SINGLE`, et `CompositionIndex.referencesFrom` ne voit pas les
  identifiants portés par un élément de collection. L'information est présente
  (`TypeRef.typeArguments`, que les règles lisent via `unwrapElement`) : c'est le
  raccourci du modèle qui reste vide, avec `Field.wrappedType` et `Field.roles`.
  Écart M2 à combler dans `Members.fieldOf` ; les 143 goldens bougeront ce
  jour-là, et c'est le genre de diff que la garde « golden existant » est là pour
  faire relire.
- 2026-08-02 (lot 17) — **Les points d'entrée posés sur une méthode échappent à
  W1-DA.** Les sélecteurs de packs apparient une annotation portée **par le
  type** ; or `@KafkaListener`, `@JmsListener` et `@RabbitListener` se posent
  en pratique sur des méthodes d'une classe `@Component`. Le modèle porte déjà
  les annotations de méthode (`Method.annotations`), donc la matière est là :
  il manque un sélecteur « annotation sur un membre ». Sans effet sur le
  profil 1 ; à instruire quand le profil 2 (entreprise) arrivera au lot 22.
  **Levée au lot 22** : `Selector.MemberAnnotated` (clé `member-annotation`),
  qui répond sur méthode, constructeur ou champ ; les trois écoutes sont
  énoncées deux fois dans le pack `spring`, une entrée par placement.
- 2026-08-03 (lot 22) — **Une entité dont l'identité est un type de plateforme
  nu se lit VALUE_OBJECT.** Mesuré sur le profil 2 : `Pet`, possédé par
  `Owner`, porte `@Id Integer id` ; R3a exige « T porte un champ IDENTIFIER »,
  or `java.lang.Integer` est hors périmètre et ne reçoit aucun verdict, donc
  R3b s'applique. Le moteur est **conforme au doc 09** et la lecture est
  néanmoins fausse sur le domaine. Le seul canal qui pourrait la corriger est
  l'annotation de persistance, que D7 interdit de faire contribuer à un kind.
  Question pour le référentiel (pas un défaut d'implémentation) : lire `@Id`
  comme « quel champ porte l'identité » est-il une décision de kind au sens de
  D7, ou une lecture de structure que R3a pourrait consommer ? Non traité au
  lot 22, le scénario `Clinic-theWholeClinic` épingle l'état actuel et le dit
  en clair. **Portée au registre le 2026-08-03 comme D16 (PENDING)** : trois
  options instruites avec leurs coûts, dont le fait que la voie structurelle
  la plus évidente — la mutabilité — est déjà condamnée par le lot 20.
- 2026-08-03 (lot 22) — **La raison « rien du périmètre ne l'utilise » est
  rendue même quand un adapter nomme le type dans une signature.** Mesuré sur
  `Clinic-aStoreOnlyTheAdapterTouchesStaysOutsideTheDomain` : `InvoiceRow` est
  le type de retour d'une méthode de l'adapter, et sort en `UNKNOWN` avec cette
  raison. Le verdict est juste (nommer un type dans une signature n'est pas un
  contexte d'usage lisible), la phrase l'est moins. Formulation à revoir quand
  M6 rédigera les remédiations.
- 2026-08-03 (lot 22) — **Le lien d'identité et le verdict d'identité ne
  viennent pas de la même source, et cela se voit sur un identifiant externe.**
  Profil 2 : `JpaRepository<Owner, Integer>` fait énoncer `IDENTIFIED_BY` par
  R1 depuis la déclaration, donc l'agrégat nomme son champ d'identité bien que
  `Integer` n'ait aucun verdict. Profil 3 : `Convoys.find(UUID)` ne produit
  rien, R2 exigeant une valeur du périmètre. Les deux comportements sont
  cohérents avec le doc 09 ; l'asymétrie est à connaître avant de lire un
  rapport.

- 2026-08-03 (M5, lot du plugin Maven) — **Le périmètre est lu à deux endroits, et
  pas de la même façon.** `AnalysisPerimeter` (frontend) filtre sur
  `includePackages`/`excludePackages` ; `Perimeter` (moteur) filtre en plus sur
  `basePackage`. Conséquence mesurée par un test : avec le seul `basePackage`
  — le paramètre que tout hôte règle — un type hors de ce package est **lu** par
  le frontend, n'entre **pas** dans l'`ArchModel`, et **aucun diagnostic ne le
  dit**. C'est exactement la dette D20, un étage plus loin, et le canal du
  frontend ne peut pas la combler : de son point de vue le type a été lu.
  Le comportement lui-même est délibéré et ne doit pas changer (le contexte hors
  périmètre nourrit la dérivation, et les 154 goldens en dépendent) ; ce qui
  manque est de le **dire**. À instruire avant la clôture de M5 : soit le moteur
  rend ses propres diagnostics, soit l'hôte compte la différence — et le second
  frôle l'interdit 07 §10.5.

## Découvertes techniques (outillage)

- 2026-08-02 — **Spoon modélise un paramètre de type comme un `CtType`** :
  `CtTypeParameter extends CtType`, donc `getElements(TypeFilter<CtType>)` renvoie
  le `T` de `Box<T>` comme un type à analyser. Sans filtre explicite, `T` devient
  un nœud du modèle et reçoit une arête `DECLARES` depuis `Box`. Filtré dans
  `SpoonFrontend.isNamedDeclaration` (avec les classes anonymes et locales).
- 2026-08-02 — **Le parsing tolérant récupère silencieusement des erreurs de
  syntaxe** : avec `noClasspath(true)`, un fichier invalide produit 0 erreur et
  3 warnings côté Spoon, et le type est quand même construit (partiel).
  `setIgnoreSyntaxErrors(false)` n'y change rien. Conséquence : « échec bruyant »
  ne peut pas s'appuyer sur une exception de parsing ; ce qui échoue vraiment,
  ce sont les entrées illisibles (racine absente, entrée de classpath absente,
  niveau de langage non supporté). La remontée des récupérations partielles
  demande un canal de diagnostics sur le résultat du frontend. **Fait à M5**
  (D20) : `FrontendResult` porte HG-FRONTEND-006, et le seuil a été **mesuré**
  avant d'être écrit — le compteur de Spoon vaut 0 sur des sources propres, 0
  sur des références non résolues (le régime normal sans classpath complet) et
  3 sur un fichier cassé, donc le signal ne se déclenche que sur une vraie
  récupération.
- 2026-08-02 — **La règle ArchUnit `SPOON_CONFINED_TO_FRONTEND` est vacante
  entre modules** : elle s'exécute dans `hexaglue-testkit`, dont le classpath de
  test ne contient pas `hexaglue-frontend`. L'invariant reste tenu par
  l'enforcer (`bannedDependencies` : un module sans la dépendance ne peut pas
  utiliser Spoon), qui en est la forme forte. Un module agrégateur de tests
  d'architecture (dépendant de tous les modules) sera à poser quand le réacteur
  en comptera davantage. **Devenu concret à M5** : l'hôte doit faire tourner le
  frontend, donc le parser lui arrive transitivement et le bannissement y a été
  scindé (Lombok banni transitivement, parser interdit **en dépendance
  déclarée**). L'invariant réel — personne n'écrit `import spoon.*` hors du
  frontend — n'est donc plus tenu que par la déclaration, et c'est exactement ce
  qu'une règle ArchUnit d'un module agrégateur vérifierait.
- 2026-08-02 — **Un `make verify` incrémental ne prouve rien sur les warnings
  de compilation** : si seules les sources de test ont changé, le module
  principal n'est pas recompilé et ses warnings Error Prone n'apparaissent
  plus dans la sortie. Un warning `IdentityHashMapUsage` a survécu ainsi à une
  clôture de jalon. **Le contrôle « zéro warning » d'une clôture doit se faire
  sur `make ci` (clean + verify)**, jamais sur un `verify` incrémental.
- 2026-08-02 — **`spoon-core` tire `org.apache.maven:maven-model` et
  `maven-invoker`** (pour son `MavenLauncher`, non utilisé : le frontend reçoit
  ses racines et son classpath de son hôte). Exclus au POM, ce qui préserve le
  sens du bannissement des API Maven hors du maven-plugin.
- 2026-08-02 — **Un deux-points dans une valeur YAML non quotée casse le
  document** : `description: Libraries every classpath carries: code
  generators…` a fait échouer le pack `platform` sur « mapping values are not
  allowed here ». Rien de subtil, mais l'échec se présente sous forme d'un
  `ExceptionInInitializerError` sur la constante `KnowledgePacks.embedded()`,
  donc en `NoClassDefFound` sur *tous* les tests de la classe : le vrai
  message n'est lisible que dans le XML surefire. La prose d'un pack ne doit
  pas contenir de `:` non quoté.
- 2026-08-02 — **Quatre règles qualité qui se déclenchent systématiquement sur
  du code neuf**, notées pour ne pas les redécouvrir : PMD `UseVarargs` (un
  paramètre tableau en dernière position, même privé) ; PMD `LinguisticNaming`
  (une méthode de test nommée `asXxx` est lue comme une transformation qui
  doit retourner une valeur) ; PMD `UseProperClassLoader`
  (`getClass().getClassLoader()` — la sortie correcte pour un module qui
  embarque ses données est `Xxx.class.getResourceAsStream("/…")`, la ressource
  voyageant alors avec la classe) ; Error Prone `ImmutableEnumChecker` (un
  champ `List` dans une enum, fût-il issu de `List.copyOf`, exige un
  `@SuppressWarnings` commenté).
- 2026-08-02 — **Un `Set.of(...)` imprimé dans un message n'est pas
  déterministe** : l'ordre d'itération d'un `Set` immuable du JDK n'est pas
  spécifié et varie d'une JVM à l'autre. Les messages d'erreur du chargeur de
  packs énumèrent des clés connues ; ils s'appuient donc sur des `List`
  triées, pas sur des `Set`. À garder en tête partout où l'invariant de
  déterminisme (doc 07 §1) touche du texte rendu.

## Journal de sessions

### 2026-07-31 — Session d'audit et de cadrage
- Audit complet du code réalisé (5 analyses parallèles) : README + 01 à 05.
- Mise en place du dispositif anti-dérive : ce fichier, DECISIONS.md, section
  chantier dans CLAUDE.md, `.claude/settings.json` (deny + hook
  block-release-commands.sh), purge des mémoires obsolètes.
- Aucune modification du code source.
- Décisions D1 (option B), D2 (suppression hexaglue-testing), D3 (retrait
  direct), D4 (7.0.0) confirmées par l'utilisateur et consignées dans
  DECISIONS.md. Toutes les phases sont débloquées.
- Prochaine étape : démarrer la Phase 0 (B1 en premier).

### 2026-08-01 — Analyse fonctionnelle classification et propagation
- Nouvelle analyse demandée par l'utilisateur : le fonctionnement métier
  (algorithmes de classification, résolution de la convention par nommage,
  propagation jusqu'aux plugins audit/validate/génération), non couvert par
  l'audit du 31/07 (qualité de code).
- 4 explorations parallèles du code vivant ; livrable :
  [06-classification-metier.md](06-classification-metier.md) (constat en 3 verrous,
  cible « évidences hiérarchisées S1-S6 + propagation par point fixe +
  FrameworkKnowledge + trace propagée », prérequis graphe G1-G5, contrat aval
  validate/audit/génération, corpus d'acceptation à 3 profils).
- 17 faits nouveaux consignés (Annexe A + section Découvertes ci-dessus).
- 6 décisions ouvertes consignées PENDING dans DECISIONS.md (D6-D11) — aucune
  action tant qu'elles ne sont pas confirmées.
- Second livrable (demande utilisateur) :
  [07-architecture-page-blanche.md](07-architecture-page-blanche.md) —
  exercice page blanche (architecture en pipeline de compilateur, moteur
  d'inférence à saturation avec preuves, packs de connaissance frameworks,
  sinks typés, choix d'outillage). Conclusion : cible de convergence pour le
  chantier, réécriture totale non recommandée (§11). Ne crée aucune décision
  nouvelle ; éclaire D6 (option A) à D11.
- Aucune modification du code source. Tests non concernés.
- Prochaine étape : inchangée (Phase 0, B1 en premier) ; trancher D6-D11
  avant tout engagement sur la « Phase 6 » proposée.

### 2026-08-01 — Décision de stratégie : réécriture ancrée (D12)
- L'utilisateur retient la **réécriture ancrée** contre la migration
  progressive : nouveau réacteur `hexaglue-next/` construit selon le doc 07,
  ancien réacteur `hexaglue/` gelé en carrière lecture seule, gate de parité
  avant la 7.0.0.
- DECISIONS.md : D12 consignée CONFIRMÉE ; D6/D9/D11 confirmées par voie de
  conséquence ; D1/D2/D3 caduques (opérations sur l'ancien réacteur) ;
  D7/D8/D10 restent PENDING (à trancher au plus tard à M3).
- Plan de construction rédigé :
  [08-plan-reecriture-ancree.md](08-plan-reecriture-ancree.md) (jalons
  M0-M8, liste de récolte, B1-B15 → tests de régression, gate de parité,
  risques/parades).
- CHANTIER.md réorienté : plan à jalons M0-M8 (l'ancien plan en phases est
  conservé pour trace, caduc), règles de conduite 10-13 ajoutées, sources de
  vérité étendues aux docs 07/08. Mémoire projet mise à jour.
- Aucune modification du code source.
- Prochaine étape : démarrer **M0** (socle du réacteur `hexaglue-next/` :
  POM parent, CI, qualité, testkit + import du corpus profil 1).
- Fin de session sur la planification : aucun travail de M0 entamé,
  `hexaglue-next/` n'existe pas encore. Reprise directe sur M0 à la
  prochaine session.

### 2026-08-01 — Jalon M0 : socle du réacteur `hexaglue-next/` (FAIT)
- Réacteur créé : dépôt git indépendant (branche `main`, 3 commits — socle /
  harnais testkit / corpus), POM parent `io.hexaglue:hexaglue-parent:7.0.0-SNAPSHOT`
  (cible Java 17, build JDK 21+, mvnvm épinglé 3.9.9), module `build/tools`
  (configs qualité, récolte R), module `hexaglue-testkit`.
- Garde-fous actifs dès le premier commit (tous absents ou non bloquants dans
  la carrière) : enforcer (requireMaven 3.9+/Java 21+, bannedDependencies —
  Spoon, Maven APIs, Lombok bannis partout, dérogations par module prévues
  aux jalons M2/M5), Error Prone 2.50.0 + NullAway 0.13.8 (JSpecify,
  NullAway coupé sur les sources de test ; couvre le contrôle des chaînes de
  format au socle), Spotless/Palantir + header MPL, checkstyle,
  **PMD bloquant** (`failOnViolation=true`), SpotBugs, JaCoCo, PIT configuré,
  ArchUnit auto-application (3 règles `allowEmptyShould` : Spoon confiné au
  futur frontend, Maven confiné au futur maven-plugin, testkit agnostique du
  moteur). CI GitHub Actions (build+format-check, quality) + CodeQL +
  dependabot portés.
- Harnais golden transplanté **découplé** de l'ancien moteur : `GoldenFiles`
  (compare-ou-crée, régénération = suppression + relance), `Determinism`,
  `SourceFixtures`, `Corpus`/`CorpusScenario` (chargeur de ressources
  classpath), `AnalysisRunner` (interface ServiceLoader — le moteur s'y
  enregistrera au jalon M3). `ArchModelSnapshotSerializer` NON transplanté à
  M0 (couplé au modèle v7 inexistant) : reporté à M1.
- Corpus profil 1 : **122 scénarios / 208 fichiers sources** extraits par
  script des text blocks inline de la carrière (21 classes de test
  classification + `GoldenFileTest`), stubs `@Generated` ré-expansés à la
  main (3 scénarios, template `.formatted()` non extractible), 3
  `legacy-golden.json` joints aux scénarios GoldenFileTest comme référence
  pour le gate de parité. `Profile1CorpusTest` : 122 tests exécutables,
  skipped tant qu'aucun `AnalysisRunner` n'est présent (critère « rouge
  autorisé » satisfait sans casser le build).
- État final : `make verify` VERT — 139 tests (0 échec, 122 skipped),
  0 violation checkstyle, PMD/SpotBugs OK, formatage vérifié.
- Ajustements vs carrière (consignés) : exclusion PMD
  `UnitTestContainsTooManyAsserts` (idiome AssertJ multi-assertions) ;
  `jacoco:check` avec seuil non installé à M0 (à poser avec le premier code
  de production réel, M1) ; cibles release/doc du Makefile non portées (gel
  D5, pipeline doc à M8).
- Revue interdits doc 07 §10 : RAS. `AnalysisRunner` n'est pas une
  abstraction spéculative : frontière de service consommée par
  `Profile1CorpusTest`, nécessaire au découplage testkit/moteur.
- Alignement versions (demande utilisateur, 4e commit) : tout le socle est
  porté aux dernières stables vérifiées sur Maven Central — JUnit 6.1.2
  (compatibilité ArchUnit 1.4.2 et PIT vérifiée empiriquement : 139 tests
  découverts, mutation OK), Spotless 3.9.0 + Palantir 2.96.0,
  Checkstyle 13.9.0, SpotBugs plugin 4.10.3.0, PIT 1.25.8, JaCoCo 0.8.15,
  surefire 3.5.6, jar 3.5.1, JSpecify 1.0.1. Écartées : pré-versions
  (compiler 4.0.0-beta, JUnit-surefire 3.6.0-M1, slf4j 2.1.0-alpha,
  AssertJ 4.0.0-M1). `mvn clean compile` vérifié explicitement.
- Correctif Javadoc (5e commit) : les `@throws` déplacés sur la Javadoc des
  records (contournement du faux positif PMD `DanglingJavadoc` sur les
  constructeurs compacts) déclenchaient `InvalidBlockTag` d'Error Prone ;
  validation désormais décrite en prose. Compilation sans aucun warning.
- État final du dépôt : 5 commits, arbre propre, `make verify` vert.
- Prochaine étape : **M1** — `hexaglue-model` (contrat pur : CodeModel,
  ArchModel, Classification avec trace complète, Finding/Diagnostic, config
  typée ; récolte des records de `hexaglue-arch`, des `AnnotationValue` de
  syntax, et du sérialiseur de snapshot).

### 2026-08-01 — Remise au vert de la CI de `hexaglue-next` (hors jalon)

Session courte consacrée aux workflows GitHub, sans toucher au code de
production. Point de départ : CI verte, mais **CodeQL en échec sur 100 % des
runs** depuis la création du dépôt (push sur `main` et 4 PR Dependabot).

- Le diagnostic s'est révélé en trois couches, chacune masquant la suivante :
  1. `actions: read` manquant dans `codeql.yml` — `codeql-action/analyze` lit
     le run pour y rattacher le SARIF. Erreur affichée : `Resource not
     accessible by integration`. Corrigé, mais insuffisant.
  2. Une fois la permission posée, l'erreur réelle apparaît : `Advanced
     Security must be enabled for this repository to use code scanning`.
     L'organisation est en plan **free** : le code scanning est indisponible
     sur dépôt privé. C'était le blocage de fond, invisible tant que la
     couche 1 renvoyait une erreur générique.
  3. Bruit Dependabot indépendant : deux PR ne montaient aucune version, elles
     remplaçaient les tags majeurs flottants (`@v4`, `@v5`) par des tags de
     patch figés — perte des correctifs automatiques et une PR par release de
     patch.
- **`hexaglue-next` est désormais PUBLIC** (décision utilisateur). Publication
  faite après vérification qu'aucun secret ne figurait ni dans l'arbre ni dans
  les 7 commits d'historique, et qu'aucun fichier supprimé ne pouvait
  resurgir. Conséquence pour la suite du chantier : plus de contrainte GHAS,
  code scanning gratuit. L'ancien réacteur `hexaglue/` était déjà public, d'où
  son CodeQL vert de longue date — la différence n'était pas de configuration
  mais de visibilité.
- `dependabot.yml` : groupe `actions` + règle `ignore` limitant les mises à
  jour d'actions aux sauts de majeure (cohérent avec l'épinglage sur tag
  majeur flottant en place partout). Les deux PR de figement se sont fermées
  d'elles-mêmes dès la prise en compte de la config.
- Historique : merge commits **désactivés** côté dépôt (squash et rebase
  seuls), titre de squash = titre de PR, corps vide, suppression de branche
  automatique. `main` est linéaire et le restera.
- PR mergées en squash : slf4j 2.0.17 → 2.0.18, `actions/checkout` v6 → v7.
- Baseline de couverture assainie via un fichier de config CodeQL dédié
  (`.github/codeql/codeql-config.yml`, référencé par `config-file`) :
  `6 out of 221` → `6 out of 13` → **`6 out of 6`**. Le numérateur n'a jamais
  bougé — l'analyse couvrait déjà tout le code de production ; c'était la
  mesure qui mentait. Exclus : les 208 fixtures du corpus sous
  `src/main/resources/corpus/`, les sources de test, et
  `docs/licence/header.java` (gabarit d'en-tête MPL portant l'extension
  `.java`). Le glob est volontairement étroit : un `**/corpus/**` aurait aussi
  écarté `io.hexaglue.testkit.corpus`, qui est du vrai code.
- **Couplage à surveiller** : l'exclusion de `**/src/test/java/**` n'est
  cohérente que tant que l'étape de scan reste `mvn compile`. Un passage à
  `test-compile` compilerait les tests tout en les écartant silencieusement de
  l'analyse. Le lien est consigné en commentaire dans le fichier de config.
- État final : aucune PR ouverte, tous les workflows verts, arbre propre.
- La prochaine étape reste **M1** (`hexaglue-model`), inchangée : cette
  session n'a touché ni au code ni au plan.

### 2026-08-01 — Jalon M1 : `hexaglue-model`, lots 1-4 (EN COURS)

Module `hexaglue-model` créé dans `hexaglue-next/` (zéro dépendance compile,
pas même JSpecify — NullAway couvre déjà `io.hexaglue` via AnnotatedPackages,
absence exprimée par `Optional`). Le jalon a été découpé en 8 lots + clôture ;
les lots 1-4 sont faits, un commit chacun :

- **Lot 1 (f03c062)** — bootstrap + vocabulaire de base : `TypeId` (identité
  stable, convention `$` pour les nested, `packageName()` corrigé vs carrière),
  `SourceLocation` (validée : lignes ≥ 1), **`TypeRef` unique récursif**
  scellé (Named/Primitive/Array/Wildcard/TypeVariable — wildcard distinct
  d'une variable de type, fusion des deux TypeRef de la carrière, interdit
  07 §10.6), `Modifier`, `TypeNature`. Garde-fous nouveaux posés au même
  lot : **doclint strict** sur tout le reactor (javadoc:jar à verify,
  failOnWarnings), **jacoco:check ≥ 80 % lignes** (testkit mesuré à 84,5 %
  avant pose), cible `mutation` du Makefile étendue au module.
- **Lot 2 (0f3f8d9)** — CodeModel (07 §3.1) : records de déclaration
  **partagés** code/arch (`Field`/`Method`/`Constructor`/`Parameter`/
  `Annotation` — une seule implémentation par concept ; rôles sémantiques
  vides côté frontend, remplis par le moteur), `AnnotationValue` scellée
  récoltée de syntax-api (une seule représentation typée, B6 corrigé par
  construction ; la carrière portait values + typedValues en double),
  `TypeNode` (nested + **stubs externes sans membres**, validé), arêtes
  typées avec provenance (`memberName`/`parameterIndex`/`typeArgumentIndex`),
  fermeture transitive de supertypes stockée en donnée, faits de corps
  derrière la capacité `METHOD_BODIES` (présents sans capacité = erreur),
  conteneur `CodeModel` (itération en ordre d'identité via TreeMap, id
  dupliqué = échec bruyant).
- **Lot 3 (92398d2)** — contrat de classification (07 §3.2, 06 §5.1) :
  **`Confidence` unique** (EXPLICIT/HIGH/MEDIUM/LOW, récolte de
  `ir/ConfidenceLevel`), `Basis` (DECLARED/INFERRED), `EvidenceTier` S1-S6
  avec **plafonds de confiance appliqués à la construction** (une évidence
  NAMING ne peut pas réclamer HIGH — doctrine 06 §3.1 outillée), `Evidence`,
  `Candidate` (conservés si ambigu), `RuleId`, `ProofNode` (arbre règle +
  prémisses ; fait de base sans règle = feuille), `Classification`
  (direction seulement sur les kinds ports, validé), remédiation typée
  récoltée (`RemediationHint`/`Action`/`Impact`). `ArchKind` et
  `PortDirection` placés à la racine du modèle (évite un cycle de packages
  arch↔classification) ; **sans les adapters** (D8 PENDING, règle 1).
- **Lot 4 (be356f1)** — hiérarchie `ArchType` scellée récoltée (Domain/
  Port/Application/Unclassified avec ses 6 catégories) + `TypeStructure`
  (mêmes records de déclaration que le CodeModel) + `UseCase`/`Invariant`/
  `DrivenPortType`. Invariant nouveau : **cohérence kind↔record**
  (`KindCoherence`) — un verdict ENTITY dans un record ValueObject est
  rejeté. `UnclassifiedType.reason` passe en `Optional<String>`.
  `PortType.direction()` exposé.
- Ajustements garde-fous consignés : exclusions PMD `DanglingJavadoc` (faux
  positif sur les constructeurs compacts, en conflit frontal avec doclint
  qui exige leur Javadoc — doclint gagne) et `UseConcurrentHashMap`
  (bruit : signale toute instanciation de Map) ; exclusion SpotBugs
  `EI_EXPOSE_REP/REP2` pour `io.hexaglue.model` (vues `unmodifiable*` à
  ordre d'itération déterministe, non prouvables par SpotBugs — `Set.copyOf`
  ne garantit pas l'ordre). `EnumSets.ordered` : copies d'ensembles d'enums
  en ordre naturel (déterminisme par construction).
- État : `make verify` VERT (275 tests : 136 model + 139 testkit, 0 échec),
  doclint strict OK, mutation **92 %** (326 mutants, PIT).
- **Reste à faire M1** (repris en prochaine session) :
  - Lot 5 : indexes (`DomainIndex`, `PortIndex`, `CompositionIndex`,
    `ModuleTopology` — récolte `ModuleIndex`/`ModuleDescriptor`/`ModuleRole`
    de `arch/model/index/`) + conteneur `ArchModel` avec `classificationOf`
    et `explain` (07 §6.1) ; tout type du périmètre a un verdict (A5).
  - Lot 6 : `Finding` codés (HG-xxxx, severity, subject, locations,
    evidences, remediation) + `Diagnostic` pour les échecs de l'outil
    (07 §3.3).
  - Lot 7 : config typée stricte — records de forme seulement (périmètre
    d'analyse, portes validate : failOnUnclassified/minConfidence/
    failOnAmbiguous/allowInferred/seuils par code de finding,
    `generation.minConfidence`) ; le binding YAML strict reste à M5.
  - Lot 8 : transplantation de l'`ArchModelSnapshotSerializer` (carrière :
    classe interne de `hexaglue-core/src/test/.../GoldenFileTest.java`,
    lignes ~578-835) adaptée au modèle v7, dans le testkit + dépendance
    testkit→hexaglue-model (autorisée par la règle ArchUnit du testkit).
  - Clôture : `make verify` complet, revue contre 07 §3 et interdits §10
    (noter : `UseCase`/`Invariant`/champs enrichis = contrat à remplir par
    le moteur à M3, comme la trace), journal.

### 2026-08-01 — Jalon M1 : lot 5, indexes + conteneur ArchModel (FAIT)

- **Lot 5 (f6aafe5)** — conteneur `ArchModel` (07 §6.1) + 4 indexes, le tout
  dans `io.hexaglue.model.arch` (pas de sous-package `index` : la carrière
  avait un cycle de packages model↔index, même doctrine que le placement
  d'`ArchKind` au lot 3).
  - `ArchModel` : types en ordre d'identité (TreeMap, id dupliqué = échec
    bruyant, même contrat que `CodeModel`), `type(id)`, `all(Class<T>)`
    (accès idiomatique plugins, les branches scellées `DomainType`/`PortType`
    matchent aussi), **`classificationOf(id)` et `explain(id)`** (07 §6.1,
    explain = arbre de preuve du verdict). A5 est constructif : tout type
    présent porte un verdict par construction (fallback UnclassifiedType
    compris) ; la couverture du périmètre (tout type du CodeModel en scope a
    son ArchType) sera un invariant du moteur à M3.
  - `DomainIndex`/`PortIndex` : récolte adaptée (streams par kind en ordre
    d'identité, `aggregateRoot(id)`, `entitiesOf`/`valueObjectsOf` résolus
    depuis la composition, `repositoryFor(aggregateId)` via
    `managedAggregate`). `portsImplementedBy` de la carrière **non récolté**
    (filtrait les interfaces DU port, sémantique fausse).
  - `CompositionIndex` : navigation sur les faits décidés uniquement (zéro
    inférence) — embedded*/identifierOf/aggregateOf depuis les records
    enrichis ; références croisées = champ (ou élément de collection) typé
    identifiant d'un autre agrégat, exclusions : identité propre, entité →
    agrégat propriétaire. `AggregateReference` sans champ nullable (vs
    carrière).
  - `ModuleTopology` (+ `ModuleDescriptor`, `ModuleRole`) : récolte de
    `ModuleIndex`/`ModuleDescriptor`/`ModuleRole` **sans `Path`** — le modèle
    ne porte aucune préoccupation d'E/S ; le routage physique (baseDir,
    sourceRoots) ira à la config des sinks à M5. Validations : nom de module
    dupliqué, type affecté deux fois, affectation vers module inconnu =
    échecs bruyants ; `empty()` pour le mono-module (les accesseurs du
    modèle ne rendent pas d'`Optional` de conteneur, contrairement à la
    carrière).
- État : `make verify` VERT (341 tests : 202 model + 139 testkit dont 122
  corpus skipped, 0 violation), doclint strict OK, PIT model **94 %**
  (417 mutants, aucun survivant dans les classes du lot ; le rapport à 73 %
  vu en console est celui du testkit, préexistant).
- Prochaine étape : **lot 6** — `Finding` codés (HG-xxxx, severity, subject,
  locations, evidences, remediation) + `Diagnostic` (07 §3.3).

### 2026-08-01 — Jalon M1 : lot 6, Finding + Diagnostic (FAIT)

- **Lot 6 (73e03a0)** — nouveau package `io.hexaglue.model.finding`
  (07 §3.3) :
  - `IssueCode` : identifiant publié **partagé findings/diagnostics** — le
    doc 07 code les deux en HG-xxxx et le §10.6 interdit deux
    implémentations d'un même concept ; un seul catalogue documenté. Forme
    canonique **`HG-CATEGORY-NNN`** (catégorie en majuscules, numéro à
    3 chiffres, ex. HG-DDD-012) validée à la construction — plus stricte que
    le « HG-xxxx » générique du doc, calée sur son seul exemple concret.
    Comparable par valeur (ordre déterministe des rapports).
  - `Finding` (record + builder) : code, `Severity` (BLOCKER/CRITICAL/
    MAJOR/MINOR/INFO, récolte de la sémantique du plugin audit), message,
    **subject typé `TypeId`** + relatedTypes, locations, et — modèle unique
    audit/validation — les `Evidence` et `RemediationHint` **du package
    classification réutilisés** (pas de second modèle d'évidence, vs
    carrière qui en avait 4 : Structural/Relationship/Dependency/
    Behavioral).
  - `Diagnostic` (record + builder) : l'outil se rapporte lui-même (échec
    d'analyse, génération refusée sous le seuil — 07 §6.4) avec
    `DiagnosticSeverity` ERROR/WARNING/INFO (échelle compilateur, distincte
    de la gate des findings), subject/location optionnels, remédiations
    typées. Concrétise « échec bruyant, jamais de modèle vide silencieux »
    (07 §2.3, B5/04-H6).
  - Non récolté du plugin audit : Violation/ConstraintId (format `cat:name`
    non conforme HG-xxxx), les 4 types d'évidences, Recommendation — le
    savoir-faire des règles sera transplanté à M6 sur ce contrat-ci.
- État : `make verify` VERT (359 tests : 220 model + 139 testkit), PIT
  model **94 %** (433 mutants, aucun survivant dans le package finding).
- Prochaine étape : **lot 7** — config typée stricte (records de forme :
  périmètre d'analyse, portes validate failOnUnclassified/minConfidence/
  failOnAmbiguous/allowInferred/seuils par code de finding,
  generation.minConfidence) ; le binding YAML strict reste à M5.

### 2026-08-01 — Jalon M1 : lot 7, config typée stricte (FAIT)

- **Lot 7 (74a6c1a)** — nouveau package `io.hexaglue.model.config` : records
  de **forme seulement** (zéro comportement ; l'évaluation des portes ira au
  moteur, le binding YAML strict — clé inconnue = erreur, snakeyaml-engine —
  à M5) :
  - `AnalysisScope` : basePackage optionnel (ancrage des lectures relatives,
    leçon B8) + préfixes include/exclude. **Préfixes de packages stricts** :
    un glob (`*.util.*` comme dans la carrière) est rejeté à la construction
    — doctrine « FQN exact ou préfixe de package, jamais par nom simple »
    (07 §5, B3/C3).
  - `ValidationConfig` (07 §6.3) : failOnUnclassified, minConfidence (portes
    ports inclus), failOnAmbiguous, allowInferred, `findingThresholds`
    Map<IssueCode, Severity> **ordonnée par code** (déterminisme des
    rapports). **Défauts permissifs** (aucune porte armée, plancher LOW,
    inferred accepté) : armer une gate est un choix explicite — même
    posture que le validate actuel de la carrière, les valeurs du doc 07
    §6.3 sont un exemple de config armée, pas des défauts.
  - `GenerationConfig` (07 §6.4) : minConfidence, **défaut HIGH** (défaut
    explicite du doc) — sous le seuil, le plugin générateur émettra un
    Diagnostic + remédiation au lieu de code faux.
  - `HexaGlueConfig` : racine composant les trois blocs + `defaults()`
    (posture documentée : tout analyser, ne rien gater, générer à HIGH).
    Les options par plugin ne sont PAS ici : opaques au modèle, elles
    passeront par le SPI (`PluginConfig`, M5).
- État : `make verify` VERT (370 tests : 231 model + 139 testkit), PIT
  model **94 %** (450 mutants, aucun survivant dans le package config).
- Prochaine étape : **lot 8** — transplantation de
  l'`ArchModelSnapshotSerializer` (carrière : classe interne de
  `hexaglue-core/src/test/.../GoldenFileTest.java`, lignes ~578-835)
  adaptée au modèle v7, dans le testkit + dépendance testkit→hexaglue-model
  (autorisée par la règle ArchUnit du testkit). Puis clôture M1.

### 2026-08-01 — Jalon M1 : lot 8, sérialiseur de snapshot dans le testkit (FAIT)

- **Lot 8 (0c8784a)** — transplantation de l'`ArchModelSnapshotSerializer`
  de la carrière (classe interne de `GoldenFileTest.java`, l.578-835) vers
  `hexaglue-testkit`, adaptée au modèle v7 sous le nom **`ArchModelSnapshots`**
  (`serialize(ArchModel)`), + **première dépendance inter-modules du
  réacteur** : testkit→hexaglue-model (compile ; conforme à la règle ArchUnit
  `TESTKIT_STAYS_ENGINE_AGNOSTIC`, qui n'interdit que engine/frontend/
  knowledge).
  - Adaptations vs carrière consignées : sections `domain`/`application`/
    `ports`/`unclassified` (la carrière ne sérialisait pas les types
    application — invisible dans les goldens ; en v7 chaque branche scellée a
    sa section, rien d'absent silencieusement) ; `basis` ajouté à côté de
    `confidence` (DECLARED/INFERRED, l'apport de la trace v7) ; `simpleName`
    supprimé (dérivable), `construct` renommé `nature` (vocabulaire v7) ;
    le pseudo-kind `USE_CASE` des driving ports abandonné (`direction` +
    `portType` sur les seuls driven) ; ordre d'identité du modèle (plus de
    tri local), champs et méthodes triés par nom (comme la carrière).
  - Réécriture du style : le StringBuilder de la carrière violait le PMD
    bloquant du réacteur (ConsecutiveAppendsShouldReuse ×17) → rendu par
    listes de lignes jointes, sans lib JSON (testkit sans dépendance).
  - **Correctif Makefile lié** : la cible `mutation` invoquait le goal PIT
    dans une session Maven séparée ; avec la nouvelle dépendance
    inter-modules, le testkit ne résolvait plus hexaglue-model (jamais
    installé en repo local). Fusion en une seule invocation
    `mvn test ... mutationCoverage` : le ReactorReader sert
    `target/classes` du model compilé dans la même session.
  - Tests : golden inline exact (text block), modèle vide, échappement JSON,
    stabilité multi-exécutions via `Determinism.assertStable`.
- État : `make verify` VERT (374 tests : 231 model + 143 testkit), PIT model
  94 % (450 mutants), PIT testkit passe de 30 à **53 mutants (84 %)** — les
  23 mutants d'`ArchModelSnapshots` sont tous tués ; les 8 survivants sont
  les préexistants du harnais M0.
- **Clôture M1 volontairement NON faite** : l'utilisateur a annoncé des
  corrections à apporter avant la clôture. Reprise : appliquer les
  corrections, puis clôture (make verify complet, revue contre 07 §3 et
  interdits §10, tableau des jalons M1 → FAIT, journal).

### 2026-08-02 — Corrections pré-clôture M1 : warnings Error Prone (FAIT)

- Corrections demandées par l'utilisateur (5 warnings Error Prone au compile
  de hexaglue-model), commit **5ef73c1** :
  - `AvoidCommonTypeNames` : `AnnotationValue.ClassValue` renommé
    **`ClassRefValue`** (collision avec `java.lang.ClassValue`).
  - `EnumOrdinal` (×4) : `Confidence.isAtLeast` passe à `compareTo` ;
    nouveau prédicat **`Confidence.isStrongerThan`** (strictement plus fort,
    testé) ; le plafond d'`Evidence` s'exprime désormais
    `force.isStrongerThan(tier.maxConfidence())` — plus aucun `ordinal()`
    dans le réacteur.
- État : `make verify` VERT (375 tests : 232 model + 143 testkit),
  **0 warning bugpattern sur tout le build**, PIT model 94 % (453 mutants,
  les 3 nouveaux tués), testkit 84 %.
- Clôture M1 toujours en attente : d'autres corrections utilisateur
  peuvent suivre.

### 2026-08-02 — Clôture du jalon M1 (FAIT)

Clôture déroulée sur feu vert utilisateur, après ses corrections (5ef73c1).

- **Gate technique** : `make verify` VERT (375 tests : 232 model +
  143 testkit dont 122 corpus skipped — harnais vert, le corpus s'activera
  avec le moteur à M3), 0 violation checkstyle/PMD/SpotBugs, doclint strict,
  jacoco ≥ 80 %, 0 warning Error Prone, PIT 94 % model (453 mutants) /
  84 % testkit. Zéro dépendance compile de `hexaglue-model` vérifiée au POM
  (junit/assertj en test uniquement). Arbre propre, 9 commits.
- **Revue contre 07 §3** : CodeModel conforme (§3.1 — TypeRef unique
  récursif, stubs externes, arêtes avec provenance, fermeture de supertypes,
  faits de corps sous capacité) ; ArchModel conforme (§3.2 — ArchType
  scellé, TypeStructure, 4 indexes, Classification avec confiance unique,
  basis, évidences S1-S6 à plafonds, candidats conservés, arbre de preuve,
  A5 constructif) ; Finding/Diagnostic conformes (§3.3 — codés HG,
  localisés ; publication doc = M8) ; config typée stricte (§6.3/6.4,
  formes ; binding YAML = M5) ; module sans E/S ni logique, API
  classificationOf/explain présente. **Écart assumé unique** :
  `DRIVING_ADAPTER`/`DRIVEN_ADAPTER` absents d'ArchKind — suspendus à
  D8 (PENDING, à trancher au plus tard à M3) ; le §3.2 les prévoit.
- **Revue contre les interdits 07 §10** : RAS sur les 10 points. Points
  notables : aucun `catch` dans le module (échecs bruyants IAE) ; aucun
  pivot String ; unicité des concepts tenue (TypeRef, Confidence,
  Evidence/RemediationHint partagés classification/findings, IssueCode
  partagé findings/diagnostics, records de déclaration partagés code/arch) ;
  aucune règle de nommage ; navigation sans inférence dans les indexes.
- **Vigilance reportée à M3** (consignée, pas un défaut M1) :
  `UseCase`/`Invariant`/champs enrichis des ArchTypes et la trace complète
  sont un contrat que le moteur devra remplir ; la couverture du périmètre
  (tout type du CodeModel en scope a son ArchType) est un invariant moteur.
- Prochaine étape : **M2 — `hexaglue-frontend`** (Spoon + classpath, stubs
  externes, types imbriqués, valeurs d'annotations typées ; dérogation
  enforcer pour Spoon dans ce seul module).

### 2026-08-02 — Jalon M2 : `hexaglue-frontend` (FAIT)

Module `hexaglue-frontend` créé : Spoon 11.5.0 (dérogation enforcer, Maven
exclu de ses transitives) + ClassGraph 4.8.180, dépendance sur
`hexaglue-model` seul. **Aucune abstraction de frontend** : l'API publique est
`SpoonFrontend.analyze(FrontendRequest) → CodeModel`, la frontière est le
modèle lui-même (07 §2.2). Le jalon a été découpé en 7 lots + clôture, un
commit chacun (10 commits, af2f7bf…ccd0c20) :

- **Lot 1 (af2f7bf)** — socle + nœuds de types : `FrontendRequest` (racines,
  classpath, niveau de langage, périmètre, capacités), lecture des types
  **imbriqués compris** (G3 ; `Order$Line` avec `enclosingType`), natures,
  modificateurs, javadoc, **localisation relative à la racine de sources**
  (déterminisme des rapports entre checkouts), `TypeRef` récursif complet
  (wildcard distinct d'une variable de type, dimensions de tableaux),
  périmètre par préfixes de packages **stricts sur segments entiers**
  (`com.acme` ne capture pas `com.acmetools`) et exclusion du code généré par
  **FQN exact** de son annotation marqueur. Le `basePackage` seul ne restreint
  pas le périmètre (sémantique du modèle M1 : il ancre les lectures relatives ;
  la restriction est le rôle des include/exclude — l'hôte fera la traduction).
  Parsing **tolérant** assumé (`noClasspath`) : une référence non résolue garde
  le nom du source plutôt que de faire échouer l'analyse.
- **Lot 2 (4a6ad59)** — valeurs d'annotations typées (G2, B6 corrigé par
  construction) : String / primitif / constante d'enum / littéral de classe /
  annotation imbriquée / tableau, récursivement, **zéro stringification**.
  Les expressions constantes (`10 * 5`, concaténations) sont **évaluées** vers
  la valeur qu'elles dénotent ; une constante nommée résolue rend sa valeur.
  Une valeur illisible est laissée hors de la map avec un WARN (pas dégradée).
- **Lot 3 (eaca239)** — membres déclarés : champs (composants de record
  compris, **constantes d'enum exclues** — ce sont les valeurs du type, pas son
  état), méthodes (modificateur `default` restauré, exceptions, paramètres
  annotés), constructeurs. Membres **implicites exclus** (constructeur
  canonique et accesseurs d'un record). Ordre déterministe par signature sur
  les **types qualifiés** des paramètres (le parseur rend les exécutables dans
  un ordre non spécifié). Rôles sémantiques, `wrappedType` et `elementType`
  laissés vides : ce sont des conclusions du moteur (et `elementType` est déjà
  lisible sur le `TypeRef`).
- **Lot 4 (1e889fe)** — arêtes typées avec provenance + stubs externes (G1,
  **correction du verrou A1**) : EXTENDS / IMPLEMENTS / PERMITS / ANNOTATED_BY /
  DECLARES / FIELD_TYPE / RETURN_TYPE / PARAMETER_TYPE / THROWS_TYPE /
  TYPE_ARGUMENT, **sans aucun filtre de package**, avec membre porteur, index
  de paramètre et index d'argument de type. `extends JpaRepository<Order,
  OrderId>` produit désormais l'arête vers le stub **et** les deux
  TYPE_ARGUMENT indexés. Tout type référencé non analysé devient un stub sans
  membres, dont la forme Java est déduite de la relation qui le nomme
  (implémenté ⇒ interface, annotant ⇒ annotation). Ce qui n'a pas d'identité
  (primitifs, variables de type, wildcards) n'est jamais une cible ; un tableau
  vise son composant.
- **Lot 5 (45be4cd)** — fermeture transitive des supertypes (G4) : parcours
  combinant la hiérarchie des sources analysées et celle **lue en bytecode**
  (ClassGraph) sur les entrées de classpath, `java.lang.Object` exclu, cycle
  déclaré fermé sans boucler. Les stubs ont leur propre fermeture : c'est ce
  qui permettra d'énoncer la connaissance framework **une fois sur la racine**
  (`Repository`) au lieu de lister tous les dérivés. Testé avec une hiérarchie
  Spring Data réellement compilée par le compilateur système.
- **Lot 6 (5ba323c)** — faits de corps sous capacité `METHOD_BODIES`
  (A10 corrigé) : invocations, instanciations et complexité cyclomatique
  extraites en **une seule traversée AST par corps**, arêtes INVOKES /
  INSTANTIATES avec le membre d'origine (dédupliquées : une relation dit que la
  dépendance existe, pas combien de fois). Sans la capacité : aucun fait,
  aucune arête, complexité vide — le modèle dit ce qu'il a couru.
- **Lot 7 (1c5902a)** — échec bruyant codé (B5, 07 §2.3) : `FrontendException`
  portant un `Diagnostic` — HG-FRONTEND-001 (racine de sources illisible),
  002 (entrée de classpath absente), 003 (échec du parseur). Les entrées sont
  vérifiées **avant** tout travail : une racine absente produirait sinon un
  modèle plus petit, qui se lit comme une base de code plus petite et non
  comme une installation cassée.
- **Clôture (764626f, 9928b5e, ccd0c20)** — renforcement des tests sur les
  points faibles révélés par PIT (toutes les formes de branchement, bornes de
  wildcard, constantes nommées, javadoc sans prose), puis correction d'un
  warning Error Prone `IdentityHashMapUsage` : les complexités de corps sont
  désormais indexées **par signature** et non par identité d'élément analysé
  (comparer deux éléments du parseur revient à comparer deux arbres de
  syntaxe entiers). Vérifié en **build propre** (`make ci`), voir la
  découverte sur les builds incrémentaux ci-dessus.

- **Périmètre volontairement non couvert** (consigné, pas un oubli) :
  les **jeux de sources par module** (`ModuleNode`, `TypeNode.moduleName`) ne
  sont pas alimentés. Aucun hôte ne peut les fournir avant le maven-plugin, et
  les poser serait exactement l'interdit 07 §10.1 (publier une abstraction sans
  consommateur réel). **Reporté de M5 à M6 par l'amendement de D21** : le
  maven-plugin existe depuis M5, mais l'analyse est par module et rien ne lit
  de rôle — le consommateur réel est l'analyse réacteur, à M6.
  Le **delombok** reste en amont du frontend (l'hôte pointe sur les sources
  délombokées) : Lombok est banni du réacteur par l'enforcer depuis M0, et le
  doc 07 §7 le place explicitement « en amont ».
- **Revue contre 07 §3.1** : conforme sur les six points — nœuds imbriqués et
  stubs externes, arêtes typées avec preuve y compris vers le classpath,
  valeurs d'annotations typées, fermetures transitives incluant le classpath,
  faits de corps optionnels en une traversée, `TypeRef` unique récursif (celui
  du modèle, aucune seconde représentation). Seuls les modules manquent
  (ci-dessus).
- **Revue contre les interdits 07 §10** : RAS sur les 10 points. Notables :
  aucune abstraction de parsing publiée ; aucun pivot String (valeurs typées
  bout en bout) ; le seul `catch` large est une **conversion en diagnostic codé
  suivie d'un rethrow**, jamais un modèle partiel rendu valide ; aucune règle
  de nommage (FQN exacts et préfixes de packages uniquement) ; une seule
  lecture par concept, partagée entre types et membres.
- **État** : `make ci` (clean + verify) VERT — 456 tests (232 model +
  143 testkit dont 122 corpus skipped + 81 frontend), 0 violation
  checkstyle/PMD/SpotBugs, doclint strict, jacoco ≥ 80 % (frontend 92,6 %
  lignes), **0 warning de compilation sur build propre**,
  PIT model 94 % / testkit 85 % / **frontend 84 %** (323 mutants ; les
  survivants restants sont des appels `super.visit*` du scanner et des gardes
  défensives). Arbre propre.
- Prochaine étape : **M3 — `hexaglue-knowledge` + `hexaglue-engine`** (base de
  faits, solveur semi-naïf, strates S0-S4, packs jMolecules/Spring/Jakarta,
  règles seed + propagation R1-R8). C'est le cœur du plan : **D7, D8 et D10
  doivent y être tranchées** (règle de conduite 1 : rien ne se fait sur une
  décision PENDING). Le corpus profil 1 (122 scénarios, aujourd'hui skipped)
  s'allumera kind par kind via `AnalysisRunner`.

### 2026-08-02 — Arbitrage de D7, D8 et D10 (ouverture de M3)

Séance de décision, aucun code modifié. Les trois dernières questions
ouvertes sont tranchées avant l'ouverture de M3 : **plus aucune décision
PENDING au registre**.

- **D8 — classifier les adapters : oui.** L'argument décisif n'est pas la
  classification pour elle-même mais l'outillage de la règle d'or : le
  garde-fou du doc 07 §10.5 interdit à un plugin de re-dériver une information
  du modèle ; sans couverture des adapters, l'audit de M6 n'aurait eu que le
  choix entre violer la règle et perdre la fonctionnalité (c'est exactement ce
  qu'a fait la carrière, 05-C1/C2). Coût pris maintenant parce que `ArchKind`
  et la hiérarchie scellée `ArchType` sont un contrat non encore publié (D5) :
  après la 7.0.0, c'est un changement cassant. Amende le principe « hors
  périmètre » de CLAUDE.md — adapter **dans les sources** = classifié, adapter
  **généré** = sortie.
- **D10 — annotations propres : non**, et sans inscription au backlog (la
  question est close, pas différée). jMolecules par FQN exact + configuration
  explicite. Manque constaté à combler à M3 : le vecteur S1 « configuration
  explicite » n'a pas de porteur dans `HexaGlueConfig` alors que
  `RemediationHint.configureExplicit` et `EvidenceTier.DECLARED_INTENT` le
  présupposent.
- **D7 — posture unique, sans profil.** L'utilisateur écarte les profils
  `strict`/`pragmatic` du doc 06 §3.4 : la cible reste hexagonale/DDD (pas
  d'annotation de persistance dans un domaine correct), mais l'outil analyse
  aussi des applications qui n'y sont pas encore, et le verdict doit le dire
  plutôt que le masquer. Vérification faite, le profil `strict` était
  **redondant** avec un mécanisme déjà au contrat M1 :
  `ValidationConfig.findingThresholds` permet `HG-DDD-0xx: BLOCKER`. La
  sévérité doctrinale se déplace donc de la classification vers la porte de
  validation — un mécanisme au lieu de deux, aucune clé nouvelle, aucune
  branche de profil dans le moteur. Le discriminant « domaine couplé » contre
  « persistance interne à un adapter » est la position dans le graphe, pas
  l'annotation : une application déjà séparée ne produit aucun faux positif
  sans rien configurer.
- **Aller plus loin (demandé sur D7)** : marquage jMolecules **retenu** comme
  remédiation portée par le finding (`@AggregateRoot` sur le domaine,
  `@SecondaryAdapter` sur l'adapter — cible que D8 rend nommable) ; extraction
  du domaine depuis l'entité JPA **écartée de la 7.0.0** (elle écrit dans les
  sources de l'utilisateur et non dans les sources générées, inverse le sens
  du pipeline, et exige des arbitrages hors de portée d'un outil non
  interactif). Raisons complètes dans D7.

Conséquences à porter dans le premier lot de M3 (avant le solveur) :

1. `hexaglue-model` : `DRIVING_ADAPTER`/`DRIVEN_ADAPTER` dans `ArchKind`
   (+ `isAdapter()`), quatrième branche scellée d'`ArchType`, couverture
   `KindCoherence`, tests et PIT au niveau du module.
2. `hexaglue-model` : porteur `classification.explicit` dans la configuration
   typée.
3. `hexaglue-model` : `RemediationHint.addAnnotation` doit porter le **FQN** —
   sur un type déjà annoté `@jakarta.persistence.Entity`, un hint « Add
   @Entity » en nom simple est ambigu ; ce serait B3 réintroduit par la
   remédiation.

Ces trois points modifient le contrat M1 clos : lots séparés, revus contre le
doc 07 §3 comme l'a été M1.

- **Découverte reportée** (règle 9) : `RemediationHint.addAnnotation` en nom
  simple, ci-dessus. Traitée dans le lot 3 de M3, pas ailleurs.
- Prochaine étape : **M3**, en commençant par ces trois amendements du
  contrat, puis `hexaglue-knowledge` (packs jMolecules/Spring/Jakarta) et le
  solveur.

### 2026-08-02 — Jalon M3 : amendements du contrat M1 (lots 1-3, FAITS)

Ouverture de M3 par les trois conséquences des décisions du jour sur le
contrat clos à M1. Un commit par lot, test rouge d'abord à chaque fois.

- **Lot 1 (ec34d97)** — les adapters entrent dans le modèle : `ArchKind`
  gagne `DRIVING_ADAPTER`/`DRIVEN_ADAPTER` (placés après la couche
  application, `UNCLASSIFIED` reste le dernier — l'ordre est un format,
  les snapshots sérialisent ces noms) et `isAdapter()` ; quatrième branche
  scellée `AdapterType` (records `DrivingAdapter`, `DrivenAdapter`) avec
  `direction()`, `ports()` et `isConnected()`. Un adapter porte les ports
  auxquels le moteur l'a relié : les ports **appelés** pour un adapter
  driving (relation invisible dans la structure : injection ou invocation),
  les ports **implémentés** pour un adapter driven. L'invariant de direction
  de `Classification` est élargi : une direction a désormais un sens sur
  toute la frontière de l'hexagone, ports **et** adapters, pas ailleurs.
  Section `adapters` ajoutée au snapshot canonique du testkit (l'invariant
  « chaque branche scellée a sa section » est explicite dans sa Javadoc).
  Deux choix de non-surface, assumés : **pas d'enum de famille d'adapter**
  (REST/JMS/persistence…) et **pas d'`AdapterIndex`** — aucun consommateur
  décidé n'en a besoin avant M6, et les poser maintenant serait l'interdit
  07 §10.1 ; `model.all(AdapterType.class)` suffit.
- **Lot 2 (9b6924e)** — `ClassificationConfig` : `Map<TypeId, ArchKind>`
  ordonnée par identité de type, copiée à la construction, avec
  `declaredKind(TypeId)`. Clé **`TypeId` et non `String`** : la config typée
  est en aval du parsing, le pivot chaîne reste au chargeur (M5). Déclarer
  un type `UNCLASSIFIED` est refusé avec un message qui renvoie vers
  l'exclusion de périmètre : c'est l'absence d'intention, pas une intention.
  `HexaGlueConfig` compose désormais quatre blocs (analysis, classification,
  validation, generation).
- **Lot 3 (6fce049)** — `RemediationHint.addAnnotation` prend un `TypeId` :
  la description nomme l'annotation en entier et l'extrait à coller porte sa
  ligne d'import. Un nom non qualifié est refusé. Un type imbriqué est
  importé par son nom source (`Intents$Aggregate` → `import
  com.acme.Intents.Aggregate;`). `configureExplicit` prend aussi un `TypeId`,
  par cohérence avec la clé de la configuration vers laquelle le hint pointe.
- **État** : `make ci` (clean + verify) VERT — 471 tests (247 model +
  143 testkit dont 122 corpus skipped + 81 frontend), 0 violation
  checkstyle/PMD/SpotBugs bloquante, doclint strict, jacoco ≥ 80 %,
  **0 warning de compilation sur build propre**, PIT model 94 %
  (468 mutants) / testkit 85 % / frontend 84 %. Arbre propre, 3 commits.
- **Découverte mineure** (règle 9, non traitée) : deux warnings checkstyle
  `HideUtilityClassConstructor` préexistants sur
  `hexaglue-testkit/src/test/.../ReactorArchitectureTest.java:28` — antérieurs
  à ces lots, non bloquants, à traiter avec le module agrégateur de tests
  d'architecture déjà consigné plus haut.
- Prochaine étape : **`hexaglue-knowledge`** (packs déclaratifs
  jMolecules/Spring/Jakarta, matching par FQN exact ou préfixe de package)
  puis le solveur `hexaglue-engine` (base de faits, semi-naïf, strates
  S0-S4, agrégateur lexicographique), découpé kind par kind : R1/R2, puis
  R3, puis R4/R5, puis R6-R8.

### 2026-08-02 — Jalon M3 : `hexaglue-knowledge` (lots 4-6, FAITS)

Le module de connaissance des frameworks est complet : contrat de pack,
chargeur strict, appariement, packs embarqués. Un commit par lot, test rouge
d'abord.

- **Lot 4 (2d3eb95)** — contrat de pack et chargeur strict. `KnowledgeFact`
  (les 7 faits techniques du doc 06 §3.3, plus `DECLARED_KIND` pour
  l'intention jMolecules de D10 et `GENERATED_CODE` pour la délimitation du
  périmètre) ; `Selector` scellé à quatre formes (`annotation`, `supertype`,
  `type`, `package-prefix`), chacune refusant un nom simple à la construction
  — **B3 devient inécrivable**, pas seulement corrigé ; `KnowledgeEntry`
  (le fait porte le kind, jamais la règle) ; `KnowledgePack` (identité slug,
  refus des doublons) ; `PackLoader` YAML strict via snakeyaml-engine, avec
  quatre codes : `HG-KNOWLEDGE-001` document illisible, `-002` structure non
  liable (clé inconnue ou manquante, forme inattendue, doublon), `-003`
  symbole non qualifié, `-004` fait inhonorable (inconnu, kind manquant ou
  en trop, capture sur un sélecteur qui n'en porte pas).
- **Lot 5 (52dede2)** — l'appariement. `FrameworkKnowledge.factsFor(model,
  type)` rend les `KnowledgeFinding` en ordre de pack puis d'entrée. La
  reconnaissance est **portée par chaque forme de sélecteur** (`matches`),
  pas par un switch que chaque lecteur répéterait ; `Supertype` s'appuie sur
  la fermeture du frontend et sait en plus retrouver la **route déclarée**
  (superclasse d'abord, puis interfaces dans l'ordre) pour capturer les
  arguments de type : c'est R1 rendu possible — `extends
  JpaRepository<Order, OrderId>` livre `subject` et `id` alors que le pack ne
  connaît que `Repository`. Les noms de capture sont déclarés par le **fait**
  et non par le pack : un pack ne peut pas les lier à la mauvaise position.
- **Lot 6 (425755f)** — quatre packs embarqués, lus par le même chargeur
  strict que n'importe quel pack utilisateur : `jmolecules` (15 entrées, kinds
  déclarés, annotations et interfaces), `spring` (26), `jakarta` (20, noms
  `jakarta.` et `javax.`), `platform` (4 : générateurs, journalisation,
  client HTTP autonome). Chaque pack a ses tests de contrat sur la plus petite
  fixture qui porte la revendication, plus un garde-fou « aucun symbole
  revendiqué par deux packs » — la divergence des ≥ 6 listes de la carrière
  (doc 06 §3.3) ne peut pas se reformer en silence.
- **Tests de régression du doc 08 §5 attribués à M3** — où ils en sont :
  **B3 est acquis** (`EmbeddedPacksTest` : `@jakarta.persistence.Entity` ne
  produit que `PERSISTENCE_MODEL`, jamais un kind ; et un nom simple est
  refusé à la construction du sélecteur, donc la faute n'est plus écrivable).
  **B4 est à moitié acquis** : le kind est bien porté par le fait
  (`DECLARED_KIND` + `declaredKind`) et non par la règle, mais son test
  canonique (`implements ValueObject` → VALUE_OBJECT bout en bout) réclame le
  solveur. **B5, B11, B12, B13 restent entiers** et relèvent du solveur.
- **Écarts assumés par rapport au doc 07 §5** (le doc y donnait un croquis) :
  clé `entries:` et non `rules:` — « règle » est le mot du moteur (règle
  d'inférence), un pack énonce, il n'infère pas ; et pas de syntaxe
  `SPRING_DATA_REPOSITORY(subject: $T0, id: $T1)` — les captures appartiennent
  au sens du fait (voir lot 5).
- **Dépendance nouvelle** : `org.snakeyaml:snakeyaml-engine` 2.10, en
  `dependencyManagement` du parent. Le moteur 2.x ne construit que des maps,
  listes et scalaires — il n'instancie jamais une classe arbitraire depuis un
  document. Le chargeur de configuration de M5 (« YAML strict, clé inconnue =
  erreur ») réutilisera la même dépendance ; pas de bannissement à l'enforcer,
  contrairement à Spoon et aux API Maven, faute de frontière à tenir.
- **État** : `make ci` (clean + verify) VERT — 562 tests (247 model + 143
  testkit dont 122 corpus skipped + 81 frontend + 91 knowledge), 0 violation
  bloquante, doclint strict, jacoco ≥ 80 %, **0 warning de compilation sur
  build propre**, PIT model 94 % / testkit 85 % / frontend 84 % / **knowledge
  88 %** (97 mutants, 97 % de lignes couvertes). Cible `mutation` du Makefile
  étendue au nouveau module. Arbre propre, 3 commits.
- **Contraintes relevées pour le solveur** (règle 9, non traitées ici) :
  1. `org.jmolecules.ddd.types.AggregateRoot` **étend** `types.Entity` : un
     type qui l'implémente reçoit deux faits S1 `DECLARED_KIND`
     (AGGREGATE_ROOT **et** ENTITY). L'agrégateur devra départager à palier
     égal par **spécificité** (le supertype le plus proche l'emporte) — sans
     cela, tout agrégat jMolecules est ambigu.
  2. `@org.jmolecules.ddd.annotation.Repository` posé sur une classe
     déclarerait DRIVEN_PORT sur un non-interface : interaction avec
     `KindCoherence` à trancher dans le solveur (dégrader et émettre un
     finding, plutôt que refuser le modèle).
  3. Les sélecteurs lisent les annotations **du type** uniquement. Les points
     d'entrée déclarés au niveau **méthode** (`@Scheduled`, `@QueryMapping`,
     `@MutationMapping`, `@StreamListener`, `@Transactional` de méthode) ne
     sont donc pas vus ; ils ont été écartés des packs plutôt qu'inscrits
     comme connaissance morte. À rouvrir si le profil 2 le réclame.
  4. `@org.springframework.stereotype.Repository` n'émet **aucun** fait : le
     doc 06 §3.3 ne lui donne pas de ligne et D8 ne nomme que
     `DRIVING_ENTRYPOINT` et `INFRA_DEPENDENCY` comme ancres d'adapter.
     Inventer un fait « adapter de persistance » aurait été hors périmètre
     gelé ; les adapters concernés restent atteignables par les deux ancres
     et par l'implémentation d'un port driven.
- Prochaine étape : **`hexaglue-engine`** — base de faits, évaluation
  semi-naïve, strates S0-S4, agrégateur lexicographique avec marge, arbre de
  preuve ; découpé kind par kind : R1/R2 d'abord (identifiants et agrégats,
  qui allument le corpus le plus vite grâce aux captures du lot 5), puis R3
  (composition), R4/R5 (ports), R6-R8 (application, événements, services).

### 2026-08-02 — Jalon M3 : `hexaglue-engine`, lots 7-10 (FAITS)

Ouverture du solveur. Un commit par lot, tests écrits avant l'implémentation.

- **Lot 7 (a456d36)** — la machinerie et la strate S1. `Fact` scellé, identifié
  par son `render()` et par rien d'autre (la preuve dit *comment*, pas *quoi*,
  donc la route arrivée la première est celle que la base garde) ; `Predicate`
  comme unité de changement que la boucle surveille ; `FactBase` trié par
  sujet puis rendu, jamais par ordre d'insertion ; `Rule` déclare
  `reads()`/`writes()` et reçoit une `Derivation` (vue étroite : ni numéro de
  tour, ni autres règles) ; `Saturation` en semi-naïf — le tour 1 exécute
  tout, les suivants n'exécutent que les règles dont un prédicat lu a grossi,
  donc une règle seed ne tourne qu'une fois. Deux échecs bruyants codés :
  `HG-ENGINE-001` (plafond de tours atteint — la monotonie garantit la
  terminaison, le plafond transforme une règle qui fabrique des faits sans
  fin en diagnostic au lieu d'un build qui pend) et `HG-ENGINE-002` (une règle
  dérive un prédicat qu'elle n'a pas déclaré — c'est le contrat sur lequel
  repose l'ordonnancement). Trois règles : `KNOWLEDGE` (assertion des packs
  sur **tous** les types, périmètre compris — le périmètre limite les
  verdicts, pas la connaissance : savoir qu'un champ injecté est un
  `EntityManager` est ce qui classe son porteur), `S1-CONFIG` et `S1-INTENT`.
  Le palier n'est jamais codé en dur : il vient de `KnowledgeFact.tier()`.
- **Lot 8 (cecd8b2)** — la décision et la boucle. `Aggregator` pèse
  lexicographiquement par palier (score = profil de paliers en base 10,
  saturé à 9 par palier pour qu'un tas de signaux faibles ne déborde jamais
  sur un palier plus fort), départage à égalité par **distance** (le signal le
  plus proche gagne : jMolecules `AggregateRoot extends Entity`, contrainte 1
  de la clôture de `hexaglue-knowledge` — **traitée**), et refuse de décider
  quand rien ne sépare les deux premiers : UNCLASSIFIED, confiance LOW,
  candidats conservés et ordonnés. B12 tombe par construction : deux
  directions qui concourent sont deux kinds pesés, plus une collision de
  priorités. **Les verdicts vivent hors de la base de faits** : un fait
  s'accumule, un verdict se remplace, et une lecture révisée doit disparaître
  avec ce qu'elle a produit. D'où `Classifier` : chaque tour repart d'une base
  vide, les règles lisent les verdicts du tour précédent, on s'arrête quand un
  tour ne change plus rien. Deux règles qui se défont mutuellement épuisent le
  plafond et sont nommées dans le diagnostic.
- **Lot 9 (7df7b88)** — R1. `interface OrderRepository extends
  JpaRepository<Order, OrderId>` livre les trois verdicts d'un coup
  (DRIVEN_PORT, AGGREGATE_ROOT, IDENTIFIER) **sans qu'aucun nom intervienne**,
  plus les liens `MANAGES` et `IDENTIFIED_BY` — nouveau fait `Relation`, parce
  qu'un consommateur qui devrait les redériver les redériverait par le nom
  (interdit 07 §10.5). Les liens sont posés même hors périmètre (un agrégat
  identifié par un `java.util.UUID` a une identité, et le générateur en a
  besoin) ; les verdicts, non. Un argument qui est une variable de type ne
  nomme aucun type : rien n'est dit.
- **Lot 10 (f369bba)** — amendement du contrat modèle, **imposé par le
  solveur** : `AggregateRoot.identityField`/`effectiveIdentityType` et
  `Identifier.wrappedType` deviennent `Optional`. Motif : R1 et l'intention
  déclarée nomment le kind **avant** que quoi que ce soit nomme le champ
  d'identité (`@AggregateRoot` de jMolecules n'en dit rien du tout).
  Composants obligatoires, l'assemblage n'aurait eu le choix qu'entre perdre
  le verdict et refuser le modèle — exactement le couplage que l'ancien
  réacteur avait, où seul un type dont on trouvait l'identité devenait un
  agrégat. Le manque devient un finding de M6, pas un kind perdu.
  `CompositionIndex` n'indexe que les agrégats dont l'identité est nommée ; le
  snapshot du testkit écrit `"identity": null`.
- **État** : `make ci` (clean + verify) VERT — 620 tests (250 model +
  143 testkit dont 122 corpus skipped + 81 frontend + 91 knowledge +
  55 engine), 0 violation bloquante, doclint strict, jacoco ≥ 80 %,
  **0 warning de compilation sur build propre** (un `EnumOrdinal` d'Error
  Prone relevé par le premier `make ci` a été résorbé en pesant les paliers
  par clé d'enum, `ed5a397` — le réacteur reste sans aucun `ordinal()`), PIT
  engine **87 %** (157 mutants, 96 % de lignes couvertes). Cible `mutation`
  du Makefile étendue au nouveau module.
- **Écarts assumés par rapport au doc 07 §4.1** :
  1. **S0 n'est pas matérialisé en faits.** Le `CodeModel` est déjà une base
     de faits syntaxiques indexée : le recopier dans `FactBase` doublerait la
     mémoire et l'ordre pour rien. Les règles le lisent, les preuves le citent.
  2. **La décision est dans la boucle, mais pas dans la base.** Le doc dit
     « agrégateur déterministe (pas une règle) » et « on boucle S2→S4 jusqu'à
     stabilité » ; l'accumulation monotone d'un fait « décidé » rendrait les
     deux incompatibles (un type décidé VALUE_OBJECT puis AGGREGATE_ROOT
     porterait les deux). La boucle externe de `Classifier` réconcilie les
     deux exigences et garde la base monotone à l'intérieur d'un tour.
  3. **Pas de « marge minimale » configurable.** La marge est d'un signal au
     palier décisif ou d'un pas d'héritage ; en dessous, il n'y a rien à
     départager. Aucune clé nouvelle (périmètre gelé).
- **Règle de conduite 4 respectée** : aucune décision par le nom n'a été
  introduite ; le capteur S6 et son vocabulaire configurable restent à écrire.
- **Découvertes reportées** (règle 9, non traitées) :
  1. `ShortClassName` exclu du ruleset PMD partagé (`Fact`, `Rule`, `Edge`,
     `Field` : le vocabulaire de la conception est court à dessein). Le
     fichier excluait déjà `ShortVariable`, `ShortMethodName`, `LongVariable`
     pour la même raison.
  2. Contrainte 2 de la clôture de `hexaglue-knowledge`
     (`@jmolecules.ddd.annotation.Repository` sur une classe → DRIVEN_PORT sur
     un non-interface) : **tranchée par la pratique** — l'intention déclarée
     gagne, le modèle accepte un port qui n'est pas une interface, et
     l'incohérence structurelle devient un finding de M6. Rien n'est dégradé
     dans le moteur : dégrader masquerait ce que l'auteur a écrit.
  3. Deux warnings checkstyle `HideUtilityClassConstructor` préexistants sur
     `ReactorArchitectureTest` : toujours là, toujours non bloquants.
- Prochaine étape : voir le **point de reprise** ci-dessus (capteur S4, puis
  S6, puis R3/R4/R5/R6-R8, puis assemblage `ArchModel` et allumage du corpus).

### 2026-08-02 — Jalon M3 : récolte des attentes du corpus et cliquet (lot 11, FAIT)

Inversion validée par l'utilisateur : mesurer avant d'écrire d'autres capteurs,
avec une réserve explicite — **l'ancien moteur porte des bugs, ses résultats ne
font pas foi**. La conception en découle : ce que la carrière disait est une
*observation*, jamais un oracle.

- **Correction d'un fait annoncé à tort en fin de session précédente** : brancher
  l'`AnalysisRunner` n'aurait pas fait passer le corpus au rouge mais **au
  vert**. `GoldenFiles.assertMatches` *crée* le golden absent, et
  `src/test/resources/golden/` n'existe pas : les 122 scénarios auraient
  enregistré les verdicts d'aujourd'hui — en majorité `UNCLASSIFIED` — comme
  référence de demain. C'est le piège inverse, et le plus dangereux.
- **Constat de fond** : les 122 scénarios ne portent **aucune attente**. Le
  script d'extraction avait récolté les sources, pas les assertions. Or le
  critère de sortie de M3 est la parité (doc 08 §6.1). La référence n'existait
  donc pas.
- **Récolte (`tools/harvest-corpus-expectations.py`)** : pour chaque scénario, le
  champ `origin` nomme la méthode de test d'origine ; le script y retourne, copie
  ses assertions **verbatim** et propose une traduction v7 quand elle est sûre.
  Il n'écrit que des brouillons (`status: draft`) et n'écrase jamais un fichier
  relu. 44 propositions automatiques sur 122, aucun fichier muet (chaque
  brouillon porte soit une proposition, soit la raison de son absence),
  5 marqués CONDITIONAL.
- **Traductions non triviales** rencontrées et encodées : la famille
  `DomainCriteriaTest` (31 scénarios) teste un criteria isolé — `matched()`,
  `priority()`, `ConfidenceLevel` — c'est-à-dire les internes de la conception
  abandonnée ; l'ancien classifieur de ports répondait une **famille**
  (REPOSITORY, GATEWAY) là où v7 répond un **kind** et porte la famille en
  attribut ; et `assertThat(results.get(...)).isEmpty()` — l'ancien moteur ne
  rendait aucun verdict — devient `UNCLASSIFIED`, puisque v7 en doit un à tout
  type du périmètre (A5).
- **Trois états, jamais deux** : ce que la carrière *disait* (les assertions
  citées en commentaire), ce que nous *tenons pour vrai* (les lignes `expect:` /
  `reject:`) et le statut de relecture. Le cliquet ne compte que les scénarios
  relus : importer en masse mesurerait la conformité aux bugs.
- **Module `hexaglue-acceptance`** (nouveau) : le seul endroit qui voit les deux
  côtés du pipeline. `engine → frontend` est interdit au contrat, et Spoon
  arriverait par transitivité dans le module moteur ; le harnais qui a besoin
  des deux vit donc en dehors des deux. C'est aussi le futur foyer de
  l'implémentation d'`AnalysisRunner`, qui n'a de place ni dans le moteur ni
  dans le frontend.
- **Le cliquet** (`CorpusScoreboardTest` + `corpus-floor.properties`) : le build
  échoue **sous** le plancher (régression) **et au-dessus** (progrès non
  enregistré). Un lot prouve qu'il a fait bouger l'aiguille en montant le
  plancher. Message d'échec explicite dans les deux sens.
- **Première relecture : 14 scénarios, 14/14 passent.** Tous des cas d'intention
  déclarée (annotations et interfaces jMolecules, `@Repository`,
  `@SecondaryPort`), donc dans le périmètre de `S1-INTENT`. Bonne surprise
  vérifiée au passage : `implements org.jmolecules.ddd.types.Identifier` est
  reconnu **sans jMolecules au classpath** — le frontend porte les interfaces
  déclarées dans la fermeture.
- **État** : `make ci` VERT — 626 tests (250 model + 148 testkit dont 122 corpus
  skipped + 81 frontend + 91 knowledge + 55 engine + 1 acceptance), 0 warning
  sur build propre, 0 violation bloquante.
- **Découvertes reportées** (règle 9, non traitées) :
  1. `GoldenFiles.assertMatches` enregistre le golden manquant. Commode pour un
     test unitaire, piège pour un corpus de référence : quand
     l'implémentation d'`AnalysisRunner` arrivera, le chemin golden du corpus
     devra exiger un fichier existant plutôt que le créer.
  2. **Le corpus a perdu la configuration** de 5 scénarios
     (`ClassificationConfigIntegrationTest`) : leur attente ne vaut que sous la
     config que le test appliquait, et `scenario.properties` ne porte que
     `basePackage` et `origin`. Ils sont marqués CONDITIONAL et exclus tant que
     le format du corpus ne porte pas de bloc de configuration.
  3. `jacoco.skip` et `maven.deploy.skip` posés sur `hexaglue-acceptance` : c'est
     un harnais de mesure, pas un contrat.
- Prochaine étape : élargir la relecture (les 108 brouillons restants) au fil des
  capteurs, puis capteur S4, puis S6 — chaque lot montant le plancher.

### 2026-08-02 — Jalon M3 : capteurs S4 et S6 (lots 12-14, FAITS)

- **Lot 12 (a883a59)** — capteur S4, la forme de la déclaration. Trois formes
  parlent : ce dont l'état ne peut pas changer est une valeur (record, enum,
  classe dont tous les champs sont finals) ; ce qui enveloppe exactement une
  valeur est la façon dont s'écrit une identité ; et une classe sans état ne dit
  **rien** — l'immutabilité vacante n'est pas un signal, c'est son absence. Un
  wrapper est délibérément lu comme identifiant **et** comme value object :
  rien de structurel ne sépare `OrderId` de `Email`, et n'en émettre qu'un
  serait une supposition déguisée en fait. Les deux concourent, le verdict
  reste indécis avec les deux lectures conservées, et un signal plus fort
  tranche.
- **Lot 13 (1be33f2)** — capteur S6 et son vocabulaire. **Quatrième amendement
  du contrat M1** : `ClassificationConfig` porte désormais `namingSuffixes`,
  `empty()` devient `defaults()` (cohérent avec `ValidationConfig.defaults()`),
  et `silent()` apparaît pour la posture « ne lis aucun nom ». Le vocabulaire
  est de la configuration parce qu'une convention de nommage est une propriété
  d'une base de code, pas de l'outil : une équipe qui écrit `OrderRef` le
  déclare et est comprise. C'est aussi le **seul** endroit du moteur où un nom
  est apparié — la règle de conduite 4 est levée là, et nulle part ailleurs. Le
  vocabulaire livré est volontairement court ; `Service` ne désigne que la
  couche application, un service de domaine portant le même suffixe et se
  distinguant par ce qu'il fait. Seul le suffixe le plus long d'un kind est lu,
  pour qu'un nom pèse une fois.
- **Lot 14 (cf5b416)** — arbitrage de neuf scénarios et montée du plancher à
  **18/20**. La mesure a d'abord servi à débusquer un vrai défaut du lot 12 :
  une classe **mutable** à un seul champ était lue comme un identifiant. Un
  état qui peut changer n'est ni une valeur ni l'identité de quoi que ce soit ;
  le signal de wrapper exige désormais l'immutabilité (corrigé dans le même
  lot que le capteur, avec son test).
- **Divergences assumées, inscrites scénario par scénario** (c'est le « fichier
  d'écarts » du doc 08 §6.1, construit au fil de l'eau) : un enum devient
  VALUE_OBJECT et un wrapper nommé `OrderId` devient IDENTIFIER là où l'ancien
  moteur ne rendait aucun verdict ; et **changement de posture** — un nom
  conventionnel produit désormais un verdict à MEDIUM au lieu du silence
  (`interfaceWithoutMarkersIsUnclassified`, `shouldHandleInterfaceTypes`). Le
  raisonnement : un verdict qu'une porte de validation peut refuser est plus
  utile qu'un silence, et la doctrine (06 §3.1) interdit au nom de l'emporter
  *contre* S2-S4, pas de conclure quand rien d'autre ne parle. **L'utilisateur a
  jugé la question déterminante pour le comportement du produit : elle est
  ouverte au registre sous D13 (PENDING) et se tranche en début de session
  suivante.** Ce qui est livré vaut donc comme état provisoire, pas comme
  posture retenue ; la reprise est bornée et décrite dans D13.
- **Deux scénarios relus échouent volontairement** : ils attendent R3
  (composition) et R4 (port par ses signatures). Le plancher les compte comme
  relus et non passants — c'est le reste à faire, mesuré.
- **État** : `make ci` VERT — 645 tests, 0 warning sur build propre, corpus
  20 relus / 18 passants sur 122.
- **Découvertes reportées** (règle 9, non traitées) :
  1. `generatedInterfacesShouldBeSkippedFromPortClassification` rend
     **NO VERDICT** sur `com.example.OrderJpaRepository` : le type n'entre pas
     dans le périmètre alors que son package est bien celui du scénario. À
     instruire — soit le frontend ne le produit pas, soit le filtre de portée
     l'écarte à tort. Laissé en brouillon.
  2. Tension de fond à trancher avec R2 : l'identité est une propriété
     **relationnelle** (le champ d'identité d'un agrégat, la capture d'un
     dépôt), alors que S4 ne voit qu'une forme et S6 qu'un nom. Tant que R2
     n'existe pas, un identifiant isolé n'est reconnu que par son nom, ce que
     la doctrine autorise mais ne recommande pas.

### 2026-08-02 — Clôture de session : D13 ouverte au registre

L'utilisateur juge la posture de classification **déterminante pour tout le
comportement de HexaGlue** et la reprend en session dédiée. Conséquences :

- **D13 (PENDING)** — que répond le moteur quand seul un signal faible parle ?
  Conclure à MEDIUM (ce qui est livré), se taire en conservant les candidats, ou
  rendre le seuil configurable. La question déborde le nommage : elle décide de
  la lisibilité des rapports, de ce que `validate` peut refuser, du seuil de
  génération, et du nombre de types laissés UNCLASSIFIED sur un parc existant.
- **D14 (PENDING)** — le signal d'un port driving : injection seule ou faits de
  corps. À trancher avant R4/R5, sans urgence.
- **Ce qui est livré reste en vigueur par défaut** mais n'est pas une posture
  retenue. La reprise, si D13 tranche autrement, est bornée à deux points de
  code (`ConventionalName`, `Aggregator.decideOne`), deux fichiers d'attentes de
  corpus et le plancher du cliquet. Rien d'autre n'en dépend aujourd'hui —
  c'est précisément pourquoi il vaut mieux trancher maintenant qu'après R2-R8.
- **Rien n'est à défaire** : les six modules sont verts, l'arbre est propre,
  le corpus mesure 18/20 relus et le reste est en brouillon assumé.

### 2026-08-02 — Session « référentiel » : D13/D14 tranchées, référentiel des règles (doc 09), plan de tests (doc 10)

Session d'analyse et de décision, sans modification de `hexaglue-next/`
(des sondes temporaires ont instrumenté la chaîne frontend → moteur puis ont
été supprimées ; dernier commit inchangé `cf5b416`, arbre propre).

- **Instruction de D13 par la mesure, sur le code réel.** Trois sondes
  successives sur les 122 scénarios du corpus : (1) 39 des 118 verdicts
  rendus reposaient sur le seul suffixe (26 DRIVEN_PORT, 6 DOMAIN_EVENT,
  4 APPLICATION_SERVICE, 2 DRIVING_PORT, 1 QUERY_HANDLER), **dont 3 faux**
  — des interfaces marqueurs (`DomainEvent`, `ApplicationEvent`) lues
  DOMAIN_EVENT, que l'ancien moteur refusait explicitement ; (2) le
  classement des 39 par « ce qu'une règle future pourrait lire sur la
  fixture » : 25 avec relation/annotation, 4 package seul, 10 rien-jamais ;
  (3) la composition du corpus : **73 scénarios mono-type** (26 avec ancre
  S1/S2, 47 nus) et 33 bi-types — héritage direct des tests unitaires des
  criteria de la carrière, qui posent au moteur des questions auxquelles
  aucun classifieur contextuel ne peut répondre.
- **Recadrage de l'utilisateur, structurant pour la suite** : la refonte
  retombait sur l'écueil n°1 de la carrière (le nommage structurant) ; les
  conventions de nommage sont à remettre en cause (« on verra si c'est
  vraiment nécessaire à la suite de M3 ») ; une interface isolée ne peut pas
  être classifiée, son contexte d'usage est requis ; et le but du corpus
  n'est pas d'être vert aujourd'hui mais que **le chemin restant à
  implémenter le fasse devenir vert**.
- **Doc 09 écrit** ([09-referentiel-regles.md](09-referentiel-regles.md)) :
  le référentiel des règles pour les deux questions (Q1 identification /
  Q2 conformité) et les deux situations (migration / hexagonal existant).
  Thèses : Q1 tolère ce que Q2 condamne (généralisation de D7) ; un seul jeu
  de règles, seules varient la densité d'ancres et les gates ; dérivation
  par vagues bord → centre ; anti-règle « toute interface n'est pas un
  port » ; l'agrégat est le sujet des signatures d'un port à rôle
  repository, l'identifiant sa clé de recherche ; S5 scindé (structurel =
  ancre, conventionnel = différé avec S6).
- **D13 et D14 consignées au registre** (plus aucune décision PENDING).
  D13 dissoute par retrait de sa prémisse : S6 et la moitié conventionnelle
  de S5 sortent de la posture par défaut jusqu'à fin M3, réévaluation
  mesurée (gain vs dégât) ensuite ; quand rien ne parle → UNCLASSIFIED
  catégorisé avec candidats et remédiation. D14 = « A étagé » : port driving
  structurel, `INVOKES` en renfort sous `METHOD_BODIES`, jamais une
  condition.
- **Doc 10 écrit** ([10-plan-tests-m3.md](10-plan-tests-m3.md)) : le plan de
  tests du reste de M3 — 4 couches (unitaires de règle, propagation
  multi-tours, acceptation corpus au cliquet, qualité), la doctrine
  d'arbitrage du corpus (une attente n'est due qu'à ce que la fixture porte ;
  fixtures S3 en noms non conventionnels), la grammaire des scénarios
  contextuels (nominal + contre-cas + 3 scénarios intégraux, un par profil),
  et les lots 15-23 avec critères de sortie et effet plancher.
- **Tenue à jour** : ligne M3 du tableau des jalons, point de reprise
  réécrit, README du dossier indexant les docs 09-10, mémoire de session.
- **Prochaine session : lot 15** (reprise bornée D13 — `defaults()` sans
  suffixes + préset nommé, ré-arbitrage des 2 attentes du lot 14 vers
  UNCLASSIFIED, plancher recompté, attendu inchangé 20/18).

### 2026-08-02 — Jalon M3 : vocabulaire de nommage rendu opt-in (lot 15, FAIT)

Un commit (`732ae0a`), TDD, `make ci` vert, plancher inchangé à 20/18.

- **Reprise bornée de D13, telle que le doc 10 la borne.**
  `ClassificationConfig.defaults()` ne porte plus de suffixes ; le vocabulaire
  conventionnel survit dans `conventional()` (préset opt-in) et
  `conventionalNamingSuffixes()` (la table seule, composable avec des
  déclarations et intrant du harnais du lot 23). `ConventionalName` (S6) et
  `Aggregator` sont **inchangés** : le capteur lit une liste vide et se tait,
  le chemin du silence existait déjà. `silent()` a été retiré — `defaults()`
  est désormais littéralement ce que cette fabrique produisait, et deux
  fabriques identiques n'auraient rien nommé de plus.
- **Une attente de plus à ré-arbitrer que prévu : trois, pas deux.** Le doc 10
  annonçait `ConflictDetectionTest-interfaceWithoutMarkersIsUnclassified` et
  `DomainClassifierTest-shouldHandleInterfaceTypes` ; la mesure a ajouté
  `PortClassifierTest-shouldHandleRecordTypes` (`record OrderId(String value)`
  seul). Sa relecture disait « lu comme une identité par la forme **et** par
  le nom à la fois » : le nom retiré, S4 rend le duel honnête
  IDENTIFIER/VALUE_OBJECT et l'agrégateur répond UNCLASSIFIED avec les deux
  candidats. C'est la doctrine du doc 10 §2.1 appliquée à la lettre — une
  fixture d'un seul record ne porte pas de quoi trancher, ce qui sépare une
  identité d'une valeur est son **usage** (clé de recherche d'un port), et ce
  scénario n'a pas de port. L'attente IDENTIFIER est due à un scénario câblé,
  pas à celui-ci.
- **Les trois relectures rejoignent le verdict de la carrière**, pour la
  raison que la carrière n'avait pas : l'anti-règle « toute interface n'est
  pas un port » et le silence honnête sur un type sans position dans le
  graphe. Chaque fichier d'attentes porte ce raisonnement et la remédiation
  (élargir le périmètre, câbler le type, ou le déclarer).
- **Arithmétique du plancher** : 15 passants après retrait du vocabulaire,
  + 3 ré-arbitrés = **18/20**, exactement l'attendu du doc 10 ; les 2 échecs
  restants sont toujours ceux qui attendent R3 et R4. `corpus-floor.properties`
  est inchangé — c'est le résultat, pas un ajustement.
- **Tests ajoutés** : côté modèle, la posture par défaut muette, le préset
  nommé, la composition vocabulaire + déclarations, et les gardes de validation
  du vocabulaire (suffixe vide, suffixe pour UNCLASSIFIED, copie défensive) qui
  n'étaient pas couvertes ; côté moteur, S6 muet sous la posture par défaut et
  le duel S4 laissé ouvert faute de relation. 655 tests (645 avant).
- **Qualité** : PIT engine 89 % (87 % avant, pas de recul), model 95 %,
  testkit 84 %, knowledge 88 %, frontend 72 % ; 0 warning sur build propre.
- **Prochaine session : lot 16** (ré-arbitrage du corpus par la doctrine
  doc 10 §2 — 47 mono-type nus vers UNCLASSIFIED, 26 ancrés relus contre leurs
  ancres seules, 33 bi-types au cas par cas ; zéro code moteur ; sortie
  attendue ~100 relus et la liste mesurée de ce que les lots 17-20 doivent
  faire passer).

### 2026-08-02 — Jalon M3 : corpus profil 1 arbitré de bout en bout (lot 16, FAIT)

Un commit (`8fa0006`), zéro code moteur, `make ci` vert. Plancher :
**122 relus, 114 passants** (20/18 avant).

- **Les 122 scénarios sont arbitrés**, chacun contre ce que sa fixture porte
  réellement (doctrine doc 10 §2), avec le raisonnement écrit dans son fichier
  d'attentes. Répartition des verdicts attendus : silence honnête pour
  l'écrasante majorité, ancres S1/S2 pour ~20, forme S4 pour ~15, et le
  câblage réel pour les 8 scénarios qui en portent.
- **La découverte structurante : la famille « dépôt amputé ».** Quinze
  scénarios posent `class Order` + `interface OrderRepository { Order
  findById(...) }` et rien d'autre. L'ancien moteur y lisait un dépôt et un
  agrégat ; le référentiel non : **R4 établit un port piloté depuis son
  consommateur** (champ ou paramètre de constructeur d'un type du cœur), et
  ces fixtures n'ont ni consommateur ni implémenteur. Sans port, pas de R1b,
  donc pas d'agrégat non plus. Ce sont donc des fixtures de l'anti-règle, pas
  des scénarios en attente de R4. **Conséquence sur deux scénarios déjà
  relus** : `shouldMatchTypeUsedInRepositoryWithIdField` (attendait R4) et
  `shouldMatchClassWithIdFieldContainedInAggregateViaCollection` (attendait
  R3, dont le possesseur n'est confirmé par rien) passent à UNCLASSIFIED —
  l'ancienne lecture « les 2 restants attendent R3 et R4 » est caduque.
- **Ce que le profil 1 prouve désormais** : l'anti-règle et les ancres. Il ne
  peut pas prouver la dérivation relationnelle, faute de fixtures câblées : les
  seules qui le sont sont les golden files (banking, coffeeshop, ecommerce,
  application-service) et les deux pivots. C'est exactement le basculement
  annoncé par D13 (« le poids du corpus bascule sur des scénarios
  contextuels ») — mesuré cette fois, et non plus supposé.
- **Les 8 échecs = la liste de travail des lots 18-20**, sans une seule entrée
  parasite : R4+R6 sur les deux pivots et l'exemple application-service ;
  R1b+R2 sur les quatre golden files (dont un remplacement de verdict à
  épingler : `com.coffeeshop.domain.order.Order` est lu VALUE_OBJECT par S4
  aujourd'hui et doit devenir AGGREGATE_ROOT par R1b).
- **Découverte du lot 14 instruite** : `OrderJpaRepository` n'est pas « hors
  périmètre » du moteur, il est **absent du modèle** — le frontend
  (`AnalysisPerimeter.covers`) écarte tout type portant `@Generated` avant que
  le moteur existe. Cela contredit le référentiel (doc 09 prévoit un
  UNCLASSIFIED catégorisé `GENERATED_CODE`) et rend morte l'entrée
  `GENERATED_CODE` du pack jakarta. **Consigné en D15 (PENDING)**, à trancher
  avant le lot 21 ; les trois scénarios concernés épinglent le comportement
  livré par une attente `= NO VERDICT`, sentinelle désormais documentée au
  contrat de `CorpusExpectations`.
- **Deux arbitrages notables, DÉLIBÉRÉMENT DIFFÉRENTS de la carrière** :
  `shouldNotMatchOutboundOnlyClass` devient APPLICATION_SERVICE (R6 demande un
  port piloté consommé **ou** un port pilotant implémenté, pas les deux) ; les
  deux fixtures `interface X extends PrimaryPort/SecondaryPort` passent à
  UNCLASSIFIED, parce que jMolecules livre ces symboles comme **annotations** —
  la déclaration ne compile pas et ne déclare donc rien. La carrière y arrivait
  par appariement de nom simple sur les supertypes, raccourci que v7 retire.
- **Autres arbitrages consignés dans les fichiers** : intention héritée (le
  sous-type d'un agrégat annoté ne reçoit rien : une annotation Java n'est pas
  héritée et aucune règle ne descend un kind) ; scénarios dont la moitié
  « configuration » n'a pas été récoltée (l'attente ne tient qu'aux sources) ;
  ports pilotants sans adapter appelant, qui restent muets par R5/D14.
- **Prochaine session : lot 17** (vague W1 adapters, D8 — T1 sur W1-DA/W1-DR
  avec contre-cas de packs, puis premiers scénarios contextuels d'adapters ;
  ce sont des scénarios **à écrire**, le profil 1 n'en porte aucun).

### 2026-08-02 — Jalon M3 : vague W1, l'anneau extérieur (lot 17, FAIT)

Deux commits (`bd57222` nettoyage rédactionnel, `26ab698` la vague), TDD avec
rouge observé, `make ci` vert (676 tests), PIT engine 89 % (pas de recul).
Plancher : **127 relus, 119 passants** (122/114 avant).

- **Deux règles livrées.** `FrameworkEntryPoint` (W1-DA) lit un point d'entrée
  du framework (`@RestController`, `@Controller`, `@ControllerAdvice`, les
  listeners Kafka/JMS/Rabbit, `@Path`) et conclut DRIVING_ADAPTER.
  `InfrastructureDependency` (W1-DR) lit un type qui **détient** un outil
  sortant (`EntityManager`, les `*Template`, `WebClient`, Feign) et conclut
  DRIVEN_ADAPTER. L'outil est cherché partout où la déclaration le pose :
  champ, paramètre de constructeur, argument de type de l'un ou l'autre, ou
  supertype — envelopper un client dans une `List` n'en fait pas moins une
  sortie. Une classe utilitaire `Adapters` porte la formulation d'évidence
  commune aux deux.
- **La garde qui compte : seuls les classes et records sont lus.** Elle n'est
  pas cosmétique — sans elle, `interface Ledger extends JpaRepository<Book,
  UUID>` serait lu DRIVEN_ADAPTER par W1-DR (le paquet vendeur porte
  `INFRA_DEPENDENCY`) **et** DRIVEN_PORT par R1, au même palier et à la même
  distance : l'agrégateur ne départagerait pas et rendrait UNCLASSIFIED. Une
  interface est un contrat, un adapter est une implémentation. Un test épingle
  la non-collision.
- **PIT a fait son travail.** Premier jet à 88 % (recul d'un point) : trois
  mutants survivants, dont un garde `tools.isEmpty()` purement décoratif
  (supprimé) et la garde de nature, que le test « à propos d'une interface »
  ne touchait pas — la règle ne lisait pas encore les interfaces implémentées,
  donc le test passait à vide. Corrigé des deux côtés (la règle lit les
  supertypes, le test épingle la collision Spring Data) : retour à 89 %.
  Restent deux mutants sur `reads()`, du même type que ceux déjà tolérés sur
  R1 et S1-INTENT : l'ordre alphabétique des règles fait passer `KNOWLEDGE`
  avant elles, donc la déclaration n'est pas observable.
- **Cinq scénarios contextuels écrits** (préfixe `AdapterRing-`), en
  vocabulaire volontairement non conventionnel (`HangarDoor`, `HangarBooks`,
  `Manifest`, `Wiring`) : un adapter pilotant, un adapter piloté, et trois
  contre-cas qui gardent la vague de trop mordre — le stéréotype seul
  (`@Service`, `@Component`), la dépendance neutre (slf4j, validation) et la
  plomberie (`@Configuration`, `@SpringBootApplication`). Les cinq passent ;
  les 8 échecs des lots 18-20 sont inchangés, donc aucune régression.
- **Limite connue, non traitée** : le pack apparie les annotations **portées
  par le type**. Un `@KafkaListener` posé sur une méthode d'un `@Component`
  n'est donc pas vu. Consigné aux découvertes.
- **Prochaine session : lot 18** (vague W2, les ports par position : R4 le
  port piloté depuis son consommateur, R5 le port pilotant depuis son
  implémenteur du cœur et son adapter appelant, l'anti-règle W2-X, le rôle du
  port par la forme des signatures, et la propagation W1→W2 dont W1-DR2).

### 2026-08-02 — Jalon M3 : vague W2, les ports par position (lot 18, FAIT)

Un commit (`9678251`), TDD avec rouge observé (7 échecs sur les cas nominaux
avant implémentation), `make ci` vert (713 tests), **PIT engine 89 % → 92 %**.
Plancher : **133 relus, 125 passants** (127/119 avant).

- **Quatre règles livrées, plus l'anti-règle.** `ConsumedContract` (R4) lit une
  interface que le cœur détient et que rien du cœur n'implémente comme port
  piloté ; `ExposedContract` (R5) lit une interface que le cœur implémente et
  qu'un adapter pilotant détient comme port pilotant, avec l'arête `INVOKES`
  en **seconde évidence** quand les corps ont été lus (D14 : la capacité
  ajoute, elle ne conditionne pas) ; `PortImplementation` (W1-DR2) lit à
  rebours l'implémenteur d'un port piloté établi comme adapter piloté ;
  `PortSignatures` (W2-ROLE) lit le métier du port piloté
  (REPOSITORY/EVENT_PUBLISHER/GATEWAY).
- **W2-X n'est pas une règle mais une condition**, portée par la classe
  `Contracts` avec la doctrine en Javadoc : une interface que le cœur écrit
  *et* appelle est une couture interne, pas un trou dans le mur. Elle ne dérive
  rien — il n'y a donc pas de preuve à signer — et deux tests l'épinglent,
  dont l'implémentation atteinte par la **fermeture des supertypes** (une
  classe descendant d'une base qui remplit le contrat le remplit aussi).
- **« Du cœur » se lit sur le tour précédent**, en une seule notion : un type
  que l'anneau réclame déjà n'est pas le cœur qui sort. À l'amorçage rien
  n'est placé, donc tout compte comme cœur et les lectures sont larges ; les
  tours suivants les resserrent, et **une lecture dont le sol a disparu
  disparaît avec lui** — c'est le second test T2 (un contrat lu comme port au
  tour 1 parce que son unique détenteur n'était pas encore sur l'anneau).
- **Le nouveau fait `PortRole`** (prédicat `PORT_ROLE`) porte le
  `DrivenPortType` jusqu'au lot 21 ; le sujet sur lequel les signatures
  convergent est publié en `MANAGES`, la même relation que R1 — donc R1b
  (lot 20) n'aura qu'une source à lire. La convergence exige **exactement un**
  sujet du périmètre : deux sujets, ce n'est pas converger, et le rôle retombe
  sur GATEWAY plutôt que d'en élire un au hasard.
- **PIT a de nouveau fait son travail** : trois mutants survivants ont révélé
  du code mort et une formulation trop bavarde. Le garde `!id.equals(contract)`
  de `holdersOf` était inatteignable (supprimé) ; le filtre de nature l'est
  devenu aussi une fois que **détenir** a cessé de compter les champs statiques
  (une constante appartient au type, pas à ses collaborateurs — donc une
  interface ne détient jamais rien) ; et `isDomain() || isApplication() ||
  isPort()` est devenu son complément exact `!= UNCLASSIFIED && !isAdapter()`,
  qui n'a pas de branche inatteignable et couvre d'avance les kinds à venir.
  Reste un seul mutant sur `reads()`, de la classe déjà tolérée sur R1, S1 et
  W1 (l'ordre alphabétique des règles rend la déclaration inobservable).
- **Six scénarios contextuels écrits** (préfixe `PortBoundary-`, vocabulaire
  non conventionnel : `Ledger`, `Checkout`, `LedgerBook`, `Assembly`,
  `AssemblyLine`, `HangarDoor`, `HangarBooks`) : le port piloté par son
  consommateur, la couture interne, le contrat que seul l'anneau appelle, le
  port pilotant, le contrat que personne n'appelle, et le port déclaré qui
  place son implémenteur. Les six passent.
- **Sur les 8 échecs hérités, aucun ne bascule mais trois se réduisent** : les
  claims `DRIVEN_PORT` des deux pivots et de l'exemple application-service
  passent désormais par R4 ; ces scénarios restent rouges sur
  `APPLICATION_SERVICE` (R6, lot 19). Le lot 10 du doc annonçait « le scénario
  relu en attente de R4 passe » : caduc depuis le lot 16, qui a ré-arbitré la
  famille « dépôt amputé » en UNCLASSIFIED. La hausse du plancher vient donc
  entièrement des scénarios écrits, comme au lot 17.
- **Correction de mesure** : le point de reprise du lot 17 intervertissait deux
  chiffres PIT. Relevé de référence vérifié sur l'arbre d'avant ce lot —
  model 95 %, **testkit 72 %**, **frontend 84 %**, knowledge 88 %, engine 89 %.
- **Prochaine session : lot 19** (vague W3, l'application : R6 le pivot, R6b
  la classe appelée par un adapter, R8 le service de domaine avec son
  contre-cas d'état mutable ; statut des classes abstraites à arbitrer dans le
  lot). Les consommateurs des scénarios `PortBoundary-` y deviendront des
  services applicatifs : leurs attentes sont à ré-arbitrer à ce moment-là.

### 2026-08-02 — Jalon M3 : vague W3, l'application (lot 19, FAIT)

Un commit (`057689b`), TDD avec rouge observé (8 échecs sur les cas nominaux
avant implémentation), `make ci` vert (736 tests), **PIT engine 92 % → 93 %**.
Plancher : **138 relus, 132 passants** (133/125 avant).

- **Trois règles livrées.** `PortPivot` (R6) lit la classe qui répond à un port
  pilotant **ou** appelle un port piloté — l'une des deux moitiés suffit, en
  exiger les deux aurait fait taire la moitié que la plupart des codes
  écrivent. `AdapterCollaborator` (R6b) lit la même couche depuis l'anneau : le
  type auquel un point d'entrée confie le travail, **à condition** qu'autre
  chose parle — un port piloté à lui, ou le stéréotype du framework.
  `DomainCollaboration` (R8) lit le comportement que le domaine possède sans
  qu'aucun de ses types puisse le porter.
- **Le stéréotype corrobore sans jamais décider**, et c'est écrit dans la
  structure de la règle : être atteint depuis l'anneau est requis, le
  stéréotype ne fait que compléter. Un `@Service` que personne n'atteint reste
  UNCLASSIFIED — un scénario de corpus l'épingle, parce que c'est exactement
  l'écueil qui classifierait toute application Spring d'un coup.
- **Quatre conditions pour R8**, chacune écartant un voisin différent : rien
  qui puisse changer (sinon c'est une entité), aucun port appelé (sinon c'est
  l'application), au moins deux types du domaine dans les signatures (sinon le
  comportement appartient au type unique), et un appelant à l'intérieur (sinon
  la position n'est pas observable). Le kind était **inatteignable** dans
  l'ancien moteur (A6) : toute une famille de code de domaine n'avait nulle
  part où atterrir.
- **Arbitrage demandé par le doc 10 — les classes abstraites** : lues comme
  n'importe quelle classe. Une classe abstraite qui détient des ports joue le
  rôle applicatif autant que la sous-classe qui la complète ; retenir la
  lecture dirait quelque chose sur la façon dont le code est factorisé, ce qui
  n'est pas la question posée. Un test l'épingle.
- **Le court-circuit reste un court-circuit** : un point d'entrée qui détient
  un port piloté directement n'est pas promu couche applicative. La lecture est
  **retenue** plutôt qu'émise puis surclassée, pour que rien dans le modèle ne
  prétende que ce type orchestre quoi que ce soit — c'est un finding de M6.
- **PIT a encore payé.** Premier jet à **91 %** (recul d'un point) : douze
  mutants survivants, dont trois défauts réels de fixtures — le contre-cas
  « appelle un port » était bloqué en amont par sa propre condition d'état (le
  champ n'était pas final), le contre-cas « un seul type du domaine » ne
  comptait qu'une mention au lieu d'en compter deux dont une seule du domaine,
  et le contre-cas « rien d'autre à dire » ne détenait rien du tout. Corrigés,
  plus un filtre de nature dans `contractsOf` retiré : il était inobservable
  **et faux** — ce qui compte est qu'un supertype ait été lu comme port, pas
  qu'il soit une interface. Retour à **93 %**, deux mutants restants, de la
  classe déjà tolérée sur `reads()`.
- **Cinq scénarios contextuels écrits** (préfixe `ApplicationLayer-`) : la
  chaîne complète des quatre vagues sur une seule fixture (point d'entrée →
  port pilotant → pivot → port piloté), la lecture depuis l'anneau avec
  stéréotype, le stéréotype sans position, le service de domaine, et le service
  qui se souvient. **Deux attentes ré-arbitrées** comme annoncé au lot 18 :
  les consommateurs des scénarios `PortBoundary-` sont désormais des services
  applicatifs.
- **Deux scénarios hérités basculent au vert** (`shouldMatchPivotClass` et
  `shouldNotMatchOutboundOnlyClass`) et un troisième se réduit à ses seules
  attentes de domaine (`createApplicationServiceExample`). **Il reste 6
  échecs**, tous du domaine : R1b/R2 sur les quatre golden files, plus les
  deux `OrderId` de coffeeshop.
- **Prochaine session : lot 20** (vague W4, le domaine par son cycle de vie :
  R1b l'agrégat comme sujet des signatures d'un port à rôle repository — la
  relation `MANAGES` est déjà émise —, R2 l'identifiant comme clé de recherche
  qui résout le duel S4, R3a/R3b la possession avec ou sans identité, R7
  l'événement. C'est le lot qui vide la liste des échecs hérités, dont un
  **remplacement de verdict** à épingler : `com.coffeeshop.domain.order.Order`
  est lu VALUE_OBJECT par S4 et doit devenir AGGREGATE_ROOT par R1b).

### 2026-08-02 — Jalon M3 : vague W4, le domaine par son cycle de vie (lot 20, FAIT)

Un commit (`1744410`), TDD avec rouge observé (15 échecs sur 29 avant implémentation),
`make ci` vert (777 tests), **PIT engine 93 % → 94 %**. Plancher : **143 relus,
143 passants** (138/132 avant) — **la liste des échecs hérités est vide**.

- **Cinq règles livrées.** `ManagedAggregate` (R1b) lit le sujet qu'un port à
  rôle repository garde et rend comme l'agrégat qu'il est — jumeau structurel de
  ce qu'une déclaration Spring Data dit en une ligne, et il consomme la même
  relation `MANAGES`. `LookupIdentity` (R2) lit la clé par laquelle ce port
  cherche l'agrégat comme son identité, ce qui tranche le duel que la forme ne
  peut pas trancher. `OwnedEntity` (R3a) et `OwnedValue` (R3b) lisent la
  composition, avec identité propre ou sans. `PublishedEvent` (R7) lit ce que le
  domaine annonce.
- **Un quatrième utilitaire partagé, `Lifecycle`** (avec `Contracts`, `Shapes`,
  `Signatures`) : les ties de stockage, la possession, et « porte une identité ».
  Deux choses qu'un possesseur garde n'y sont **pas** des parties : un contrat
  (le tenir est une question de couche, l'appeler valeur effacerait la
  frontière) et une valeur écrite comme une identité — la composition ne sait
  pas distinguer l'identité de l'agrégat d'une valeur voisine, les deux étant
  des champs, et trancher par la position serait une devinette. `Shapes` porte
  désormais cette lecture unique (`readsAsIdentity`), que S4 utilisait déjà en
  interne : une seule définition, donc les deux ne peuvent plus diverger.
- **Le corollaire du doc 10 sur R2 tient par construction** : ce que R3 refuse
  de toucher est exactement ce que R2 peut réclamer. Un wrapper qu'aucun port ne
  cherche reste candidat, comme le plan l'exigeait.
- **R7 a besoin de ce que l'agrégat garde**, sinon tout accesseur annoncerait
  quelque chose. Une méthode qui rend un champ lit l'état ; une méthode qui rend
  ce que le type ne garde pas l'a construit. L'interface marqueur se tait par la
  seule condition d'immuabilité — un contrat n'a pas d'état, donc rien à son
  sujet n'a pu se produire.
- **Une lecture de la vague W3 déplacée, débusquée par le corpus** : `Hull`,
  partie immuable d'un agrégat avec deux accesseurs, a exactement la forme que
  R8 lit, et son possesseur est exactement le « appelant à l'intérieur » que R8
  exige. R8 gagne donc une cinquième condition — **aucun agrégat n'en est fait**
  — parce que le même champ est une composition vu d'un bout et une
  collaboration vu de l'autre, et que seul le kind du possesseur les sépare.
- **PIT a encore payé, et cette fois surtout en simplification.** Douze mutants
  survivants au premier jet (94 % → 92 %), dont trois gardes réellement mortes :
  le filtre de rôle de `storageTies` (la relation `MANAGES` n'est énoncée que
  d'un port qui stocke — le redemander répétait sa propre prémisse), le
  `subject.isEmpty()` de R2 (un verdict d'agrégat implique déjà que le type
  existe) et deux filtres de périmètre redondants. Les autres ont produit cinq
  tests qui manquaient : le tie d'identité n'est pas un cycle de vie, chercher
  par un attribut n'en fait pas une identité, une méthode qui ne rend pas
  l'agrégat n'est pas une recherche, un agrégat n'est pas fait de lui-même, et
  un service de domaine reste un service quand un agrégat existe ailleurs.
- **Cinq scénarios contextuels écrits** (préfixe `DomainLifecycle-`, vocabulaire
  non conventionnel : `Fleet`, `FleetTag`, `Ledger`, `Hull`, `HullTag`,
  `Manifest`, `Berth`, `Sailing`, `Notice`) : la chaîne intégrale des quatre
  vagues jusqu'à l'identité, le wrapper que rien ne cherche, la composition sans
  identité (partie mutable comprise), la composition avec identité, et
  l'événement annoncé avec son contre-cas marqueur dans la même fixture.
  **Aucune attente héritée n'a eu à être ré-arbitrée** : les six échecs restants
  sont passés au vert tels quels.
- **D15 tranchée en fin de session (option A, commit `2dc797f`)** : le code
  généré n'entre pas dans le modèle. L'instruction est au registre ; l'essentiel
  en trois points — l'option « verdict plein » détruit la lecture du code écrit
  à la main (mesuré, voir les découvertes) ; le rapport livré ne perd rien,
  `InventoryTotals` ne comptant que des kinds classifiés, sans total ni ligne
  « unclassified » ; et la seule chose que l'option mixte achetait, transporter
  un décompte jusqu'aux plugins, coûtait un 5ᵉ amendement du contrat M1 pour un
  besoin que le produit n'exprime pas. Rien n'est supprimé : les entrées de pack
  restent le prix de l'option si M6 réclame l'inventaire.
  **La doctrine est écrite là où elle se lit** — Javadoc de `AnalysisPerimeter`
  (pourquoi le filtre est à la lecture et non au verdict, avec la mesure),
  Javadoc de `KnowledgeFact.GENERATED_CODE` et commentaires des deux packs
  (pourquoi ces entrées n'ont aucun sujet possible), et les trois scénarios de
  corpus passent de « divergence à trancher » à référence.
  **Dette laissée, à ne pas perdre** : « où est passé mon type ? » n'a pas de
  réponse aujourd'hui ; sa place est le canal de diagnostics du frontend, écart
  assumé de M2 à instruire à M5 avec l'hôte — un diagnostic, jamais un verdict.
- **Prochaine session : lot 21** (assemblage `ArchModel` depuis
  (`CodeModel`, `FactBase`, `Verdicts`), catégorisation des UNCLASSIFIED,
  `AnalysisRunner` par ServiceLoader et goldens `ArchModelSnapshots` avec la
  garde « golden existant »). Catégories à écrire : `TECHNICAL` depuis le fait
  S2 du même nom, `AMBIGUOUS` depuis l'agrégateur, `UNKNOWN` pour le vide —
  **pas de code généré, le moteur n'en voit aucun** (D15).

### 2026-08-03 — Jalon M3 : assemblage du modèle et corpus branché (lot 21, FAIT)

Trois commits (`332ed45`, `1fa043b`, `f643418`), `make ci` vert (**817 tests,
aucun skipped**), 0 warning sur build propre. **PIT engine 94 % → 95 %,
testkit 72 % → 76 %** (model 95 %, frontend 84 %, knowledge 88 %). Plancher du
cliquet inchangé : **143 relus, 143 passants** — l'assemblage ne déplace aucun
verdict, et c'était la vérification attendue.

- **Un point d'entrée, `Analysis.analyze(EngineContext)` → `ArchModel`**, dans
  l'ordre que le moteur impose : les verdicts d'abord (une règle qui lit le kind
  d'un voisin doit lire le définitif), puis une dernière saturation **contre ces
  verdicts stabilisés** parce que la base de faits de chaque tour est jetée et
  que seule la dernière décrit le code tel que les verdicts le lisent enfin,
  puis les records.
- **La ligne qui structure l'assemblage** (écrite dans la Javadoc de `Links`) :
  *ce qu'une règle a décidé se relit dans le lien que cette règle a énoncé ; ce
  que les sources disent en clair se relit sur la déclaration.* Recalculer une
  décision ici en ferait une seconde définition libre de diverger de la
  première ; relire une déclaration, c'est lire deux fois le même texte.
- **Conséquence : deux liens nouveaux**, sur le modèle de ce que R2 faisait déjà
  pour l'identité. `OWNS` (R3a/R3b, via un `Lifecycle.tie` partagé) parce que
  « ce qui compte comme partie » est une décision à trois exclusions — soi-même,
  un contrat, une valeur en forme d'identité — qu'un générateur ne peut pas
  redériver ; `ANNOUNCES` (R7, uniquement sur la lecture « rendu sans être
  gardé ») parce qu'un port qui annonce dit que l'événement sort, pas d'où il
  vient. Aucun verdict ne bouge : les lecteurs de `RELATION` filtrent `MANAGES`.
- **Six classes, chacune sur une question** : `Analysis` (l'enchaînement),
  `Links` (ce que l'analyse a atteint), `Structures` (la déclaration reportée
  telle quelle, plus le renversement des liens d'imbrication fait une fois),
  `Assembly` (l'aiguillage par anneau), `DomainAssembly` (les six records du
  domaine), `Fallback` (les catégories). L'aiguillage passe par
  `ArchKind.isDomain()/isPort()/isAdapter()/isApplication()` plutôt que par un
  switch à quatorze branches — PMD l'a demandé, et le résultat suit les branches
  scellées du modèle.
- **Ce que l'assemblage refuse de deviner**, et le dit avec du vide : un agrégat
  qu'aucun port ne garde n'a pas d'identité nommée ; une partie possédée par une
  partie ne nomme aucun agrégat (le remonter serait un pas que l'analyse n'a pas
  fait) ; une identité écrite autour de plusieurs valeurs n'a pas de valeur
  effective ; deux réponses à une question qui n'en admet qu'une donnent zéro
  (`Links.single`). **`timestampField` d'un événement reste vide** : rien dans
  l'analyse ne sait nommer une horloge, et une liste de types temporels en dur
  serait de la connaissance qui appartient à un pack.
- **Les trois catégories du doc 09, dans cet ordre** : `TECHNICAL` (fait S2 du
  même nom) d'abord — c'est du plombage correctement rangé, pas une lacune, donc
  **aucune remédiation** ; `AMBIGUOUS` (candidats sans marge) et `UNKNOWN` (le
  vide) ensuite, toutes deux avec la raison et l'unique remède que le moteur lit
  sans aucun voisin : déclarer le kind. Câbler le type et élargir le périmètre
  sont dans le texte du remède, pas dans une action — ils dépendent d'un code
  que le moteur ne regarde pas.
- **Le corpus s'exécute enfin** : `AnalysisChain` (hexaglue-acceptance,
  ServiceLoader) est le seul endroit qui voit les deux bouts de la chaîne, et
  `CorpusRun` lit désormais l'`ArchModel` au lieu des seuls verdicts — le
  scoreboard et les goldens mesurent la même chose. `Profile1CorpusTest`
  **quitte le testkit** (il n'y skippait par construction, faute de moteur sur
  son classpath) pour devenir `Profile1GoldenTest` dans le module qui a les deux
  côtés ; 143 snapshots enregistrés et relus.
- **La garde du piège §2.5** : `GoldenFiles.assertMatchesExisting` échoue sur un
  golden absent au lieu de l'écrire, et `assertMatches` garde son comportement
  pour les fixtures écrites à la main. L'enregistrement est une exécution qui se
  déclare (`-Dhexaglue.golden.regenerate=true`) : sur 143 fichiers, la relecture
  ne peut passer que par un diff, jamais par un test vert.
- **Le sérialiseur rendait la moitié du modèle** — verdicts, identité, propriétés
  — et rien des liens. Un golden pareil continuerait de matcher pendant que la
  composition change dessous, alors que ces liens sont l'essentiel de ce qu'un
  plugin lit. `ArchModelSnapshots` rend maintenant composition, agrégat
  possesseur, source d'événement, ways out injectés, agrégat géré, cas d'usage,
  types en entrée/sortie et raison du non-classement.
- **PIT a encore payé, et surtout en simplification.** Une garde réellement
  morte retirée : le repli de la « valeur effective » vers le type d'identité
  lui-même, inatteignable puisque R2 n'énonce `IDENTIFIED_BY` que d'une valeur
  enveloppant exactement une chose. Les autres survivants ont produit sept
  fixtures qui manquaient : identité déclarée en dernier champ (ce qui la porte
  se lit du lien, jamais de la position), constante statique dans un
  identifiant, identité à deux valeurs, partie possédée par une partie, adapter
  qui tient et implémente aussi ce qui n'est pas un port, type imbriqué avec
  superclasse et documentation, et un mapping vers un stockage qui reste
  `UNKNOWN` et non `TECHNICAL` (D7 vue depuis les catégories).
- **Découverte à ne pas perdre** : `Field.elementType` n'est jamais alimenté par
  le frontend, donc un champ `List<Hull>` sort du snapshot en
  `cardinality: SINGLE`. L'information est dans `TypeRef.typeArguments` (les
  règles la lisent via `unwrapElement`), c'est le raccourci du modèle qui reste
  vide — écart M2 à combler côté frontend, et les 143 goldens bougeront ce
  jour-là.
- **Prochaine session : lot 22** (corpus profils 2 et 3, D11) : fixtures
  petclinic-like et starwars-like, clés `profile2.*` / `profile3.*` au plancher,
  épingles D7/D8 sur le profil 2. À instruire au passage, recensé au lot 17 :
  les points d'entrée posés sur une **méthode** (`@KafkaListener` et consorts)
  échappent à W1-DA faute de sélecteur « annotation sur un membre », et le
  profil 2 est exactement là où ça se verra.

### 2026-08-03 — Jalon M3 : corpus profils 2 et 3 (lot 22, FAIT)

Trois commits (`700c09a`, `ceb63c2`, `1abc58b`), `make ci` vert (**831 tests,
aucun skipped**), 0 warning sur build propre. PIT inchangé : engine 95 %,
model 95 %, knowledge 88 %, frontend 84 %, testkit 76 %. Le cliquet compte
désormais par profil : **143/143 (profil 1), 6/6 (profil 2), 5/5 (profil 3)**.
D11 a son critère d'acceptation exécutable sur les trois populations.

- **La limite du lot 17 est levée, et par un sélecteur et non par un cas
  particulier.** `Selector.MemberAnnotated` (clé YAML `member-annotation`)
  répond quand une annotation est portée par un membre déclaré — méthode,
  constructeur ou champ. Le choix de ne pas se limiter aux méthodes est
  délibéré : où un framework attend son symbole est la règle du framework, et
  coder par fournisseur quel placement est le bon est exactement l'hypothèse
  qui a produit le défaut. Les trois écoutes Spring (`@KafkaListener`,
  `@JmsListener`, `@RabbitListener`) sont désormais énoncées **deux fois** dans
  le pack, une entrée par placement, parce que les deux placements sont deux
  affirmations qu'un lecteur peut contester séparément. Aucun golden profil 1
  ne bouge : la matière était absente du profil 1, elle arrive avec le profil 2.
- **Le corpus connaît ses profils.** `CorpusProfile` (enum), `Corpus.of(profile)`,
  `CorpusExpectations.of(scenario)`, un `profile` porté par `CorpusScenario`,
  goldens sous `golden/<profil>/`, `Profile1GoldenTest` → `CorpusGoldenTest`.
  Le scoreboard est **paramétré par profil et le plancher compte par profil** :
  un total unique laisserait un gain sur les sources écrites dans notre
  vocabulaire payer une perte sur celles qui n'en ont aucun.
- **Profil 2 (six scénarios, `Clinic-`) : le moteur lit une application
  d'entreprise sans en lire un seul nom.** Chaîne complète contrôleur →
  service → dépôt → agrégat, plus une passerelle mail derrière un contrat
  écrit à la main. D7 épinglée des deux côtés : le même `@Entity` donne
  AGGREGATE_ROOT quand un port le garde et UNCLASSIFIED quand seul un adapter
  le touche — ce qui les sépare n'est jamais l'annotation, c'est la position.
  D8 épinglée : contrôleur et détenteur de `JdbcTemplate` reçoivent leur kind.
  Le point d'entrée écrit sur la méthode est vert de bout en bout.
- **Profil 3 (cinq scénarios, `Armada-`) : dix types, quatre vagues, aucun nom
  qui dise un rôle.** Le scénario intégral (`Armada-theWholeArmada`) classe les
  deux côtés de l'hexagone avec pour seuls symboles de framework les deux qui
  placent l'anneau (ce qui répond en HTTP, ce qui tient un client de base). Le
  duel S4 d'un `record` autour d'un `UUID` est tranché par la clé que le port
  cherche, et par rien d'autre. Un scénario est écrit **contre** le nommage :
  une classe suffixée `Repository` qui n'en est pas une reste indécidée, et une
  interface sans suffixe est le port — matière directe pour le harnais du
  lot 23.
- **Trois constats à ne pas perdre**, tous consignés en Découvertes ci-dessus :
  l'entité dont l'identité est un type de plateforme nu se lit VALUE_OBJECT ;
  la raison « rien du périmètre ne l'utilise » est rendue même quand un adapter
  nomme le type dans une signature ; le lien d'identité vient de la déclaration
  Spring Data même vers un type externe, là où R2 se tait sur un `UUID` nu.
- **Prochaine session : lot 23** (harnais de réévaluation du nommage, clause de
  D13) — runner comparatif vocabulaire éteint vs allumé sur les trois profils,
  rapport *gain* / *dégât* par scénario, issue à consigner au registre (D13).
  Le profil 3 a été écrit en pensant à cette mesure.

### 2026-08-03 — Jalon M3 : harnais de réévaluation du nommage (lot 23, FAIT)

Un commit (`05fda64`), `make ci` vert (**854 tests, aucun skipped**), 0 warning
sur build propre. Plancher inchangé (143/143, 6/6, 5/5) : le harnais mesure,
il ne déplace rien. **La clause de D13 a son issue, et elle est consignée au
registre.**

- **Le harnais** : `NamingVocabularyTest` passe les 154 scénarios relus deux
  fois dans la chaîne complète — `ClassificationConfig.defaults()` puis
  `conventional()` — et, pour chaque verdict qui bouge, demande au relecteur
  qui avait raison. Cinq issues : *gain* (le nom atteint la réponse relue que
  la position manquait), *damage* (il contredit une réponse que la position
  atteignait), *unarbitrated* (le verdict bouge sur un type dont personne n'a
  parlé — jamais compté au score), *neither*, *indifferent*.
  `AnalysisChain.modelOf` prend désormais une posture de classification en
  paramètre : une question sur le moteur qui ne peut se poser qu'en le lançant
  deux fois.
- **Le rapport est un golden** (`golden/naming-vocabulary.txt`), pas un seuil.
  Le nombre n'est pas une règle à tenir : c'est la pièce d'un dossier tranché
  ailleurs, et ce qui compte au prochain passage est que le diff montre ce qui
  a bougé.
- **La mesure crédibilise sa propre nullité** : `NamingShiftTest` exerce les
  cinq issues séparément, contre des claims écrites sur place. Sans lui,
  « gain 0 » se lirait aussi bien comme « le harnais ne sait pas voir un gain ».
- **Résultat : 0 gain partout ; 55 dégâts sur le profil 1, rien sur les
  profils 2 et 3.** Tous les mouvements sont de la forme `UNCLASSIFIED → un
  kind`. Sur du code câblé (profils 2-3), le vocabulaire a de quoi parler
  (`OwnerRepository`, `OwnerService`) et dit exactement ce que la position
  disait déjà : il est redondant. Sur les fixtures mono-type héritées, il
  conclut là où le relu dit que le silence est la réponse — et plusieurs de ces
  conclusions sont fausses au fond, pas seulement bruyantes (interfaces
  marqueurs lues DOMAIN_EVENT, un type de `ports.in` lu DRIVEN_PORT,
  `PlaceOrderUseCase` lu DRIVING_PORT dans le scénario même qui affirme
  l'inverse).
- **Ce que la mesure ne dit pas**, et qu'il ne faut pas sur-lire : les 55
  dégâts portent sur des fixtures unitaires héritées, pas sur du code de
  production. Elles établissent le mode d'échec que D13 nommait, pas que le
  nommage nuirait sur un parc réel — sur un parc réel, il ne fait rien.
- **Limites énoncées dans le rapport lui-même** : la comparaison porte sur les
  *verdicts*, pas sur les candidats ni la confiance derrière eux ; et le
  périmètre est le corpus relu — les `examples/` du doc 10 vivent dans la
  carrière gelée et ne sont pas branchés au nouveau réacteur (à instruire si
  un profil 4 « exemples » est un jour importé).
- **Les trois résidus du doc 09 §6 sont couverts** : le duel d'un wrapper non
  possédé (S6 le tranche, et le relu dit qu'il doit rester ouvert),
  COMMAND/QUERY_HANDLER (une occurrence, en dégât), les types définitivement
  isolés (les 55).
- **Prochaine étape : la clôture de M3** (règle de conduite 13 : corpus vert +
  revue contre les interdits du doc 07 §10 + journal), qui demande le feu vert
  de l'utilisateur comme pour M1. Les lots 15-23 du doc 10 sont tous faits ;
  S5 structurel reste reporté à M5. Ensuite M4 (Explain + CLI).

### 2026-08-03 — Jalon M3 : CLÔTURE

Règle de conduite 13 remplie : corpus vert, revue contre les interdits du
doc 07 §10, journal à jour. Un commit de clôture (`27eac65`).

**État mesuré à la clôture** : `make ci` vert — **854 tests, aucun skipped**,
0 warning sur build propre, PMD/SpotBugs/checkstyle/doclint OK. Cliquet :
143/143 (profil 1), 6/6 (profil 2), 5/5 (profil 3) ; 154 goldens relus.
PIT : engine 95 %, model 95 %, knowledge 88 %, frontend 84 %, testkit 76 %.
Une décision PENDING au registre à la clôture : **D16**, ouverte par la mesure
du lot 22 et sans effet sur M4 (à trancher au plus tard à M6, de préférence
avant M7).

**Revue contre les dix interdits (07 §10)** — vérifiée dans le code, pas de
mémoire :

1. *Abstraction sans second consommateur* — RAS. `Rule` a une vingtaine
   d'implémentations, `Fact` quatre, `Selector` cinq ; `AnalysisRunner` garde
   sa raison d'être (le testkit publie le corpus sans moteur, l'acceptance
   lie les deux).
2. *Pivot `String` entre deux étages* — RAS côté production : `Fact` est
   scellé, `Predicate` et `KnowledgeFact` sont des enums, l'identité est
   `TypeId`. Les seules chaînes sont dans le harnais de test, qui lit un
   fichier de claims écrit à la main — c'est un format, pas un pivot.
3. *`catch (Exception)` global* — aucun dans les quatre modules de production
   (vérifié par recherche).
4. *Annotation appariée par nom simple* — impossible par construction :
   `Symbols` refuse un nom simple à la construction du sélecteur, et un test
   l'épingle pour les cinq formes.
5. *Plugin qui re-dérive une information du modèle* — sans objet à M3 (aucun
   plugin) ; l'analogue interne est tenu depuis le lot 21 par la ligne de
   `Links` : une décision de règle se relit dans le lien que cette règle a
   énoncé.
6. *Deux implémentations vivantes du même concept* — **un cas trouvé et
   corrigé** : « le kind qu'un modèle donne à un type, ou l'absence de
   verdict » existait deux fois dans `hexaglue-acceptance` (cliquet et
   harnais de nommage), avec la sentinelle `NO VERDICT` écrite en dur des
   deux côtés alors que c'est `CorpusExpectations` qui en porte le contrat.
   La sentinelle est désormais une constante de ce contrat et la lecture est
   `CorpusRun.kindIn` — une seule. Les utilitaires du moteur (`Shapes`,
   `Signatures`, `Contracts`, `Lifecycle`) restent la lecture unique de leur
   question respective.
7. *Code mort publié* — rien n'est publié (D5). Le seul code sans sujet
   possible est `KnowledgeFact.GENERATED_CODE` et ses quatre entrées de pack,
   **écart assumé et décidé** : c'est le prix conservé de D15, commenté comme
   tel.
8. *Décision de classification sans évidence tracée* — RAS : toute
   `KindEvidence` porte une `Evidence` (palier, confiance, motif, source),
   l'agrégateur conserve les candidats, `explain` rend l'arbre de preuve, et
   les catégories d'UNCLASSIFIED portent raison et remédiation.
9. *Règle de nommage hors du vocabulaire configurable* — RAS, et c'est le
   point le plus surveillé du jalon. Recherche exhaustive de
   `endsWith`/`startsWith`/`Pattern` dans les trois modules de production :
   sept occurrences, toutes légitimes — une seule lit un nom de type,
   `ConventionalName`, contre le vocabulaire configuré. Les autres sont le
   périmètre de lecture (`Perimeter`), la validation de forme d'un identifiant
   (`Symbols`, `KnowledgePack`, `IssueCode`) et `Selector.PackagePrefix`. Ce
   dernier mérite d'être distingué : apparier un package **vendeur** est de la
   connaissance de framework, donnée d'un pack remplaçable par l'utilisateur —
   à ne pas confondre avec la moitié conventionnelle de S5 (des mots dans les
   packages **de l'utilisateur**, `ports.in`, `.domain.`), qui reste différée.
10. *Échec de construction avalé sans diagnostic* — RAS : HG-KNOWLEDGE-001..004,
    HG-ENGINE-001/002, HG-FRONTEND-001/002/003, tous codés et testés.

**Revue contre la cible du jalon (07 §4.1)** : faits typés, règles en classes
Java déclarant leurs prédicats, évaluation semi-naïve, stratification
S0→S1→décision→propagation, terminaison par monotonie, preuves par arbre,
déterminisme par ordre stable — tout est livré. Deux écarts de forme assumés
et documentés : les verdicts vivent **hors** de la base de faits (un fait
s'accumule, un verdict se remplace — `Classifier` rejoue chaque tour depuis
une base vide), et l'agrégateur est un composant et non une strate de règles,
ce que le doc prévoyait déjà (« pas une règle »).

**Écarts assumés reportés, tous déjà inscrits** : S5 structurel (modules non
alimentés avant l'hôte, M5) ; canal de diagnostics du frontend (M5, dette de
D15) ; choix des racines de sources (M5) ; `Field.elementType`/`wrappedType`/
`roles` jamais alimentés par le frontend (écart M2, les 154 goldens bougeront
le jour où il sera comblé) ; COMMAND/QUERY_HANDLER en retrait (doc 09 §4) ;
et la question ouverte du lot 22 — une entité dont l'identité est un type de
plateforme nu se lit VALUE_OBJECT (**portée au registre comme D16, PENDING**).

**M3 est clos.** Prochaine étape : **M4 — Explain + CLI**.

### 2026-08-03 — Jalon M4 : Explain, la restitution (FAIT)

Le jalon a été **redéfini à son ouverture**, sur une objection instruite dans
le code puis tranchée par l'utilisateur (**D17**).

- **Ce que la lecture du code a montré** : l'arbre de preuve était une **sortie
  morte**. Chaque règle en construit un, chaque `Classification` le transporte,
  il traverse les 154 scénarios — et rien ne le lisait : aucun golden ne le
  portait, le sérialiseur de snapshot l'ignorait, les tests du moteur y
  touchaient cinq fois dont deux sur sa structure. Neuf lots de M3 avaient
  produit des preuves que personne n'avait jamais regardées.
- **Ce qui a fait tomber le CLI du jalon** : c'est un **hôte**, et toutes les
  décisions d'hôte sont à M5 (YAML strict, racines de sources, canal de
  diagnostics, sinks). L'écrire avant, c'est l'écrire deux fois ou préempter M5
  depuis un module qui ne portera pas ces décisions. Sa justification au doc 08
  était un argument de calendrier, pas de dépendance : aucun jalon aval ne
  l'attend. Et `audit`, troisième commande annoncée, n'aurait rien eu à lire —
  le plugin qui produit les findings est à M6.
- **Ce que l'utilisateur a énoncé, et qui a décidé la forme du jalon** :
  HexaGlue passera principalement par le plugin Maven, la possibilité d'un CLI
  reste ouverte, et ce qu'il faut dès maintenant est un moyen d'obtenir les
  informations de classification qui alimente **aussi bien** les logs du plugin
  Maven que ceux d'un CLI. Soit : une restitution **indépendante de l'hôte**.
  D17 consignée CONFIRMÉE ; doc 08 amendé (M4 et M5).

**Quatre commits** (`c9bba94`, `69affda`, `16a829c`, `bc19f11`) :

- **`Explanation`** (`hexaglue-engine`) : `of(ArchType)` rend le verdict, ses
  raisons par palier, la localisation, les types que chaque raison nomme, les
  candidats d'une décision ambiguë, la catégorie et la raison d'un UNCLASSIFIED
  et ses remédiations ; `withDerivation(ArchType)` y ajoute l'arbre indenté ;
  `of(Outcome)` rend le bilan d'un run.
- **`Outcome`** : le bilan agrégé — un décompte par kind, la répartition du
  fallback par catégorie, déclaré contre inféré, et les décisions laissées
  ambiguës. C'est la donnée ; le rendre est le travail d'`Explanation`. La
  « section fiabilité » que M6 réclamera lit le même objet.
- **Le rendu est une feuille du pipeline, jamais un étage.** Les hôtes reçoivent
  une `List<String>` — un plugin Maven fait `forEach(log::info)`, un CLI joint,
  un rapport indente — et la structure reste lisible séparément (`ArchType`,
  `Classification`, `Outcome`). Aucun consommateur ne relit le texte : c'est ce
  qui tient l'interdit 07 §10.2 et la leçon 05-H2.
- **Cliquet** : `explanation-profile{1,2,3}.txt` (2 924 lignes de golden), plus
  quatre invariants tenus sur **tous** les types de **tous** les scénarios —
  l'en-tête a la forme attendue et nomme le type demandé, aucune ligne n'est
  vide ni ne traîne d'espace, demander la dérivation n'enlève jamais rien à ce
  que le verdict disait. Plus un double run sur des arbres de sources distincts,
  octet à octet.

**Ce que la relecture du golden a corrigé** (commit `16a829c`) : la
localisation portée par une évidence est celle de la **déclaration du sujet**,
pas du signal, donc elle était réimprimée sous chaque raison en répétant
l'en-tête ; et `involving` citait parfois le type expliqué lui-même. Les deux
sont désormais tus. Ce qui reste sous une raison est un endroit où aller
ensuite, ce qui est la seule chose qui vaille une ligne de plus.

**Revue contre les interdits 07 §10** :

1. *Abstraction sans second consommateur* — RAS, et c'est l'argument qui a
   renversé mon objection initiale : `Explanation` a **trois** consommateurs
   réels (corpus aujourd'hui, plugin Maven à M5, rapport d'audit à M6). Aucune
   interface `Renderer`, aucune stratégie de format : un seul rendu, en dur.
2. *Pivot `String`* — RAS par construction (voir ci-dessus). Le seul endroit qui
   regarde le texte est une **assertion** de test (l'expression régulière de
   l'en-tête), pas un étage qui le consomme.
3. *`catch (Exception)` global* — aucun ajouté.
4. *Annotation par nom simple* — sans objet.
5. *Plugin qui re-dérive* — sans objet ; `Outcome.of` lit le modèle, il ne
   reclassifie rien.
6. *Deux implémentations d'un même concept* — le point le plus surveillé.
   `ArchModelSnapshots` (testkit) sérialise le modèle en JSON pour comparaison
   machine et **ne porte aucune preuve** ; `Explanation` rend une restitution
   humaine avec la dérivation. Contenus, audiences et emplacements distincts.
   **Consigne pour M6** : un rapport JSON se dérive du modèle, jamais de l'un de
   ces deux-là, et le sérialiseur de snapshot reste au testkit.
7. *Code mort publié* — RAS, tout est consommé par les tests ou les goldens.
8. *Décision sans évidence tracée* — M4 n'ajoute aucune classification.
9. *Règle de nommage hors vocabulaire* — RAS : `Explanation` ne fait aucun
   `endsWith`/`startsWith`/`contains` sur un nom ; ses seules comparaisons sont
   des égalités de `TypeId` et de `SourceLocation`.
10. *Échec avalé sans diagnostic* — le rendu d'un run vide dit « no type was
    analysed » plutôt que d'imprimer un tableau vide.

**Ce que M4 a rencontré sans le traiter** : rien ne dit encore *pourquoi un
type est absent* du rapport. `Explanation` ne parle que de ce que le modèle
contient ; hors périmètre, code généré et récupération partielle au parsing
restent muets. C'est le canal de diagnostics du frontend, dette de M5, déjà
inscrite — la rencontre était annoncée au point de reprise de M3.

**Un défaut de forme du moteur, constaté et non corrigé** : les justifications
des règles disent « is a AGGREGATE_ROOT » là où l'anglais veut « an ». Le
corriger touche les règles et déplacerait les trois goldens de restitution.

**Le score interne, corrigé après coup** (commit `2c2f4fb`, à la demande de
l'utilisateur — la ligne était entrée dans le README public). La ligne de
décision affichait `[decided on 11000 at distance 0]` : un entier positionnel,
une décimale par palier, qui est la **clé de comparaison** de l'agrégateur et
rien d'autre. Elle dit désormais ce que ce nombre encode —
`[decided on 1 signal at S2, 1 at S3]` — et ne mentionne la distance que
lorsqu'un signal n'a pas été trouvé sur le type lui-même
(`, nearest 2 steps away`). Le même nombre affleurait une seconde fois, en
`candidate ENTITY (score 1000)` dans la restitution : lui aussi rend maintenant
les paliers. **Une seule lecture pour les deux**, `Tiers.carrying(evidences)`
(interdit 07 §10.6) ; `Candidate.score` reste au contrat, c'est lui qui explique
l'ordre de la liste.
- *Ce que le cliquet a prouvé au passage* : les trois goldens ont refusé
  l'ancienne forme, et le diff est **208 lignes remplacées, 208 — exclusivement
  des lignes `decided on` et `candidate`**. Aucun verdict n'a bougé.
- *À savoir* : aucun scénario du corpus n'a de signal à distance non nulle, donc
  la branche `nearest N steps away` n'est tenue que par un test unitaire.
- *Mesure* : `make ci` vert, **892 tests**, aucun warning ; PIT engine 95 %,
  `Tiers` 5/5 et `Explanation` 23/23 mutants tués. `Aggregator` est à 31/35, et
  les quatre survivants sont **antérieurs** (dans `rank` et `ambiguous`, non
  touchés) : trois construisent la phrase `[AMBIGUOUS between … and …]`, que les
  goldens d'acceptation couvrent mais que PIT ne voit pas, son périmètre
  s'arrêtant au module.

**Les logs rendus lisibles sans légende** (commits `c32e408` et `09ca88d`, à la
demande de l'utilisateur : « on ne comprend pas S1, S2… ni la hiérarchie entre
eux »). Trois constats, en creusant :

- **Le jeton `S<n>` désignait deux choses dans la même sortie** : les paliers
  d'évidence (S1 à S6) et des identifiants de règles (`S1-CONFIG`, `S1-INTENT`,
  `S3-DECISION`, `S4-SHAPE`, `S6-NAMING`). Pire qu'une collision franche :
  `S4-SHAPE` émet bien du S4 et `S6-NAMING` du S6, donc le lecteur apprend la
  correspondance, puis rencontre `[S3-DECISION] … [decided on … at S3]` où
  `S3-DECISION` est l'agrégateur — une strate du solveur — sans rapport avec le
  palier S3 de la même ligne. Une convention qui tient cinq fois sur six trompe
  davantage qu'une convention absente. **Les cinq identifiants sont renommés**
  (`DECISION`, `CONFIG`, `INTENT`, `SHAPE`, `NAMING`) ; le doc 09 ne cite que
  `R*`, `W1-*` et `W2-X`, donc l'opération ne coûte aucune retouche de doctrine.
- **Les paliers sont nommés plutôt que codés** — `[framework knowledge]` — par
  un libellé **dérivé du nom de la constante d'enum** : aucun vocabulaire
  inventé, aucun changement de contrat, une seule lecture (`Tiers.named`).
  `EvidenceTier.code()` reste en place : S1-S6 indexe dans le référentiel et
  dans les règles, il a seulement quitté les logs, où il n'expliquait rien.
- **La hiérarchie est dite**, engendrée depuis l'ordre de déclaration de l'enum
  — qui *est* l'ordre de pesée, donc la ligne ne peut pas dériver de ce qu'elle
  décrit. Sur les verdicts comme dans la dérivation, mais **seulement quand deux
  familles de signaux au moins ont été pesées l'une contre l'autre**, candidats
  d'un AMBIGUOUS compris : sur un verdict mono-palier, aucune comparaison n'a eu
  lieu et la légende expliquerait un arbitrage qui n'a pas existé.
- **Chaque règle dit ce qu'elle conclut.** `Rule.title()` est **obligatoire** sur
  l'interface : un intitulé qui vit à côté de la logique est corrigé par qui
  change la logique, là où un catalogue tenu ailleurs vieillit à la première
  réécriture. Les vingt phrases viennent de la Javadoc que chaque règle avait
  déjà écrite sur elle-même. Rendu en bloc `rules cited:` **sous** l'arbre, une
  entrée par règle en ordre de première apparition — pas sur chaque nœud, qui
  répéterait quatre fois la phrase d'une règle tirant quatre fois. `Titles` ne
  fait que l'indexation par `RuleId` (une preuve transporte des identifiants,
  pas les règles qui les ont produits) ; `Aggregator.TITLE` porte la décision,
  qui n'est pas une règle. Le `d0` disparaît aussi quand la distance est nulle.
- **Innocuité prouvée par le cliquet** : pour les intitulés, **599 lignes
  ajoutées et zéro supprimée** ; pour le renommage, aucun en-tête de verdict ni
  aucun décompte de run touché. `make ci` vert, **898 tests**, aucun warning.

**Dépôt poussé et dépendances migrées** (2026-08-03, fin de session). Les neuf
commits de la session sont sur `origin/main`, puis les deux PR Dependabot
mergées **en squash** — historique linéaire préservé, zéro merge commit :
`#5` classgraph 4.8.180→4.8.186, `#6` snakeyaml-engine **2.10→3.0.1, un saut de
majeure** sur le parser YAML de `hexaglue-knowledge`.

- **Méthode à reprendre** : la CI verte d'une PR Dependabot ne prouve rien sur
  l'état courant — elle a tourné sur une base antérieure aux commits locaux.
  Éprouver les montées **localement** (appliquer les propriétés au POM, `make
  ci`, vérifier par `dependency:tree` la version **réellement résolue** et non
  un repli silencieux, puis `git checkout pom.xml`), et seulement ensuite
  merger.
- **Deux PR de dépendances se gênent toujours** : elles modifient le même bloc
  `<properties>`, donc merger la première rend la seconde `CONFLICTING`.
  Commenter `@dependabot rebase`, attendre le changement de tête **et** les
  quatre contrôles, ne jamais pousser soi-même sur la branche du bot.
- État final : aucune PR ouverte, `main` local et distant identiques, arbre
  propre, `make ci` vert (898 tests, aucun warning).

**M4 est clos.** Prochaine étape : **M5 — SPI, sinks, gates validate,
maven-plugin**, qui est aussi le jalon des décisions d'hôte.

### 2026-08-03 — Jalon M5 : ouverture, l'hôte (D18-D21)

Le jalon a été **redéfini à son ouverture**, comme M4 : instruction du code
d'abord, arbitrage utilisateur ensuite.

**Ce que l'instruction a montré** (vérifié dans le code, pas dans le doc) :

- `ValidationConfig` et `GenerationConfig` sont **forme seule depuis M1** :
  personne ne les lit. M5 donne son consommateur au premier ; le second attend
  M7.
- `ModuleNode`/`ModuleDescriptor`/`ModuleTopology` sont livrés et testés au
  modèle et n'ont **aucun producteur** — le frontend n'en construit jamais.
- `SpoonFrontend.analyze` rend un `CodeModel` nu ou lève : rien ne dit ce qui a
  été écarté.
- La carrière passe **toutes** les racines de compilation,
  `target/generated-sources` comprise, et charge le YAML en permissif (warn plus
  défauts silencieux).
- Le SPI n'a **aucun consommateur possible à M5** : pas de module plugin au
  réacteur, rien à écrire (la validation ne produit qu'un verdict, et le rapport
  de `validate` se tire de la restitution de M4), et sur les 43 `test-param-*`
  de la carrière, **34 sont des paramètres de plugins** jpa/rest/livingdoc/audit.

**Quatre décisions au registre** : **D18** (le SPI et l'exécution des plugins
passent en tête de M6 ; M5 est le jalon de l'hôte), **D19** (racine de sources
déclarée seule, delombok substitué, jamais une racine sous le répertoire de
build), **D20** (le frontend rend un résultat porteur : modèle + diagnostics
codés), **D21** (topologie alimentée et S5 structurel à M5, goals réacteur avec
leur sortie à M6/M7).

**Plan de lots** :

| Lot | Contenu | Où |
|---|---|---|
| 1 | Le frontend dit ce qu'il écarte : `analyze` rend le modèle **et** ses diagnostics codés — hors périmètre de package, code généré, racine offerte non lue | `hexaglue-frontend`, chaîne d'acceptation |
| 2 | Les gates, politique du moteur : `failOnUnclassified`, `minConfidence` (ports inclus), `failOnAmbiguous`, `allowInferred` ; chaque refus porte la remédiation du type concerné | `hexaglue-engine` |
| 3 | Chargement de configuration en YAML strict : `hexaglue.yaml` → `HexaGlueConfig` ; clé inconnue ou valeur malformée = erreur codée, jamais un défaut silencieux | hôte |
| 4 | Le plugin Maven mince : goal `validate`, racines D19, classpath du projet, restitution M4 dans les logs, rapport écrit par l'hôte | `hexaglue-maven-plugin` |
| 5 | Topologie de modules alimentée par l'hôte + règle S5 structurelle — muette en mono-module, et le plancher du cliquet ne doit pas bouger | hôte + `hexaglue-engine` |
| 6 | Les neuf `test-param-*` d'hôte portés en tests d'intégration ; revue contre les interdits 07 §10 et clôture | intégration |

Les lots 1 à 3 sont indépendants ; le lot 4 les consomme tous les trois.

**Ordre réel : 1, 2, 4, 3.** Un module `maven-plugin` sans mojo ne construit pas
(`No mojo definitions were found`), donc le chargeur de configuration n'avait où
vivre qu'une fois le goal écrit. Les deux lots restent homogènes et verts
séparément : « l'hôte fait tourner l'analyse et applique les gates », puis
« l'hôte lit sa configuration ».

**Faits (2026-08-03)** — `make ci` vert à chaque lot, **947 tests** (898 à la
clôture de M4), aucun warning de compilation, goldens inchangés :

- **Lot 1** (`ac34e38`) — `SpoonFrontend.analyze` rend un `FrontendResult`
  (modèle + diagnostics). Trois causes codées : HG-FRONTEND-004 (hors périmètre
  de package), HG-FRONTEND-005 (code généré, marqueur nommé), HG-FRONTEND-006
  (récupération au parsing). **La troisième a été mesurée avant d'être écrite** :
  le compteur de Spoon vaut 0 sur des sources propres, 0 sur des références non
  résolues (le régime normal sans classpath complet) et 3 sur un fichier cassé —
  donc le signal est précis et le canal ne crie pas sur tous les projets.
- **Lot 2** (`504f1a1`) — `Validation.of(model, gates)` dans `hexaglue-engine` :
  `Gate` (UNCLASSIFIED, CONFIDENCE, AMBIGUOUS, INFERRED), une `Refusal` par
  (type, gate) — les gates sont des conditions indépendantes, en désarmer une ne
  doit pas changer ce que les autres disent. Le refus porte le type, donc la
  remédiation rendue est celle que le moteur a écrite pour lui. `Explanation.of(
  Validation)` groupe par type au rendu. `findingThresholds` reste sans sujet :
  rien ne produit de finding.
- **Lot 4** (`dd4d698`) — module `hexaglue-maven-plugin`, goal `validate`.
  `ProjectSources` applique D19 (racine déclarée, delombok substitué, jamais une
  racine sous `target/`) ; `ProjectAnalysis` est la chaîne testable hors build ;
  le Mojo est un adaptateur (paramètres → lignes de log → condition de sortie).
  Deux points de garde-fou : le bannissement transitif de Spoon mordait l'hôte
  (qui doit faire tourner le frontend), scindé en « Lombok banni transitivement »
  + « le parser n'est pas déclaré ici » ; et la classe de goal est exclue du
  plancher de couverture, ce que les tests d'intégration couvriront.
- **Lot 3** (`1d1dd58`) — `ConfigLoader` lit `hexaglue.yaml`/`.yml` strictement
  (clé inconnue, valeur de mauvaise forme, mot hors vocabulaire, clé dupliquée →
  `ConfigException` codée HG-CONFIG-001/002/003), `YamlDocument` porte la
  mécanique de lecture stricte séparément du sens des clés. Les paramètres Maven
  priment sur le document **et seulement s'ils sont posés** : `Boolean` plutôt
  que `boolean`, pour qu'un paramètre laissé tranquille ne désarme pas une porte
  que le document a armée.

**Ce que M5 ne fait pas, et pourquoi c'est écrit** : aucun sink, aucun manifest
de plugin, aucun ordonnancement — D18. Aucun goal `reactor-*` — D21. Aucune
clé de configuration nouvelle pour ajouter une racine de sources — D19.
`ValidationConfig.findingThresholds` reste sans sujet jusqu'à M6 : les gates de
M5 portent sur les statistiques de classification, pas sur des findings qui
n'existent pas encore.

### 2026-08-03 — Jalon M5 : CLÔTURE

**État mesuré** : `make ci` vert — **951 tests**, aucun skipped, aucun warning de
compilation, PMD/SpotBugs/checkstyle/doclint OK ; **7 cas d'intégration sur 7**
(builds Maven réels, ~7 s). Six commits : `ac34e38`, `504f1a1`, `dd4d698`,
`1d1dd58`, `901949d`, `c306e8c`. Cliquet inchangé — 143/143, 6/6, 5/5, 154
goldens : M5 n'a déplacé aucun verdict, ce qui est exactement ce qu'un jalon
d'hôte doit prouver.

**Deux décisions de plus, toutes deux sur mesure du code** :

- **D21 amendée** : la topologie de modules et S5 partent à M6.
  `TypeNode.moduleName` n'est alimenté par personne, le modèle ne porte aucune
  dépendance inter-modules, `Assembly` ne construit aucune `ModuleTopology`,
  aucune règle ne lit de rôle — le substrat n'existe pas et le premier
  consommateur est le rapport agrégé.
- **D22** : le moteur rend ses propres diagnostics. Le lot 4 avait mis au jour,
  par un test, que le périmètre est lu à deux endroits : le frontend filtre sur
  include/exclude, le moteur filtre **en plus** sur `basePackage`. Un type hors
  du `basePackage` était lu, absent du modèle, et personne ne le disait.
  `Analysis.analyze` rend désormais un `AnalysisResult` (modèle + diagnostics),
  symétrique de D20 : **celui qui écarte est celui qui le dit**.

**Lot 5** (`901949d`) — `Perimeter.excluded()` porte la raison, `AnalysisResult`
la remonte, code HG-ENGINE-003, trois appelants adaptés. Les goldens ne bougent
pas : un diagnostic n'est pas un verdict.

**Lot 6** (`c306e8c`) — harnais `maven-invoker` (`src/it`), sept cas qui font
tourner de vrais builds : kind déclaré honoré, porte armée par le document qui
casse le build (avec la remédiation dans le log), paramètre qui prime sur le
document, `allowInferred: false`, `skip`, clé mal orthographiée refusée, et le
périmètre qui se rend compte de lui-même. Cible `make integration` ajoutée ; les
cas tournent aussi dans `make ci`.

**Le portage des `test-param-*` d'hôte, cas par cas** (critère de sortie de
D18 : neuf cas) :

| Cas de la carrière | Sort en v7 |
|---|---|
| `classification-explicit` | porté — `classification.explicit` |
| `classification-fail-unclassified-yaml` | porté — la clé passe de `classification.validation.*` à `validation.*` |
| `classification-no-inferred` | porté — `validation.allowInferred` |
| `fail-on-unclassified` | porté — et la **précédence** paramètre/document est ce que le cas prouve |
| `skip` | porté |
| `skip-validation` | **sans objet** : un seul goal, `skip` le couvre |
| `classification-exclude` | **écart assumé** : la carrière excluait des types par motif (`*.shared.DomainEvent`) ; `AnalysisScope` refuse les globs par construction et n'exclut que par préfixe de package. Aucune exclusion type par type en v7 — à porter au gate de parité (M8) |
| `validation-report-path` | **écart assumé** : `validate` rend un verdict et des logs, pas un fichier. Le rapport est le produit de l'audit (M6) |
| `output-directory` | **hors périmètre** : génération, M7 |

Deux cas que la carrière n'avait pas : `config-unknown-key` (le YAML strict) et
`perimeter-accounts-for-itself` (D22).

**Revue contre les dix interdits (07 §10)** — vérifiée dans le code :

1. *Abstraction sans second consommateur* — RAS, et c'est ce que D18 a retiré du
   jalon. Aucune interface nouvelle : `FrontendResult`, `AnalysisResult`,
   `Validation`, `Gate`, `YamlDocument` sont des records, des enums ou des
   classes finales, tous consommés.
2. *Pivot `String`* — RAS. L'hôte journalise les `List<String>` de `Explanation`
   et ne les relit jamais ; le seul code qui regarde du texte est un
   `verify.groovy` d'intégration, c'est-à-dire une assertion. Le YAML est un
   format lu une fois, pas un pivot entre deux étages.
3. *`catch (Exception)` global* — aucun ajouté ; le seul reste celui du parser
   (M2), commenté et converti en diagnostic codé.
4. *Annotation par nom simple* — sans objet.
5. *Re-dérivation par un consommateur du modèle* — **c'est la raison du choix
   de D22** : faire compter l'écart à l'hôte l'aurait fait re-dériver une
   information du modèle.
6. *Deux implémentations d'un même concept* — deux points examinés, tous deux
   assumés et justifiés. `AnalysisPerimeter.Exclusion` (frontend) et
   `Perimeter.Exclusion` (moteur) portent le même nom mais répondent à deux
   questions distinctes — ce qui n'a pas été **lu**, ce qui n'a pas été
   **classé** — et chacune formule ses propres raisons. `YamlDocument` (hôte) et
   la mécanique interne de `PackLoader` (knowledge) lisent tous deux du YAML
   strictement, sur deux documents, deux vocabulaires et deux familles de
   diagnostics ; **la partie commune fait une quarantaine de lignes de
   vérification de forme**. À surveiller : un troisième document appellerait
   l'extraction.
7. *Code mort* — **un trouvé et retiré à la revue** : `YamlDocument.origin()`,
   accesseur que personne n'appelait.
8. *Décision sans évidence tracée* — M5 n'ajoute aucune classification.
9. *Règle de nommage hors vocabulaire* — RAS. Recherche exhaustive : le seul
   `startsWith` ajouté est celui du périmètre du moteur, qui apparie un
   **préfixe de package configuré**, pas une convention ; `ConventionalName`
   reste la seule lecture d'un nom de type.
10. *Échec avalé sans diagnostic* — RAS : HG-CONFIG-001/002/003 et
    HG-ENGINE-003 s'ajoutent aux codes existants, et le goal échoue en nommant
    ce qu'il a refusé.

**Écarts assumés reportés** : le rapport de validation en fichier et l'exclusion
type par type (tableau ci-dessus, gate de parité M8) ; la topologie de modules et
S5 (D21 amendée, M6) ; `findingThresholds` sans sujet jusqu'à M6 ; la lecture
hiérarchique de `hexaglue.yaml` (module puis racine du réacteur) que la carrière
faisait et que M5 ne fait pas — le document est cherché à côté du POM analysé,
ce qui suffit au mono-module et sera repris avec les goals réacteur.

**M5 est clos.** Prochaine étape : **M6 — SPI + living-doc + audit**, le SPI en
tête de jalon (D18) et la topologie de modules avec lui (D21).

### 2026-08-03 — Jalon M6 : ouverture, SPI + living-doc + audit (D16, D23-D25)

Jalon **cadré à son ouverture**, comme M4 et M5 : instruction du code d'abord,
arbitrage utilisateur ensuite.

**Ce que l'instruction a montré** (vérifié dans le code, pas dans le doc) :

- **Rien de M6 n'existe au réacteur** : aucun module SPI, aucun module plugin,
  aucun module de rendu. `Finding`, `IssueCode` et `Severity` sont au modèle
  depuis M1 avec **zéro producteur** ; `findingThresholds` est lu par
  `ConfigLoader`, passé par `ValidateMojo` et **consommé par personne** ;
  `ModuleTopology`/`ModuleNode`/`ModuleDescriptor`/`ModuleRole` n'ont toujours
  aucun producteur et `TypeNode.moduleName` n'est jamais alimenté — exactement
  ce que D21 amendée avait constaté.
- **Ni JGraphT ni Jackson** au `dependencyManagement` : les deux entrent à M6
  (doc 07 §7).
- **La carrière est lourde et inégale** : `hexaglue-plugin-audit` pèse
  **26 858 lignes** — 15 contraintes (8 `ddd:*`, 7 `hexagonal:*`), 20
  calculateurs de métriques, 10 constructeurs de diagrammes, 4 renderers,
  ~60 records de rapport, plus un estimateur de dette, un comparateur
  d'historique, un générateur de configuration CI et un moteur de
  recommandations. `hexaglue-plugin-living-doc` en pèse 6 627, dont les DSL
  markdown et Mermaid. Le `hexaglue-spi` de la carrière compte 32 fichiers sur
  six packages, très au-delà du contrat du doc 07 §6.1.
- **14 `test-param-*` relèvent du jalon** : 8 `audit-*`, 6 `livingdoc-*` (sur
  les 34 de plugins recensés par D18).
- **Le doc 07 se contredit sur les findings** : §3.3 les dit produits par les
  règles d'audit, §2.2 et §6.3 font gater la validation dessus alors qu'elle
  « n'est pas un plugin ». Les deux ne tiennent pas ensemble.

**Quatre décisions au registre** : **D25** (le SPI est écrit contre living-doc),
**D24** (les règles de findings et le substrat de graphe vivent au moteur),
**D23** (le rapport publié fait foi), et **D16 tranchée** (option C : Q1 muet,
Q2 le dit). **Plus aucune décision PENDING au registre.**

**Ce que D23 met explicitement au backlog post-7.0.0** : générateur de
configuration CI, comparateur d'historique d'audit, charts radar et quadrant,
renderer HTML. Ce sont des écarts **décidés**, à porter au gate de parité (M8)
comme tels — pas à y découvrir.

**Plan de lots** :

| Lot | Contenu | Où |
|---|---|---|
| 1 | Le SPI écrit contre living-doc : `HexaGluePlugin` (manifest + contribute), `PluginManifest`, `ReportSink`, exécution en deux passes (B7) et isolation `Exception \| LinkageError`. Aucun sink sans consommateur | `hexaglue-spi` |
| 2 | Les DSL de rendu markdown et Mermaid transplantées avec leurs tests, écrites dans living-doc ; échappement centralisé | plugin living-doc |
| 3 | living-doc, le premier plugin : lecture du modèle seul (ArchUnit), rendu vers le sink, ses six `livingdoc-*` | plugin + hôte |
| 4 | Le substrat de graphe au moteur : Tarjan/SCC (B10), métriques de Martin avec le SDP dans le bon sens (B1), bounded contexts relatifs au `basePackage` en une seule implémentation (B8/B9) | `hexaglue-engine` |
| 5 | Les 15 règles de findings au moteur, codées, avec évidences et remédiations ; plus le finding de D16 | `hexaglue-engine` |
| 6 | Le gate sur les findings : `findingThresholds` consommé, `Validation` étendue, restitution des findings | `hexaglue-engine` |
| 7 | Le plugin audit : les sept sections du rapport publié, provenance, fiabilité, renderers console/markdown/JSON, Mermaid (DSL promue en module commun) ; ses huit `audit-*`. **Plus le câblage de l'hôte reporté du lot 3** : découverte par ServiceLoader, goal qui exécute les plugins, matérialisation des documents — introduit une fois, avec deux consommateurs | plugin + hôte |
| 8 | L'analyse réacteur (D21 amendée) : nom de module au frontend, canal de rôles, `ModuleTopology` assemblée, règle S5 muette en mono-module, goal `reactor-audit` agrégé, lecture hiérarchique de `hexaglue.yaml` | frontend + moteur + hôte |
| 9 | Clôture : les 14 `test-param-*` portés, rapports comparés à l'ancien écart par écart, `case-study-banking` rejoué, revue des dix interdits | intégration |

Le lot 2 précède le 3 ; le 4 précède le 5, qui précède le 6 et le 7 ; la DSL est
promue en module commun au lot 7, quand l'audit en devient le second
consommateur — c'est le chemin que le doc 07 §7 décrit (« DSL commune promue de
living-doc »), et il fournit le test de généralité que l'interdit §10.1 réclame.

**Faits (2026-08-03)** — **1 119 tests** (951 à la clôture de M5), aucun skipped,
`verify -Pquality` vert **sur tout le réacteur** (checkstyle, PMD, SpotBugs,
doclint strict, plancher de couverture), **8/8 en intégration** :

- **Lot 1** (`949a1b5`) — module `hexaglue-spi`. `HexaGluePlugin` = un manifeste
  et une contribution ; `contribute(ArchModel, PluginConfig, Sinks)` est une
  fonction pure du modèle. `Schedule` résout l'ordre **en deux passes** — une
  pour apprendre quels identifiants existent, une pour tracer les arêtes — et
  **B7 est le test qui le prouve** : quatre plugins déclarés en ordre inverse de
  leurs dépendances. Rien n'y lève : dépendance absente, cycle et blocage
  transitif sont des exclusions portant leur raison, sur le modèle de
  `Perimeter.Exclusion` (M5). `PluginExecutor` isole `Exception | LinkageError`
  par plugin — y compris pendant `manifest()`, le premier endroit où un plugin
  peut échouer — et rend un `PluginRun` (documents, diagnostics, exécutés,
  écartés). Sept codes : HG-PLUGIN-001 à 007.
- **Trois choix de forme du lot 1**, chacun mesuré sur un test :
  1. *Aucun sink avant son consommateur* : `Sinks` ne porte que `DocumentSink`.
     `FindingSink` arrive au lot 7 avec l'audit, `SourceSink` à M7 avec jpa/rest
     — interdit §10.1 appliqué à l'intérieur même du jalon.
  2. *Un identifiant revendiqué deux fois n'est pas une exclusion* : le plugin
     tourne quand même, à travers le premier qui l'a revendiqué. La première
     version l'avait modélisé comme une exclusion, et le plugin survivant se
     retrouvait écarté — le test l'a dit avant que le code ne parte.
  3. *Un refus, un code, ses conséquences avec lui* : un plugin écarté parce
     qu'une dépendance a échoué n'a pas de diagnostic à lui ; c'est le
     diagnostic de la cause qui le nomme.
  Le manifeste ne porte **pas** de `minConfidence` : le doc 07 §6.1 le prévoit,
  mais aucun plugin ne le lit avant M7 (§6.4) — un champ que personne ne lit est
  du code mort (interdit §10.7).
- **Lot 2** (`da08de5`) — module agrégateur `hexaglue-plugins` et
  `hexaglue-plugin-living-doc`, avec les DSL de rendu transplantées :
  `Markdown` (titres, paragraphes, listes, tables, blocs de code, sections
  dépliables, règles), `Table`, `Graph` et `ClassDiagram`. **L'échappement est
  centralisé** : `Mermaid.identifier`/`Mermaid.label` ne sont pas publics, ce
  sont les constructeurs de diagrammes qui les appliquent — la carrière laissait
  passer les étiquettes d'arête et de nœud non échappées, et l'appelant devait
  savoir quand appeler `sanitizeId` lui-même. Une relation se nomme par ce
  qu'elle veut dire (`COMPOSITION`, `ASSOCIATION`…), jamais par sa flèche. Un
  diagramme rend sa propre source **sans clôture markdown** : un diagramme n'est
  pas du markdown, c'est le document qui l'encadre.

- **Lot 3** (`5eb6d00`) — le plugin living-doc : trois pages dérivées du modèle
  seul — la porte d'entrée (comptes par kind, packages, index, **et ce que le
  moteur n'a pas su lire**), le domaine (identité, parties, valeurs, événements,
  diagramme de classes) et la frontière (sens de chaque port, famille et agrégat
  géré d'un port piloté, graphe). **Chaque verdict peut dire ce sur quoi il
  repose** : une section repliée porte confiance, base et évidences par palier —
  une documentation qui ne montrerait que ses conclusions laisserait prendre un
  kind inféré pour un kind déclaré. Le plugin tourne dans son test **à travers
  l'exécuteur**, pas en direct : c'est le seul chemin qu'un build prendra.
- **Deux choix de forme du lot 3** :
  1. *Les ancres sont écrites, pas devinées.* Markdown dérive l'ancre d'un titre,
     donc deux types de même nom simple obtiennent des ancres qui diffèrent par
     un numéro qu'aucun générateur ne peut prédire. Chaque section pose son
     `<a id="…">` sur le **nom qualifié** : le lien reste juste quoi qu'il
     arrive aux titres.
  2. *Les noms sont raccourcis à l'affichage.* `TypeRef.toDisplayString()` rend
     le nom qualifié ; une page qui en est pavée ne se lit pas. `Names` retire
     les packages pour l'affichage et les garde là où ils désambiguïsent — sous
     le titre du type, et dans l'ancre.
- **Reporté au lot 7, et pourquoi** : le câblage de l'hôte (découverte par
  ServiceLoader, goal qui exécute les plugins, matérialisation des documents).
  L'introduire ici obligeait à inventer un goal pour **un** plugin ; au lot 7 il
  arrive avec deux consommateurs, ce qui est exactement le critère de D18. Le
  plugin est démontré sans hôte, par l'exécuteur.

- **Lot 4** (`85501ac`) — le substrat de graphe au moteur (D24). `Dependencies`
  plie les arêtes **que le frontend a déjà enregistrées** en un graphe de
  packages ; en dériver un second jeu en reparcourant les structures de types
  aurait été une deuxième réponse à la même question. Trois régressions y ont
  leur test : **B10** (un nœud de packages est **une** composante fortement
  connexe, donc un fait, pas un fait par chemin — JGraphT, Gabow), **B1** (la
  direction du SDP, sur un cas canonique où `core` I=0,25 dépend de `leaf`
  I=0,5 : c'est la seule dépendance fautive du graphe), **B8/B9**
  (`BoundedContexts`, segment **relatif à la racine** et implémentation unique).
- **Trois choix de forme du lot 4** :
  1. *Deux natures d'arête sont écartées, et le test le dit.* `DECLARES` joint
     un type à ses types imbriqués, qui sont dans son propre package ;
     `PERMITS` est le miroir d'`EXTENDS` — les compter tous deux ferait passer
     **toute hiérarchie scellée à cheval sur deux packages pour un cycle**.
  2. *La racine se déduit quand elle n'est pas déclarée* : le plus long préfixe
     de package que tous les types partagent. Même réponse qu'un `basePackage`
     déclaré sur un code qui en a un, réponse honnête sur un code qui n'en a pas.
  3. *`Stability` n'est plus `Comparable`* : il comparait sur le seul nom alors
     que son `equals` compare les six composantes — un comparateur incohérent
     avec `equals`. Les listes sortent déjà en ordre de nom ; l'interface ne
     servait à personne.
- **Le piège de déterminisme s'est présenté une troisième fois** : `Map.copyOf`,
  comme `Set.copyOf`, a un **ordre d'itération non spécifié**. `BoundedContexts`
  rendait les types d'un contexte en ordre d'insertion ; un test l'a pris.
  La sortie correcte est `Collections.unmodifiableSortedMap(new TreeMap<>(…))`,
  comme `ArchModel`. À vérifier partout où un ordre est rendu.

- **Lot 5a** (`492d755`) — les **huit règles du domaine** au moteur, HG-DDD-001 à
  008 : partie atteinte hors de son agrégat, partie revendiquée deux fois,
  agrégats en cercle, agrégat que rien ne stocke, type du domaine qui nomme
  l'extérieur, entité sans identité, valeur qui change en place, et **le finding
  de D16** (HG-DDD-008). `Judgement` porte ce qu'une règle a le droit de lire —
  les verdicts et qui nomme qui — et rien d'autre : aucune règle ne classe, ne
  lit une source ni ne touche un disque. `Findings.of` rend un ordre stable
  (code, sujet, message) pour qu'un rapport se diffe et qu'une porte ne change
  pas d'avis sur les mêmes sources.
- **Le lot 5 est scindé** : la famille hexagonale (7 règles HG-HEX-*) part au
  **lot 5b**, avec la question laissée ouverte du nommage des événements
  (`ddd:event-naming` de la carrière) — la règle de conduite 4 interdit toute
  règle de nommage hors vocabulaire, donc soit elle lit le vocabulaire opt-in,
  soit c'est un écart déclaré à porter au gate M8. Ne pas la porter en silence.
- **Le substrat du lot 4 a grandi avec son consommateur** : `usedBy`, `usersOf`
  et `knotsAmong(types)` — la même lecture de composantes fortement connexes
  répond pour les packages et pour un ensemble de types choisi, donc il n'y en a
  qu'une.
- **Écart de modèle constaté, non corrigé** : `Finding.remediations` est typé
  `List<RemediationHint>`, et `RemediationHint` ne sait exprimer qu'un correctif
  **de classification** (`RemediationImpact` porte `resultingKind` et
  `resultingConfidence`). Un finding dont le correctif est structurel — scinder
  un agrégat, donner une identité à une entité — n'a nulle part typé où le
  mettre : le conseil vit dans le message. Seul HG-DDD-008 porte une
  remédiation typée, parce que son correctif **est** une déclaration de kind. À
  trancher au lot 7, quand l'audit rendra les remédiations.

- **Lot 5b** (`84e68af`) — la famille hexagonale, HG-HEX-001 à 007, et la règle
  de nommage HG-NAME-001. **Le recouvrement de la carrière est défait, pas
  reproduit** : ses quatre règles de couches (pureté applicative, direction des
  dépendances, inversion des dépendances, isolation des couches) condamnaient la
  même référence jusqu'à trois fois, et un rapport disait donc une chose de trois
  façons. Ici chaque fait est énoncé une fois par la règle qui le porte — un test
  l'épingle (« says it once, not once per rule that could have said it »).
  **Le tableau de correspondance avec la carrière n'est donc pas 1:1, et c'est à
  porter au gate M8** :
  | Contrainte de la carrière | Sort en v7 |
  |---|---|
  | `hexagonal:application-purity` | HG-HEX-001, qui nomme **le port à utiliser à la place** |
  | `hexagonal:dependency-inversion` | fondu dans HG-HEX-001 : même fait, même sujet |
  | `hexagonal:dependency-direction` | fondu dans HG-DDD-005 (domaine) et HG-HEX-001 (application) |
  | `hexagonal:layer-isolation` | le seul cas non couvert par les précédents devient HG-HEX-007, adapter → adapter |
  | `hexagonal:port-coverage` | scindée par sens : HG-HEX-002 (rien n'implémente un port piloté), HG-HEX-005 (rien ne pilote un port pilotant) |
  | `hexagonal:port-direction` | scindée par sens : HG-HEX-003 (le cœur ne répond pas à un port pilotant), HG-HEX-004 (le cœur n'appelle pas un port piloté) |
  | `hexagonal:port-interface` | HG-HEX-006 |
  | `ddd:event-naming` | HG-NAME-001, **généralisée et retournée** (voir ci-dessous) |
- **Le nommage est tranché sans enfreindre la règle de conduite 4.** La carrière
  appariait les noms d'événements à une liste de terminaisons au passé **gravée
  dans le code** — exactement l'opinion sous couvert de convention que D13 a
  retirée. HG-NAME-001 ne connaît aucune convention : elle lit le **vocabulaire
  que le projet a déclaré** (`ClassificationConfig.suffixesFor`) et l'y tient,
  kind par kind. Sans vocabulaire configuré — le défaut — elle est muette sur
  tout code. La liste au passé de la carrière est un **écart assumé** à porter au
  gate M8.
- **Un port est vérifié des deux côtés**, et c'est délibéré : « rien ne s'y
  branche » et « le cœur ne s'en sert pas » sont deux pannes différentes, et les
  fondre ferait dire au rapport la moitié de ce qu'il sait.

- **Lot 6** (`d2f34fe`) — **`findingThresholds` a enfin son sujet**, lu depuis M5
  et consommé par personne. `Gate.FINDING` s'ajoute aux quatre portes ;
  `Validation.of(model, findings, gates)` refuse une construction sur un code
  **que la configuration a armé**, et seulement à partir de la sévérité qui y est
  déclarée. Un code que personne n'arme ne dit rien, quoi qu'il ait trouvé — un
  outil qui déciderait seul qu'une architecture n'a pas le droit de compiler
  serait un outil qu'on désinstalle. Le refus porte les mots du finding, pas une
  phrase sur les findings en général.
- **`AnalysisResult` porte les findings** (modèle, findings, diagnostics) : le
  moteur juge une fois, et les deux consommateurs — la porte qui casse la
  construction, le rapport qui l'explique — regardent **la même liste**. Un
  jugement calculé deux fois est un jugement qui divergera une fois.
  `ProjectAnalysis` passe désormais `analysed.findings()` au gate : `validate`
  gate sur les findings **sans exécuter le moindre plugin** (D24).
- **`Severity.isAtLeast`** est né du besoin : l'ordre de déclaration va du plus
  grave au moins grave, donc « au moins aussi grave » se lit à l'envers de
  l'énumération — exactement le genre de comparaison qu'un appelant écrit une
  fois à l'envers sans jamais s'en apercevoir. Il y en a une, et toutes les
  portes l'utilisent.
- **Un mécanisme, pas deux** : les paramètres `audit-error-on-blocker` /
  `audit-error-on-critical` / `audit-fail-on-error` de la carrière étaient une
  seconde façon de faire échouer une construction sur des findings, côté plugin.
  Leur équivalent v7 est `validation.findings`, au moteur. **Écart assumé à
  porter au gate M8** — le plugin audit affiche, il ne décide pas (doc 07 §6.3).

- **Lot 7, en quatre commits homogènes** — le jalon le plus lourd, découpé parce
  qu'un seul commit n'aurait pas été relu :
  - **7a** (`e730c94`) — `contribute(Contribution)` remplace les trois
    paramètres. La contribution porte le modèle, **les findings** et les mesures.
    C'est le second consommateur qui a montré le contrat trop étroit, un jalon
    plus tôt que D18 ne le prévoyait. **Conséquence de D24 à consigner : le
    `FindingSink` du doc 07 §6.1 n'a aucun producteur** — les findings viennent
    du moteur, l'audit les affiche. Pas de sink sans consommateur : il n'existe
    pas.
  - **7b** (`3df83eb`) — la DSL promue en `hexaglue-render`, au moment exact où
    l'audit en devient le second consommateur (le chemin du doc 07 §7).
    `Stability` part au modèle : c'est de la donnée, et l'y mettre évite que le
    moteur arrive au classpath des plugins, où un plugin pourrait rejuger.
    `Measurements` (SPI) porte ce que le moteur a mesuré ; l'hôte le remplit.
  - **7c** (`0d1f285`) — le plugin audit : les sept sections de D23 plus la
    fiabilité, markdown et JSON (Jackson, sortie ordonnée — un test prouve que
    deux runs donnent le même octet). **Le score est un choix de conception à
    relire** : quatre parts, chacune une proportion de quelque chose de
    comptable, aucun poids inventé — *lu*, *sain*, *démêlé*, *bien orienté*,
    moyenne, grade A-E. Un test a pris un vrai défaut : sur un code vide,
    *sain* tombait à 0, et le premier commit d'un projet aurait été noté comme
    un échec.
  - **7d** (`9942fe5`) — le câblage de l'hôte : `PluginDiscovery` (ServiceLoader,
    ordre d'identifiant), déclarations `META-INF/services`, section `plugins:`
    du document YAML, goal `report` qui découvre, exécute et **écrit** — un
    plugin ne touche jamais un disque. Un huitième cas d'intégration le prouve
    sur un vrai build : deux backends déclarés en dépendances du goal, leurs
    documents écrits là où le document le demande, et le backend à qui on a dit
    de ne pas dessiner ne dessine pas.
- **Trois défauts que le harnais d'intégration a pris, et qu'aucun test unitaire
  n'aurait vus** :
  1. *Une option de plugin écrite `false` en YAML est un booléen*, refusé par la
     lecture stricte alors qu'une option est du texte par contrat. `sections`
     lit désormais les scalaires avec indulgence — la sévérité qui compte est
     celle du plugin, qui sait nommer les options qu'il accepte.
  2. *`make integration` n'installait pas les modules de plugins* : la cible
     construisait `-pl hexaglue-maven-plugin -am`, qui ne les atteint pas.
     Elle installe maintenant tout le réacteur.
  3. *`install -DskipTests` prive JaCoCo de données* et fait tomber la
     vérification de couverture à zéro ; la cible passe `-Djacoco.skip=true`
     avec.

- **Lot 8, premier geste sur quatre** (`d34d1b4`) — **le frontend enregistre de
  quel module il a lu un type**. C'est le premier des quatre gestes que D21
  amendée énumère, et le substrat qu'elle avait constaté absent :
  `FrontendRequest.moduleName` (seul l'hôte le sait — une racine de sources ne
  dit rien du module qui l'a déclarée), `TypeNode.inModule` au modèle, et
  `SpoonFrontend` qui marque **ce qu'il a analysé** en enregistrant le
  `ModuleNode`. Un stub du classpath n'est **pas** marqué : ce serait mettre les
  types d'un autre projet à l'intérieur de la frontière de celui-ci — un test
  l'épingle. Cliquet inchangé : 143/143, 6/6, 5/5.
- **Ce qui reste du lot 8, et pourquoi c'est couplé** : les trois autres gestes
  (canal de rôles en configuration, `ModuleTopology` assemblée, règle S5) ne se
  découpent pas plus finement. `ModuleDescriptor` **exige** un rôle et
  `ModuleRole` n'a pas de valeur neutre, donc assembler une topologie sans le
  canal de rôles obligerait à inventer un rôle par défaut — c'est-à-dire à
  mentir. Le canal demande une cinquième composante à `HexaGlueConfig` et sa
  clé au chargeur ; la règle S5 suit, muette en mono-module. Puis viennent le
  goal `reactor-audit` agrégé et la lecture hiérarchique de `hexaglue.yaml`.

### 2026-08-04 — Jalon M6 : lot 8, l'analyse réacteur (FAIT)

Les trois gestes restants du lot 8, en quatre commits (`1eee676`, `be451be`,
`769b6c9`, `3176ed8`). **1 163 tests**, aucun skipped, `verify -Pquality` vert
sur tout le réacteur, **9/9 en intégration**, cliquet inchangé — 143/143, 6/6,
5/5, ce qui est la preuve que D21 réclamait : S5 est muette sur un corpus
mono-module.

- **D26 tranchée avant d'écrire** (option A) : la lecture S5 établit une
  **propriété de module**, jamais un kind. `KindEvidence` exige un `ArchKind`
  précis et « domaine » n'en est pas un ; le corpus étant entièrement
  mono-module, un signal qui aurait pesé sur la classification n'aurait été
  mesuré par aucun test. Conséquence de forme : **S5 n'est pas une `Rule` du
  catalogue** — une règle écrit des faits indexés par `TypeId` — mais une lecture
  au moteur, à côté du substrat de graphe du lot 4.
- **Le canal de rôles** (`1eee676`) — `ModulesConfig` au modèle, cinquième bloc
  de `HexaGlueConfig`, clé `modules:` au chargeur, lue strictement comme le
  reste. Les clés sont des noms de modules, donc elles sont lues sur le document
  lui-même et non contre une liste de clés connues : seule la **valeur** est
  tenue à un vocabulaire. **Le rôle est déclaré, jamais deviné** : la carrière le
  déduisait du suffixe de l'`artifactId` (`-infra`, `-core`, `-api`, table de 21
  entrées dans `ModuleRoleDetector`), ce qui est exactement l'opinion sous
  couvert de convention que la règle de conduite 4 interdit. **Écart assumé à
  porter au gate M8.**
- **La topologie assemblée** (`be451be`) — `Modules` au moteur plie les arêtes
  **que le frontend a déjà enregistrées** en un graphe de modules, avec la même
  définition de « dépendre » que `Dependencies` (`COUPLING` partagé, pas
  redéfini). `ModuleTopology` gagne les dépendances inter-modules et les
  candidats domaine ; `Assembly` la pose sur l'`ArchModel`. Un module lu sans
  rôle déclaré **n'entre pas** dans la topologie et se fait nommer :
  `HG-ENGINE-004`, WARNING.
- **Deux choix de forme du lot 8c** :
  1. *La candidature se décide sur le réacteur entier, la topologie ne montre que
     le déclaré.* Un module domaine qui dépend d'un module dont personne n'a
     déclaré le rôle dépend de quelque chose ; ne compter que les modules
     déclarés aurait transformé un trou de configuration en compliment. Un test
     l'épingle.
  2. *« Ni d'infra » se lit dans les faits, pas dans les noms* : un module dont
     un type porte `INFRA_DEPENDENCY` n'est pas candidat, quoi qu'il référence.
- **La lecture unifiée** (`769b6c9`) — `FrontendRequest` porte désormais le
  module de **chaque racine** (`sourceRoot(path, module)`) au lieu d'un nom
  unique pour toute la requête, et `SourceLocations` sait rendre la racine dont
  un type vient. Un réacteur se lit **en une passe** : lu module par module, un
  port déclaré dans l'un et implémenté dans l'autre se résoudrait en stub de
  lui-même, et la couture que l'analyse existe pour trouver est précisément ce
  qu'elle perdrait. L'attribution se fait sur les deux listes parallèles (types
  parsés, nœuds mappés) plutôt qu'en redérivant une identité.
- **L'hôte** (`3176ed8`) — goal **`reactor-report`** agrégateur, lecture
  hiérarchique de `hexaglue.yaml`, `Documents` partagé entre les deux goals qui
  exécutent des backends. Le rapport d'audit rend la disposition du réacteur
  **dans sa section d'inventaire** (rôle déclaré, ce dont chaque module dépend,
  nombre de types, candidature domaine) plus une colonne `Module` : D23 fige les
  sections publiées, donc la topologie entre dans une section existante, pas dans
  une huitième.
- **Écart de nommage assumé** : le plan dit `reactor-audit`, le goal s'appelle
  `reactor-report`. Les goals de la v7 sont `validate` et `report` ; `reactor-audit`
  aurait laissé croire à un goal propre au plugin d'audit, alors qu'il exécute
  les backends que le projet a installés, quels qu'ils soient.
- **La lecture hiérarchique : le plus proche gagne, en entier.** Un module sans
  document lit celui du réacteur ; un module qui en a un est lu sur le sien seul.
  Fusionner deux documents ferait dépendre chaque valeur d'un second fichier que
  le lecteur n'a pas sous les yeux, et une porte serait alors armée par ce que le
  module ne dit pas.
- **Trois défauts que le harnais d'intégration a pris, et qu'aucun test unitaire
  n'aurait vus** :
  1. *Un goal agrégateur lié au cycle de vie s'exécute sur le **dernier** projet
     du réacteur*, pas sur sa racine : le rapport atterrissait dans
     `shop-infra/target/`. `reportDirectory` n'a donc **pas** de défaut
     `${project.build.directory}` — la sortie est calculée depuis
     `session.getTopLevelProject()`.
  2. *La liaison est héritée par chaque module*, donc le réacteur entier était
     analysé trois fois pour trois rapports identiques. Le goal ne fait quelque
     chose que depuis la racine ; un test d'intégration compte les lignes de log.
  3. ***Le dépôt local des tests d'intégration gardait un jar de backend de la
     veille.*** `invoker:install` n'installe que le projet et ses dépendances, or
     les backends sont déclarés par les projets de test, pas par le plugin : rien
     ne les rafraîchissait, et un build prouvait ce qu'une session antérieure
     avait laissé là. `<extraArtifacts>` les installe désormais explicitement.
     **C'est le défaut le plus grave des trois** : il rend un harnais menteur.
### 2026-08-04 — Jalon M6 : lot 9, la clôture (FAIT)

Quatre commits (`cade589`, `50acc8c`, `cbf1827`, `055408a`). **1 163 tests**,
aucun skipped, `verify -Pquality` vert sur tout le réacteur, **10/10 en
intégration**, cliquet inchangé — 143/143, 6/6, 5/5.

**Les 14 `test-param-*` du jalon, cas par cas** :

| Cas de la carrière | Sort en v7 |
|---|---|
| `audit-error-on-blocker`, `-critical`, `audit-fail-on-error` et leurs trois variantes `no-` (6 cas) | **écart assumé, déjà décidé au lot 6** : remplacés par `validation.findings` au moteur. Le plugin affiche, il ne décide pas (07 §6.3) |
| `audit-report-directory` | porté — paramètre de goal `hexaglue.reportDirectory` **plus** option `outputDirectory` du plugin, les deux prouvés sur un vrai build |
| `audit-generate-docs` | **sans objet** : la carrière laissait l'audit émettre *aussi* de la documentation. En v7 l'audit rend son rapport et living-doc documente — deux backends, deux sorties, pas d'option |
| `livingdoc-generate-diagrams`, `-no-diagrams` | porté — `generateDiagrams` |
| `livingdoc-output-dir` | porté, **renommé** `outputDirectory` (une seule orthographe pour tous les backends) |
| `livingdoc-max-properties` | porté, **renommé** `propertiesPerDiagram` |
| `livingdoc-include-debug`, `-no-debug` | porté, **renommé** `includeProvenance` — ce n'est pas du débogage, c'est ce sur quoi le verdict repose |

Chaque option a déjà son test unitaire de comportement ; ce qu'aucun ne prouvait
est le **câblage** — document YAML → `PluginConfig` → backend — et le paramètre
de goal. D'où un dixième cas d'intégration, `plugin-options-are-honoured`.

- **Sa fixture est le premier vrai build qui classe sans aucun symbole
  framework** : `PlaceOrder` détient `Orders`, personne ne l'implémente, donc
  port piloté par position ; puis agrégat, identité, valeur. Les fixtures
  d'intégration précédentes étaient trop petites pour que le moteur conclue quoi
  que ce soit, et leurs assertions sur les pages de living-doc auraient été
  vides — un test qui passe sans rien vérifier.

**`case-study-banking` rejoué** (réacteur Spring Boot 5 modules, rejeu sur copie,
le projet n'a pas été touché). Le goal s'exécute **une fois, à la racine**, lit
les 6 projets et dispose les 5 qui portent des sources.

| | carrière | v7, vocabulaire éteint | v7, vocabulaire déclaré |
|---|---|---|---|
| Types analysés | 38 | 45 | 45 |
| Score / grade | 67, D — « PASSED » | 74, C | 79, B |
| Agrégats | 6 | 5 | **6** |
| Identifiants | 6 | **1** | **6** |
| Valeurs | 8 | 6 | 6 |
| Services applicatifs | 5 | 5 | 5 |
| Ports pilotés | 8 | 7 | **8** |
| Ports pilotants | 5 | **0** | **0** |
| UNCLASSIFIED | — | 19 | 12 |
| Violations | 0 | 5 | 9 |

**Deux causes expliquent tout l'écart, et aucune n'est un défaut de lecture.**

1. **Le code généré est l'anneau extérieur de ce projet.** 51 des 99 fichiers
   portent `@Generated` : 26/28 dans `banking-persistence`, 25/25 dans
   `banking-api`. La carrière générait ses adapters JPA et ses contrôleurs REST
   dans `src/main/java` **puis les relisait** ; v7 les écarte (D15/D19). D'où les
   0 ports pilotants (les contrôleurs qui les détiennent ont disparu) et les
   5 `HG-HEX-002` (« rien n'implémente ce port piloté ») : v7 dit vrai **du code
   écrit à la main**, la carrière disait vrai du code après génération.
   **Conséquence à trancher (candidate D27)** : sur un projet qui utilise la
   génération de HexaGlue, l'audit v7 signalera chaque port de dépôt comme non
   implémenté. C'est une fausse alerte pour cet utilisateur, et la corriger
   demande soit de connaître la génération (M7), soit de rouvrir D15.
2. **Le vocabulaire de nommage éteint coûte 5 identifiants et 1 agrégat.**
   Déclaré, v7 **rejoint la carrière exactement** sur les agrégats (6), les
   identifiants (6), les ports pilotés (8) et les services (5), et 7 types
   sortent d'UNCLASSIFIED. **Cela ne contredit pas D13** — le défaut reste
   opt-in, le projet déclare ses mots — **mais cela borne la portée de la mesure
   du lot 23**, qui concluait « 0 gain » sur un corpus dont les profils 2 et 3
   sont petits. `case-study-banking` est le premier parc réel mesuré : sur du
   code qui suit ses conventions, le vocabulaire déclaré est ce qui referme
   l'écart avec la carrière. À porter au gate M8.

**Revue des dix interdits (07 §10)** — faite dans le code, **trois violations
trouvées et corrigées**, ce que les revues précédentes n'avaient pas vu :

1. *Abstraction sans second consommateur* — RAS. `Sinks` ne porte toujours que
   `DocumentSink` ; `FindingSink` n'existe pas (D24 : les findings viennent du
   moteur).
2. *Pivot `String`* — RAS.
3. *`catch (Exception)` global* — le seul est `Exception | LinkageError` dans
   `PluginExecutor`, qui **est** l'isolation des plugins, décidée au lot 1.
4. *Annotation par nom simple* — sans objet.
5. *Re-dérivation par un consommateur du modèle* — RAS : le rapport lit les
   mesures que le moteur lui tend (`Measurements`), il ne reparcourt rien.
6. **Violation trouvée et corrigée** : « dire ce qu'une lecture a écarté »
   existait dans `ValidateMojo` seul, et mes deux goals de rapport avaient
   chacun leur idée — dont une pire, qui ne disait rien. Extrait en
   `Diagnostics`, employé par les trois. C'est `case-study-banking` qui l'a
   révélé : **54 types écartés, aucune ligne de log**.
7. **Violation trouvée et corrigée** : trois accesseurs que rien ne lit —
   `ModuleTopology.dependentsOf` et `domainCandidates()` (les miens, lot 8) et
   `modulesByRole` (mort depuis M1). Retirés.
8. *Décision sans évidence tracée* — RAS.
9. **Violation trouvée et corrigée, la plus sérieuse** : deux règles de findings
   du lot 5 lisaient des noms **hors vocabulaire**, contre la règle de conduite 4,
   et personne ne l'avait consigné.
   - `HG-DDD-007` appelait « mutateur » toute méthode dont le nom commence par
     `set` : elle manquait tout mutateur écrit sous un autre verbe et aurait
     signalé un `settle(payment)` qui ne change rien. Elle lit désormais
     **l'état** — un champ ni statique ni final — et nomme ce qui peut changer.
   - `HG-DDD-008` (le finding de D16) cherchait un champ nommé `id` ou finissant
     par `Id`. Rien de structurel ne distingue une partie identifiée par un
     `Long` nu d'une valeur qui en tient un : le mot est le seul signal restant,
     donc il est lu — mais **seulement celui que le projet a déclaré**
     (`suffixesFor(IDENTIFIER)`). Sans vocabulaire, le finding est muet, comme
     HG-NAME-001. Même résolution que le lot 5b, appliquée là où elle manquait.
10. *Échec avalé sans diagnostic* — RAS : HG-ENGINE-004 s'ajoute aux codes
    existants.

**Écarts de forme relevés au passage, non traités** : le bloc `modules:` de la
carrière est imbriqué (`banking-core:` puis `role: DOMAIN`), celui de la v7 est
plat (`banking-core: DOMAIN`) — la forme imbriquée réservait la place pour
d'autres clés par module, que rien ne demande (interdit §10.1). À porter au gate
M8.

- **Point à instruire avant M8** : le goal `validate` reste **mono-module** — un
  réacteur gate module par module, chacun analysé seul. Le gate ne voit donc pas
  les findings qui traversent une frontière de module, alors que `reactor-report`
  les voit. Les deux ne peuvent pas rester en désaccord.

### 2026-08-04 — Après la clôture de M6 : la documentation publique (FAIT)

Trois commits hors jalon, demandés après coup (`b83612c`, `ee9ea50`, `ed989a0`).

- **README du dépôt** (`b83612c`) — il datait d'avant M5. Les quatre modules
  livrés depuis y entrent (`hexaglue-spi`, `hexaglue-render`,
  `hexaglue-plugins`, `hexaglue-maven-plugin`), la ligne du moteur dit qu'il
  **juge** aussi (D24), et le paragraphe « d'autres modules arrivent » disparaît.
  Deux sections nouvelles : **« Using it »** (les trois goals, la ligne qui les
  sépare — l'un juge, les autres écrivent —, un `hexaglue.yaml` couvrant les cinq
  blocs, l'héritage du document, la lecture du réacteur en une passe, et le fait
  que seul un code que le projet arme peut casser sa construction) et **« What is
  read, and what is not »**, née du rejeu de `case-study-banking` : les racines
  déclarées seules, le code généré écarté, et la conséquence énoncée en clair —
  *sur un projet dont les adapters sont générés, HexaGlue rapporte sur la part
  écrite à la main*. Mieux vaut que le lecteur l'apprenne du README que du
  rapport.
- **Un README par backend** (`ee9ea50`) — la carrière en avait un par plugin
  (416 lignes pour l'audit, 163 pour living-doc), la v7 n'avait qu'une ligne de
  tableau. Chacun dit comment on l'installe, ce qu'il écrit, ses options avec
  leurs défauts, et un fragment de sortie réelle. Celui de l'audit porte **la
  table des seize codes de findings**, qui n'existait nulle part pour un
  utilisateur. Chaque affirmation a été vérifiée contre les documents que le
  harnais d'intégration produit, pas contre le souvenir du code.
- **Dette d'outillage relevée, non traitée** : la carrière extrayait ces tables
  d'options depuis le code (`extract-doc-metadata.js`, `make doc-readmes`,
  `make doc-check` en CI). La v7 n'a pas d'équivalent, donc les tables sont
  écrites à la main et dériveront le jour où une option change. Le manifeste de
  chaque plugin est déjà la source de vérité ; un test comparant le README à
  `KEYS` coûterait peu. **À décider à M8** — c'est de l'outillage, pas de la
  documentation, et le périmètre est gelé.
- **Un trou de couverture trouvé en répondant à une question** (`ed989a0`) :
  l'option `generateDiagrams` existe des deux côtés, mais **l'audit ne dessinait
  dans aucun test d'intégration** — il ne dessine que s'il trouve un nœud de
  dépendances, et aucune fixture n'avait de cycle de packages. Son chemin « true »
  n'était tenu que par un test unitaire. Deux packages qui se référencent en
  cercle ont été ajoutés **hors du domaine** (vérifié : la page domaine ne bouge
  pas), plus trois assertions sur l'audit et deux sur le graphe de frontière de
  living-doc, qui était rendu et assertionné par personne.
  **Leçon** : une option dont l'effet dépend de ce que le code contient n'est pas
  couverte par une fixture qui ne le contient pas — l'absence de sortie s'y lit
  comme un « false » réussi.

**Erreur de manipulation à connaître** : deux commandes `mvn` de cette session
ont tourné dans `hexaglue/` (la carrière) au lieu de `hexaglue-next/`, le
répertoire courant du shell y étant resté après une lecture. Elles n'ont produit
que du `target/` ; aucune source de la carrière n'a été touchée. Toujours
préfixer les commandes de build d'un `cd` explicite.

### 2026-08-04 — Jalon M7 : ouverture, jpa + rest (D27-D29)

Ouverture selon le protocole des jalons précédents : instruction du code
d'abord, arbitrage utilisateur ensuite. Déclencheur de l'instruction : la
question « qu'a-t-on fait dans living-doc, est-on passé à côté de
l'architecture prévue ? » — la réponse a imposé de relire le doc 07 §6 en
entier contre le SPI livré, avant d'ouvrir quoi que ce soit.

**Ce que la relecture a établi.** living-doc v7 est conforme : 923 lignes
contre 6 627 à la carrière, et le facteur 7 est un déménagement, pas une
perte (les DSL de rendu sont devenues `hexaglue-render` ; les 14 records
`*Doc` et leur factory sont morts par lecture directe de l'`ArchModel` ;
`BoundedContextDetector` est au moteur par D24 ; `DebugInfo` est devenu
`includeProvenance`). Le SPI est étagé volontairement — chaque étage différé
a sa trace… **sauf deux, jamais consignés** :

1. **`DiagnosticSink`** — le seul des quatre sinks de §6.1 dont le sort
   n'avait jamais été énuméré (`ReportSink` fondu au lot 1 de M6,
   `FindingSink` tué par D24, `SourceSink` daté par D25 — lui : rien). Son
   premier consommateur est précisément la génération : §6.4 exige un
   diagnostic *au lieu de* code, donc un plugin qui se tait sur un type et
   continue — or le seul canal actuel est l'échec total (HG-PLUGIN-003).
2. **`consumes` / `produces` du manifeste** — zéro occurrence au registre.
   `minConfidence`, lui, avait été refusé explicitement au lot 1 (§10.7) :
   c'est l'énumération de D25 qui a manqué, pas le principe.

**Le défaut de processus** : la revue de clôture lit les dix interdits du
§10, jamais la section du doc 07 que le jalon prétendait livrer — un manque
de §6.1 était invisible à ce filtre. **La règle de conduite 13 est amendée**
en conséquence, et l'en-tête du doc 07 porte désormais l'état réel de §6.1.

**Quatre arbitrages utilisateur (« je valide tel quel »)** :

- **Périmètre scindé : M7a = jpa, M7b = rest.** jpa force les décisions
  structurantes (sink de sources, `Field`, seuil, B2, B15) ; rest les
  reprend sans les rouvrir. Même logique que D25 : le premier consommateur
  arbitre.
- **D27** — `produces` au manifeste (par famille de port couverte), le
  jugement lit les couvertures déclarées, HG-HEX-002/005 se taisent en le
  disant. Au registre.
- **D28** — `SourceSink` (paquet + type + contenu + module cible optionnel),
  `DiagnosticSink`, seuil de génération typé dans la `Contribution` ;
  `minConfidence` du manifeste et `consumes` restent dehors (§10.7). Goal
  `generate` en GENERATE_SOURCES, qui écrit et ne juge pas. Au registre.
- **D29** — l'assemblage remplit `Field` : rôles relus des liens énoncés
  (doctrine `Links`), déballage par `TypeRef`/`Shapes` ; frontend intouché.
  Au registre.

**B2 et B15 relus dans le code de la carrière** (aucune décision à prendre,
le plan §5 les lie) : B2 n'est pas une liste incomplète —
`NamingConventions.toColumnName` gère les mots réservés, mais
`PropertyFieldSpec:138` passe par le second `toSnakeCase` de
`JpaModelUtils:90-96` qui court-circuite la liste ; le correctif v7 est
**une seule implémentation de nommage** (§10.6) plus le test paramétré des
22 mots. B15 : `JpaConfig:96` lit `ASSIGNED` en défaut là où sa Javadoc
annonce `IDENTITY`, `valueOf` sans message — couvert par le motif d'options
typées strictes du SPI. **Note pour M7b** : la carrière porte une seconde
`NamingConventions` dans rest ; l'extraction d'un commun se décide à M7b,
quand le second consommateur existe (§10.1).

**Poids de la récolte** : jpa = 14 078 lignes de production + 13 396 de test
(44 fichiers) ; rest = 5 339 + 7 002 (48 fichiers). L'estimation « M7 ≈ 2
semaines » du plan 08 date d'avant les neuf lots de M6 ; à ré-étalonner
après M7a.

**Plan de lots M7a (jpa)** :

| Lot | Contenu | Où |
|---|---|---|
| 1 | D29 : l'assemblage remplit `Field` — `IDENTITY` ← `IDENTIFIED_BY`, `AGGREGATE_REFERENCE` ← composition, `EMBEDDED` ← `OWNS`, `COLLECTION`/`elementType` ← déballage `TypeRef`, `wrappedType` ← `Shapes` ; `AUDIT`/`TECHNICAL` sur annotation de pack seulement. Les 154 goldens régénérés et relus en un seul diff | engine (`Assembly`) + acceptance |
| 2 | D28 : `SourceSink` + `DiagnosticSink` au SPI, seuil typé dans la `Contribution` ; `PluginExecutor` sert les trois | spi |
| 3 | jpa, socle : options typées strictes (B15 en régression), l'unique implémentation de nommage (B2 : test des 22 mots réservés) | plugin-jpa |
| 4 | jpa, entités : entités + embeddables depuis `CompositionIndex`/`DomainIndex` (javapoet) ; le seuil appliqué — sous le seuil, diagnostic + remédiation, pas de code | plugin-jpa |
| 5 | jpa, dépôts : repositories Spring Data + adapters de ports (stratégies transplantées avec leurs tests) | plugin-jpa |
| 6 | jpa, mappers ; le manifeste déclare `produces` (familles de ports couvertes) — D27 côté déclarant | plugin-jpa |
| 7 | D27 côté juge : `Judgement` reçoit les couvertures, HG-HEX-002/005 tus en le disant (INFO) ; `case-study-banking` rejoué pour mesurer | engine + maven-plugin |
| 8 | L'hôte : goal `generate` (GENERATE_SOURCES), matérialisation + `addCompileSourceRoot`, restitution des diagnostics ; intégration : exemple généré **et compilé**, sorties golden-diffées | maven-plugin + acceptance |

Comme à M6, les plugins se démontrent **à travers l'exécuteur** (lots 3-6),
l'hôte arrive quand il a quelque chose à matérialiser (lot 8). Clôture M7a
(règle 13 amendée) : corpus vert + cliquet inchangé + revue §10 +
**relecture ligne à ligne de §6.4**.

Aucun code écrit à l'ouverture : registre D27-D29, règle 13, en-tête du
doc 07, ce journal.

### 2026-08-04 — Jalon M7a : lot 1, l'assemblage remplit les champs (FAIT)

Un commit (`43460fd`). **1 178 tests** (+11), aucun skipped, `make ci` vert,
**10/10 en intégration**, **cliquet inchangé** — 143/143, 6/6, 5/5 : un lot
qui ajoute de l'information sur les champs ne doit déplacer aucun verdict, et
n'en a déplacé aucun.

- **`Fields` (nouvelle classe du moteur)** lit chaque champ contre les
  verdicts stabilisés, et `Assembly` la branche avant de construire la
  structure. Les six lectures, avec leur source :

  | Composant | D'où il vient |
  |---|---|
  | `elementType` | `TypeRef.unwrapElement()` — l'unique déballage du modèle, celui que les règles empruntent déjà |
  | `COLLECTION` | `TypeRef.isCollectionLike()`, tableaux compris |
  | `IDENTITY` | sur un agrégat, **le lien `IDENTIFIED_BY`** et lui seul ; sur une partie ou un événement, le verdict IDENTIFIER sur le type du champ |
  | `EMBEDDED` | le type du champ lu VALUE_OBJECT ou IDENTIFIER |
  | `AGGREGATE_REFERENCE` | le type du champ lu AGGREGATE_ROOT |
  | `wrappedType` | la valeur unique du type du champ, quand ce type est lu valeur ou identité |

- **`AUDIT` et `TECHNICAL` ne sont jamais posés, et c'est écrit dans l'enum.**
  La carrière les lisait sur le **nom** du champ (`FieldRoleDetector`,
  `AUDIT_FIELD_PATTERNS`/`TECHNICAL_FIELD_PATTERNS`) — interdit 07 §10.9 et
  règle de conduite 4. Les distinguer demanderait qu'un pack nomme une
  annotation **sur un membre**, ce qu'aucun ne fait. Vérifié au passage : le
  code de production des générateurs de la carrière ne lit ni l'un ni
  l'autre ; seuls des tests les fabriquent à la main. Rien ne bloque M7a.
- **Une lecture retirée, pas dupliquée** : `DomainAssembly` ne cherche plus
  le champ d'identité (`fieldCarryingAnIdentity`, `fieldHolding` supprimées),
  il lit le rôle que `Fields` a posé. Sans quoi le modèle aurait porté deux
  versions du même champ — celle de `structure().fields()` et celle
  d'`identityField()` — et `CompositionIndex`, qui lit la seconde, aurait pu
  diverger de ce que lit un générateur. Un test l'épingle : les deux sont le
  même objet.
- **Le cliquet ne voyait pas ce que le lot ajoute** — le piège du lot 21, à
  l'identique. `ArchModelSnapshots` ne rendait d'un champ que `name`, `type`
  et une `cardinality` dérivée : les rôles et le wrapper auraient changé sous
  des goldens qui continuaient de matcher. `cardinality` est **remplacée** par
  ce qu'elle résumait — `elementType`, `wrappedType`, `roles`, chacun omis
  quand vide, pour que le diff reste lisible.
- **Le diff des goldens, relu ligne à ligne** : 38 fichiers, 176 insertions,
  263 suppressions. Réparti par clé : `-150 cardinality` (la clé disparaît),
  `±113 type` (**vérifié : seule la virgule finale bouge**), `+37 roles`,
  `+16 wrappedType`, `+10 elementType`. Aucune autre clé touchée, donc aucun
  verdict, aucun lien, aucune remédiation.
- **Ce que le diff prouve, et qui vaut mieux qu'un compte** : sur `Armada`
  (profil 3, aucune convention de nommage), l'identité sort en
  `roles: ["IDENTITY", "EMBEDDED"]` avec `wrappedType: java.util.UUID` —
  trouvée **par le seul lien qu'un port a énoncé**, là où la carrière
  cherchait un champ nommé `id` ou `<Type>Id`. Sur `Clinic` (profil 2),
  `Owner.id` est IDENTITY **sans** `wrappedType` (son identité est un
  `java.lang.Integer` hors périmètre) et `Pet.id` n'est pas IDENTITY du
  tout — le cas D16, Q1 muet, épinglé une fois de plus.
- **PIT a trouvé quatre trous, tous comblés** (`Fields` : 25/30 → **30/30
  mutants tués**) :
  1. `AGGREGATE_REFERENCE` n'apparaissait **nulle part** dans les 154
     scénarios — un domaine bien dessiné ne tient pas un autre agrégat en
     entier. Le rôle serait parti couvert par rien ; il a désormais sa
     fixture (`twoAggregates`).
  2. Aucun test ne distinguait l'identité d'un agrégat d'un **autre**
     identifiant qu'il garde : la fixture n'avait qu'un seul agrégat, donc
     les deux branches de `carriesIdentity` rendaient la même chose.
  3. Aucun test ne couvrait le filtre `static` — le court-circuit `||`
     l'empêchait d'être atteint.
  4. Rien ne vérifiait que la **documentation et la localisation** d'un champ
     survivent à sa reconstruction.
- **Leçon à garder** : la première version de la fixture des points 1-2 était
  muette parce que `Berthing` n'y était détenu par personne — donc pas un
  port, donc pas d'agrégat, donc pas d'identifiant. Une fixture qui ne câble
  pas ses ports ne prouve rien du domaine, et c'est PIT qui l'a dit, pas la
  suite verte.

**Trouvaille hors périmètre (règle 9), non traitée** : `make ci` émet un
warning `[NotJavadoc]` sur `ConfigLoader.java:221` (« multiple Javadoc
comments; only the last one will be used »). Le fichier n'est pas touché par
ce lot — le warning **préexiste** et contredit le « aucun warning de
compilation » de la clôture M6. À corriger dans un lot qui touche déjà
l'hôte, ou en tête de M7a lot 8. **Le compte exact a été fait au lot 2 :
onze warnings, pas un** (voir ci-dessous).

### 2026-08-04 — Jalon M7a : lot 2, ce avec quoi un backend écrit (FAIT)

Un commit (`37fb5a3`). **1 208 tests** (+30), aucun skipped, `make ci` vert,
**10/10 en intégration**, cliquet inchangé — un lot de contrat ne touche
aucun verdict.

- **La forme du `SourceSink` a été arbitrée par le producteur réel, pas
  devinée.** Le `CodeWriter` de la carrière expose treize méthodes ; ses deux
  générateurs n'en appellent que **deux** — `writeJavaSource(package, class,
  content)` et sa variante à module — plus `isMultiModule()`. `exists`,
  `delete`, `writeResource`, `getOutputDirectory`, `writeDoc` n'ont **aucun
  consommateur dans la carrière elle-même** : leur absence en v7 n'est pas un
  écart fonctionnel, et il n'y a rien à porter au gate M8 de ce côté.
- **`SourceFile(module?, packageName, typeName, content)`**, avec `path()` et
  `qualifiedName()` dérivés. Un plugin nomme, il ne place jamais : le
  confinement est une propriété de forme, comme pour `Document`. Ce qui n'est
  pas un identifiant Java est refusé — et `..`, `/`, `C:` ne sont pas des cas
  particuliers filtrés un par un, ils ne sont simplement pas des
  identifiants. Le module est **optionnel** et le plugin ne demande plus à
  l'hôte s'il est en multi-module (`isMultiModule` disparaît) : il dit où va
  ce qu'il écrit, l'hôte résout.
- **`DiagnosticSink`** — le sink de §6.1 dont le sort n'avait jamais été
  énuméré (relevé à l'ouverture de M7). Il ouvre le cas que le SPI ne savait
  pas dire : un backend qui **décline une partie** de son travail et continue.
  Échouer avait déjà son canal ; se taire sur un type et poursuivre n'en avait
  aucun, et §6.4 l'exige. Un test épingle la frontière : ce qu'un plugin
  rapporte **meurt avec lui** s'il échoue ensuite — une contribution ratée est
  une contribution qui n'a pas eu lieu, documents et sources compris.
- **Le seuil voyage dans la `Contribution`**, avec `isCertainEnough(archType)`
  à côté. La comparaison va **dans le sens inverse** de l'enum qu'elle lit
  (`EXPLICIT` est le plus fort et le premier) ; un backend qui la referait
  à l'envers écrirait du code pour exactement les verdicts que le seuil est
  là pour écarter. Trois tests l'épinglent dans les deux sens.
  `Confidence.isAtLeast` existait déjà — aucune seconde lecture écrite.
  Le seuil est **branché sur le YAML** (`config.generation().minConfidence()`
  dans les deux goals) : `GenerationConfig`, chargé depuis M5 et lu par
  personne, a enfin son sujet.
- **Une collision de sources est arbitrée comme une collision de documents** —
  le premier écrivain garde, les deux sont nommés, le run continue —, ce qui a
  fait extraire l'arbitrage dans `Outputs` : une seule implémentation, deux
  codes (`HG-PLUGIN-006` documents, **`HG-PLUGIN-008` types**).
- **Un risque fermé avant qu'il n'existe** : à partir du lot 4, un backend
  émettra des sources dans un `report` qui ne les écrit pas. Les jeter sans un
  mot se lirait comme « ce backend n'a rien généré » (interdit §10.10). Le
  goal le dit désormais et renvoie vers `hexaglue:generate`.
- **PIT a de nouveau trouvé ce que la suite verte ne disait pas** (SPI :
  `SourceFile` 11/11, `Contribution` 5/5, `Outputs` 4/4, aucun survivant) :
  1. **Les trois méthodes de `Contribution` — celles avec lesquelles un plugin
     écrit — n'étaient exercées par aucun test.** Les tests passaient par les
     sinks en dessous ; l'API réelle des backends était couverte par rien.
  2. Aucun cas de nom dont le **premier** caractère est invalide (`1Order`,
     `com.2acme`) : tous les cas cassaient plus loin dans la boucle, donc la
     garde de tête n'était tenue par personne.
  3. Une garde redondante (`isBlank` vérifié deux fois sur le même segment),
     retirée.
- **Le compte des warnings, fait cette fois sur la sortie complète : onze,
  tous préexistants, aucun dans un fichier de ce lot** — `ModuleReadingTest`
  ×7 (`OptionalMapToOptional`), `ConfigLoader:221` (`NotJavadoc`),
  `Severity:50` ×2 (`EnumOrdinal`), `Markdown:102` (`LoopOverCharArray`).
  **Le « zéro warning » que les clôtures M3-M6 revendiquent n'est donc plus
  tenu**, et il l'était encore à M4 (vérifié à l'époque sur le log complet).
  **Soldé immédiatement après, hors jalon** (voir ci-dessous).

### 2026-08-04 — Hors jalon : les onze warnings soldés (FAIT)

Un commit (`aa85f99`), demandé avant d'ouvrir le lot 3. **`make ci` n'émet
plus aucun warning** ; 1 208 tests, 10/10 en intégration, cliquet inchangé.
Aucun de ces défauts n'appartenait aux lots 1-2 : ils avaient traversé les
clôtures M3 à M6, que le contrôle « zéro warning » aurait dû arrêter.

Les quatre causes, et ce que chacune enseigne :

1. **`Severity.isAtLeast` comparait par `ordinal()`** (2 warnings
   `EnumOrdinal`). `Confidence.isAtLeast` fait la même comparaison par
   `compareTo` — la forme adoptée à M1, précisément pour retirer les
   `ordinal()` du modèle. `Severity` avait échappé au nettoyage : deux
   écritures du même geste, dont une que l'outillage condamne (interdit
   §10.6). Aligné sur `Confidence`.
2. **`ConfigLoader` portait deux blocs Javadoc consécutifs** (`NotJavadoc`).
   Le premier décrit `load(origin, yaml)` **mot pour mot** — la méthode existe
   trente lignes plus bas avec sa propre copie du même texte. C'est une
   Javadoc orpheline laissée par une réorganisation : le compilateur disait
   qu'elle serait ignorée, et elle documentait une méthode qui n'était pas la
   suivante. Supprimée.
3. **`Markdown.escape` allouait un tableau** par `toCharArray()`
   (`LoopOverCharArray`) là où `charAt` suffit — sur le chemin de tous les
   documents rendus. Boucle indexée.
4. **Sept assertions de `ModuleReadingTest` construisaient un
   `Optional<Optional<String>>`** (`OptionalMapToOptional`). Le correctif
   qu'Error Prone suggère — `flatMap` — aurait été **faux** : ces assertions
   vérifient deux choses à la fois (le type est là, et voici son module), et
   `flatMap` rend un type absent indistinguable d'un type sans module.
   Réécrites en `type(...).orElseThrow().moduleName()`, qui dit laquelle des
   deux conditions a lâché. **À retenir : un warning peut avoir raison sur le
   symptôme et tort sur le remède.**

**Ce que l'épisode dit du dispositif** : le contrôle est réel — il a bien
listé les onze — mais il n'a jamais été *lu* sur la sortie complète depuis M4.
Un `grep | tail` sur un log de `make ci` en montre la fin, pas les warnings de
compilation, qui sortent au milieu. La clôture de M7a doit compter les
warnings, pas les regarder.

### 2026-08-04 — Jalon M7a : lot 3, le socle de jpa (FAIT)

Un commit (`ec37dab`). **1 281 tests** (+73), **zéro warning**, `make ci`
vert, 10/10 en intégration, cliquet inchangé. **Le code du lot est à 100 % de
mutants tués** (`SqlNames` 13/13, `JpaOptions` 8/8, `PluginConfig.choice`
entièrement couvert ; le module jpa rend 21/21).

Module `hexaglue-plugins/hexaglue-plugin-jpa`, qui ne dépend que du SPI. Pas
encore de `HexaGluePlugin` : rien à générer avant le lot 4, donc rien à
enregistrer.

- **B2 est corrigé par construction, et il était plus large que recensé.**
  L'audit décrit « deux `toSnakeCase` divergents, donc mots réservés non
  échappés ». Le second effet n'était pas noté : la version courte
  (`JpaModelUtils`, une seule regex) **perd aussi les runs de capitales** —
  `XMLParser` y devient `xmlparser`. Et c'est elle que `PropertyFieldSpec:138`
  appelle, donc **toutes les colonnes des value objects** passaient par la
  mauvaise. En v7 il y a une seule `SqlNames.snake`, celle qui traite les deux
  frontières, et le test la vérifie sur `XMLParser`, `HTTPStatus`, `IBANCode`.
- **Le test des mots réservés en a trouvé un troisième défaut, non recensé.**
  La liste compte **23 mots** (le plan en annonçait 22). La carrière
  pluralisait « sauf si le mot finit déjà par s », ce qui rend `values`
  **inchangé — donc toujours réservé** ; et `value` s'y pluralise en `values`,
  **qui l'est aussi**. Deux mots sur 23 n'étaient donc pas protégés du tout.
  La règle v7 est écrite autour de l'invariant plutôt que du procédé :
  pluraliser tant que cela suffit, suffixer `_tbl` sinon — et le test
  paramétré exige des 23 que la sortie **ne soit pas un mot réservé**, ce
  qu'aucune formulation en « pluriel attendu » n'aurait attrapé.
- **Écart de sortie assumé, à porter au gate M8** : `SqlNames.snake` coupe
  aussi après un chiffre (`address2Line` → `address2_line`), là où la carrière
  rendait `address2line`. Le §6 du plan prévoit exactement ce cas — « B2
  corrigé change légitimement un nom de colonne ».
- **B15 : ce n'est pas le code qui avait tort, c'est sa documentation.** La
  carrière lit `ASSIGNED` par défaut et sa Javadoc annonce `IDENTITY`. Le
  comportement juste est celui du code : un domaine qui possède ses identités
  les construit lui-même — un `OrderId` existe avant qu'il y ait une ligne —
  donc le magasin enregistre la valeur qu'on lui tend. La v7 garde `ASSIGNED`,
  le documente comme une phrase sur le domaine, et **le prouve par un test**.
- **`PluginConfig.choice(key, enum, fallback)`** ajouté au SPI — le remède que
  l'audit prescrivait pour B15. Une valeur inconnue est refusée en nommant
  l'option, la valeur reçue **et les valeurs acceptées**, là où `valueOf` ne
  nomme rien. Insensible à la casse et aux espaces.
- **Deux pièges repris de la mémoire, l'un tombé quand même** : les tests du
  nouveau module ont d'abord échoué sur `NoSuchMethod choice` — le jar
  `hexaglue-spi` du dépôt local était périmé. C'est le piège déjà consigné
  pour les ITs, qui vaut aussi pour un module neuf du réacteur.
- **PIT a de nouveau montré deux trous** : `choice` n'était couvert **que
  depuis l'autre module** (PIT s'arrête au module, donc la méthode était
  NO_COVERAGE dans le SPI où elle vit) — même motif qu'au lot 2 avec
  `Contribution` ; et une garde `isEmpty()` redondante dans `JpaOptions`.

### 2026-08-04 — Jalon M7a : lot 4, les entités et les embeddables (FAIT)

Un commit (`1b19d3b`). **1 300 tests** (+19), zéro warning, `make ci` vert,
10/10 en intégration, cliquet inchangé. **Le module jpa est à 100 % de mutants
tués — 85/85.**

Le plugin est enregistré (ServiceLoader) et se démontre **à travers
l'exécuteur**, seul chemin qu'un build prend. Dépendance nouvelle :
**javapoet 0.18.0** — Java écrit comme un arbre de syntaxe et imprimé depuis
lui, pas comme du texte. *(La carrière est en 0.11.0 ; l'utilisateur a signalé
la 0.18.0 alors que ma recherche avait rendu « 0.7.0 » — l'API de recherche
Maven trie les versions **lexicographiquement**. Montée éprouvée localement
puis vérifiée par `dependency:tree`, comme le veut la méthode Dependabot.)*

- **Ce qui décide de la forme d'un champ est le verdict sur ce qu'il tient**,
  et rien d'autre — pas son nom, pas sa forme. `Stored` demande au modèle le
  kind de l'élément : une valeur devient un embeddable, une partie ou un
  agrégat devient l'entité générée pour lui, une identité devient **la valeur
  qu'elle enveloppe**. Un agrégat identifié par un `OrderId` est retrouvé par
  l'`UUID` dedans, et une colonne portant le wrapper est une colonne qu'aucune
  requête ne joint.
- **Une entité et un embeddable sont le même exercice** à une différence près
  — ce qui a une vie propre porte une identité et une table —, donc une seule
  classe (`StoredType`) plutôt que deux qui divergeraient. Le type généré a un
  constructeur sans argument et des accesseurs en lecture seule : ce n'est pas
  une opinion sur le domaine, c'est ce que le fournisseur de persistance exige
  de ce qu'il instancie — et la raison pour laquelle on ne demande pas au
  domaine de ressembler à ça.
- **Le seuil de D28 a son premier consommateur réel.** Sous `minConfidence`,
  aucun code : un diagnostic `HG-JPA-001` qui dit le kind lu, la confiance
  atteinte, le seuil demandé, **et transporte la remédiation que le moteur a
  écrite pour ce type précis** — ce qui rend un verdict plus sûr est une
  propriété du type, pas du backend qui a décliné. Un second code,
  `HG-JPA-002`, pour l'agrégat dont rien ne nomme l'identité : une ligne ne se
  retrouve pas sans clé. Dans les deux cas le plugin **continue** — c'est
  exactement ce que `DiagnosticSink` a été posé pour permettre.
- **Ce que PIT a coûté et rapporté, en quatre passes** (89 % → 98 % → 100 %) :
  une méthode `source()` que personne n'appelait ; une condition morte dans
  `storable` (`|| identity.isPresent()` ne pouvait jamais être vraie quand le
  reste était faux) ; trois branches qu'aucune fixture n'atteignait — un champ
  tenant **une partie** entière, un champ tenant **un autre agrégat** entier,
  un identifiant dont l'analyse n'a pas vu l'intérieur ; et un `DomainEvent`,
  pour lequel le store doit se taire **sans même un diagnostic**.
- **Le dernier mutant a demandé l'assertion la plus intéressante du lot** :
  que le champ portant l'identité d'un autre agrégat reste **une colonne** et
  ne devienne pas une jointure. C'est la règle DDD que le générateur applique
  sans qu'aucun test ne l'ait dite jusque-là ; les quatre assertions
  précédentes vérifiaient toutes le cas inverse.
- **Deux leçons de forme** : les assertions sur du code généré doivent lire ce
  qu'il **dit**, pas sa mise en page — javapoet imprime une annotation à
  membre sur trois lignes, d'où un `flat()` qui écrase les blancs ; et une
  assertion sur une annotation doit la **lier à son champ**
  (`@ManyToOne private ShipmentEntity shipment;`), sinon n'importe quel autre
  champ annoté la satisfait.
- **Piège PMD reconfirmé** : `LinguisticNaming` refuse une méthode de test
  nommée `toXxx`, lue comme une transformation devant retourner une valeur.
  Déjà consigné pour `asXxx` au lot 14 de M3.

### 2026-08-04 — Jalon M7a : lot 5, les dépôts (FAIT)

Un commit (`8874576`). **1 312 tests** (+12), zéro warning, `make ci` vert,
10/10 en intégration, cliquet inchangé. Module jpa : 93 % de mutants tués
(132 mutations), au niveau du reste du réacteur.

**Le lot a été resserré à son ouverture, et il faut le dire** : le plan
annonçait « repositories **+ adapters de ports** ». L'adapter convertit
l'entité en objet du domaine, donc il appelle le mapper — qui est au lot 6.
Écrire l'adapter ici produirait du code qui ne compile pas avant le lot
suivant, contre le critère de sortie de M7a. **Les adapters partent donc au
lot 6, avec les mappers dont ils dépendent** ; le lot 5 livre le dépôt, qui
compile seul.

- **La carrière dérive ses requêtes en lisant le nom des méthodes du port** —
  `inferMethodKind(methodName)`, et `findAllActive` devient
  `findByActiveTrue`. C'est l'interdit §10.9 au cœur du générateur : un domaine
  qui nomme autrement n'obtenait rien. **La v7 dérive de la forme** : une
  méthode mérite une requête quand **chacun de ses paramètres correspond, par
  son type, à un champ de l'agrégat** qui n'est pas son identité — chercher par
  identité étant ce que `findById` fait déjà. Ce que la réponse **est** dit
  laquelle des trois questions c'est : une vérité est une existence, un nombre
  est un compte, le reste est une recherche.
- **Un nom est bien écrit, mais ce n'est pas le même geste** : Spring Data
  construit sa requête depuis le nom de la méthode, donc `findByCustomer` est
  produit — depuis le nom du **champ de l'entité que nous venons de générer**,
  pas depuis un nom du code de l'utilisateur. Écrire un nom qu'un framework
  impose n'est pas lire un nom pour en déduire une architecture.
- **Trois refus, trois codes** : `HG-JPA-001` (verdict sous le seuil, sur le
  port **ou** sur l'agrégat qu'il garde), `HG-JPA-002` (l'agrégat n'a pas
  d'identité nommée — il n'y a pas de clé pour servir des lignes),
  **`HG-JPA-003`** (le port ne garde rien que l'analyse ait su nommer : un
  magasin sert une chose, pas n'importe laquelle).
- **Ce que PIT a fait dire au code** (91 % → 95 % → 93 % après ajout de
  fixtures) : aucune requête ne rendait **un seul** agrégat — toutes les
  méthodes de la fixture rendaient une liste ou un booléen, donc la branche
  `Optional` n'était jamais écrite ; aucun port n'était d'un autre type que
  REPOSITORY ; aucun agrégat gardé n'était sans identité.
- **Un comportement découvert en écrivant le test, et épinglé** : quand un port
  demande la même chose de deux façons (`List` et `Optional` du même champ),
  les deux produiraient **le même nom Spring Data**. La première rencontrée
  gagne, la seconde est écartée — deux méthodes de même signature ne peuvent
  pas coexister. C'est déterministe et c'était implicite ; un test le dit
  maintenant.

### 2026-08-04 — Jalon M7a : lot 6, les mappers (FAIT, PARTIEL)

Un commit (`bf11931`). **1 317 tests** (+5), zéro warning, `make ci` vert,
10/10 en intégration, cliquet inchangé. Module jpa : **83 % de mutants tués**
(201 mutations) — **en retrait des 93-100 % des lots 4-5**, le code neuf
(`StoredMapper`, `DomainAccess`) étant le moins couvert.

**Ce que le lot ne livre pas, et qui reste à faire : les adapters de ports.**
Le lot 5 les avait déplacés ici pour qu'ils arrivent avec les mappers dont ils
dépendent ; les mappers ont pris tout le lot. **Les adapters passent au lot 7**,
avec `produces` (D27 côté déclarant), qui y trouve d'ailleurs sa place plus
naturellement — le lot 7 est celui où le juge le lit.

**Les deux décisions du lot sont au registre comme D30.** (Le mapper est le
seul endroit de M7a où le générateur **appelle le code écrit à la main** :
partout ailleurs il écrit des fichiers neufs, et une erreur coûte une
régénération ; ici elle casse la compilation du projet de l'utilisateur) :

1. **Lire l'état du domaine par la forme, le nom en départage seulement.** Une
   méthode sans paramètre dont le type de retour est celui du champ ; si une
   seule répond, c'est elle, aucun nom n'est lu. Si plusieurs (deux champs
   `Money`), départage par les trois orthographes que **le langage** impose —
   composant de record, `get`, `is` — jamais par une convention du projet lu.
   La carrière faisait `"get" + nom capitalisé` sans regarder les signatures.
2. **Reconstruire par le constructeur qui prend l'état**, dans l'ordre — celui
   d'un record y répond toujours. Aucune factory devinée par son nom, ce que la
   carrière faisait (`reconstitution.factoryMethodName()`).

- **Le mapper est tout ou rien.** Un mapper qui porterait la moitié d'un type
  perdrait le reste à l'aller et reconstruirait autre chose que ce qui a été
  stocké. Il s'écrit entier, ou pas du tout avec `HG-JPA-004` **nommant le
  champ qui a bloqué**. Les collections ne passent pas encore : rendre une
  liste de lignes en collection du domaine demande au domaine de dire comment
  il en prend une, ce que rien du modèle n'énonce.
- **Amendement du lot 4 qu'exigeait le mapper** : l'entité n'avait que des
  accesseurs, donc rien ne pouvait la construire. Elle a maintenant un
  constructeur complet — pas de mutateurs pour autant : une ligne est lue par
  le fournisseur ou bâtie d'un coup.
- **Un vrai bug trouvé par un test** : le mapper d'une valeur pointait vers
  `MoneyEntity`, une classe que ce backend n'écrit jamais — une valeur est
  stockée dans un `Embeddable`. `Stored.entity()` était appelé pour tout.
- **Ce que le générateur produit, vérifié sur pièce** :
  `new InvoiceEntity(domain.id().value(), domain.reference())` à l'aller,
  `new Invoice(new InvoiceId(row.getId()), row.getReference())` au retour.

### 2026-08-04 — Jalon M7a : lot 7, les adapters et D27 (FAIT)

Six commits (`76e8e93`, `631b128`, `962c537`, `3c06416`, `a561e8f`, plus la
remise au vert de PMD). **1 377 tests** (+60), aucun skipped, `make ci` vert,
**11/11 en intégration** (un cas neuf), cliquet inchangé. Module jpa : **96 %
de mutants tués** (270/281) — au-dessus des 93 % du lot 5, contre **83 % à
l'ouverture du lot**.

**Les warnings, comptés et non regardés** : 36 au total sur `make ci`, **aucun
de compilation**. Quatre familles, toutes issues des plugins de rapport Maven :
12 « Unable to locate Source XRef », 12 idem côté tests, 11 « Overwriting
artifact » (le jar d'un module réécrit par son `target/classes`), 1 « JAR will
be empty » (l'agrégateur `hexaglue-plugins`, qui n'a pas de sources). Aucune ne
concerne le code du réacteur ; elles sont inhérentes au harnais de rapport.

**Deux violations PMD introduites et soldées dans le lot** : `LooseCoupling`
sur un `TreeMap` local (devenu `SortedMap`, l'interface qu'exige
`unmodifiableSortedMap`) et `UnusedFormalParameter` sur un helper de test.
**Piège de mesure au passage** : `make ci` passe par `tee`, donc son code de
sortie est celui de `tee` et vaut 0 même quand Maven échoue. Lire `BUILD
SUCCESS` dans le log, jamais `$?`.

**Le lot a trouvé deux défauts que personne ne cherchait**, et c'est le fait
marquant : ni l'un ni l'autre n'était visible depuis les tests unitaires.

1. **Le jar de jpa ne déclarait aucun service.** Au lot 4 le descripteur
   `META-INF/services/io.hexaglue.spi.HexaGluePlugin` a été écrit sous
   `src/main/java/io/hexaglue/plugin/jpa/hexaglue-plugins/…/src/main/resources/…`
   — un chemin relatif résolu depuis le mauvais répertoire, qui a créé une
   arborescence entière **dans les sources Java**. Le fichier n'a donc jamais
   été dans le jar : **aucun build réel n'a jamais découvert ce backend**. Les
   tests du module instancient `new JpaPlugin()` directement, donc rien ne
   pouvait le dire. Le journal du lot 4 affirmait « le plugin est enregistré
   (ServiceLoader) » : c'était vrai du dépôt, faux de l'artefact.
2. **jpa n'était pas dans les `extraArtifacts` du harnais d'intégration** — le
   POM du maven-plugin explique pourtant en commentaire pourquoi ils y sont
   (« rien ne rafraîchirait les backends dans le dépôt isolé »). Les ITs des
   lots 4-6 ne l'installaient donc pas, ce qui a laissé le premier défaut
   invisible trois lots durant.

**Piège d'hôte à consigner** : `make integration` exécute `mvn install`
**avant** l'invoker, et l'install du maven-plugin passe par la phase
`integration-test` — donc **un IT qui échoue empêche l'installation du
correctif qui le ferait passer**. Le build reste alors sur le jar précédent en
silence. Contournement : `mvn install -DskipTests -Dinvoker.skip=true` avant de
relancer les ITs. (Diagnostic coûteux : ~40 minutes à croire à un bug de D27
alors que l'hôte servait un jar d'une heure.)

- **Les adapters (`StoredAdapter`)**, reportés du lot 6. Un adapter est **tout
  ou rien** : une classe qui implémente un port l'implémente en entier, donc
  une méthode sans réponse ferait lever le code généré — pire qu'un fichier
  jamais écrit. `HG-JPA-005` nomme **toutes** les méthodes en cause.
- **D31 arbitrée en cours de lot** (au registre) : la forme décide, et quand
  deux opérations du magasin partagent une forme, le mot du magasin départage.
  Voir le registre pour la règle et ses conséquences.
- **Une seule implémentation de la dérivation** (interdit §10.6) : le dépôt et
  l'adapter lisaient tous deux « quelle question du port le magasin répond ».
  `StoreQuestion` est née de là, et `StoredRepository` a été reposé dessus sans
  changer une ligne de sa sortie. Même geste pour `Crossing`, extraite du
  mapper : ce qui traverse entre le domaine et sa ligne est dit une fois.
- **`@Generated` sur tout ce que le backend écrit** (`Written`). Le réacteur
  n'en posait aucun, alors que le frontend écarte les types générés du
  périmètre (D15/D19) : sans cette annotation, la seconde lecture d'un projet
  aurait classé ses propres adapters comme l'architecture. C'est la condition
  sous laquelle D27 tient.
- **D27 des deux côtés** : `PortFamily` (scellée : famille de ports pilotés,
  ou pilotants) et `Backends` (ce que les backends installés déclarent) au
  modèle ; `PluginManifest.produces` au SPI ; `Judgement` gagne son quatrième
  composant ; HG-HEX-002/005 se taisent sur un port couvert **en le disant**
  (`HG-ENGINE-005`, INFO, un seul diagnostic qui compte les ports et nomme le
  backend). Les trois goals le tendent au moteur, `validate` compris.
- **Le canal INFO du log a dû être scindé** : `Diagnostics` comptait tout INFO
  comme « types non analysés » et renvoyait le reste en `-X`. Un diagnostic qui
  parle **du run** (aucun sujet) et non d'un type serait passé sous silence —
  exactement ce que D27 interdit. La distinction est le sujet : ce qui nomme un
  type est compté, ce qui parle du run est dit.
- **Ce que le juge lit ne passe pas par le contexte du moteur** : `Backends`
  est tendu à `Analysis.analyze(context, backends)` et rejoint le `Judgement`,
  jamais l'`EngineContext`. Une règle qui pourrait le lire ferait dépendre la
  classification des plugins installés.
- **La passe de couverture a porté sur les deux classes qui touchent le code
  écrit à la main**, les seules où une erreur casse le build de l'utilisateur :
  `DomainAccess` était à **48 %** (elle n'avait aucun test direct, seulement
  l'exercice indirect du plugin) et `Crossing` à 71 %. Toutes deux ont
  maintenant un test qui énonce chaque règle contre le plus petit type qui peut
  la casser. `StoreQuestion` (80 %) a reçu le sien : c'est la doctrine de D31,
  et PIT disait qu'aucune de ses alternatives n'était éprouvée.
- **Une méthode morte trouvée par PIT** : `DomainAccess.local`, que personne
  n'appelait (interdit §10.7). Supprimée.

**`case-study-banking` rejoué, et la mesure est exactement celle que D27
annonçait.** La chaîne complète (frontend + moteur) passée sur les 48 types du
projet, deux fois, avec pour seule différence ce que les backends déclarent :

| Ce que le build installe | HG-HEX-002 | Total des findings |
|---|---|---|
| aucun backend | **5** | 5 |
| jpa (ports pilotés à rôle REPOSITORY) | **0** | 0 |

Les cinq sont `AccountRepository`, `CardRepository`, `CustomerRepository`,
`TransactionRepository`, `TransferRepository` — les cinq fausses alertes de la
clôture M6, mot pour mot. Le second run n'a plus aucun finding et **dit
pourquoi** : un `HG-ENGINE-005` qui compte les cinq ports, les nomme, et nomme
le backend. C'est le dossier de D27 refermé.

**Ce que le lot 7 a découvert et qui change le lot 8.** Le premier vrai build
avec jpa découvert montre le backend refusant presque tout d'un domaine
pourtant idiomatique :

```
HG-JPA-004  Money : rien en lui ne prend son propre état
HG-JPA-004  Order : son champ id ne peut pas traverser
HG-JPA-005  Orders : le magasin n'a pas de réponse pour findById(OrderId)
```

Une seule cause, et elle n'est pas dans jpa : **les membres implicites d'un
record sont absents du modèle**. `Members.java` du frontend filtre
`!method.isImplicit()` et `!constructor.isImplicit()` — une ligne écrite quand
le modèle ne servait qu'à *classer* (un accesseur implicite n'apprend rien sur
ce qu'un type est). Elle devient fausse dès qu'un générateur doit **appeler**
ces membres : d'un `record Money(BigDecimal amount, String currency)` le modèle
rend `ctors=0, methods=[]`. Or D30 énonce « un composant de record répond sous
son propre nom » — le modèle n'a même pas la méthode.

Les fixtures des lots 4-6 construisaient ces membres à la main
(`Constructor.of(...)`, `answers("value", UUID)`), ce qui rendait le défaut
invisible. **Aucun domaine à base de records ne peut aujourd'hui être généré**,
et le critère de sortie de M7a est « un exemple généré **et compilé** ».

**Prochaine étape : lot 8**, dont c'est désormais la tête — rendre au modèle
les membres qu'un record déclare implicitement (frontend, avec les goldens à
relire), avant le goal `generate` et l'exemple compilé. La portée du lot 8 est
donc plus large que « câbler l'hôte » : décision de découpage à prendre à son
ouverture.

### 2026-08-04 — Jalon M7a : lot 8, ce qu'un record déclare et le goal qui écrit (FAIT)

Deux commits (`2e22a6e`, `1f3d60a`). **1 384 tests**, aucun skipped, `make ci`
vert, **12/12 en intégration** (deux cas neufs), cliquet inchangé.

**Le correctif du frontend tient en une ligne, et sa justification en une
phrase** : *ce qu'un record écrit dans son en-tête EST sa déclaration*. Les
accesseurs de composants et le constructeur canonique reviennent au modèle ; ce
qu'une classe obtient pour n'avoir écrit aucun constructeur reste dehors — il ne
prend rien et n'énonce rien. Instruit avant d'être écrit, contre Spoon : les
accesseurs sont bien des méthodes implicites, le constructeur canonique n'est
implicite **que** si le record n'écrit pas de constructeur compact, et
`equals`/`hashCode`/`toString` **ne sont pas rendus du tout** — la crainte d'un
`toString()` venant concurrencer un accesseur de type `String` était sans objet.

- **Ce que le cliquet prouve, et ce qu'il ne prouve pas.** La suite complète est
  restée verte : **aucun verdict n'a bougé**, ce qui est exactement ce qu'un lot
  qui ajoute de l'information sur des membres doit faire. Mais
  `ArchModelSnapshots` ne rend les méthodes **que des ports** — les accesseurs
  rendus à un record sont invisibles au golden. Le piège des lots 1 et 21, à
  l'identique : la verdure prouve que la classification n'a pas bougé, pas que
  les membres sont là. **Ce que les membres changent est prouvé ailleurs** : sur
  le projet d'intégration, les trois refus de jpa (`HG-JPA-004` sur `Money`,
  `HG-JPA-004` sur `Order`, `HG-JPA-005` sur `Orders`) ont disparu — le backend
  écrit désormais les six fichiers sans un seul diagnostic.
- **Le goal `generate`** (D28) : lié à `GENERATE_SOURCES`, il écrit sous
  `target/generated-sources/hexaglue`, appelle `addCompileSourceRoot` et **ne
  juge rien**. Le seul geste de conception à trancher a été le routage : un goal
  tourne dans UN module, donc ce qu'un backend adresse à un autre module
  (`targetModule`) n'est pas à ce run de le placer — c'est laissé au run du
  module nommé, et **dit** plutôt que jeté (`Sources.addressedTo` /
  `addressedElsewhere`).
- **La racine de sources est ajoutée même quand rien n'a été écrit** : un build
  qui ne génère rien cette fois garde ce qu'un run précédent a laissé, et
  retirer la racine sortirait ses propres sources de sous ses pieds.
- **L'exemple est généré ET compilé** (`generated-code-compiles`) : six types
  écrits depuis un domaine lu de vraies sources — entité, embeddable, deux
  mappers, l'interface Spring Data et l'adapter — puis **javac les accepte**.
  C'est le critère de sortie de M7a, tenu sur pièce.
- **Les sorties sont golden-diffées**, et le golden a été **éprouvé** : une
  altération d'un caractère dans `OrderMapper.java` fait échouer le cas, la
  restauration le remet au vert. Un golden qu'on n'a pas vu mordre ne prouve
  rien (leçon du lot 21).
- **Doc publique corrigée dans le même lot**, comme l'exige la règle posée à la
  clôture M6 : le README racine annonçait « trois goals », il en annonce quatre
  et dit ce que `generate` ne fait pas ; le paragraphe sur le code généré, qui
  s'arrêtait au constat, dit maintenant ce que D27 en fait. **jpa a enfin son
  README** (le seul backend qui n'en avait pas) : ce qu'il écrit, ce qu'il lit
  par la forme, ce qu'il refuse et ses cinq codes, ses douze options, et ce
  qu'il déclare avant de tourner.

### 2026-08-04 — Après la clôture de M7a : premier banc sur un projet réel

Nouveau répertoire **`_probes/`** (hors réacteur, hors corpus) : y passer le
réacteur sur de vrais projets, en lisant leurs sources **là où elles vivent**
(rien n'est copié, donc un banc ne peut pas dériver de ce qu'il mesure) et en
n'écrivant que sous son propre `target/`. Un banc n'est pas un test : ce qu'il
trouve se fige ailleurs — au journal, au registre, ou en test du réacteur.

Premier banc : **`case-study-ecommerce/hexagonal`** (56 fichiers, 6 agrégats,
8 ports pilotés). Détail dans `_probes/ecommerce-hexagonal/FINDINGS.md`. Le
projet porte un `hexaglue.yaml` v6 que la configuration stricte v7 refuserait ;
l'intention est **restatée dans le banc**, le projet observé n'est pas touché.

Résultat : **15 types écrits, aucun échec de build, et un seul agrégat sur six
servi**. D27 tient sur un vrai projet (six fausses alertes évitées, et dites).
Deux défauts, qu'aucune des deux suites du réacteur ne pouvait voir :

1. **Le code généré pour un enum ne compile pas** (bloquant). Un enum est lu
   VALUE_OBJECT, donc le backend lui écrit un `@Embeddable` ; n'ayant aucun champ
   d'état, celui-ci sort avec **deux constructeurs sans paramètre** — vérifié au
   compilateur : `constructor OrderStatusEmbeddable() is already defined`. Cinq
   fichiers de ce type ici. Le backend n'a aucune notion d'enum, alors que JPA en
   a une (`@Enumerated`, ni embeddable ni mapper) ; d'où en cascade 5 `HG-JPA-004`
   et un `HG-JPA-005` sur `findByCategory(Category)`. **Aucune fixture des lots 4
   à 8 ne contenait d'enum.**
2. **Cinq agrégats sur six perdent leur identité** (structurant) : R2 énonce
   `IDENTIFIED_BY` pour « la clé par laquelle le port cherche l'agrégat,
   **exactement une, sinon silence** », et le seul port qui aboutit est le seul
   qui ne cherche jamais par l'identité d'un autre agrégat. `findByOrderId`,
   `findByCustomerId`, `findByEmail(Email)` suffisent à faire taire la règle.
   Le départage manquant est dans le modèle sans rien inventer : **l'agrégat
   détient un champ du type de sa propre identité**, et une seule des clés
   candidates est un de ses champs. Doctrine du moteur, donc **question de
   registre**, à instruire avant M8.

### 2026-08-04 — Fin de session : la doc publique remise au niveau du code (hors jalon)

Deux commits (`a687c4e`, `3a62fb8`). Passe sur les quatre README de
`hexaglue-next/`, en corrigeant ce qui était devenu **faux** plutôt qu'en
réécrivant ce qui allait.

- **README racine** : l'accroche promettait la génération « tomorrow » — elle est
  livrée ; `hexaglue-plugins` ne listait que deux backends (jpa manquait au
  tableau des modules **et** à l'exemple `hexaglue.yaml`) ; `hexaglue-spi`
  décrivait « one pure function … to the documents it wants written », trop
  étroit depuis D27/D28 (types Java, diagnostics, `produces` au manifeste) ;
  `hexaglue-maven-plugin` ne parlait que de documents. Une phrase ajoutée à
  `generate` : ce qu'il écrit porte la marque de génération, ce qui referme la
  boucle avec « What is read, and what is not ».
- **README jpa** : l'enum manquait entièrement — une ligne au tableau (il n'écrit
  **rien** pour lui) et le pourquoi de `EnumType.STRING`.
- **README audit** : le tableau des findings ne disait pas que `HG-HEX-002` et
  `HG-HEX-005` se taisent sur une famille couverte, ni que le run le dit.
- **README living-doc** : **inchangé après vérification** — rien de la session ne
  touche ce qu'il écrit ni ce qu'il accepte.

**Chaque affirmation ajoutée est adossée à un test ou à un cas d'intégration
existant** (le `@Generated` par `soTheNextReadingLeavesItOut`, la colonne d'enum
par `storingItInTheColumnOfWhateverHoldsIt`, le silence de l'audit par l'IT
`generated-adapters-are-not-holes`, la compilation par `generated-code-compiles`)
et les six liens relatifs résolvent.

**Piège d'outillage** : le garde-fou de session refuse `git commit --amend` ; une
précision de formulation part donc en second commit plutôt qu'en réécriture.

### 2026-08-04 — Les deux défauts du banc, corrigés (hors jalon)

Deux commits (`e07c52d`, `7b208d8`). `make ci` vert, cliquet inchangé
(143/143 + 6/6 + 5/5), 12/12 en intégration.

**Défaut 1 — l'enum, dans le backend.** Un enum était lu VALUE_OBJECT, donc jpa
lui écrivait un `@Embeddable` ; sans champ d'état, celui-ci sortait avec **deux
constructeurs sans paramètre** et ne compilait pas. Le correctif est une lecture
de plus, prise au modèle et non au nom : `TypeNature.ENUM`. Une valeur qui est
l'une d'un ensemble fermé **reste elle-même** — colonne
`@Enumerated(EnumType.STRING)` dans ce qui la tient, ni embeddable ni mapper, et
elle traverse `Crossing` sans rien. **`STRING` et non le rang** : le défaut du
fournisseur est l'ordinal, et le jour où une constante s'insère au milieu, toutes
les lignes déjà écrites veulent dire autre chose.

**Défaut 2 — l'identité, dans le moteur : D32 au registre.** R2 exigeait
« exactement une clé, sinon silence » ; sur du code réel cinq agrégats sur six en
ont deux (`findByCustomerId` à côté de `findById`), et R2 étant **l'unique source
du lien `IDENTIFIED_BY`**, son silence prive le modèle de l'identité *et* laisse
le duel S4 du wrapper ouvert. Arbitrage utilisateur : **A+B**, deux lectures
structurelles, aucun nom lu.

- **A, la forme de la réponse** : seule une méthode qui retrouve **au plus un**
  agrégat (`A` ou `Optional<A>`) élit une clé ; une qui rend `List<A>` filtre
  parmi eux. Le réacteur possédait déjà cette distinction — dans jpa
  (`StoreQuestion`) — pas dans le moteur.
- **B, le point fixe** : à plusieurs candidates restantes, une clé déjà identité
  d'un **autre** agrégat s'écarte. **Garde de monotonie** : B départage, il ne
  met jamais de veto sur une clé unique — sans quoi l'arrivée tardive d'un
  `IDENTIFIED_BY` reprendrait une élection déjà émise, et une conclusion
  rétrécirait sous saturation. Un test l'épingle.
- **C écartée** (le mot du magasin, `findById`, en dernier ressort) : lire un nom
  dans le classifieur est le cœur de l'interdit §10.9, et là une lecture fausse
  se propage au modèle entier — au lieu d'un fichier généré, comme à D31.

**Mesuré sur le banc** : identifiants lus IDENTIFIER **1/6 → 5/6** ; laissés
entre candidats 6 → 2 ; non classés 14 → 10 ; types générés 13 → 25.
`CustomerId` reste seul muet, et c'est le silence annoncé : `findByEmail(Email)`
rend `Optional<Customer>` exactement comme `findById`, rien de structurel ne les
sépare (cas D16, le rapport porte la question).

**Le cliquet n'a pas bougé, et c'est une information** : aucun des 154 scénarios
n'a de dépôt à deux clés — tous cherchent par une seule ou par `findAll()`. D32
est tenue par cinq tests de règle passant par le `Classifier` réel, pas par le
corpus. **Lacune du corpus mise au jour par le banc.**

**Deux défauts de plus, non traités et consignés** : **D33 (PENDING)** — `Email`
sort DOMAIN_EVENT à HIGH parce que deux signaux R7 (un notifieur one-way lu
EVENT_PUBLISHER) pèsent plus que la possession par `Customer` (R3b, un signal,
même palier) ; et le **défaut 4**, le mapper exigeant que le constructeur
présente l'état **dans l'ordre des champs** (`Order` déclare `lines` en 4e et le
construit en 7e). Ce dernier n'a pas de correctif évident : apparier par type
seul écrirait, sur les cinq champs `LocalDateTime` d'`Order`, un mapper qui
compile et met `placedAt` dans `cancelledAt`. Un appariement sûr demande les noms
des paramètres (territoire de D30) ou **ce que le constructeur affecte**, que le
modèle sait porter sous `METHOD_BODIES`. À instruire.

### 2026-08-04 — Jalon M7a : CLÔTURE (règle 13 amendée)

État mesuré : `make ci` **vert**, **1 384 tests**, aucun skipped, **12/12 en
intégration**, **cliquet inchangé** (143/143 + 6/6 + 5/5, 154 goldens), module
jpa à **96 % de mutants tués**.

**Les warnings, comptés** : 37 sur `make ci`, dont **un seul de compilation** —
et c'est le comptage qui l'a trouvé, pas la lecture. `StreamResourceLeak`
(Error Prone) sur un `Files.list` de test laissé sans `try`-with-resources,
écrit dans ce lot même ; soldé. Les 36 autres sont le harnais de rapport Maven,
inchangés : 12 XRef, 12 XRef de test, 11 « Overwriting artifact », 1 « JAR will
be empty » (l'agrégateur `hexaglue-plugins`, sans sources).

**Relecture ligne à ligne du doc 07 §6.4** (ce que M7a prétendait livrer) —
quatre phrases, deux écarts :

| Ce que §6.4 énonce | Ce que M7a livre |
|---|---|
| « Seuil de certitude **par plugin** (`generation.minConfidence`, défaut HIGH) » | **ÉCART assumé** : le seuil est celui du **projet**, pas du backend (D28) — c'est la politique du build, la même question pour tout générateur, énoncée une fois. Défaut HIGH conforme. |
| « sous le seuil, diagnostic + remédiation au lieu de code faux » | **conforme** : `HG-JPA-001` porte le kind lu, la confiance atteinte, le seuil demandé, **et la remédiation que le moteur a écrite pour ce type**. |
| « Le plugin JPA lit la composition (`CompositionIndex`) et les identifiants (`DomainIndex`) depuis le modèle » | **ÉCART de moyen, fin tenue** : jpa n'appelle ni l'un ni l'autre. Il lit `all(DomainType.class)` et les liens que l'assemblage a posés (`identityField`, `managedAggregate`) — la même information, à sa source. La composition ne lui sert pas : il écrit un type stocké par type stockable et lit chaque champ par le verdict sur ce qu'il tient. |
| « zéro re-dérivation nominale (leçons 05-H4/H5) » | **conforme, et c'est la colonne vertébrale du jalon** : la carrière dérivait ses requêtes de `inferMethodKind(methodName)` ; v7 dérive de la forme (D30, D31). |

**Revue des dix interdits du §10, dans le code du jalon** :

- **§10.1 (abstraction sans second consommateur)** — un cas, **assumé et
  décidé** : `PortFamily.Driving` n'a aucun déclarant (jpa ne déclare que
  `Driven(REPOSITORY)`). D27 nomme explicitement les deux familles et son
  premier déclarant réel arrive à M7b avec rest ; le juge, lui, l'exerce déjà
  des deux côtés.
- **§10.6 (deux implémentations du même concept)** — **deux évitées
  activement** : `StoreQuestion` (le dépôt et l'adapter lisaient tous deux
  « quelle question du port le magasin répond ») et `Crossing` (le mapper et
  l'adapter font traverser les mêmes valeurs). Le dépôt a été reposé sur la
  première sans changer une ligne de sa sortie.
- **§10.7 (code mort publié)** — un trouvé par PIT, retiré : `DomainAccess.local`.
- **§10.9 (règle de nommage hors vocabulaire)** — recherche exhaustive de
  `endsWith`/`startsWith`/`Pattern` dans le code neuf : **une seule occurrence**,
  `SqlNames` (un nom de table, écrit vers SQL, jamais lu depuis le projet).
  **Deux lectures de nom subsistent, toutes deux arbitrées** : D30 (les trois
  orthographes que **le langage** impose, en départage seulement) et D31 (les
  mots que **le magasin** emploie, en départage seulement). Aucune ne lit une
  convention du projet analysé.
- **§10.2, 10.3, 10.4, 10.5, 10.8, 10.10** — RAS dans le code du jalon.

**Ce qui reste ouvert après M7a** : `validate` mono-module et la portée de la
mesure D13, tous deux datés de M8 ; les écarts au gate de parité M8 listés à la
clôture M6, plus les deux ci-dessus.

### 2026-08-05 — Jalon M7b : ouverture, rest (D34-D37)

Ouverture au protocole des jalons précédents : instruction du code de la
carrière d'abord, arbitrage utilisateur ensuite, aucun code écrit.

**Ce que la carrière fait, et qui ne passera pas.** Le backend rest de la
carrière pèse 5 339 lignes de production et 7 002 de test (48 fichiers), et
**dérive presque tout de noms** : le verbe HTTP par expression régulière sur le
préfixe de la méthode (`^(get|find|load|fetch).+`, puis un jeu de huit préfixes
de création `create|open|add|register|initiate|issue|new|save`), l'agrégat par
retrait d'un suffixe (`UseCases|Service|Port`), le paquet d'API par recherche
d'un segment `core|domain|model|port`, et le statut d'erreur par **quatorze
règles de suffixe** (`…NotFoundException` → 404, jusqu'à
`contains("Insufficient")` → 400). Le seul départage structurel de tout le
backend est le repli de l'association d'agrégat, au score (retour 2,
paramètre 1).

**Le fait qui manquait, mesuré plutôt que supposé.** v7 porte
`DrivingPort.useCases()`, mais `UseCase.type()` ne veut rien dire :
`Assembly:121` le pose à `void ? COMMAND : QUERY` et **personne ne le lit**
(living-doc n'affiche que les noms). Or sur les **27 cas d'usage** du banc
`ecommerce-hexagonal`, la forme ne sépare pas les lectures des changements
d'état :

| Forme partagée | Lisent | Changent |
|---|---|---|
| `[identité] → agrégat` | `getOrder`, `getCustomer`, `getProduct` | `placeOrder`, `deactivate` |
| `[une chaîne] → agrégat` | `getOrderByNumber` | `capturePayment`, `shipOrder`, `markDelivered` |

Contrairement à jpa, **aucun mot de magasin ne peut trancher** : l'échappatoire
de D31 était le vocabulaire que Spring Data emploie lui-même, et HTTP définit
des verbes, pas des noms de méthodes Java. Un GET qui mute n'est pas une erreur
de compilation — c'est un préchargement de navigateur qui passe une commande.

**Découverte d'instruction, non consignée jusqu'ici** : la capacité
`METHOD_BODIES` **n'est jamais demandée en production** — `ProjectSources` bâtit
sa requête sans aucune capacité. Toutes les règles marquées `[M]` au doc 09 (R5
en renfort, R6b) sont dormantes dans un vrai build depuis M5.

**Quatre arbitrages utilisateur** :

- **D34** — les corps sont lus en production, et `UseCase.type()` devient
  véridique : un cas d'usage **remet un type que le domaine possède** (agrégat
  ou entité) à un port piloté, donc il change quelque chose. Aucun nom lu.
  Vérifié sur les 27 cas d'usage ; la marche suit les renvois internes, parce
  que sur ce banc les lectures passent par une méthode privée
  (`getProduct` → `findProductOrThrow`) là où les changements remettent
  directement — une marche à un seul niveau serait sûre par accident ici et
  fausse ailleurs. Le doute penche vers POST.
- **D35** — ce dont un port pilotant parle devient un lien du moteur, énoncé par
  une règle : l'agrégat que ses cas d'usage prennent et rendent, **exactement
  un sinon silence**, comme R2.
- **D36** — la lecture du domaine de D30 (`DomainAccess`, `Crossing`) sort de
  jpa vers un commun. §10.1 est satisfait au moment même : le second
  consommateur existe, donc le commun s'écrit contre deux backends et non
  contre un plus une hypothèse.
- **D37** — le `@ControllerAdvice` global et la classe `@Configuration` de
  câblage sortent du périmètre. M7b livre des contrôleurs et leurs DTO.

**Ce que M7b reprend sans les rouvrir** : D27/D28/D30/D31, `SourceSink`, le
seuil de certitude, `@Generated`.

**Plan de lots M7b (rest)** :

| Lot | Contenu | Où |
|---|---|---|
| 1 | D34 : l'hôte demande les corps ; `Assembly.useCase` lit ce que l'implémentation remet ; le cliquet mesure ce que les règles `[M]` réveillées déplacent | maven-plugin + engine + acceptance |
| 2 | **D38** (ouverte par la mesure du lot 1) : un port pilotant se lit aussi par l'intérieur — answered par le cœur, détenu par personne. Sans elle le banc n'a aucun port pilotant et rest n'a rien à écrire | engine |
| 3 | D35 : la relation nouvelle, la règle qui l'énonce, `DrivingPort` qui la porte | engine + model |
| 4 | D36 : la lecture du domaine extraite hors de jpa, jpa reposé dessus sans changer une ligne de sa sortie | commun + jpa |
| 5 | rest, socle : options typées strictes, le nommage écrit **vers** HTTP, le manifeste déclare `produces` = `PortFamily.driving()` — le second déclarant, §10.1 de M7a soldé | plugin-rest |
| 6 | rest, les endpoints : forme + type du cas d'usage → verbe, chemin, statut ; le seuil appliqué ; ce que rien ne tranche refusé en le nommant | plugin-rest |
| 7 | rest, les DTO : requête et réponse depuis la lecture commune, tout ou rien comme le mapper | plugin-rest |
| 8 | rest, les contrôleurs : la classe complète, le port injecté, la traversée DTO↔domaine | plugin-rest |
| 9 | L'hôte et le banc : intégration — exemple généré **et compilé** ; banc `ecommerce-hexagonal` rejoué et mesuré | maven-plugin + acceptance + `_probes/` |

Clôture M7b (règle 13 amendée) : corpus vert + revue des dix interdits +
**relecture ligne à ligne de §6.4** + warnings comptés.

Aucun code écrit à l'ouverture : registre D34-D37, ce journal.

### 2026-08-05 — Jalon M7b : lot 1, ce qu'un cas d'usage fait (FAIT)

Un commit (`5a53135`). **1 392 tests**, aucun skipped, `make verify` **vert**
(0 violation Checkstyle), **cliquet déplacé de trois fichiers, tous relus**.

- **`Effects` (nouvelle classe du moteur)** lit, pour chaque cas d'usage d'un
  port pilotant, ce que le code qui y répond **remet** à un port piloté.
  `Assembly` la branche à la place du `void ? COMMAND : QUERY` qui tenait lieu de
  lecture. `Links` gagne `answering(contract)` — l'inverse d'`answeredBy`, et le
  seul chemin du port vers le code qui l'honore.
- **L'hôte demande `METHOD_BODIES`**, en projet seul comme en réacteur.
- **Le corpus lit ce qu'un build lit** : `AnalysisChain` demande la même
  capacité. Sans cela le corpus mesurait une chaîne que personne n'exécute — et
  c'est ce qui a failli faire passer ce lot pour neutre.

**Le cliquet, déplacé et arbitré** (protocole du lot 19) :

| Golden | Ce qui bouge | Verdict |
|---|---|---|
| `profile3/Armada-theWholeArmada.json` | `assemble: QUERY` → **COMMAND** | **retenu** : `Quartermaster.assemble` bâtit une `Fleet` et la **remet** à `fleets.keep(fleet)` ; `at(FleetTag)` ne fait que `fleets.find(tag)` et reste QUERY. Les deux rendent un `Fleet`, donc l'ancienne lecture les disait toutes deux QUERY. C'est le seul scénario des 154 qui porte la forme — **la même lacune de corpus que D32 a révélée**, sur un autre sujet. |
| `explanation-profile1.txt`, `explanation-profile3.txt` | R5 cite un signal de plus : `CALLED_BY_DRIVING_ADAPTER`, « ce que les corps montrent directement » | **retenu, et c'est le réveil attendu** : **aucun verdict ne change** — même kind, même HIGH — seule la preuve s'enrichit, 2 signaux au lieu d'1. C'est exactement le renfort que D14 avait dessiné et que personne n'exécutait. |

**Piège d'outillage, à retenir** : `mvn test -pl hexaglue-acceptance` **sans
`-am`** résout `hexaglue-engine` depuis le dépôt local, pas depuis le réacteur.
Trois exécutions « stables » de suite ont ainsi mesuré un moteur périmé, et le
golden régénéré sous elles est revenu identique à celui d'avant. C'est la lecture
des sources de la fixture (`Quartermaster` remet sa `Fleet`) qui a tranché, pas
la répétition du test. **Toute mesure de corpus se prend avec `-am` ou depuis la
racine.**

**Mesure sur le banc** : `hexaglue:generate` écrit toujours **25 types**, et les
verdicts sont **inchangés** (score 62, grade C, 5/6 identifiants, 10 non
classés). Allumer les corps ne coûte aucun verdict à ce projet.

**Ce que le banc a montré d'autre, et qui n'était pas cherché** : sur ses 49
types, **les sept interfaces `*UseCases` sont UNCLASSIFIED**, donc le projet n'a
**aucun port pilotant**. Vérifié à la source : `ExposedContract` (R5) est la
**seule** règle qui conclut DRIVING_PORT, et elle exige qu'un DRIVING_ADAPTER
détienne ou appelle l'interface ; or le projet observé n'a ni contrôleur ni
listener — `infrastructure/` ne contient que de la configuration, deux adapters
pilotés et des utilitaires. **Conséquence directe pour M7b : le backend rest
n'aurait rien à écrire sur le banc du chantier**, et plus généralement rien à
écrire sur un hexagone qui n'a pas encore sa couche web — c'est-à-dire sur le
projet qui en aurait le plus besoin. Miroir exact de D27. **Question de
doctrine, arbitrée le jour même : D38**, et traitée au lot 2.

### 2026-08-05 — Jalon M7b : lot 2, le port qu'aucun anneau n'appelle (FAIT)

Un commit. **1 397 tests** (+5), aucun skipped, `make verify` **vert** (0
violation Checkstyle), **cliquet déplacé de 5 scénarios sur 154, tous relus**.

**`OfferedContract` (R5b)** : une interface qu'un type du cœur implémente et
qu'**aucune déclaration du périmètre ne détient** est un port pilotant. La
clause qui la fait marcher est celle qui exclut le faux positif : un contrat que
le cœur garde pour lui est un contrat que le cœur **détient**. Palier **S4**,
sous le S3 de R5 — l'anneau qui parle vaut mieux que l'anneau qui manque — mais
confiance HIGH, sans quoi le seuil de génération (D28) refuserait tout ce que la
règle vient de rendre lisible.

**Le cliquet, déplacé et arbitré, scénario par scénario** — les cinq portent la
même phrase dans leur revue (« aucun adapter ne l'appelle, donc l'interface
reste indécise (R5) »), l'une la qualifiant même de « mesure de l'écart, pas un
oubli de la revue ». C'est exactement l'écart que D38 referme :

| Scénario | Ce qui bouge |
|---|---|
| `ClassificationGoldenFilesTest-createApplicationServiceExample` | `OrderUseCases` → DRIVING_PORT |
| `FlexibleApplicationServiceCriteriaTest-shouldMatchPivotClass` | `OrderUseCases` → DRIVING_PORT. **La carrière v6 lisait déjà ce port** (l'assertion héritée exige `justification()` contenant « driving port ») : v7 avait régressé là-dessus, et D38 le restaure |
| `…-shouldNotMatchAbstractClass` | `OrderUseCases` → DRIVING_PORT, `AbstractService` → APPLICATION_SERVICE |
| `…-shouldNotMatchInboundOnlyClass` | `QueryHandler` → DRIVING_PORT, `SimpleQueryHandler` → APPLICATION_SERVICE |
| `PortBoundary-contractNoRingCallsStaysSilent` | `Assembly` → DRIVING_PORT, `AssemblyLine` → APPLICATION_SERVICE. **Scénario renommé** `PortBoundary-contractNoRingCallsIsStillAWayIn` : son nom énonçait la doctrine que D38 retourne |

**Aucun verdict n'est perdu** : les cinq mouvements vont tous d'UNCLASSIFIED
vers un kind. La règle n'ajoute que de la lecture.

**Le gain mesuré là où le chantier le vise** : le harnais de réévaluation du
nommage (lot 23 de M3) passe de **damage 55 sur 47 scénarios à damage 53 sur
45**. Deux scénarios de moins dont le verdict dépend de la lecture des noms —
c'est le but même de §10.9, et il se lit sur un compteur plutôt que dans une
intention.

**Mesure sur le banc** : **0 → 6 ports pilotants**, non classés **10 → 4**,
score **62 → 65**. `InventoryUseCases` reste le seul muet, et c'est le silence
annoncé au registre **avant** d'écrire la règle : deux services applicatifs le
détiennent comme état, donc il a la forme d'un collaborateur interne. Les 25
types générés par jpa sont inchangés.

### 2026-08-05 — Jalon M7b : lot 3, ce dont un port pilotant parle (FAIT)

Un commit. **1 401 tests** (+4), aucun skipped, `make verify` **vert**
(0 violation Checkstyle). **Cliquet : 15 goldens déplacés, 15 lignes ajoutées,
zéro supprimée** — aucun verdict ne bouge, la règle n'énonce qu'un lien.

**D35 amendée AVANT d'écrire, sur mesure.** La formulation initiale disait
« l'agrégat que ses cas d'usage **prennent et rendent** », calquée sur la
convergence dont `PortSignatures` (W2-ROLE) lit un port piloté. **Cette lecture
ne se transporte pas** : un port piloté converge parce qu'il **reçoit**
l'agrégat pour le garder (`save(Order)`), un port pilotant reçoit une identité
et **rend** l'agrégat (`getOrder(OrderId) → Order`). Sur les **six** ports
pilotants du banc, **aucun ne prend jamais son agrégat en paramètre** :
l'intersection est vide sur les six et la règle en convergence se serait tue
partout. Ce qui se lit des deux côtés est « l'agrégat que les signatures
nomment », et c'est ce qui est écrit. Le registre porte l'amendement et sa
mesure.

- **`ExposedAggregate` (R9)** énonce `CONCERNS(port, agrégat)` quand les
  signatures d'un port pilotant nomment **exactement un** agrégat, conteneurs
  déballés. Zéro ou deux : silence — élire entre deux serait une supposition que
  les sources n'ont pas faite, et le backend peut dire qu'il n'a pas de
  ressource. `DrivingPort.subject` porte le lien comme `DrivenPort` porte
  `managedAggregate` ; `ArchModelSnapshots` le rend, sans quoi un golden
  continuerait de matcher pendant que le sujet change dessous.

**Ce que les 15 goldens disent, relus** : **3 sujets établis** (`Order` dans
l'exemple de service applicatif et dans le coffeeshop, `Fleet` dans l'armada),
**12 silences**, tous vérifiés à la fixture et tous honnêtes — la plupart de ces
scénarios n'ont aucun type de domaine du tout. Le cas instructif est
`GoldenFileTest-createBankingDomain` : `TransferUseCase.transfer(AccountId,
AccountId, Money)` ne nomme **aucun** agrégat, seulement deux identités et une
valeur. Le silence est le bon verdict — un virement est une action, pas une
ressource — et c'est le même cas de figure que « deux agrégats », traité
pareil.

**Extension examinée et ÉCARTÉE par la mesure** : faire qu'un identifiant nomme
son agrégat (le modèle porte `IDENTIFIED_BY`, rien à deviner) donnerait un sujet
à `TransferUseCase`. Mais sur le banc, `createShipment(OrderId, String) →
Shipment` nommerait alors **deux** agrégats — `Shipment` et `Order` — et
perdrait un sujet que la lecture directe obtient. L'extension gagne un cas
artificiel et en perd un réel : non retenue.

**Mesure sur le banc, prise et non prédite** (harnais jetable, retiré après) :
**6 sujets sur 6 ports pilotants**, chacun le bon agrégat — `CatalogUseCases` et
`ProductUseCases` → `Product`, `CustomerUseCases` → `Customer`,
`OrderUseCases` → `Order`, `PaymentUseCases` → `Payment`, `ShippingUseCases` →
`Shipment`. Score et verdicts du banc inchangés (65, grade C) : un lien de plus
ne déplace aucun kind.

### 2026-08-05 — Jalon M7b : lot 4, la lecture du domaine mise en commun (FAIT, PARTIEL)

Un commit. **1 401 tests** après `make ci` **clean** (compte identique au lot 3 :
la transplantation a emporté ses tests, ni plus ni moins), `make verify` vert
(0 violation Checkstyle), **12/12 en intégration**.

**`DomainAccess` vit désormais dans `hexaglue-spi`**, publique et documentée
comme API publiée. C'est la lecture de D30 — l'état d'un type, l'accesseur d'un
champ trouvé par la forme puis départagé par les trois orthographes que le
langage impose, et « ce type se reconstruit-il par un constructeur qui prend son
état ». Elle ne contient rien de JPA et répond à une question que tout backend
pose : la persistance lit un champ pour le mettre dans une ligne, une couche web
lit le même champ pour le mettre dans une réponse, et deux réponses
différentes voudraient dire que l'une est fausse.

**Où, et pourquoi pas un module** : `hexaglue-spi` est déjà la dépendance de
tous les backends et porte déjà plus qu'un contrat nu (`PluginExecutor`,
`PluginDiscovery`, `Measurements`). Un module pour une centaine de lignes serait
de la structure sans nécessité, et le doc 07 §2.1 ne liste pas de commun. Le
paquet suffit ; D36 laissait explicitement le choix ouvert (« un module **ou un
paquet** »).

**La preuve que D36 demandait, prise directement** : le dépôt local portait
encore le plugin d'avant le déplacement, donc le banc a été généré **avant**,
sauvegardé, puis regénéré **après** installation du réacteur déplacé.
`diff -r` sur les **25 fichiers** produits : **identiques, pas une ligne**. Le
vert des 12 cas d'intégration prouve que la sortie compile ; ceci prouve qu'elle
n'a pas bougé.

**Fin de session (2026-08-05)** : l'utilisateur arrête ici et annonce que **le
chantier a connu des dérives**, à résorber avant de continuer. La prochaine
session s'ouvre sur cette revue — voir le point de reprise en tête de fichier,
qui porte les observations de l'agent, à confirmer ou infirmer. Le lot 5 n'est
pas ouvert.

**`Crossing` n'est PAS extrait, et c'est délibéré.** Sa question est « comment
une valeur traverse entre le domaine et **la ligne** » : il écrit
`$T.toEntity(...)` / `$T.toDomain(...)` — les noms de méthodes du mapper que ce
backend génère —, nomme les types `Stored` et lit `JpaOptions.mapperFor`.
Ce qui est partageable dessous est la **partition** qu'il applique (un conteneur
ne traverse pas ; un type hors modèle passe tel quel ; une identité se déballe
sur son unique composant ; un ensemble fermé passe comme lui-même ; une valeur
avec état passe par son porteur ; ce qui a une vie propre ne traverse pas, il
est nommé par son identité). **Extraire cette partition aujourd'hui reviendrait
à la dessiner contre jpa plus une hypothèse sur rest** — exactement le piège du
§10.1, et exactement la discipline qui a fait attendre `DomainAccess` jusqu'ici.
Elle sera extraite **au lot 7**, quand le constructeur de DTO en sera le second
consommateur réel. Consigné comme dette datée, pas comme oubli.
