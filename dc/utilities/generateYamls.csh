#!/bin/csh -f

# $1 = base yaml file
# $2 = base variation


if ($#argv != 2) then
 
  echo "Usage: generateYamls <base-yaml-file> <base-variation>"
  exit 0
 
endif

set yaml = $1
set variation = $2
set var  = ( r0 r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz rga_fall2018 )

mkdir -p yamls

echo
echo generating yaml files starting from $yaml
echo base variation set to $variation
echo

set v = 1
while ( $v <= $#var )
    set reg = `echo $var[$v] | awk -F"_" '{print $1}'`
    set axs = `echo $var[$v] | awk -F"_" '{print $2}'`

    set shift = 0
    if( $axs == "x" ) then
        set shift = 0.1
    else if( $axs == "y" ) then
    	set shift = 0.8
    else if( $axs == "z" ) then
    	set shift = 0.2
    else if ( $axs == "cy" ) then
        set shift = 0.2
    else if ( $axs == "cz" ) then
        set shift = 0.2
    endif
    
    if ( $var[$v] == "rga_fall2018" ) then
         set variation = "rga_fall2018"
    endif
    
    cp $yaml tmp.yaml
    sed -i s/"default"/$variation/g tmp.yaml
    sed -i s/"null"/$var[$v]":"$shift/g tmp.yaml
    mv tmp.yaml yamls/$var[$v]".yaml"
    echo generated yamls/$var[$v]".yaml"

    @ v ++
end

