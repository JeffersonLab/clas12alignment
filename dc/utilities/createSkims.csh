#!/bin/csh -f

# $1 = cooked files directory
# $2 = output skims directory


if ($#argv == 0 || $#argv >2) then
 
  echo "Usage: createSkims.sh <reconstructed-files-directory> [<output-directory>]"
  exit 0
 
endif

set indir  = $1
if ($#argv == 2) then
    set outdir = $2
else 
    set outdir = $1
endif
mkdir -p $outdir/skims

echo
echo reading input files from $indir
echo writing skims to $outdir/skims
echo


foreach var ( r0 r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz rga_fall2018 )

    mkdir -p $outdir/skims/$var
    
    if(`filetest -d $indir/$var` == 1 ) then
        echo
        echo found variation $var

        foreach run (`ls $indir/$var/dst/recon`)
            hipo-utils -reduce -ct "REC::Particle://beta>0[GT]0,REC::Cherenkov://nphe>2[GT]0,REC::Calorimeter://energy>0[GT]0,TimeBasedTrkg::TBTracks://Vtx0_z>-20&&Vtx0_z<10[GT]0" -r "TimeBasedTrkg::TBHits://trkID>0" -b "RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBasedTrkg::TBTracks,TimeBasedTrkg::TBHits" -merge -o $outdir/skims/$var/$var"_clas_"$run".hipo" $indir/$var/dst/recon/$run/*
        end

    endif

end

