rm -f *.7z

7z a '-x!*.dex'  '-x!*.jar' '-xr!.git' '-xr!.idea' '-xr!build' '-xr!.gradle' '-x!*.exe' '-x!*.rar' '-x!*.7z' '-x!*.hprof' '-x!*.sfx' '-x!*.log' '-x!*.tar' '-x!*.apk' '-x!*.zip' '-x!*.dex' '-x!*.class' Flym.7z ../Flym

cp Flym*.7z /home/user/Yandex.Disk

rem pause
