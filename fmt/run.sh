#!/bin/sh -f

# Setup (Point these to the appropiate location)
TORUSMAP="Symm_torus_r2501_phi16_z251_24Apr2018.dat"
SOLENOIDMAP="Symm_solenoid_r601_phi1_z1201_13June2018.dat"
MVN="/home/twig/data/code/jsw/netbeans-12.0/netbeans/java/maven/bin/mvn"
JAVAHOME="/usr"
COATJAVA="/home/twig/data/code/jsw/coatjava-5.6.8"

# FILE="/home/twig/data/code/jsw/recon_data/out_clas_011983.hipo"
# FILE="/home/twig/data/code/jsw/recon_data/out_clas_012016.hipo"
FILE="/home/twig/data/code/jsw/recon_data/out_clas_012439_unaligned.hipo"

# Don't touch these!
JAVALOC="$JAVAHOME/bin/java"
MAGFIELDSETUP="COAT_MATFIELD_TORUSMAP=$TORUSMAP COAT_MAGFIELD_SOLENOIDMAP=$SOLENOIDMAP"
DEXECARGS="-Dexec.args=-DCLAS12DIR=$COATJAVA"
CLASSPATHARG="-classpath %classpath"
DEXECEXECUTABLE="$JAVALOC"

PREVARGS="$DEXECARGS $CLASSPATHARG"
POSTARGS="-Dexec.executable=$DEXECEXECUTABLE process-classes org.codehaus.mojo:exec-maven-plugin:1.2.1:exec"

# Run
export COAT_MAGFIELD_TORUSMAP="$TORUSMAP"
export COAT_MAGFIELD_SOLENOIDMAP="$SOLENOIDMAP"
$MVN "$PREVARGS org.clas.test.Main $FILE -n 100000 -v dZ -i -0.2 -l 0.2 -d 0.1" $POSTARGS # > log.txt
