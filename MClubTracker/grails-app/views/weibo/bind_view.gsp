<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=GB18030" />
<meta name="layout" content="main" />
<title>Weibo Bind</title>
</head>
<body>
	<div class="body">
		<div>${flash.message}</div>
		<g:form action="bind">
			<div class="fieldcontain">
				<label for="deviceId">Device Id</label>
				<g:textField name="deviceId" value="${bindForm?.deviceId}" />
			</div>
			<div class="fieldcontain">
				<label for="username">Weibo Username</label>
				<g:textField name="username" value="${bindForm?.deviceId}" />
			</div>
			<div class="fieldcontain">
				<label for="password">Weibo Password</label>
				<g:textField name="password" value="${bindForm?.deviceId}" />
			</div>
			<fieldset>
				<g:submitButton name="Bind" class="update" value="Bind" />
			</fieldset>
		</g:form>
	</div>
</body>
</html>