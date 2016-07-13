<%@ page import="mclub.user.User" %>


<div class="fieldcontain ${hasErrors(bean: userInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="user.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${userInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: userInstance, field: 'phone', 'error')} required">
	<label for="phone">
		<g:message code="user.phone.label" default="Phone" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="phone" required="" value="${userInstance?.phone}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: userInstance, field: 'type', 'error')} required">
	<label for="type">
		<g:message code="user.type.label" default="Type" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="type" type="number" value="${userInstance?.type}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: userInstance, field: 'displayName', 'error')} ">
	<label for="displayName">
		<g:message code="user.displayName.label" default="Display Name" />
	</label>
	<g:textField name="displayName" required="" value="${userInstance?.displayName}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: userInstance, field: 'avatar', 'error')} ">
	<label for="avatar">
		<g:message code="user.avatar.label" default="Avatar" />
		
	</label>
	<g:textField name="avatar" value="${userInstance?.avatar}"/>

</div>
