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

import com.sun.org.apache.xpath.internal.SourceTree;
import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Way;
import top.supcar.server.session.SessionObjects;
import top.supcar.server.graph.Distance;
import top.supcar.server.graph.Graph;
import top.supcar.server.holder.CarHolder;
import top.supcar.server.model.Car;
import top.supcar.server.model.ModelConstants;
import top.supcar.server.model.RoadThing;
import top.supcar.server.update.WorldUpdater;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static java.lang.Math.max;

public class CarSetter {
    private SessionObjects sessionObjects;
    private CityCarFactory ccFactory;
    private int busyLvl;
    private List<Node> sources;
    private List<Node> sinks;
    private List<Double> periods; //how often cars should arrive at each source
    private Instant lastInstant;
    private List<Double> timePassed;
    public List<Boolean> carArrived;
    private List<Integer> sinksPriorities;
    private List<Integer> sinkPosInQueue;
    private List<Map> manualSettedRoutes;
    private Map<Node, Node> manualSettedSinksSources;
    private List<Integer> carsCounter;
    private int capacity = 1;

    public CarSetter(SessionObjects sessionObjects, int busyLvl, List<Map> routes) {
        this.busyLvl = busyLvl;
        this.sessionObjects = sessionObjects;
        this.manualSettedRoutes = routes;
        ccFactory = new CityCarFactory(sessionObjects);
        findSrcsSinks();
        setCars();
    }

    /**
     * устанавливает периоды, с которыми в точках старта появляются машины
     */
    private void setPeriods() {
        if (sources == null || sinks == null) {
            findSrcsSinks();
        }
        if (manualSettedRoutes.size() == 0) {
            setPeriodsAuto();
        } else {
            System.out.println("msr size: " + manualSettedRoutes.size());
            setPeriodsManual();
        }

    }

    private void setPeriodsManual() {
        periods = new ArrayList<>(sources.size());
        carsCounter = new ArrayList<>(Collections.nCopies(sources.size(), 0));
        timePassed = new ArrayList<>(Collections.nCopies(sources.size(), 0.0));
        carArrived = new ArrayList<>(Collections.nCopies(sources.size(), false));
        for(int i = 0; i < sources.size(); i++) {
            periods.add(max(0.2, (Double)manualSettedRoutes.get(i).get("delay")));
        }

    }

