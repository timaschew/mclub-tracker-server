/**
 * Project: MClubTracker
 * 
 * File Created at 2013-8-6
 * $id$
 * 
 * Copyright 2013, Shawn Chain (shawn.chain@gmail.com).
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mclub.util;

/**
 * @author shawn
 * 
 */
public class LocationUtils {
	 /**
     * Helper that calculates distances between 2 points of WGS From article
     * "Calculate distance, bearing and more between Latitude/Longitude points"
     * http://www.movable-type.co.uk/scripts/latlong.html
     */
    private static final double earthRadius = 6378135;
    
	/**
	 * Calculate 2 point distances, in meter
	 * 
	 * @param formLat
	 * @param fromLon
	 * @param toLat
	 * @param toLon
	 * @return
	 */
	public static double distance(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
		        * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;

		return new Double(dist).doubleValue();
	}

	/**
	 * Check whether location is valid
	 * 
	 * @param lat
	 * @param lon
	 * @return
	 */
	public static boolean isValidLocation(double lat, double lon) {
		// Latitude: Valid range is -90 to +90
		// "+" for North and "-" for South
		// Longitude: Valid range is -180 to +180
		// "-" for West and "+" for East
		return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
	}
}
