import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.data.Schema;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.hipo4.io.HipoWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

String eventListFile = null;
String inputPath = null
boolean merge = false;


if(args.length<2) {
    System.out.println("Usage:\\n\t run-groovy searchEvents.groovy <run-and-event-numbers-file> <hipo-file-directory> [merge-runs(true/false)]");
    return;
}
else {
    eventListFile = args[0];
    inputPath     = args[1];
    if(args.length>2) {
        if(args[2].equals("true")) merge = true;
    }
}

// read event list
FileReader fileReader = new FileReader(eventListFile);
BufferedReader bufferedReader = new BufferedReader(fileReader);

Map<Integer, Map<Integer, Boolean>> eventMap = new LinkedHashMap<>();
String line = null;
while ((line = bufferedReader.readLine()) != null) {
    String[] cols = line.split("\\s+");
    if(cols.length==2) {
        int run   = Integer.parseInt(cols[0].trim());
        int event = Integer.parseInt(cols[1].trim());
        if(!eventMap.containsKey(run)) {
            eventMap.put(run, new HashMap<>());
        }
        eventMap.get(run).put(event, false);
    } 
}
bufferedReader.close();


HipoWriter writer = null;
String outFile = null;

for(Integer run : eventMap.keySet()) {    
    Map<Integer, Boolean> eventList = eventMap.get(run);
    
    System.out.println("Searching " + eventList.size() + " events for run " + run);
    if(eventList.size()==0) continue;
    
    String dirname  = inputPath + String.format("/%06d",run);
    File file = new File(dirname);
    String[] files = file.list();
    Arrays.sort(files); 
    System.out.printf("File list for run %d: \n%s\n\n", run, Arrays.toString(files));
 
    // find the file
    int nfound = 0;
    for(int ifile=0; ifile<files.length; ifile++) {
        //now get the event
        HipoReader reader = new HipoReader();
        reader.setTags(0);
        reader.open(dirname + "/" + files[ifile]);
        
        if(writer==null) {
            writer = reader.createWriter();
            writer.open("skim4.hipo")
        }
        
        Schema runConfig = reader.getSchemaFactory().getSchema("RUN::config");

        Event event = new Event();
        while (reader.hasNext()) {
            
            reader.nextEvent(event);
            
            Bank bank = new Bank(runConfig);
            event.read(bank);

            // getting event number
            if (bank.getRows()>0) {

                int ev = bank.getInt("event",0);
                long ts = bank.getLong("timestamp",0);
                if(eventList.containsKey(ev) && ts>0) {
                    if(eventList.get(ev)) System.out.println("duplicates!!!!!" +ev);
                    eventList.replace(ev, true);
                    writer.addEvent(event);
	            nfound++;
                    if(nfound%10000==0) System.out.println("Found event " + ev + "(" + nfound + "/" + eventList.size() + ") in run " + run);
                }
            }
        }    
    }
    System.out.println("Found " + nfound + " events of " + eventList.size());   
}

if(writer!=null) writer.close();
