<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.contest.model.IRun"%>
<%@page import="org.icpc.tools.contest.model.internal.Contest"%>
<html>
<head>
  <title>Solved Submissions</title>
  <link rel="stylesheet" href="/cds.css">
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<h1>Solved Submissions</h1>

<span id="status"></span>

<table>
<tr>
<th>Id</th>
<th>Team</th>
<th>Video</th>
</tr>
<%
   Contest contest = CDSConfig.getContests()[0];
	IRun[] runs = contest.getRuns();
   for (IRun r : runs) {
      if (!r.isSolved())
         continue;
      ITeam team = contest.getTeamById(r.getTeamId());
%>

<tr>
<td><%= r.getId() %></td>
<td><%= team.getId() %> - <%= team.getName() %></td>
<td><a href="/video/reaction/<%= r.getId() %>">Reaction</a></td>
</tr>
<% } %>
</table>

</body>
</html>