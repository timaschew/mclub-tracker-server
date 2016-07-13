package mclub.tracker



import org.junit.*
import grails.test.mixin.*

@TestFor(TrackerPositionController)
@Mock(TrackerPosition)
class TrackerPositionControllerTests {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        //params["name"] = 'someValidName'
    }

    void testIndex() {
        controller.index()
        assert "/trackerPosition/list" == response.redirectedUrl
    }

    void testList() {

        def model = controller.list()

        assert model.trackerPositionInstanceList.size() == 0
        assert model.trackerPositionInstanceTotal == 0
    }

    void testCreate() {
        def model = controller.create()

        assert model.trackerPositionInstance != null
    }

    void testSave() {
        controller.save()

        assert model.trackerPositionInstance != null
        assert view == '/trackerPosition/create'

        response.reset()

        populateValidParams(params)
        controller.save()

        assert response.redirectedUrl == '/trackerPosition/show/1'
        assert controller.flash.message != null
        assert TrackerPosition.count() == 1
    }

    void testShow() {
        controller.show()

        assert flash.message != null
        assert response.redirectedUrl == '/trackerPosition/list'

        populateValidParams(params)
        def trackerPosition = new TrackerPosition(params)

        assert trackerPosition.save() != null

        params.id = trackerPosition.id

        def model = controller.show()

        assert model.trackerPositionInstance == trackerPosition
    }

    void testEdit() {
        controller.edit()

        assert flash.message != null
        assert response.redirectedUrl == '/trackerPosition/list'

        populateValidParams(params)
        def trackerPosition = new TrackerPosition(params)

        assert trackerPosition.save() != null

        params.id = trackerPosition.id

        def model = controller.edit()

        assert model.trackerPositionInstance == trackerPosition
    }

    void testUpdate() {
        controller.update()

        assert flash.message != null
        assert response.redirectedUrl == '/trackerPosition/list'

        response.reset()

        populateValidParams(params)
        def trackerPosition = new TrackerPosition(params)

        assert trackerPosition.save() != null

        // test invalid parameters in update
        params.id = trackerPosition.id
        //TODO: add invalid values to params object

        controller.update()

        assert view == "/trackerPosition/edit"
        assert model.trackerPositionInstance != null

        trackerPosition.clearErrors()

        populateValidParams(params)
        controller.update()

        assert response.redirectedUrl == "/trackerPosition/show/$trackerPosition.id"
        assert flash.message != null

        //test outdated version number
        response.reset()
        trackerPosition.clearErrors()

        populateValidParams(params)
        params.id = trackerPosition.id
        params.version = -1
        controller.update()

        assert view == "/trackerPosition/edit"
        assert model.trackerPositionInstance != null
        assert model.trackerPositionInstance.errors.getFieldError('version')
        assert flash.message != null
    }

    void testDelete() {
        controller.delete()
        assert flash.message != null
        assert response.redirectedUrl == '/trackerPosition/list'

        response.reset()

        populateValidParams(params)
        def trackerPosition = new TrackerPosition(params)

        assert trackerPosition.save() != null
        assert TrackerPosition.count() == 1

        params.id = trackerPosition.id

        controller.delete()

        assert TrackerPosition.count() == 0
        assert TrackerPosition.get(trackerPosition.id) == null
        assert response.redirectedUrl == '/trackerPosition/list'
    }
}
