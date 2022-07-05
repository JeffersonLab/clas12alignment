# dc-alignment

This code implements the CLAS12 DC alignment procedure developed by T. Hayward and documented at clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf.

The algorithm is based on the assumption that the *real* DC geometry is close to the nominal and that the transformation between the two can be described as a linear combination of translations and rotations in x, y and z of each DC region. The size of these translations and rotations is determined studying how the tracking fit residuals and the z vertex distribution for straight tracks varies as a function of individual translations or rotations and finding the set that minimize the residuals and the difference between the z vertex distribution and the known target position.
Specifically:
* Straight electron tracks are reconstructed and histograms of fit residuals for each DC layer and of the z-vertex distribution are made in bins of sector, polar and azimuthal angles. These histograms are analyzed to extract the shifts from zero of the fit residuals and the shifts of the z-vertex from the known target position. Since straight track runs are usually on empty target, the z-vertex histograms show peaks corresponding to the target cell windows that can be fit and compared to the installation position.
* The same tracks are reconstructed applying a single translation or rotation. The size of these is chosen to be large enough to have a measureable effect on fit residuals and vertex distributions, while being small compared to the DC cell size. The values used so far are 0.1-0.8 cm for shifts and 0.2 deg for rotations. Translations and rotations are applied in the tilted-sector coordinate frame (y axis along the DC wires and z axis perpendicular to the DC layers, i.e. at 25 deg from the beamline axis). Currently, the x rotation is not supported by tracking software. Therefore, the total number of shifts and rotation is 15, 3 translations and 2 rotations for each of the 3 regions. 
* The derivatives of the fit residuals and z-vertex versus each translation and rotation are extracted comparing the fit residuals and z-vertex for that geometry to the nominal geometry. 
* The nominal-geometry residual and vertex shifts with respect to the desired positions are fit to a linear combinations of the derivatives to extract the translations and rotation sizes. This global fit is performed with Minuit, minimizing a chi2 defined as the sum of squares of the residual and vertex shift normalized to their uncertainties. 
* The fit parameters are printout in a format corresponding to the /geometry/dc/alignment CCDB table used by reconstruction. Here translations are defined in the CLAS12 frame while rotations are defined in the tilted-sector coordinate system.

### Prerequisites
* Software:
  * A Linux or Mac computer
  * Java Development Kit 11 or newer
  * maven 
* Data:
  * Straight-track data (both solenoid and torus should be off) with electron tracks in the forward detector.

