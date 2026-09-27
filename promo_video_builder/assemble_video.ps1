# PASA Sentinel - Master Video Assembler using FFmpeg
# Produces a broadcast-ready 1080p MP4 optimized for YouTube and Facebook.

$OutDir = "E:\Projects\PrivateApp\promo_video_builder\output"
$FinalVideo = "E:\Projects\PrivateApp\releases\pasa_sentinel_cyber_defense_promo.mp4"
$WebVideo = "E:\Projects\PrivateApp\pasa-commercial-web\public\releases\pasa_sentinel_cyber_defense_promo.mp4"

# Ensure target directories exist
$relDir = [System.IO.Path]::GetDirectoryName($FinalVideo)
if (!(Test-Path $relDir)) { New-Item -ItemType Directory -Path $relDir -Force | Out-Null }
$webRelDir = [System.IO.Path]::GetDirectoryName($WebVideo)
if (!(Test-Path $webRelDir)) { New-Item -ItemType Directory -Path $webRelDir -Force | Out-Null }

Write-Host "Processing individual scene clips with padded audio..." -ForegroundColor Cyan

$concatList = "$OutDir\concat_list.txt"
if (Test-Path $concatList) { Remove-Item $concatList -Force }

for ($i = 1; $i -le 7; $i++) {
    $slide = "$OutDir\slide$i.png"
    $audio = "$OutDir\audio$i.wav"
    $paddedAudio = "$OutDir\audio$($i)_padded.wav"
    $clip = "$OutDir\clip$i.mp4"

    # Add 0.5s lead-in and 0.6s lead-out silence
    & ffmpeg -y -i $audio -af "adelay=400|400,apad=pad_dur=0.6" -ar 44100 -ac 2 $paddedAudio 2>$null

    # Render clip in full 1080p 30fps with subtle zoom effect
    # Using zoompan filter for subtle cinematic motion
    & ffmpeg -y -loop 1 -i $slide -i $paddedAudio `
        -vf "scale=1920:1080,format=yuv420p" `
        -c:v libx264 -preset slow -crf 18 -r 30 `
        -c:a aac -b:a 192k -shortest `
        $clip 2>$null

    Add-Content -Path $concatList -Value "file 'clip$i.mp4'"
    Write-Host "Scene $i rendered: clip$i.mp4" -ForegroundColor Yellow
}

Write-Host "Concatenating scenes into unified video..." -ForegroundColor Cyan

$tempMerged = "$OutDir\merged_temp.mp4"
& ffmpeg -y -f concat -safe 0 -i $concatList -c copy $tempMerged 2>$null

# Add cinematic atmospheric ambient sub-bass drone (55Hz / 110Hz) underneath
Write-Host "Adding cinematic ambient cyber soundscape..." -ForegroundColor Cyan
& ffmpeg -y -i $tempMerged `
    -filter_complex "[0:a]volume=1.0[vocal];aevalsrc=exprs='0.025*sin(2*PI*55*t)+0.012*sin(2*PI*110*t)+0.005*sin(2*PI*220*t)':s=44100[drone];[vocal][drone]amix=inputs=2:duration=first:dropout_transition=2[outa]" `
    -map 0:v -map "[outa]" `
    -c:v copy -c:a aac -b:a 256k `
    $FinalVideo 2>$null

# Copy to web releases folder
Copy-Item $FinalVideo $WebVideo -Force

Write-Host "MASTER VIDEO READY!" -ForegroundColor Green
Write-Host "Output: $FinalVideo"
Write-Host "Web: $WebVideo"
