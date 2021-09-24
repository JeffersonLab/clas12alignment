#!/bin/sh -f

# Setup (Point these to the appropiate location)
TORUSMAP="Symm_torus_r2501_phi16_z251_24Apr2018.dat"
SOLENOIDMAP="Symm_solenoid_r601_phi1_z1201_13June2018.dat"
MVN="/path/to/mvn"
JAVAHOME="/usr"
COATJAVA="/path/to/coatjava-5.6.8"

# Don't touch anything from this point forward!
JAVALOC="$JAVAHOME/bin/java"
MAGFIELDSETUP="COAT_MATFIELD_TORUSMAP=$TORUSMAP COAT_MAGFIELD_SOLENOIDMAP=$SOLENOIDMAP"
DEXECARGS="-Dexec.args=-DCLAS12DIR=$COATJAVA"
CLASSPATHARG="-classpath %classpath"
DEXECEXECUTABLE="$JAVALOC"

PREVARGS="$DEXECARGS $CLASSPATHARG"
POSTARGS="-Dexec.executable=$DEXECEXECUTABLE process-classes org.codehaus.mojo:exec-maven-plugin:1.2.1:exec"

i="0"
args=""
for arg; do args="$args $arg"; done

# Run
export COAT_MAGFIELD_TORUSMAP="$TORUSMAP"
export COAT_MAGFIELD_SOLENOIDMAP="$SOLENOIDMAP"
$MVN "$PREVARGS org.clas.test.Main$args" $POSTARGS # > log.txt
