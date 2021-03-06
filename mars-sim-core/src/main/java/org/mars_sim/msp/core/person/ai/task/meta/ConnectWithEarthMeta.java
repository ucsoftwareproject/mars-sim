/**
 * Mars Simulation Project
 * ConnectWithEarthMeta.java
 * @version 3.08 2015-06-28
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.io.Serializable;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.RoleType;
import org.mars_sim.msp.core.person.ai.task.ConnectWithEarth;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * Meta task for the ConnectWithEarth task.
 */
public class ConnectWithEarthMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.connectWithEarth"); //$NON-NLS-1$

    public RoleType roleType;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new ConnectWithEarth(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (!person.getPreference().isTaskDue(this) && person.isInside()) {
        		
            // Probability affected by the person's stress and fatigue.
            PhysicalCondition condition = person.getPhysicalCondition();
            double fatigue = condition.getFatigue();
            double stress = condition.getStress();
            double hunger = condition.getHunger();
            
            if (fatigue > 1000 || hunger > 500)
            	return 0;
            
            double pref = person.getPreference().getPreferenceScore(this);
            
         	result = pref * 5D;
         	
            if (pref > 0) {
             	if (stress > 45D)
             		result*=1.5;
             	else if (stress > 65D)
             		result*=2D;
             	else if (stress > 85D)
             		result*=3D;
             	else
             		result*=4D;
            }

            // Get an available office space.
            Building building = ConnectWithEarth.getAvailableBuilding(person);

            if (building != null) {
            	result = 10D;
            	// A comm facility has terminal equipment that provides communication access with Earth
            	// It is necessary
                result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, building);
                result *= TaskProbabilityUtil.getRelationshipModifier(person, building);


            }
            else {
            //if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {            	
                if (result > 0)
                	result *= RandomUtil.getRandomDouble(2); // more likely than not if on a vehicle
            }   	
            

            // 2015-06-07 Added Preference modifier
            if (result > 0D) {
                result = result + result * person.getPreference().getPreferenceScore(this)/2D;
            }
         
	        if (result < 0) result = 0;

        }
            

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getProbability(Robot robot) {
		// TODO Auto-generated method stub
		return 0;
	}
}