
<%@ page import="mclub.tracker.TrackerDevice" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'trackerDevice.label', default: 'TrackerDevice')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-trackerDevice" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-trackerDevice" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
				<thead>
					<tr>
					
						<g:sortableColumn property="phoneNumber" title="${message(code: 'trackerDevice.phoneNumber.label', default: 'Phone Number')}" />
					
						<g:sortableColumn property="imei" title="${message(code: 'trackerDevice.imei.label', default: 'Imei')}" />
					
						<g:sortableColumn property="latestPositionId" title="${message(code: 'trackerDevice.latestPositionId.label', default: 'Latest Position Id')}" />
					
						<g:sortableColumn property="udid" title="${message(code: 'trackerDevice.udid.label', default: 'Unique Device Id')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${trackerDeviceInstanceList}" status="i" var="trackerDeviceInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${trackerDeviceInstance.id}">${fieldValue(bean: trackerDeviceInstance, field: "phoneNumber")}</g:link></td>
					
						<td>${fieldValue(bean: trackerDeviceInstance, field: "imei")}</td>
					
						<td>${fieldValue(bean: trackerDeviceInstance, field: "latestPositionId")}</td>
					
						<td>${fieldValue(bean: trackerDeviceInstance, field: "udid")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${trackerDeviceInstanceTotal}" />
			</div>
		</div>
	</body>
</html>
