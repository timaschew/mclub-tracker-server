<%@ page import="mclub.user.User" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Admin Login</title>
	</head>
	<body>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/admin')}"><g:message code="default.home.label"/></a></li>
			</ul>
		</div>
		<div id="login-form" class="content scaffold-edit" role="main">
			<h1>Please Login</h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<g:hasErrors bean="${loginCommand}">
			<ul class="errors" role="alert">
				<g:eachError bean="${loginCommand}" var="error">
				<li <g:if test="${error in org.springframework.validation.FieldError}">data-field-id="${error.field}"</g:if>><g:message error="${error}"/></li>
				</g:eachError>
			</ul>
			</g:hasErrors>
			<g:form method="post" >
				<fieldset class="form">
					<div class="fieldcontain ${hasErrors(bean: loginCommand, field: 'password', 'error')} ">
						<label for="password">
							<g:message code="login.username.label" default="Username" />
						</label>
						<g:textField name="username" value="${loginCommand?.username}"/>
					</div>
					
					<div class="fieldcontain ${hasErrors(bean: loginCommand, field: 'password', 'error')} ">
						<label for="password">
							<g:message code="login.password.label" default="Password" />
						</label>
						<g:textField name="password" value="${loginCommand?.password}"/>
					</div>
				</fieldset>
				<fieldset class="buttons">
					<g:actionSubmit class="save" action="login" value="Login" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
