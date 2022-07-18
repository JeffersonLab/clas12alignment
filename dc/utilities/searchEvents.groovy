import org.jlab.io.hipo.*;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

if(args.length<2) {
    System.out.println("Usage:\\n\t run-groovy saveEventNumbers.groovy <hipo-file-directory> <run-and-event-numbers-file>");
    return;
}

// read event list
FileReader fileReader = new FileReader(args[0]);
BufferedReader bufferedReader = new BufferedReader(fileReader);

Map<Integer, ArrayList<Integer>> eventMap = new HashMap<>();
String line = null;
while ((line = bufferedReader.readLine()) != null) {
    String[] cols = line.split("\\s+");
    if(cols.length==2) {
        int run   = Integer.parseInt(cols[0].trim());
        int event = Integer.parseInt(cols[1].trim());
        if(!eventMap.containsKey(run)) {
            eventMap.put(run, new ArrayList<Integer>());
        }
        eventMap.get(run).add(event);
    } 
}
bufferedReader.close();

String inputpath = args[1];

HipoDataSync writer = new HipoDataSync();
writer.open("skim4.hipo");

for(Integer run : eventMap.keySet()) {
    System.out.println("Searching events for run " + run);
    
    ArrayList eventList = eventMap.get(run);
    Collections.sort(eventList);
    
    String dirname  = inputpath + String.format("/%06d",run);
    File file = new File(dirname);
    String[] files = file.list();
    Arrays.sort(files); 
    // build list of first events
    ArrayList<Integer> filesMinEvents = new ArrayList<>();
    for(int j=0; j<files.length; j++) {
        String filename = files[j];
        HipoDataSource reader = new HipoDataSource();
        reader.open(dirname + "/" + filename);
        while(reader.hasEvent()) {   
            DataEvent event = reader.getNextEvent();
            // getting event number
            if (event.hasBank("RUN::config")) {
                DataBank bank = event.getBank("RUN::config");
                if(bank.getInt("event",0)>0) {
                    filesMinEvents.add(bank.getInt("event",0));
                    break;
                }
            }
        }
    }
    
    // group events by file 
    Map<Integer, Integer> eventByFile = new HashMap();
    for(int ev : eventMap.get(run)) {
        int ifile = -1;
        for(int i=0; i<filesMinEvents.size()-1; i++) {
            if(ev>=filesMinEvents.get(i) && ev<filesMinEvents.get(i+1)) {
                ifile = i
                break;
            }
        }
        if(ifile==-1 && ev>=filesMinEvents.get(filesMinEvents.size()-1)) {
            ifile = filesMinEvents.size()-1;
        }
        if(ifile>=0) {
            if(!eventByFile.containsKey(ifile)) {
                eventByFile.put(ifile, new ArrayList<Integer>());
            }
            eventByFile.get(ifile).add(ev);
        }
    }
    // find the file
    int nfound = 0;
    for(int ifile : eventByFile.keySet()) {
        List<Integer> fileEvents = eventByFile.get(ifile);
        //now get the event
        HipoDataSource reader = new HipoDataSource();
        reader.open(dirname + "/" + files[ifile]);
        while(reader.hasEvent()) {   
            DataEvent event = reader.getNextEvent();
            // getting event number
            if (event.hasBank("RUN::config")) {
                DataBank bank = event.getBank("RUN::config");
                int ev = bank.getInt("event",0);
                if(fileEvents.contains(ev)) {
                    writer.writeEvent(event);
	            nfound++;
                    if(nfound%10000) System.out.println("Found event " + ev + "(" + nfound + "/" + eventMap.get(run).size() + ") in run " + run);
                }
            }
        }    
    }
    System.out.println("Found " + nfound + " events of " + eventMap.size());   
}

writer.close();
