package mclub.tracker



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import mclub.user.AuthUtils

@Transactional(readOnly = true)
class TrackerMapController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond TrackerMap.list(params), model:[trackerMapInstanceCount: TrackerMap.count()]
    }

    def show(TrackerMap trackerMapInstance) {
        respond trackerMapInstance
    }

    def create() {
		TrackerMap map = new TrackerMap(params);
		if(map.uniqueId == null){
			//generatr unique id for the new map
			map.uniqueId = AuthUtils.generateSalt(7).toLowerCase();
		}
        respond map
    }

    @Transactional
    def save(TrackerMap trackerMapInstance) {
        if (trackerMapInstance == null) {
            notFound()
            return
        }

        if (trackerMapInstance.hasErrors()) {
            respond trackerMapInstance.errors, view:'create'
            return
        }

        trackerMapInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'trackerMap.label', default: 'TrackerMap'), trackerMapInstance.id])
                redirect trackerMapInstance
            }
            '*' { respond trackerMapInstance, [status: CREATED] }
        }
    }

    def edit(TrackerMap trackerMapInstance) {
        respond trackerMapInstance
    }

    @Transactional
    def update(TrackerMap trackerMapInstance) {
        if (trackerMapInstance == null) {
            notFound()
            return
        }

        if (trackerMapInstance.hasErrors()) {
            respond trackerMapInstance.errors, view:'edit'
            return
        }

        trackerMapInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'TrackerMap.label', default: 'TrackerMap'), trackerMapInstance.id])
                redirect trackerMapInstance
            }
            '*'{ respond trackerMapInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(TrackerMap trackerMapInstance) {

        if (trackerMapInstance == null) {
            notFound()
            return
        }

        trackerMapInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'TrackerMap.label', default: 'TrackerMap'), trackerMapInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerMap.label', default: 'TrackerMap'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
