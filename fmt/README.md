# FMT Analysis
Code used for the FMT detector alignment using residual analysis. A residual is the distance between an FMT cluster of hits and a DC track. In case the README and/or the usage of the program is not clear enough, a short presentation with details on how to run is included in this directory, named `usage_help.pdf`.

## Alignment algorithm
The position of each FMT layer is adjusted by minimizing the residuals between the reconstructed FMT clusters and the DC track trajectory point. The values of the misalignment parameters are identified by performing subsequent scans of z translations, z rotations, xy translations and xy rotations, fitting the residuals for each set of misalignments and selecting the values for which the mean or sigma of the fit are minimal. Multiple iterations can be performed to account for correlations between the parameters. Because of the 1D strip readout of the FMT layers, while z misalignment can be optmized for each layer independently, xy misalignments can be studied only if at least two layers are simultaneously displaced or rotated. For this reason, z misalignments are defined per layer, while the same xy misalignments are applied to the whole detector.

## Prerequisites
* Software:
  * A Linux or Mac computer
  * Java Development Kit 11 or newer
  * A coatjava installation
* Data:
  * Beam data with electron tracks in the forward detector and the following banks: ```RUN::config, REC::Event, 
REC::Particle, REC::Track, REC::Trajectory, FMT::Hits, FMT::Clusters, FMT::Tracks```
  * A recent Sqlite snapshot of CCDB (see https://clasweb.jlab.org/clas12offline/sqlite/ccdb/)

## Setup
Some setting up is required to run the program. The `run.sh` file requires some tuning, which includes
* Name of the torus and solenoid maps to be used.
* Path to the coatjava installation.
After this initial setup is done, simply run the script without giving it any parameters to get the programs' usage and continue from there.

After successfully running, a plot of shifts versus sigma (for dZ or rZ alignment) or mean (for dXY and rXY alignment) will be shown. The best shift is the one with the mean and sigma closest to 0.

Note that per-layer XY alignment of FMT is impossible with the current conditions, so the mean of the three layers distribution is shown.

**NOTE**. To change number of FMT layers, change the `FMTLAYERS` variable in `src/.../test/Constants.java`. Default is 3.

## Usage
```
Usage: alignment <file> [-n --nevents] [-v --var] [-i --inter]
                        [-s --swim] [-c --cutsinfo] [-V --variation]
                        [-p -plot]
                        [-x --dx] [-y --dy] [-z --dz]
                        [-X --rx] [-Y --ry] [-Z --rz]
  * file      : hipo input file.
  * nevents   : number of events to run. If unspecified, runs all events in
                input file.
  * var       : variable to be aligned. Can be dXY, dZ, rXY, or rZ.
  * inter (2) : [0] range between nominal position and position to be tested.
                [1] interval for each tested value between <nominal - range>
                    and <nominal + range>.
  * swim  (3) : Setup for the Swim class. If unspecified, uses default from
                    RG-F data (-0.75, -1.0, 3.0).
                [0] Solenoid magnet scale.
                [1] Torus magnet scale.
                [2] Solenoid magnet shift.
  * cutsinfo  : int describing how much info on the cuts should be printed. 0
                is no info, 1 is minimal, 2 is detailed. Default is 1.
  * variation : CCDB variation to be used. Default is ``rgf_spring2020''.
  * plot      : int describing if plots are to be shown. 1 means show them.
                Plots are always saved in the ``histograms.hipo'' file.
  * dx    (3) : x shift for each FMT layer.
  * dy    (3) : y shift for each FMT layer.
  * dz    (3) : z shift for each FMT layer.
  * rx    (3) : x rotation for each FMT layer.
  * ry    (3) : y rotation for each FMT layer.
  * rz    (3) : z rotation for each FMT layer.

For example, if var == dZ, inter == 0.2 0.1, and dz == 0.5, then the values
tested for z are:

        (0.3, 0.4, 0.5, 0.6, 0.7).

If a position or rotation is not specified, it is assumed to be 0 for all FMT
layers. If no argument is specified, a plot showing the residuals is shown.

NOTE. All measurements are in cm, while the ccdb works in mm.
```

## Useful Information
### Reconstruction:
* Currently, the FMT engine handles FMT data.
* The engine grabs the DC tracks and reconstructs them, updating them with the FMT cluster data.
* Reconstruction works in a similar fashion to the DC's:
    * Clusters are made from the hits via a simple Cluster Finding algorithm.
    * Crosses are constructed by grouping clusters from the six different FMT layers.
    * The DC track is updated with these crosses via a Kalman Filter algorithm.

**NOTE**. Crosses are not implemented for runs with only three FMT layers.

### Plotting Residuals:
* Residuals are the difference between the DC track and the FMT clusters in y in the FMT layer's local coordinate system.
* Looking at the residuals gives us an idea of how to fix misalignments in the geometry.

### Comparing results:
* For *dZ* and *rZ* alignment, it is ideal to use the sigma of the residuals distribution, fixing as much as sigmaError allows.
* For *dXY* and *rXY* alignment, the mean of the residuals distribution can be used, fixing as much as sigma allows.

### Example of alignment results
See https://logbooks.jlab.org/entry/3947235. The validation plots can be obtained running the script 
```fmtVertex.groovy``` on data reprocessed with the alignment constants.
