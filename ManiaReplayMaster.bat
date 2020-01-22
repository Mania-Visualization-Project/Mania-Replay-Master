@echo off

java -version
echo.

echo Osu!Mania beatmap path [*.osu]:
set /p beatmap=
echo Replay path [*.osr]:
set /p replay=

cd library
java -jar ManiaReplayMaster.jar "%beatmap%" "%replay%"

echo.
@pause


