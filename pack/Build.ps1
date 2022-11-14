cd $PSScriptRoot
cd ..

$7zipPath = "$env:ProgramFiles\7-Zip\7z.exe"
$date = Get-Date -Format "yyyy-MM-dd"

if (-not (Test-Path -Path $7zipPath -PathType Leaf)) {
    throw "7 zip file '$7zipPath' not found"
}

Set-Alias 7zip $7zipPath
7zip a -bsp1 "-xr@pack\7z.exclude.txt" "pack\FallrimTools $date.7z" "@pack\7z.listfile.txt"
pause
