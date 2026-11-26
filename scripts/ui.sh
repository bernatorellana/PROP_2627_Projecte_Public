#!/usr/bin/env bash
# Interfície gràfica: jugar contra el vostre jugador o veure'l jugar.
#
# Com fer-lo servir: editeu els valors de la secció CONFIGURACIÓ i executeu l'script
# (scripts/ui.sh), sense arguments. Les línies comentades són altres valors possibles.

# ==================== CONFIGURACIÓ ====================
# Jugador 1 (blanques, comença)
PLAYER1="human"
#PLAYER1="profe1"
#PLAYER1="random"
#PLAYER1="edu.upc.epsevg.prop.bombardment.player.MyPlayer"
#PLAYER1="v2=edu.upc.epsevg.prop.bombardment.player.MyPlayer(2)"

# Jugador 2 (negres)
PLAYER2="edu.upc.epsevg.prop.bombardment.player.MyPlayer"
#PLAYER2="profe1"
#PLAYER2="human"
#PLAYER2="random"

# Mida del tauler (competició: 8)
SIZE="8"
#SIZE="6"
#SIZE="10"

# Temps per jugada en ms (competició: 200)
TIME_MS="200"
#TIME_MS="500"
#TIME_MS="1000"

# 1 = comença la partida en obrir la finestra
AUTOSTART="0"
#AUTOSTART="1"

# 1 = recompila abans d'executar (agafa els canvis del vostre jugador); 0 = no
BUILD=1
# ======================================================

set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$BUILD" = 1 ]; then mvn -q package -DskipTests; fi
exec java -cp "bombardment-ui/target/classes:bombardment-ui/target/lib/*" edu.upc.epsevg.prop.bombardment.Game "$PLAYER1" "$PLAYER2" "$SIZE" "$TIME_MS" $([ "$AUTOSTART" = 1 ] && echo start)
