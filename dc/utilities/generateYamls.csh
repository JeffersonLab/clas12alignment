#!/bin/csh -f

# $1 = base yaml file
# $2 = base variation
# $3 = output directory
# $4 = compare variation

if ($#argv < 3) then
 
  echo "Usage: generateYamls <base-yaml-file> <variation> <output-directory> [<compare-to-variation>]"
  exit 0
 
endif

set yaml = $1
set variation = $2
set outdir = $3
set compvariation = rga_fall2018
if ($#argv == 4) then
   set compvariation = $4
endif  

set var   = ( r0  r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz $compvariation )
set shift = ( 0.0 0.1  0.8  0.2  0.2   0.2   0.1  0.8  0.2  0.2   0.2   0.1  0.8  0.2  0.2   0.2   0.0          )

mkdir -p $outdir

echo
echo generating yaml files starting from $yaml
echo base variation set to $variation
echo

set v = 1
while ( $v <= $#var )
    set reg = `echo $var[$v] | awk -F"_" '{print $1}'`
    set axs = `echo $var[$v] | awk -F"_" '{print $2}'`

    
    if ( v == $#var ) then
        set variation = $var[$v]
        set align = "null"
    else if ( $var[$v] == "r0" ) then
        set align = "null"
    else
        set align = $var[$v]":"$shift[$v]       
    endif
    
    cp $yaml tmp.yaml
    sed -i s/"default"/$variation/g tmp.yaml
    sed -i s/"null"/$align/g tmp.yaml
    mv tmp.yaml $outdir/$var[$v]".yaml"
    echo generated $outdir/$var[$v]".yaml"

    @ v ++
end


