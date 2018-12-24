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

import java.util.*;

import com.sun.org.apache.xpath.internal.operations.Mod;
import info.pavie.basicosmparser.model.*;
import info.pavie.basicosmparser.model.Node;
import top.supcar.server.session.SessionObjects;
import top.supcar.server.dijkstra.Dijkstra;
import top.supcar.server.model.ModelConstants;

/**
 * This class allows you to make graph which consists of {@link Node} and edges between them <br>
 * The graph are being built according to {@link info.pavie.basicosmparser.BasicOSMParser} data <br>
 * The edges correspond to {@link Relation} between {@link Node} <br>
 * Four objects are being created for one graph <br>
 * @author nataboll
*/

public class Graph extends PriorityQueue{
//ATTRIBUTES
    /** The Earth radius **/
    private static final double R = 6371000;
    /** Is used to convert angular measure **/
    private static final double TRANS = Math.PI/180;
    /** An adjacency list **/
    private Map<Node, List<Node>> adjList;
    /** For getting data from {@link info.pavie.basicosmparser.BasicOSMParser}**/
    private Map<String, Way> interMap;
    /** List of weights, the keys are {@link Node} ID concatenation **/
    private Map<String, Double> weightList;
    /** List of vertexes of {@link Node} type **/
    private List<Node> vertexList;
    /** Contains all the roads for every vertex **/
    private Map<Node, List<Way>> nodeWays;
    /** The class is connected with {@link Dijkstra} having it as a field **/
    private Dijkstra dijkstra;
    /** The field is for getting the saved parameters of this session **/
    private SessionObjects sessionObjects;

    public Graph(Map<String,Way> map, SessionObjects sessionObjects){
        this.sessionObjects = sessionObjects;
        setInterMap(map);
        setMap();
        setVertexList();
        setNodeWays();
        setWeightList();
        dijkstra = new Dijkstra(this);
    }

//OTHER METHODS
    /**
     * @param map is created in setMap
     */
    public void setInterMap(Map<String,Way> map) {
        interMap = map;
    }

    /**
     * @return data of {@link info.pavie.basicosmparser.BasicOSMParser}
     */
    public Map<String, Way> getInterMap(){
        return this.interMap;
    }

    /**
     * Generates adjList iterating by interMap (after {@link info.pavie.basicosmparser.BasicOSMParser})
     */
    public void setMap(){

        adjList = new HashMap<>();
        Iterator<Map.Entry<String, Way>> interMapIter = interMap.entrySet().iterator();
        Iterator<Node> roadIter;
        Map.Entry<String, Way> currEntry;
        Way currWay;
        List<Node> road;

        vertexList = new ArrayList<>();
        weightList = new HashMap<>();

        while (interMapIter.hasNext()) {
            currEntry = interMapIter.next();
            currWay = currEntry.getValue();
            String tmp = currWay.getTags().get("oneway");
            boolean twoway = (tmp == null) || (tmp.equals("no"));

            road = currWay.getNodes();

            roadIter = road.listIterator();
            Node currNode;
            Node nextNode;

            if (roadIter.hasNext()) {
                currNode = roadIter.next();

                while (roadIter.hasNext()) {

                    nextNode = roadIter.next();

                    addInAdjList(currNode, nextNode);

                    if (twoway) {
                        addInAdjList(nextNode, currNode);
                    }

                    currNode = nextNode;
                }
            }
        }
    }

    /**
     * Adds 'b' in the list of 'a' is connected with <br>
     * @param a is where to add <br>
     * @param b is what to add
     */
    private void addInAdjList(Node a, Node b) {
        List<Node> adjNodes = adjList.get(a);
        if(adjNodes == null) {
            adjNodes = new ArrayList<>();
            adjList.put(a, adjNodes);
        }
        adjNodes.add(b);
    }

    /**
     * @return list of edges' weights.
     */
    public Map<String, Double> getWeightList(){
        return weightList;
    }

    /**
     * @return adjacency list.
     */
    public Map<Node, List<Node>> getAdjList(){
        return this.adjList;
    }

    /**
     * Creating list of vertexes iterating by adjList.
     */
    public void setVertexList() {
        Iterator<Map.Entry<Node, List<Node>>> listIter = adjList.entrySet().iterator();
        while (listIter.hasNext()) {
            Node cNode = listIter.next().getKey();
            Node nNode;
            List<Node> road = adjList.get(cNode);
            Iterator roadIter = road.iterator();
            if (!vertexList.contains(cNode))
                vertexList.add(cNode);

            while (roadIter.hasNext()){
                nNode = (Node) roadIter.next();
                if (!vertexList.contains(nNode))
                    vertexList.add(nNode);
            }
        }
    }

    /**
     * @return list of vertexes.
     */
    public List<Node> getVertexList(){
        return vertexList;
    }

