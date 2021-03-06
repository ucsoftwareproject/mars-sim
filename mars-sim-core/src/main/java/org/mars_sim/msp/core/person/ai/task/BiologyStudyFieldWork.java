/**
 * Mars Simulation Project
 * BiologyStudyFieldWork.java
 * @version 3.1.0 2017-05-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.NaturalAttributeType;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.MissionMember;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A task for the EVA operation of performing biology field work at a research site
 * for a scientific study.
 */
public class BiologyStudyFieldWork
extends EVAOperation
implements Serializable {

    /** default serial id.*/
    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(BiologyStudyFieldWork.class.getName());

	private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1,
			 logger.getName().length());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.biologyFieldWork"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase FIELD_WORK = new TaskPhase(Msg.getString(
            "Task.phase.fieldWork.biology")); //$NON-NLS-1$

    // Data members
    private Person leadResearcher;
    private ScientificStudy study;
    private Rover rover;

    /**
     * Constructor.
     * @param person the person performing the task.
     * @param leadResearcher the researcher leading the field work.
     * @param study the scientific study the field work is for.
     * @param rover the rover
     */
    public BiologyStudyFieldWork(Person person, Person leadResearcher, ScientificStudy study,
            Rover rover) {

        // Use EVAOperation parent constructor.
        super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D);

        // Initialize data members.
        this.leadResearcher = leadResearcher;
        this.study = study;
        this.rover = rover;

        // Determine location for field work.
        Point2D fieldWorkLoc = determineFieldWorkLocation();
        setOutsideSiteLocation(fieldWorkLoc.getX(), fieldWorkLoc.getY());

        // Add task phases
        addPhase(FIELD_WORK);
    }

    /**
     * Determine location for field work.
     * @return field work X and Y location outside rover.
     */
    private Point2D determineFieldWorkLocation() {

        Point2D newLocation = null;
        boolean goodLocation = false;
        for (int x = 0; (x < 5) && !goodLocation; x++) {
            for (int y = 0; (y < 10) && !goodLocation; y++) {

                double distance = RandomUtil.getRandomDouble(100D) + (x * 100D) + 50D;
                double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
                double newXLoc = rover.getXLocation() - (distance * Math.sin(radianDirection));
                double newYLoc = rover.getYLocation() + (distance * Math.cos(radianDirection));
                Point2D boundedLocalPoint = new Point2D.Double(newXLoc, newYLoc);

                newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(),
                        boundedLocalPoint.getY(), rover);
                goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(),
                        person.getCoordinates());
            }
        }

        return newLocation;
    }

    /**
     * Checks if a person can research a site.
     * @param member the member.
     * @param rover the rover
     * @return true if person can research a site.
     */
    public static boolean canResearchSite(MissionMember member, Rover rover) {

        if (member instanceof Person) {
            Person person = (Person) member;

            // Check if person can exit the rover.
            if(!ExitAirlock.canExitAirlock(person, rover.getAirlock()))
            	return false;
            
            if (isGettingDark(person)) {
    			LogConsolidated.log(Level.FINE, 5000, sourceName,
    					"[" + person.getLocationTag().getLocale() + "] " + person.getName() + " ended "
    					+ person.getTaskDescription() + " : too dark to continue with the EVA.");
    			return false;
    		}

            // Check if person's medical condition will not allow task.
            if (person.getPerformanceRating() < .5D)
            	return false;
        }

        return true;
    }

    @Override
    protected TaskPhase getOutsideSitePhase() {
        return FIELD_WORK;
    }

    @Override
    protected double performMappedPhase(double time) {

        time = super.performMappedPhase(time);

        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (FIELD_WORK.equals(getPhase())) {
            return fieldWorkPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Perform the field work phase of the task.
     * @param time the time available (millisols).
     * @return remaining time after performing phase (millisols).
     * @throws Exception if error performing phase.
     */
    private double fieldWorkPhase(double time) {

        // Check for an accident during the EVA operation.
        checkForAccident(time);

        // 2015-05-29 Check for radiation exposure during the EVA operation.
        if (isRadiationDetected(time)){
            setPhase(WALK_BACK_INSIDE);
            return time;
        }

        // Check if site duration has ended or there is reason to cut the field
        // work phase short and return to the rover.
        if (shouldEndEVAOperation()) {
            setPhase(WALK_BACK_INSIDE);
            return time;
        }

        if (addTimeOnSite(time)) {
    		LogConsolidated.log(Level.INFO, 0, sourceName,
    				"[" + person.getLocationTag().getLocale() + "] " + person.getName() 
    				+ " was done doing biology study field work and going back to the rover.");
            setPhase(WALK_BACK_INSIDE);
            return time;
        }
        
        // Add research work to the scientific study for lead researcher.
        addResearchWorkTime(time);

        // Add experience points
        addExperience(time);


        return 0D;
    }

    /**
     * Adds research work time to the scientific study for the lead researcher.
     * @param time the time (millisols) performing field work.
     */
    private void addResearchWorkTime(double time) {
        // Determine effective field work time.
        double effectiveFieldWorkTime = time;
        int skill = getEffectiveSkillLevel();
        if (skill == 0) {
            effectiveFieldWorkTime /= 2D;
        }
        else if (skill > 1) {
            effectiveFieldWorkTime += effectiveFieldWorkTime * (.2D * skill);
        }

        // If person isn't lead researcher, divide field work time by two.
        if (!person.equals(leadResearcher)) {
            effectiveFieldWorkTime /= 2D;
        }

        // Add research to study for primary or collaborative researcher.
        if (study.getPrimaryResearcher().equals(leadResearcher)) {
            study.addPrimaryResearchWorkTime(effectiveFieldWorkTime);
        }
        else {
            study.addCollaborativeResearchWorkTime(leadResearcher, effectiveFieldWorkTime);
        }
    }

    @Override
    protected void addExperience(double time) {
        // Add experience to "EVA Operations" skill.
        // (1 base experience point per 100 millisols of time spent)
        double evaExperience = time / 100D;

        // Experience points adjusted by person's "Experience Aptitude" attribute.
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        int experienceAptitude = nManager.getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
        double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
        evaExperience += evaExperience * experienceAptitudeModifier;
        evaExperience *= getTeachingExperienceModifier();
        person.getMind().getSkillManager().addExperience(SkillType.EVA_OPERATIONS, evaExperience);

        // If phase is performing field work, add experience to biology skill.
        if (FIELD_WORK.equals(getPhase())) {
            // 1 base experience point per 10 millisols of field work time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            double biologyExperience = time / 10D;
            biologyExperience += biologyExperience * experienceAptitudeModifier;
            person.getMind().getSkillManager().addExperience(SkillType.BIOLOGY, biologyExperience);
        }
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(2);
        results.add(SkillType.EVA_OPERATIONS);
        results.add(SkillType.BIOLOGY);
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = person.getMind().getSkillManager();
        int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
        int biologySkill = manager.getEffectiveSkillLevel(SkillType.BIOLOGY);
        return (int) Math.round((double)(EVAOperationsSkill + biologySkill) / 2D);
    }

    @Override
    public void destroy() {
        super.destroy();

        leadResearcher = null;
        study = null;
        rover = null;
    }
}