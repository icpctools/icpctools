<%@page import="java.util.List"%>
<%@page import="java.util.Arrays"%>
<%@page import="org.icpc.tools.contest.Trace"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IGroup"%>
<%@page import="org.icpc.tools.contest.model.IOrganization"%>
<%@page import="org.icpc.tools.contest.model.ILanguage"%>
<%@page import="org.icpc.tools.contest.model.IJudgementType"%>
<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.contest.model.IProblem"%>
<%@page import="org.icpc.tools.contest.model.IState"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request);
   IState state = contest.getState();
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
  <div id="navigation-cds"><%= contest.getFormalName() %> - Details (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<p>
<a href="<%= webRoot %>">Overview</a> -
Details -
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

CDS version: <%= request.getAttribute("version") %>

<h3><a href="<%= apiRoot %>">Contest</a></h3>

<table>
<tr><td><b>Name:</b></td><td><%= contest.getName() %></td>
  <td><b>Problems:</b></td><td><%= contest.getNumProblems() %></td></tr>
<tr><td><b>Start:</b></td><td><%= ContestUtil.formatStartTime(contest) %></td>
  <td><b>Organizations:</b></td><td><%= contest.getNumOrganizations() %></td></tr>
<tr><td><b>Duration:</b></td><td><%= ContestUtil.formatDuration(contest.getDuration()) %></td>
  <td><b>Teams:</b></td><td><%= contest.getNumTeams() %></td></tr>
<tr><td><b>Freeze duration:</b></td><td><%= ContestUtil.formatDuration(contest.getFreezeDuration()) %></td>
  <td><b>Submissions:</b></td><td><%= contest.getNumSubmissions() %></td></tr>
<tr><td><b>Last event:</b></td><td><%= ContestUtil.formatDuration(contest.getContestTimeOfLastEvent()) %></td>
  <td><b>Judgements:</b></td><td><%= contest.getNumJudgements() %></td></tr>

<% String validation = "";
   List<String> validationList = contest.validate();
   if (validationList == null)
      validation = "No errors";
   else if (validationList.size() < 20) {
      for (String s : validationList)
         validation += s + "<br/>";
   } else
      validation = validationList.size() + " errors"; %>
<tr><td valign=top><b>Validation:</b></td><td><a href="<%= webRoot %>/validation"><%= validation %></a></td></tr>
<tr><td valign=top><b>Logo:</b></td><td rowspan=2 id="logo"></td><td valign=top><b>Banner:</b></td><td rowspan=2 id="banner"></td></tr>
</table>

<h3><a href="<%= apiRoot %>/state">State</a></h3>

<table>
  <tr><td><b>Started:</b></td><td><%= ContestUtil.formatStartTime(state.getStarted()) %></td></tr>
  <tr><td><b>Frozen:</b></td><td><%= ContestUtil.formatStartTime(state.getFrozen()) %></td></tr>
  <tr><td><b>Ended:</b></td><td><%= ContestUtil.formatStartTime(state.getEnded()) %></td></tr>
  <tr><td><b>Finalized:</b></td><td><%= ContestUtil.formatStartTime(state.getFinalized()) %></td></tr>
  <tr><td><b>Thawed:</b></td><td><%= ContestUtil.formatStartTime(state.getThawed()) %></td></tr>
  <tr><td><b>End of updates:</b></td><td><%= ContestUtil.formatStartTime(state.getEndOfUpdates()) %></td></tr>
</table>

<h3><a href="<%= apiRoot %>/languages">Languages</a> (<%= contest.getLanguages().length %>)</h3>

<table>
<tr><th>Id</th><th>Name</th></tr>

<% ILanguage[] languages = contest.getLanguages();
   for (int i = 0; i < languages.length; i++) {
     List<String> valList = languages[i].validate(contest);
     String val = null;
     if (valList != null && !valList.isEmpty()) {
       val = "";
       for (String s : valList)
         val += s + "\n";
     } %>
    <tr><td><a href="<%= apiRoot %>/languages/<%= languages[i].getId() %>"><%= languages[i].getId() %></a>
    <% if (val != null) { %>
       <font color="red"><%= val %></font>
     <% } %>
     </td><td><%= languages[i].getName() %></td></tr>
  <% } %>
</table>


<h3><a href="<%= apiRoot %>/judgement-types">Judgement Types</a> (<%= contest.getJudgementTypes().length %>)</h3>

<table>
<tr><th>Id</th><th>Name</th><th>Penalty</th><th>Solved</th></tr>

<% IJudgementType[] judgementTypes = contest.getJudgementTypes();
   for (int i = 0; i < judgementTypes.length; i++) {
     List<String> valList = judgementTypes[i].validate(contest);
     String val = null;
     if (valList != null && !valList.isEmpty()) {
       val = "";
       for (String s : valList)
         val += s + "\n";
     } %>
    <tr><td><a href="<%= apiRoot %>/judgement-types/<%= judgementTypes[i].getId() %>"><%= judgementTypes[i].getId() %></a>
    <% if (val != null) { %>
       <font color="red"><%= val %></font>
     <% } %>
     </td><td><%= judgementTypes[i].getName() %></td><td><%= judgementTypes[i].isPenalty()+"" %></td><td><%= judgementTypes[i].isSolved()+"" %></td></tr>
  <% } %>
</table>


<h3><a href="<%= apiRoot %>/problems">Problems</a> (<%= contest.getNumProblems() %>)</h3>

<table>
<tr><th>Id</th><th>Label</th><th>Name</th><th>Color</th><th>RGB</th></tr>

<% IProblem[] problems = contest.getProblems();
   for (int i = 0; i < problems.length; i++) {
     List<String> valList = problems[i].validate(contest);
     String val = null;
     if (valList != null && !valList.isEmpty()) {
       val = "";
       for (String s : valList)
         val += s + "\n";
     } %>
    <tr><td><a href="<%= apiRoot %>/problems/<%= problems[i].getId() %>"><%= problems[i].getId() %></a>
    <% if (val != null) { %>
       <font color="red"><%= val %></font>
     <% } %>
     </td><td><%= problems[i].getLabel() %></td><td><%= problems[i].getName() %></td><td><%= problems[i].getColor() %></td><td><%= problems[i].getRGB() %></td></tr>
  <% } %>
</table>


<h3><a href="<%= apiRoot %>/groups">Groups</a> (<%= contest.getGroups().length %>)</h3>

<table>
<tr><th>Id</th><th>ICPC Id</th><th>Name</th><th>Type</th><th>Hidden</th></tr>

<% IGroup[] groups = contest.getGroups();
   for (int i = 0; i < groups.length; i++) {
     List<String> valList = groups[i].validate(contest);
     String val = null;
     if (valList != null && !valList.isEmpty()) {
       val = "";
       for (String s : valList)
         val += s + "\n";
     }
     String typ = "";
     if (groups[i].getGroupType() != null)
       typ = groups[i].getGroupType();
     String hidden = "";
     if (groups[i].isHidden())
       hidden = "true"; %>
     <tr><td><a href="<%= apiRoot %>/groups/<%= groups[i].getId() %>"><%= groups[i].getId() %></a>
     <% if (val != null) { %>
       <font color="red"><%= val %></font>
     <% } %>
     </td><td><%= groups[i].getICPCId() %></td><td><%= groups[i].getName() %></td><td><%= typ %></td><td><%= hidden %></td></tr>
  <% } %>
</table>

</div>

<script src="/js/jquery-3.3.1.min.js"></script>
<script src="/js/model.js"></script>
<script src="/js/contest.js"></script>
<script type="text/javascript">
$(document).ready(function() {
contest.setContestId("<%= cc.getId() %>");

function update() {
  var info = contest.getInfo();
  var logo = bestSquareLogo(info.logo,50);
  console.log(info.name + " - " + info.logo + " -> " + logo);
  if (logo != null) {
    var elem = document.createElement("img");
    elem.setAttribute("src", "/api/" + logo.href);
    elem.setAttribute("height", "40");
    document.getElementById("logo").appendChild(elem);
  }
  var banner = bestLogo(info.banner,100, 50);
  console.log(info.name + " - " + info.banner + " -> " + banner);
  if (banner != null) {
    var elem = document.createElement("img");
    elem.setAttribute("src", "/api/" + banner.href);
    elem.setAttribute("height", "40");
    document.getElementById("banner").appendChild(elem);
  }
}

$.when(contest.loadInfo()).done(function() { update() }).fail(function(result) {
    alert("Error loading page: " + result);
  })
})
</script>

</body>
</html>