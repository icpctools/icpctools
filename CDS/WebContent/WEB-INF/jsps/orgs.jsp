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
  <div id="navigation-cds"><%= contest.getFormalName() %> - Organizations (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<a href="<%= webRoot %>">Overview</a> -
<a href="<%= webRoot %>/details">Details</a> -
Organizations -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>

<h3><a href="<%= apiRoot %>/organizations">Organizations</a> (<%= contest.getOrganizations().length %>)</h3>

<table id="org-table">
<tr><th>Id</th><th></th><th>Name</th><th>Formal Name</th><th>Country</th></tr>
<tr><td colspan=5>Loading...</td></tr>
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
  $("#org-table").find("tr:gt(0)").remove();
  orgs = contest.getOrganizations();
  for (var i = 0; i < orgs.length; i++) {
    var org = orgs[i];
    var logoSrc='';
    var logo = bestSquareLogo(org.logo,20);
    if (logo != null)
      logoSrc = '/api/' + logo.href;
      
    var formal_name = '';
    if (org.formal_name != null)
      formal_name = org.formal_name;
      
    var country = '';
    if (org.country != null)
      country = org.country;
  
    var col = $('<td><a href="<%= apiRoot %>/organizations/' + org.id + '">'+org.id +'</a></td><td align=middle><img src="' + logoSrc + '" height=20/></td>'+
      '<td>' + org.name + '</td><td>' + formal_name + '</td><td>' + country + '</td>');
    var row = $('<tr></tr>');
    row.append(col);
    $('#org-table').append(row);
  }
}

sortByColumn($('#org-table'));

$.when(contest.loadOrganizations()).done(function() { fillTable() }).fail(function(result) {
    alert("Error loading page: " + result);
  })
})
</script>

</body>
</html>