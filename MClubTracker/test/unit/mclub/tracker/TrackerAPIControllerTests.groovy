package mclub.tracker



import grails.converters.JSON
import grails.test.mixin.*
import mclub.user.UserService.UserSession
import org.junit.*
import spock.lang.Specification

import mclub.user.*
import mclub.sys.*

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(TrackerAPIController)
@Mock([User,UserService,TaskService,TrackerDataService])
class TrackerAPIControllerTests extends Specification {

	void testUpdateLocation(){
		given:
		def cmd = new PositionUpdateCommand(udid:'1234',latitude:-1,longitude:-1);
		cmd.validate();
		
		UserSession session  = new UserSession(username:'admin',type:2,token:'the_test_token');
		request['session'] = session;
		
		when:
		controller.update_position2(cmd)
		
		then:
		def result = [code:1,message:'Invalid parameters'];
		response.text == result as JSON;
	}
	
	void testQueryGeoJsonInBounds(){
		String case1 = "31.063981,119.035390,29.299308,121.500648" // ZHEJIANG
		String case2 = "33.134480,115.361048,26.967927,123.960963" // EASTERN_CHINA
		String case3 = "30.399728,119.944333,30.057721,120.462355" // HANGZHOU
		given:
		def filter = new TrackerDeviceFilter(bounds:case1);

		UserSession session  = new UserSession(username:'admin',type:2,token:'the_test_token');
		request['session'] = session;

		when:
		controller.geojson(filter)

		then:
		def result = {}
		response.text == result as JSON
	}
}
