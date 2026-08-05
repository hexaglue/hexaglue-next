#!/usr/bin/env bash
#
# Mesures de l'etat du reacteur v7, reproductibles a la demande.
#
# Toute affirmation factuelle de la controverse doit pouvoir se relancer ici.
# Le script ne lit rien d'autre que les sources et n'ecrit nulle part.
#
# Usage : ./mesures.sh [section]
#   sections : corpus | moteur | echelle | outillage | bancs | all (defaut)

set -euo pipefail

# La racine de l'espace de travail, quatre niveaux au-dessus de
# hexaglue-next/docs/chantier/20260805-convergence/ : les bancs et les projets
# observés vivent à côté du réacteur, pas dedans.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
readonly ROOT
readonly NEXT="${ROOT}/hexaglue-next"
readonly GOLDEN="${NEXT}/hexaglue-acceptance/src/test/resources/golden"
readonly RULES="${NEXT}/hexaglue-engine/src/main/java/io/hexaglue/engine/rule"
readonly MODEL="${NEXT}/hexaglue-model/src/main/java/io/hexaglue/model"

title() {
    printf '\n## %s\n\n' "$1"
}

require() {
    local tool="$1"
    if ! command -v "$tool" >/dev/null 2>&1; then
        printf 'outil manquant : %s\n' "$tool" >&2
        exit 1
    fi
}

# Combien de scenarios par profil, et combien de types chaque scenario met en jeu.
# Un moteur dont la these est « le role est une position dans un graphe » ne peut
# pas etre exerce par un scenario qui ne contient qu'un type.
corpus_taille() {
    title "Corpus : scenarios par profil"
    local profile count
    for profile in profile1 profile2 profile3; do
        count=$(find "${GOLDEN}/${profile}" -name '*.json' | wc -l | tr -d ' ')
        printf '%-10s %s scenarios\n' "$profile" "$count"
    done

    title "Corpus profil 1 : nombre de types par scenario"
    local file types
    while IFS= read -r file; do
        types=$(jq '[(.domain,.application,.ports,.adapters,.unclassified)|length]|add' "$file")
        printf '%s\n' "$types"
    done < <(find "${GOLDEN}/profile1" -name '*.json' | sort) | sort -n | uniq -c |
        awk '{printf "%3d scenarios a %s type(s)\n", $1, $2}'
}

# Ce que le cliquet certifie reellement : un scenario dont toutes les attentes
# sont UNCLASSIFIED atteste un silence, pas une classification.
corpus_silence() {
    title "Corpus profil 1 : ce que les attentes affirment"
    local file classes inconnus
    local -i total=0 muets=0 muets_mono=0 sum_classes=0 sum_inconnus=0
    while IFS= read -r file; do
        classes=$(jq '[(.domain,.application,.ports,.adapters)|length]|add' "$file")
        inconnus=$(jq '.unclassified|length' "$file")
        total+=1
        sum_classes+=classes
        sum_inconnus+=inconnus
        if ((classes == 0)); then
            muets+=1
            ((inconnus == 1)) && muets_mono+=1
        fi
    done < <(find "${GOLDEN}/profile1" -name '*.json' | sort)

    printf 'scenarios                      : %d\n' "$total"
    printf 'entrees classees               : %d\n' "$sum_classes"
    printf 'entrees UNCLASSIFIED           : %d\n' "$sum_inconnus"
    printf 'scenarios 100%% UNCLASSIFIED    : %d\n' "$muets"
    printf '  dont un type unique          : %d\n' "$muets_mono"
}

# D'ou viennent les scenarios : un nom de la forme « XxxTest-methode » est une
# methode de test de l'ancien reacteur, transplantee.
corpus_provenance() {
    title "Corpus profil 1 : provenance des scenarios"
    find "${GOLDEN}/profile1" -name '*.json' -exec basename {} .json \; |
        sed 's/-.*//' | sort | uniq -c | sort -rn |
        awk '{ if ($2 ~ /Test$/) h += $1; else n += $1; printf "%4d  %s\n", $1, $2 }
             END { printf "\n%4d transplantes de la carriere (nom en *Test)\n%4d ecrits pour le nouveau moteur\n", h, n }'
}

# La confiance portee par les goldens : si une seule valeur domine, le champ
# ne distingue rien et le seuil de generation qui le lit ne filtre rien.
corpus_confiance() {
    title "Corpus : valeurs de confiance attendues"
    find "${GOLDEN}" -name '*.json' -exec jq -r '..|.confidence? // empty' {} + |
        sort | uniq -c | sort -rn
}

