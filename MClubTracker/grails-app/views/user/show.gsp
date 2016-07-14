
<%@ page import="mclub.user.User" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'user.label', default: 'User')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-user" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-user" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list user">
			
				<g:if test="${this.user?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="user.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${user}" field="name"/></span>
					
				</li>
				</g:if>
				
				<g:if test="${this.user?.displayName}">
				<li class="fieldcontain">
					<span id="display-name-label" class="property-label"><g:message code="user.displayName.label" default="Display Name" /></span>
					
						<span class="property-value" aria-labelledby="display-name-label"><g:fieldValue bean="${user}" field="displayName"/></span>
				</li>
				</g:if>
			
				<g:if test="${this.user?.phone}">
				<li class="fieldcontain">
					<span id="phone-label" class="property-label"><g:message code="user.phone.label" default="Phone" /></span>
					
						<span class="property-value" aria-labelledby="phone-label"><g:fieldValue bean="${user}" field="phone"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${this.user?.type}">
				<li class="fieldcontain">
					<span id="type-label" class="property-label"><g:message code="user.type.label" default="Type" /></span>
					
						<span class="property-value" aria-labelledby="type-label"><g:fieldValue bean="${user}" field="type"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${this.user?.creationDate}">
				<li class="fieldcontain">
					<span id="creationDate-label" class="property-label"><g:message code="user.creationDate.label" default="Creation Date" /></span>
					
						<span class="property-value" aria-labelledby="creationDate-label"><g:formatDate date="${user?.creationDate}" /></span>
					
				</li>
				</g:if>			
			
				<g:if test="${this.user?.avatar}">
				<li class="fieldcontain">
					<span id="avatar-label" class="property-label"><g:message code="user.avatar.label" default="Avatar" /></span>
					
						<span class="property-value" aria-labelledby="avatar-label"><g:fieldValue bean="${user}" field="avatar"/></span>
					
				</li>
				</g:if>
			</ol>
			<g:form url="[resource:user, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${user}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
