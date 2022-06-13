#!/bin/csh -f

# $1 = base yaml file
# $2 = base variation


if ($#argv != 2) then
 
  echo "Usage: generateYamls <base-yaml-file> <base-variation>"
  exit 0
 
endif

set yaml = $1
set variation = $2
set var   = ( r0  r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz rga_fall2018 )
set shift = ( 0.0 0.1  0.8  0.2  0.2   0.2   0.1  0.8  0.2  0.2   0.2   0.1  0.8  0.2  0.2   0.2   0.0          )

mkdir -p yamls

echo
echo generating yaml files starting from $yaml
echo base variation set to $variation
echo

set v = 1
while ( $v <= $#var )
    set reg = `echo $var[$v] | awk -F"_" '{print $1}'`
    set axs = `echo $var[$v] | awk -F"_" '{print $2}'`

    
    if ( $var[$v] == "rga_fall2018" ) then
        set variation = "rga_fall2018"
        set align = "null"
    else if ( $var[$v] == "r0" ) then
        set align = "null"
    else
        set align = $var[$v]":"$shift[$v]       
    endif
    
    cp $yaml tmp.yaml
    sed -i s/"default"/$variation/g tmp.yaml
    sed -i s/"null"/$align/g tmp.yaml
    mv tmp.yaml yamls/$var[$v]".yaml"
    echo generated yamls/$var[$v]".yaml"

    @ v ++
end

