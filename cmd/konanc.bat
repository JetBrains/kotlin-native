@echo off
rem based on scalac.bat from the Scala distribution
rem ##########################################################################
rem # Copyright 2002-2011, LAMP/EPFL
rem # Copyright 2011-2017, JetBrains
rem #
rem # This is free software; see the distribution for copying conditions.
rem # There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A
rem # PARTICULAR PURPOSE.
rem ##########################################################################

setlocal
call :set_home

if "%_KONAN_COMPILER%"=="" set _KONAN_COMPILER=org.jetbrains.kotlin.cli.bc.K2NativeKt

if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%_JAVACMD%"=="" set _JAVACMD=java

set "KONAN_JAR=%_KONAN_HOME%\konan\lib\backend.native.jar"
set "KOTLIN_JAR=%_KONAN_HOME%\konan\lib\kotlin-compiler.jar"
set "INTEROP_JAR=%_KONAN_HOME%\konan\lib\Runtime.jar"
set "HELPERS_JAR=%_KONAN_HOME%\konan\lib\helpers.jar"
set "NATIVE_LIB=%_KONAN_HOME%\konan\nativelib"
set "KONAN_CLASSPATH=%KOTLIN_JAR%;%INTEROP_JAR%;%KONAN_JAR%;%HELPERS_JAR%"
set "JAVA_OPTS=-ea -Dfile.encoding=UTF-8"

"%_JAVACMD%" %JAVA_OPTS% "-Dkonan.home=%_KONAN_HOME%" -noverify -cp "%KONAN_CLASSPATH%" ^
    "-Djava.library.path=%NATIVE_LIB%" ^
    -ea -Dfile.encoding=UTF-8 ^
    %_KONAN_COMPILER% %*

exit /b %ERRORLEVEL%
goto end

rem ##########################################################################
rem # subroutines

:set_home
  set _BIN_DIR=
  for %%i in (%~sf0) do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _KONAN_HOME=%_BIN_DIR%..
goto :eof

:end
endlocal

