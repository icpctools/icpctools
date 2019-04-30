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
Teams -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>

<h3><a href="<%= apiRoot %>/teams">Teams</a> (<%= contest.getNumTeams() %>)</h3>

<table id="team-table">
<tr><th>Id</th><th>Name</th><th>Organization</th><th></th><th>Organization (formal name)</th><th>Group</th><th>Summary</th></tr>
<tr><td colspan=7>Loading...</td></tr>
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
  $("#team-table").find("tr:gt(0)").remove();
  teams = contest.getTeams();
  orgs = contest.getOrganizations();
  groups = contest.getGroups();
  for (var i = 0; i < teams.length; i++) {
    var team = teams[i];
    var org = findById(orgs, team.organization_id);
    var orgName = '';
    var orgFormalName = '';
    var logoSrc='';
    if (org != null) {
      orgName = org.name;
      if (org.formal_name != null)
        orgFormalName = org.formal_name;
      var logo = bestSquareLogo(org.logo,20);
      if (logo != null)
        logoSrc = '/api/' + logo.href;
    }
    var groupNames = '';
    var groups2 = findGroups(team.group_ids);
    if (groups2 != null) {
      var first = true;
      for (var j = 0; j < groups2.length; j++) {
        if (!first)
          groupNames += ', ';
        groupNames += groups2[j].name;
        first = false;
      }
    }
  
    var col = $('<td><a href="<%= apiRoot %>/teams/' + team.id + '">'+team.id +'</td><td>' + team.name + '</td><td align=center><img src="' + logoSrc + '" height=20/></td><td>' + orgName + '</td><td>' + orgFormalName + '</td><td>' + groupNames + '</td>'
    + '<td><a href="<%= webRoot  %>/teamSummary/' + team.id + '">summary</a></td>');
    var row = $('<tr></tr>');
    row.append(col);
    $('#team-table').append(row);
  }
}

sortByColumn($('#org-table'));

$.when(contest.loadTeams(),contest.loadOrganizations(),contest.loadGroups()).done(function() { fillTable() }).fail(function(result) {
    alert("Error loading page: " + result);
    console.log(result);
  })

})
</script>
</body>
</html>