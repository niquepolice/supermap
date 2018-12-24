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

import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Way;
import top.supcar.server.graph.Distance;
import top.supcar.server.graph.Graph;
import top.supcar.server.holder.CarHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by 1 on 18.04.2017.
 */
public class Driver {

	private Car car;
    private double shiftRemainder = 0;
    private boolean slowing = false;
    private int slowingToIndex = -1;
    private Way lastway;
    private Random random;
    private int desiredLane;

	public Car getCar() {
		return car;
	}

	public double pushPedal() {
        if(car.speed < 0) car.speed = 0.5;
        double accelerate, tempacc = 0;
        CarHolder carHolder = car.sessionObjects.getCarHolder();
        Distance distance = car.sessionObjects.getDistance();
		if(car.speed < ModelConstants.CITY_MAX_SPEED) {
            accelerate = 0.6;
            if(car.speed < 5) // ускорение на старте
                accelerate = 0.8;
        }
		else
			accelerate = 0;
        if(car.speed >  ModelConstants.CITY_MAX_SPEED) accelerate = -0.1;

        if(car.toNextNode > 0) {
            double dist = car.toNextNode;
            for(int i = car.prevNodeIndex + 1, j = 0; i + j < car.routeArray.size() && j < 4; j++) {
                dist += j > 0 ? distance.distanceBetween(car.routeArray.get(j-1), car.routeArray.get(j)) : 0;
                tempacc = Math.min((Math.pow(car.maxspeeds.get(i + j), 2) - Math.pow(car.speed, 2)) /
                        (2 * dist), tempacc);
            }
            if (tempacc < 0) {
                //if(tempacc < -10) System.out.println("bigacc " + tempacc);
                tempacc /= car.mu * 9.8;
                if (slowingToIndex != car.prevNodeIndex + 1)
                    slowing = false;
                if (car.speed <= car.maxspeeds.get(car.prevNodeIndex + 1))
                    slowing = false;
                if (tempacc < -0.2 || slowing) {
                    slowing = true;
                    slowingToIndex = car.prevNodeIndex + 1;
                    accelerate = tempacc;
                }
            }
        }
        if(Math.abs(car.orientation) >  Math.PI)
            System.out.println("orientation: " + car.orientation);
        tempacc = accelerate;

        List<RoadThing> cars = carHolder.getNearby(car.getPos());
        double divspeed, spaceLeftToCr, spaceLeft;
        Node v1 = car.getVelocityVector();
        double u0 = v1.getLon();
        double w0 = v1.getLat();
        double x = car.getPosWithShift().getLon();
        double y = car.getPosWithShift().getLat();
        Node v2;
        for(RoadThing thing : cars) {
            Car anotherCar = (Car)thing;
            if(anotherCar.pos.equals(car.pos)) continue;
            if(!iCareAbout(anotherCar)) continue;
            v2 = anotherCar.getVelocityVector();
            double a = anotherCar.getPosWithShift().getLon();
            double b = anotherCar.getPosWithShift().getLat();
            //относительная скорость
            double u = u0 - v2.getLon();
            double w = w0 - v2.getLat();
            if(u*w0 - w*u0 != 0) {
                if (u != 0 || w != 0) {
                    double alpha = ((a - x) * u + (b - y) * w) / (u * u + w * w);
                    //координаты проекции второй машины на траекторию(прямую) первой в СО, где вторая покоится
                    double px = x + alpha * u;
                    double py = y + alpha * w;
                    Node projection = new Node(0, py, px);
                    double closestDist = distance.distanceBetween(projection, anotherCar.getPosWithShift());
                    double dist = distance.distanceBetween(projection, car.getPosWithShift());
                    double rvx = distance.latDegToMeters(w), rvy = distance.lonDegToMeters(u);
                    double relativeSpeed = Math.sqrt(rvx * rvx + rvy * rvy);
                    //System.out.println("relspeed: " + relativeSpeed + " reldistance: " + dist);
                    if (closestDist < ModelConstants.SERIOUS_CLOSESNESS_M && alpha > 0) {
                        if(dist/relativeSpeed < ModelConstants.SERIOUS_CLOSESNESS_T) {
                            //if(u0*v2.getLat()- w0*v2.getLon() > 0) {
                            if(car.speed < anotherCar.getSpeed())
                                if (u0*(a-x) + w0*(b-y) > 0) {
                                    tempacc = -(relativeSpeed +5)/ dist;
                                }
                            }


                    }

                }
            }
            else {
                double dist = distance.distanceBetween(car.getPosWithShift(),anotherCar.getPosWithShift());
                if(dist == 0) car.crashed = true;
                //если наша машина догоняет(едет навстречу(хотя не должна)) другую
                if((a-x)*u0 + (b-y)*w0 > 0 && (a-x)*u + (b-y)*w > 0) {
                    double relativeSpeed = (Math.abs(car.speed - anotherCar.getSpeed()));
                    if(dist/relativeSpeed <  ModelConstants.SERIOUS_CLOSESNESS_T) {
                        tempacc = -(relativeSpeed+5)/dist*3;

                    }
                }
            }

        }
        if(tempacc < accelerate) {
            if(car.speed > 1)
                accelerate = tempacc/(car.mu * 9.8);
            else
                car.speed = 0;
        }


       /* for(RoadThing thing : cars) {
            Car cr = (Car)thing;
            if(successor(cr)) {
                divspeed = car.speed - cr.speed;
                spaceLeftToCr = distance.distanceBetween(car.pos, cr.pos);
                spaceLeft = spaceLeftToCr - ModelConstants.RECOMMENDED_DISTANCE *
                 (car.speed + 5) / ModelConstants.CITY_MAX_SPEED;

                if (spaceLeft <= 0) {
                        accelerate = -1;
                        car.speed = cr.speed;
                } else {
                    tempacc = -divspeed * divspeed / (2 * spaceLeft);
                    tempacc /= car.mu * 9.8;
                    if (tempacc < -0.2)
                        if (tempacc < accelerate)
		        accelerate = tempacc;
        }
                // System.out.println("spaceleft: " +spaceLeft + " accelerate: " + accelerate);

            }
        }*/




        if(accelerate > 1) accelerate = 1;
		else if(accelerate < -1) accelerate = -1;

		return accelerate;
	}

