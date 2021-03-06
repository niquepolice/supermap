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

import java.time.Instant;
import top.supcar.server.graph.Distance;
import top.supcar.server.graph.Graph;
import top.supcar.server.holder.CarHolder;
import top.supcar.server.holder.TlKunteynir;
import top.supcar.server.model.creation.CarSetter;
import top.supcar.server.physics.Physics;
import top.supcar.server.session.ClientProcessor;
import top.supcar.server.session.SelectedRect;
import top.supcar.server.update.CarsUpdater;
import top.supcar.server.update.WorldUpdater;

/**
 * The holder for session objects <br>
 * @author niquepolice
 */

public class SessionObjects {
//ATTRIBUTES
	private CarHolder carHolder;
	private SelectedRect selectedRect;
	private Physics physics;
	private Distance distance;
	private CarsUpdater carsUpdater;
	private WorldUpdater worldUpdater;
	private Graph graph;
	private CarSetter carSetter;
	private Instant currInstant;
	private TlKunteynir tlKunteynir;
	private ClientProcessor clientProcessor;

	public void setClientProcessor(ClientProcessor clientProcessor) {
		this.clientProcessor = clientProcessor;
	}

	public void setTlKunteynir(TlKunteynir tlKunteynir) {
		this.tlKunteynir = tlKunteynir;
	}

	public void setCurrInstant(Instant currInstant) {
		this.currInstant = currInstant;
	}

	public void setCarsUpdater(CarsUpdater carsUpdater) {
		this.carsUpdater = carsUpdater;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	public void setWorldUpdater(WorldUpdater worldUpdater) {
		this.worldUpdater = worldUpdater;
	}

	public void setDistance(Distance distance) {
		this.distance = distance;
	}

	public void setCarHolder(CarHolder carHolder) {
		this.carHolder = carHolder;
	}

	public void setPhysics(Physics physics) {
		this.physics = physics;
	}

	public void setSelectedRect(SelectedRect selectedRect) {
		this.selectedRect = selectedRect;
	}

	public void setCarSetter(CarSetter carSetter) {
		this.carSetter = carSetter;
	}

	public WorldUpdater getWorldUpdater() {
		return worldUpdater;
	}

	public CarsUpdater getCarsUpdater() {
		return carsUpdater;
	}

	public Distance getDistance() {
		return distance;
	}

	public CarHolder getCarHolder() {
		return carHolder;
	}

	public Physics getPhysics() {
		return physics;
	}

	public SelectedRect getSelectedRect() {
		return selectedRect;
	}

	public Graph getGraph() {
		return graph;
	}

	public Instant getCurrInstant() {
		return currInstant;
	}

	public CarSetter getCarSetter() {
		return carSetter;
	}

	public TlKunteynir getTlKunteynir() {
		return tlKunteynir;
	}

	public ClientProcessor getClientProcessor() {
		return clientProcessor;
	}
}


