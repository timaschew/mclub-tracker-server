import grails.util.Environment

class UrlMappings {

	static mappings = {
//		"/$controller/$action?/$id?"{
//			constraints {
//				// apply constraints here
//			}
//		}

		"/map/$id?"(controller:"trackerMap", action:'index'){}
		"/map/aprs/$id?"(controller:"trackerMap", action:'aprs'){}
		"/map/mclub/$id?"(controller:"trackerMap", action:'mclub'){}


		// For compatible with the application
		"/mtracker/api/$action/$id?"(controller:"trackerAPI"){
		
		}

				
		"/api/$action/$id?"(controller:"trackerAPI"){
			
		}
	
		"/admin/$action?/$id?"(controller:"admin"){
			
		}
		
		"/admin/user/$action/$id?"(controller:"user"){
			
		}
		
		"/admin/device/$action/$id?"(controller:"trackerDevice"){
		
		}
		
		"/admin/position/$action/$id?"(controller:"trackerPosition"){
		
		}
		
		"/admin/console/$action/$id?"(controller:"console"){
			
		}

		"/"(controller:'trackerMap',action:'index')

//		"/"(view:"/index")
		
		if (Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST) {
			"500"(view:'/error')
		}else{
			"500"(view:'/errors/500')
			"404"(view:'/errors/404')
		}
	}
}
