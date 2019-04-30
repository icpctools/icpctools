<%@page import="java.io.File"%>
<%
 String folder = (String) request.getAttribute("folder");
 File[] files = (File[]) request.getAttribute("files");
%>

<html>
<head>
  <title>Directory <%= folder %></title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>
<h1>Directory listing for <%= folder %></h1>

<table>
<tr><th>Files</th></tr>
<% for (File f : files) { %>
<tr><td><a href="<%= f.getName() %>"><%= f.getName() %></a></td></tr>
<% } %>

</table>

</body>
</html>