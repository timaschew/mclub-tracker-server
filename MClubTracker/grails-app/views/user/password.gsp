<%@ page import="mclub.user.User" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Change Password</title>
	</head>
	<body>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
			</ul>
		</div>
		<div id="login-form" class="content scaffold-edit" role="main">
			<h1>Welcome ${session['user']?.name}, Please Input</h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<g:hasErrors bean="${passwordChangeCommand}">
			<ul class="errors" role="alert">
				<g:eachError bean="${passwordChangeCommand}" var="error">
				<li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message error="${error}"/></li>
				</g:eachError>
			</ul>
			</g:hasErrors>
			<g:form method="post" >
				<fieldset class="form">
					<div class="fieldcontain ${hasErrors(bean: passwordChangeCommand, field: 'oldPassword', 'error')} ">
						<label for="oldPassword">
							<g:message code="login.oldpassword.label" default="Old Password" />
						</label>
						<g:passwordField name="oldPassword"/>
					</div>
					
					<div class="fieldcontain ${hasErrors(bean: passwordChangeCommand, field: 'newPassword1', 'error')} ">
						<label for="newPassword1">
							<g:message code="login.newpassword1.label" default="New Password" />
						</label>
						<g:passwordField name="newPassword1"/>
					</div>
					
					<div class="fieldcontain ${hasErrors(bean: passwordChangeCommand, field: 'newPassword2', 'error')} ">
						<label for="newPassword2">
							<g:message code="login.newpassword2.label" default="New Password Confirmed" />
						</label>
						<g:passwordField name="newPassword2"/>
					</div>
				</fieldset>
				<fieldset class="buttons">
					<g:actionSubmit class="save" action="password" value="Change Password!" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
