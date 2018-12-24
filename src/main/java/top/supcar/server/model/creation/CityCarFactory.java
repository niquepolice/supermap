/*
 * Copyright 2017 SUPMUP
 *
 * This file is part of Supermap.
 *
 * Supermap is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Supermap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Supermap. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package top.supcar.server.model.creation;

import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Way;
import top.supcar.server.session.SessionObjects;
import top.supcar.server.graph.Distance;
import top.supcar.server.graph.Graph;
import top.supcar.server.holder.CarHolder;
import top.supcar.server.model.*;

import java.util.List;
import java.util.Random;

/**
 * Created by 1 on 19.04.2017.
 */
public class CityCarFactory implements CarFactory {

	SessionObjects sessionObjects;
	Random random;

	public CityCarFactory(SessionObjects sessionObjects) {
		this.sessionObjects = sessionObjects;
		random = new Random();
	}

	@Override
	public CityCar createCar(Node start, Node destination) {

		Graph graph = sessionObjects.getGraph();
		Distance distance = sessionObjects.getDistance();
		CarHolder carHolder = sessionObjects.getCarHolder();
		Driver driver = new Driver();
		double angle = 0;
		double maxAcc;
		int lane;
		List<Node> route = graph.getWay(start, destination);
		if(route == null) {
           // System.out.println("no way");
            return null;
        }

        Node next = route.get(1);
		Node first = route.get(0);

		if(first != null && next != null) angle = distance.angle(first, next);

		Way way = sessionObjects.getGraph().wayWhereAreBothNodesArePlaced(first, next);
		List<Node> nodes = way.getNodes();

		//lane = (int)(Math.round(Math.random()*(graph.numOfLanesBetween(first, next) - 1))) + 1;
		int lanes = graph.numOfLanesBetween(first, next);
		lane = random.nextInt(lanes) + 1;
       // System.out.println("lanes: " + lanes + " chosen lane: " + lane);


		maxAcc = ModelConstants.CITY_CAR_DEF_MAX_ACC;
		maxAcc += Math.random()*maxAcc/5;
		double mucoef = 1;//0.8 +  Math.random()*0.3;
		double mu = ModelConstants.CITY_CAR_DEF_MU*mucoef;
		CityCar car = new CityCar(sessionObjects, route, driver, maxAcc, mu, lane);
		List<RoadThing> cars = carHolder.getNearby(car.getPos());
		Car successor = null;
		for(RoadThing thing : cars) {
			Car cr = (Car) thing;
			if(driver.successor(cr)) {
				double spaceLeftToCr = distance.distanceBetween(car.getPos(), cr.getPos());
				double spaceLeft = spaceLeftToCr -
						ModelConstants.SPAWN_DISTANCE;
				//System.out.println(spaceLeft);
				if(spaceLeft <= 0) return null;
				successor = car;
			}
		}

		if (carHolder.updatePosition(car))
            return car;
		else
		    return null;


	}
}
