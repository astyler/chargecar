package org.chargecar.algodev;

import java.util.ArrayList;
import java.util.List;

import org.chargecar.prize.battery.BatteryModel;
import org.chargecar.prize.policies.Policy;
import org.chargecar.prize.util.PointFeatures;
import org.chargecar.prize.util.PowerFlowException;
import org.chargecar.prize.util.PowerFlows;
import org.chargecar.prize.util.Trip;
import org.chargecar.prize.util.TripFeatures;

public class OmniscientPolicy implements Policy {
    List<Double> optimalBatteryDraw;
    public OmniscientPolicy(int lookAheadSeconds) {
	super();
	this.lookAheadSeconds = lookAheadSeconds;
    }
    int currentIndex;
    final int lookAheadSeconds;
    
    public void parseTrip(Trip t){
	List<PointFeatures> points = t.getPoints();
	optimalBatteryDraw = new ArrayList<Double>(points.size());
	List<Double> cumulativeSum = new ArrayList<Double>(points.size());
	List<Integer> timeStamps = new ArrayList<Integer>(points.size());
	List<Double> rates = new ArrayList<Double>(points.size());
	double sum = 0;
	int timesum = 0;
	
	for(PointFeatures pf : points){
	    sum += pf.getPowerDemand();
	    timesum += pf.getPeriodMS();
	    cumulativeSum.add(sum);
	    timeStamps.add(timesum);
	    rates.add(1000*sum/timesum);
	}
	
	
	for(int startInd = 0; startInd < rates.size(); startInd++){
	    double maxRate = Double.NEGATIVE_INFINITY;	    
	    for(int i = startInd;(i < startInd + lookAheadSeconds && i<rates.size());i++){
		if(rates.get(i) >= maxRate){
		    maxRate = rates.get(i);
		}
	    }
	 
	    rates.set(startInd, maxRate);

	    int timesub = timeStamps.get(startInd);
	    for(int i = startInd+1;i<rates.size();i++){
		cumulativeSum.set(i, cumulativeSum.get(i)-maxRate*timesub/1000);
		timeStamps.set(i, timeStamps.get(i)-timesub);
		rates.set(i,1000*cumulativeSum.get(i)/timeStamps.get(i));
	    }
	}
	optimalBatteryDraw = rates;

    }
    private BatteryModel modelCap;
    private BatteryModel modelBatt;
    private String name = "Omniscient Policy";
    
    public void beginTrip(TripFeatures tripFeatures, BatteryModel batteryClone,
	    BatteryModel capacitorClone) {
	modelCap = capacitorClone;
	modelBatt = batteryClone;
	currentIndex = 0;
    }
    
    @Override
    public PowerFlows calculatePowerFlows(PointFeatures pf) {
	double wattsDemanded = pf.getPowerDemand();
	int periodMS = pf.getPeriodMS();
	double minCapPower = modelCap.getMinPowerDrawable(periodMS);
	double maxCapPower = modelCap.getMaxPowerDrawable(periodMS);
	
	double capToMotorWatts = wattsDemanded > maxCapPower ? maxCapPower : wattsDemanded;
	capToMotorWatts = capToMotorWatts < minCapPower ? minCapPower : capToMotorWatts;
	double batteryToMotorWatts = wattsDemanded - capToMotorWatts;
	double batteryToCapWatts = optimalBatteryDraw.get(currentIndex) - batteryToMotorWatts;
	
	batteryToCapWatts = batteryToCapWatts  < 0 ? 0 : batteryToCapWatts;
	
	if (capToMotorWatts - batteryToCapWatts < minCapPower) {
		batteryToCapWatts = capToMotorWatts - minCapPower;
	    } else if(capToMotorWatts - batteryToCapWatts > maxCapPower){
		batteryToCapWatts = capToMotorWatts - maxCapPower;
	    }

	try {
	    modelCap.drawPower(capToMotorWatts - batteryToCapWatts, pf);
	    modelBatt.drawPower(batteryToMotorWatts + batteryToCapWatts, pf);
	} catch (PowerFlowException e) {
	}
	currentIndex ++;
	return new PowerFlows(batteryToMotorWatts, capToMotorWatts,
		batteryToCapWatts);
    }
    
    @Override
    public void endTrip() {
	// TODO Auto-generated method stub
	
    }
    
    @Override
    public String getName() {
	return name;
    }
    
    @Override
    public void loadState() {
	// TODO Auto-generated method stub
	
    }
    
}
