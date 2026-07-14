param(
    [string]$ApplicationId = "com.example.neurotrack.debug",
    [string]$Activity = "com.example.neurotrack.MainActivity"
)

$ErrorActionPreference = "Stop"

$adb = if ($env:ANDROID_HOME) {
    Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
} else {
    "C:\Users\chiye\AppData\Local\Android\Sdk\platform-tools\adb.exe"
}

if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb was not found: $adb"
}

function Get-UiHierarchy([string]$remotePath) {
    & $adb shell uiautomator dump $remotePath | Out-Null
    $rawXml = (& $adb exec-out cat $remotePath) -join ""
    return [xml]$rawXml
}

function Get-ChoiceCount($document, [string[]]$labels) {
    $count = 0
    foreach ($label in $labels) {
        $count += @($document.SelectNodes("//node[@text='$label']")).Count
    }
    return $count
}

& $adb shell input keyevent 224
& $adb shell wm dismiss-keyguard
& $adb shell am force-stop $ApplicationId
& $adb shell am start -W -n "$ApplicationId/$Activity" | Out-Null
Start-Sleep -Milliseconds 800

$displaySize = (& $adb shell wm size | Select-String -Pattern "(\d+)x(\d+)" | Select-Object -Last 1).Matches[0]
$width = [int]$displaySize.Groups[1].Value
$height = [int]$displaySize.Groups[2].Value

$root = Get-UiHierarchy "/sdcard/neurotrack-root.xml"
$settingsLabel = "$([char]0x8BBE)$([char]0x7F6E)"
$settingsNodes = @($root.SelectNodes("//node[@text='$settingsLabel']")) + @($root.SelectNodes("//node[@text='Settings']"))
if ($settingsNodes.Count -eq 0) {
    throw "The settings navigation item was not found."
}

$settingsBounds = $settingsNodes |
    ForEach-Object {
        $match = [regex]::Match($_.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
        [pscustomobject]@{
            Left = [int]$match.Groups[1].Value
            Top = [int]$match.Groups[2].Value
            Right = [int]$match.Groups[3].Value
            Bottom = [int]$match.Groups[4].Value
        }
    } |
    Sort-Object Bottom -Descending |
    Select-Object -First 1

$tapX = [int](($settingsBounds.Left + $settingsBounds.Right) / 2)
$tapY = [int](($settingsBounds.Top + $settingsBounds.Bottom) / 2)
& $adb shell input tap $tapX $tapY
Start-Sleep -Milliseconds 500

$top = Get-UiHierarchy "/sdcard/neurotrack-settings-top.xml"
$week = [char]0x5468
$weekdayCount = Get-ChoiceCount $top @(
    "$week$([char]0x4E00)",
    "$week$([char]0x4E8C)",
    "$week$([char]0x4E09)",
    "$week$([char]0x56DB)",
    "$week$([char]0x4E94)",
    "$week$([char]0x516D)",
    "$week$([char]0x65E5)",
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday"
)

$swipeX = [int]($width / 2)
$swipeFromY = [int]($height * 0.70)
$swipeToY = [int]($height * 0.28)
& $adb shell input swipe $swipeX $swipeFromY $swipeX $swipeToY 350
Start-Sleep -Milliseconds 500

$bottom = Get-UiHierarchy "/sdcard/neurotrack-settings-bottom.xml"
$languageCount = Get-ChoiceCount $bottom @(
    "$([char]0x4E2D)$([char]0x6587)",
    "English"
)
$themeCount = Get-ChoiceCount $bottom @(
    "$([char]0x8DDF)$([char]0x968F)$([char]0x7CFB)$([char]0x7EDF)",
    "$([char]0x6D45)$([char]0x8272)",
    "$([char]0x6DF1)$([char]0x8272)",
    "System",
    "Light",
    "Dark"
)

$actual = "refresh-day=$weekdayCount, language=$languageCount, appearance=$themeCount"
if ($weekdayCount -ne 1 -or $languageCount -ne 1 -or $themeCount -ne 1) {
    throw "Settings choices are still expanded ($actual); each row must expose only its current value."
}

Write-Output "PASS: settings home only exposes current values ($actual)."
