@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
cd /d C:\Users\Admin\Downloads\Raival
call gradlew.bat assembleDebug --offline > C:\Users\Admin\Downloads\Raival\bg-build.log 2>&1
echo DONE_%ERRORLEVEL% >> C:\Users\Admin\Downloads\Raival\bg-build.log