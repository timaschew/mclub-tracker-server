class UrlMappings {

	static mappings = {
//		"/$controller/$action?/$id?"{
//			constraints {
//				// apply constraints here
//			}
//		}
		
		"/map/$action/$id?"(controller:"trackerMap"){
			
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

		"/"(controller:'trackerMap',action:'aprs')

//		"/"(view:"/index")
		"500"(view:'/error')
	}
}
