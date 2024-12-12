#!/bin/csh -f

# $1 = cooked files directory
# $2 = output skims directory
# $3 = list of variations

if ( $#argv < 1 || $#argv > 3) then

  echo "Usage: createSkims.sh <reconstructed-files-directory> [<output-directory>] [<list of variations>]"
  exit 0
 
endif

set indir  = $1
if ($#argv >= 2) then
    set outdir = $2
else 
    set outdir = $1
endif
mkdir -p $outdir/skims
if ( $#argv >= 3) then
    set vars = "$3"
else
    set vars = "r0 r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz"
endif

set schema = `ls $indir/r0/*/recon/README.json | awk -F"$indir/r0/" '{print $2}' | awk -F"/" '{print $1}'`

echo
echo reading input files from $indir with subdirectory $schema
echo writing skims to $outdir/skims for variations $vars
echo


foreach var ( $vars )
#r0 r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz )

    set vardir = $indir/$var/$schema/recon

    if(`filetest -d $vardir` == 1 && `find $vardir -type d | wc -l` > 0) then
        echo
        echo found variation $var

        mkdir -p $outdir/skims/$var
    
        foreach rundir (`find $vardir/* -type d`)
            echo skimming files in $rundir
            set run = `echo $rundir | rev | awk -F"/" '{print$1}' | rev`
            hipo-utils -reduce -ct "REC::Particle://beta>0[GT]0,REC::Cherenkov://nphe>2[GT]0,REC::Calorimeter://energy>0[GT]0,TimeBasedTrkg::TBTracks://Vtx0_z>-15&&Vtx0_z<35[GT]0" -r "TimeBasedTrkg::TBHits://trkID>0" -b "RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBasedTrkg::TBTracks,TimeBasedTrkg::TBHits" -merge -o $outdir/skims/$var/$var"_clas_"$run".hipo" $rundir/\*
        end

    endif

end


