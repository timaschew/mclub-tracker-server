package mclub.tracker

import mclub.user.AuthUtils

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class TrackerMapController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond TrackerMap.list(params), model:[trackerMapCount: TrackerMap.count()]
    }

    def show(TrackerMap trackerMap) {
        respond trackerMap
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
    def save(TrackerMap trackerMap) {
        if (trackerMap == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (trackerMap.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond trackerMap.errors, view:'create'
            return
        }

        trackerMap.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'trackerMap.label', default: 'TrackerMap'), trackerMap.id])
                redirect trackerMap
            }
            '*' { respond trackerMap, [status: CREATED] }
        }
    }

    def edit(TrackerMap trackerMap) {
        respond trackerMap
    }

    @Transactional
    def update(TrackerMap trackerMap) {
        if (trackerMap == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (trackerMap.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond trackerMap.errors, view:'edit'
            return
        }

        trackerMap.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'trackerMap.label', default: 'TrackerMap'), trackerMap.id])
                redirect trackerMap
            }
            '*'{ respond trackerMap, [status: OK] }
        }
    }

    @Transactional
    def delete(TrackerMap trackerMap) {

        if (trackerMap == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        trackerMap.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'trackerMap.label', default: 'TrackerMap'), trackerMap.id])
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
