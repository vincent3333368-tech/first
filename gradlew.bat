@echo off
setlocal

set GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7
if exist "%GRADLE_HOME%\bin\gradle.bat" (
  call "%GRADLE_HOME%\bin\gradle.bat" %*
  set GRADLE_EXIT_CODE=%ERRORLEVEL%
  exit /b %GRADLE_EXIT_CODE%
)

echo Gradle 8.7 was not found in the local cache.
echo Install Gradle or generate a standard Gradle wrapper, then rerun this command.
exit /b 1
