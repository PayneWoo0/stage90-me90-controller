# Stage 90

Stage 90 是一款离线 Android 控制器，用于通过 OTG/USB 管理 BOSS ME-90 的音色与效果参数。它只与已授权的本地设备通信，不依赖账户、云端或网络服务。

## 功能

### 连接与同步

- 通过系统 USB 授权连接 ME-90，并在连接后自动读取当前设备状态。
- 支持设备面板、脚钉或 App 发起的音色切换后的状态同步；Manual 模式仍保留当前音色名称与预置位置。
- 连接状态、音色名称、模块开关、效果类型和参数值均在主界面即时呈现。

### 音色与效果控制

- `USER` 与 `PRESET` 采用双列音色选择器；可切换 9 组 × 4 槽音色，并将名称写入 USER 槽位。
- 效果链覆盖 COMP/FX1、OD/DS、AMP、CAB/IR、MOD、EQ/FX2、DLY、REV、PEDAL、NS、S/R 与 AIRD OUTPUT。
- 支持模块启用/关闭、TYPE/SELECTABLE 类型、连续参数、箱体 IR、AIRD 输出目标以及 Send/Return 前后位置。
- 效果链保留设备对应的固定位置；模块状态以颜色和选中轮廓清晰区分。

### 演出与调音

- LIVE 模式复用与主页完全相同的音色选择与同步逻辑，以大转盘快速选择 USER 或 PRESET 的组与槽位。
- 调音器显示音名、偏高/偏低方向和指针；无输入时保持中性状态，不会误显示“音准准确”。
- 提供中文/English 界面及调音器静音/直通、基准音高设置。

## 原理

手机作为 USB Host 连接 ME-90，应用取得系统 USB 授权后选择 MIDI OUT/IN 接口，并把 Roland SysEx 封装为 USB-MIDI 四字节数据包传输。设备身份确认后，应用依次切换通信级别、进入编辑状态并读取当前状态；设备或 App 的音色变更会触发后续完整同步。

控制协议使用 Roland 的 RQ1（读取）与 DT1（写入）消息，地址由四个 7-bit 字节组成，数据采用 7-bit 编码。校验和计算如下：

```text
CS = (128 - (sum(bytes & 0x7f) mod 128)) mod 128
```

示例帧：

```text
RQ1: F0 41 10 01 05 03 11 AA BB CC DD SS SS SS SS CS F7
DT1: F0 41 10 01 05 03 12 AA BB CC DD VV... CS F7
```

## 构建

在安装 Android SDK（API 35）和 JDK 17 的环境中：

```powershell
cd D:\Project\Reverse\me90-local-controller
gradle :app:assembleDebug
```

本仓库也提供可离线使用的工具链：

```powershell
$env:JAVA_HOME=(Resolve-Path '.\tools\jdk-17.0.10+7').Path
$env:ANDROID_HOME=(Resolve-Path '.\tools\android-sdk').Path
.\tools\gradle-8.10.2\bin\gradle.bat --offline --no-daemon --console=plain :app:packageDebug
```

生成文件位于 `app/build/outputs/apk/debug/Stage90-v1.0.2.apk`。安装后使用 OTG/USB 连接 ME-90，在系统提示中授予设备访问权限。写入 USER 音色会覆盖目标槽位，请先备份重要音色。
