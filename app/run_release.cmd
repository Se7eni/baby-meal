@echo off
set JAVA_HOME=C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\android-dev\jdk\jdk-17.0.20.1+1
set ANDROID_HOME=C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\android-dev\sdk
set ANDROID_SDK_ROOT=C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\android-dev\sdk
set GRADLE_USER_HOME=C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\android-dev\gradle_home
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\app
call C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\android-dev\gradle\gradle-8.7\bin\gradle.bat clean assembleRelease --console=plain --no-daemon > C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\android-dev\build_release.out 2>&1
echo EXITCODE=%ERRORLEVEL% >> C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\android-dev\build_release.out
