/**
 * Mars Simulation Project
 * Botanist.java
 * @version 3.07 2014-12-06
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.NaturalAttributeType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.AreologyStudyFieldMission;
import org.mars_sim.msp.core.person.ai.mission.BiologyStudyFieldMission;
import org.mars_sim.msp.core.person.ai.mission.Exploration;
import org.mars_sim.msp.core.person.ai.task.AssistScientificStudyResearcher;
import org.mars_sim.msp.core.person.ai.task.CompileScientificStudyResults;
import org.mars_sim.msp.core.person.ai.task.InviteStudyCollaborator;
import org.mars_sim.msp.core.person.ai.task.PeerReviewStudyPaper;
import org.mars_sim.msp.core.person.ai.task.PerformLaboratoryExperiment;
import org.mars_sim.msp.core.person.ai.task.PerformLaboratoryResearch;
import org.mars_sim.msp.core.person.ai.task.ProduceFood;
import org.mars_sim.msp.core.person.ai.task.ProposeScientificStudy;
import org.mars_sim.msp.core.person.ai.task.ResearchScientificStudy;
import org.mars_sim.msp.core.person.ai.task.RespondToStudyInvitation;
import org.mars_sim.msp.core.person.ai.task.StudyFieldSamples;
import org.mars_sim.msp.core.person.ai.task.TendGreenhouse;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.structure.building.function.farming.Farming;

/**
 * The Botanist class represents a job for a botanist.
 */
public class Botanist
extends Job
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	//private static Logger logger = Logger.getLogger(Botanist.class.getName());

	/**
	 * Constructor.
	 */
	public Botanist() {
		// Use Job constructor
		super(Botanist.class);

		// Add botany-related tasks.
		jobTasks.add(PerformLaboratoryExperiment.class);
		jobTasks.add(ProduceFood.class);
		jobTasks.add(StudyFieldSamples.class);
		jobTasks.add(TendGreenhouse.class);

		// Research related tasks
		jobTasks.add(AssistScientificStudyResearcher.class);
		jobTasks.add(CompileScientificStudyResults.class);
		jobTasks.add(InviteStudyCollaborator.class);
		jobTasks.add(PeerReviewStudyPaper.class);
		jobTasks.add(PerformLaboratoryExperiment.class);
		jobTasks.add(PerformLaboratoryResearch.class);
		jobTasks.add(ProposeScientificStudy.class);
		jobTasks.add(ResearchScientificStudy.class);
		jobTasks.add(RespondToStudyInvitation.class);

		// Add side tasks
		// None

		// Add botanist-related missions.
		jobMissionJoins.add(AreologyStudyFieldMission.class);
		
		jobMissionStarts.add(BiologyStudyFieldMission.class);
		jobMissionJoins.add(BiologyStudyFieldMission.class);
		
		jobMissionJoins.add(Exploration.class);
		
//		jobMissionStarts.add(TravelToSettlement.class);
//		jobMissionJoins.add(TravelToSettlement.class);
//		
//		jobMissionStarts.add(RescueSalvageVehicle.class);
//		jobMissionJoins.add(RescueSalvageVehicle.class);
//		
//		jobMissionJoins.add(BuildingConstructionMission.class);
//		jobMissionJoins.add(BuildingSalvageMission.class);
//		
//		jobMissionStarts.add(EmergencySupplyMission.class);
//		jobMissionJoins.add(EmergencySupplyMission.class);
	}

	/**
	 * Gets a person's capability to perform this job.
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {

		double result = 0D;

		int botanySkill = person.getMind().getSkillManager().getSkillLevel(SkillType.BOTANY);
		result = botanySkill;

		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);
//		result += result * ((academicAptitude - 50D) / 100D);
		int experienceAptitude = attributes.getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
		result += result * ((academicAptitude + experienceAptitude - 100) / 100D);
		
//		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
//		result+= result * ((averageAptitude - 50D) / 100D);

		if (person.getPhysicalCondition().hasSeriousMedicalProblems()) 
			result = result/2D;

//		System.out.println(person + " botanist : " + Math.round(result*100.0)/100.0);
		
		return result;
	}


	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {
		double result = 0D;

		int pop = settlement.getNumCitizens();
		
		// Add (labspace * tech level) / 2 for all labs with botany specialties.
		List<Building> laboratoryBuildings = settlement.getBuildingManager().getBuildings(FunctionType.RESEARCH);
		Iterator<Building> i = laboratoryBuildings.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Research lab = building.getResearch();
			if (lab.hasSpecialty(ScienceType.BOTANY)) {
				result += (double) (lab.getResearcherNum() * lab.getTechnologyLevel()) / 6D;
			}
		}

		// Add (growing area in greenhouses) / 25
		List<Building> greenhouseBuildings = settlement.getBuildingManager().getBuildings(FunctionType.FARMING);
		Iterator<Building> j = greenhouseBuildings.iterator();
		while (j.hasNext()) {
			Building building = j.next();
			Farming farm = building.getFarming();
			result += (farm.getGrowingArea() / 120D);
		}

		// Multiply by food value at settlement.
		//Good foodGood = GoodsUtil.getResourceGood(AmountResource.findAmountResource(LifeSupport.FOOD));
		//double foodValue = settlement.getGoodsManager().getGoodValuePerItem(foodGood);
		//result *= foodValue;
		//System.out.println("getSettlementNeed() : result is " + result);

		result += pop/6D;
		return result;
	}


}