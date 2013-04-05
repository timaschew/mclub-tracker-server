
<%@ page import="mclub.tracker.TrackerPosition" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'trackerPosition.label', default: 'TrackerPosition')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-trackerPosition" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-trackerPosition" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
				<thead>
					<tr>
					
						<g:sortableColumn property="extendedInfo" title="${message(code: 'trackerPosition.extendedInfo.label', default: 'Extended Info')}" />
					
						<g:sortableColumn property="address" title="${message(code: 'trackerPosition.address.label', default: 'Address')}" />
					
						<g:sortableColumn property="altitude" title="${message(code: 'trackerPosition.altitude.label', default: 'Altitude')}" />
					
						<g:sortableColumn property="course" title="${message(code: 'trackerPosition.course.label', default: 'Course')}" />
					
						<g:sortableColumn property="deviceId" title="${message(code: 'trackerPosition.deviceId.label', default: 'Device Id')}" />
					
						<g:sortableColumn property="latitude" title="${message(code: 'trackerPosition.latitude.label', default: 'Latitude')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${trackerPositionInstanceList}" status="i" var="trackerPositionInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${trackerPositionInstance.id}">${fieldValue(bean: trackerPositionInstance, field: "extendedInfo")}</g:link></td>
					
						<td>${fieldValue(bean: trackerPositionInstance, field: "address")}</td>
					
						<td>${fieldValue(bean: trackerPositionInstance, field: "altitude")}</td>
					
						<td>${fieldValue(bean: trackerPositionInstance, field: "course")}</td>
					
						<td>${fieldValue(bean: trackerPositionInstance, field: "deviceId")}</td>
					
						<td>${fieldValue(bean: trackerPositionInstance, field: "latitude")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${trackerPositionInstanceTotal}" />
			</div>
		</div>
	</body>
</html>
