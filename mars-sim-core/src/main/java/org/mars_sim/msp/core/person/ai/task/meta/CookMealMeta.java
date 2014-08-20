/**
 * Mars Simulation Project
 * CookMealMeta.java
 * @version 3.07 2014-08-05
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.CookMeal;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * Meta task for the CookMeal task.
 */
public class CookMealMeta implements MetaTask {

    // TODO: Use enum instead of string for name for internationalization.
    private static final String NAME = "Cooking";
    
    /** default logger. */
    private static Logger logger = Logger.getLogger(CookMealMeta.class.getName());
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new CookMeal(person);
    }

    @Override
    public double getProbability(Person person) {
        
        double result = 0D;

        if (CookMeal.isMealTime(person)) {

            try {
                // See if there is an available kitchen.
                Building kitchenBuilding = CookMeal.getAvailableKitchen(person);
                if (kitchenBuilding != null) {
                    result = 200D;

                    // Crowding modifier.
                    result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, kitchenBuilding);
                    result *= TaskProbabilityUtil.getRelationshipModifier(person, kitchenBuilding);

                    // Check if there is enough food available to cook.
                    PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
                    double foodRequired = config.getFoodConsumptionRate() * (1D / 3D);
                    AmountResource food = AmountResource.findAmountResource("food");
                    double foodAvailable = person.getSettlement().getInventory().getAmountResourceStored(
                            food, false);
                    if (foodAvailable < foodRequired) result = 0D;
                }
            }
            catch (Exception e) {
                logger.log(Level.SEVERE,"CookMealMeta.getProbability()" ,e);
            }

            // Effort-driven task modifier.
            result *= person.getPerformanceRating();

            // Job modifier.
            Job job = person.getMind().getJob();
            if (job != null) result *= job.getStartTaskProbabilityModifier(CookMeal.class);
        }

        return result;
    }
}