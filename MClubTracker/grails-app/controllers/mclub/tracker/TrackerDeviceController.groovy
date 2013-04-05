package mclub.tracker

import org.springframework.dao.DataIntegrityViolationException

class TrackerDeviceController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        [trackerDeviceInstanceList: TrackerDevice.list(params), trackerDeviceInstanceTotal: TrackerDevice.count()]
    }

    def create() {
        [trackerDeviceInstance: new TrackerDevice(params)]
    }

    def save() {
        def trackerDeviceInstance = new TrackerDevice(params)
        if (!trackerDeviceInstance.save(flush: true)) {
            render(view: "create", model: [trackerDeviceInstance: trackerDeviceInstance])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'trackerDevice.label', default: 'TrackerDevice'), trackerDeviceInstance.id])
        redirect(action: "show", id: trackerDeviceInstance.id)
    }

    def show(Long id) {
        def trackerDeviceInstance = TrackerDevice.get(id)
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

        trackerDeviceInstance.properties = params

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

        try {
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
