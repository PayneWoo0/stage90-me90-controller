# Stage 90

[⬇ 下载最新 APK / Download the latest APK](https://github.com/PayneWoo0/stage90-me90-controller/releases/tag/v1.0.2)

让 Android 手机通过一根 OTG 线直接控制 BOSS ME-90。无需额外购买蓝牙适配器，也不必携带电脑；连接手机即可编辑音色、调整效果，并用于现场演出。

Control BOSS ME-90 directly from an Android phone over a single OTG cable. No extra Bluetooth adapter or computer is needed—connect your phone to edit patches, shape effects, and use it on stage.

## 主要功能 / Features

### 有线直连控制 / Direct wired control

使用手机的 USB-C/OTG 接口直接连接 ME-90，享受稳定、低延迟的参数控制体验。适合不想额外购买无线模块，或希望把编辑器随身带在手机上的吉他手。

Connect the ME-90 directly through your phone's USB-C/OTG port for stable, responsive parameter control. Ideal for guitarists who do not want to buy an additional wireless module or carry a computer.

### 完整效果链编辑 / Full effect-chain editing

在一条清晰的效果链中管理 COMP/FX1、OD/DS、AMP、CAB/IR、MOD、EQ/FX2、DLY、REV、PEDAL、NS、S/R 和 AIRD OUTPUT。可调整开关、效果类型、参数、箱体 IR、输出目标和 Send/Return 前后位置。

Manage COMP/FX1, OD/DS, AMP, CAB/IR, MOD, EQ/FX2, DLY, REV, PEDAL, NS, S/R, and AIRD OUTPUT from one clear effect chain. Adjust switches, effect types, parameters, cabinet IRs, output targets, and Send/Return position.

### 音色管理与写入 / Patch management and writing

双列浏览 USER 与 PRESET 音色，快速选择 9 组 × 4 槽预设。可为当前音色命名并写入 USER 槽位，方便整理自己的演出音色。

Browse USER and PRESET patches side by side, then quickly select any of the 9 banks × 4 slots. Name the current sound and write it to a USER slot to organize your own stage-ready patches.

### LIVE 演出模式 / LIVE performance mode

横屏大转盘交互专为演出设计：使用两个大旋钮选择组与槽位，并在 USER 和 PRESET 之间切换。进入 LIVE 后只保留音色切换所需的控制，让操作更专注。

Landscape LIVE mode is built for performance: two large dials select bank and slot, with a switch between USER and PRESET. It keeps only the controls needed for patch changes, so operation stays focused on stage.

### 调音器与本地化 / Tuner and localization

内置调音器可显示音名、音高偏低/偏高方向和指针；支持调音器静音/直通与基准音高设置。界面提供中文和 English 两种语言。

The built-in tuner shows note name, flat/sharp direction, and a visual needle. It supports mute/thru behavior and reference-pitch adjustment. The interface is available in Chinese and English.

## 使用要求 / Requirements

- Android 8.0 或更高版本，并支持 USB OTG。\
  Android 8.0 or later with USB OTG support.
- BOSS ME-90 与可用的数据线。\
  A BOSS ME-90 and a data-capable USB cable.
- 安装后连接设备，并在 Android 系统提示时授予 USB 访问权限。\
  After installation, connect the device and grant USB access when Android asks.

> 写入 USER 音色会覆盖目标槽位，请先备份重要音色。
>
> Writing a USER patch overwrites the selected slot; back up important patches first.

## 工作原理 / How it works

Stage 90 让手机作为 USB Host，通过 USB-MIDI 向 ME-90 发送和接收 Roland SysEx 控制消息。应用会在连接后读取设备状态，并在你切换音色或调整参数时把对应控制信息发送到效果器。

Stage 90 makes the phone act as a USB host and exchanges Roland SysEx control messages with the ME-90 over USB-MIDI. It reads device state after connection and sends the corresponding control data whenever you change a patch or parameter.

底层使用 Roland 的 RQ1 读取与 DT1 写入消息，采用四个 7-bit 地址字节和 Roland 校验和。该实现仅用于本地控制已连接的设备。

Under the hood, it uses Roland RQ1 read and DT1 write messages with four 7-bit address bytes and the Roland checksum. The implementation is used only for local control of the connected device.

## 构建 / Build

在安装 Android SDK（API 35）和 JDK 17 的环境中：

With Android SDK (API 35) and JDK 17 installed:

```powershell
cd D:\Project\Reverse\me90-local-controller
gradle :app:assembleDebug
```

本仓库也提供可离线使用的工具链：

The repository also includes an offline toolchain:

```powershell
$env:JAVA_HOME=(Resolve-Path '.\tools\jdk-17.0.10+7').Path
$env:ANDROID_HOME=(Resolve-Path '.\tools\android-sdk').Path
.\tools\gradle-8.10.2\bin\gradle.bat --offline --no-daemon --console=plain :app:packageDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/Stage90-v1.0.2.apk`。

The generated APK is located at `app/build/outputs/apk/debug/Stage90-v1.0.2.apk`.

## 许可证 / License

本项目代码计划采用 MIT License 发布。BOSS 和 ME-90 是其各自权利人的商标；本项目与其不存在官方关联或背书关系。

This project's code is intended to be released under the MIT License. BOSS and ME-90 are trademarks of their respective owners; this project is not affiliated with or endorsed by them.
