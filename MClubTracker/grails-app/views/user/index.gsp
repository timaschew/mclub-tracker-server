
<%@ page import="mclub.user.User" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'user.label', default: 'User')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-user" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-user" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="name" title="${message(code: 'user.name.label', default: 'Name')}" />
						
						<g:sortableColumn property="displayName" title="${message(code: 'user.displayName.label', default: 'Display Name')}" />
						
						<g:sortableColumn property="phone" title="${message(code: 'user.phone.label', default: 'Phone')}" />
					
						<g:sortableColumn property="type" title="${message(code: 'user.type.label', default: 'Type')}" />
						
						<g:sortableColumn property="creationDate" title="${message(code: 'user.creationDate.label', default: 'Creation Date')}" />
						
						<g:sortableColumn property="avatar" title="${message(code: 'user.avatar.label', default: 'Avatar')}" />
						
						<th>More</th>
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${userInstanceList}" status="i" var="userInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${userInstance.id}">${fieldValue(bean: userInstance, field: "name")}</g:link></td>
						
						<td>${fieldValue(bean: userInstance, field: "displayName")}</td>
					
						<td>${fieldValue(bean: userInstance, field: "phone")}</td>
					
						<td>${fieldValue(bean: userInstance, field: "type")}</td>

						<td><g:formatDate date="${userInstance.creationDate}" /></td>
						
						<td>${fieldValue(bean: userInstance, field: "avatar")}</td>
						<td><g:link action="list" controller="trackerDevice" params="[username:userInstance.name]">Device...</g:link></td>					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${userInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