### Data processing
  * Process the straight-track data with the CLAS12 reconstruction code, using the nominal geometry (variation: default) and each of the individual translations or rotations in xyz for each of the DC regions.  This will results in up to 15 sets of reconstructed files that will be the input of the alignment code. 
    * The reconstruction configuration files or yaml files to produce these sets of data can be generated from the template file ``dcalign.yaml`` provided with the coatjava distribution (supported starting from coatjava 8.1.2), using the script [generateYamls.csh](https://github.com/JeffersonLab/clas12alignment/blob/dcDev3/dc/utilities/generateYamls.csh):
      ```
      ./generateYamls.csh <base-yaml-file> <base-variation>  <output-directory>
      ```
      where the base yaml file will be dcalign.yaml and the variation will be default. 
    * The generated yaml files will be in the folder specified when running the command. In the yaml files, the desired rotation or translation defines the value of the variable ```alignmentShift```, which has to be set for each of the DC reconstruction services. For example, the configuration        
      ```
      dcGeometryVariation: "default"
      alignmentShifts: "r1_cz:0.2"
      ```
      specifies that a 0.2 deg z rotation of region 1 will be applied on top of the geometry defined in the variation selected with the variablee ```dcGeometryVariation```.
    * Generate and run one cooking workflow for each yaml file to process the straigth track data (see the [CLAS12 chef documentation](https://clasweb.jlab.org/wiki/index.php/CLAS12_Chef_Documentation). Use as output directory name for the workflow the same name of the yaml file without the extention (e.g. the output of the data processed with r1_cz.yaml should be in a directory named r1_cz). Make sure to use a schema including the banks: ``RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBasedTrkg::TBTracks,TimeBasedTrkg::TBHits`` (tip: copy the dst schema directory from the coatjava distribution to a suitable location and add to it the two time-based tracking banks)
* Alignment input files:
  * To reduce the data volume and speed up the processing, files for each geometry variation can be filtered with:
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
Clone this repository and checkout the dcDev3 branch:
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
      -phi : phi bin limits, e.g. "-30:-15:0:15:30" (default = -30:0:30)
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

The following is an example of how one would process directories of the input data hipo files, utilizing 12 geometry variations (9 translations and 3 rotations). In this example, three theta bins are used (8 to 14, and 14 to 22 [degrees]) and two bins in phi are used (-30 to 0, and 0 to 30 [degrees]).
```
./bin/dc-alignment -process -nominal /path/to/noShift/ -r1_x /path/to/1_x_0p2cm -r1_y /path/to/r1_y_0p2cm -r1_z /path/to/1_z_0p2cm -r1_cy /path/to/r1_cy_0p2deg -r2_x /path/to/2_x_0p2cm -r2_y /path/to/2_y_0p2cm -r2_z /path/to/2_z_0p2cm -r2_cy /path/to/2_cy_0p2deg -r3_x /path/to/3_x_0p2cm -r3_y /path/to/3_y_0p2cm -r3_z /path/to/r3_z_0p2cm -r3_cy /path/to/r3_cy_0p2deg -theta "8:14:22" -phi "-30:0:30" -compare rga_fall2018 -fit 0 -o output_file_name
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
The code will read the histograms from the specified file, analyze them, compute the unit derivatives and nominal residuals and vertex values, perform themisalignment fit,plot the results and print the extracted alignment constants. 

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

Here is example of using the "fit" option to extract misalignments an already created histogram hipo file:
```
./bin/dc-alignment -fit -compare rga_fall2018 -input /path/to/histo.hipo
```

### Output
When the ``-process`` option is chosen, a file containing all histograms produced in the data processing is saved and can be re-analyzed with the ``-analyze`` or eith the ``-fit`` options.
With all options, the extracted misalignment constants are printed out in a format consistent with the /geometry/dc/alignment CCDB table.

### Parameters
In addition to the parameters that can be selected from command line, the code uses parameters defined in the ``Constants`` class:
* the electron selection cuts,
* target parameters relevant for the vertex fits,
* global fit parameters initialization values and step size.
These can be modified according to the needs before compiling and running the code.

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
* * shift magnitude: histogram of the residual and vertex shifts associated with the chosen translations and rotations. The top histograms shows the average change of the track residual for translations and rotations of region 1 (x:1-6), region 2 (x:7-12), and region 3 (x:13-18). Within each region range, the first 3 x values coresponds to the xyz translations and the second 3 x values to xyz rotations. The missing bins at x=4 are due to the x rotations not being used. The different colors correspond t the theta bins (black is all theta) and the different color shadows to the phi bins. The bottom histogram shows the same information for vertex shifts.
![Plot_07-05-2022_07 06 10_PM](https://user-images.githubusercontent.com/7524926/177380554-d91f0da9-29c7-412d-b429-796c22653b6f.png)
* r1_x, ...r3_cy: graphs of the fit residuals and vertex derivatives for the corresponding translation or rotation. Each graph corresponds to a different angular bin. Colors correspond to different sectors while the average is shown in black. An example is shown by the following graph.
![Plot_02-19-2022_10 22 13_PM](https://user-images.githubusercontent.com/7524926/154819842-7f8f4f72-ad4d-4d27-a0e3-ada02b65447a.png)
* corrected(with new parameters) and ocrrected(with new parameters) vs. theta: same as nominal or corrected, but after aplying the combination of shifts and rotations resulting from the global fit. (Example not shown.)
* before/after: shows the distribution of all tracking and vertex residuals corrected with the fitted misalignements 9red), with the misalignments specified iwth the ``-compare`` option (green) and with nominal geometry (black). In the example below, the nominal residuals are almost completely out of scale while the comparison between the new and old parameters shows the level of improvement.
![Plot_07-05-2022_07 05 04_PM](https://user-images.githubusercontent.com/7524926/177380199-77dec15e-95b8-41b5-8ed5-75dc42662ec5.png)
* misalignments: shows the comparison of the current analysis and minuit fit (red) and of the constants from the /geometry/dc/alignment table in the CCDB variation specified with the ``-compare`` option (black). If the ``-compare`` option is not specified, the black points will be at x=0. Each column correspomnds to a sector. Top and bottom corresponds to translations and rotations, respectively. The y coordinate identifies the DC region and translation/rotation coordinate. For example, points at y 0, 1 and 2 correspond to region 1, x, y and z translation or rotations.
![Plot_07-05-2022_07 05 21_PM](https://user-images.githubusercontent.com/7524926/177382223-27eb9b1b-44fa-4405-87de-f1f777bb5beb.png)
* internal-only: same as the previous tab but after removing ''global'' geometry transformation that cannot be constrained with the current straight track alignment. These so-called weak modes are overall contraction or expansions in size of the detector and rotations of whole sector. The displayed parameters are corrected by normalizing the region1 position to the nominal one.
![Plot_07-05-2022_07 05 37_PM](https://user-images.githubusercontent.com/7524926/177382868-88ae1ed1-d3b7-4245-9764-4ff90fb9ee65.png)




#### Electron
The tab displays the relevant distributions for the selected electron tracks.

#### Nominal
This tab displays histograms of the z-vertex distributions, fit and time residuals for each angular bin and sector.
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

