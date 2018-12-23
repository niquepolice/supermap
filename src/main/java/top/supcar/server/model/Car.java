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
import top.supcar.server.session.SessionObjects;
import top.supcar.server.graph.Distance;
import top.supcar.server.graph.Graph;
import top.supcar.server.physics.Physics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by 1 on 16.04.2017.
 */
public abstract class Car extends RoadThing {
	protected List<Node> routeList;
	protected SessionObjects sessionObjects;
    protected ArrayList<Node> routeArray;
	protected double speed;
	protected double orientation;
	//protected int destIndex;
	protected Driver driver;
	protected double maxAcc;
	protected int prevNodeIndex = 0;
	protected List<Integer> lane = new LinkedList<>();
	protected double shift;
	protected double shiftSpeed;
	protected boolean shifting = false;
	protected double mu;
	protected Way lastway;
	public int turns;

    public boolean isCrashed() {
        return crashed;
    }

    protected boolean crashed = false;

    protected double currStep;
    protected double shiftStep = 0;
    protected double toNextNode;
	protected List<Double> maxspeeds; //maxspeed for each node in route
    public double[] lineWhereCarTurns;
    /**
     * границы номеров полос, из которых можно поворачивать.
     * нумерация полос от обочины справа, начинается с 1
      */
    protected int minlane, maxlane;

	public void updatePos() {
	    int destIndex = routeArray.size() - 1;
		double requestedAcc = driver.pushPedal();
		driver.turnWheel();
		if(requestedAcc > 0 ) requestedAcc *= maxAcc;
		else requestedAcc *= 3*mu*9.8;
		double ratio, dx, dy;
		Physics physics = sessionObjects.getPhysics();
		Distance distance = sessionObjects.getDistance();
		Graph graph = sessionObjects.getGraph();
		Node prev, next;

		//Attention! Shit code

		physics.getStepSpeed(this, requestedAcc);

        //если пора повернуть
        while(lineWhereCarTurns != null) {
            Node lastpos = getPosWithShift();
            makeStep(currStep);
            shift += shiftStep;
            Node newpos = getPosWithShift();
            double a = lineWhereCarTurns[0], b = lineWhereCarTurns[1], c = lineWhereCarTurns[2];
           // System.out.println((a*lastpos.getLon() + b*lastpos.getLat() + c) + " " +
            //    + (a*newpos.getLon() + b*newpos.getLat() + c));
            if ((a*lastpos.getLon() + b*lastpos.getLat() + c)*(a*newpos.getLon() + b*newpos.getLat() + c) <= 0) {
                turns++;
                prev = routeArray.get(prevNodeIndex);
                next = routeArray.get(prevNodeIndex+1);
                if(routeArray.size() > prevNodeIndex+2) {
                    Node nextnext = routeArray.get(prevNodeIndex + 2);
                    Node intersection = distance.linesIntersection(lastpos, newpos, lineWhereCarTurns);
                    Node proj1 = distance.projection(prev, next, intersection);
                   // System.out.println("mistake: " + distance.distanceBetween(pos,proj1));
                    Node proj2 = distance.projection(next, nextnext, intersection);
                    double x1 = prev.getLon(), y1 = prev.getLat();
                    double x2 = next.getLon(), y2 = next.getLat();
                    double x0 = intersection.getLon(), y0 = intersection.getLat();
                    double aa = x2 - x1;
                    double bb = y2 - y1;
                    double cc = -a*x0 - b*y0;
//                    List<Node> dbg = new ArrayList<>();
//                    Node nd = new Node(0, prev.getLat() + 0.01, prev.getLon());
//                    dbg.add(intersection);
//                    dbg.add(nd);
//                    nd = new Node(0, prev.getLat(), prev.getLon() + 0.01);
//                    dbg.add(nd);
//                    dbg.add(prev);
//                    System.out.println(dbg.get(0).getLat() + " " + dbg.get(5).getLat());
//                    sessionObjects.getClientProcessor().drawNodesTogether(dbg, 7);
//
//                    dbg.add(proj1);
//                    dbg.add(proj2);
//                    dbg.add(intersection);

                    pos = proj2;
                    double angle = distance.angle(prev, next) - distance.angle(next,nextnext);
                    if(distance.distanceBetween(intersection, getPosWithShift()) > 10) {
                        System.out.println(" jump: " + distance.distanceBetween(intersection, getPosWithShift()) + " angle: " + angle);
                        //sessionObjects.getClientProcessor().drawNodesTogether(dbg, 6);
                    }
			prevNodeIndex++;
                    currStep -= distance.distanceBetween(lastpos, intersection);
                    toNextNode = distance.distanceBetween(proj2, nextnext);
                    double oldshift = shift;
                    shift = distance.distToLine(next, nextnext, intersection);
                    if(graph.oneway(next, nextnext))
                        if(oldshift < 0)
                            shift *= -1;

                    //System.out.println("shift after turn: " + shift);
                    setLineWhereCarTurns();
                    driver.setDesiredLane();

                    orientation = distance.angle(next, nextnext);
                } else {
                    prevNodeIndex++;
                    return;
                }
            } else {
                toNextNode -= currStep;
                break;
            }
        }


		/*while (currStep >= toNextNode) {

            setMinMaxLanes();
			if(prevNodeIndex == destIndex)
			    return;
            pos.setLat(routeArray.get(prevNodeIndex).getLat());
            pos.setLon(routeArray.get(prevNodeIndex).getLon());

          /*  prev = routeArray.get(prevNodeIndex);
            next = routeArray.get(prevNodeIndex+1);
            Way way = sessionObjects.getGraph().wayWhereAreBothNodesArePlaced(prev, next);
            List<Node> nodes = way.getNodes();

            if(nodes.indexOf(prev) < nodes.indexOf(next)) {
                lane  = 1;
            } else {
                lane = -1;
            }*/

           // correctPosAccountForShift();


		/*	toNextNode = distance.distanceBetween(routeArray.get(prevNodeIndex),
					routeArray.get(prevNodeIndex + 1));
		}

		prev = routeArray.get(prevNodeIndex);
		next = routeArray.get(prevNodeIndex + 1);
		ratio = currStep / distance.distanceBetween(prev, next);
		dy = distance.latDegToMeters(next.getLat() - prev.getLat()) * ratio;
		dx = distance.lonDegToMeters(next.getLon() - prev.getLon()) * ratio;
		pos.setLat(pos.getLat() + distance.metersToLatDeg(dy));
		pos.setLon(pos.getLon() + distance.metersToLonDeg(dx));
		toNextNode -= currStep;*/


	};

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public void setCurrStep(double currStep) {
		this.currStep = currStep;
	}
    public void setShiftStep(double shiftStep) {
        this.shiftStep = shiftStep;
    }

