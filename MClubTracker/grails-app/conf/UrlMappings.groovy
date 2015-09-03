class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}
		
		"/api/$action/$id?"(controller:"trackerAPI"){
			
		}

		"/admin"(view:"/admin")
		"/"(view:"/index")
		"500"(view:'/error')
	}
}
