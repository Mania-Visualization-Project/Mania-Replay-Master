@echo off

set JAVAPATH=java
%JAVAPATH% -version
echo.

echo Osu!Mania beatmap path [*.osu]:
set /p beatmap=
echo Replay path [*.osr]:
set /p replay=
echo Enter falling down speed, in pixels per frame (default = 15)
set /p speed=

cd library
if "%speed%"=="" (
    %JAVAPATH% -jar ManiaReplayMaster.jar "%beatmap%" "%replay%"
) else (
    %JAVAPATH% -jar ManiaReplayMaster.jar -speed=%speed% "%beatmap%" "%replay%"
)

echo.
@pause


