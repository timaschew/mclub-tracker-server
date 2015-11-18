<%@ page import="mclub.tracker.TrackerDevice" %>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'udid', 'error')} ">
	<label for="udid">
		<g:message code="trackerDevice.udid.label" default="UDID" />
		
	</label>
	<g:textField name="udid" value="${trackerDeviceInstance?.udid}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'username', 'error')} ">
	<label for="username">
		<g:message code="trackerDevice.username.label" default="User Name" />
		
	</label>
	<g:textField name="username" value="${trackerDeviceInstance?.username}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'latestPositionId', 'error')} ">
	<label for="latestPositionId">
		<g:message code="trackerDevice.latestPositionId.label" default="Latest Position Id" />
		
	</label>
	<g:field name="latestPositionId" type="number" value="${trackerDeviceInstance.latestPositionId}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'status', 'error')} ">
	<label for="status">
		<g:message code="trackerDevice.status.label" default="Status" />
		
	</label>
	<g:field name="status" type="number" value="${trackerDeviceInstance.status}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'icon', 'error')} ">
	<label for="icon">
		<g:message code="trackerDevice.icon.label" default="Icon" />
		
	</label>
	<g:textField name="icon" value="${trackerDeviceInstance.icon}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: trackerDeviceInstance, field: 'comments', 'error')} ">
	<label for="comments">
		<g:message code="trackerDevice.comments.label" default="Comments" />
		
	</label>
	<g:textField name="comments" value="${trackerDeviceInstance.comments}"/>
</div>