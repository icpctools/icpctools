<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request);
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Contest Details</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Teams (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<a href="<%= webRoot %>">Overview</a> -
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
Awards -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>

<h3><a href="<%= apiRoot %>/awards">Awards</a> (<%= contest.getAwards().length %>)</h3>

<table id="award-table">
<tr><th>Id</th><th>Citation</th><th>Teams</th></tr>
<tr><td colspan=3>Loading...</td></tr>
</table>

</div>

<script src="/js/jquery-3.3.1.min.js"></script>
<script src="/js/model.js"></script>
<script src="/js/contest.js"></script>
<script src="/js/ui.js"></script>
<script type="text/javascript">
$(document).ready(function() {
contest.setContestId("<%= cc.getId() %>");

function fillTable() {
  $("#award-table").find("tr:gt(0)").remove();
  awards = contest.getAwards();
  teams = contest.getTeams();
  for (var i = 0; i < awards.length; i++) {
    var award = awards[i];
  
    var teamsStr = "";
    for (var j = 0; j < award.team_ids.length; j++) {
       if (j > 0)
         teamsStr += "<br>";
       teamsStr += award.team_ids[j] + ": ";
       var t = contest.getTeamById(award.team_ids[j]);
       if (t != null)
          teamsStr += t.name;
    }
    var col = $('<td><a href="<%= apiRoot %>/awards/' + award.id + '">' + award.id + '</td><td>' + award.citation + '</td><td>' + teamsStr + '</td>');
    var row = $('<tr></tr>');
    row.append(col);
    $('#award-table').append(row);
  }
}

$.when(contest.loadAwards(),contest.loadTeams()).done(function() { fillTable() }).fail(function(result) {
    alert("Error loading page: " + result);
    console.log(result);
  })

})
</script>
</body>
</html>