<!DOCTYPE html>
<html>
	<head>
		<title>View Raw Data</title>
	</head>
	<body>
		<div class="content scaffold-edit" role="main">
			<h1>Raw data viewer</h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<g:if test="${decoded}">
			<div>Decoded:</div>
			<textArea rows="30" cols="100">
<g:each in="${decoded}">${it}
</g:each>
			</textArea>
			</g:if>
			<hr/>
			<div>Input:</div>
			<g:form method="post" action="raw">
				<fieldset class="form">
					<div class="fieldcontain">
						<g:textArea name="hex" rows="30" cols="100">${hex}</g:textArea>
					</div>
				</fieldset>
				<fieldset class="buttons">
					<g:actionSubmit class="save" action="raw" value="Upload" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
