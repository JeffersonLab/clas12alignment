# dc-alignment

This code implements the CLAS12 DC alignment procedure developed by T. Hayward and documented at clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf.

### Prerequisites:
* Software:
  * A Linux or Mac computer
  * Java Development Kit 11 or newer
  * maven 
* Data:
  * Straight-track data (both solenoid and torus should be off) with electron tracks in the forward detector.
  * Reconstructed files from the data above, processed with nominal geometry (variation: default) and with individual shifts or rotations in xyz for each of the DC regions. The latter amount to a total of 3 regions x (3 shifts + 3 rotations) = 18 sets. See clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf for the CCDB variations. Note that rotations in x and z can be skipped because
 they are not supported by the current tracking algorithm.

### Build and run
Clone this repository and checkout the dcDev2 branch:
```  
  git clone https://github.com/JeffersonLab/clas12alignment
  git check dcDev2
```
Go to the folder clas12alignment/dc/java/dc-alignment and compile with maven:
```
  cd clas12alignment/dc/java/dc-alignment
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
The code supports two main usage options to either process the hipo files with the reconstructed events using the nominal geometry and shifted/rotated geometry or analyze a pre-existing histogram file.
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
The code will process the input files specified with the ```-nominal``` or ```-r[123]_[c][xyz]``` options, create and fill histograms according to the selected theta and phi bins, run the analysis, plot the results and printout the extract alignment constants. All histograms will be saved to an histogram file named ``prefix_histo.hipo``, with ``prefix`` being the string specified with the ```-o``` option, or ``histo.hipo`` if the option is not used. 
By specifying ``-display 0``, the graphical window presenting the plotted results will not be opened.

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
The code will read the histograms from the specified file, analyze them, plot the results and printout the extract alignment constants. All histograms will be saved to an histogram file named ``prefix_histo.hipo``, with ``prefix`` being the string specified with the ```-o``` option, or ``histo.hipo`` if the option is not used. 
By specifying ``-display 0``, the graphical window presenting the plotted results will not be opened.

### Input files
Hipo event files used with the ``-process`` option should contain straight tracks matched to HTCC and ECAL and contain the banks ``RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBasedTrkg::TBTracks,TimeBasedTrkg::TBHits``
The tracks selection to identify electrons is performed by the ``getElectron()`` method in the ``Histo`` class, using parameters from the ``Constants`` class

To reduce the data volume and speed up the processing, files for each geometry variation can be filtered with:
```
hipo-utils -reduce -ct "REC::Particle://beta>0[GT]0,REC::Cherenkov://nphe>2[GT]0,REC::Calorimeter://energy>0[GT]0,TimeBasedTrkg::TBTracks://
Vtx0_z>-20&&Vtx0_z<10[GT]0" -r "TimeBasedTrkg::TBHits://trkID>0" -b "RUN::config,REC::Particle,REC::Cherenkov,REC::Calorimeter,REC::Track,TimeBased
Trkg::TBTracks,TimeBasedTrkg::TBHits" -o outputfilename inputfiles
```
where the vertex, nphe and energy cut should be selected according to the experiment configuration (beam energy and target).

### Output
When the ``-process`` option is chosen, a file containing all histograms produced in the data processing is saved and can be re-analyzed with the ``-analyze`` option.
With both the ``-process`` and ``-analyze`` options, the extracted misalignment constants are prit out in a format consistent with the /geometry/dc/alignment CCDB table.

### Parameters
In addition to the parameters that can be selected from command line, the code uses parameters defined in the ``Constants`` class:
* the electron selection cuts,
* target parameters relevant for the vertex fits,
* global fit parameters initialization values and step size.
These can be modified according to the needs before compiling and running the code.

### Plots and results
If the ``-display`` option is set to 1 (default), a graphic window displaying histograms and relevant graphs is opened. 
![Screen Shot 2022-02-19 at 21 22 06](https://user-images.githubusercontent.com/7524926/154817793-a9ab8c07-5bab-4dd2-8699-f1888b54e17d.png)

