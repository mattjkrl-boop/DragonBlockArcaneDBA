# Prepare-Blockbench.ps1
# Put this file inside your GoodAnimations folder and run it

$goodAnimsFolder = $PSScriptRoot
$parentFolder    = Split-Path $goodAnimsFolder -Parent
$outputFolder    = Join-Path $goodAnimsFolder "BlockbenchImport"

# Reset output folder
if (Test-Path $outputFolder) {
    Remove-Item $outputFolder -Recurse -Force
}
New-Item -ItemType Directory -Path $outputFolder | Out-Null
New-Item -ItemType Directory -Path (Join-Path $outputFolder "model") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $outputFolder "animations") | Out-Null

Write-Host "=== Preparing already-rigged Meshy files ===" -ForegroundColor Cyan

# 1. Extract the base model
$modelZip = Get-ChildItem -Path $parentFolder -Filter "namekian#0000.zip" -ErrorAction SilentlyContinue
if ($modelZip) {
    Write-Host "Extracting base model..." -ForegroundColor Green
    Expand-Archive -Path $modelZip.FullName -DestinationPath (Join-Path $outputFolder "model") -Force
} else {
    Write-Host "WARNING: namekian#0000.zip not found in parent folder!" -ForegroundColor Yellow
}

# 2. Extract all animations
$animZips = Get-ChildItem -Path $goodAnimsFolder -Filter "*.zip"
foreach ($zip in $animZips) {
    $animName = [System.IO.Path]::GetFileNameWithoutExtension($zip.Name)
    $dest = Join-Path $outputFolder "animations\$animName"
    New-Item -ItemType Directory -Path $dest -Force | Out-Null
    Expand-Archive -Path $zip.FullName -DestinationPath $dest -Force
    Write-Host "Extracted → $animName" -ForegroundColor Green
}

Write-Host "`nAll files ready in:" -ForegroundColor Yellow
Write-Host $outputFolder

# Open the folder
Start-Process explorer.exe $outputFolder

# Try to launch Blockbench
$possiblePaths = @(
    "$env:LOCALAPPDATA\Programs\Blockbench\Blockbench.exe",
    "$env:LOCALAPPDATA\Blockbench\Blockbench.exe",
    "C:\Program Files\Blockbench\Blockbench.exe",
    "C:\Program Files (x86)\Blockbench\Blockbench.exe"
)

$bbPath = $possiblePaths | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($bbPath) {
    Write-Host "Launching Blockbench..." -ForegroundColor Cyan
    Start-Process $bbPath
} else {
    Write-Host "Blockbench not found automatically. Open it manually." -ForegroundColor Yellow
}

Write-Host "`n=== Fast Blockbench Steps (already rigged) ===" -ForegroundColor Magenta
Write-Host "1. File → New → Generic Model"
Write-Host "2. File → Import → Import the model FBX/GLB from the 'model' folder"
Write-Host "3. Switch to Animate tab"
Write-Host "4. For each animation folder: Animation → Import Animations → select the FBX/GLB"
Write-Host "5. File → Export → Export GeckoLib Model"
Write-Host "6. File → Export → Export Animations"
Write-Host "================================================"