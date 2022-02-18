# dc-alignment

This code implements the CLAS12 DC alignment procedure developed by T. Hayward and documented at clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf.

### Prerequisites:
* Straight-track data (both solenoid and torus should be off) with electron tracks in the forward detector.
* Reconstructed files from the data above, processed with nominal geometry (variation: default) and with individual shifts or rotations in xyz for each of the DC regions. The latter amount to a total of 3 regions x (3 shifts + 3 rotations) = 18 sets. See clas12alignment/dc/original_scripts_and_docs/CLAS12___DC_Alignment_Github_Tutorial.pdf for the CCDB variations. Note that rotations in x and z can be skipped because
 they are not supported by the current tracking algorithm.
 
### Usage 
 
### Input files

### Output files
