/**
 * Mars Simulation Project
 * TaskManager.java
 * @version 3.1.0 2017-02-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.person.CircadianClock;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ShiftType;
import org.mars_sim.msp.core.person.TaskSchedule;
import org.mars_sim.msp.core.person.ai.Mind;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.task.meta.MetaTask;
import org.mars_sim.msp.core.person.ai.task.meta.MetaTaskUtil;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The TaskManager class keeps track of a person's current task and can randomly
 * assign a new task to a person based on a list of possible tasks and that
 * person's current situation.
 *
 * There is one instance of TaskManager per person.
 */
public class TaskManager implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(TaskManager.class.getName());

	private static String loggerName = logger.getName();

	private static String sourceName = loggerName.substring(loggerName.lastIndexOf(".") + 1, loggerName.length());

	private static final String WALK = "walk";

	private static final int MAX_TASK_PROBABILITY = 3000;

	// Data members
	/** The cache for msolInt */
	private double msolCache = -1.0;

	private double totalProbCache;

	private String taskNameCache = "";

	private String taskDescriptionCache = "Relaxing";

	private String taskPhaseNameCache = "Relaxing";

	/** The current task the person/robot is doing. */
	private Task currentTask;
	/** The last task the person/robot was doing. */
	private Task lastTask;
	/** The mind of the person the task manager is responsible for. */
	private Mind mind;

	private Person person = null;

	private PhysicalCondition health;

	private CircadianClock circadian;

	private TaskSchedule ts;

	private int shiftCache;

	private transient Map<MetaTask, Double> taskProbCache;
	private transient List<MetaTask> mtListCache;

	private List<String> pendingTasks;
	
	private static MarsClock marsClock;
	private static MissionManager missionManager;

	/**
	 * Constructor.
	 * 
	 * @param mind the mind that uses this task manager.
	 */
	public TaskManager(Mind mind) {
		// Initialize data members
		this.mind = mind;

		person = mind.getPerson();

		missionManager = Simulation.instance().getMissionManager();

		circadian = person.getCircadianClock();

		health = person.getPhysicalCondition();

		currentTask = null;

		// Initialize cache values.
//		timeCache = null;
		taskProbCache = new HashMap<MetaTask, Double>();
		totalProbCache = 0D;

		// Ensure no NULLPOiNTEREXCEPTION in maven test
		// since marsClock won't be initiated in maven test
		if (Simulation.instance().getMasterClock() != null)
			marsClock = Simulation.instance().getMasterClock().getMarsClock();
		
		pendingTasks = new ArrayList<>();
	}

	/**
	 * Initializes tash schedule instance
	 */
	public void initialize() {
		ts = person.getTaskSchedule();
	}

	/**
	 * Returns true if person has an active task.
	 * 
	 * @return true if person has an active task
	 */
	public boolean hasActiveTask() {
		return (currentTask != null) && !currentTask.isDone();
	}

	/**
	 * Returns true if person has a task (may be inactive).
	 * 
	 * @return true if person has a task
	 */
	public boolean hasTask() {
		return currentTask != null;
	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskName() {
		if (currentTask != null) {
			return currentTask.getName();
		} else {
			return "";
		}
	}

	/**
	 * Returns the task name of the current or last task (one without the word
	 * "walk" in it).
	 * 
	 * @return task name
	 */
	public String getFilteredTaskName() {
		String s = getTaskName();
		if (s.toLowerCase().contains(WALK)) {
			// check if the last task is walking related
			if (lastTask != null) {
				s = lastTask.getName();
				if (lastTask != null && !s.toLowerCase().contains(WALK)) {
					return s;
				} else
					return "";
			} else
				return "";
		} else
			return s;

	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskClassName() {
		if (currentTask != null) {
			return currentTask.getTaskName();
		} else {
			return "";
		}
	}

	/**
	 * Returns a description of current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return a description of the current task
	 */
	public String getTaskDescription(boolean subTask) {
		if (currentTask != null) {
			String t = currentTask.getDescription(subTask);
			if (t != null)
				return t;
			else
				return "";
		} else
			return "";
	}
	
	
//	public FunctionType getFunction(boolean subTask) {
//		if (currentTask != null &&
//			currentTask.getFunction(subTask) != null) {
//			return
//					currentTask.getFunction(subTask); 
//		}
//					
//		else {
//			return FunctionType.UNKNOWN; 
//		} 
//	}
	

	/**
	 * Returns the current task phase if there is one. Returns null if current task
	 * has no phase. Returns null if there is no current task.
	 * 
	 * @return the current task phase
	 */
	public TaskPhase getPhase() {
		if (currentTask != null) {
			return currentTask.getPhase();
		} else {
			return null;
		}
	}

	/**
	 * Returns the current task phase if there is one. Returns null if current task
	 * has no phase. Returns null if there is no current task.
	 * 
	 * @return the current task phase
	 */
	public TaskPhase getMainTaskPhase() {
		if (currentTask != null) {
			return currentTask.getMainTaskPhase();
		} else {
			return null;
		}
	}

	/**
	 * Returns the current task. Return null if there is no current task.
	 * 
	 * @return the current task
	 */
	public Task getTask() {
		return currentTask;
	}

	public String getLastTaskName() {
		return taskNameCache;
	}

	public String getLastTaskDescription() {
		return taskDescriptionCache;
	}

	/**
	 * Sets the current task to null.
	 */
	public void clearTask() {
		if (currentTask != null) {
			currentTask.endTask();
			currentTask = null;
		}

		person.fireUnitUpdate(UnitEventType.TASK_EVENT);

	}

	public boolean isEVATask(String taskName) {
		return (taskName.toLowerCase().contains("eva") || taskName.toLowerCase().contains("dig")
				|| taskName.toLowerCase().contains("exploresite") || taskName.toLowerCase().contains("salvagebuilding")
				|| taskName.toLowerCase().contains("walkoutside") || taskName.toLowerCase().contains("minesite")
				|| taskName.toLowerCase().contains("collectmined") || taskName.toLowerCase().contains("fieldwork")
				|| taskName.toLowerCase().contains("collectresources"));
	}

	/**
	 * Filters task for recording 
	 * 
	 * @param time
	 */
	public void recordFilterTask(double time) {
		String taskDescription = getTaskDescription(false);
		String taskName = getTaskClassName();

		String missionName = "";
		if (missionManager.getMission(person) != null)
			missionName = missionManager.getMission(person).toString();

		// Remove tasks such as Walk, WalkRoverInterior, WalkSettlementInterior,
		// WalkSteps
		// Filters off descriptions such as "Walking inside a settlement"
		if (taskName != null) {

			if (isEVATask(taskName)) {
				person.addEVATime(taskName, time);
			}

			if (!taskDescription.equals(taskDescriptionCache)
//				&& !taskName.toLowerCase().contains(WALK) 
//				&& !taskDescription.toLowerCase().contains(WALK) 
					&& !taskDescription.equals("")) {

				String taskPhaseName = null;
				TaskPhase tp = getMainTaskPhase();

				if (tp != null) {
					taskPhaseName = tp.getName();
					if (!taskPhaseNameCache.equals(taskPhaseName)) {
						// Note : can taskPhaseName be null ?
						// TODO: decide if it needs to record the same task description as the last
						ts.recordTask(taskName, taskDescription, taskPhaseName, missionName);
						taskPhaseNameCache = taskPhaseName;
						taskDescriptionCache = taskDescription;
					}
				}
			}
		}
	}

	/**
	 * Adds a task to the stack of tasks.
	 * 
	 * @param newTask the task to be added
	 */
	public void addTask(Task newTask) {

		if (hasActiveTask()) {
			currentTask.addSubTask(newTask);

		} else {
			lastTask = currentTask;
			currentTask = newTask;
			taskNameCache = currentTask.getTaskName();
			taskDescriptionCache = currentTask.getDescription();

			TaskPhase tp = currentTask.getPhase();
			if (tp != null)
				if (tp.getName() != null)
					taskPhaseNameCache = tp.getName();
				else
					taskPhaseNameCache = "";
			else
				taskPhaseNameCache = "";

		}

		person.fireUnitUpdate(UnitEventType.TASK_EVENT, newTask);

	}

	/**
	 * Reduce the person's caloric energy over time.
	 * 
	 * @param time the passing time (
	 */
	public void reduceEnergy(double time) {
		if (health == null)
			health = person.getPhysicalCondition();
		health.reduceEnergy(time);

	}

	/**
	 * Perform the current task for a given amount of time.
	 * 
	 * @param time       amount of time to perform the action
	 * @param efficiency The performance rating of person performance task.
	 * @return remaining time.
	 * @throws Exception if error in performing task.
	 */
	public double executeTask(double time, double efficiency) {
		double remainingTime = 0D;

		if (currentTask != null) {
			// For effort driven task, reduce the effective time based on efficiency.
			if (efficiency < .1D) {
				efficiency = .1D;
			}

			if (currentTask.isEffortDriven()) {
				time *= efficiency;
			}

//			 if (person.isInside()) {
//			 checkForEmergency();
//			 }

			remainingTime = currentTask.performTask(time);

			// Expend energy based on activity.
			double energyTime = time - remainingTime;

			// Double energy expenditure if performing effort-driven task.
			if (currentTask != null && currentTask.isEffortDriven()) {
				// why java.lang.NullPointerException at TR = 2048 ?
				energyTime *= 2D;
			}

			if (energyTime > 0D) {
				if (person.isOutside()) {

					if (circadian == null)
						circadian = person.getCircadianClock();

					// it takes more energy to be in EVA doing work
					reduceEnergy(energyTime);
					circadian.exercise(time);
				} else
					reduceEnergy(energyTime);
			}
		}

		return remainingTime;

	}

	private boolean doingEmergencyRepair() {
		// Check if person is already repairing either a EVA or non-EVA emergency.
		return (currentTask != null) && ((currentTask instanceof RepairEmergencyMalfunctionEVA)
				|| (currentTask instanceof RepairEmergencyMalfunction));

	}

	private boolean doingAirlockTask() {
		// Check if person is performing an airlock task.
		boolean hasAirlockTask = false;
		Task task = currentTask;
		while (task != null) {
			if ((task instanceof EnterAirlock) || (task instanceof ExitAirlock)) {
				hasAirlockTask = true;
			}
			task = task.getSubTask();
		}

		return hasAirlockTask;
	}

	/**
	 * Checks if the person or robot is walking through a given building.
	 * 
	 * @param building the building.
	 * @return true if walking through building.
	 */
	public boolean isWalkingThroughBuilding(Building building) {

		boolean result = false;

		Task task = currentTask;
		while ((task != null) && !result) {
			if (task instanceof Walk) {
				Walk walkTask = (Walk) task;
				if (walkTask.isWalkingThroughBuilding(building)) {
					result = true;
				}
			}
			task = task.getSubTask();
		}

		return result;
	}

	/**
	 * Checks if the person or robot is walking through a given vehicle.
	 * 
	 * @param vehicle the vehicle.
	 * @return true if walking through vehicle.
	 */
	public boolean isWalkingThroughVehicle(Vehicle vehicle) {

		boolean result = false;

		Task task = currentTask;
		while ((task != null) && !result) {
			if (task instanceof Walk) {
				Walk walkTask = (Walk) task;
				if (walkTask.isWalkingThroughVehicle(vehicle)) {
					result = true;
				}
			}
			task = task.getSubTask();
		}

		return result;
	}

//	/**
//	 * Checks if any emergencies are happening in the person's local.
//	 * Adds an emergency task if necessary.
//	 */
//	private void checkForEmergency() {
//
//		// Check for emergency malfunction.
//		if (!RepairEmergencyMalfunction.hasEmergencyMalfunction(person))
//			return;
//
//	    // Check if person is already repairing an emergency.
//	    if (doingEmergencyRepair())
//	    	return;
//
//		// Check if person is performing an airlock task.
//		if(doingAirlockTask())
//			return;
//
//		if (RepairEmergencyMalfunctionEVA.requiresEVARepair(person)) {
//
//            if (RepairEmergencyMalfunctionEVA.canPerformEVA(person)) {
//
//            	//if (person.isOutside())
//            	//	return;
//
//				//int numOutside = person.getAssociatedSettlement().getNumOutsideEVAPeople();
//				//if (numOutside == 0) {		
//				//}
//            	
//        		// if he is not outside, he may take on this repair task
//        		LogConsolidated.log(Level.INFO, 1000, sourceName, 
//        				person + " cancelled '" + currentTask +
//                        "' and rushed to the scene to participate in an EVA emergency repair.", null);
//                clearTask();
//                
//                addTask(new RepairEmergencyMalfunctionEVA(person));
//
//            }
//		}
//		
//		else { // requires no EVA for the repair
//			
//    		LogConsolidated.log(Level.INFO, 1000, sourceName, 
//    				person + " cancelled '" + currentTask +
//                    "' and rushed to the scene to participate in an non-EVA emergency repair.", null);
//            clearTask();
//            
//            addTask(new RepairEmergencyMalfunction(person));
//		}
//
//	}

	/**
	 * Gets a new task for the person based on tasks available.
	 * 
	 * @return new task
	 */
	public Task getNewTask() {
		Task result = null;
		MetaTask selectedMetaTask = null;

		// If cache is not current, calculate the probabilities.
		if (!useCache()) {
			calculateProbability();
		}
		// Get a random number from 0 to the total weight
		double totalProbability = getTotalTaskProbability(true);

		if (totalProbability == 0D) {
//			throw new IllegalStateException(mind.getPerson() + " has zero total task probability weight.");
//			LogConsolidated.log(Level.SEVERE, 5_000, sourceName,
//					person.getName() + " has zero total task probability weight.");

			// Switch to loading non-work hour meta tasks since
			// leisure tasks are NOT based on needs
			List<MetaTask> list = MetaTaskUtil.getNonWorkHourMetaTasks();
			selectedMetaTask = list.get(RandomUtil.getRandomInt(list.size() - 1));

		} else {

			double r = RandomUtil.getRandomDouble(totalProbability);

			// Determine which task is selected.
			for (MetaTask mt : taskProbCache.keySet()) {
				double probWeight = taskProbCache.get(mt);
				if (r <= probWeight) {
					// Select this task
					selectedMetaTask = mt;
				} else {
					r -= probWeight;
				}
			}
		}

		if (selectedMetaTask == null) {
//			throw new IllegalStateException(mind.getPerson() + " could not determine a new task.");
			LogConsolidated.log(Level.SEVERE, 5_000, sourceName, person.getName() + " could not determine a new task.");
		} else {
			// Call constructInstance of the selected Meta Task to commence the ai task
			result = selectedMetaTask.constructInstance(mind.getPerson());
//			System.out.println(person + " is going to " + selectedMetaTask.getName());
		}

		// Clear time cache.
		msolCache = -1;

		return result;
	}

	/**
	 * Determines the total probability weight for available tasks.
	 * 
	 * @return total probability weight
	 */
	public double getTotalTaskProbability(boolean useCache) {
		// If cache is not current, calculate the probabilities.
		if (!useCache) {
			calculateProbability();
		}
		return totalProbCache;
	}

	public static boolean isInMissionWindow(double time) {
		boolean result = false;

		return result;
	}

	/**
	 * Calculates and caches the probabilities.
	 */
	private void calculateProbability() {

		double msol1 = marsClock.getMillisolOneDecimal();

		int diff = Double.compare(msolCache, msol1);
		if (diff < 0 || diff > 0) {
			msolCache = msol1;

			int shift = 0;

			if (ts.getShiftType() == ShiftType.ON_CALL) {
				shift = 0;
			}

			else if (ts.isShiftHour(marsClock.getMillisolInt())) {
				shift = 1;
			}

			else {
				shift = 2;
			}

			// Note : mtListCache is null when loading from a saved sim
			if (shiftCache != shift || mtListCache == null) {
				shiftCache = shift;

				List<MetaTask> mtList = null;

				// NOTE: any need to use getAnyHourTasks()
				if (shift == 0) {
					mtList = MetaTaskUtil.getAllMetaTasks();
				}

				else if (shift == 1) {
					mtList = MetaTaskUtil.getDutyHourTasks();
				}

				else if (shift == 2) {
					mtList = MetaTaskUtil.getNonDutyHourTasks();
				}

				// Use new mtList
				mtListCache = mtList;
				// Create new taskProbCache
				taskProbCache = new HashMap<MetaTask, Double>(mtList.size());
			}

			// Clear total probabilities.
			totalProbCache = 0D;
			// Determine probabilities.
			for (MetaTask mt : mtListCache) {
				double probability = mt.getProbability(person);
				if ((probability >= 0D) && (!Double.isNaN(probability)) && (!Double.isInfinite(probability))) {
					if (probability > MAX_TASK_PROBABILITY) {
						LogConsolidated.log(Level.FINER, 5_000, sourceName, mind.getPerson().getName() + " - "
								+ mt.getName() + " : Probability is " + Math.round(probability * 10.0) / 10.0 + ".");
						if (mt.getName().contains("eat"))
							addTask(new EatMeal(person));
						else if (mt.getName().contains("sleep"))
							addTask(new Sleep(person));
						else
							probability = MAX_TASK_PROBABILITY;
					}
//					if (person.getName().contains("Enrique")) // && mt.getName().contains("Review"))
//						System.out.println(person + " " + mt.getName() + " " + Math.round(probability*10.0)/10.0);
					taskProbCache.put(mt, probability);
					totalProbCache += probability;
//					System.out.println(person + " totalProbCache : " + Math.round(totalProbCache*10.0)/10.0);
				}

				else {
					taskProbCache.put(mt, 0D);
					LogConsolidated.log(Level.SEVERE, 5_000, sourceName,
							mind.getPerson().getName() + " has invalid probability when calculating " + mt.getName()
									+ " : Probability is " + probability + ".");
				}
			}
		}
	}

	/**
	 * Checks if task probability cache should be used.
	 * 
	 * @return true if cache should be used.
	 */
	private boolean useCache() {
		int retval = Double.compare(msolCache, marsClock.getMillisolOneDecimal());
		if (retval > 0 || retval < 0) {
			return false;
		}
		return true;
	}

	public TaskSchedule getTaskSchedule() {
		return ts;
	}

	/**
	 * Gets all pending tasks 
	 * 
	 * @return
	 */
	public List<String> getPendingTasks() {
		return pendingTasks;
	}
	
	public boolean hasPendingTask() {
		return !pendingTasks.isEmpty();
	}
	
	/**
	 * Adds a pending task
	 * 
	 * @param task
	 */
	public void addAPendingTask(String task) {
		pendingTasks.add(task);
	}
	
	/**
	 * Deletes a pending task
	 * 
	 * @param task
	 */
	public void deleteAPendingTask(String task) {
		pendingTasks.remove(task);
	}
	
	/**
	 * Gets the first pending meta task in the queue
	 * 
	 * @return
	 */
	public MetaTask getAPendingMetaTask() {
		if (!pendingTasks.isEmpty()) {
			String firstTask = pendingTasks.get(0);
			pendingTasks.remove(firstTask);
			return convertTask2MetaTask(firstTask);
		}
		return null;
	}
	
	/**
	 * Converts a task to its corresponding meta task
	 * 
	 * @param a task
	 */
	public static MetaTask convertTask2MetaTask(String task) {
		MetaTask result = null;
		result = MetaTaskUtil.getMetaTask(task.replaceAll(" ","") + "Meta");
		return result;
	}
	
	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param clock
	 * @param mgr
	 */
	public static void initializeInstances(MarsClock clock, MissionManager mgr) {
		marsClock = clock;
		missionManager = mgr;
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		if (currentTask != null)
			currentTask.destroy();
		mind = null;
		person = null;
//		timeCache = null;
		lastTask = null;
		health = null;
		circadian = null;
		ts = null;
		marsClock = null;
		if (taskProbCache != null) {
			taskProbCache.clear();
			taskProbCache = null;
		}
	}
}