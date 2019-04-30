<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<html>

<head>
  <title>Scoreboard Comparison</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds">Scoreboard Comparison (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">

<table>
<tr><td>Comparing</td><td><%= request.getAttribute("a") %></td></tr>
<tr><td align=right>to</td><td><%= request.getAttribute("b") %></td></tr>
</table>
<p/>

<%= (String) request.getAttribute("compare") %>
</div>

</body>
</html>