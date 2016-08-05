import grails.util.Environment

class UrlMappings {

	static mappings = {
//		"/$controller/$action?/$id?"{
//			constraints {
//				// apply constraints here
//			}
//		}

		"/map"(controller:"map", action:'index'){}
		"/map/$id?"(controller:"map", action:'index'){}
		"/map/aprs/$id?"(controller:"map", action:'aprs_compatible'){}
		// The legacy aprs v1 map
		"/map/aprs1"(controller:"map", action:'aprs_v1'){}

		"/map/query"(controller:"map", action:'query'){}

		"/map/all"(controller:"map", action:'all'){}
		"/map/mclub"(controller:"map", action:'all'){}


		// For compatible with the application
		"/mtracker/api/$action/$id?"(controller:"trackerAPI"){
		
		}
				
		"/api/$action/$id?"(controller:"trackerAPI"){
			
		}

		"/api/report/$action/$id?"(controller:"trackerReportAPI"){

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

		"/admin/map/$action/$id?"(controller:"trackerMap"){
		
		}

		"/"(controller:'map',action:'index')

//		"/"(view:"/index")
		
		if (Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST) {
			"500"(view:'/error')
		}else{
			"500"(view:'/errors/500')
			"404"(view:'/errors/404')
		}
	}
}
