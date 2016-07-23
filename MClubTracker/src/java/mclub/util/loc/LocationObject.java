/**
 * Project: MClubTracker
 * 
 * File Created at 2014-8-2
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
package mclub.util.loc;

import java.util.Random;

/**
 * @author shawn
 *
 */
public class LocationObject {
	String addr;
	Double lat;
	Double lon;
	/**
	 * @return the addr
	 */
	public String getAddr() {
		return addr;
	}
	/**
	 * @param addr the addr to set
	 */
	public void setAddr(String addr) {
		this.addr = addr;
	}
	/**
	 * @return the lat
	 */
	public Double getLat() {
		return lat;
	}
	/**
	 * @param lat the lat to set
	 */
	public void setLat(Double lat) {
		this.lat = lat;
	}
	/**
	 * @return the lon
	 */
	public Double getLon() {
		return lon;
	}
	/**
	 * @param lon the lon to set
	 */
	public void setLon(Double lon) {
		this.lon = lon;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((addr == null) ? 0 : addr.hashCode());
	    return result;
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    LocationObject other = (LocationObject) obj;
	    if (addr == null) {
		    if (other.addr != null)
			    return false;
	    } else if (!addr.equals(other.addr))
		    return false;
	    return true;
    }
    
    private static Random rnd = new Random(System.currentTimeMillis());
	public String getLatWithRandOffset(){
		int x = rnd.nextInt(50000);
		double offset = (double)(x - 25000) / 1000000.f;
		String s = String.format("%.6f", lat + offset);
		//System.out.println(lat + "->" + s);
		return s;
	}
	
	public String getLonWithRandOffset(){
		int x = rnd.nextInt(50000);
		double offset = (double)(x - 25000) / 1000000.f;
		String s = String.format("%.6f", lon + offset);
		//System.out.println(lon + "->" + s);
		return s;
	}
	
}