    private void setPeriodsAuto() {
        if (periods == null) {
            System.out.println(sources.size());
            periods = new ArrayList<>(sources.size());
            for(int i = 0; i < sources.size(); i++) {
                periods.add(0.0);
            }

        }
        Node node;
        double period;
        for (int i = 0; i < sources.size(); i++) {
            node = sources.get(i);
            period = findPeriod(node);

            periods.set(i, period);
        }
        timePassed = new ArrayList<>(sources.size());
        for(int i = 0; i < sources.size(); i++) {
            timePassed.add(0.0);
        }
        carArrived = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            carArrived.add(false);
        }
    }

    /**
     * подготавливает массив, определяющий вероятность назначения машине данной точки назначения
     */
    private void setSinksPriorities() {
        if (manualSettedRoutes.size() > 0)
                return;

        List<Double> periods = new ArrayList<>(sinks.size());
        sinksPriorities = new ArrayList<Integer>(sinks.size());
        sinkPosInQueue = new ArrayList<>(sinks.size());
        double period, minperiod = Double.MAX_VALUE;
        int priority;
        for(int i = 0; i < sinks.size(); i++) {
            period = findPeriod(sinks.get(i));
            periods.add(period);
           // if (period > maxperiod) maxperiod = period;
            if (period < minperiod) minperiod = period;
        }
        //maxMinRatio = maxperiod/minperiod;
        System.out.println("minperiod: " + minperiod);
        for(int i = 0; i < sinks.size(); i++) {
            priority = (int)(periods.get(i)/minperiod);
            sinksPriorities.add(priority);
            sinkPosInQueue.add(priority);
        }



    }

    private double findPeriod(Node node) {
        double period, newPeriod;
        Graph graph = sessionObjects.getGraph();
        Map<Node, List<Way>> nodeWays = graph.getNodeWays();
        period = Integer.MAX_VALUE;
        for (Way way : nodeWays.get(node)) {
            switch (way.getTags().get("highway")) {
                case "service":
                    newPeriod = CreationParams.SERVICE_SPAWN_PERIOD;
                    break;
                case "living_street":
                    newPeriod = CreationParams.LIVING_STREET_SPAWN_PERIOD;
                    break;
                case "residential":
                    newPeriod = CreationParams.RESIDENTIAL_SPAWN_PERIOD;
                    break;
                case "tertiary":
                    newPeriod = CreationParams.TERTIARY_SPAWN_PERIOD;
                    break;
                case "secondary":
                    newPeriod = CreationParams.SECONDARY_SPAWN_PERIOD;
                    break;
                case "primary":
                    newPeriod = CreationParams.PRIMARY_SPAWN_PERIOD;
                    break;
                case "motorway":
                    newPeriod = CreationParams.MOTORWAY_SPAWN_PERIOD;
                    break;
                default:
                    newPeriod = CreationParams.OTHER_SPAWN_PERIOD;
                    break;
            }
            int lanesImpact = Math.min(graph.numOfLanesFwd(way), graph.numOfLanesBwd(way));
            if(lanesImpact != 0) newPeriod *= lanesImpact;
            if (newPeriod < period)
                period = newPeriod;
        }

        return period/capacity*3;
    }

    private Node findSink(Node currSource) {
        Node sink = null;
        if (manualSettedRoutes.size() == 0) {
            int sinksSize = sinks.size();
            int index = (int) (Math.random() * sinksSize);
            int posInQueue;
            while (sink == null) {
                if (index == sinksSize) index = 0;
                posInQueue = sinkPosInQueue.get(index);
                sinkPosInQueue.set(index, --posInQueue);
                if (posInQueue == 0) {
                    sinkPosInQueue.set(index, sinksPriorities.get(index));
                    sink = sinks.get(index);
                }
                index++;
            }
        } else {
           sink = manualSettedSinksSources.get(currSource);
        }
        return sink;
    }

    /**
     * Помещает машины на карту в момент начала моделирования
     */

     // Помещает машины на карту в момент начала моделирования


    private void setCars() {

        double initSpawnProbability = CreationParams.DEFAULT_LVL_INIT_SPAWN_PROBABILITY;

        if (periods == null) setPeriods();
        if (sinksPriorities == null && manualSettedRoutes.size() == 0) setSinksPriorities();

       /* List<Node> nodes = sessionObjects.getGraph().getVertexList();

        int i = 0;
        int cars = Integer.MAX_VALUE;
        int sinksSize = sinks.size();
        Node sink;

        for (Node nd : nodes) {
            sink = nd;
            Car car = null;
            if (i >= cars)
                break;
            if(Math.random() <= initSpawnProbability) {
                for (int j = 0; j < 10 && car == null; j++) {
                    sink = sinks.get((int)( Math.random() * sinksSize));
                    car = ccFactory.createCar(nd, sink);
                    if (car != null)
                        i++;
                }
            }
        }
        System.out.println("cars setted at fist: " + i);

        //sessionObjects.getCarHolder().dump();*/

    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        setPeriods();
        setSinksPriorities();
    }

    public void maintain() {
        // double lastTimeQuant = sessionObjects.getWorldUpdater().getTimeQuant();
        double lastTimeQuant;
        Instant instant;
        int ndIndexInSources;
        double spawnProbability, random;
        Node currSource;

        //int sinksSize = sinks.size();
        Car cr = null;
        if(lastInstant == null) {
            lastInstant = Instant.now();
            System.out.println("li " + lastInstant);
            lastTimeQuant = WorldUpdater.FIRST_QUANT;
        }
        else {
            instant = Instant.now();
            lastTimeQuant = Duration.between(lastInstant,instant).toMillis();
            lastTimeQuant /= 1000;
          //  System.out.println("lasttq: " + lastTimeQuant + " duration: " +  Duration.between(lastInstant,instant).toMillis());
            lastInstant = instant;
        }

        for(int i = 0; i < sources.size(); i++) {
            currSource = sources.get(i);

            timePassed.set(i, timePassed.get(i) + lastTimeQuant);
            if (timePassed.get(i) >= periods.get(i)) {

                carArrived.set(i, false);
                timePassed.set(i, 0.0);
            }
            if (!carArrived.get(i)) {
                spawnProbability = lastTimeQuant/(periods.get(i) - timePassed.get(i));
                random = Math.random();

                if (random <= spawnProbability) {
                    if (allowedToSpawnMoreCars(i))
                    {
                        //TODO: optimize
                        for (int j = 0; j < 5 && cr == null; j++) {

                            cr = ccFactory.createCar(currSource, findSink(currSource));
                        }
                        if (cr != null) {
                            carArrived.set(i, true);
                            if (manualSettedRoutes.size() > 0)
                                carsCounter.set(i, carsCounter.get(i)+1);
                            //System.out.println(i + " " + carsCounter.get(i) + " " +
                            //        sessionObjects.getCarHolder().getCars().size());

                        }
                    }

                }
            }
        }
    }

    private boolean allowedToSpawnMoreCars(int i) {
        if (manualSettedRoutes.size() == 0)
            return true;
        int maxCarsAllowed =
                ((Double)manualSettedRoutes.get(i).get("maxCarsCount")).intValue();

        if (carsCounter.get(i) < maxCarsAllowed || maxCarsAllowed < 0)
            return true;

        return false;
    }

    /**
     *
     * @return true if there are cars too close to the source, false if a new car could be placed at the source
     */

    private boolean sourceBlocked(Node source) {

        CarHolder holder =  sessionObjects.getCarHolder();
        Distance distance = sessionObjects.getDistance();
        boolean carsNearby = false;
        List<RoadThing> nearbyList = holder.getNearby(source);
        if (nearbyList != null) {
            for (RoadThing car : holder.getNearby(source)) {
                if (distance.distanceBetween(source, car.getPos()) < ModelConstants.RECOMMENDED_DISTANCE)
                    carsNearby = true;
            }
        }

        return carsNearby;

    }

    private void findSrcsSinks() {
        if (manualSettedRoutes.size() == 0) {
            findSrcsSinksAuto();
            return;
        } else {
            findSrcsSinksManual();
        }

    }

    private void findSrcsSinksManual() {
        Graph graph = sessionObjects.getGraph();
        sources = new ArrayList<>();
        manualSettedSinksSources = new HashMap<>();
        for (int i = 0; i < manualSettedRoutes.size(); i++) {
            ArrayList<Double> st = (ArrayList<Double>)manualSettedRoutes.get(i).get("start");
            ArrayList<Double> fn = (ArrayList<Double>)manualSettedRoutes.get(i).get("finish");
            Node start = new Node(0, st.get(0), st.get(1));
            Node finish = new Node(0, fn.get(0), fn.get(1));
            start = graph.nearestNode(start);
            finish = graph.nearestNode(finish);
           // Node[] _ = {start, finish};
            //ArrayList<Node> pr = new ArrayList<>(Arrays.asList(_));
            //sessionObjects.getClientProcessor().drawNodesTogether(pr, 2);
            manualSettedSinksSources.put(start, finish);
            sources.add(start);
        }
        System.out.println("sources len: " + sources.size());
        sinks = new ArrayList<>();
    }

    private void findSrcsSinksAuto() {
        Map<Node, List<Node>> adjList = sessionObjects.getGraph().getAdjList();
        List<Node> candidates = new ArrayList<>(); //первые и последние ноды в дорогах
        List<Node> sources = new ArrayList<>();
        List<Node> sinks = new ArrayList<>();
        //find candidates
        Map<String, Way> map = sessionObjects.getGraph().getInterMap();
        Iterator<Map.Entry<String, Way>> mapIt = map.entrySet().iterator();
        Map.Entry<String, Way> mapEntry;
        List<Node> road;
        Map<Node, List<Way>> nodeWays = sessionObjects.getGraph().getNodeWays();
        List<Way> incWays;
        int sumsize = 0;
        while (mapIt.hasNext()) {
            mapEntry = mapIt.next();
            road = mapEntry.getValue().getNodes();
            int size = road.size();
            //System.out.println("size: "+ size + " way: " + mapEntry.getKey());
            sumsize += size;
            if (size > 0) {
                candidates.add(road.get(0));
                if (size > 1)
                    candidates.add(road.get(size - 1));
            }
        }

        //уберём из кандидатов ноды, лежащие в середине к/л дороги

       Iterator<Node> candIt = candidates.iterator();
        while(candIt.hasNext()) {
            Node candidate = candIt.next();
            List<Way> ways = nodeWays.get(candidate);
            for(Way way: ways) {
                //System.out.println(way);
                List<Node> nodes = way.getNodes();
                int index = nodes.indexOf(candidate);
                if(index != 0 && index != nodes.size() - 1) {
                    if(!candidate.getId().equals("N-13")) { // node on boundary
                        candIt.remove();
                    }
                    break;
                }
            }
        }

        System.out.println("number of candidates to be src or sink:" + candidates.size
                () + " sumsize: " + sumsize);

        List<Node> adjVertexes;
        Iterator<Node> cndIt = candidates.iterator();
        Node nd;
        List<Node> waysNodes;
        boolean ndIsSource;

        while (cndIt.hasNext()) {
            nd = cndIt.next();
            adjVertexes = adjList.get(nd);
            ndIsSource = true;
            incWays = nodeWays.get(nd);

            if(incWays.size() == 1) {

                if (adjVertexes != null && adjVertexes.size() != 0) {

                    //проверка на то, что nd не является последней ни для какой дороги
                    for (Way way : incWays) {
                        waysNodes = way.getNodes();
                        String highway = way.getTags().get("highway");
                        if (!(highway.equals("service") || highway.equals("living street") || highway.equals("residential"))) {
                            if (waysNodes.size() > 1) {
                                if (waysNodes.indexOf(nd) == waysNodes.size() - 1) {
                                    // System.out.println("last node in " + way);
                                    ndIsSource = false;
                                }
                            }
                        }
                    }
                    if (ndIsSource) {
                        sources.add(nd);
                    }
                    // cndIt.remove();
                } else {
                    sinks.add(nd);
                }
            }
        }
        //добавим к точкам назначения точки старта, расположенные на маленьких дорогах
        String highway;
        for (Node node : sources) {
            incWays = nodeWays.get(node);
            for (Way way : incWays) {
                highway = way.getTags().get("highway");
                if (highway.equals("service") || highway.equals("living street") || highway.equals("residential"))
                    sinks.add(node);
                else;
                    //System.out.println(highway);
            }
        }
        //добавим точки на границе прямоугольника

        List<Node> nodes = sessionObjects.getGraph().getVertexList();
        for(Node node : nodes) {
            if(node.getId().equals("N-13")) {
                incWays = nodeWays.get(node);
                boolean oneway = true;
                for(Way way : incWays) {
                    if((way.getTags().get("oneway") == null ) || (way.getTags().get("oneway").equals("no"))) {
                        oneway = false;
                    }
                }
                if(!oneway) {
                    sinks.add(node);
                    sources.add(node);
                } else {
                    boolean sink = false, source = false;
                    for(Way way : incWays) {
                        if(way.getNodes().indexOf(node) == 0) source = true;
                        else sink = true;
                    }
                    if(sink) sinks.add(node);
                    if(source) sources.add(node);
                }

            }
        }

        this.sources = sources;
        this.sinks = sinks;

        System.out.println("sources: " + sources.size() + " sinks: " + sinks.size());

        //test : show sources/sinks
       /* List<double[]> ndCoords = new ArrayList<>();
        for(Node src: sinks) {
            double[] coords = {src.getLon(), src.getLat()};
            ndCoords.add(coords);
            //System.out.println(src);
        }
        System.out.println("sending sources");
        sessionObjects.getClientProcessor().sendTestCoord(ndCoords);
        try {
            Thread.sleep(1000000);
        } catch (Exception e) {
            System.out.println("EXEPTION!");
        }*/
    }
}

