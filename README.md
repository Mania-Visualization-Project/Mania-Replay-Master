# Mania Replay Render

Mania Replay Render是一个下落式音游（Mania）回放的可视化工具。它可在谱面中加上打击位置，并标注音符判定情况，最终渲染成视频文件输出。项目通过可视化某一游玩记录的判定细节，以此达到提升观赏效果、学习糊法、分析玩家的综合实力、判断是否为科技等目的。

Mania Replay Render is a visualization tool for falling music games (Mania) replay. It can attach hit positions to the map, mark judgement of the note, and render an output video. By visualizing the judgment details of a replay record, the project aims to improve the viewing effect, learn the play skills, analyze the comprehensive strength of a player, and judge if a player is trick.

目前项目仅支持Osu!Mania游戏的可视化。

Currently the project only supports Osu!Mania replay visualization.

## 截图 Screenshot

Demo: https://www.bilibili.com/video/av84322571/

- Lynessa's Jack Collection [Uetso Shi - Firmament Castle Velier]

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image3.png?raw=true)

- YELL! [Colorful Smile] + DT

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image1.png?raw=true)

- Mario Paint (Time Regression Mix for BMS) [D-ANOTHER]

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image2.png?raw=true)


## 使用方法 Usage

待填 TODO


## 编译 Compiling

### 安装依赖 Setting up the dependency

本项目的依赖工具如下：

The project depends on the following tools:

- Java 1.8
- Kotlin 1.3.11
- C++ 11
- OpenCV 3.4.1
- CMake 3.15.3
- FFmpeg (optional)

### 编译方法 Compiling method

推荐使用IntelliJ IDEA和CLion进行编译。此处以Windows 10系统为例说明，其余系统参照下面方法进行编译。

IntelliJ IDEA and CLion are recommended for compilation. Here the compiling platform is Windows 10, and the other systems can be compiled refer to follows.

- 进入src/cpp目录，使用CMake将JNI编译为librender.dll。可能需要修改OpenCV和JDK依赖目录。

- 将编译后的库文件librender.dll复制到lib/

- 编译Kotlin代码为ManiaReplayMaster.jar

- Go to src/cpp, use CMake to compile JNI codes to librender.dll. You may need to modify the OpenCV and JDK dependency directories.

- Copy the library file librender.dll to lib/

- Compile the Kotlin codes as ManiaReplayMaster.jar

使用方法 Usage:

```bash
java ManiaReplayMaster.jar [osu beatmap file.osu] [replay file.osr]
```

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