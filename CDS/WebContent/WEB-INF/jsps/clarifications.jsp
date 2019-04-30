<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request);
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Contest Clarifications</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Clarifications (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<p>
<a href="<%= webRoot %>">Overview</a> -
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
Clarifications -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>
</p>

<h3><a href="<%= apiRoot %>/clarifications">Clarifications</a> (<%= contest.getClarifications().length %>)</h3>

<table id="clar-table">
<tr><th>Id</th><th align=center>Time</th><th>Problem</th><th>From Team</th><th>To Team</th><th>Text</th></tr>
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
  clars = contest.getClarifications();
  teams = contest.getTeams();
  problems = contest.getProblems();
  for (var i = 0; i < clars.length; i++) {
    var clar = clars[i];
    var problem = '';
    var time = '';
    var fromTeam = '';
    var toTeam = '';
    if (clar.contest_time != null) {
      time = clar.contest_time;
      if (time != null)
        time = time;
    }
    if (clar.problem_id != null) {
      problem = findById(problems, clar.problem_id);
      if (problem != null)
        problem = problem.label + ' (' + problem.id + ')';
    }
    if (clar.from_team_id != null) {
      fromTeam = findById(teams, clar.from_team_id);
      if (fromTeam != null)
        fromTeam = fromTeam.id + ' (' + fromTeam.name + ')';
    }
    if (clar.to_team_id != null) {
      toTeam = findById(teams, clar.to_team_id);
      if (toTeam != null)
        toTeam = toTeam.id + ' (' + toTeam.name + ')';
    }
    
    var col = $('<td><a href="<%= apiRoot %>/clarifications/' + clar.id + '">'+clar.id +'</a></td>'+
      '<td>' + time + '</td><td>' + problem + '</td><td>' + fromTeam + '</td>' +
      '<td>' + toTeam + '</td><td>' + clar.text + '</td>');
    var row = $('<tr></tr>');
    row.append(col);
    $('#clar-table').append(row);
  }
}

sortByColumn($('#clar-table'));

$.when(contest.loadClarifications(),contest.loadTeams(),contest.loadProblems()).done(function() { fillTable() }).fail(function(result) {
    alert("Error loading page: " + result);
  })
})
</script>

</body>
</html>