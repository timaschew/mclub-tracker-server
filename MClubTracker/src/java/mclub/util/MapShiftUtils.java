package mclub.util;

import static java.lang.Math.*;

/**
 * 
 * @author shawn
 *
 */
public class MapShiftUtils {
	private static final double pi = 3.14159265358979324;

	//
	// Krasovsky 1940
	//
	// a = 6378245.0, 1/f = 298.3
	// b = a * (1 - f)
	// ee = (a^2 - b^2) / a^2;
	private static final double a = 6378245.0;
	private static final double ee = 0.00669342162296594323;

	private static boolean outOfChina(double lat, double lon) {
		if (lon < 72.004 || lon > 137.8347)
			return true;
		if (lat < 0.8293 || lat > 55.8271)
			return true;
		return false;
	}

	private static double transformLat(double x, double y) {
		double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
				+ 0.2 * sqrt(x > 0 ? x : -x);
		ret += (20.0 * sin(6.0 * x * pi) + 20.0 * sin(2.0 * x * pi)) * 2.0 / 3.0;
		ret += (20.0 * sin(y * pi) + 40.0 * sin(y / 3.0 * pi)) * 2.0 / 3.0;
		ret += (160.0 * sin(y / 12.0 * pi) + 320 * sin(y * pi / 30.0)) * 2.0 / 3.0;
		return ret;
	}

	private static double transformLon(double x, double y) {
		double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
				* sqrt(x > 0 ? x : -x);
		ret += (20.0 * sin(6.0 * x * pi) + 20.0 * sin(2.0 * x * pi)) * 2.0 / 3.0;
		ret += (20.0 * sin(x * pi) + 40.0 * sin(x / 3.0 * pi)) * 2.0 / 3.0;
		ret += (150.0 * sin(x / 12.0 * pi) + 300.0 * sin(x / 30.0 * pi)) * 2.0 / 3.0;
		return ret;
	}

	public static double[] WGSToGCJ(double lon, double lat) {
		Coordinate2D coord = WGSToGCJ(new Coordinate2D(lat,lon));
		double[] result = new double[2];
		result[0] = coord.longitude;
		result[1] = coord.latitude;
		return result;
	}
	
	public static Coordinate2D WGSToGCJ(Coordinate2D wgLoc) {
		double dLat = 0.0, dLon = 0.0, radLat = 0.0, magic = 0.0;

		Coordinate2D mgLoc = new Coordinate2D();
		if (outOfChina(wgLoc.latitude, wgLoc.longitude)) {
			mgLoc = wgLoc;
			return mgLoc;
		}

		dLat = transformLat(wgLoc.longitude - 105.0, wgLoc.latitude - 35.0);
		dLon = transformLon(wgLoc.longitude - 105.0, wgLoc.latitude - 35.0);
		radLat = wgLoc.latitude / 180.0 * pi;
		magic = sin(radLat);
		magic = 1 - ee * magic * magic;
		double sqrtMagic = sqrt(magic);
		dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
		dLon = (dLon * 180.0) / (a / sqrtMagic * cos(radLat) * pi);
		mgLoc.latitude = wgLoc.latitude + dLat;
		mgLoc.longitude = wgLoc.longitude + dLon;

		return mgLoc;
	}

	/**
	 * Transform GCJ-02 to WGS-84 Reverse of transformFromWGSToGC() by
	 * iteration.
	 * 
	 * @param gcLoc
	 * @return
	 */
	public static Coordinate2D GCJToWGS(Coordinate2D gcLoc) {
		Coordinate2D wgLoc = new Coordinate2D(gcLoc);
		Coordinate2D currGcLoc = new Coordinate2D();
		Coordinate2D dLoc = new Coordinate2D();
		while (true) {
			currGcLoc = WGSToGCJ(wgLoc);
			dLoc.latitude = gcLoc.latitude - currGcLoc.latitude;
			dLoc.longitude = gcLoc.longitude - currGcLoc.longitude;
			if (abs(dLoc.latitude) < 1e-7 && abs(dLoc.longitude) < 1e-7) { // 1e-7
																			// ~
																			// centimeter
																			// level
																			// accuracy
				// Result of experiment:
				// Most of the time 2 iterations would be enough for an 1e-8
				// accuracy (milimeter level).
				//
				return wgLoc;
			}
			wgLoc.latitude += dLoc.latitude;
			wgLoc.longitude += dLoc.longitude;
		}

		// return wgLoc;
	}

	private static final double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

	/**
	 * Transform GCJ-02 to BD-09
	 * 
	 * @param gcLoc
	 * @return
	 */
	Coordinate2D bd_encrypt(Coordinate2D gcLoc) {
		double x = 0.0, y = 0.0, z = 0.0;
		x = gcLoc.longitude;
		y = gcLoc.latitude;
		z = sqrt(x * x + y * y) + 0.00002 * sin(y * x_pi);
		double theta = atan2(y, x) + 0.000003 * cos(x * x_pi);
		return new Coordinate2D(z * cos(theta) + 0.0065, z * sin(theta) + 0.006);
	}

	/**
	 * Transform BD-09 to GCJ-02
	 * 
	 * @param bdLoc
	 * @return
	 */
	Coordinate2D bd_decrypt(Coordinate2D bdLoc) {
		double x = 0.0, y = 0.0, z = 0.0, theta = 0.0;
		x = bdLoc.longitude - 0.0065;
		y = bdLoc.latitude - 0.006;
		z = sqrt(x * x + y * y) - 0.00002 * sin(y * x_pi);
		theta = atan2(y, x) - 0.000003 * cos(x * x_pi);
		return new Coordinate2D(z * cos(theta), z * sin(theta));
	}

	public static class Coordinate2D {
		public double latitude;
		public double longitude;

		public Coordinate2D() {

		}

		public Coordinate2D(Coordinate2D copy) {
			this.latitude = copy.latitude;
			this.longitude = copy.longitude;
		}

		public Coordinate2D(double lat, double lon) {
			this.latitude = lat;
			this.longitude = lon;
		}
	}
}
