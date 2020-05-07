/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.viewer;

import javax.swing.JPanel;
import org.jlab.detector.view.DetectorPane2D;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

/**
 *
 * @author ziegler
 */
public class AnalysisMonitor implements IDataEventListener {    
    
    private final String           analysisName;
    private IndexedList<DataGroup> analysisData    = new IndexedList<DataGroup>(1);
    private DataGroup              analysisSummary = null;
    private JPanel                 analysisPanel   = null;
    private EmbeddedCanvasTabbed   analysisCanvas  = null;
    private DetectorPane2D         analysisView    = null;
    private int                    numberOfEvents;

    public AnalysisMonitor(String name){
        this.analysisName = name;
        this.analysisPanel  = new JPanel();
        this.analysisCanvas = new EmbeddedCanvasTabbed();
        this.analysisView   = new DetectorPane2D();
        this.numberOfEvents = 0;
    }

    
    public void analyze() {
        // analyze detector data at the end of data processing
    }

    public void createHistos() {
        // initialize canvas and create histograms
    }
    
    @Override
    public void dataEventAction(DataEvent event) {
        
        this.setNumberOfEvents(this.getNumberOfEvents()+1);
        if (event.getType() == DataEventType.EVENT_START) {
//            resetEventListener();
            processEvent(event);
	} else if (event.getType() == DataEventType.EVENT_SINGLE) {
            processEvent(event);
            plotEvent(event);
	} else if (event.getType() == DataEventType.EVENT_ACCUMULATE) {
            processEvent(event);
	} else if (event.getType() == DataEventType.EVENT_STOP) {
            analyze();
	}
    }

    public EmbeddedCanvasTabbed getAnalysisCanvas() {
        return analysisCanvas;
    }
    
    public IndexedList<DataGroup>  getDataGroup(){
        return analysisData;
    }

    public String getAnalysisName() {
        return analysisName;
    }
    
    public JPanel getAnalysisPanel() {
        return analysisPanel;
    }
    
    public DataGroup getAnalysisSummary() {
        return analysisSummary;
    }
    
    public DetectorPane2D getAnalysisView() {
        return analysisView;
    }
    
    public int getNumberOfEvents() {
        return numberOfEvents;
    }

    public void init() {
        // initialize monitoring application
    }
    
    public void processEvent(DataEvent event) {
        // process event
    }
    
    public void plotEvent(DataEvent event) {
        // process event
    }
    
    @Override
    public void resetEventListener() {
        
    }
    
    public void setAnalysisCanvas(EmbeddedCanvasTabbed canvas) {
        this.analysisCanvas = canvas;
    }
    
    public void setAnalysisSummary(DataGroup group) {
        this.analysisSummary = group;
    }
    
    public void setCanvasUpdate(int time) {
        this.analysisCanvas.getCanvas().initTimer(time);
    }
    
    public void setNumberOfEvents(int numberOfEvents) {
        this.numberOfEvents = numberOfEvents;
    }

    @Override
    public void timerUpdate() {
        
    }
 
    
}
