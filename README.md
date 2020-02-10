# Mania Replay Render

[English version](README_EN.md)

Mania Replay Render是一个下落式音游（Mania）回放的可视化工具。它可在谱面中加上打击位置，并标注音符判定情况，最终渲染成视频文件输出。项目通过可视化某一游玩记录的判定细节，以此达到提升观赏效果、学习糊法、分析玩家的综合实力、判断是否为科技等目的。

目前项目仅支持Osu!Mania游戏的可视化。

## 截图

演示: https://www.bilibili.com/video/av84322571/

- Lynessa's Jack Collection [Uetso Shi - Firmament Castle Velier]

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image3.png?raw=true)

- YELL! [Colorful Smile] + DT

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image1.png?raw=true)

- Mario Paint (Time Regression Mix for BMS) [D-ANOTHER]

![](https://github.com/Keytoyze/Mania-Replay-Master/blob/master/screenshot/image2.png?raw=true)


## 使用方法

（仅限Windows平台）

首先点击[此处](https://github.com/Keytoyze/Mania-Replay-Master/releases/download/v1.2/ManiaReplayMaster.v1.2.zip)下载工具包。

方法一（推荐）：使用游戏内渲染插件“MRM-extension.exe”，在结算界面点击F1键。

方法二：双击`ManiaReplayMaster.bat`批处理脚本，按照提示输入谱面路径、回放路径和下落速度（默认为15像素/帧）。

方法三：使用如下命令行参数进行操作。
```bash
cd library
java -jar ManiaReplayMaster [-speed=15] beatmap.osu replay.osr
```

## 编译

### 安装依赖

本项目的依赖工具如下：

- Java 1.8
- Kotlin 1.3.11
- C++ 11
- OpenCV 3.4.1
- CMake 3.15.3
- FFmpeg (可选)

### 编译方法

推荐使用IntelliJ IDEA和CLion进行编译。此处以Windows 10系统为例说明，其余系统参照下面方法进行编译。

- 进入src/cpp目录，使用CMake将JNI编译为librender.dll。可能需要修改OpenCV和JDK依赖目录。

- 将编译后的库文件librender.dll路径添加到java.library.path

- 编译Kotlin代码为ManiaReplayMaster.jar

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