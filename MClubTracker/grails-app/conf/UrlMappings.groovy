class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}
		
		"/api/$action/$id?"(controller:"trackerAPI"){
			
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
