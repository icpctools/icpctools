<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IProblem"%>
<%@page import="org.icpc.tools.cds.CDSConfig"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<%@page import="org.icpc.tools.cds.util.Role"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request);
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Contest Scoreboard</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Unofficial Scoreboard (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<p>
<a href="<%= webRoot %>">Overview</a> -
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
Scoreboard -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>
</p>

<% if (Role.isBlue(request)) { %>
Compare to: 
<% ConfiguredContest[] ccs = CDSConfig.getContests();
   for (ConfiguredContest cc2 : ccs)
     if (!cc2.equals(cc)) { %>
   <a href="<%= webRoot%>/scoreboardCompare/<%= cc2.getId() %>"><%= cc2.getId() %></a>&nbsp;&nbsp;
<% } %>

<a href="<%= webRoot%>/scoreboardCompare/compare2src">source</a>

<% } %>
<table id="score-table">
<tr><th align=right>Rank</th><th></th><th>Team</th><th>Organization</th>
<% int numProblems = contest.getNumProblems();
   for (int j = 0; j < numProblems; j++) {
     IProblem p = contest.getProblems()[j]; %>

<th align=center valign=center><%= p.getLabel() %><div class="circle" style="background-color: <%= p.getRGB() %>"></div></th>
<% } %>

<th align=right>Solved</th><th align=right>Time</th></tr>

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
  score = contest.getScoreboard();
  teams = contest.getTeams();
  orgs = contest.getOrganizations();
  problems = contest.getProblems();
  for (var i = 0; i < score.rows.length; i++) {
    var scr = score.rows[i];
    var logoSrc='';
    var team = '';
    var org = '';
    if (scr.team_id != null) {
      team = findById(teams, scr.team_id);
      if (team != null) {
        org = findById(orgs, team.organization_id);
        if (org != null) {
          var logo = bestSquareLogo(org.logo,20);
          if (logo != null)
            logoSrc = '/api/' + logo.href;
          org = org.name;
        }
        team = team.id + ': ' + team.name;
      }
    }
  
    var col = $('<td align=right>'+ scr.rank +'</td><td align=center><img src="' + logoSrc + '" height=20/></td><td>' + team + '</td><td>' + org + '</td>');
    var row = $('<tr></tr>');
    row.append(col);
    for (var j = 0; j < problems.length; j++) {
      var prb = $('<td align=center></td>');
      for (var k = 0; k < scr.problems.length; k++) {
        var prob = scr.problems[k];
        if (prob.problem_id == problems[j].id) {
          var p = '<td align=center';
          if (prob.first_to_solve == true)
            p += " bgcolor=#00aa00";
          else if (prob.solved == true)
            p += " bgcolor=#44dd44";
          else if (prob.num_judged > 0)
            p += " bgcolor=#ee4444";
          else
            p += " bgcolor=#dddd44";
          p += '>' + prob.num_judged;
          if (prob.solved)
            p += ' / ' + prob.time;
          p += '</td>';
          prb = $(p);
        }
      }
      row.append(prb);
    }
    var col2 = $('<td align=right>' + scr.score.num_solved + '</td><td align=right>' + scr.score.total_time + '</td>');
    row.append(col2);
    $('#score-table').append(row);
  }
}

sortByColumn($('#score-table'));

$.when(contest.loadOrganizations(),contest.loadTeams(),contest.loadProblems(),contest.loadScoreboard()).done(function() { fillTable() }).fail(function(result) {
    alert("Error loading page: " + result);
  })
})
</script>

</body>
</html>