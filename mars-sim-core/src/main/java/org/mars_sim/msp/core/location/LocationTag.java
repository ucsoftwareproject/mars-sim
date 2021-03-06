/**
 * Mars Simulation Project
 * LocationTag.java
* @version 3.1.0 2017-10-10
 * @author Manny Kung
 */
package org.mars_sim.msp.core.location;

import java.io.Serializable;
import java.util.Collection;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.mars.MarsSurface;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.vehicle.Vehicle;

public class LocationTag implements LocationState, Serializable {

	private static final long serialVersionUID = 1L;

	public static final String OUTSIDE_ON_MARS = "outside on Mars";

	public static final String VICINITY = " vicinity";

	private static final String UNKNOWN = "unknown";

	private static final String IN = " in ";

	private Unit unit;

	private Person p = null;
	private Robot r = null;
	private Equipment e = null;
	private Building b = null;
	private Vehicle v = null;

	private static UnitManager unitManager = Simulation.instance().getUnitManager();

	public LocationTag(Unit unit) {
		this.unit = unit;
		if (unit instanceof Person)
			p = (Person) unit;
		else if (unit instanceof Robot)
			r = (Robot) unit;
		else if (unit instanceof Equipment)
			e = (Equipment) unit;
		else if (unit instanceof Building)
			b = (Building) unit;
		else if (unit instanceof Vehicle)
			v = (Vehicle) unit;

	}

