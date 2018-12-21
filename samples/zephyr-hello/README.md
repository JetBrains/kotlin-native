# Zephyr Hello World

Hello World sample for embedded devices based on [Zephyr](https://zephyrproject.org/)

## Installation

1. Checkout the source code of Zephyr:

        git clone https://github.com/zephyrproject-rtos/zephyr.git

2. Install Zephyr dependencies as described in
http://docs.zephyrproject.org/getting_started/getting_started.html

3. If you can succesfully run `./build.sh`, and you should get something like this:

        Building for board qemu_cortex_m3
        [32/38] Linking C executable zephyr/zephyr_prebuilt.elf
        Memory region         Used Size  Region Size  %age Used
                   FLASH:      149200 B       256 KB     56.92%
                    SRAM:       12824 B        64 KB     19.57%
                IDT_LIST:          20 B         2 KB      0.98%
        [38/38] To exit from QEMU enter: 'CTRL+a, x'[QEMU] CPU: cortex-m3
        ***** BOOTING ZEPHYR OS v1.10.99 - BUILD: Feb 27 2018 18:16:50 *****
        Hello Kotlin!

Congratulations, your first Kotlin programm runs in ARM Cortex-M simulator,
and you can write something for real devices.