    public void turnWheel() {
        double maxshift = car.calcShift(car.minlane);
        double minshift = car.calcShift(car.maxlane);
        double ln = car.calcLane();
       /* if(car.shift - maxshift > ModelConstants.SHIFT_MISTAKE) {
            System.out.println("car.shift > maxshift");
            car.shifting = true;
            car.shiftSpeed = -Math.min(car.speed, ModelConstants.CITY_CAR_MAX_SHIFT_SPEED);
        }
        else if(minshift - car.shift > ModelConstants.SHIFT_MISTAKE) {
            System.out.println(minshift+ " " + car.shift + " " + maxshift
                    + " minmaxlanes: "+car.minlane + " " + car.maxlane);
            car.shifting = true;
            car.shiftSpeed = Math.min(car.speed, ModelConstants.CITY_CAR_MAX_SHIFT_SPEED);
        }
        else if(Math.abs(ln - Math.round(ln))*ModelConstants.LINE_BREADTH > ModelConstants.SHIFT_MISTAKE/2) {
            car.shifting = true;
            int lane = (int)Math.round(ln);
            if(lane > ln) car.shiftSpeed = - Math.min(car.speed, ModelConstants.CITY_CAR_MAX_SHIFT_SPEED);
            else car.shiftSpeed = Math.min(car.speed, ModelConstants.CITY_CAR_MAX_SHIFT_SPEED);
        }
        else */if(Math.abs(Math.round(ln) - desiredLane)*ModelConstants.LINE_BREADTH > ModelConstants.SHIFT_MISTAKE/2) {
            car.shifting = true;
            int lane = (int)Math.round(ln);
            //System.out.println("lane: " + lane + " " + desiredLane + " ln: " + ln);
            if(lane > desiredLane) car.shiftSpeed =  Math.min(car.speed, ModelConstants.CITY_CAR_MAX_SHIFT_SPEED);
            else car.shiftSpeed = -Math.min(car.speed, ModelConstants.CITY_CAR_MAX_SHIFT_SPEED);
        }
        else {
            car.shifting = false;
            car.shiftSpeed = 0;
        }
//        if(Math.abs(car.shiftSpeed) > 0)
//           System.out.println(car+ " shiftspeed " + car.shiftSpeed +" "+  car.shift);
	}

