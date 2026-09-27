Add-Type -AssemblyName System.Speech
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.SelectVoice("Microsoft David Desktop")
$synth.Rate = 0
$synth.SetOutputToWaveFile("E:\Projects\PrivateApp\promo_video_builder\test_audio.wav")
$synth.Speak("PASA Sentinel. Uncompromising physical anti-theft and mobile cyber defense.")
$synth.SetOutputToNull()
$synth.Dispose()
Write-Output "AUDIO SUCCESS"
