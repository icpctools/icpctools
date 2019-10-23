<%@page import="java.util.List"%>
<%@page import="java.util.Arrays"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IOrganization"%>
<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<html>

<head>
  <title>Contest Comparison</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds">Contest Comparison (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">

<table>
<tr><td>Comparing</td><td><%= request.getAttribute("a") %></td></tr>
<tr><td align=right>to</td><td><%= request.getAttribute("b") %></td></tr>
</table>
<p/>

<table>
<tr><td valign=top>Contest</td><td><%= (String) request.getAttribute("info") %></td></tr>
<tr><td valign=top>Languages</td><td><%= (String) request.getAttribute("languages") %></td></tr>
<tr><td valign=top>Judgement Types</td><td><%= (String) request.getAttribute("judgement-types") %></td></tr>
<tr><td valign=top>Problems</td><td><%= (String) request.getAttribute("problems") %></td></tr>
<tr><td valign=top>Groups</td><td><%= (String) request.getAttribute("groups") %></td></tr>
<tr><td valign=top>Organizations</td><td><%= (String) request.getAttribute("organizations") %></td></tr>
<tr><td valign=top>Teams</td><td><%= (String) request.getAttribute("teams") %></td></tr>
<tr><td valign=top>Submissions</td><td><%= (String) request.getAttribute("submissions") %></td></tr>
<tr><td valign=top>Judgements</td><td><%= (String) request.getAttribute("judgements") %></td></tr>
<tr><td valign=top>Awards</td><td><%= (String) request.getAttribute("awards") %></td></tr>
</table>

</div>

</body>
</html>