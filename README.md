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
* Residuals are the difference between the DC track and the FMT clusters.
* Looking at the residuals gives us an idea of how to fix misalignments in the geometry.
