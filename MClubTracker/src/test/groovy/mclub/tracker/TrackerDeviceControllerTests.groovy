package mclub.tracker



import org.junit.*
import grails.test.mixin.*

@TestFor(TrackerDeviceController)
@Mock(TrackerDevice)
class TrackerDeviceControllerTests {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        //params["name"] = 'someValidName'
    }

    void testIndex() {
        controller.index()
        assert "/trackerDevice/list" == response.redirectedUrl
    }

    void testList() {

        def model = controller.list()

        assert model.trackerDeviceInstanceList.size() == 0
        assert model.trackerDeviceInstanceTotal == 0
    }

    void testCreate() {
        def model = controller.create()

        assert model.trackerDeviceInstance != null
    }

    void testSave() {
        controller.save()

        assert model.trackerDeviceInstance != null
        assert view == '/trackerDevice/create'

        response.reset()

        populateValidParams(params)
        controller.save()

        assert response.redirectedUrl == '/trackerDevice/show/1'
        assert controller.flash.message != null
        assert TrackerDevice.count() == 1
    }

    void testShow() {
        controller.show()

        assert flash.message != null
        assert response.redirectedUrl == '/trackerDevice/list'

        populateValidParams(params)
        def trackerDevice = new TrackerDevice(params)

        assert trackerDevice.save() != null

        params.id = trackerDevice.id

        def model = controller.show()

        assert model.trackerDeviceInstance == trackerDevice
    }

    void testEdit() {
        controller.edit()

        assert flash.message != null
        assert response.redirectedUrl == '/trackerDevice/list'

        populateValidParams(params)
        def trackerDevice = new TrackerDevice(params)

        assert trackerDevice.save() != null

        params.id = trackerDevice.id

        def model = controller.edit()

        assert model.trackerDeviceInstance == trackerDevice
    }

    void testUpdate() {
        controller.update()

        assert flash.message != null
        assert response.redirectedUrl == '/trackerDevice/list'

        response.reset()

        populateValidParams(params)
        def trackerDevice = new TrackerDevice(params)

        assert trackerDevice.save() != null

        // test invalid parameters in update
        params.id = trackerDevice.id
        //TODO: add invalid values to params object

        controller.update()

        assert view == "/trackerDevice/edit"
        assert model.trackerDeviceInstance != null

        trackerDevice.clearErrors()

        populateValidParams(params)
        controller.update()

        assert response.redirectedUrl == "/trackerDevice/show/$trackerDevice.id"
        assert flash.message != null

        //test outdated version number
        response.reset()
        trackerDevice.clearErrors()

        populateValidParams(params)
        params.id = trackerDevice.id
        params.version = -1
        controller.update()

        assert view == "/trackerDevice/edit"
        assert model.trackerDeviceInstance != null
        assert model.trackerDeviceInstance.errors.getFieldError('version')
        assert flash.message != null
    }

    void testDelete() {
        controller.delete()
        assert flash.message != null
        assert response.redirectedUrl == '/trackerDevice/list'

        response.reset()

        populateValidParams(params)
        def trackerDevice = new TrackerDevice(params)

        assert trackerDevice.save() != null
        assert TrackerDevice.count() == 1

        params.id = trackerDevice.id

        controller.delete()

        assert TrackerDevice.count() == 0
        assert TrackerDevice.get(trackerDevice.id) == null
        assert response.redirectedUrl == '/trackerDevice/list'
    }
}
