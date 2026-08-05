# Mesures — trois projets réels, pas un

> Auteur : **B**. Date : 2026-08-05. Commit mesuré : `hexaglue-next` à
> `d5386a2`, installé au dépôt local avant la mesure.
>
> Fait suite à [07-B-mesure-d33.md](07-B-mesure-d33.md), dont le constat
> reposait sur un seul banc. Six résultats, dont deux qui portent sur la manière
> de mesurer et invalident une partie de mes propres runs.

## Comment relancer

```bash
cd hexaglue-next/docs/chantier/20260805-convergence/signaux
mvn -q -o compile

# une ou plusieurs racines, séparées par ':' ; '*' pour tout dumper
mvn -q -o exec:java -Dexec.mainClass=io.hexaglue.probe.Signaux \
  -Dsignaux.classpath="<jars:séparés:par:deux-points>" \
  -Dexec.args="<racines> <basePackage> <exclusions|-> <type...|*>"
```

Le harnais affiche désormais le **registre de passage** — fichiers `.java` sous
les racines, types dans le `CodeModel`, types dans l'`ArchModel` — et **les
diagnostics des deux étages**. C'est l'écart entre ces trois compteurs qui dit
ce qui a été perdu en route, et le diagnostic qui dit pourquoi.

---

## R1 — Le classpath n'est pas un détail : il change le verdict

`spring-petclinic`, même sources, même configuration, seule différence le
classpath Spring Data :

| | sans classpath | avec classpath |
|---|---|---|
| UNCLASSIFIED | **14** | **10** |
| AGGREGATE_ROOT | 1 | **3** |
| VALUE_OBJECT | 1 | **2** |
| DRIVEN_PORT | 2 | **3** |

Cause : `VetRepository extends Repository<Vet, Integer>` — match direct sur le
FQN, reconnu sans classpath. `OwnerRepository extends JpaRepository<Owner,
Integer>` — la fermeture transitive vers `Repository` **passe par le bytecode**,
donc sans classpath `Owner` n'est pas un agrégat, `Pet` n'est la partie de
personne, et six types restent muets.

**Conséquence de méthode** : toute mesure qui omet le classpath ne mesure pas le
même moteur que la production. Mes trois premiers runs l'omettaient. Le recueil
d'oracle de E4a doit imposer le classpath, et le registre de passage doit le
dire quand il est absent — le harnais l'affiche maintenant en toutes lettres.

---

## R2 — D16 se reproduit à l'identique sur petclinic

Avec le classpath :

```text
== org.springframework.samples.petclinic.owner.Owner ==
  verdict : AGGREGATE_ROOT (HIGH, INFERRED)
== org.springframework.samples.petclinic.owner.Pet ==
  verdict : VALUE_OBJECT (HIGH, INFERRED)
    [S3/HIGH] OWNED_BY(…owner.Owner)   ancre=Owner
```

`Pet` porte `@Id Integer`, `Integer` est hors périmètre et n'a pas de verdict,
donc R3a ne voit pas d'identité et R3b conclut `VALUE_OBJECT`. C'est exactement
le cas que D16 décrit et tranche par « Q1 muet, Q2 le dit » — **vérifié sur le
projet réel**, pas seulement sur la fixture du corpus.

`Visit` reste UNCLASSIFIED : il n'est la partie de personne.

---

## R3 — L'invisibilité des enveloppes, confirmée sur un second projet

C'est le constat de [07](07-B-mesure-d33.md), qui n'était étayé que par
`case-study-ecommerce`. `case-study-banking` le reproduit, et **sans le
confondant** : cinq agrégats y sont reconnus, donc la composition fonctionne.

| Type | Forme | `OWNED_BY` | Verdict |
|---|---|---|---|
| `Address(street, city, zipCode, country)` | record multi-champs | **1** (Customer) | VALUE_OBJECT |
| `Money(amount, currency)` | record multi-champs | **5** | VALUE_OBJECT |
| `Email(String value)` | **enveloppe** | **0** | **UNCLASSIFIED** |
| `Iban(String value)` | **enveloppe** | **0** | **UNCLASSIFIED** |

Sur le même run, avec les mêmes propriétaires reconnus, un objet-valeur
multi-champs reçoit son signal de possession et un objet-valeur en enveloppe
n'en reçoit aucun. La cause est celle lue en 07 : `Lifecycle.isPart` écarte tout
ce que `Shapes.readsAsIdentity` reconnaît.

`Email` et `Iban` sortent tous deux sur un duel parfait :

```text
  candidat IDENTIFIER   (score 100)  [S4/HIGH] SINGLE_VALUE_WRAPPER(…)
  candidat VALUE_OBJECT (score 100)  [S4/HIGH] IMMUTABLE_SHAPE(…)
```

Et **trois des six `*Id`** de banking restent UNCLASSIFIED pour la même raison
(`CustomerId`, `BeneficiaryId`, `TransactionId`), là où `AccountId`, `CardId` et
`TransferId` sont sauvés par un port qui les prend comme clé.

