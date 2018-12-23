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

package top.supcar.server.model;

import com.sun.org.apache.xpath.internal.operations.Mod;
import info.pavie.basicosmparser.model.Node;
import top.supcar.server.session.SessionObjects;
import top.supcar.server.graph.Distance;
import top.supcar.server.graph.Graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by 1 on 18.04.2017.
 */
public class CityCar extends Car {

	public CityCar(SessionObjects sessionObjects, List<Node> route, Driver driver,
				   double maxAcc, double mu, int lane) {

	    Distance distance = sessionObjects.getDistance();
	    Graph graph = sessionObjects.getGraph();
		this.routeList = route;
		this.speed = speed;
		//this.destIndex = route.size() - 1;
		this.sessionObjects = sessionObjects;
		this.mu = mu;
		this.lane.add(lane);

		routeArray = new ArrayList<Node>();

		Iterator it = route.iterator();

		while(it.hasNext()) {
			routeArray.add((Node)it.next());
		}

		//	System.out.println("Citycar, routearr: " + routeArray);

		this.maxAcc = maxAcc;
		this.driver = driver;
		driver.setCar(this);
		type = "C";
		Node first = route.get(0); Node next = route.get(1);
		pos = new Node(0, first.getLat(), first.getLon());
		toNextNode = distance.distanceBetween(first, next);
		setMaxSpeeds();
        if(route.get(0).getId().equals("N-13")) {
            if (maxspeeds.size() > 1) {
                speed = Math.min(Math.max(3, maxspeeds.get(1)), ModelConstants.CITY_CAR_DEF_SPEED);
            }
        }



        setMinMaxLanes();
        setShift();
        setLineWhereCarTurns();
        orientation = distance.angle(first, next);
        //System.out.println("init shift = " + shift);
        //correctPosAccountForShift();
	}



}
