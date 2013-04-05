<%@ page import="mclub.tracker.TrackerPosition" %>



<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'extendedInfo', 'error')} ">
	<label for="extendedInfo">
		<g:message code="trackerPosition.extendedInfo.label" default="Extended Info" />
		
	</label>
	<g:textField name="extendedInfo" value="${trackerPositionInstance?.extendedInfo}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'address', 'error')} ">
	<label for="address">
		<g:message code="trackerPosition.address.label" default="Address" />
		
	</label>
	<g:textField name="address" value="${trackerPositionInstance?.address}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'altitude', 'error')} required">
	<label for="altitude">
		<g:message code="trackerPosition.altitude.label" default="Altitude" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="altitude" value="${fieldValue(bean: trackerPositionInstance, field: 'altitude')}" required=""/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'course', 'error')} required">
	<label for="course">
		<g:message code="trackerPosition.course.label" default="Course" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="course" value="${fieldValue(bean: trackerPositionInstance, field: 'course')}" required=""/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'deviceId', 'error')} required">
	<label for="deviceId">
		<g:message code="trackerPosition.deviceId.label" default="Device Id" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="deviceId" type="number" value="${trackerPositionInstance.deviceId}" required=""/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'latitude', 'error')} required">
	<label for="latitude">
		<g:message code="trackerPosition.latitude.label" default="Latitude" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="latitude" value="${fieldValue(bean: trackerPositionInstance, field: 'latitude')}" required=""/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'longitude', 'error')} required">
	<label for="longitude">
		<g:message code="trackerPosition.longitude.label" default="Longitude" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="longitude" value="${fieldValue(bean: trackerPositionInstance, field: 'longitude')}" required=""/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'power', 'error')} required">
	<label for="power">
		<g:message code="trackerPosition.power.label" default="Power" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="power" value="${fieldValue(bean: trackerPositionInstance, field: 'power')}" required=""/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'speed', 'error')} required">
	<label for="speed">
		<g:message code="trackerPosition.speed.label" default="Speed" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="speed" value="${fieldValue(bean: trackerPositionInstance, field: 'speed')}" required=""/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'time', 'error')} required">
	<label for="time">
		<g:message code="trackerPosition.time.label" default="Time" />
		<span class="required-indicator">*</span>
	</label>
	<g:datePicker name="time" precision="day"  value="${trackerPositionInstance?.time}"  />
</div>

<div class="fieldcontain ${hasErrors(bean: trackerPositionInstance, field: 'valid', 'error')} ">
	<label for="valid">
		<g:message code="trackerPosition.valid.label" default="Valid" />
		
	</label>
	<g:checkBox name="valid" value="${trackerPositionInstance?.valid}" />
</div>

