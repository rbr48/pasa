# Assemble 36-Second High-Energy Kinetic Promo Video for Facebook & YouTube
# Master 1080p 30fps MP4 with 130 BPM Cyberpunk Soundtrack

$OutDir = "E:\Projects\PrivateApp\promo_video_builder\output"
$FinalVideo = "E:\Projects\PrivateApp\releases\pasa_sentinel_cyber_defense_promo.mp4"
$WebVideo = "E:\Projects\PrivateApp\pasa-commercial-web\public\releases\pasa_sentinel_cyber_defense_promo.mp4"
$Soundtrack = "$OutDir\soundtrack.wav"

$concatList = "$OutDir\kinetic_concat.txt"
if (Test-Path $concatList) { Remove-Item $concatList -Force }

Write-Host "Rendering 9 individual 4.0-second kinetic video beats..." -ForegroundColor Cyan

# Each beat is exactly 4.0 seconds (120 frames at 30fps)
for ($i = 1; $i -le 9; $i++) {
    $slide = "$OutDir\k_slide$i.png"
    $clip = "$OutDir\k_clip$i.mp4"

    # Smooth subtle Ken Burns zoom effect
    # zoom in from 1.0 to 1.05 over 120 frames
    & ffmpeg -y -loop 1 -i $slide -t 4.0 `
        -vf "scale=1920:1080,zoompan=z='min(zoom+0.0004,1.05)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=120:s=1920x1080:fps=30,format=yuv420p" `
        -c:v libx264 -preset medium -crf 18 -r 30 `
        $clip 2>$null

    Add-Content -Path $concatList -Value "file 'k_clip$i.mp4'"
    Write-Host "Beat $i rendered: k_clip$i.mp4 (4.0s)" -ForegroundColor Yellow
}

Write-Host "Concatenating all beats..." -ForegroundColor Cyan
$tempVideo = "$OutDir\k_merged_video.mp4"
& ffmpeg -y -f concat -safe 0 -i $concatList -c copy $tempVideo 2>$null

Write-Host "Muxing with 130 BPM Cyberpunk Soundtrack..." -ForegroundColor Cyan
& ffmpeg -y -i $tempVideo -i $Soundtrack `
    -map 0:v -map 1:a `
    -c:v copy -c:a aac -b:a 320k -shortest `
    $FinalVideo 2>$null

# Copy to web releases directory
Copy-Item $FinalVideo $WebVideo -Force

Write-Host "HIGH-ENERGY PROMO VIDEO COMPLETE!" -ForegroundColor Green
Write-Host "Target: $FinalVideo"
Write-Host "Web: $WebVideo"