**Le symptôme diffère, le mécanisme est le même** : sur ecommerce un notifieur
parle et l'enveloppe sort DOMAIN_EVENT à HIGH ; sur banking personne ne parle et
elle sort UNCLASSIFIED. Le premier cas est faux et générable, le second est un
silence honnête — mais dans les deux cas la possession n'a pas voix.

Petclinic ne porte aucune enveloppe (identités `Integer` nues), donc il
n'infirme ni ne confirme : il délimite la population concernée, qui est celle
des domaines à types-valeurs.

---

## R4 — Sur banking, 100 % des ports pilotants sont lus d'une absence

```text
DRIVING_PORT : 5
  dont R5b  OFFERED_BY_THE_CORE (absence) : 5
  dont R5   HELD_BY_DRIVING_ADAPTER (présence) : 0
```

Banking a pourtant un module `banking-api` **avec des contrôleurs**. Le registre
de passage dit pourquoi ils ne comptent pas :

```text
fichiers .java sous les racines : 99
types dans le CodeModel        : 48
[INFO] HG-FRONTEND-005 com.acme.banking.api.controller.AccountController
       was not analyzed: it is generated code, marked by @jakarta.annotation.Generated
       … (idem pour les 4 autres contrôleurs et 17 DTO)
```

Les adapters pilotants de banking **sont du code généré par HexaGlue**, écarté
du périmètre par D15.

**C'est la composition la plus préoccupante trouvée jusqu'ici, et elle ne vient
pas de D19 :**

> HexaGlue génère un contrôleur → le run suivant l'écarte parce qu'il est généré
> → R5 ne peut donc jamais mordre → R5b conclut « personne dedans ne le
> détient » → le port reste générable → HexaGlue régénère le contrôleur.

La règle qui lit une absence conclut d'une absence **que le pipeline crée
lui-même**. Sur tout projet qui laisse HexaGlue écrire ses adapters pilotants,
R5 est structurellement inatteignable et R5b est la seule voix. C'est P0a en
conditions réelles, avec un périmètre réduit non par une décision d'hôte mais
par un invariant produit.

À noter, honnêtement : **ajouter les deux racines que j'avais omises
(`banking-app`, `banking-persistence`) n'a déplacé aucun des cinq verdicts**.
Cette paire de vues-là ne viole donc pas P0a. Le risque décrit ci-dessus reste
un raisonnement sur le mécanisme, à confirmer par l'expérience de E3a.

---

## R5 — Deux exceptions sortent VALUE_OBJECT

```text
== com.acme.banking.core.exception.InsufficientFundsException ==
  verdict : VALUE_OBJECT (HIGH, INFERRED)
    [S4/HIGH] IMMUTABLE_SHAPE(…)
== com.acme.banking.core.exception.TransferRejectedException ==
  verdict : VALUE_OBJECT (HIGH, INFERRED)
```

Une exception immuable a la forme d'un objet-valeur, et rien ne l'en distingue.
`AccountNotFoundException` échappe au verdict parce qu'elle est mutable, ce qui
rend la règle d'autant plus arbitraire.

Le banc e-commerce ne pouvait pas le voir : son `hexaglue.yaml` de sondage
exclut `com.acme.shop.exception`. **Une exclusion de configuration masquait un
faux positif** — c'est l'argument de A pour que le dénominateur d'une mesure
vienne des sources et non du modèle.

---

## R6 — Le registre de passage marche, et il était sous nos yeux

Le canal de diagnostics de D20/D22 fait exactement ce que A demande en E4a : il
nomme chaque type écarté et pourquoi. Ce qui manquait n'était pas le mécanisme,
c'était **de le regarder** — mon propre harnais l'ignorait jusqu'à ce que
`banking-api` disparaisse sans explication.

Un point reste ouvert : sans classpath, le frontend signale
`HG-FRONTEND-006 : the parser recovered from 38 problem(s)` — un compteur
agrégé, sans la liste des déclarations incomplètes. Pour un oracle relu type par
type, un compte global ne suffit pas.

---

## Ce que ces mesures changent au plan

| Résultat | Effet |
|---|---|
| R1 | E4a doit **imposer le classpath** et le registre doit dire quand il manque. Toute mesure antérieure sans classpath est à refaire. |
| R2 | D16 est confirmée sur le réel ; sa famille (« modèle métier ») a son cas nominal pour E7. |
| R3 | Le constat de 07 tient sur **deux** projets et n'est pas un artefact du banc. La question « une enveloppe doit-elle rester invisible à la composition ? » entre en E7, famille modèle métier, avec deux cas nominaux et un contre-exemple (`Address`, `Money`). |
| R4 | **P0a gagne un cas d'école qui ne vient pas de D19** : le pipeline écarte sa propre sortie, et une règle conclut de cette absence. À instruire en E3a avant E5. |
| R5 | Un faux positif que la configuration masquait. Argument supplémentaire pour le dénominateur issu des sources (E4a). |
| R6 | Le canal existe ; E4a le consomme au lieu de le réinventer. Reste à détailler `HG-FRONTEND-006`. |

Rien n'est corrigé. La règle 1 du plan s'applique : ces six trouvailles sont
enregistrées, aucune n'est tranchée dans le lot qui l'a faite.
