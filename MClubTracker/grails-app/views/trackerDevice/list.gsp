
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
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
				<li><g:link class="list" action="list" params="[type:0, max:100]">未激活设备</g:link></li>
				<li><g:link class="list" action="list" params="[type:1,max:100]">已激活设备</g:link></li>
				<li><g:link class="list" action="list" params="[type:2,max:100]">APRS设备</g:link></li>
			</ul>
		</div>
		<div id="list-trackerDevice" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /> - Total <%=trackerDeviceInstanceTotal%></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
				<thead>
					<tr>
						<g:sortableColumn property="udid" title="${message(code: 'trackerDevice.udid.label', default: 'UDID')}" />
						<g:sortableColumn property="username" title="${message(code: 'trackerDevice.username.label', default: 'User Name')}" />
						<g:sortableColumn property="status" title="${message(code: 'trackerDevice.status.label', default: 'Status')}" />
						<g:sortableColumn property="latestPositionId" title="${message(code: 'trackerDevice.latestPositionId.label', default: 'Latest Position Id')}" />
						<g:sortableColumn property="latestPositionTime" title="${message(code: 'trackerDevice.latestPositionTime.label', default: 'Latest Position Time')}" />
						<g:sortableColumn property="icon" title="${message(code: 'trackerDevice.icon.label', default: 'Icon')}" />
						<th>More</th>
					</tr>
				</thead>
				<tbody>
				<g:each in="${trackerDeviceInstanceList}" status="i" var="trackerDeviceInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
						<td><g:link action="show" id="${trackerDeviceInstance.id}">${fieldValue(bean: trackerDeviceInstance, field: "udid")}</g:link></td>
						<td>${fieldValue(bean: trackerDeviceInstance, field: "username")}</td>
						<td>${fieldValue(bean: trackerDeviceInstance, field: "status")}</td>
						<td><g:link controller="trackerPosition" action="show" id="${trackerDeviceInstance.latestPositionId}">${fieldValue(bean: trackerDeviceInstance, field: "latestPositionId")}</g:link></td>
						<td>${fieldValue(bean: trackerDeviceInstance, field: "latestPositionTime")}</td>
						<td>${fieldValue(bean: trackerDeviceInstance, field: "icon")}</td>
						<td><g:link action="list" controller="trackerPosition" params="[udid:trackerDeviceInstance.udid,max:100,sort:'id',order:'desc']">positions...</g:link></td>
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<%if(trackerDeviceType != null){%>
				<g:paginate total="${trackerDeviceInstanceTotal}" params="[type:trackerDeviceType]"/>
				<%}else{%>
				<g:paginate total="${trackerDeviceInstanceTotal}" />
				<%}%>
				
			</div>
		</div>
	</body>
</html>
