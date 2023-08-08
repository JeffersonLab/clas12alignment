import org.jlab.io.hipo.*;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

if(args.length<2) {
    System.out.println("Usage:\\n\t run-groovy saveEventNumbers.groovy <hipo-file-directory> <run-and-event-numbers-file>");
    return;
}


// get list of files
String inputpath = args[0];
File file = new File(inputpath);
String[] files = file.list();
Arrays.sort(files); 


// open text file
FileWriter fileWriter = new FileWriter(args[1]);
BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

// loop over files
for(int j=0; j<files.length; j++) {
    String filename = files[j];
    if(files[j].endsWith("hipo")) {
        HipoDataSource reader = new HipoDataSource();
        reader.open(inputpath + "/" + filename);
        while(reader.hasEvent()) {   
            DataEvent event = reader.getNextEvent();
            // getting event number
            if (event.hasBank("RUN::config")) {
                DataBank bank = event.getBank("RUN::config");
                if(bank.getInt("event",0)>0) {
                    bufferedWriter.write(bank.getInt("run", 0) + " " + bank.getInt("event",0) + "\n");
                }
            }
        }
    }
}
bufferedWriter.close();


