
<%@ page import="mclub.tracker.TrackerPosition" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'trackerPosition.label', default: 'TrackerPosition')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-trackerPosition" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-trackerPosition" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list trackerPosition">
				<g:if test="${trackerPositionInstance?.deviceId}">
				<li class="fieldcontain">
					<span id="deviceId-label" class="property-label"><g:message code="trackerPosition.deviceId.label" default="Device Id" /></span>
					
						<span class="property-value" aria-labelledby="deviceId-label"><g:fieldValue bean="${trackerPositionInstance}" field="deviceId"/></span>
					
				</li>
				</g:if>

				<g:if test="${trackerPositionInstance?.latitude}">
				<li class="fieldcontain">
					<span id="latitude-label" class="property-label"><g:message code="trackerPosition.latitude.label" default="Latitude" /></span>
						<span class="property-value" aria-labelledby="latitude-label">${String.format("%.6f",trackerPositionInstance.latitude)}</span>
				</li>
				</g:if>
			
				<g:if test="${trackerPositionInstance?.longitude}">
				<li class="fieldcontain">
					<span id="longitude-label" class="property-label"><g:message code="trackerPosition.longitude.label" default="Longitude" /></span>
						<span class="property-value" aria-labelledby="longitude-label">${String.format("%.6f",trackerPositionInstance.longitude)}</span>
				</li>
				</g:if>
				<g:if test="${trackerPositionInstance?.altitude}">
				<li class="fieldcontain">
					<span id="altitude-label" class="property-label"><g:message code="trackerPosition.altitude.label" default="Altitude" /></span>
					
						<span class="property-value" aria-labelledby="altitude-label"><g:fieldValue bean="${trackerPositionInstance}" field="altitude"/></span>
					
				</li>
				</g:if>
				<g:if test="${trackerPositionInstance?.speed}">
				<li class="fieldcontain">
					<span id="speed-label" class="property-label"><g:message code="trackerPosition.speed.label" default="Speed" /></span>
					
						<span class="property-value" aria-labelledby="speed-label"><g:fieldValue bean="${trackerPositionInstance}" field="speed"/></span>
					
				</li>
				</g:if>
				<g:if test="${trackerPositionInstance?.course}">
				<li class="fieldcontain">
					<span id="course-label" class="property-label"><g:message code="trackerPosition.course.label" default="Course" /></span>
					
						<span class="property-value" aria-labelledby="course-label"><g:fieldValue bean="${trackerPositionInstance}" field="course"/></span>
					
				</li>
				</g:if>
				
				<g:if test="${trackerPositionInstance?.power}">
				<li class="fieldcontain">
					<span id="power-label" class="property-label"><g:message code="trackerPosition.power.label" default="Power" /></span>
					
						<span class="property-value" aria-labelledby="power-label"><g:fieldValue bean="${trackerPositionInstance}" field="power"/></span>
					
				</li>
				</g:if>				

				<g:if test="${trackerPositionInstance?.address}">
				<li class="fieldcontain">
					<span id="address-label" class="property-label"><g:message code="trackerPosition.address.label" default="Address" /></span>
					
						<span class="property-value" aria-labelledby="address-label"><g:fieldValue bean="${trackerPositionInstance}" field="address"/></span>
					
				</li>
				</g:if>

				<g:if test="${trackerPositionInstance?.time}">
				<li class="fieldcontain">
					<span id="time-label" class="property-label"><g:message code="trackerPosition.time.label" default="Time" /></span>
					
						<span class="property-value" aria-labelledby="time-label"><g:formatDate date="${trackerPositionInstance?.time}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerPositionInstance?.valid}">
				<li class="fieldcontain">
					<span id="valid-label" class="property-label"><g:message code="trackerPosition.valid.label" default="Valid" /></span>
					
						<span class="property-value" aria-labelledby="valid-label"><g:formatBoolean boolean="${trackerPositionInstance?.valid}" /></span>
					
				</li>
				</g:if>

				<g:if test="${trackerPositionInstance?.extendedInfo}">
					<li class="fieldcontain">
						<span id="extendedInfo-label" class="property-label"><g:message code="trackerPosition.extendedInfo.label" default="Extended Info" /></span>
						<span class="property-value" aria-labelledby="extendedInfo-label"><g:fieldValue bean="${trackerPositionInstance}" field="extendedInfo"/></span>

					</li>
				</g:if>

			</ol>
			<g:form>
				<fieldset class="buttons">
					<g:hiddenField name="id" value="${trackerPositionInstance?.id}" />
					<g:link class="edit" action="edit" id="${trackerPositionInstance?.id}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
