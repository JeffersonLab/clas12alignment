# FMT Analysis
Code used for the FMT detector residuals analysis.

### Useful Information
###### Reconstruction:
* Currently, the FVT engine handles FMT data.
* The engine grabs the DC tracks and reconstructs them, updating them with the FMT cluster data.
* Reconstruction works in a similar fashion to the DC's:
    * Clusters are made from the hits via a simple Cluster Finding algorithm.
    * Crosses are constructed by grouping clusters from the three different FMT layers.
        * **NOTE: this will eventually be updated to the planned six FMT layers when they're
          installed.**
    * The DC track is updated with these crosses via a Kalman Filter algorithm.

###### Plotting Residuals:
* Residuals are the difference between the DC track and the FMT clusters in y in the FMT layer's
  local coordinate system.
* Looking at the residuals gives us an idea of how to fix misalignments in the geometry.

### Doing Alignment
###### General setup:
* Before running, you need to add the location of the input hipo file to be processed as an
  argument.
* Also, set the CLAS12 dir as an option to the java VM like this:
  `-DCLAS12DIR=/path/to/coatjava` to let the program access the coatjava code.
    * **NOTE: Coatjava 6.5.8 is required for the program to run properly!**

###### Running the code:
* The Main class of the program is `org.clas.test.Main`, and is where you should apply changes to
  draw plots and run analysis.
* To run different types of analysis or plot different variables, edit the configuration variables
  in the Main class.
    * While this is not the ideal way to run, it allows for fast testing without changing the
      command used to execute the code.
* The explanation for all the methods are in the JavaDoc and as comments in the classes themselves.
  We hope it isn't hard to get everything set up and running, but if you experience any issues don't
  be shy to raise an issue!

###### Comparing results:
* To compare the mean or sigma of different shifts, a jupyter notebook file is provided in
  `/jnotebook_plots/mean_sigma_vs_shifts.ipynb`.
* The file should be easy to understand and run, it simply provides two functions to do 1D scatter
  plots and 2D heatmaps to see where the minimum of the distributions lie.

**NOTE: Due to the hasty implementation, it is a bit awkward to read x & y and pitch & yaw alignment
results. This should be fixed at some point in time, but it's not an urgent task.**
