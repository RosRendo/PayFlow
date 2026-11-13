@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Startup Batch script
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%" == "" @ECHO OFF
@REM set %CURRENT_DIR% to the path to the current directory
SET CURRENT_DIR=%~dp0

@REM Set local scope for the variables with windows NT shell
IF "%OS%"=="Windows_NT" SETLOCAL

@REM Find JAVA_HOME if not set
IF NOT "%JAVA_HOME%" == "" GOTO skipJavaDetection
IF EXIST "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    SET "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
:skipJavaDetection

SET MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
IF NOT EXIST %MAVEN_JAVA_EXE% (
    SET MAVEN_JAVA_EXE=java
)

@REM Execute Maven Wrapper downloader or fallback PowerShell command
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { & '%~dp0mvnw.ps1' %* }"
EXIT /B %ERRORLEVEL%