	public double getSpeed() {
		return speed;
	}
    public double getShift() {
        return shift;
    }
    public double getShiftSpeed() {
        return shiftSpeed;
    }

    public List<Double> getMaxspeeds() {
        return maxspeeds;
    }

    public ArrayList<Node> getRouteList() {
		return routeArray;
	}

    public int getPrevNodeIndex() {
        return prevNodeIndex;
    }

    protected void setMaxSpeeds() {
	    Graph graph  = sessionObjects.getGraph();
        Way way;
        maxspeeds = new ArrayList<>();
        Distance distance = sessionObjects.getDistance();
        double direction = 0, newDirection, angle, dx, dy, radius, maxspeed;
        Node currNode, nextNode;
        double frictionK = sessionObjects.getPhysics().getFrictionCoef();
        for(int i = 0; i < routeArray.size() - 1; i++) {
            currNode = routeArray.get(i);
            nextNode = routeArray.get(i+1);
            dy = distance.latDegToMeters(nextNode.getLat() - currNode.getLat());
            dx = distance.lonDegToMeters(nextNode.getLon() - currNode.getLon());
            newDirection = Math.acos(dx/Math.sqrt(dx*dx+dy*dy));
            if(dy < 0)
                newDirection *= -1;
            if(i == 0)
                direction = newDirection;
            angle = newDirection - direction;
            if(Math.abs(angle) < 0.001)
                maxspeed = 800;//Integer.MAX_VALUE;
            else {
                radius = ModelConstants.LINE_BREADTH / (1 - Math.cos(angle / 2));
                maxspeed = Math.sqrt(mu*9.8*radius*frictionK/3);
            }
            way = graph.wayWhereAreBothNodesArePlaced(currNode, nextNode);
            String highway = way.getTags().get("highway");
            if (highway.equals("service") || highway.equals("living street") || highway.equals("residential"))
                maxspeed /= 2;
            maxspeeds.add(maxspeed);
            direction = newDirection;

        }
        if(routeArray.get(routeArray.size()-1).getId().equals("N-13")) {
            maxspeeds.add(ModelConstants.CITY_MAX_SPEED);
        } else maxspeeds.add(0.5);
    }

