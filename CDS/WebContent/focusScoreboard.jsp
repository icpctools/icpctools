<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.contest.model.IRun"%>
<%@page import="org.icpc.tools.contest.model.internal.Contest"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<html>
<head>
  <title>Focus Scoreboard</title>
  <link rel="stylesheet" href="/cds.css">
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<script>
var last;
function sendCommand(id) {
   document.getElementById("team"+id).disabled = true;

   var xmlhttp = new XMLHttpRequest();

   xmlhttp.onreadystatechange = function() {
     document.getElementById("status").innerHTML = "Changing to "+id;
     if (xmlhttp.readyState == 4) {
        if (xmlhttp.status == 200) {
           document.getElementById("status").innerHTML = "Success";
           if (last != null) {
              document.getElementById(last).innerHTML = "-";
           }
           document.getElementById("current"+id).innerHTML = "Now";
           last = "current"+id;
        } else
           document.getElementById("status").innerHTML = xmlhttp.responseText;
        document.getElementById("team"+id).disabled = false;
     }
   }
   
   xmlhttp.open("PUT", "focus/" + id, true);
   xmlhttp.send();
}
</script>

<h1>Focus Scoreboard Control</h1>

Status: <span id="status">ready</span>

<table>
<tr>
<th>Id</th>
<th>Team</th>
<th>Current</th>
</tr>
<%
   Contest contest = CDSConfig.getContests()[0];
	ITeam[] teams = ContestUtil.sort(contest.getTeams());
   for (ITeam t : teams) {
%>

<tr>
<td><button id="team<%= t.getId() %>" onclick="sendCommand('<%= t.getId() %>')"><%= t.getLabel() %></button></td>
<td><%= t.getActualDisplayName() %></td>
<td id="current<%= t.getId() %>">-</td>
</tr>
<% } %>
</table>

</body>
</html>