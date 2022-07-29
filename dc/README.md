# dc-alignment

This code implements the CLAS12 DC alignment procedure developed by T. Hayward and documented at clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf.

The algorithm is based on the assumption that the *real* DC geometry is close to the *nominal* geometry and that the transformation between the two can be described as a linear combination of translations and rotations in x, y and z of each DC region. The size of these translations and rotations is determined studying how the tracking fit residuals and the z vertex distribution for straight tracks vary as a function of individual translations or rotations and finding the set that minimize the residuals and the difference between the z vertex distribution and the known target position.
Specifically:
* Straight electron tracks are reconstructed and histograms of fit residuals for each DC layer and of the z-vertex distribution are made in bins of sector, polar and azimuthal angles. These histograms are analyzed to extract the shifts from zero of the fit residuals and the shifts of the z-vertex from the known target position. Since straight track runs are usually on empty target, the z-vertex histograms show peaks corresponding to the target cell windows that can be fit and compared to the installation position.
* The same tracks are reconstructed applying a single translation or rotation. The size of these is chosen to be large enough to have a measureable effect on fit residuals and vertex distributions, while being small compared to the DC cell size. The values used so far are 0.1-0.8 cm for shifts and 0.2 deg for rotations. Translations and rotations are applied in the tilted-sector coordinate frame (y axis along the DC wires and z axis perpendicular to the DC layers, i.e. at 25 deg from the beamline axis). Currently, the x rotation is not supported by tracking software. Therefore, the total number of shifts and rotation is 15, 3 translations and 2 rotations for each of the 3 regions. 
* The derivatives of the fit residuals and z-vertex versus each translation and rotation are extracted comparing the fit residuals and z-vertex for that geometry to the nominal geometry. 
* The nominal-geometry residual and vertex shifts with respect to the desired positions are fit to a linear combinations of the derivatives to extract the translations and rotation sizes. This global fit is performed with Minuit, minimizing a chi2 defined as the sum of squares of the residual and vertex shift normalized to their uncertainties. 
* The fit parameters are printed out in a format corresponding to the /geometry/dc/alignment CCDB table used by reconstruction. Here translations are defined in the CLAS12 frame while rotations are defined in the tilted-sector coordinate system.

### Prerequisites
* Software:
  * A Linux or Mac computer
  * Java Development Kit 11 or newer
  * maven 