	public String getSettlementName() {
		if (p != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == p.getLocationStateType())
				return p.getSettlement().getName();
			else
				return p.getCoordinates().getFormattedString();
		} else if (e != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == e.getLocationStateType())
				return e.getSettlement().getName();
			else
				return e.getCoordinates().getFormattedString();
		} else if (r != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == r.getLocationStateType())
				return r.getSettlement().getName();
			else
				return r.getCoordinates().getFormattedString();// OUTSIDE_ON_MARS;
		} else if (b != null) {
			return b.getSettlement().getName();
		}

		else if (v != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == v.getLocationStateType())
				return v.getSettlement().getName();
			else
				return v.getCoordinates().getFormattedString();// OUTSIDE_ON_MARS;
		}

		return UNKNOWN;
	}

	/**
	 * Obtains the quick location name (either settlement, buried settlement,
	 * vehicle or coordinates)
	 * 
	 * @return the name string of the location the unit is at
	 */
	public String getQuickLocation() {
		if (p != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == p.getLocationStateType())
				return p.getSettlement().getName();
			else if (LocationStateType.INSIDE_VEHICLE == p.getLocationStateType())
				return p.getVehicle().getName();
			else if (p.isBuried() && p.getBuriedSettlement() != null)
				return p.getBuriedSettlement().getName();
			else if (p.isDeclaredDead())
				return p.getAssociatedSettlement().getName();
			else
				return p.getCoordinates().getFormattedString();
		}

		else if (e != null) {
			if (!(e.getContainerUnit() instanceof MarsSurface))
				return e.getContainerUnit().getName();
			else if (!(e.getTopContainerUnit() instanceof MarsSurface))
				return e.getTopContainerUnit().getName();
			else
				return e.getCoordinates().getFormattedString();
		}

		else if (r != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == r.getLocationStateType())
				return r.getSettlement().getName();
			else if (LocationStateType.INSIDE_VEHICLE == r.getLocationStateType())
				return r.getVehicle().getName();
			else
				return r.getCoordinates().getFormattedString();

		} else if (b != null) {
			return b.getNickName() + " in " + b.getSettlement().getName();
		}

		else if (v != null) {
			if (LocationStateType.OUTSIDE_SETTLEMENT_VICINITY == v.getLocationStateType())
				return v.getSettlement().getName();
			else if (LocationStateType.INSIDE_SETTLEMENT == v.getLocationStateType())
				return v.getBuildingLocation().getNickName();
			else
				return v.getCoordinates().getFormattedString();
		}

		return UNKNOWN;
	}

	/**
	 * Obtains the general locale (settlement or coordinates)
	 * 
	 * @return the settlement or the coordinates
	 */
	public String getLocale() {
		if (p != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == p.getLocationStateType())
				return p.getSettlement().getName();
			else
				return p.getCoordinates().getFormattedString();
		}

		else if (e != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == e.getLocationStateType())
				return e.getSettlement().getName();
			else
				return e.getCoordinates().getFormattedString();
		}

		else if (r != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == r.getLocationStateType())
				return r.getSettlement().getName();
			else
				return r.getCoordinates().getFormattedString();

		} 
		
		else if (b != null) {
			return b.getSettlement().getName();
		}

		else if (v != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == v.getLocationStateType())
				return v.getSettlement().getName();		
			else
				return v.getCoordinates().getFormattedString();
		}

		return UNKNOWN;
	}

	/**
	 * Obtains the extended location details
	 * 
	 * @return the name string of the location the unit is at
	 */
	public String getExtendedLocations() {
		return getImmediateLocation() + IN + getLocale();
	}

	/**
	 * Obtains the immediate location (either building, vehicle, a settlement's
	 * vicinity or outside on Mars)
	 * 
	 * @return the name string of the location the unit is at
	 */
	public String getImmediateLocation() {
		if (p != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == p.getLocationStateType()) {
				if (p.getBuildingLocation() != null) {
					return p.getBuildingLocation().getNickName();
				}
				else {
					return p.getLocationStateType().getName();
				}
			} else if (LocationStateType.INSIDE_VEHICLE == p.getLocationStateType()) {
				Vehicle v = p.getVehicle();
				if (v.getBuildingLocation() == null) {
					return v.getNickName();
				} else {
					return v.getBuildingLocation().getNickName();
				}
			} else if (p.isRightOutsideSettlement())
				return findSettlementVicinity().getName() + VICINITY;
			// TODO: check if it works in case of a trader arrives at any settlements for
			// trades.
			else
				return OUTSIDE_ON_MARS;
		}

		else if (e != null) {
			if (LocationStateType.ON_A_PERSON == e.getLocationStateType())
				return e.getContainerUnit().getLocationTag().getImmediateLocation();
			else if (e.isRightOutsideSettlement())
				return findSettlementVicinity().getName() + VICINITY;
			else if (e.isInside()) //!(e.getContainerUnit() instanceof MarsSurface))
				return e.getContainerUnit().getName();
			else
				return OUTSIDE_ON_MARS;
		}

		else if (r != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == r.getLocationStateType()) {
				if (r.getBuildingLocation() != null) {
					return r.getBuildingLocation().getNickName();
				} else {
					return OUTSIDE_ON_MARS;
				}
			} else if (r.getVehicle() != null) {
				Vehicle v = r.getVehicle();
				if (v.getBuildingLocation() == null) {
					return v.getNickName();
				} else {
					return v.getBuildingLocation().getNickName();
				}
			} else if (r.isRightOutsideSettlement())
				return findSettlementVicinity().getName() + VICINITY;
			else
				return OUTSIDE_ON_MARS;

		} else if (b != null) {
			return b.getNickName();
		}

		else if (v != null) {
			if (LocationStateType.INSIDE_SETTLEMENT == v.getLocationStateType()) {
				if (v.getBuildingLocation() != null) {
					return v.getBuildingLocation().getNickName();
				} else {
					return OUTSIDE_ON_MARS;
				}
			} else if (v.isRightOutsideSettlement())
				return findSettlementVicinity().getName() + VICINITY;
			else
				return OUTSIDE_ON_MARS;
		}

		return UNKNOWN;
	}

	public Settlement findSettlementVicinity() {
		Coordinates c = unit.getCoordinates();

		if (unitManager == null)
			unitManager = Simulation.instance().getUnitManager();
				
		Collection<Settlement> ss = unitManager.getSettlements();
		for (Settlement s : ss) {
			if (s.getCoordinates().equals(c) || s.getCoordinates() == c)
				return s;
		}

		return unit.getAssociatedSettlement(); 
		// WARNING : using associated settlement needs to exercise more caution
	}

	public LocationStateType getType() {
		return unit.getLocationStateType();
	}

	public LocationSituation getLocationSituation() {
		if (p != null) {
			if (p.getLocationSituation() != null)
				return p.getLocationSituation();
		} else if (e != null) {
			if (e.getLocationSituation() != null)
				return e.getLocationSituation();
		} else if (r != null) {
			if (r.getLocationSituation() != null)
				return r.getLocationSituation();
		}
		return LocationSituation.UNKNOWN;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public void destroy() {
		unit = null;
		p = null;
		r = null;
		e = null;
		b = null;
		v = null;
	}

}