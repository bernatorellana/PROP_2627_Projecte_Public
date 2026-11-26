@echo off
setlocal
rem Banc de proves (c): moltes partides entre dos jugadors, configurades en un fitxer .properties.
rem Resultats a results/ (resum HTML + CSV). Per fer-ne un de vostre, copieu challenges/exemple.properties i editeu-lo.
rem
rem Com fer-lo servir: editeu els valors de la seccio CONFIGURACIO i executeu l'script
rem (scripts\challenge.bat), sense arguments. Les linies comentades son altres valors possibles.

rem ==================== CONFIGURACIO ====================
rem Fitxer de configuració del challenge
set "CONFIG=challenges/exemple.properties"
rem set "CONFIG=challenges/benchmark-1-profe1.properties"    (benchmark oficial 1: 200 partides, prop d'1 h)
rem set "CONFIG=challenges/la-meva-prova.properties"    (una còpia vostra)

rem 1 = recompila abans d'executar (agafa els canvis del vostre jugador); 0 = no
set "BUILD=1"
rem ======================================================

cd /d "%~dp0.."
if "%BUILD%"=="1" ( call mvn -q package -DskipTests || exit /b 1 )
java -cp "bombardment-bench\target\classes;bombardment-bench\target\lib\*" edu.upc.epsevg.prop.bombardment.bench.Challenge "%CONFIG%"
endlocal
