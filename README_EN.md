# Mania Replay Render

[中文版](README.md)

Mania Replay Render is a visualization tool for falling music games (Mania) replay. It can attach hit positions to the map, mark judgement of the note, and render an output video. By visualizing the judgment details of a replay record, the project aims to improve the viewing effect, learn the play skills, analyze the comprehensive strength of a player, and judge if a player is trick.

Currently the project only supports Osu!Mania replay visualization.

## Screenshot

Demo: https://www.bilibili.com/video/av84322571/

- Lynessa's Jack Collection [Uetso Shi - Firmament Castle Velier]

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image3.png?raw=true)

- YELL! [Colorful Smile] + DT

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image1.png?raw=true)

- Mario Paint (Time Regression Mix for BMS) [D-ANOTHER]

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image2.png?raw=true)


## Usage

(Only in Windows)

First click [here](https://github.com/Keytoyze/Mania-Replay-Master/releases/download/v1.1/ManiaReplayMaster.v1.1.zip) to download the tool kit.

Method one: Double click `ManiaReplayMaster.bat` batch script, enter beatmap path, replay path and falling down speed (15 pixels per frame by default) according to the hint.

Method two: Use the following commands to render the video.

```bash
cd library
java -jar ManiaReplayMaster [-speed=15] beatmap.osu replay.osr
```

## Compiling

### Setting up the dependency

The project depends on the following tools:

- Java 1.8
- Kotlin 1.3.11
- C++ 11
- OpenCV 3.4.1
- CMake 3.15.3
- FFmpeg (optional)

### Compiling method

IntelliJ IDEA and CLion are recommended for compiling. Here the compiling platform is Windows 10, and the other systems can be compiled refer to follows.

- Go to src/cpp, use CMake to compile JNI codes to librender.dll. You may need to modify the OpenCV and JDK dependency directories.

- Copy the library file librender.dll to lib/

- Compile the Kotlin codes as ManiaReplayMaster.jar

## Current issues

- The program currently runs well only on JDK 8 platform. On JDK 10 and JDK 13, it will crash when loading DLL: "A dynamic link library (DLL) initialization routine failed". See issue [#1](https://github.com/Keytoyze/Mania-Replay-Master/issues/1)。

- In Osu!Mania, a hit ahead of time will lead to miss. But I cannot find the exact time threshold value. See issue [#2](https://github.com/Keytoyze/Mania-Replay-Master/issues/2)

If someone knowns how to solve these two issues, please reply in the relevant issues. Thx!

## LICENSE

```
Copyright (c) 2020-present, project contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```