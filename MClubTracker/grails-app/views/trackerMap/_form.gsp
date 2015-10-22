<%@ page import="mclub.tracker.TrackerMap" %>



<div class="fieldcontain ${hasErrors(bean: trackerMapInstance, field: 'uniqueId', 'error')} required">
	<label for="uniqueId">
		<g:message code="trackerMap.uniqueId.label" default="Map Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="uniqueId" required="" value="${trackerMapInstance?.uniqueId}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: trackerMapInstance, field: 'name', 'error')} ">
	<label for="name">
		<g:message code="trackerMap.name.label" default="Map Title" />
		
	</label>
	<g:textField name="name" value="${trackerMapInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: trackerMapInstance, field: 'username', 'error')} ">
	<label for="username">
		<g:message code="trackerMap.username.label" default="Username" />
		
	</label>
	<g:textField name="username" value="${trackerMapInstance?.username}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: trackerMapInstance, field: 'type', 'error')} required">
	<label for="type">
		<g:message code="trackerMap.type.label" default="Type" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="type" type="number" value="${trackerMapInstance.type}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: trackerMapInstance, field: 'filterJSON', 'error')} ">
	<label for="filterJSON">
		<g:message code="trackerMap.filterJSON.label" default="Filter JSON" />
		
	</label>
	<g:textArea name="filterJSON" value="${trackerMapInstance?.filterJSON}"/>

</div>
