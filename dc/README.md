# dc-alignment

This code implements the CLAS12 DC alignment procedure developed by T. Hayward and documented at clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf.

The algorithm is based on the assumption that the *real* DC geometry is close to the nominal and that the transformation between the two can be described as a linear combination of translations and rotations in x, y and z of each DC region. The size of these translations and rotations is determined studying how the tracking fit residuals and the z vertex distribution for straight tracks varies as a function of individual translations or rotations and finding the set that minimize the residuals and the difference between the z vertex distribution and the known target position.
Specifically:
* Straight electron tracks are reconstructed and histograms of fit residuals for each DC layer and of the z-vertex distribution are made in bins of sector, polar and azimuthal angles. These histograms are analyzed to extract the shifts from zero of the fit residuals and the shifts of the z-vertex from the known target position. Since straight track runs are usually on empty target, the z-vertex histograms show peaks corresponding to the target cell windows that can be fit and compared to the installation position.
* The same tracks are reconstructed applying a single translation or rotation. The size of these is chosen to be large enough to have a measureable effect on fit residuals and vertex distributions, while being small compared to the DC cell size. The values used so far are 0.2 cm for shifts and 0.2 deg for rotations. Translations are applied in the sector frame (y axis along the DC wires, z axis along the beamline) and rotations are applied in the tilted-sector coordinate frame (y axis along the DC wires and z axis perpendicular to the DC layers, i.e. at 25 deg from the beamline axis). The total number of shifts and rotation is 18, 3 translations and 3 rotations for each of the 3 regions. Note that rotations in x and z are ignored because they are not supported by the current tracking software.
* The derivatives of the fit residuals and z-vertex versus each translation and rotation are extracted comparing the fit residuals and z-vertex for that geometry to the nominal geometry. 
* The nominal-geometry residual and vertex shifts with respect to the desired positions are fit to a linear combinations of the derivatives to extract the translations and rotation sizes. This global fit is performed with Minuit, minimizing a chi2 defined as the sum of squares of the residual and vertex shift normalized to their uncertainties. 
* The fit parameters are printout in a format corresponding to the /geometry/dc/alignment CCDB table used by reconstruction. Here translations are defined in the CLAS12 frame while rotations are defined in the tilted-sector coordinate system.

### Prerequisites:
* Software:
  * A Linux or Mac computer
  * Java Development Kit 11 or newer
  * maven 
* Data:
  * Straight-track data (both solenoid and torus should be off) with electron tracks in the forward detector.
  * Reconstructed files from the data above, processed with nominal geometry (variation: default) and with individual translations or rotations in xyz for each of the DC regions. See clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf for the CCDB variations. 

### Build and run
Clone this repository and checkout the dcDev2 branch:
```  
  git clone https://github.com/JeffersonLab/clas12alignment
  cd clas12alignment
  git checkout dcDev2
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
          -analyze : analyze histogram files
```
  
### Usage 
The code supports two main usage options to either process the hipo files with the reconstructed events using the nominal geometry and translated/rotated geometry or analyze a pre-existing histogram file.
#### Process hipo event files
Check the command line options with:
```
./bin/dc-alignment -process

     Usage : dc-alignment -nominal [nominal geometry hipo file or directory]  [input1] [input2] ....

   Options :
  -display : display histograms (0/1) (default = 1)
      -fit : fit residuals (1) or use mean (0) (default = 1)
     -init : init global fit from previous constants (1) or from zero shifts (0) (default = 0)
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
   -sector : sector-dependent derivatives (1) or average (0) (default = 0)
    -stats : histogram stat option (default = )
    -theta : theta bin limits, e.g. "5:10:20:30" (default = 5:10:20)
-variation : database variation for constant test (default = )
  -verbose : global fit verbosity (1/0 = on/off) (default = 0)
```
The code will process the input files specified with the ```-nominal``` or ```-r[123]_[c][xyz]``` options, create and fill histograms according to the selected theta and phi bins, run the analysis, plot the results and printout the extracted alignment constants. All histograms will be saved to an histogram file named ``prefix_histo.hipo``, with ``prefix`` being the string specified with the ```-o``` option, or ``histo.hipo`` if the option is not used. 
By specifying ``-display 0``, the graphical window presenting the plotted results will not be opened.

