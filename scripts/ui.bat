@echo off
setlocal
rem Interfície gràfica: jugar contra el vostre jugador o veure'l jugar.
rem
rem Com fer-lo servir: editeu els valors de la seccio CONFIGURACIO i executeu l'script
rem (scripts\ui.bat), sense arguments. Les linies comentades son altres valors possibles.

rem ==================== CONFIGURACIO ====================
rem Jugador 1 (blanques, comença)
set "PLAYER1=human"
rem set "PLAYER1=profe1"
rem set "PLAYER1=random"
rem set "PLAYER1=edu.upc.epsevg.prop.bombardment.player.MyPlayer"
rem set "PLAYER1=v2=edu.upc.epsevg.prop.bombardment.player.MyPlayer(2)"

rem Jugador 2 (negres)
set "PLAYER2=edu.upc.epsevg.prop.bombardment.player.MyPlayer"
rem set "PLAYER2=profe1"
rem set "PLAYER2=human"
rem set "PLAYER2=random"

rem Mida del tauler (competició: 8)
set "SIZE=8"
rem set "SIZE=6"
rem set "SIZE=10"

rem Temps per jugada en ms (competició: 200)
set "TIME_MS=200"
rem set "TIME_MS=500"
rem set "TIME_MS=1000"

rem 1 = comença la partida en obrir la finestra
set "AUTOSTART=0"
rem set "AUTOSTART=1"

rem 1 = recompila abans d'executar (agafa els canvis del vostre jugador); 0 = no
set "BUILD=1"
rem ======================================================

cd /d "%~dp0.."
if "%BUILD%"=="1" ( call mvn -q package -DskipTests || exit /b 1 )
set "START="
if "%AUTOSTART%"=="1" set "START=start"
java -cp "bombardment-ui\target\classes;bombardment-ui\target\lib\*" edu.upc.epsevg.prop.bombardment.Game "%PLAYER1%" "%PLAYER2%" %SIZE% %TIME_MS% %START%
endlocal
