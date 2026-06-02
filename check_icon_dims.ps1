Add-Type -AssemblyName System.Drawing

$nodpi = "c:\Users\terje\dev\FieldApp\app\src\main\res\drawable-nodpi"

foreach ($name in @("icon_set1.png", "icon_set2.png", "icon_set3.png", "icon_set4.png")) {
    $path = Join-Path $nodpi $name
    if (Test-Path $path) {
        $img = [System.Drawing.Image]::FromFile((Resolve-Path $path))
        Write-Host "$name (drawable-nodpi): $($img.Width)x$($img.Height)"
        $img.Dispose()
    }
}
