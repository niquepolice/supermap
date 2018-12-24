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

package top.supcar.server.session;

import com.google.gson.Gson;
import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Way;
import org.eclipse.jetty.websocket.api.Session;
import top.supcar.server.graph.Distance;
import top.supcar.server.graph.Graph;
import top.supcar.server.holder.CarHolder;
import top.supcar.server.model.Car;
import top.supcar.server.model.ModelConstants;
import top.supcar.server.model.creation.CarSetter;
import top.supcar.server.parse.OSMData;
import top.supcar.server.physics.Physics;
import top.supcar.server.update.CarsUpdater;
import top.supcar.server.update.WorldUpdater;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static sun.misc.Version.println;

/**
 * Created by 1 on 25.04.2017.
 */
public class ClientProcessor {
	private SessionObjects sessionObjects;
	private Session session;
	private Gson gson;
	private int runFlag;
	private int stopFlag;
	private OSMData data;
	private Kmp kmp;
	private double X = 1;

	public ClientProcessor(Session session) {
		this.session = session;
		this.gson = new Gson();
		runFlag = 0;
		stopFlag = 0;
	}

	public class Kmp extends Thread {
		public void run() {
			go();
		}
	}
	private void prepare(Node ll, Node ur, List<Map> routes) {

		//String url = "http://www.overpass-api.de/api/xapi?way[bbox=30.258916543827283,59.917968282222404,30.34371726404213,59.94531882096226]";
        String url = "http://www.overpass-api.de/api/xapi?way[bbox="+ll.getLon() +"," +ll.getLat()+","+ur.getLon()+","+ur.getLat()+"]";



		//String url = "https://overpass.kumi.systems/api/xapi?way[bbox="+ll.getLon() +"," +ll.getLat()+","+ur.getLon()+","+ur.getLat()+"]";
        //ll = new Node(0, 59.9179682, 30.258916);
		// ur = new Node(0, 59.945318820, 30.343717);

		sessionObjects = new SessionObjects();
		sessionObjects.setClientProcessor(this);

		SelectedRect selectedRect = new SelectedRect(ll, ur);
		sessionObjects.setSelectedRect(selectedRect);

		Physics physics = new Physics(sessionObjects);
		sessionObjects.setPhysics(physics);

		Distance distance = new Distance(selectedRect);
		sessionObjects.setDistance(distance);

		Map<String, Way> roads;
		data = new OSMData(url, sessionObjects);
		try {
			data.loadData();
		} catch (Exception e) {//рассмотреть различные варианты возвр ошибок от апи
			e.printStackTrace();
//			System.err.println("XAPI Error");
//			//log(e.getMessage());
//			StackTraceElement[] stack = e.getStackTrace();
//			String stackstr="";
//			for (int i = 0; i < Arrays.asList(stack).size(); i++)
//				stackstr += stack[i] + "\n";
//			log(stackstr);
		}
		log("map downloaded from XAPI ");
		data.makeMap();
		roads = data.getMap();
		Graph graph = new Graph(roads, sessionObjects);
		sessionObjects.setGraph(graph);

		CarHolder carHolder = new CarHolder(sessionObjects, 100);
		sessionObjects.setCarHolder(carHolder);

		CarSetter cSetter = new CarSetter(sessionObjects, 1, routes);
		sessionObjects.setCarSetter(cSetter);

		CarsUpdater carsUpdater = new CarsUpdater(sessionObjects);
		sessionObjects.setCarsUpdater(carsUpdater);

		WorldUpdater worldUpdater = new WorldUpdater(sessionObjects);
		sessionObjects.setWorldUpdater(worldUpdater);

		runFlag = 1;
	}
	private void log(String message) {
		System.out.println(message);
		try {
			session.getRemote().sendString("{ \"log\":\" " + message + "\"}");
		} catch (Exception e) {
			System.err.println("error while sending log message");
		}
	}

