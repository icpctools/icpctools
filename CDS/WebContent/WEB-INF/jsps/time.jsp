<%@page import="java.util.Date"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Calendar"%>
<%@page import="org.icpc.tools.cds.CDSConfig"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<html>
<head>
  <title>Contest Time</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>

<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContest(); %>

<body onload="connectTime('<%= cc.getId() %>')">

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getActualFormalName() %> - Time</div>
</div>

<div id="main">
<b><font size="+20"><span id="countdown">0?:0?:0?</span></font></b>
<p/>

<span id="contestStatus"></span>
<p/>

<script src="/time.js"></script>
</div>

</body>
</html>