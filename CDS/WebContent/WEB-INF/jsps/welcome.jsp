<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IState"%>
<%@page import="org.icpc.tools.contest.model.internal.State"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.cds.CDSConfig"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest.Mode"%>
<%@page import="org.icpc.tools.cds.util.Role"%>
<html>
<head>
  <title>Contest Data Server</title>
  <link rel="stylesheet" href="/cds.css">
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds">Contest Data Server</div>
</div>

<div id="main">
<% ConfiguredContest[] ccs = CDSConfig.getContests();

for (ConfiguredContest cc : ccs) { %>
<% if (!cc.isHidden() || Role.isAdmin(request) || Role.isBlue(request)) {
IContest contest = cc.getContest();
 try {
   IState state = contest.getState();
   if (state == null)
	   state = new State();
   if (state.getEnded() != null) { %>
<div id="contest-ended">
   <% } else if (state.getFrozen() != null) { %>
<div id="contest-frozen">
   <% } else if (state.getStarted() != null) { %>
<div id="contest-started">
   <% } else { %>
<div id="contest">
<% }
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<div id="contest-title"><%= contest.getFormalName() %> (<%= cc.getId() %>)</div>

<table class="contestTable">
 <tr class="contest-header">
   <td class="contest-header">
   <% if (state.getStarted() == null) { 
        if (contest.getStartStatus() == null) { %>Not scheduled
        <% } else if (contest.getStartStatus() < 0) {%>Countdown paused
        <% } else { %>Scheduled<% } %>
   <% } else if (state.getEnded() != null) { %>Finished
   <% } else { %>
   <div class="progressbar" style="height:100%;width:100%"><div class="progressval" style="height:100%;width:<%= Math.min(99,(long)((System.currentTimeMillis() - state.getStarted()) * contest.getTimeMultiplier()) * 100 / contest.getDuration()) %>%">
   <%= ContestUtil.formatTime((long)((System.currentTimeMillis() - state.getStarted()) * contest.getTimeMultiplier())) %>
   </div></div>
   <% } %></td>
   <td class="contest-header" colspan="4"><%= ContestUtil.formatDuration(contest.getDuration()) %> starting at <%= ContestUtil.formatStartTime(contest) %></td>
   <td class="contest-header" align=right>
      <% if (cc.getMode() == Mode.ARCHIVE) { %>Archive
      <% } else if (cc.getMode() == Mode.LIVE) { %>Live
      <% } else { %>Playback (<%= cc.getTest().getMultiplier() %>x)<% } %></td></tr>
 <tr class="trcontest">
   <td class="big"><b>Services:</b></td>
   <td class="big"><a href="<%= apiRoot %>/">REST API root</a></td>
   <td class="big"><a href="<%= apiRoot %>/event-feed">Event feed</a></td>
   <td class="big"><a href="<%= apiRoot %>/scoreboard">JSON scoreboard</a></td>
   <td class="big"><a href="<%= webRoot %>/time">Time</a></td>
 </tr>

 <tr class="trcontest">
   <td class="big"><b>Admin:</b></td>
   <td class="big"><a href="<%= webRoot %>">Overview</a></td>
   <td class="big"><a href="<%= webRoot %>/details">Details</a></td>
   <td class="big"><a href="<%= webRoot %>/orgs">Organizations</a></td>
   <td class="big"><a href="<%= webRoot %>/teams">Teams</a></td>
   <td class="big"><a href="<%= webRoot %>/submissions">Submissions</a></td>
 </tr>
 <tr class="trcontest">
   <td></td>
   <td class="big"><a href="<%= webRoot %>/clarifications">Clarifications</a></td>
   <td class="big"><a href="<%= webRoot %>/scoreboard">Scoreboard</a></td>
   <td class="big"><a href="<%= webRoot %>/countdown">Countdown</a></td>
   <td class="big"><a href="<%= webRoot %>/finalize">Finalize</a></td>
   <td class="big"><a href="<%= webRoot %>/video/status">Video</a></td>
 </tr>
</table>
<% } catch (Exception e) { %>
<div id="contest-title">Error loading contest <%= cc.getId() %></div>
<% } %>
</div>
<% }
 } %>


<h2>Administration</h2>

<table>
 <tr>
   <td>Video</td>
   <td class="big"><a href="/video/control/1">Channel 1</a></td>
   <td class="big"><a href="/video/control/2">Channel 2</a></td>
   <td class="big"><a href="/video/control/3">Channel 3</a></td>
 </tr>
</table>

<p class="verticalgap"/>

<h3><a href="/presentation/admin/web">Presentation Admin</a></h3>

<h3><a href="/search">Search</a></h3>

<p class="verticalgap"/>


<h3>Links</h3>

<p>For up to date information on the services provided, please see the
<a href="https://clics.ecs.baylor.edu/index.php/CDS">CDS documentation</a>.</p>

<table>
 <tr>
   <td>ICPC Tools</td>
   <td><a href="https://icpc.baylor.edu/icpctools">https://icpc.baylor.edu/icpctools</a></td>
 </tr>
 <tr>
   <td>Bugzilla</td>
   <td><a href="http://pc2.ecs.csus.edu/projects/bugzilla">http://pc2.ecs.csus.edu/projects/bugzilla</a></td>
 </tr>
 <tr>
   <td>Latest internal builds</td>
   <td><a href="http://pc2.ecs.csus.edu/pc2projects/build">http://pc2.ecs.csus.edu/pc2projects/build</a></td>
 </tr>
</table>
</div>

</body>
</html>