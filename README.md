# ME-90 Local Controller

一个简洁的 Android 效果器编辑器，使用设备连接进行控制。

## 当前功能

- 优先枚举标准 USB MIDI Streaming OUT endpoint；兼容暴露为 vendor-specific bulk/interrupt OUT 的设备，并请求系统 USB 授权。
- 深色蓝绿色效果链界面，涵盖 COMP、OD/DS、PREAMP、CAB/IR、MOD、EQ/FX2、DELAY、REVERB、PEDAL、NS、SEND/RETURN 与 AIRD OUTPUT。
- `CAB/IR` 位于效果链中，写入当前音色的 PREAMP `SP TYPE`：ORIGINAL、IR-1、IR-2、IR-3。
- 常用连续参数、开关、TYPE 选择、预置切换和用户预置写入；调音器基准音高位于顶部设置菜单。
- Pedal FX 包含 WAH、VOICE、±1/±2 OCT、FREEZE、OSC DELAY、OD/DS、MOD RATE 和 DELAY LEVEL；Preamp 包含主 TYPE 与 SELECTABLE 子类型。
- 按 Roland checksum 规则构造 ME-90 单参数和多字节 DT1 写入，以及单参数 RQ1 请求。
- 将 SysEx 转换成 USB-MIDI bulk-transfer packet，并以 1 秒超时发送。
- 用四个 7-bit 地址字节和一个 7-bit 值进行手动诊断。

## 构建

在安装了 Android SDK（API 35）和 JDK 17 的环境中：

```powershell
cd D:\Project\Reverse\me90-local-controller
gradle :app:assembleDebug
```

首次构建会下载 Android Gradle Plugin。安装生成的 `app-debug.apk` 后，用 OTG/USB 将你的 ME-90 连接至手机，选择对应的 VID/PID 设备并在系统弹窗中授权。USB 授权由 Android 系统控制；若选择“拒绝”，断开并重新连接设备后再试。

## 协议范围

已实现的帧格式：

```text
RQ1: F0 41 10 01 05 03 11 AA BB CC DD 00 00 00 01 CS F7
DT1: F0 41 10 01 05 03 12 AA BB CC DD VV CS F7
```

`CS = (128 - (sum(bytes & 0x7f) mod 128)) mod 128`。

当前版本不会显示设备返回的参数状态；编辑控件显示的是当前的本地设置值。