    /**
     * Толкает машину вперёд на step.
     * Изменяет только поле pos
     * @param step
     */

    protected void makeStep(double step) {
	    Distance distance = sessionObjects.getDistance();
        Node prev = routeArray.get(prevNodeIndex);
        Node next = routeArray.get(prevNodeIndex + 1);
        double ratio = step / distance.distanceBetween(prev, next);
        double dy = distance.latDegToMeters(next.getLat() - prev.getLat()) * ratio;
        double dx = distance.lonDegToMeters(next.getLon() - prev.getLon()) * ratio;
        pos.setLat(pos.getLat() + distance.metersToLatDeg(dy));
        pos.setLon(pos.getLon() + distance.metersToLonDeg(dx));
    }

    /**
     *
     * @return {a, b, c}
     */

    protected double[] setLineWhereCarTurns() {
        Distance distance = sessionObjects.getDistance();
        Graph graph = sessionObjects.getGraph();

	    double x1, y1, x2, y2, a1, b1, c1, a2, b2, c2, d, phi1, phi2, angle, a, b, c;

	    double  breadth1, breadth2;

	    Node prev = routeArray.get(prevNodeIndex);
	    Node next = routeArray.get(prevNodeIndex+1);
        if(routeArray.size() <= prevNodeIndex + 2) {
            lineWhereCarTurns = distance.perp(prev,next,next);
            return lineWhereCarTurns;
        }
	    Node nextnext = routeArray.get(prevNodeIndex+2);

        //если поворот налево - сводим к случаю, когда направо

        phi1 = distance.angle(prev, next);
        phi2 = distance.angle(next, nextnext);
        angle = phi2 - phi1;
        if(angle > 0) {
            Node tmp = nextnext;
            nextnext = prev;
            prev = tmp;
        }
        //если нет поворота. 0.01 - даёт погрешность около метра на 100 метров
        if(Math.abs(angle) < 0.01) {
            lineWhereCarTurns = distance.perp(prev,next,next);
            return lineWhereCarTurns;
        }
        Way way1 =  graph.wayWhereAreBothNodesArePlaced(prev, next);
        Way way2 =  graph.wayWhereAreBothNodesArePlaced(next, nextnext);
        List<Node> nodes1 = way1.getNodes();
        List<Node> nodes2 = way2.getNodes();
        breadth1 = ModelConstants.LINE_BREADTH*
                ((nodes1.indexOf(prev) < nodes1.indexOf(next)) ?
                        graph.numOfLanesFwd(way1) : graph.numOfLanesBwd(way1));
        breadth2 = ModelConstants.LINE_BREADTH*
                ((nodes1.indexOf(next) < nodes1.indexOf(nextnext)) ?
                        graph.numOfLanesFwd(way2) : graph.numOfLanesBwd(way2));


	    /* получаем уравения прямых */

        //координаты узлов
        x1 = prev.getLon(); y1 = prev.getLat();
        x2 = next.getLon(); y2 = next.getLat();


        //координаты единичного вектора(в метрах) направленного вдоль первого направления
        double v1x = x2 - x1;
        double v1y = y2 - y1;
        v1x =  distance.lonDegToMeters(v1x);
        v1y = distance.latDegToMeters(v1y);
        double length = Math.sqrt(v1x*v1x + v1y*v1y);
        v1x /= length;
        v1y /= length;

        //координаты вектора смещения (в градусах) границы дороги от линии, соединяющей узлы
        double nx = v1y;
        double ny = -v1x;
        nx *= breadth1;
        ny *= breadth1;
        nx = distance.metersToLonDeg(nx);
        ny = distance.metersToLatDeg(ny);

        //координаты точек на границе первой дороги

        x1 += nx; y1 += ny; x2 += nx; y2 += ny;


        a1 = y1  - y2;
        b1 = x2 - x1;
        c1 = x1*y2 - x2*y1;

        //аналогично для второй дороги
        x1 = next.getLon(); y1 = next.getLat();
        x2 = nextnext.getLon(); y2 = nextnext.getLat();
        double v2x = x2 - x1; double v2y = y2 - y1;
        v2x =  distance.lonDegToMeters(v2x); v2y = distance.latDegToMeters(v2y);
        length = Math.sqrt(v2x*v2x + v2y*v2y);
        v2x /= length; v2y /= length;
        nx = v2y; ny = -v2x;
        nx *= breadth1; ny *= breadth1;
        nx = distance.metersToLonDeg(nx); ny = distance.metersToLatDeg(ny);
        x1 += nx; y1 += ny; x2 += nx; y2 += ny;

        a2 = y1  - y2;
        b2 = x2 - x1;
        c2 = x1*y2 - x2*y1;

        //ищем пересечение

        d = a1*b2 - a2*b1;

        if(d == 0) {
           lineWhereCarTurns = null;
           return null;
        }

        //первая точка прямой
        double x01 = (b1 * c2 - b2 * c1) / d;
        double y01 = (a2 * c1 - a1 * c2) / d;

        //вторая точка прямой(сдвинута вдоль внешней биссектрисы угла поворота)
        double x02 = x01 + distance.metersToLonDeg(v1x - v2x);
        double y02 = y01 + distance.metersToLatDeg(v1y - v2y);

        a = y01  - y02;
        b = x02 - x01;
        c = x01*y02 - x02*y01;

        double[] line = {a, b, c};
        lineWhereCarTurns = line;

        //отправим прямую(для отладки)
        /*if(a == 0) {
            System.out.println("lineWhereCarTurns : a == 0");
            return null;

        }
        */
//        if(true) {
//
//
//            List<Node> dbg = new ArrayList<>();
//            Node nd = new Node(0, y01, x01);
//            dbg.add(nd);
//            nd = new Node(0, y02, x02);
//            dbg.add(nd);
//            // sessionObjects.getClientProcessor().drawNodesTogether(dbg, 2);
//
//            for (int i = 0; i < 100; i++) {
//                double yy = ((double) i) / 1000000;
//                nd = new Node(0, y01 + yy, (-b * (y01 + yy) - c) / a);
//                dbg.add(nd);
//                nd = new Node(0, y01 - yy, (-b * (y01 - yy) - c) / a);
//                dbg.add(nd);
//            }
//            //System.out.println(Thread.currentThread().getName());
//            sessionObjects.getClientProcessor().drawNodesTogether(dbg, 5);
//        }

        return lineWhereCarTurns;

    }

