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

args=""
for arg; do args="$args $arg"; done

# Run
export COAT_MAGFIELD_TORUSMAP="$TORUSMAP"
export COAT_MAGFIELD_SOLENOIDMAP="$SOLENOIDMAP"
$MVN "$PREVARGS org.clas.test.Main$args" $POSTARGS
