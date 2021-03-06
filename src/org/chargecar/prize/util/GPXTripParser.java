package org.chargecar.prize.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Stack;
import java.io.*;
import org.xml.sax.*;

/**
 * DO NOT EDIT
 * 
 * A class to read a GPX file into a java class containing the lat, lon,
 * elevation and time information.
 * 
 * @author Alex Styler
 */
public class GPXTripParser extends org.xml.sax.helpers.DefaultHandler {
    private List<Calendar> rawTimes;
    private List<Double> rawLats;
    private List<Double> rawLons;
    private List<Double> rawEles;
    private Vehicle vehicle;
    private List<List<PointFeatures>> trips;
    private Stack<String> elementNames;
    private StringBuilder contentBuffer;
    private int points;
    
    public GPXTripParser() {
	clear();
    }
    
    public void clear() {
	elementNames = new Stack<String>();
	contentBuffer = new StringBuilder();
	trips = new ArrayList<List<PointFeatures>>();
	clearRawData();
	points = 0;
    }
    
    private void clearRawData() {
	rawTimes = new ArrayList<Calendar>();
	rawLats = new ArrayList<Double>();
	rawLons = new ArrayList<Double>();
	rawEles = new ArrayList<Double>();
    }
    
    public List<List<PointFeatures>> read(File gpxFile, Vehicle vehicle)
    throws IOException {
	clear();
	double accLimit = 4.9;
	double minTripDist = 500;
	this.vehicle = vehicle;
	FileInputStream in = new FileInputStream(gpxFile);
	InputSource source = new InputSource(in);
	XMLReader parser;
	try {
	    parser = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();// "org.apache.xerces.parsers.SAXParser");
	    parser.setContentHandler(this);
	    parser.parse(source);
	    
	} catch (SAXException e) {
	    System.err.println("Error reading file: "+gpxFile.getCanonicalPath());
	    e.printStackTrace();
	    throw new IOException();
	}
	in.close();
	
	for (int i = 0; i < trips.size();) {
	    if(trips.get(i) == null || trips.get(i).size() < 100){
		trips.remove(i);
		continue;
	    }
	    
	    double sumPlanarDist = 0.0;
	    double maxAccel = 0.0;
	    double acc;
	    for (PointFeatures p : trips.get(i)) {
		sumPlanarDist += p.getPlanarDist();
		acc = Math.abs(p.getAcceleration());
		if(acc > maxAccel){
		    maxAccel = acc;
		}
	    }
	    if (sumPlanarDist < minTripDist || maxAccel > accLimit){
		trips.remove(i);
	    } else {
		i++;
	    }
	}
	
	return trips;
    }
    
    private void processTrips() {
	if (rawTimes.isEmpty()) {
	    return;
	}
	// clean of duplicate readings
	removeDuplicates();
	
	List<Calendar> times = new ArrayList<Calendar>();
	List<Double> lats = new ArrayList<Double>();
	List<Double> lons = new ArrayList<Double>();
	List<Double> eles = new ArrayList<Double>();
	
	times.add(rawTimes.get(0));
	lats.add(rawLats.get(0));
	lons.add(rawLons.get(0));
	eles.add(rawEles.get(0));
	
	for (int i = 1; i < rawTimes.size(); i++) {
	    long msDiff = rawTimes.get(i).getTimeInMillis()
	    - rawTimes.get(i - 1).getTimeInMillis();
	    if (msDiff > 360000) {
		// if enough time has passed between points (360 seconds)
		// consider them disjoint trips
		if(times.size() > 1)
		    trips.add(TripBuilder2.calculateTrip(times, lats, lons, eles, vehicle));
	    
		times.clear();
		lats.clear();
		lons.clear();
		eles.clear();
	    }
	    
	    times.add(rawTimes.get(i));
	    lats.add(rawLats.get(i));
	    lons.add(rawLons.get(i));
	    eles.add(rawEles.get(i));
	}
	
	if (times.size() > 60) {
	    // get last trip
	    trips.add(TripBuilder2.calculateTrip(times, lats, lons, eles, vehicle));
	}
	
	clearRawData();
    }
    
    private void removeDuplicates() {
	for (int i = 1; i < rawTimes.size();) {
	    if (rawTimes.get(i).getTimeInMillis()
		    - rawTimes.get(i - 1).getTimeInMillis() < 500) {
		rawTimes.remove(i);
		rawLats.remove(i);
		rawLons.remove(i);
		rawEles.remove(i);
	    } else {
		i++;
	    }
	    
	}
    }
    
    /*
     * DefaultHandler::startElement() fires whenever an XML start tag is
     * encountered
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName,
	    Attributes attributes) throws SAXException {
	// the <trkpht> element has attributes which specify latitude and
	// longitude (it has child elements that specify the time and elevation)
	if (localName.compareToIgnoreCase("trkpt") == 0) {
	    rawLats.add(Double.parseDouble(attributes.getValue("lat")));
	    rawLons.add(Double.parseDouble(attributes.getValue("lon")));
	    points++;
	}
	// Clear content buffer
	contentBuffer.delete(0, contentBuffer.length());
	
	// Store name of current element in stack
	elementNames.push(qName);
    }
    
    /*
     * the DefaultHandler::characters() function fires 1 or more times for each
     * text node encountered
     */
    public void characters(char[] ch, int start, int length)
    throws SAXException {
	contentBuffer.append(String.copyValueOf(ch, start, length));
    }
    
    /*
     * the DefaultHandler::endElement() function fires for each end tag
     */
    public void endElement(String uri, String localName, String qName)
    throws SAXException {
	String currentElement = elementNames.pop();
	
	if (points > 0 && currentElement != null) {
	    if (currentElement.compareToIgnoreCase("ele") == 0) {
		rawEles.add(Double.parseDouble(contentBuffer.toString()));
	    } else if (currentElement.compareToIgnoreCase("time") == 0) {
		rawTimes.add(gmtStringToCalendar(contentBuffer.toString()));
	    } else if (currentElement.compareToIgnoreCase("gpx") == 0) {
		processTrips();
	    }
	}
    }
    
    private static Calendar gmtStringToCalendar(String dateTimeString) {
	// incoming format: 2010-02-25T22:44:57Z
	String dateString = dateTimeString.substring(0, dateTimeString
		.indexOf('T'));
	String[] dates = dateString.split("-");
	int year = Integer.parseInt(dates[0]);
	int month = Integer.parseInt(dates[1]) - 1;// 0 indexed
	int day = Integer.parseInt(dates[2]);
	// format: 22:44:57
	String timeString = dateTimeString.substring(dateTimeString
		.indexOf('T') + 1, dateTimeString.indexOf('Z'));
	String[] times = timeString.split(":");
	int hour = Integer.parseInt(times[0]);
	int minute = Integer.parseInt(times[1]);
	int second = Integer.parseInt(times[2]);
	
	Calendar calTime = Calendar.getInstance();
	calTime.setTimeInMillis(0);
	calTime.set(year, month, day, hour, minute, second);
	
	return calTime;
    }
}