The following is an example of how one would process directories of the input data hipo files, utilizing 12 geometry variations (9 translations and 3 rotations). In this example, three theta bins are used (0 to 8, 8 to 14, and 14 to 22 [degrees]) and two bins in phi are used (-30 to 0, and 0 to 30 [degrees]).
```
./bin/dc-alignment -process -nominal /path/to/noShift/ -r1_x /path/to/1_x_0p2cm -r1_y /path/to/r1_y_0p2cm -r1_z /path/to/1_z_0p2cm -r1_cy /path/to/r1_cy_0p2deg -r2_x /path/to/2_x_0p2cm -r2_y /path/to/2_y_0p2cm -r2_z /path/to/2_z_0p2cm -r2_cy /path/to/2_cy_0p2deg -r3_x /path/to/3_x_0p2cm -r3_y /path/to/3_y_0p2cm -r3_z /path/to/r3_z_0p2cm -r3_cy /path/to/r3_cy_0p2deg -theta "0:8:14:22" -phi "-30:0:30" -variation rga_fall2018 -fit 0 -o theta_0-8-14-22
```
#### Analyze a histogram file
Check the command line options with:
```
./bin/dc-alignment -analyze

     Usage : dc-alignment -input [input histogram file]  [input1] [input2] ....

   Options :
  -display : display histograms (0/1) (default = 1)
      -fit : fit residuals (1) or use mean (0) (default = 1)
     -init : init global fit from previous constants (1) or from zero shifts (0) (default = 0)
     -iter : number of global fit iterations (default = 1)
   -sector : sector-dependent derivatives (1) or average (0) (default = 0)
    -stats : set histogram stat option (default = )
-variation : database variation for constant test (default = )
  -verbose : global fit verbosity (1/0 = on/off) (default = 0)
```
The code will read the histograms from the specified file, analyze them, plot the results and printout the extracted alignment constants. All histograms will be saved to an histogram file named ``prefix_histo.hipo``, with ``prefix`` being the string specified with the ```-o``` option, or ``histo.hipo`` if the option is not used. 
By specifying ``-display 0``, the graphical window presenting the plotted results will not be opened.

### Input files
Hipo event files used with the ``-process`` option should contain straight tracks matched to HTCC and ECAL and contain the banks ``RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBasedTrkg::TBTracks,TimeBasedTrkg::TBHits``.
The tracks selection to identify electrons is performed by the ``getElectron()`` method in the ``Histo`` class, using parameters from the ``Constants`` class.

To reduce the data volume and speed up the processing, files for each geometry variation can be filtered with:
```
hipo-utils -reduce -ct "REC::Particle://beta>0[GT]0,REC::Cherenkov://nphe>2[GT]0,REC::Calorimeter://energy>0[GT]0,TimeBasedTrkg::TBTracks://
Vtx0_z>-20&&Vtx0_z<10[GT]0" -r "TimeBasedTrkg::TBHits://trkID>0" -b "RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBased
Trkg::TBTracks,TimeBasedTrkg::TBHits" -o outputfilename inputfiles
```
where the vertex, nphe and energy cut should be selected according to the experiment configuration (beam energy and target).

### Output
When the ``-process`` option is chosen, a file containing all histograms produced in the data processing is saved and can be re-analyzed with the ``-analyze`` option.
With both the ``-process`` and ``-analyze`` options, the extracted misalignment constants are printed out in a format consistent with the /geometry/dc/alignment CCDB table.

### Parameters
In addition to the parameters that can be selected from command line, the code uses parameters defined in the ``Constants`` class:
* the electron selection cuts,
* target parameters relevant for the vertex fits,
* global fit parameters initialization values and step size.
These can be modified according to the needs before compiling and running the code.

### Plots and results
If the ``-display`` option is set to 1 (default), a graphic window displaying histograms and relevant graphs is opened. 
![Screen Shot 2022-02-19 at 21 22 06](https://user-images.githubusercontent.com/7524926/154817793-a9ab8c07-5bab-4dd2-8699-f1888b54e17d.png)

#### Analysis tab
The tab displays a summary of the extracted residuals and vertex shifts and derivatives. It includes the following sub-tabs:
* nominal: graphs of the extracted residuals and vertex shifts for the nominal geometry. Each plot corresponds to a different sector and the color points to different polar angle bins; different symbols are used to display the phi bins. The vertex shifts are displayed as layer=0, in 10s of um. See screenshot of graph below.
![Plot_02-19-2022_10 20 52_PM](https://user-images.githubusercontent.com/7524926/154820094-0bca8488-a895-474d-b8e2-22e25f42e7a6.png)
* nominal vs. theta: same as above but with the y-axis defined as the angular bin number plus the layer number. The different colors correspond to the different DC superlayers and the black points show the vertex shifts. See screenshot of graph below.
![Plot_02-19-2022_10 21 21_PM](https://user-images.githubusercontent.com/7524926/154819746-af0ee5bc-3e22-41b1-a00f-50f6d20d84c7.png)
* corrected and corrected vs. theta: same as above but after applying the translations and rotations from the /geometry/dc/alignment table in the CCDB variation specified with the ``-variation`` option. (Example not shown.)
* r1_x, ...r3_cy: graphs of the fit residuals and vertex derivatives for the corresponding translation or rotation. Each graph corresponds to a different angular bin. Colors correspond to different sectors while the average is shown in black. An example is shown by the following graph.

![Plot_02-19-2022_10 22 13_PM](https://user-images.githubusercontent.com/7524926/154819842-7f8f4f72-ad4d-4d27-a0e3-ada02b65447a.png)
* Fitted and fitted vs. theta: same as nominal or corrected, but after aplying the combination of shifts and rotations resulting from the global fit. (Example not shown.)

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

