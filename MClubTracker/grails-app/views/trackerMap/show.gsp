
<%@ page import="mclub.tracker.TrackerMap" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'trackerMap.label', default: 'TrackerMap')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-trackerMap" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-trackerMap" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list trackerMap">
			
				<g:if test="${trackerMapInstance?.uniqueId}">
				<li class="fieldcontain">
					<span id="uniqueId-label" class="property-label"><g:message code="trackerMap.uniqueId.label" default="Unique Id" /></span>
					
						<span class="property-value" aria-labelledby="uniqueId-label"><g:fieldValue bean="${trackerMapInstance}" field="uniqueId"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerMapInstance?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="trackerMap.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${trackerMapInstance}" field="name"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerMapInstance?.filterJSON}">
				<li class="fieldcontain">
					<span id="filterJSON-label" class="property-label"><g:message code="trackerMap.filterJSON.label" default="Filter JSON" /></span>
					
						<span class="property-value" aria-labelledby="filterJSON-label"><g:fieldValue bean="${trackerMapInstance}" field="filterJSON"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerMapInstance?.username}">
				<li class="fieldcontain">
					<span id="username-label" class="property-label"><g:message code="trackerMap.username.label" default="Username" /></span>
					
						<span class="property-value" aria-labelledby="username-label"><g:fieldValue bean="${trackerMapInstance}" field="username"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${trackerMapInstance?.type}">
				<li class="fieldcontain">
					<span id="type-label" class="property-label"><g:message code="trackerMap.type.label" default="Type" /></span>
					
						<span class="property-value" aria-labelledby="type-label"><g:fieldValue bean="${trackerMapInstance}" field="type"/></span>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:trackerMapInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${trackerMapInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
