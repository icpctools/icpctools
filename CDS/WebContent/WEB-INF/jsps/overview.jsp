<%@page import="java.util.List"%>
<%@page import="java.util.Arrays"%>
<%@page import="org.icpc.tools.cds.CDSConfig"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IState"%>
<%@page import="org.icpc.tools.contest.model.IGroup"%>
<%@page import="org.icpc.tools.contest.model.IOrganization"%>
<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.contest.model.IProblem"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request);
   IState state = contest.getState();
   long[] metrics = cc.getMetrics();
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Contest Overview</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<script>
function validateSource() {
   var id = "sourceValidation";
   document.getElementById(id).innerHTML = "?";
   
   var xmlhttp = new XMLHttpRequest(); 
   xmlhttp.onreadystatechange = function() {
     if (xmlhttp.readyState == 4) {
        if (xmlhttp.status == 200)
           document.getElementById(id).innerHTML = xmlhttp.responseText;
        else
           document.getElementById(id).innerHTML = "<font color='red'>FAIL</font>";
     }
   };
   
   xmlhttp.open("GET", "<%= webRoot %>/validate");
   xmlhttp.send();
}
</script>

<body onload="validateSource()">

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Overview (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<p>
Overview -
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>
</p>

<% String validation = "";
   List<String> validationList = contest.validate();
   if (validationList == null)
      validation = "No errors";
   else if (validationList.size() < 20) {
      for (String s : validationList)
         validation += s + "<br/>";
   } else
      validation = validationList.size() + " errors"; %>

<table>
<tr><td><b>Name:</b></td><td><%= contest.getName() %></td>
  <td><b>Problems:</b></td><td><%= contest.getNumProblems() %></td><td><a href="<%= apiRoot %>/problems">API</a></td></tr>
<tr><td><b>Start:</b></td><td><%= ContestUtil.formatStartTime(contest) %></td>
  <td><b>Organizations:</b></td><td><%= contest.getNumOrganizations() %></td><td><a href="<%= apiRoot %>/organizations">API</a></td></tr>
<tr><td><b>Duration:</b></td><td><%= ContestUtil.formatDuration(contest.getDuration()) %></td>
  <td><b>Teams:</b></td><td><%= contest.getNumTeams() %></td><td><a href="<%= apiRoot %>/teams">API</a></td></tr>
<tr><td><b>Freeze duration:</b></td><td><%= ContestUtil.formatDuration(contest.getFreezeDuration()) %></td>
  <td><b>Submissions:</b></td><td><%= contest.getNumSubmissions() %></td><td><a href="<%= apiRoot %>/submissions">API</a></td></tr>
<tr><td><b>Last event:</b></td><td><%= ContestUtil.formatDuration(contest.getContestTimeOfLastEvent()) %></td>
  <td><b>Judgements:</b></td><td><%= contest.getNumJudgements() %></td><td><a href="<%= apiRoot %>/judgements">API</a></td></tr>
<tr><td valign=top><b>Validation:</b></td><td><a href="<%= webRoot %>/validation"><%= validation %></a></td>
  <td><b>Runs:</b></td><td><%= contest.getNumRuns() %></td><td><a href="<%= apiRoot %>/runs">API</a></td></tr>
<tr><td><b>Current time:</b></td><td><% if (state.getStarted() == null) { %>Not started
   <% } else if (state.getEnded() != null) { %>Finished
   <% } else { %><%= ContestUtil.formatTime((long) ((System.currentTimeMillis() - state.getStarted()) * contest.getTimeMultiplier())) %><% } %></td>
  <td><b>Clarifications:</b></td><td><%= contest.getNumClarifications() %></td><td><a href="<%= apiRoot %>/clarifications">API</a></td></tr>
</table>

<p>
Compare to:
<% ConfiguredContest[] ccs = CDSConfig.getContests();
   for (ConfiguredContest cc2 : ccs)
     if (!cc2.equals(cc)) { %>
   <a href="<%= webRoot%>/contestCompare/<%= cc2.getId() %>"><%= cc2.getId() %></a>&nbsp;&nbsp;
<% } %>
</p>

Freeze details & verification: <a href="<%= webRoot%>/freeze">here</a>.
<p/>

<h2>Clients currently connected to feed:</h2>
<% List<String> efClients = cc.getClients();
  if (efClients == null || efClients.isEmpty()) { %>
  <ul><li>None</li></ul>
<% } else {%>
<ol>
<%   for (String s : efClients) { %>
    <li><%= s %></li>
<%   } %>
</ol>
<% } %>


<h2>Contest Data Requests</h2>
<table>
<tr><td>REST:</td><td><%= metrics[0] %></td></tr>
<tr><td>Feed:</td><td><%= metrics[1] %></td></tr>
<tr><td>Websocket:</td><td><%= metrics[2] %></td></tr>
<tr><td>Web:</td><td><%= metrics[3] %></td></tr>
<tr><td>Download:</td><td><%= metrics[4] %></td></tr>
<tr><td>Scoreboard:</td><td><%= metrics[5] %></td></tr>
<tr><td>XML:</td><td><%= metrics[6] %></td></tr>
<tr><td>Desktop:</td><td><%= metrics[7] %></td></tr>
<tr><td>Webcam:</td><td><%= metrics[8] %></td></tr>
</table>


<h2>Configuration</h2>
<table>
<tr><td><b>Contest location:</b></td><td><%= cc.getLocation() %></td></tr>
<tr><td><b>CCS:</b></td><td><%= cc.getCCS() %></td></tr>
<tr><td><b>Test:</b></td><td><%= cc.getTest() %></td></tr>
<tr><td><b>Video:</b></td><td><%= cc.getVideo() %></td></tr>
<tr><td valign=top><b>Validation:</b></td><td id="sourceValidation">-</td></tr>
</table>
</div>

</body>
</html>
