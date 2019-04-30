<%@page import="java.util.Date"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Calendar"%>
<%@page import="org.icpc.tools.cds.CDSConfig"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<%@page import="org.icpc.tools.contest.model.internal.Contest"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   IContest contest = cc.getContest();
   String webRoot = "/contests/" + cc.getId(); %>
<html>
<head>
  <title>Countdown Control</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>

<script>
function sendCommand(id, command) {
   document.getElementById(id).disabled = true;

   var xmlhttp = new XMLHttpRequest();
   xmlhttp.onreadystatechange = function() {
     document.getElementById("status").innerHTML = "";
     if (xmlhttp.readyState == 4) {
        var resp = xmlhttp.responseText;
        if (xmlhttp.status == 200) {
           if (resp == null || resp.trim().length == 0)
              targetTime = null;
           else
              targetTime = parseInt(resp) / 1000.0;
        } else
           document.getElementById("status").innerHTML = resp;
        document.getElementById(id).disabled = false;
     }
   }
   xmlhttp.timeout = 10000;
   xmlhttp.ontimeout = function () {
      document.getElementById("status").innerHTML = "Request timed out";
      document.getElementById(id).disabled = false;
   }
   xmlhttp.open("PUT", "<%= webRoot %>/finalize/" + command, true);
   xmlhttp.send();
}
</script>

<body>

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Finalize Control (<%= ConfiguredContest.getUser(request) %>)</div>
</div>

<div id="main">
<a href="<%= webRoot %>">Overview</a> -
<a href="<%= webRoot %>/details">Details</a> -
<a href="<%= webRoot %>/orgs">Organizations</a> -
<a href="<%= webRoot %>/teams">Teams</a> -
<a href="<%= webRoot %>/submissions">Submissions</a> -
<a href="<%= webRoot %>/clarifications">Clarifications</a> -
<a href="<%= webRoot %>/scoreboard">Scoreboard</a> -
<a href="<%= webRoot %>/countdown">Countdown</a> -
Finalize -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
<a href="<%= webRoot %>/reports">Reports</a>

<p/>

Set value of b:

<select id="bSelect">
  <option value="0">0</option>
  <option value="1">1</option>
  <option value="2">2</option>
  <option value="3">3</option>
  <option value="4">4</option>
  <option value="5">5</option>
  <option value="6">6</option>
</select>

<button id="set" onclick="var e = document.getElementById('bSelect'); sendCommand('set', 'b:' + e.options[e.selectedIndex].value)">Apply!</button>

<p/>

</div>
<p/>
<span id="status"></span>

</body>
</html>