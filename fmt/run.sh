#!/bin/sh -f

# Setup (Point these to the appropiate location)
TORUSMAP="Symm_torus_r2501_phi16_z251_24Apr2018.dat"
SOLENOIDMAP="Symm_solenoid_r601_phi1_z1201_13June2018.dat"
MVN="mvn"
JAVAHOME="/usr"
COATJAVA="/path/to/coatjava"

# Don't touch anything from this point forward!
JAVALOC="$JAVAHOME/bin/java"
DEXECARGS="-Dexec.args=-DCLAS12DIR=$COATJAVA"
CLASSPATHARG="-classpath %classpath"
DEXECEXECUTABLE="$JAVALOC"

PREVARGS="$DEXECARGS $CLASSPATHARG"
POSTARGS="-Dexec.executable=$DEXECEXECUTABLE process-classes org.codehaus.mojo:exec-maven-plugin:1.2.1:exec"

# Get arguments from user.
args=""
for arg; do args="$args $arg"; done

# If args are given, run. Otherwise, print usage and exit gracefully.
if [ ! -z "$args" ]; then
    export COAT_MAGFIELD_TORUSMAP="$TORUSMAP"
    export COAT_MAGFIELD_SOLENOIDMAP="$SOLENOIDMAP"
    $MVN "$PREVARGS org.clas.test.Main$args" $POSTARGS
    STATUS=$?
else
    STATUS=1
fi

# If status is 1, cat usage.
if [ $STATUS -ne 0 ]; then
    if [ -f error_report.txt ]; then
        printf "\nError: "
        cat error_report.txt
        rm error_report.txt
    fi
    printf "\n"

    # TODO. Improve usage.txt.
    # NOTE. Currently, we print usage.txt even if there's a compilation. Ideally, we don't want
    #       this, but I'm not familiar enough with maven to fix this behavior. -- Bruno.
    printf "Usage: $0 infile\n"
    cat usage.txt
    printf "\n"
fi
