<%@page import="java.util.List"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContestByRole(request); %>
<html>

<head>
  <title>Contest Validation</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body onload="checkInBackground()">

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Validation (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<table>
<% 
   List<String> validationList = contest.validate();
   if (validationList == null) { %>
   <tr><td>No errors</td></tr>
<% } else {
      for (String s : validationList) { %>
   <tr><td><%= s %></td></tr>
   <% } %>
   <tr><td><%= validationList.size() %> errors.</td></tr>
<% } %>
</table>
</div>

</body>
</html>