    protected void correctPosAccountForShift() {
	    Node prev = routeArray.get(prevNodeIndex);
	    Node next = routeArray.get(prevNodeIndex+1);
	    Distance distance = sessionObjects.getDistance();



        double ax = next.getLon() - prev.getLon();
        double ay = next.getLat() - prev.getLat();
        ax =  distance.lonDegToMeters(ax);
        ay = distance.latDegToMeters(ay);
        double length = Math.sqrt(ax*ax + ay*ay);
        ax /= length;
        ay /= length;

        //единичный вектор нормали, полученный повортом на 90 по часовой стрелке от вектора направления(торчит вправо)
        double nx = ay;
        double ny = -ax;

        //System.out.println("nx : " + nx + " ny: " + ny);

      //  pos.setLon(pos.getLon() + distance.metersToLonDeg(nx)*Math.abs(lane)*ModelConstants.LINE_BREADTH_DRAW);
      //  pos.setLat(pos.getLat() + distance.metersToLatDeg(ny)*Math.abs(lane)*ModelConstants.LINE_BREADTH_DRAW);

    }

    protected void setMinMaxLanes() {
        Distance distance = sessionObjects.getDistance();
        Graph graph = sessionObjects.getGraph();
        if(routeArray.size() - prevNodeIndex > 2) {
            Node prev = routeArray.get(prevNodeIndex);
            Node next = routeArray.get(prevNodeIndex+1);
            Node nextnext = routeArray.get(prevNodeIndex+2);
            double phi1 = distance.angle(prev, next);
            double phi2 = distance.angle(next, nextnext);
            double angle = phi2 - phi1;
            int lanesOnThisWay = graph.numOfLanesBetween(prev, next);
            int lanesOnNextWay = graph.numOfLanesBetween(next, nextnext);
            //если поворот направо
            if(angle < 0) {
                minlane = 1;
                maxlane = lanesOnNextWay;
            } else {
                maxlane = lanesOnThisWay;
                if(lanesOnThisWay > lanesOnNextWay) minlane = lanesOnThisWay -  lanesOnNextWay + 1;
                else minlane = 1;
            }
        } else {
            minlane = -228;
            maxlane = -228;
        }
    }

