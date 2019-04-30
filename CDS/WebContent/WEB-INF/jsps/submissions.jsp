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
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request);
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Contest Submissions</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Submissions (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<p>
<a href="<%= webRoot %>">Overview</a> -
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
Submissions -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>
</p>

<h3><a href="<%= apiRoot %>/submissions">Submissions</a> (<%= contest.getNumSubmissions() %>)</h3>
<h3><a href="<%= apiRoot %>/judgements">Judgements</a> (<%= contest.getNumJudgements() %>)</h3>
<h3><a href="<%= apiRoot %>/runs">Runs</a> (<%= contest.getNumRuns() %>)</h3>

<b>Judge Queue</b>
<table>
<tr><th>Id</th><th align=center>Time</th><th>Problem</th><th>Language</th><th>Team</th><th>Organization</th></tr>

<% ISubmission[] subs = contest.getSubmissions();
   int numJudging = 0;
   for (int i = 0; i < subs.length; i++) {
     String id = subs[i].getTeamId();
     String teamStr = "";
     String orgStr = "";
     if (id != null) {
       ITeam team = contest.getTeamById(id);
       if (team != null) {
         teamStr = id + ": " + team.getName();
         IOrganization org = contest.getOrganizationById(team.getOrganizationId());
         if (org != null)
            orgStr = org.getName();
       }
     }
     
     id = subs[i].getId();
     boolean judged = false;
     if (id != null) {
       IJudgement[] jud = contest.getJudgementsBySubmissionId(id);
       if (jud != null) {
         for (IJudgement j : jud) {
            if (j.getJudgementTypeId() != null)
               judged = true;
         }
       }
     }
     if (judged)
       continue;
     numJudging++;
     
     String langStr = "";
     id = subs[i].getLanguageId();
     if (id != null) {
       ILanguage lang = contest.getLanguageById(id);
       if (lang != null)
         langStr = lang.getName();
     }
     
     String probStr = "";
     id = subs[i].getProblemId();
     if (id != null) {
       IProblem prob = contest.getProblemById(id);
       if (prob != null)
         probStr = id + " (" + prob.getLabel() + ")";
     }
     %>
     <tr><td><a href="<%= apiRoot %>/submissions/<%= subs[i].getId() %>"><%= subs[i].getId() %></a></td>
     <td align=center><%= ContestUtil.formatTime(subs[i].getContestTime()) %></td><td><%= probStr %></td><td><%= langStr %></td><td><%= teamStr %></td><td><%= orgStr %></td></tr>
  <% } %>
  <tr><td colspan=6><%= numJudging %> pending judgements</td></tr>
</table>

<p/>

<b>All Submissions</b>

<table>
<tr><th>Id</th><th align=center>Time</th><th>Problem</th><th>Language</th><th>Team</th><th>Organization</th><th>Judgements</th></tr>

<% for (int i = 0; i < subs.length; i++) {
     String id = subs[i].getTeamId();
     String teamStr = "";
     String orgStr = "";
     if (id != null) {
       ITeam team = contest.getTeamById(id);
       if (team != null) {
         teamStr = id + ": " + team.getName();
         IOrganization org = contest.getOrganizationById(team.getOrganizationId());
         if (org != null)
            orgStr = org.getName();
       } else
         teamStr = "<font color=\"red\">" + id + "</font>";
     }
     
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
              if (contest.isFirstToSolve(subs[i]))
                judgeColor = "bgcolor=#33AA33";
              else if (jt.isSolved())
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
     </td><td align=center><%= ContestUtil.formatTime(subs[i].getContestTime()) %></td><td><%= probStr %></td><td><%= langStr %></td><td><%= teamStr %></td><td><%= orgStr %></td><td <%= judgeColor %>><%= judgeStr %></td></tr>
  <% } %>
</table>
</div>

</body>
</html>