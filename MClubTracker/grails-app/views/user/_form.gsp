<%@ page import="mclub.user.User" %>


<div class="fieldcontain ${this.hasErrors(bean: user, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="user.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${this.user?.name}"/>
</div>

<div class="fieldcontain ${this.hasErrors(bean: user, field: 'phone', 'error')} required">
	<label for="phone">
		<g:message code="user.phone.label" default="Phone" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="phone" required="" value="${this.user?.phone}"/>

</div>

<div class="fieldcontain ${this.hasErrors(bean: user, field: 'type', 'error')} required">
	<label for="type">
		<g:message code="user.type.label" default="Type" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="type" type="number" value="${this.user?.type}" required=""/>

</div>

<div class="fieldcontain ${this.hasErrors(bean: user, field: 'displayName', 'error')} ">
	<label for="displayName">
		<g:message code="user.displayName.label" default="Display Name" />
	</label>
	<g:textField name="displayName" required="" value="${this.user?.displayName}"/>
</div>

<div class="fieldcontain ${this.hasErrors(bean: user, field: 'avatar', 'error')} ">
	<label for="avatar">
		<g:message code="user.avatar.label" default="Avatar" />
		
	</label>
	<g:textField name="avatar" value="${this.user?.avatar}"/>

</div>