	public void setCar(Car car) {
		this.car = car;
        random = new Random();
        desiredLane = car.lane.get(0);
    }

    public boolean successor(Car cr) {
        if(cr == car) return false;
        Distance distance = car.sessionObjects.getDistance();
        Graph graph = car.sessionObjects.getGraph();
        if(distance.distanceBetween(car.pos, cr.pos) < 60) {
            Node prev = cr.routeArray.get(cr.prevNodeIndex);
            Node next = cr.routeArray.get(cr.prevNodeIndex + 1);
            int i = car.prevNodeIndex;
            while (i < car.routeArray.size()) {
                if (car.routeArray.get(i) == prev) {
                    if (i + 1 < car.routeArray.size()) {
                        if (car.routeArray.get(i + 1) == next)
                            if(cr.toNextNode < car.toNextNode)
                                return true;
                    }
                }
                i++;
            }
        }
        return false;
    }

    void setDesiredLane() {
        Graph graph = car.sessionObjects.getGraph();

        if(lastway == null) {
            lastway = graph.wayWhereAreBothNodesArePlaced(car.routeArray.get(0), car.routeArray.get(1));
        }
        if(car.prevNodeIndex == car.routeArray.size() - 1) return;
        Way newWay = graph.wayWhereAreBothNodesArePlaced(car.routeArray.get(car.prevNodeIndex),
                car.routeArray.get(car.prevNodeIndex+1));
        if(!newWay.equals(lastway)) {
            int lanes = graph.numOfLanesBetween(car.routeArray.get(car.prevNodeIndex),
                    car.routeArray.get(car.prevNodeIndex+1));
            if (lanes < 1) lanes = 1; //TODO: fix java.lang.IllegalArgumentException: bound must be positive
            desiredLane = random.nextInt(lanes) + 1;
//            System.out.println(lastway.getTags().get("name") + " -> " + newWay.getTags().get("name") +
//                     " lanes: " + lanes + " desired lane: " + desiredLane );
        }
        lastway = newWay;

    }

    private boolean iCareAbout(Car anotherCar) {
        Distance distance = car.sessionObjects.getDistance();
        for(int i = car.prevNodeIndex; i < car.routeArray.size(); i++) {
            if(distance.distanceBetween(car.routeArray.get(i), car.getPos()) > 100) return false;
            for(int j = anotherCar.getPrevNodeIndex(); j < anotherCar.routeArray.size(); j++) {
                if(distance.distanceBetween(anotherCar.getPos(), anotherCar.routeArray.get(j)) > 200) break;
                if(anotherCar.routeArray.get(j).equals(car.routeArray.get(i))) {
                    if(j < anotherCar.routeArray.size() - 1)
                        if(i > 0)
                            if(anotherCar.routeArray.get(j+1).equals(car.routeArray.get(i-1))) return false;
                    if(i < car.routeArray.size() - 1)
                        if(j > 0)
                            if(anotherCar.routeArray.get(j-1).equals(car.routeArray.get(i+1))) return false;

                    return true;
                }

            }
        }
        return false;
	}

    /*private ArrayList<Node> tempPathPart() {

    }*/



}
