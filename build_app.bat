@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
cd /d C:\Users\Admin\Downloads\Raival
call gradlew.bat assembleDebug > C:\Users\Admin\Downloads\Raival\build_out.txt 2>&1
echo EXIT_CODE=%ERRORLEVEL% >> C:\Users\Admin\Downloads\Raival\build_out.txt
