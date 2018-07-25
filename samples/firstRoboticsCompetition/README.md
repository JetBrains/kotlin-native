# FIRST Robotics demo

Small demonstration of Kotlin-Native with [WPILib](https://github.com/wpilibsuite/allwpilib#wpilib-mission) on the [RoboRIO](http://www.ni.com/en-us/landing/first.html).

## Installation

    ./downloadWpilib.sh

will build [WPILib](https://github.com/wpilibsuite/allwpilib) in
`$HOME/.konan/third-party/allwpilib` (if not already built). One may override the location of
`third-party/allwpilib` by setting the `KONAN_DATA_DIR` environment variable.

To build use `../gradlew build` or `./build.sh`.

Make sure the `/home/lvuser/robotCommand` file on the RoboRIO is set to:

    /home/lvuser/FRCUserProgram.kexe
    
To deploy the demo, run the following:

    scp somePathHere/FRCUserProgram.kexe lvuser@roboRIO-YOURTEAMNUMBER-FRC.local:~/
    
    ssh admin@roboRIO-YOURTEAMNUMBER-FRC.local
    cd /home/lvuser
    setcap 'cap_sys_nice=pe' FRCUserProgram.kexe    # enable real-time execution
    /usr/local/frc/bin/frcKillRobot.sh -t -r        # restart robot code