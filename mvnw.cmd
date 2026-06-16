@REM Maven Wrapper startup batch script
@REM This script downloads Maven if not present and runs the build

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0..
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

if exist %WRAPPER_JAR% goto runWrapper

echo Downloading Maven Wrapper...
powershell -Command "& { Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper.jar' -OutFile %WRAPPER_JAR% }"

:runWrapper
%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar %*