    /**
     * Creates weightList iterating by adjList.
     */
    public void setWeightList(){
        Iterator<Map.Entry<Node, List<Node>>> listIter = adjList.entrySet().iterator();
        while (listIter.hasNext()) {
            Node cNode = listIter.next().getKey();
            Node nNode;
            List<Node> road = adjList.get(cNode);
            Iterator roadIter = road.iterator();

            while (roadIter.hasNext()){
                nNode = (Node) roadIter.next();
                weightList.put(cNode.getId()+nNode.getId(), getWeight(cNode, nNode));
            }
        }
    }

    /**
     * Finds all the roads for every vertex.
     */
    public void setNodeWays(){
        nodeWays = new HashMap<>();
        Iterator<Map.Entry<String, Way>> interMapIter = interMap.entrySet().iterator();
        Iterator<Node> roadIter;
        Map.Entry<String, Way> currEntry;
        Way currWay;
        List<Node> road;

        while (interMapIter.hasNext()) {
            currEntry = interMapIter.next();
            currWay = currEntry.getValue();
            road = currWay.getNodes();

            roadIter = road.listIterator();
            Node currNode;

            while (roadIter.hasNext()){
                currNode = roadIter.next();
                List<Way> wayList = nodeWays.get(currNode);
                if (wayList==null){
                    wayList = new ArrayList<>();
                    nodeWays.put(currNode, wayList);
                }
                wayList.add(currWay);
            }
        }
    }

    public Map<Node, List<Way>> getNodeWays(){
        return nodeWays;
    }

    /**
     * Finds the shortest way between two vertexes using {@link Dijkstra} <br>
     * @param a is the start <br>
     * @param b is the end <br>
     * @return the way
     */
    public List<Node> getWay(Node a, Node b) {
        return dijkstra.getWay(a, b);
    }

    public Node nearestNode(Node node) {
        double distance = 1e10;
        double currentDist;
        Node nearest = new Node(0,0,0);
        for (int i = 0; i < vertexList.size(); i++) {
            currentDist = Distance.distanceBetween(vertexList.get(i), node);
            if (currentDist < distance) {
                distance = currentDist;
                nearest = vertexList.get(i);
            }
        }
        return nearest;
    }

    public Way wayWhereAreBothNodesArePlaced(Node a, Node b) {
        Way wayWhereAreBothNodesArePlaced = null;
        List<Way> list1 = nodeWays.get(a), list2 = nodeWays.get(b);
        for(Way way1  : list1) {
            for(Way way2 : list2) {
                if(way1 == way2)
                    wayWhereAreBothNodesArePlaced = way1;
            }
        }
        return  wayWhereAreBothNodesArePlaced;
    }

    /**
     *
     * @param way
     * @return lanes forward or 1 if lanes == 1 and road isn't oneway
     */

    public int numOfLanesFwd(Way way) {
        String lanes = way.getTags().get("lanes:forward");
        String onewayStr = way.getTags().get("oneway");
        boolean oneway = (onewayStr == null || onewayStr.equals("no"));

        if (lanes != null) return Integer.parseInt(lanes);

        lanes = way.getTags().get("lanes");
        //if(lanes != null)
       // System.out.println(Integer.parseInt(lanes));

        if (lanes != null) {
            if(!oneway) return Math.max(Integer.parseInt(lanes) / 2, 1);
             else return Integer.parseInt(lanes);
        }
        else return 1;
    }
    /**
     *
     * @param way
     * @return lanes backward or 1 if lanes == 1 and road isn't oneway
     */

    public int numOfLanesBwd(Way way) {
        boolean oneway = oneway(way);
        String lanes = way.getTags().get("lanes:backward");

        if(oneway) return 0;
        else if(lanes != null) return Integer.parseInt(lanes);
        lanes = way.getTags().get("lanes");
        if(lanes != null) return Math.max(Integer.parseInt(lanes)/2, 1);
        else return 1;
    }

    public int numOfLanesBetween(Node n1, Node n2) {
        Way way = wayWhereAreBothNodesArePlaced(n1, n2);
        if(way.getNodes().indexOf(n1) < way.getNodes().indexOf(n2))
            return numOfLanesFwd(way);
        else
            return numOfLanesBwd(way);
    }
    public boolean oneway(Way way) {
        String onewayStr = way.getTags().get("oneway");
        return (onewayStr != null && onewayStr.equals("yes"));
    }
    public boolean oneway(Node n1, Node n2) {
        return oneway(wayWhereAreBothNodesArePlaced(n1,n2));
    }


    private double getWeight(Node currNode, Node nextNode) {

        double speed = getAvgSpeed(wayWhereAreBothNodesArePlaced(currNode, nextNode));

        return sessionObjects.getDistance().distanceBetween(currNode, nextNode)/speed;
    }

    private double getAvgSpeed(Way way) {
        double speed = ModelConstants.CITY_MAX_SPEED/2;
        String highway = way.getTags().get("highway");
        if(highway.equals("service") || highway.equals("living street") || highway.equals("residential")) {
            speed /= 3;
        }
        return speed;

    }
}