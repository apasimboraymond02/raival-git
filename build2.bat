@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
cd /d C:\Users\Admin\Downloads\Raival
call gradlew.bat assembleDebug 2>&1
echo EXIT_CODE=%ERRORLEVEL%