	private void go() {
		WorldUpdater worldUpdater = sessionObjects.getWorldUpdater();
		while (true) {
			if(stopFlag == 1){
				break;
			}
			if(runFlag == 1) {
				worldUpdater.update();
				//System.out.println(Instant.now());
				try {
					sendJson();
					Thread.sleep((long)(20/X));

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(2);
			} catch (Exception e){
				e.printStackTrace();
			}
		}

	}

	public void stop(){
		runFlag = 0;
		data.clear();
	}

	public void pause(){
		runFlag = 0;
	}

	public void play() {
		runFlag = 1;
	}

	public void handleMsg (String message){

        Map result = (Map) gson.fromJson(message, Object.class);
		String messageType = (String)result.get("messageType");

        if(messageType.equals("selectedRect")) {
			String[] coordinates = result.get("coordinates").toString().split(",");
			Node ll = new Node(0, Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[0]));
			Node ur = new Node(0, Double.parseDouble(coordinates[3]), Double.parseDouble(coordinates[2]));
			this.prepare(ll, ur, new ArrayList<>());
			kmp = new Kmp();
			kmp.start();
			//this.go();
		}

        else if(messageType.equals("buttonMsg")){
            System.out.println("key: " +messageType);
            String msg = (String)result.get("value");
            if(msg.equals("close")){
                stop();
			}
			if(msg.equals("pause")){
				pause();
			}
			if(msg.equals("play")){
				play();
			}
        }
		else if(messageType.equals("speedChange")){
			Map values = (Map)result.get("value");
			//Double oldVal = (Double)values.get(values.keySet().toArray()[0]);
			Double newVal = (Double)values.get(values.keySet().toArray()[1]);
			if( X != newVal){
				setX(newVal.intValue());
            }

		}
		else if(messageType.equals("capacityChange")){
			Map values = (Map)result.get("value");
			Double oldVal = (Double)values.get(values.keySet().toArray()[0]);
			Double newVal = (Double)values.get(values.keySet().toArray()[1]);
			if(!oldVal.equals(newVal)){
				setCapacity(newVal.intValue());
            }
        }
        else if(messageType.equals("configFile")){
			ArrayList<Double> coordinates = (ArrayList<Double>)result.get("rectangle");

			Node ll = new Node(0, coordinates.get(0),coordinates.get(1));
			Node ur = new Node(0, coordinates.get(2), coordinates.get(3));

			String mode = (String)result.get("mode");
			if (mode.equals("auto")) {

				this.prepare(ll, ur, new ArrayList<>());
				kmp = new Kmp();
				kmp.start();
			} else {
				this.prepare(ll, ur, (List<Map>)result.get("routes"));
				kmp = new Kmp();
				kmp.start();
			}
		}


    }

	private void sendJson() {
		ArrayList<double[]> carsCoordinates = new ArrayList<>();
		ArrayList<Car> cars = sessionObjects.getCarHolder().getCars();
		double lon, lat;

		//System.out.println("num of cars: " + cars.size());

		for (Car car : cars) {
		    Node nd = car.getPosWithShift();
		    lon = nd.getLon();
		    lat = nd.getLat();
			double[] coordinates = {lon, lat};
			carsCoordinates.add(coordinates);
		}
		try {
			String point = gson.toJson(carsCoordinates);
			if(session.isOpen()) {
				session.getRemote().sendString(point);
			}
		} catch (Exception e) {
			stop();
			System.out.println("closing connection on client side");
			e.printStackTrace();
		}

	}
	public void sendTestCoord(List<double[]> list) {
        try {
            String point = gson.toJson(list);
			if(session.isOpen()) {
				session.getRemote().sendString(point);
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param list
     * @param pause in seconds
     */
    public void drawNodesWithPauses(List<Node> list, double pause) {
        List<double[]> coords = new ArrayList<>();
        for(Node node : list) {
            double[] coord = {node.getLon(), node.getLat()};
            coords.add(coord);
            sessionObjects.getClientProcessor().sendTestCoord(coords);
            try {
                System.out.println(Thread.currentThread().getName());
                Thread.sleep((long)pause*1000);
            } catch (Exception e) {
                System.out.println("EXCEPTION!");
            }

        }

    }
    public void drawNodesTogether( List<Node> list, double pause) {
		List<double[]> coords = new ArrayList<>();
		for (Node node : list) {
			double[] coord = {node.getLon(), node.getLat()};
			coords.add(coord);
			sessionObjects.getClientProcessor().sendTestCoord(coords);
		}
		try {
			//  System.out.println(Thread.currentThread().getName());
			Thread.sleep((long) pause * 1000);
		} catch (Exception e) {
			System.out.println("EXCEPTION!");
		}
	}

    private void setX(int X) {
        sessionObjects.getWorldUpdater().setX(X);
        this.X = X;
    }
    private void setCapacity(int capacity) {
        sessionObjects.getCarSetter().setCapacity(capacity);
    }

}

