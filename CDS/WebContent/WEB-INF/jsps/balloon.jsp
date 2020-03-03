<%@page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@page import="org.icpc.tools.contest.model.IContest" %>
<%@page import="org.icpc.tools.contest.model.IProblem" %>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
    IContest contest = cc.getContestByRole(request);
    IProblem[] problems = contest.getProblems(); %>
<html><head>
<title>Balloons</title>
<style type="text/css">
  html { height: 100%; margin: 0; padding: 0; }
  body { height: 100%; margin: 0; padding: 0; }
  p { font-weight: bold; font-size: 300pt; text-align: center; page-break-after: always; }
  table { margin-left: auto; margin-right: auto; width: 60%; }
  img { width: 100px; }
  td { font-weight: bold; font-size: 45pt; }
  td.sm { font-weight: bold; font-size: 15pt; }
  @media print {
     table { width: 80%; }
     table tr { page-break-inside: avoid; }
  }
</style>
</head><body>

<% for (IProblem p : problems) { %>
  <p><%= p.getLabel() %></p>
<% } %>

<table>
<% for (IProblem p : problems) { %>
  <tr><td><img src='balloon/<%= p.getLabel() %>.png'/></td><td>Problem <%= p.getLabel() %></td><td class="sm">(<%= p.getColor() %>)</td></tr>
<% } %>
</table>

</body></html>