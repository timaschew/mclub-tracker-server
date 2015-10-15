package mclub.tracker

import org.springframework.dao.DataIntegrityViolationException

class TrackerDeviceController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max, Integer type) {
        params.max = Math.min(max ?: 10, 100)
		
		if(params.username){
			// filter by username
			def r = TrackerDevice.findAllByUsername(params.username);
			return [trackerDeviceInstanceList: r, trackerDeviceInstanceTotal: r?.size()]
		}else if(type != null){
			def r = TrackerDevice.findAllByStatus(type);
			return [trackerDeviceInstanceList: r, trackerDeviceInstanceTotal: r?.size()]
		}else{
        	return [trackerDeviceInstanceList: TrackerDevice.list(params), trackerDeviceInstanceTotal: TrackerDevice.count()]
		}
    }

    def create() {
		def t = new TrackerDevice();
		t.properties = request;
        [trackerDeviceInstance: t];
    }

    def save() {
		// data binding changes as per http://grails.github.io/grails-doc/2.5.x/guide/upgradingFrom23.html
        def trackerDeviceInstance = new TrackerDevice();
		trackerDeviceInstance.properties = request;
        if (!trackerDeviceInstance.save(flush: true)) {
            render(view: "create", model: [trackerDeviceInstance: trackerDeviceInstance])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), trackerDeviceInstance.id])
        redirect(action: "show", id: trackerDeviceInstance.id)
    }

    def show(Long id,String udid) {
        def trackerDeviceInstance;
		if(udid){
			trackerDeviceInstance = TrackerDevice.findByUdid(udid);
		}else{
			trackerDeviceInstance = TrackerDevice.get(id)
		}
        if (!trackerDeviceInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), id])
            redirect(action: "list")
            return
        }

        [trackerDeviceInstance: trackerDeviceInstance]
    }

    def edit(Long id) {
        def trackerDeviceInstance = TrackerDevice.get(id)
        if (!trackerDeviceInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), id])
            redirect(action: "list")
            return
        }

        [trackerDeviceInstance: trackerDeviceInstance]
    }

    def update(Long id, Long version) {
        def trackerDeviceInstance = TrackerDevice.get(id)
        if (!trackerDeviceInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (trackerDeviceInstance.version > version) {
                trackerDeviceInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'trackerDevice.label', default: 'TrackerDevice')] as Object[],
                          "Another user has updated this TrackerDevice while you were editing")
                render(view: "edit", model: [trackerDeviceInstance: trackerDeviceInstance])
                return
            }
        }

        trackerDeviceInstance.properties = request;

        if (!trackerDeviceInstance.save(flush: true)) {
            render(view: "edit", model: [trackerDeviceInstance: trackerDeviceInstance])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), trackerDeviceInstance.id])
        redirect(action: "show", id: trackerDeviceInstance.id)
    }

    def delete(Long id) {
        def trackerDeviceInstance = TrackerDevice.get(id)
        if (!trackerDeviceInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), id])
            redirect(action: "list")
            return
        }

		TrackerDevice.withTransaction{
			try {
				// First delete the associated positions
				TrackerPosition.executeUpdate("DELETE TrackerPosition tp WHERE tp.device=:dev",[dev:trackerDeviceInstance]);
				trackerDeviceInstance.delete(flush: true)
				flash.message = message(code: 'default.deleted.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), id])
				redirect(action: "list")
			}
			catch (DataIntegrityViolationException e) {
				flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), id])
				redirect(action: "show", id: id)
			}
	
		}
    }
}
