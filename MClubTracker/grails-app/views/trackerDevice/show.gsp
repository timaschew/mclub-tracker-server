
<%@ page import="mclub.tracker.TrackerDevice" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'trackerDevice.label', default: 'TrackerDevice')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-trackerDevice" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-trackerDevice" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list trackerDevice">
			
				<g:if test="${trackerDeviceInstance?.phoneNumber}">
				<li class="fieldcontain">
					<span id="phoneNumber-label" class="property-label"><g:message code="trackerDevice.phoneNumber.label" default="Phone Number" /></span>
					
						<span class="property-value" aria-labelledby="phoneNumber-label"><g:fieldValue bean="${trackerDeviceInstance}" field="phoneNumber"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerDeviceInstance?.imei}">
				<li class="fieldcontain">
					<span id="imei-label" class="property-label"><g:message code="trackerDevice.imei.label" default="Imei" /></span>
					
						<span class="property-value" aria-labelledby="imei-label"><g:fieldValue bean="${trackerDeviceInstance}" field="imei"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerDeviceInstance?.latestPositionId}">
				<li class="fieldcontain">
					<span id="latestPositionId-label" class="property-label"><g:message code="trackerDevice.latestPositionId.label" default="Latest Position Id" /></span>
					
						<span class="property-value" aria-labelledby="latestPositionId-label"><g:fieldValue bean="${trackerDeviceInstance}" field="latestPositionId"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerDeviceInstance?.deviceId}">
				<li class="fieldcontain">
					<span id="deviceId-label" class="property-label"><g:message code="trackerDevice.deviceId.label" default="Device Id" /></span>
					
						<span class="property-value" aria-labelledby="deviceId-label"><g:fieldValue bean="${trackerDeviceInstance}" field="deviceId"/></span>
					
				</li>
				</g:if>
			
			</ol>
			<g:form>
				<fieldset class="buttons">
					<g:hiddenField name="id" value="${trackerDeviceInstance?.id}" />
					<g:link class="edit" action="edit" id="${trackerDeviceInstance?.id}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
