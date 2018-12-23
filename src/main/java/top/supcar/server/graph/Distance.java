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

package top.supcar.server.graph;

import info.pavie.basicosmparser.model.Element;
import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Way;
import top.supcar.server.session.SelectedRect;

import java.util.*;

/**
 * Operations with coordinates and distances
 *
 */
public class Distance {


	private double metersPerDegLat;
	private double metersPerDegLon;

	public Distance(SelectedRect selectedRect) {

		Node x0y0 = new Node(0, selectedRect.getLowerLeft().getLat(), selectedRect
				.getLowerLeft().getLon());
		Node x1y0 = new Node(0, selectedRect.getUpperRight().getLat(), selectedRect
				.getLowerLeft().getLon());

		Node x0y1 = new Node(0, selectedRect.getLowerLeft().getLat(), selectedRect
				.getUpperRight().getLon());

		metersPerDegLat = distanceBetween(x0y0, x1y0)/(x1y0.getLat() - x0y0.getLat());
		metersPerDegLon = distanceBetween(x0y0, x0y1)/(x0y1.getLon() - x0y0.getLon());
	}
	/**
	 * @param a start
	 * @param b end
	 * @return distance in meters
	 */
	public double distanceBetween(Node a, Node b) {
		double R = 6371000; // Earth's radius
		double TRANS = Math.PI/180;

		double lat1 = a.getLat()*TRANS;
		double lat2 = b.getLat()*TRANS;
		double dlat = lat2 - lat1;
		double dlon = (b.getLon()- a.getLon())*TRANS;

		double x = Math.sin(dlat/2) * Math.sin(dlat/2) +
				Math.cos(lat1) * Math.cos(lat2) *
						Math.sin(dlon/2) * Math.sin(dlon/2);
		double c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));

		double d = R * c;
		return  d;
	}

    /**
     * Возвращает угол между осью абсцисс и ветором n1n2
     * @param n1
     * @param n2
     * @return
     */

	public double angle(Node n1, Node n2) {

	    double angle;

        double dx = lonDegToMeters(n2.getLon() - n1
                .getLon());
        double dy = latDegToMeters(n2.getLat() - n1
                .getLat());

        angle = Math.acos(dx/Math.sqrt(dx*dx+dy*dy));

        if(dy < 0)
            angle *= -1;

        return angle;
    }

    public Node linesIntersection(Node n1, Node n2, double[] line2) {
	    double a2 = line2[0], b2 = line2[1], c2 = line2[2];
	    double[] abc = lineVertToAbc(n1, n2);
        double a1 = abc[0], b1 = abc[1], c1 = abc[2];

        //ищем пересечение

        double d = a1*b2 - a2*b1;

        if(d == 0) {
            return null;
        }

        double x01 = (b1 * c2 - b2 * c1) / d;
        double y01 = (a2 * c1 - a1 * c2) / d;
        Node nd = new Node(0, y01, x01);
        return nd;
    }

    public double[] lineVertToAbc(Node n1, Node n2){
        double x1 = n1.getLon(), y1 = n1.getLat(), x2 = n2.getLon(), y2 = n2.getLat();
        double a = y1 - y2;
        double b = x2 - x1;
        double c = x1*y2 - x2*y1;

        return new double[]{a, b, c};
    }

    public double distToLine(Node lineN1, Node lineN2, Node node) {
	  return distanceBetween(node, projection(lineN1, lineN2, node));
    }

    public Node projection(Node lineN1, Node lineN2, Node node) {
        double[] line = perp(lineN1, lineN2, node);
        return linesIntersection(lineN1, lineN2, line);
    }
    public double[] perpVect(Node lineN1, Node lineN2) {
        double x1 = lineN1.getLon(), y1 = lineN1.getLat();
        double x2 = lineN2.getLon(), y2 = lineN2.getLat();
        double a = x2 - x1;
        a *= lonDegToMeters(1);
        double b = y2 - y1;
        b *= latDegToMeters(1);
        double n1 = -b;
        double n2 = a;
        n1 /= lonDegToMeters(1);
        n2 /= latDegToMeters(1);

        return new double[]{n1, n2};
    }
    public double[] perp(Node lineN1, Node lineN2, Node node) {
        double x0 = node.getLon(), y0 = node.getLat();
        double[] normVect = perpVect(lineN1, lineN2);
        double b = -normVect[0];
        double a = normVect[1];
        double c = -a*x0 - b*y0;
        double[] line = {a, b, c};

        return line;
    }

	public double latDegToMeters(double deg) {
		return deg*metersPerDegLat;
	}
	public double lonDegToMeters(double deg) {
		return deg*metersPerDegLon;
	}
	public double metersToLatDeg(double meters) {
		return meters/metersPerDegLat;
	}
	public double metersToLonDeg(double meters) {
		return meters/metersPerDegLon;
	}
				/*
				/**
					*
					* For each pair Way, Node
					* counts distance between this node(along this way) from the fist node of the way
					* The key is concatenation of WayId and NodeInd(Use method getId() )
					*
					* @param  roads from OSMData.getRoads
					* @return  Map with concatenation (WayId+NodeId) as key and way length in meters
					* as value
					*/
/*
				public static Map< String, Double> setMilestones(Map<String,Way> roads) {

								Map< String, Double> milestones = new HashMap< String, Double>();

								Map.Entry currEntry;
								Way currWay;
								Node node;
								String pair;
								int first_iteration;
								double wayLenght;

								Iterator roadsIt = roads.entrySet().iterator();
								Iterator dorogaIt;
								List<Node> dorogaAsList;
								Node prev = null;

								while (roadsIt.hasNext()) {
												currEntry = (Map.Entry) roadsIt.next();
												currWay = (Way) currEntry.getValue();
												dorogaAsList = currWay.getNodes();
												dorogaIt = dorogaAsList.listIterator();
												first_iteration = 1;
												wayLenght = 0;

												while (dorogaIt.hasNext()) {
																node = (Node) dorogaIt.next();
																pair = currWay.getId();
																pair += node.getId();
																if (first_iteration == 1) {
																				prev = node;
																				milestones.put(pair, (double) 0);

																}
																wayLenght += distanceBetween(node, prev);
																if (!milestones.containsKey(pair))
																				milestones.put(pair, wayLenght);

																prev = node;
																first_iteration = 0;
												}


								}

								return milestones;
				}
*/
}
