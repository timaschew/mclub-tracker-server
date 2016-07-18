package mclub.util;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import grails.test.GrailsUnitTestCase;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

import static org.junit.Assert.*;
/**
 * Created by shawn on 16/7/18.
 */
public class GeoHashTest extends GrailsUnitTestCase {
    @Test
    public void testBenchGeoHash(){
        long start1 = System.currentTimeMillis();
        String h1 = "";
        for(int i = 0;i < 10000;i++){
            h1 = com.github.davidmoten.geo.GeoHash.encodeHash(30.0 + (0.0001*i),120.0);
        }
        long elapsed1 = System.currentTimeMillis() - start1;
        System.out.println("" + h1 + "elapsed " + elapsed1 + "ms");
    }

    @Test
    public void testGenerateCoverBound() throws Exception{
        //String bound = "30,120,29,121";

        String bound = "30.471671,119.919922,30.140247,120.384984";
        String[] bounds = bound.split(",");
        double lat1 = Double.parseDouble(bounds[0]);
        double lon1 = Double.parseDouble(bounds[1]);
        double lat2 = Double.parseDouble(bounds[2]);
        double lon2 = Double.parseDouble(bounds[3]);

        Coverage coverage = GeoHash.coverBoundingBox(lat1, lon1, lat2, lon2);
        String h1 = GeoHash.encodeHash(lat1,lon1);
        String h2 = GeoHash.encodeHash(lat2,lon2);
        Set<String> hash = coverage.getHashes();
        System.out.println("hash1: " + h1 + " hash2: " + h2 + " geohash: " + hash);
    }
}