* Data:
  * Straight-track data (both solenoid and torus should be off) with electron tracks in the forward detector
  * Recent Sqlite snapshot of CCDB (see https://clasweb.jlab.org/clas12offline/sqlite/ccdb/)


### Data processing
  * Process the straight-track data with the CLAS12 reconstruction code, using the nominal geometry and each of the individual translations or rotations in xyz for each of the DC regions. This will result in up to 16 sets of reconstructed files that will be the input of the alignment code.  The nominal geometry can be the DC design geometry (CCDB variation: *nominal*) or a geometry determined from a previous alignment with non-zero shifts compared to the nominal case. 
    * Setup or check the chosen geometry variation in the Sqlite file which is being used. The selected variation should also be populated with realistic geometry tables for the other CLAS12 detectors. For instance, in the CCDB variation *nominal* detectors such as FTOF and ECAL are shifted by ~5 cm downstream the ideal position to account for the actual installation position of the Forward Carriage. If using a variation different from *nominal*, make sure the beam offsets constants (CCDB table ``/geometry/beam/position``) are set appropriately, i.e. set equal to the best guess of the actual bam position for the straight track run or to x=y=0, if no other information is available.
    * The reconstruction configuration files (yaml files) to produce the 16 sets of data can be generated from the template file ``dcalign.yaml`` provided with the coatjava distribution (supported starting from coatjava 8.2.1). Copy the file to your work directory and edit it, replacing the variation in the global section (*rga_fall2018* in the coatjava 8.2.1 file) with the chosen variation. Leave the other variation settings unchanged. Run the script [generateYamls.csh](https://github.com/JeffersonLab/clas12alignment/blob/dcDev3/dc/utilities/generateYamls.csh):
      ```
      ./generateYamls.csh <base-yaml-file> <variation>  <output-directory>
      ```
      where:
      * The base yaml file will be ```dcalign.yaml```.
      * The variation will be the one containing the chosen nominal geometry as discussed above. 
      * The generated yaml files will be in the folder specified when running the command. The folder will contain the 15 yaml files corresponding to the translations and rotations described above, the yaml file ```r0.yaml```, corresponding to the nominal geometry, and rga_fall2018.yaml, corresponding to the original alignment results by T. Haywards.
        In the yaml files, the desired rotation or translation defines the value of the variable ```alignmentShift```, which has to be set for each of the DC reconstruction services. For example, the configuration        
        ```
        dcGeometryVariation: "default"
        alignmentShifts: "r1_cz:0.2"
        ```
        specifies that a 0.2 deg z rotation of region 1 will be applied on top of the geometry defined in the variation selected with the variable ```dcGeometryVariation```. This allows to perform multiple iterations by selecting a variation where the ``/geometry/dc/alignment table`` contains the results of the previous iteration.
    * For each yaml file, generate and run one cooking workflow to process the straight track data (see the [CLAS12 chef documentation](https://clasweb.jlab.org/wiki/index.php/CLAS12_Chef_Documentation):
      * Use as output directory name for the workflow the same name of the yaml file without the extention (e.g. the output of the data processed with r1_cz.yaml should be in a directory named r1_cz). 
      * Make sure to use a schema including the banks: ``RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBasedTrkg::TBTracks,TimeBasedTrkg::TBHits`` (tip: copy the dst schema directory from the coatjava distribution to a suitable location and add to it the two time-based tracking banks. The dst schema directory can be found at $COATJAVA/etc/bankdefs/hipo4/singles/dst)
      * Use the --ccdbsqlite workflow option to point to the Sqlite snapshot that is being used for the alignment.
      * Since the workflows that should be generated have the same configuration except for the selected yaml file, the tag, and the output directory, an easy way to generate all of then with a single command is to use a command-line for-loop. In cshell or tcshell, this would look like:
              ```
              foreach var ( r0 r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz rga_fall2018 )
                 clas12-workflow.py --runGroup rgb --model rec --tag myTag_$var --inputs /mss/clas12/rg-b/production/decoded/6.5.6/006342 --runs 6342 --clara /group/clas12/packages/clara/5.0.2_8.1.2 --outDir /my-ouput-drectory-path/$var --reconYaml path-to-yamls-directory/$var".yaml" --ccdbsqlite path-to-sqlite-file --reconSize 1 --threads 16
              end
              ```
        Similarly, the workflows submission can be done as follows:
              ```
              foreach var ( r0 r1_x r1_y r1_z r1_cy r1_cz r2_x r2_y r2_z r2_cy r2_cz r3_x r3_y r3_z r3_cy r3_cz rga_fall2018 )
                 swif2 import -file rgb-r-myTag_$var"-6342.json"
                 swif2 run -workflow  rgb-r-myTag_$var"-6342"
              end
              ```
      * Notes: 
	* The above workflow creation command is just an example and should be modified depending on the current workflow tools options and the data set. 
	* Only [a-z A-z 0-9 _] symbols can be used for the --tag option.
        * Since the alignment procedure requires processing the same data multiple times, it may be convenient to prefilter the events in the alignment run to select the ones of interest for the FD. This applies in particular to recent alignment runs where the trigger included a FD electron trigger and a CD trigger, with the latter having much higher rate than the former. The selection could be done on decoded files, saving only the events where the FD trigger bit is set or that have HTCC/ECAL banks.
             
* Alignment input files:
  * To reduce the reconstructed data volume and speed up the analysis, files for each geometry variation can be filtered with the command:
    ```
    hipo-utils -reduce -ct "REC::Particle://beta>0[GT]0,REC::Cherenkov://nphe>2[GT]0,REC::Calorimeter://energy>0[GT]0,TimeBasedTrkg::TBTracks://
    Vtx0_z>-15&&Vtx0_z<35[GT]0" -r "TimeBasedTrkg::TBHits://trkID>0" -b "RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBased
    Trkg::TBTracks,TimeBasedTrkg::TBHits" -o outputfilename inputfiles
    ```
    where the vertex, nphe and energy cut should be selected according to the experiment configuration (beam energy and target).
    To facilitate this step, use the script [``createSkims.csh``](https://github.com/JeffersonLab/clas12alignment/blob/dcDev3/dc/utilities/createSkims.csh):
    ```
    createSkims.sh <reconstructed-files-directory> [<output-directory>]
    ```
    The tracks selection is further refined by the alignment code to identify electrons. See the ``getElectron()`` method in the ``Histo`` class, using     parameters from the ``Constants`` class.

### Build and run
Clone this repository and checkout the dcDev4 branch:
```  
  git clone https://github.com/JeffersonLab/clas12alignment
  cd clas12alignment
  git checkout dcDev3
```
Go to the folder clas12alignment/dc/java/dc-alignment and compile with maven:
```
  cd dc/java/dc-alignment
  mvn install
```
Set the CCDB connection environment variable to point with the Sqlite file that is being used for alignment:
```
  setenv CCDB_CONNECTION sqlite:///path-to-sqlite-file
```

* Note: The CCDB_CONNECTION variable may be overwritten when loading certain modules, such as the clas12 module. Make sure to set this path after loading such a module.

Run the code with:
```
  ./bin/dc-alignment
  
    Usage : dc-alignment [commands] [options]


    commands:
          -process : process event files
              -fit : perform misalignment fit
          -analyze : analyze histogram files
```
  
### Usage 
The code supports three main usage options to either process the hipo files with the reconstructed events using the nominal geometry and translated/rotated geometry or analyze a pre-existing histogram file starting from the fit of residuals and vertex distributions or performing only the final minimization to extract the misalignments.
#### Process hipo event files
Check the command line options with:
```
./bin/dc-alignment -process

     Usage : dc-alignment -nominal [nominal geometry hipo file or directory]  [input1] [input2] ....

   Options :
  -compare : database variation for constant comparison (default = default)
  -display : display histograms (0/1) (default = 1)
     -init : init global fit from previous constants from the selected variation (default = default)
     -iter : number of global fit iterations (default = 1)
   -nevent : maximum number of events to process (default = -1)
        -o : output histogram file name prefix (default = )
      -phi : phi bin limits, e.g. "-30:-10:0:10:30" (default = -30:0:30)
    -r1_cx : r1_cx hipo file or directory (default = )
    -r1_cy : r1_cy hipo file or directory (default = )
    -r1_cz : r1_cz hipo file or directory (default = )
     -r1_x : r1_x hipo file or directory (default = )
     -r1_y : r1_y hipo file or directory (default = )
     -r1_z : r1_z hipo file or directory (default = )
    -r2_cx : r2_cx hipo file or directory (default = )
    -r2_cy : r2_cy hipo file or directory (default = )
    -r2_cz : r2_cz hipo file or directory (default = )
     -r2_x : r2_x hipo file or directory (default = )
     -r2_y : r2_y hipo file or directory (default = )
     -r2_z : r2_z hipo file or directory (default = )
    -r3_cx : r3_cx hipo file or directory (default = )
    -r3_cy : r3_cy hipo file or directory (default = )
    -r3_cz : r3_cz hipo file or directory (default = )
     -r3_x : r3_x hipo file or directory (default = )
     -r3_y : r3_y hipo file or directory (default = )
     -r3_z : r3_z hipo file or directory (default = )
-residuals : fit residuals (2) or use mean (1) (default = 2)
   -sector : sector-dependent derivatives (1) or average (0) (default = 1)
    -stats : histogram stat option (default = )
    -theta : theta bin limits, e.g. "5:10:20:30" (default = 5:10:20)
     -time : make time residual histograms (1=true, 0=false) (default = 0)
  -verbose : global fit verbosity (1/0 = on/off) (default = 0)
   -vertex : fit vertex plots with 3 gaussians (4), 2 gaussians (3), 1 gaussian plus background (2) or only 1 gaussian (1) (default = 4)
```
The code will process the input files specified with the ```-nominal``` or ```-r[123]_[c][xyz]``` options, create and fill histograms according to the selected theta and phi bins, run the analysis, plot the results and printout the extracted alignment constants. All histograms will be saved to an histogram file named ``prefix_histo.hipo``, with ``prefix`` being the string specified with the ```-o``` option, or ``histo.hipo`` if the option is not used. 
By specifying ``-display 0``, the graphical window presenting the plotted results will not be opened.

The following is an example of how one would process directories of the reconstructed data files, utilizing 12 geometry variations (9 translations and 3 rotations). In this example, three theta bins are used (8 to 14, and 14 to 22 [degrees]) and two bins in phi are used (-30 to 0, and 0 to 30 [degrees]).
```
./bin/dc-alignment -process -nominal path-to-r0-files -r1_x path-to-r1_x -r1_y path-to-r1_y/ -r1_z path-to-r1_z -r1_cy path-to-r1_cy -r1_cz path-to-r1_cz -r2_x path-to-r2_x -r2_y path-to-r2_y -r2_z path-to-r2_z -r2_cy path-to-r2_cy -r2_cz path-to-r2_cz -r3_x path-to-r3_x -r3_y path-to-r3_y -r3_z path-to-r3_z -r3_cy path-to-r3_cy -r3_cz path-to-r3_cz -theta 6:8:12:36 -phi -30:-10:0:10:30 -vertex 4 -residuals 2  -compare rga_fall2018 -o output_file_name_prefix

```
#### Analyze a histogram file
Check the command line options with:
```
./bin/dc-alignment -analyze

     Usage : dc-alignment -input [input histogram file]  [input1] [input2] ....

   Options :
  -compare : database variation for constant comparison (default = default)
  -display : display histograms (0/1) (default = 1)
     -init : init global fit from previous constants from the selected variation (default = default)
     -iter : number of global fit iterations (default = 1)
-residuals : fit residuals (2) or use mean (1) (default = 2)
   -sector : sector-dependent derivatives (1) or average (0) (default = 1)
   -shifts : use event-by-event subtraction for unit shifts (1=on, 0=off) (default = 0)
    -stats : set histogram stat option (default = )
  -verbose : global fit verbosity (1/0 = on/off) (default = 0)
   -vertex : fit vertex plots with 3 gaussians (4), 2 gaussians (3), 1 gaussian plus background (2) or only 1 gaussian (1) (default = 4)
```
The code will read the histograms from the specified file, analyze them, compute the unit derivatives and nominal residuals and vertex values, perform the misalignment fit,plot the results and print the extracted alignment constants. 

Here is example of using the "analyze" option to analyze an already created histogram hipo file:
```
./bin/dc-alignment -analyze -compare rga_fall2018 -input /path/to/histo.hipo
```

#### Fit misalignments from a histogram file
Check the command line options with:
```
./bin/dc-alignment -fit

     Usage : dc-alignment -input [input histogram file]  [input1] [input2] ....

   Options :
  -compare : database variation for constant comparison (default = default)
  -display : display histograms (0/1) (default = 1)
     -init : init global fit from previous constants from the selected variation (default = default)
     -iter : number of global fit iterations (default = 1)
   -sector : sector-dependent derivatives (1) or average (0) (default = 1)
   -shifts : use event-by-event subtraction for unit shifts (1=on, 0=off) (default = 0)
    -stats : set histogram stat option (default = )
  -verbose : global fit verbosity (1/0 = on/off) (default = 0)
  ```
The code will read the histograms from the specified file, retrieve the relevant parameters from the histograms or fits, compute the unit derivatives and nominal residuals and vertex values, perform themisalignment fit,plot the results and print the extracted alignment constants. 

Here is example of using the "fit" option to extract misalignments from an already created histogram hipo file:
```
./bin/dc-alignment -fit -compare rga_fall2018 -input /path/to/histo.hipo
```

### Parameters
In addition to the parameters that can be selected from command line, the code uses parameters defined in the ``Constants`` class:
* the electron selection cuts,
* target parameters relevant for the vertex fits,
* global fit parameters initialization values and step size.
These can be modified according to the needs before compiling and running the code.

### Output
When the ``-process`` option is chosen, a file containing all histograms produced in the data processing is saved and can be re-analyzed with the ``-analyze`` or with the ``-fit`` options.
With all options, the extracted misalignment constants are printed out in a format consistent with the /geometry/dc/alignment CCDB table.

### Plots and results
If the ``-display`` option is set to 1 (default), a graphic window displaying histograms and relevant graphs is opened. 
![Screenshot 2022-07-05 at 18 55 23](https://user-images.githubusercontent.com/7524926/177378621-92170209-c0bc-4d68-aa38-7caf9f72cc66.png)

#### Analysis tab
The tab displays a summary of the extracted residuals and vertex shifts and derivatives. It includes the following sub-tabs:
* nominal: graphs of the extracted residuals and vertex shifts for the nominal geometry. Each plot corresponds to a different sector and the color points to different polar angle bins; different symbols are used to display the phi bins. The vertex shifts are displayed as layer=0, in 10s of um. See screenshot of graph below.
![Plot_02-19-2022_10 20 52_PM](https://user-images.githubusercontent.com/7524926/154820094-0bca8488-a895-474d-b8e2-22e25f42e7a6.png)
* nominal vs. theta: same as above but with the y-axis defined as the angular bin number plus the layer number. The different colors correspond to the different DC superlayers and the black points show the vertex shifts. See screenshot of graph below.
![Plot_02-19-2022_10 21 21_PM](https://user-images.githubusercontent.com/7524926/154819746-af0ee5bc-3e22-41b1-a00f-50f6d20d84c7.png)
* corrected and corrected vs. theta: same as above but after applying the translations and rotations from the /geometry/dc/alignment table in the CCDB variation specified with the ``-compare`` option. (Example not shown.)
* shift magnitude: histogram of the residual and vertex shifts associated with the chosen translations and rotations. The top histogram shows the average change of the track residual for translations and rotations of region 1 (x:1-6), region 2 (x:7-12), and region 3 (x:13-18). Within each region range, the first 3 x values coresponds to the xyz translations and the second 3 x values to xyz rotations. The missing bins at x=4 are due to the x rotations not being used. The different colors correspond to the theta bins (black is all theta) and the different color shades correspond to the phi bins. The bottom histogram shows the same information for vertex shifts.
![Plot_07-05-2022_07 06 10_PM](https://user-images.githubusercontent.com/7524926/177380554-d91f0da9-29c7-412d-b429-796c22653b6f.png)
* r1_x, ...r3_cy: graphs of the fit residuals and vertex derivatives for the corresponding translation or rotation. Each graph corresponds to a different angular bin. Colors correspond to different sectors while the average is shown in black. An example is shown by the following graph.
![Plot_02-19-2022_10 22 13_PM](https://user-images.githubusercontent.com/7524926/154819842-7f8f4f72-ad4d-4d27-a0e3-ada02b65447a.png)
* corrected(with new parameters) and corrected(with new parameters) vs. theta: same as nominal or corrected, but after aplying the combination of shifts and rotations resulting from the global fit. (Example not shown.)
* before/after: shows the distribution of all tracking and vertex residuals corrected with the fitted misalignements (red), with the misalignments specified with the ``-compare`` option (green) and with nominal geometry (black). In the example below, the nominal residuals are almost completely out of scale while the comparison between the new and old parameters shows the level of improvement.
![Plot_07-05-2022_07 05 04_PM](https://user-images.githubusercontent.com/7524926/177380199-77dec15e-95b8-41b5-8ed5-75dc42662ec5.png)
* misalignments: shows the comparison of the current analysis and minuit fit (red) and of the constants from the /geometry/dc/alignment table in the CCDB variation specified with the ``-compare`` option (black). If the ``-compare`` option is not specified, the black points will be at x=0. Each column corresponds to a sector. Top and bottom corresponds to translations and rotations, respectively. The y coordinate identifies the DC region and translation/rotation coordinate. For example, points at y 0, 1 and 2 correspond to region 1 x, y and z translations or rotations.
![Plot_07-05-2022_07 05 21_PM](https://user-images.githubusercontent.com/7524926/177382223-27eb9b1b-44fa-4405-87de-f1f777bb5beb.png)
* internal-only: same as the previous tab but after removing ''global'' geometry transformation that cannot be constrained with the current straight track alignment. These so-called weak modes are overall contractions or expansions in the size of the detector and rotations of whole sectors. The displayed parameters are corrected by normalizing the region 1 position to the nominal one.
![Plot_07-05-2022_07 05 37_PM](https://user-images.githubusercontent.com/7524926/177382868-88ae1ed1-d3b7-4245-9764-4ff90fb9ee65.png)


#### Electron
The tab displays the relevant distributions for the selected electron tracks. Examples of the plots are shown in the following two figures. These are meant to facilitate the choice of angular bins for the alignment, since having enough statistics in each bin is critical for the fits to converge.
![Plot_07-06-2022_12 32 00_PM](https://user-images.githubusercontent.com/7524926/177531547-8cd47d41-52d9-4834-aea8-de0644820fa0.png)
![Plot_07-06-2022_12 32 20_PM](https://user-images.githubusercontent.com/7524926/177531572-964c21c9-4b42-4ee2-907b-3e11382118b4.png)

#### Nominal
This tab displays histograms of the z-vertex distributions and residuals for each angular bin and sector.
* Z-vertex histograms: the picture below shows an example of a typical distribution for data taken with the 5cm-long LH2 target cell. The three peaks correspond to the cell windows and to a superinsulation foil. The upstream cell window is the left-most peak, the downstream cell window is the next peak to the right, and the superinsulation foil is the smaller, right-most peak. The distribution is fit to the sum of three Gaussians describing the windows and foil, plus a fourth broader gaussian describing the background from residual gas or badly reconstructed tracks. To ensure a good fit convergence, the 3 peak's sigmas are set to be the same and the distances between the peaks are constrained to the known values from the target geometry. The fit parameters are:
  * amp: height of the downstream cell window peak,
  * mean: mean of the downstream cell window peak (to be compared with the nominal of 0.5 cm),
  * tl: distance between the cell windows,
  * sigma: sigma of the downstream cell window peak,
  * wd: distance between the 2nd and 3rd peak,
  * bg: amplitude of the background function.

![Plot_02-19-2022_10 47 51_PM](https://user-images.githubusercontent.com/7524926/154820232-aa246a66-d90a-4d02-8049-eb6183aad146.png)
* Residual plots: residual histograms for each sector and angular bin are displayed in separate subtabs. On each, 6x6 plots show the distributions for each DC layer, as shown by the picture below. The residual shift from zero is by default estimated by performing a gaussian fit. Alternatively, the histogram mean can be used setting the ``-fit`` option to 0. Plots of time-residuals (on tabs TSec...) are included to allow checking the quality of the time calibrations.
![Plot_02-19-2022_11 03 15_PM](https://user-images.githubusercontent.com/7524926/154820651-63a37d6b-53ad-4669-84c6-1befab1216a6.png)

Equivalent sub-tabs and plots are shown for each translation and rotation in the r1_c, r1_y, ...,r3_cy tabs.

#### Terminal output
When launched, the alignment code will print-out relevant information as it goes through the various steps. These include:
* The chosen alignment configuration parameters, as for example:
  ```
  [CONFIG] subtractedShifts set to false
  [CONFIG] sectorShifts set to true
  [CONFIG] fitIteration set to 1
  [CONFIG] fitVerbosity set to false
  ...
  Opening file: iss805-dcAlMin-oldCalib-iter1.hipo
  [CONFIG] Setting theta bins to:
	   bin: 6.0-8.0
	   bin: 8.0-12.0
	   bin: 12.0-36.0
  [CONFIG] Setting phi bins to:
   	bin: -30.0--10.0
	   bin: -10.0-0.0
	   bin: 0.0-10.0
	   bin: 10.0-30.0
  [CONFIG] resFit set to 0
  [CONFIG] vertexFit set to 0
  ```
* The CCDB connection logs, as for example:
  ```
  [DB] --->  open connection with : sqlite:////Users/devita/Work/clas12/simulations/dcalign/data_fall18/ccdb_20220529_newt2d.sqlite
  [DB] --->  database variation   : default
  [DB] --->  database run number  : 11
  [DB] --->  database time stamp  : Tue Jul 05 22:39:55 CEST 2022
  [DB] --->  database connection  : success
  [DB LOAD] ---> loading data table : /geometry/dc/dc
  [DB LOAD] ---> number of columns  : 2
  [DB LOAD] ---> loading data table : /geometry/dc/region
  [DB LOAD] ---> number of columns  : 9
  [DB LOAD] ---> loading data table : /geometry/dc/superlayer
  [DB LOAD] ---> number of columns  : 8
  [DB LOAD] ---> loading data table : /geometry/dc/layer
  [DB LOAD] ---> number of columns  : 2
  [DB LOAD] ---> loading data table : /geometry/dc/alignment
  [DB LOAD] ---> number of columns  : 9
  [DB LOAD] ---> loading data table : /geometry/dc/ministagger
  [DB LOAD] ---> number of columns  : 4
  [DB LOAD] ---> loading data table : /geometry/dc/endplatesbow
  [DB LOAD] ---> number of columns  : 4
  [DB] --->  database disconnect  : success
  [ConstantsManager] --->  loading table for run = 11
  [DB] --->  open connection with : sqlite:////Users/devita/Work/clas12/simulations/dcalign/data_fall18/ccdb_20220529_newt2d.sqlite
  [DB] --->  database variation   : rgb_spring19_am_oc_i0
  [DB] --->  database run number  : 11
  [DB] --->  database time stamp  : Tue Jul 05 22:39:59 CEST 2022
  [DB] --->  database connection  : success
  ***** >>> adding : /geometry/dc/alignment / table = /geometry/dc/alignment
  [DB] --->  database disconnect  : success
  [RCDB] --->  open connection with : mysql://rcdb@clasdb.jlab.org/rcdb
  [RCDB] --->  database connection  : success
  [RCDB] --->  database disconnect  : success
  [ConstantsManager] --->  loading table for run = 11
  [DB] --->  open connection with : sqlite:////Users/devita/Work/clas12/simulations/dcalign/data_fall18/ccdb_20220529_newt2d.sqlite
  [DB] --->  database variation   : rga_fall2018
  [DB] --->  database run number  : 11
  [DB] --->  database time stamp  : Tue Jul 05 22:40:06 CEST 2022
  [DB] --->  database connection  : success
  ***** >>> adding : /geometry/dc/alignment / table = /geometry/dc/alignment
  [DB] --->  database disconnect  : success
  ```
  where the first connections will always be to variation default, to download the design geometry parameters. The following connections will be to the variations selected with the command line options.
* The pre-existing misalignments set with the ```-init``` command-line option. These are printed twice, the first in the local frame and the second in the CCDB-compliant format.
* The minuit fit results for each sector:
  ```
  Sector 1
  Chi2 and benchmark with constants from variation rga_fall2018:
  chi2 = 7336.928 NDF = 426
  [fit-benchmark] Time = 0.000 , Iterations = 0, Status = false, Chi2/NDF = 7336.928/426
  Current minuit results:
  iteration 0	
  chi2 = 1278.164 NDF = 426
  [fit-benchmark] Time = 0.277 , Iterations = 4797, Status = true, Chi2/NDF = 1278.164/426
     r1_x: -0.1083 ± 0.0059 (-0.0059 - 0.0059)
     r1_y: -0.0495 ± 0.0369 (-0.0370 - 0.0370)
     r1_z:  0.2277 ± 0.0182 (-0.0182 - 0.0182)
    r1_cx:  0.0000 ± 0.0000 ( 0.0000 - 0.0000)
    r1_cy:  0.0197 ± 0.0056 (-0.0056 - 0.0056)
    r1_cz:  0.0072 ± 0.0154 (-0.0155 - 0.0154)
     r2_x: -0.0172 ± 0.0063 (-0.0063 - 0.0063)
     r2_y:  0.0533 ± 0.0367 (-0.0367 - 0.0367)
     r2_z:  0.0075 ± 0.0151 (-0.0151 - 0.0151)
    r2_cx:  0.0000 ± 0.0000 ( 0.0000 - 0.0000)
    r2_cy:  0.0116 ± 0.0033 (-0.0033 - 0.0033)
    r2_cz:  0.0584 ± 0.0102 (-0.0102 - 0.0102)
     r3_x:  0.0363 ± 0.0094 (-0.0094 - 0.0094)
     r3_y:  0.0712 ± 0.0555 (-0.0556 - 0.0556)
     r3_z: -0.0494 ± 0.0212 (-0.0212 - 0.0212)
    r3_cx:  0.0000 ± 0.0000 ( 0.0000 - 0.0000)
    r3_cy: -0.0107 ± 0.0025 (-0.0025 - 0.0025)
    r3_cz:  0.0268 ± 0.0138 (-0.0138 - 0.0138)
  ```
  A good fit should have status set to true, indicating minuit converged.
* The fitted misalignment constants from the current analysis, the final constants obtained as the sum of this and previous iteration, and the pre-existing constants to compare to. The second of this table is the one to be loaded into the Sqlite file for testing or for proceeding with a subsequent iteration.


#### Loading alignment constants to Sqlite/CCDB
The final constants can be loaded to a new variation in the Sqlite file for testing purposes or to proceed with a new iteration. The constants should be loaded to CCDB only when really finalized and vetted.
To load them to Sqlite:
* Create a new variation using nominal variation chosen above as a parent to inherit the correct geometry for all other detectors. For example:
  ```
  ccdb -c sqlite:///path-to-sqlite-file mkvar variation_name -p nominal
  ```
  choosing a suitable name for the variation. Note that only strings containing letters, numbers, and/or _ will be allowed for the variation name.
* Create a text file with the constants from this analysis.
	* This can be done by simply copying the "Final alignment parameters in CCDB format" table that results from running the alignment program and pasting this into a txt file. 
* Load the constants:
  ```
  ccdb -c sqlite:///path-to-sqlite-file add /geometry/dc/alignment file-with-constants.txt -v variation_name
  ```
* Check the constants were loaded correctly:
  ```
  ccdb -c sqlite:///path-to-sqlite-file dump /geometry/dc/alignment -v variation_name
  ```  
The Sqlite file can then be used for a new iteration by repeating the all procedure, using the new variation as the nominal.

#### Performing a new iteration
Once the Sqlite is updated with the constants from a given iteration, a new iteration can be performed repeating the whole procedure, starting with generating new yaml files using the new variation created above as nominal.
When running the alignment code on the processed event files, include the command line option:
```
-init variation_name
```
with the variation created and filled with the previous iteration constants. In this way the final constants printed on the terminal will be the sum of the previous and current misalignments.
* Note: The ```-init``` option should be used also with the ```-analyze``` and ```-fit``` options in order for previous misalignments to be accounted for.

Usually, two iterations are sufficient for the alignment to converge. The metric to decide whther to stop or perform another iteration is given by multiple factors:
* The chi2 of the final fit doesn't improve significantly compared to the previous iteration;
* The residuals and vertex offset distributions corrected for the current iteration results (red histogram in the before/after analysis tab) are not significantly narrower than the nominal (black histogram in the same tab). 

#### Testing the results
To test the alignment results:
* Load the final alignment table in the Sqlite file as you would do to perform a new iteration and generate the yaml files. 
* Select a suitable field-on run to process.
* Create and submit ONE single workflow to process the selected data with the ```r0.yaml``` file.
* Analyze the cooking output with the script [```kinemtics.groovy```](https://github.com/JeffersonLab/clas12alignment/blob/dcDev4/dc/utilities/kinematics.groovy). Check the usage options with:
  ```
  run-groovy kinematics.groovy
  ``` 