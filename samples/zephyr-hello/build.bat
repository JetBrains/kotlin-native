@echo off
setlocal

set BOARD_ARCH=arm_cortex_m3
set BOARD_NAME=qemu_cortex_m3
set TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-win32
if not defined ZEPHYR_BASE (set ZEPHYR_BASE=%userprofile%\zephyr)

set DIR=%~dp0
if "%KONAN_DATA_DIR%"=="" (set KONAN_DATA_DIR=%userprofile%\.konan)
set KONAN_DEPS=%KONAN_DATA_DIR%/dependencies
set GNU_ARM=%KONAN_DEPS%/%TOOLCHAIN%
set ZEPHYR_TOOLCHAIN_VARIANT=gnuarmemb
set GNUARMEMB_TOOLCHAIN_PATH=%GNU_ARM%

if defined KONAN_HOME (
    set "PATH=%KONAN_HOME%\bin;%PATH%"
) else (
    set "PATH=%DIR%..\..\dist\bin;%DIR%..\..\bin;%PATH%"
)

if not exist build\ (mkdir build)
cd build
if not exist CMakeCache.txt (cmake -GNinja -DBOARD=%BOARD_NAME% .. || exit /b)

echo|set /p="-- Compiling Kotlin files ... "
if exist program.o (del program.o)
if not exist kotlin\ (mkdir kotlin)
call konanc %DIR%/src/main.kt ^
        -target zephyr_%BOARD_ARCH% ^
        -g -o %DIR%/build/kotlin/program || exit /b
echo done

ninja run || exit /b