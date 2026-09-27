Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 1920, 1080
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(10, 15, 26))

$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(6, 182, 212))
$font = New-Object System.Drawing.Font("Arial", 40, [System.Drawing.FontStyle]::Bold)
$g.DrawString("PASA SENTINEL CYBER DEFENSE", $font, $brush, 100, 100)

$bmp.Save("E:\Projects\PrivateApp\promo_video_builder\test.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "SUCCESS"
