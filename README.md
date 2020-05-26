# FMT Analysis
Code used for the FMT detector residual analysis.

### Useful Information
Reconstruction:
* Currently, the FVT engine handles FMT data.
* The engine grabs the DC tracks and reconstructs them, updating them with the FMT cluster data.
* Reconstruction works in a similar fashion to the DC's:
    * Clusters are made from the hits via a Cluster Finding algorithm.
    * Crosses are constructed from this clusters by grouping them.
    * The track is updated with these crosses via a Kalman Filter algorithm.

Plotting Residuals:
* Residuals are the difference between the DC track and the FMT clusters in the y coordinate.
* Looking at the residuals gives us an idea of how to fix misalignments in the geometry.

### Running
Before running, you need to add the location of the input hipo file to be processed as an argument.
Also, set the CLAS12 dir as an option to the java VM like this: `-DCLAS12DIR=/path/to/coatjava_6.5.3` to let the program access the coatjava code.

The Main class of the program is `org.clas.test.Main`, and is where you should apply changes to draw plots and run analysis.
To run the different types of analysis, simply uncomment the one you need to run, compile and run.
This is by no means a clean way to use the program, but due to the way Netbeans works it is the quickest one by far.
The explanation for all the methods in the ResolutionAnalysis class are in the JavaDoc and as comments in the class itself, so it shouldn't be hard to get everything set up and running.
