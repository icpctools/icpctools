<%@page import="java.util.List"%>
<%@page import="java.util.Arrays"%>
<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.contest.model.IOrganization"%>
<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<%@page import="org.icpc.tools.contest.model.util.EventFeedUtil.CompareFull"%>
<%@page import="org.icpc.tools.contest.model.util.EventFeedUtil.DiffType"%>
<html>

<head>
  <title>Contest Comparison</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds">Contest Comparison (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">

<table>
<tr><td>Comparing</td><td><%= request.getAttribute("a") %></td></tr>
<tr><td align=right>to</td><td><%= request.getAttribute("b") %></td></tr>
</table>
<p/>

<table>
<tr><td valign=top>Contest</td><td><%= (String) request.getAttribute("info") %></td></tr>
<tr><td valign=top>Languages</td><td><%= (String) request.getAttribute("languages") %></td></tr>
<tr><td valign=top>Judgement Types</td><td><%= (String) request.getAttribute("judgement-types") %></td></tr>
<tr><td valign=top>Problems</td><td><%= (String) request.getAttribute("problems") %></td></tr>
<tr><td valign=top>Groups</td><td><%= (String) request.getAttribute("groups") %></td></tr>
<tr><td valign=top>Organizations</td><td><%= (String) request.getAttribute("organizations") %></td></tr>
<tr><td valign=top>Teams</td><td><%= (String) request.getAttribute("teams") %></td></tr>
<tr><td valign=top>Submissions</td><td><%= (String) request.getAttribute("submissions") %></td></tr>
<tr><td valign=top>Judgements</td><td><%= (String) request.getAttribute("judgements") %></td></tr>
<tr><td valign=top>Awards</td><td><%= (String) request.getAttribute("awards") %></td></tr>
</table>

<h3>Comparison detail</h3>

<table>
<% org.icpc.tools.contest.model.util.EventFeedUtil.CompareFull fc = (org.icpc.tools.contest.model.util.EventFeedUtil.CompareFull) request.getAttribute("awards2");
int numContests = fc.vals.length;
// header
%><tr><th>Id</th><%
for (int j = 0; j < numContests; j++) {
	%><th>Contest <%= j %></th><%
}%></tr><%

// body
for (int i = 0; i < fc.ids.size(); i++) {
	  %><tr><td><%= fc.ids.get(i) %></td><%
    for (int j = 0; j < numContests; j++) {
    	%><td><%
    	String[] vars = fc.vals[j][i];
    	org.icpc.tools.contest.model.util.EventFeedUtil.DiffType[] dt = fc.diff[i];
    	if (vars != null) {
    	  for (int k = 0; k < vars.length; k++) {
    		 if (vars[k] != null) {
    	    	if (dt[k] == org.icpc.tools.contest.model.util.EventFeedUtil.DiffType.DIFF_BAD) {
    	    		%><font color="red"><%= vars[k] %></font><br/><%
    	    	} else {
    	    		%><%= vars[k] %><br/><%
    	    	}
    	    }
      	  }
    	}
    	%></td><%
    }
    %></tr><%
  }
%>
</table>

</div>

</body>
</html>