class UrlMappings {

	static mappings = {
//		"/$controller/$action?/$id?"{
//			constraints {
//				// apply constraints here
//			}
//		}
		
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

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
