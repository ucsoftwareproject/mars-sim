/**
 * Mars Simulation Project
 * Gardenbot.java
 * @version 3.07 2015-02-02
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;

import org.mars_sim.msp.core.person.Robot;
import org.mars_sim.msp.core.person.ai.SkillType;

import org.mars_sim.msp.core.person.ai.task.TendGreenhouse;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Farming;
import org.mars_sim.msp.core.structure.building.function.Research;

/** 
 * The Gardenbot class represents a job for a gardenbot.
 */
public class Gardenbot
extends RobotJob
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	//private static Logger logger = Logger.getLogger(Botanist.class.getName());

	/**
	 * Constructor.
	 */
	public Gardenbot() {
		// Use Job constructor
		super(Gardenbot.class);

		jobTasks.add(TendGreenhouse.class);

	}

	/**
	 * Gets a robot's capability to perform this job.
	 * @param robot the robot to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Robot robot) {

		double result = 0D;

		int botanySkill = robot.getMind().getSkillManager().getSkillLevel(SkillType.BOTANY);
		result = botanySkill;

		NaturalAttributeManager attributes = robot.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttribute.ACADEMIC_APTITUDE);
		int experienceAptitude = attributes.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
		result+= result * ((averageAptitude - 50D) / 100D);

		if (robot.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;

		return result;
	}
	
	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {
		double result = 0D;

		// Add (labspace * tech level) / 2 for all labs with botany specialties.
		List<Building> laboratoryBuildings = settlement.getBuildingManager().getBuildings(BuildingFunction.RESEARCH);
		Iterator<Building> i = laboratoryBuildings.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Research lab = (Research) building.getFunction(BuildingFunction.RESEARCH);
			if (lab.hasSpecialty(ScienceType.BOTANY)) {
				result += (double) (lab.getResearcherNum() * lab.getTechnologyLevel()) / 2D;
			}
		}

		// Add (growing area in greenhouses) / 25
		List<Building> greenhouseBuildings = settlement.getBuildingManager().getBuildings(BuildingFunction.FARMING);
		Iterator<Building> j = greenhouseBuildings.iterator();
		while (j.hasNext()) {
			Building building = j.next();
			Farming farm = (Farming) building.getFunction(BuildingFunction.FARMING);
			result += (farm.getGrowingArea() / 25D);
		}

		return result;	
	}

}