    protected void setShift() {
        if(lane.size() > 1) return;
        if(prevNodeIndex + 1 >= routeArray.size()) return;
       // System.out.println("curr lane : "+lane.get(0)+" prevndindex: "+prevNodeIndex+" routesize: "+routeArray.size());
        shift = calcShift(lane.get(0));
    }
    protected double calcShift(int ln) {
        Graph graph = sessionObjects.getGraph();
        Node prev = routeArray.get(prevNodeIndex);
        if(prevNodeIndex + 1 >= routeArray.size()) return -1;
        Node next = routeArray.get(prevNodeIndex + 1);
        boolean oneway = graph.oneway(prev, next);
        double lanes = graph.numOfLanesBetween(prev, next);
        //System.out.println("lanes on this way: " + lanes + " oneway: " + oneway);
        if(oneway) {
            return (lanes/2 - Math.abs(ln) + 0.5)* ModelConstants.LINE_BREADTH;
        } else {
            double sh = (lanes - Math.abs(ln) + 0.5) * ModelConstants.LINE_BREADTH;
          //  System.out.println("sh: " + sh);
            return (lanes - Math.abs(ln) + 0.5) * ModelConstants.LINE_BREADTH;
        }
    }
    public void setLane() {

        /*if(lane.size() > 1) {
            if (Math.abs(shift - calcShift(lane.get(0))) > ModelConstants.LINE_BREADTH) {
                lane.remove(0);
            }
        }*/
        double ln = calcLane();
        if(ln == 0) return;
        lane = new LinkedList<>();
        if(!shifting) {
            if (Math.abs(ln - Math.round(ln))*ModelConstants.LINE_BREADTH <= ModelConstants.SHIFT_MISTAKE) {
                lane.add((int)Math.round(ln));
            } else {
                shifting = true;
                setLane();
                return;
               // System.out.println("error: shifting naebalova : " + Math.abs(ln - Math.round(ln))*ModelConstants.LINE_BREADTH);

            }
        } else {
            lane = new LinkedList<>();
            lane.add((int) ln);
            lane.add((int) ln + 1);
        }
    }
    public double calcLane() {
        Graph graph = sessionObjects.getGraph();
        Node prev = routeArray.get(prevNodeIndex);
        if(prevNodeIndex + 1 >= routeArray.size()) return 0;
        Node next = routeArray.get(prevNodeIndex + 1);
        boolean oneway = graph.oneway(prev, next);
        double lanes = graph.numOfLanesBetween(prev, next);
        double ln;
        if(oneway) ln = (lanes + 1.0)/2 - shift/ModelConstants.LINE_BREADTH;
        else ln = lanes + 1/2 - shift/ModelConstants.LINE_BREADTH;
        return ln;
    }
    public Node getPosWithShift() {
        Node prev = routeArray.get(prevNodeIndex);
        Node next = routeArray.get(prevNodeIndex+1);
        Distance distance = sessionObjects.getDistance();

        double ax = next.getLon() - prev.getLon();
        double ay = next.getLat() - prev.getLat();
        ax =  distance.lonDegToMeters(ax);
        ay = distance.latDegToMeters(ay);
        double length = Math.sqrt(ax*ax + ay*ay);
        ax /= length;
        ay /= length;

        //единичный вектор нормали, полученный повортом на 90 по часовой стрелке от вектора направления(торчит вправо)
        double nx = ay;
        double ny = -ax;
        double lon = pos.getLon() + distance.metersToLonDeg(nx)*shift;
        double lat = pos.getLat() + distance.metersToLatDeg(ny)*shift;
        Node node = new Node(0 , lat, lon);
        return node;

    }

    public Node getVelocityVector() {
        Node prev = routeArray.get(prevNodeIndex);
        Node next = routeArray.get(prevNodeIndex+1);
        Distance distance = sessionObjects.getDistance();

        double ax = next.getLon() - prev.getLon();
        double ay = next.getLat() - prev.getLat();
        ax =  distance.lonDegToMeters(ax);
        ay = distance.latDegToMeters(ay);
        double length = Math.sqrt(ax*ax + ay*ay);
        ax /= length;
        ay /= length;

        double vx = ax*speed;
        double vy = ay*speed;

        //единичный вектор нормали, полученный повортом на 90 по часовой стрелке от вектора направления(торчит вправо)
        double nx = ay;
        double ny = -ax;
        vx += nx*shiftSpeed;
        vy += ny*shiftSpeed;
        double lon = distance.metersToLonDeg(vx);
        double lat = distance.metersToLatDeg(vy);
        Node node = new Node(0 , lat, lon);
        return node;
    }

    public double getToNextNode(){
        return toNextNode;
    }
}
