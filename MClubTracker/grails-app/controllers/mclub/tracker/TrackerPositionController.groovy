package mclub.tracker

import org.springframework.dao.DataIntegrityViolationException

class TrackerPositionController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max,String udid) {
        params.max = Math.min(max ?: 10, 100)
		def positions = [];
		int positionCount = 0;
		if(udid){
			TrackerDevice device = TrackerDevice.findByUdid(udid);
			if(device){
				positions = TrackerPosition.findAllByDeviceId(device.id);
				positionCount = positions?.size();
			}
		}else{
			positions = TrackerPosition.list(params);
			positionCount = TrackerPosition.count();
		}
        [trackerPositionInstanceList: positions, trackerPositionInstanceTotal: positionCount]
    }
	
    def create() {
        [trackerPositionInstance: new TrackerPosition(params)]
    }

    def save() {
        def trackerPositionInstance = new TrackerPosition(params)
        if (!trackerPositionInstance.save(flush: true)) {
            render(view: "create", model: [trackerPositionInstance: trackerPositionInstance])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), trackerPositionInstance.id])
        redirect(action: "show", id: trackerPositionInstance.id)
    }

    def show(Long id) {
        def trackerPositionInstance = TrackerPosition.get(id)
        if (!trackerPositionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), id])
            redirect(action: "list")
            return
        }

        [trackerPositionInstance: trackerPositionInstance]
    }

    def edit(Long id) {
        def trackerPositionInstance = TrackerPosition.get(id)
        if (!trackerPositionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), id])
            redirect(action: "list")
            return
        }

        [trackerPositionInstance: trackerPositionInstance]
    }

    def update(Long id, Long version) {
        def trackerPositionInstance = TrackerPosition.get(id)
        if (!trackerPositionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (trackerPositionInstance.version > version) {
                trackerPositionInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'trackerPosition.label', default: 'TrackerPosition')] as Object[],
                          "Another user has updated this TrackerPosition while you were editing")
                render(view: "edit", model: [trackerPositionInstance: trackerPositionInstance])
                return
            }
        }

        trackerPositionInstance.properties = params

        if (!trackerPositionInstance.save(flush: true)) {
            render(view: "edit", model: [trackerPositionInstance: trackerPositionInstance])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), trackerPositionInstance.id])
        redirect(action: "show", id: trackerPositionInstance.id)
    }

    def delete(Long id) {
        def trackerPositionInstance = TrackerPosition.get(id)
        if (!trackerPositionInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), id])
            redirect(action: "list")
            return
        }

        try {
            trackerPositionInstance.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'trackerPosition.label', default: 'TrackerPosition'), id])
            redirect(action: "show", id: id)
        }
    }
}
