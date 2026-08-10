$source = "C:\Users\carte\Documents\Code\MC Mods\DragonBlockArcaneDBA\Animated"
$destination = Join-Path $source "GoodAnimations"

if (!(Test-Path $destination)) {
    New-Item -ItemType Directory -Path $destination | Out-Null
}

$keep = @{
    # Movement
    "namekian#0000_idle.zip"                  = "idle.zip"
    "namekian#0000_walk.zip"                  = "walk.zip"
    "namekian#0000_walk_back.zip"             = "walk_back.zip"
    "namekian#0000_run.zip"                   = "run.zip"
    "namekian#0000_crouch.zip"                = "sneak_idle.zip"
    "namekian#0000_crouch_walk.zip"           = "sneak_walk.zip"
    "namekian#0000_crawl.zip"                 = "crawl.zip"
    "namekian#0000_jump.zip"                  = "jump.zip"
    "namekian#0000_jump_start.zip"            = "jump_start.zip"
    "namekian#0000_jump_fall.zip"             = "fall.zip"
    "namekian#0000_jump_end.zip"              = "land.zip"
    "namekian#0000_running_jump.zip"          = "running_jump.zip"
    "namekian#0000_lie_down.zip"              = "sleep.zip"
    "namekian#0000_lying_down_idle.zip"       = "sleep_idle.zip"
    "namekian#0000_die.zip"                   = "death.zip"
    "namekian#0000_eat.zip"                   = "eat.zip"

    # Items / Actions
    "namekian#0000_punch_right.zip"           = "punch.zip"
    "namekian#0000_punch_left.zip"            = "attack.zip"
    "namekian#0000_axe_attack.zip"            = "use_tool.zip"
    "namekian#0000_arm_parry.zip"             = "block.zip"
    "namekian#0000_sword_idle.zip"            = "item_idle.zip"
}

Write-Host "Copying only the animations you want..." -ForegroundColor Cyan

foreach ($file in Get-ChildItem -Path $source -Filter "*.zip") {
    if ($keep.ContainsKey($file.Name)) {
        $newName = $keep[$file.Name]
        Copy-Item -Path $file.FullName -Destination (Join-Path $destination $newName) -Force
        Write-Host "Kept → $newName" -ForegroundColor Green
    }
}

# Also make use_item from the eat animation
$eatSource = Join-Path $destination "eat.zip"
if (Test-Path $eatSource) {
    Copy-Item -Path $eatSource -Destination (Join-Path $destination "use_item.zip") -Force
    Write-Host "Kept → use_item.zip (copied from eat)" -ForegroundColor Green
}

Write-Host "`nDone! Clean animations are in:" -ForegroundColor Yellow
Write-Host $destination