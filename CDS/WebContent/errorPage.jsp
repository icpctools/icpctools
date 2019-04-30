<%@ page isErrorPage="true" %>
<html>
<head>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>
<body>
<h1>${pageContext.errorData.statusCode} Error</h1>

<p>(Detail: ${pageContext.errorData.throwable.message})</p>

<p>For up to date information on the services provided, please see the
<a href="https://clics.ecs.baylor.edu/index.php/CDS">CLICS CDS documentation</a>.</p>

</body>
</html>