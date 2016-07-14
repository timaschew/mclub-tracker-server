import grails.util.Environment

class UrlMappings {

	static mappings = {
//		"/$controller/$action?/$id?"{
//			constraints {
//				// apply constraints here
//			}
//		}

		"/map/$id?"(controller:"map", action:'index'){}
		"/map/aprs/$id?"(controller:"map", action:'aprs'){}

		"/map/all"(controller:"map", action:'all'){}
		"/map/mclub"(controller:"map", action:'all'){}
		
		// For compatible with the application
		"/mtracker/api/$action/$id?"(controller:"trackerAPI"){
		
		}

		// API Mappings
		"/api/"(controller:"trackerAPI", action:'about')
		"/api/$action/$id?"(controller:"trackerAPI")

		// Admin Mappings
		"/admin/$action?/$id?"(controller:"admin")
		"/admin/user/$action/$id?"(controller:"user")
		"/admin/device/$action/$id?"(controller:"trackerDevice")
		"/admin/position/$action/$id?"(controller:"trackerPosition")
		"/admin/console/$action/$id?"(controller:"console")
		"/admin/map/$action/$id?"(controller:"trackerMap")

		"/"(controller:'map',action:'index')
//		"/"(view:"/index")

		// Error Mappings
		if (Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST) {
			"500"(view:"/error")
			"404"(view:'/errors/404')
		}else{
			"500"(view:'/errors/500')
			"404"(view:'/errors/404')
		}
	}
}