# Les regles qui concluent d'une absence (« personne ne detient », « rien
# n'implemente ») sont sensibles au perimetre analyse : leur conclusion peut
# devenir fausse quand un fait apparait. Il faut savoir combien il y en a.
moteur_regles() {
    title "Moteur : regles"
    local total absence
    total=$(find "${RULES}" -name '*.java' -not -name 'package-info.java' | wc -l | tr -d ' ')
    printf 'fichiers de regles : %s\n' "$total"

    title "Moteur : regles raisonnant sur une absence"
    local -i absence=0
    local file count
    while IFS= read -r file; do
        count=$(grep -c 'isEmpty()\|noneMatch' "$file" || true)
        if ((count > 0)); then
            absence+=1
            printf '%4d occurrence(s)  %s\n' "$count" "$(basename "$file")"
        fi
    done < <(find "${RULES}" -name '*.java' | sort)
    printf '\n%d fichiers sur %s concluent au moins une fois d une absence\n' "$absence" "$total"
}

# La boucle exterieure repart d une base de faits vide a chaque tour : le moteur
# n est pas monotone, contrairement a ce que le doc 07 §4.1 annonce.
moteur_boucle() {
    title "Moteur : forme de la boucle"
    grep -n 'new FactBase()\|MAX_ROUNDS' \
        "${NEXT}/hexaglue-engine/src/main/java/io/hexaglue/engine/Saturation.java" \
        "${NEXT}/hexaglue-engine/src/main/java/io/hexaglue/engine/Classifier.java"
}

# Chaque palier d evidence est traduit en une confiance. Si plusieurs paliers
# tombent sur la meme valeur, l ordre des paliers ne survit pas a la traduction.
echelle_paliers() {
    title "Echelle : palier d'evidence vers confiance"
    grep -E '^\s+[A-Z_]+\("S[0-9]", Confidence\.[A-Z]+\)' \
        "${MODEL}/classification/EvidenceTier.java" |
        sed 's/^[[:space:]]*/  /;s/,$//'

    title "Echelle : seuil de generation par defaut"
    grep -n 'Confidence\.' "${MODEL}/config/GenerationConfig.java"

    title "Echelle : ce que le seuil compare"
    grep -n 'isAtLeast' "${NEXT}/hexaglue-spi/src/main/java/io/hexaglue/spi/Contribution.java"
}

# Le doc 07 §7 annonce jqwik pour les proprietes « determinisme, monotonie,
# idempotence du point fixe » : exactement les proprietes en cause.
outillage_proprietes() {
    title "Outillage : jqwik declare ?"
    if grep -rq 'jqwik' "${NEXT}" --include='pom.xml'; then
        grep -rn 'jqwik' "${NEXT}" --include='pom.xml'
    else
        printf 'absent de tous les pom.xml du reacteur\n'
    fi

    title "Outillage : tests de propriete sur le point fixe"
    if grep -rlq 'fixpoint\|fixed point\|idempot' "${NEXT}" --include='*Test.java'; then
        grep -rln 'fixpoint\|fixed point\|idempot' "${NEXT}" --include='*Test.java'
    else
        printf 'aucun test nommant le point fixe\n'
    fi
}

# Un banc mesure ce que le reacteur fait d un domaine que personne n a ecrit
# pour lui. Encore faut il que le projet observe soit tiers.
bancs_reels() {
    title "Bancs : projets observes"
    local banc
    while IFS= read -r banc; do
        printf '%-24s -> %s\n' "$(basename "$(dirname "$banc")")" \
            "$(grep -o '<sourceDirectory>.*</sourceDirectory>' "$banc" |
                sed 's|<[^>]*>||g;s|.*/\.\./||')"
    done < <(find "${ROOT}/_probes" -name 'pom.xml' | sort)

    title "Bancs : projets reels disponibles dans le depot"
    find "${ROOT}" -maxdepth 1 -type d \
        \( -name 'case-study-*' -o -name 'spring-*' \) -exec basename {} \; | sort
}

main() {
    require jq
    local section="${1:-all}"
    case "$section" in
    corpus) corpus_taille; corpus_silence; corpus_provenance; corpus_confiance ;;
    moteur) moteur_regles; moteur_boucle ;;
    echelle) echelle_paliers ;;
    outillage) outillage_proprietes ;;
    bancs) bancs_reels ;;
    all)
        corpus_taille
        corpus_silence
        corpus_provenance
        corpus_confiance
        moteur_regles
        moteur_boucle
        echelle_paliers
        outillage_proprietes
        bancs_reels
        ;;
    *)
        printf 'section inconnue : %s\n' "$section" >&2
        exit 1
        ;;
    esac
}

main "$@"
