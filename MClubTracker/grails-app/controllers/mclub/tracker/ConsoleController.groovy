package mclub.tracker

import org.apache.commons.codec.binary.Hex

class ConsoleController {

    def index() { }
	
	def raw(){
		if(request.method == 'GET'){
			render view:'raw_data_view'
			return;
		}
		def hex = params.hex
		if(!hex){
			flash.message = 'No Input'
			render view:'raw_data_view'
			return;
		}
		
		def result = []
		hex.eachLine{
			String decoded = new String(Hex.decodeHex(it.toCharArray()));
			result << decoded
		}
		
		render(view:'raw_data_view', model:[decoded:result, hex:hex])
	}
}
