package org.chargecar.algodev;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.chargecar.algodev.knn.KnnTableTrainer;
import org.chargecar.algodev.policies.MDPPolyTrainer;
import org.chargecar.algodev.policies.MDPTrainer;
import org.chargecar.prize.battery.SimpleCapacitor;
import org.chargecar.prize.util.GPXTripParser;
import org.chargecar.prize.util.PointFeatures;
import org.chargecar.prize.util.Trip;
import org.chargecar.prize.util.TripFeatures;
import org.chargecar.prize.util.Vehicle;
import org.chargecar.prize.visualization.CSVWriter;

/**
 * DO NOT EDIT Runs the simulation of an electric car running over a commute
 * defined by GPX file from real world commutes. Uses a compound energy storage
 * Policy to decide whether to get/store power in either the capacitor or
 * battery inside the car.
 * 
 * Competitors need only modify UserPolicy with their algorithm.
 * 
 * @author Alex Styler
 * 
 */
public class SimulatorTrainer {
    static Vehicle civic = new Vehicle(1200, 1.988, 0.31, 0.015);
    static double systemVoltage = 120;
  //  static double capWhr = 50;
    /**
     * @param args
     *            A pathname to a GPX file or folder containing GPX files (will
     *            be recursively traversed)
     *            Alternate policies to test, either in a referenced JAR file or 
     *            within the project
     *        	  e.g. java Simulator "C:\testdata\may" org.chargecar.policies.SpeedPolicy
     *        	       java Simulator "C:\testdata" NaiveBufferPolicy SpeedPolicy
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
	if (args == null || args.length < 1) {
	    System.err.println("ERROR: No GPX directory path provided.");
	    System.exit(1);
	}
	
	String gpxFolder = args[0];
	String optFolder = args[1];
	//int capWhr = Integer.parseInt(args[2]);
	
	File folder = new File(gpxFolder);
	List<File> gpxFilesT = getGPXFiles(folder);
	List<File> gpxFiles = new ArrayList<File>(gpxFilesT.size());
	for(int i = gpxFilesT.size() - 1; i >= 0; i--){
	    gpxFiles.add(gpxFilesT.get(i));
	}
	
	System.out.println("Training on "+gpxFiles.size()+" GPX files.");
	//MDPPolyTrainer policy = new MDPPolyTrainer(optFolder, new SimpleCapacitor(capWhr, 0, systemVoltage), 4, 100);
	//MDPTrainer policy = new MDPTrainer(optFolder, new SimpleCapacitor(capWhr, 0, systemVoltage), 20);
	//KnnTableTrainer policy = new KnnTableTrainer(optFolder);
	int count = 0;
	for (File tripFile : gpxFiles) {
	    List<Trip> tripsToTest = parseTrips(tripFile);
	    for (Trip t : tripsToTest) {
		//policy.parseTrip(t);
		CSVWriter.writeTrip(optFolder+"/"+t.getFeatures().getFileName()+".csv", t);
		count++;
	    }
	}	
	//policy.finishTraining();
	System.out.println("Complete. Trips trained on: "+count);
    }    
    
    private static List<Trip> parseTrips(File gpxFile) throws IOException {
	List<Trip> trips = new ArrayList<Trip>();
	int i=0;
	GPXTripParser gpxparser = new GPXTripParser();
	for (List<PointFeatures> tripPoints : gpxparser.read(gpxFile, civic)) {
	    String driverName = gpxFile.getParentFile().getName();
	    String fileName = driverName+gpxFile.getName().substring(0, gpxFile.getName().lastIndexOf('.'))+"_"+i;
	    TripFeatures tf = new TripFeatures(driverName, fileName, civic, tripPoints
		    .get(0));
	    trips.add(new Trip(tf, tripPoints));
	    gpxparser.clear();
	    i++;
	}
	return trips;
    }
    
    static List<File> getGPXFiles(File gpxFolder) {
	List<File> gpxFiles = new ArrayList<File>();
	File[] files = gpxFolder.listFiles();
	for (File f : files) {
	    if (f.isDirectory()) {
		gpxFiles.addAll(getGPXFiles(f));
	    } else if (f.isFile()
		    && (f.getAbsolutePath().endsWith("gpx") || f
			    .getAbsolutePath().endsWith("GPX"))) {
		gpxFiles.add(f);
	    }
	}
	return gpxFiles;
    }
}
