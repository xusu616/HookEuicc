# HookEuicc
解除某些软件对eSIM的要求，也可用于获取eSIM代码

在某些不直接提供eSIM激活码的应用，将会复制激活码到剪切板

## giffgaff 与物理 eUICC 卡

1.0.2 添加 giffgaff 专用 Hook 需要支持 libxposed API 102 的框架，并在模块作用域中勾选 `com.giffgaffmobile.controller`，然后强行停止并重新打开 giffgaff

giffgaff 21.0.1 的 `SimCardsManagerModule.isEsimSupported(Promise)` 会检查 eUICC 系统特性、`EuiccManager`、`isEnabled()` 和 EID 物理 eUICC 卡由独立 LPA 管理，不会给手机创建 Android eUICC 控制器 旧模块只修改系统特性和 `isEnabled()`，仍会被其余检查拦截 新版直接在 giffgaff 原生桥接入口返回支持，不伪造 EID

**已在 OnePlus 7 Pro（GM1911）、Android 17、Vector 2.2 / API 102、giffgaff 21.0.1 上验证：首页由 `This phone doesn't support eSIM` 变为 `Get an eSIM`，并显示 `Choose your plan`**

购买后，当 giffgaff 调用 `setupEsim()` 时，模块会读取其 `confirmationCode` 参数，复制并弹出激活码分享窗口 请保存激活码，使用 EasyEUICC 等与你的小白卡兼容的 LPA 手动安装 若 LPA 要求完整的标准激活码，可在 `1$…` 前加上 `LPA:`，得到 `LPA:1$…`

此 giffgaff Hook 使用手动 LPA 安装流程 导出激活码后，模块向 App 报告需要手动安装；giffgaff 21.0.1 会把该结果显示为通用安装错误 这个提示不代表激活码已被安装，也不妨碍你把已经导出的激活码交给小白卡 LPA 真实购买、激活码导出和联网尚未实机验证

### 回归检查

打开 giffgaff 首页的 eSIM 标签后，在 Windows 上运行：

```powershell
pwsh -File .\tools\verify-giffgaff-esim.ps1 -AdbPath "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```

该检查读取当前界面，确认不支持提示消失且出现 eSIM 套餐入口 连接多台设备时用 `-Serial` 指定手机；也可以用 `-UiDumpPath` 检查保存的 UI XML

## 其他功能

### OMAPI Bypass
绕过 ARA、ARF 限制 通常用于无ARA的卡进行OMAPI访问

> [!NOTE]
> 需勾选 `com.android.se` 并重启手机 (或执行 `su -c killall com.android.se`)

## 捐赠
如果您喜欢这个项目，可以向以下地址捐赠：

USDT (ERC20): `0x4d62f0c2a5bd9358bcd58352b0a3efd60afcf180`

USDT (TRC20): `TDJvmrzpS496wjyx7rTR4bediAvWvx1XLf`
