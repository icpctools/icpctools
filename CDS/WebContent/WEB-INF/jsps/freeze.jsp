<%@page import="java.util.List"%>
<%@page import="java.util.Arrays"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IJudgement"%>
<%@page import="org.icpc.tools.contest.model.IJudgementType"%>
<%@page import="org.icpc.tools.contest.model.ISubmission"%>
<%@page import="org.icpc.tools.contest.model.IState"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest2 = cc.getContestByRole(true, false);
   IContest contest = cc.getContestByRole(false, false);
   IState state = contest.getState();
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Contest Freeze</title>
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
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>
</p>

<h3>Contest Freeze</h3>

<table>
  <tr><td><b>Frozen:</b></td><td><%= ContestUtil.formatStartTime(state.getFrozen()) %></td></tr>
  <tr><td><b>Ended:</b></td><td><%= ContestUtil.formatStartTime(state.getEnded()) %></td></tr>
  <tr><td><b>Finalized:</b></td><td><%= ContestUtil.formatStartTime(state.getFinalized()) %></td></tr>
  <tr><td><b>Thawed:</b></td><td><%= ContestUtil.formatStartTime(state.getThawed()) %></td></tr>
</table>

<p/>

<% IJudgement[] juds = contest.getJudgements();
   int num = juds.length;
   ISubmission s = null;
   String col = "CCCCFF";
   if (num > 0) {
      s = contest.getSubmissionById(juds[num - 1].getSubmissionId());
   }
   
   ISubmission t = null;
   IJudgementType jt = null;
   int jaf = 0;
   for (ISubmission sub : contest.getSubmissions()) {
      if (!contest.isBeforeFreeze(sub)) {
         if (contest.isJudged(sub))
            jaf++;
         if (t == null) {
            t = sub;
            jt = contest.getJudgementType(sub);
         }
      }
   }
   
   IJudgement[] juds2 = contest2.getJudgements();
   int num2 = juds2.length;
   ISubmission s2 = null;
   if (num2 > 0)
      s2 = contest2.getSubmissionById(juds2[num2 - 1].getSubmissionId());
   
   ISubmission t2 = null;
   IJudgementType jt2 = null;
   int jaf2 = 0;
   for (ISubmission sub : contest2.getSubmissions()) {
      if (!contest2.isBeforeFreeze(sub)) {
         if (contest2.isJudged(sub))
            jaf2++;
         if (t2 == null) {
            t2 = sub;
            jt2 = contest2.getJudgementType(sub);
         }
      }
   }
   
   if (jt != null || jaf > 0)
      col = "FFAAAA";
   else if (jt == null && jt2 != null)
      col = "AAFFAA";
 %>

<table>
<tr><th></th><th>Public Contest</th><th>Full Contest</th></tr>
<tr><td># submissions:</td>
   <td><%= contest.getNumSubmissions() %></td>
   <td><%= contest2.getNumSubmissions() %></td></tr>
<tr><td># judgements:</td>
   <td><%= num %></td>
   <td><%= num2 %></td></tr>
<tr bgcolor=<%= col %>><td># of judgements after freeze:</td>
  <td><%= jaf %></td>
  <td><%= jaf2 %></td></tr>
<tr><td>First submission after the freeze:</td>
   <td><%= t == null ? "n/a" : t.getId() %></td>
   <td><%= t2 == null ? "n/a" : t2.getId() %></td></tr>
<tr bgcolor=<%= col %>><td>Judgement of first submission after the freeze:</td>
  <td><%= jt == null ? "n/a" : jt.getName() %></td>
  <td><%= jt2 == null ? "n/a" : jt2.getName() %></td></tr>
<tr><td>Submission time of most recent judgement:</td>
   <td><%= s == null ? "no judgements" : ContestUtil.formatTime(s.getContestTime()) %></td>
   <td><%= s2 == null ? "no judgements" :ContestUtil.formatTime(s2.getContestTime()) %></td></tr>

</div>

</body>
</html>