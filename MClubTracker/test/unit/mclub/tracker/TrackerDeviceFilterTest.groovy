package mclub.tracker

import com.github.davidmoten.geo.Coverage
import com.github.davidmoten.geo.GeoHash
import grails.test.GrailsUnitTestCase
import org.junit.Test

/**
 * Created by shawn on 16/7/18.
 */
public class TrackerDeviceFilterTest extends GrailsUnitTestCase {
    @Test
    public void testCheckCoordinateInsideBounds(){
        TrackerDeviceFilter f = new TrackerDeviceFilter();
        f.bounds = "30.454241,119.648473,30.084522,120.465204";

        PositionData p = new PositionData();
        p.latitude = 30.285485;
        p.longitude = 119.812783;
        assertTrue(f.accept(p))
    }
}
