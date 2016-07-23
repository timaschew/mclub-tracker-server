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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @author shawn
 * 
 */
public class LocationCoderService {
	private static final Logger log = LoggerFactory
			.getLogger(LocationCoderService.class);
	Map<String, LocationObject> addressLocationCache = new ConcurrentHashMap<String, LocationObject>();
	boolean dataUpdated;
	String localCacheFileName;

	public void start() {
		// startup the
	}

	public void stop() {

	}

	public LocationCoderService(String fileName) {
		this.localCacheFileName = fileName;
		loadCache();
	}

	public boolean isDataUpdated() {
		return dataUpdated;
	}

	public void loadCache() {
		InputStream is = null;
		try {
			// first load from local file system
			File f = new File(localCacheFileName);
			if (f.exists()) {
				is = new FileInputStream(f);
			} else {
				is = this.getClass().getResourceAsStream(localCacheFileName);
			}

			if (is != null) {
				String json = IOUtils.toString(is);
				// parse as json
				List<LocationObject> loc = JSON.parseArray(json,
						LocationObject.class);
				if (loc != null) {
					for (LocationObject l : loc) {
						addressLocationCache.put(l.addr, l);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	public void flushCache() {
		if (addressLocationCache == null || addressLocationCache.isEmpty()) {
			return;
		}
		FileOutputStream out = null;
		try {
			String jsonString = JSON
					.toJSONString(addressLocationCache.values());
			out = new FileOutputStream(localCacheFileName);
			IOUtils.write(jsonString, out);
			log.info("Locatio coder cache file" + localCacheFileName + " saved");
		} catch (Exception e) {
			log.warn("Error saving location coder cache file: "
					+ e.getMessage());
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	/**
	 * Get the location data of the address
	 * @param addr
	 * @return
	 */
	public LocationObject getLocation(String addr) {
		return getLocation(addr, false);
	}

	public LocationObject getLocation(String addr, boolean onlineSearch) {
		LocationObject loc = addressLocationCache.get(addr);
		if (loc == null && onlineSearch) {
			loc = queryGeoCodeService(addr);
			if (loc != null) {
				addressLocationCache.put(loc.addr, loc);
				dataUpdated = true;
			}
		}
		return loc;
	}

	protected LocationObject queryGeoCodeService(String addr) {
		// - http://jueyue.iteye.com/blog/1688718
		// -
		// http://api.map.baidu.com/geocoder?address=%E6%B5%99%E6%B1%9F%E6%9D%AD%E5%B7%9E&output=json&key=g2fQ61PYZCgBwTY7YAk9c8n7&city=%E6%B5%99%E6%B1%9F%E6%9D%AD%E5%B7%9E
		LocationObject location = null;
		String apiURL = "http://api.map.baidu.com/geocoder";
		try {
			HttpClient client = new HttpClient();
			GetMethod httpget = new GetMethod(apiURL /* "http://api.map.baidu.com/geocoder" */);
			httpget.setRequestHeader("Accept-Charset", "utf-8,gb2312");

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new NameValuePair("address", addr));
			params.add(new NameValuePair("city", addr));
			params.add(new NameValuePair("output", "json"));
			params.add(new NameValuePair("key", "g2fQ61PYZCgBwTY7YAk9c8n7"));

			httpget.setQueryString(params.toArray(new NameValuePair[0]));

			int status = client.executeMethod(httpget);
			// System.out.println("Response status: " + status);
			log.debug("Response status: " + status);

			String jsonString = httpget.getResponseBodyAsString();
			log.debug("JSON:");
			log.debug(jsonString);
			// System.out.println("JSON:");
			// System.out.println(jsonString);

			JSONObject obj = (JSONObject) JSON.parse(jsonString);
			if (obj != null) {
				if ("OK".equalsIgnoreCase(obj.getString("status"))) {
					JSONObject loc = obj.getJSONObject("result").getJSONObject(
							"location");
					if (loc != null) {
						// construct the GeoLocation object
						location = new LocationObject();
						location.addr = addr;
						location.lat = loc.getDouble("lat");
						location.lon = loc.getDouble("lng");
//						location.lat = String.format("%.6f",
//								loc.getDouble("lat"));// new String(loc("lat");
//						location.lon = String.format("%.6f",
//								loc.getDouble("lng"));// loc.getString("lon");
					}
				}

			}
		} catch (Exception e) {
			log.info("Error query location : " + e.getMessage());
		}

		return location;
	}
}
