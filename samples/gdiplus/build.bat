@echo off
setlocal
set DIR=.
set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw

set "LFLAGS=-Wl,--subsystem,windows"

call konanc -target "%TARGET%" "%DIR%\src\main\kotlin" -linkerOpts "%LFLAGS%" -o gdiplus || exit /b
