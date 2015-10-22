
<%@ page import="mclub.tracker.TrackerMap" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'trackerMap.label', default: 'TrackerMap')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-trackerMap" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-trackerMap" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="uniqueId" title="${message(code: 'trackerMap.uniqueId.label', default: 'Unique Id')}" />
					
						<g:sortableColumn property="name" title="${message(code: 'trackerMap.name.label', default: 'Name')}" />
					
						<g:sortableColumn property="filterJSON" title="${message(code: 'trackerMap.filterJSON.label', default: 'Filter JSON')}" />
					
						<g:sortableColumn property="username" title="${message(code: 'trackerMap.username.label', default: 'Username')}" />
					
						<g:sortableColumn property="type" title="${message(code: 'trackerMap.type.label', default: 'Type')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${trackerMapInstanceList}" status="i" var="trackerMapInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${trackerMapInstance.id}">${fieldValue(bean: trackerMapInstance, field: "uniqueId")}</g:link></td>
					
						<td>${fieldValue(bean: trackerMapInstance, field: "name")}</td>
					
						<td>${fieldValue(bean: trackerMapInstance, field: "filterJSON")}</td>
					
						<td>${fieldValue(bean: trackerMapInstance, field: "username")}</td>
					
						<td>${fieldValue(bean: trackerMapInstance, field: "type")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${trackerMapInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
