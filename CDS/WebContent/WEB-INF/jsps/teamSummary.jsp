<%@page import="java.util.List"%>
<%@page import="java.util.Arrays"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IGroup"%>
<%@page import="org.icpc.tools.contest.model.IOrganization"%>
<%@page import="org.icpc.tools.contest.model.ILanguage"%>
<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.contest.model.IProblem"%>
<%@page import="org.icpc.tools.contest.model.ISubmission"%>
<%@page import="org.icpc.tools.contest.model.IJudgement"%>
<%@page import="org.icpc.tools.contest.model.IJudgementType"%>
<%@page import="org.icpc.tools.contest.model.IRun"%>
<%@page import="org.icpc.tools.contest.model.IStanding"%>
<%@page import="org.icpc.tools.contest.model.IResult"%>
<%@page import="org.icpc.tools.contest.model.Status"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request);
   String teamId = (String) request.getAttribute("teamId");
   ITeam team = contest.getTeamById(teamId);
   IStanding st = contest.getStanding(team);
   IOrganization org = contest.getOrganizationById(team.getOrganizationId());
   IGroup[] groups2 = contest.getGroupsByIds(team.getGroupIds());
     String groupName = "";
     if (groups2 != null) {
       boolean first = true;
       for (IGroup group : groups2) {
         if (!first)
           groupName += ", ";
         groupName += group.getName();
         first = false;
       }
     }
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Team Summary</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Team Summary (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<p>
<a href="<%= webRoot %>">Overview</a> -
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/video/status">Video</a>
</p>

<h3>Team Summary</h3>

<table>
<tr><td><b>Id:</b></td><td><%= team.getId() %></td></tr>
<tr><td><b>Name:</b></td><td><%= team.getName() %></td></tr>
<tr><td><b>Group:</b></td><td><%= groupName %></td></tr>
<% if (org != null) { %>
  <tr><td><b>Org id:</b></td><td><%= org.getId() %></td></tr>
  <tr><td><b>Org formal name:</b></td><td><%= org.getFormalName() %></td></tr>
  <tr><td><b>Org name:</b></td><td><%= org.getName() %></td></tr>
  <tr><td><b>Country:</b></td><td><%= org.getCountry() %></td></tr>
<% } %>
</table>

<h3>Scoreboard</h3>

<table>
<tr><th align=right>Rank</th>
<% int numProblems = contest.getNumProblems();
   for (int j = 0; j < numProblems; j++) {
     IProblem p = contest.getProblems()[j]; %>

<th align=center><%= p.getLabel() %></th>
<% } %>

<th align=right>Solved</th><th align=right>Time</th></tr>

<tr><td align=right><%= st.getRank() %></td>

<% for (int j = 0; j < numProblems; j++) {
     IResult r = contest.getResult(team, j);
     String time = ContestUtil.getTime(r.getContestTime()); %>

<td align=center
<% if (r.isFirstToSolve()) { %>
  bgcolor=#00aa00
<% } else if (r.getStatus() == Status.SOLVED) { %>
  bgcolor=#44dd44
<% } else if (r.getStatus() == Status.FAILED) { %>
  bgcolor=#ee4444
<% } else if (r.getStatus() == Status.SUBMITTED) { %>
  bgcolor=#dddd44
<% } %>
><% if (r.getNumSubmissions() > 0) { %><%= r.getNumSubmissions() + " / " + time %><% } %></td>
<% } %>

<td align=right><%= st.getNumSolved() %></td>
<td align=right><%= st.getTime() %></td>
</tr>
</table>

<h3>Submissions</h3>

<table>
<tr><th>Id</th><th align=center>Time</th><th>Problem</th><th>Language</th><th>Judgements</th></tr>

<% ISubmission[] subs = contest.getSubmissions();
   for (int i = 0; i < subs.length; i++) {
     String id = subs[i].getTeamId();
     if (id.equals(teamId)) { 
     id = subs[i].getId();
     String judgeStr = "";
     String judgeColor = "";
     if (id != null) {
       IJudgement[] jud = contest.getJudgementsBySubmissionId(id);
       if (jud != null) {
         for (IJudgement j : jud) {
           IJudgementType jt = contest.getJudgementTypeById(j.getJudgementTypeId());
           if (jt != null) {
              judgeStr += jt.getName();
              if (jt.isSolved())
                judgeColor = "bgcolor=#44dd44";
              else if (jt.isPenalty())
                judgeColor = "bgcolor=#ee4444";
           } else {
              judgeColor = "bgcolor=#4444ee";
              judgeStr += "...";
           }
           judgeStr += " (<a href=\""+ apiRoot + "/judgements/" + j.getId() + "\">" + j.getId() + "</a>) ";
           /*IRun[] runs = contest.getRunsByJudgementId(j.getId());
           if (runs != null) {
              //judgeStr += runs.length;
              for (IRun r : runs) {
                 judgeStr += "<a href=\""+ apiRoot + "/runs/" + r.getId() + "\">" +r.getId() + "</a> ";
              }
           }
           judgeStr += "]";*/
         }
       }
     }
     
     String langStr = "";
     id = subs[i].getLanguageId();
     if (id != null) {
       ILanguage lang = contest.getLanguageById(id);
       if (lang != null)
         langStr = lang.getName();
       else
         langStr = "<font color=\"red\">" + id + "</font>";
     }
     
     String probStr = "";
     id = subs[i].getProblemId();
     if (id != null) {
       IProblem prob = contest.getProblemById(id);
       if (prob != null)
         probStr = id + " (" + prob.getLabel() + ")";
       else
         probStr = "<font color=\"red\">" + id + "</font>";
     }
     
     List<String> valList = subs[i].validate(contest);
     String val = null;
     if (valList != null && !valList.isEmpty()) {
       val = "";
       for (String s : valList)
         val += s + "\n";
     } %>
     <tr><td><a href="<%= apiRoot %>/submissions/<%= subs[i].getId() %>"><%= subs[i].getId() %></a>
     <% if (val != null) { %>
       <font color="red"><%= val %></font>
     <% } %>
     </td><td align=center><%= ContestUtil.formatTime(subs[i].getContestTime()) %></td><td><%= probStr %></td><td><%= langStr %></td><td <%= judgeColor %>><%= judgeStr %></td></tr>
  <% } } %>
</table>

</div>

</body>
</html>