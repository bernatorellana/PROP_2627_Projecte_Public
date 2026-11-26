#!/usr/bin/env bash
# Banc de proves (c): moltes partides entre dos jugadors, configurades en un fitxer .properties.
# Resultats a results/ (resum HTML + CSV). Per fer-ne un de vostre, copieu challenges/exemple.properties i editeu-lo.
#
# Com fer-lo servir: editeu els valors de la secció CONFIGURACIÓ i executeu l'script
# (scripts/challenge.sh), sense arguments. Les línies comentades són altres valors possibles.

# ==================== CONFIGURACIÓ ====================
# Fitxer de configuració del challenge
CONFIG="challenges/exemple.properties"
#CONFIG="challenges/benchmark-1-profe1.properties"    # (benchmark oficial 1: 200 partides, prop d'1 h)
#CONFIG="challenges/la-meva-prova.properties"    # (una còpia vostra)

# 1 = recompila abans d'executar (agafa els canvis del vostre jugador); 0 = no
BUILD=1
# ======================================================

set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$BUILD" = 1 ]; then mvn -q package -DskipTests; fi
exec java -cp "bombardment-bench/target/classes:bombardment-bench/target/lib/*" edu.upc.epsevg.prop.bombardment.bench.Challenge "$CONFIG"
