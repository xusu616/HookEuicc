param(
    [string]$AdbPath = 'adb',
    [string]$Serial,
    [string]$UiDumpPath
)

$ErrorActionPreference = 'Stop'

if ($UiDumpPath) {
    [xml]$window = Get-Content -LiteralPath $UiDumpPath -Raw
} else {
    $deviceArguments = @()
    if ($Serial) {
        $deviceArguments = @('-s', $Serial)
    }

    & $AdbPath @deviceArguments shell uiautomator dump /sdcard/HookEuicc-verification.xml | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw '无法读取手机界面，请检查 ADB 连接并打开 giffgaff 首页的 eSIM 标签。'
    }

    $xmlLines = & $AdbPath @deviceArguments shell cat /sdcard/HookEuicc-verification.xml
    if ($LASTEXITCODE -ne 0) {
        throw '无法读取 uiautomator 生成的界面 XML。'
    }
    [xml]$window = $xmlLines -join "`n"
}

$appNodes = $window.SelectNodes("//node[@package='com.giffgaffmobile.controller']")
if ($appNodes.Count -eq 0) {
    throw '当前界面不是 giffgaff。请打开首页的 eSIM 标签再检查。'
}

$texts = @($appNodes | ForEach-Object { $_.GetAttribute('text') })
if ($texts -contains "This phone doesn't support eSIM" -or $texts -contains "The phone doesn't support eSIM") {
    throw '回归检查失败：giffgaff 仍然显示手机不支持 eSIM。'
}
if ($texts -notcontains 'Get an eSIM' -or $texts -notcontains 'Choose your plan') {
    throw '未找到 Get an eSIM 和 Choose your plan。请打开未激活账号的首页 eSIM 标签再检查。'
}

Write-Output 'PASS: giffgaff 首页显示 Get an eSIM 和 Choose your plan，不支持提示已消失。'
