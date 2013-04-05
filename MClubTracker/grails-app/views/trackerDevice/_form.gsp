<%@ page import="mclub.tracker.TrackerDevice" %>



<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'phoneNumber', 'error')} ">
	<label for="phoneNumber">
		<g:message code="trackerDevice.phoneNumber.label" default="Phone Number" />
		
	</label>
	<g:textField name="phoneNumber" value="${trackerDeviceInstance?.phoneNumber}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'imei', 'error')} ">
	<label for="imei">
		<g:message code="trackerDevice.imei.label" default="Imei" />
		
	</label>
	<g:textField name="imei" value="${trackerDeviceInstance?.imei}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'latestPositionId', 'error')} ">
	<label for="latestPositionId">
		<g:message code="trackerDevice.latestPositionId.label" default="Latest Position Id" />
		
	</label>
	<g:field name="latestPositionId" type="number" value="${trackerDeviceInstance.latestPositionId}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'deviceId', 'error')} ">
	<label for="deviceId">
		<g:message code="trackerDevice.deviceId.label" default="Device Id" />
		
	</label>
	<g:textField name="deviceId" value="${trackerDeviceInstance?.deviceId}"/>
</div>

