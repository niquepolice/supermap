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

/**
 * Created by 1 on 18.04.2017.
 */
public class ModelConstants {
    //meters, seconds
	public static final double CITY_CAR_DEF_LEN = 4;
	public static final double CITY_CAR_DEF_SPEED = 13;
	public static final double CITY_MAX_SPEED = 17;
    public static final double CITY_CAR_DEF_MU = 0.5;
	public static final double CITY_CAR_DEF_MAX_ACC = 3;
	public static final double RECOMMENDED_DISTANCE = 20;
	public static final double SPAWN_DISTANCE = 10;
	public static final double LINE_BREADTH = 1.5;
	public static final double CITY_CAR_MAX_SHIFT_SPEED = 0.5;
	public static final double SHIFT_MISTAKE = 0.3;
	public static final double SERIOUS_CLOSESNESS_T = 3; //time
	public static final double DRIVER_SIGHT_RANGE = Math.PI/4; //максимальное отклонение от напралвения взгляда
    public static final double SERIOUS_CLOSESNESS_M = 7;
    //public static final double LINE_BREADTH_DRAW = 1;

}
