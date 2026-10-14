<#
  打源码包：把小程序源码打成 zip（排除 .git / dist / preview 等）

  用法：
    powershell -File tools\package.ps1
    powershell -File tools\package.ps1 -Version 1.0.1

  产物：dist\xinghuo-camo-miniprogram-v<版本>-<日期>.zip
#>
param(
  [string]$Version = '1.0.0'
)

$ErrorActionPreference = 'Stop'
$src = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$distDir = Join-Path $src 'dist'
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

$stamp = Get-Date -Format 'yyyyMMdd'
$zipPath = Join-Path $distDir ("xinghuo-camo-miniprogram-v$Version-$stamp.zip")
if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }

# 排除项：git 历史、打包产物、预览二维码/截图、依赖
$exclude = '\\(\.git|dist|preview|node_modules|备案)(\\|$)'
$files = Get-ChildItem -LiteralPath $src -Recurse -File -Force |
  Where-Object { $_.FullName -notmatch $exclude }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($zipPath, 'Create')
try {
  foreach ($f in $files) {
    $rel = $f.FullName.Substring($src.Length + 1) -replace '\\', '/'
    [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $f.FullName, $rel)
  }
} finally {
  $zip.Dispose()
}

$sizeMb = [Math]::Round((Get-Item -LiteralPath $zipPath).Length / 1MB, 2)
Write-Output "已生成: $zipPath"
Write-Output "文件数: $($files.Count)  大小: $sizeMb MB"
Write-Output ''
Write-Output '包内目录：'
$files | ForEach-Object { $_.FullName.Substring($src.Length + 1) } |
  ForEach-Object { ($_ -split '\\')[0] } | Group-Object | Sort-Object Name |
  ForEach-Object { "  $($_.Name.TrimEnd('\'))\  ($($_.Count) 个文件)" }
