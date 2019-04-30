<%@page import="org.icpc.tools.contest.model.ContestUtil"%>
<%@page import="org.icpc.tools.contest.model.IOrganization"%>
<%@page import="org.icpc.tools.contest.model.ITeam"%>
<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<%@page import="org.icpc.tools.contest.model.IContest"%>
<%@page import="java.util.Arrays"%>
<%
 ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
 IContest contest = cc.getContest();
 int numTeams = contest.getNumTeams();
 String webRoot = "/contests/" + cc.getId();
%>
<html>

<head>
  <title>Video Status</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<script>
function updateStatus(base, st) {
	for (var i = 0; i < st.streams.length; i++) {
	  var str = st.streams[i];
	  var id = str.id;
	  var d = document.getElementById(base + "-" + id);
	  if (d == null)
	    continue;

	  var link = "<a href='/video/"+base+"/"+i+"'>link</a>";
 	  d.innerHTML = str.current + " / " + str.max_current + " / " + str.total_listeners + "  " + link;
 	  document.getElementById(base + "-" + id +"m").innerHTML = str.mode;

 	  var stat = str.status;
      var col = "BBBBFF";
      if (stat == "ACTIVE")
          col = "99FF99";
      else if (stat == "FAILED")
          col = "FF9999";
      else if (stat == "UNKNOWN")
          col = "DDDDDD";
      d.style.backgroundColor = col;
    }
    document.getElementById(base+"Streams").innerHTML = st.streams.length;
    
    document.getElementById(base+"Current").innerHTML = st.current;
    //document.getElementById(base+"Max").innerHTML = st.max;
    document.getElementById(base+"Total").innerHTML = st.total_listeners;
    document.getElementById(base+"TotalTime").innerHTML = st.total_time;
    //document.getElementById(base+"Mode").innerHTML = st.mode;
}

function updateStatus2(st) {
    document.getElementById("totalStreams").innerHTML = st.streams.length;
    document.getElementById("totalCurrent").innerHTML = st.current;
    document.getElementById("totalMax").innerHTML = st.max_current;
    document.getElementById("total").innerHTML = st.total_listeners;
    document.getElementById("totalTime").innerHTML = st.total_time;
}

function verifyVideo() {
   var xmlhttp = new XMLHttpRequest(); 
   xmlhttp.onreadystatechange = function() {
     if (xmlhttp.readyState == 4) {
        if (xmlhttp.status == 200) {
           updateStatus("desktop", JSON.parse(xmlhttp.responseText));
        }
     }
   };
   
   xmlhttp.open("GET", "/video/desktop");
   xmlhttp.send();
   
   var xmlhttp2 = new XMLHttpRequest(); 
   xmlhttp2.onreadystatechange = function() {
     if (xmlhttp2.readyState == 4) {
        if (xmlhttp2.status == 200) {
           updateStatus("webcam", JSON.parse(xmlhttp2.responseText));
        }
     }
   };
   
   xmlhttp2.open("GET", "/video/webcam");
   xmlhttp2.send();
   
   var xmlhttp3 = new XMLHttpRequest(); 
   xmlhttp3.onreadystatechange = function() {
     if (xmlhttp3.readyState == 4) {
        if (xmlhttp3.status == 200) {
           updateStatus2(JSON.parse(xmlhttp3.responseText));
        }
     }
   };
   
   xmlhttp3.open("GET", "/video");
   xmlhttp3.send();
}

function request(url) {
	   var xmlhttp = new XMLHttpRequest();
	   xmlhttp.onreadystatechange = function() {
	     if (xmlhttp.readyState == 4) {
	        if (xmlhttp.status == 200) {
	        	//window.location.reload(false);
	        	verifyVideo();
	        } else {
	           alert(xmlhttp.status + ": " + xmlhttp.responseText)
	        }
	     }
	   };
	   
	   xmlhttp.open("GET", url);
	   xmlhttp.send();
	}
</script>

<body onload="verifyVideo()">

<div id="navigation-header">
  <div id="navigation-cds"><%= contest.getFormalName() %> - Video Status</div>
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
<a href="<%= webRoot %>/awards">Awards</a> -
Video -
<a href="<%= webRoot %>/reports">Reports</a>
</p>

<h2>Teams</h2>

<table>
<tr><th>Id</th><th>Name</th><th>Organization</th><th colspan=3>Desktop</th><th colspan=3>Webcam</th></tr>
<tr><td colspan=3></td><td colspan=3>Current / Max Current / Total</td><td colspan=3>Current / Max Current / Total</td></tr>

<% ITeam[] teams = contest.getTeams();
   teams = Arrays.copyOf(teams, teams.length);
   ContestUtil.sort(teams);
   for (int i = 1; i <= numTeams; i++) {
	  ITeam t = teams[i-1];
	  if (t != null) {
		String tId = t.getId();
	    IOrganization org = contest.getOrganizationById(t.getOrganizationId());
	    String orgName = "";
	    if (org != null)
	       orgName = org.getFormalName(); %>
        <tr><td><%= tId %></td><td><%= t.getName() %></td><td><%= orgName %></td>
          <td id="desktop-<%= tId %>" align="center">-</td><td id="desktop-<%= tId %>m" align="center"></td><td><a href="javascript:request('/video/desktop/<%= tId %>?reset=true');">Reset</a></td>
          <td id="webcam-<%= tId %>" align="center">-</td><td id="webcam-<%= tId %>m" align="center"></td><td><a href="javascript:request('/video/webcam/<%= tId %>?reset=true');">Reset</a></td></tr>
      <% } else { %>
        <tr><td>?</td><td>?</td><td>?</td>
          <td id="desktop<%= i %>" align="center">-</td><td></td>
          <td id="webcam<%= i %>" align="center">-</td><td></td></tr>
      <% }
  } %>
  <tr><td></td><td></td><td align=right>Total streams:</td>
      <td id="desktopStreams" align="center">-</td><td></td>
      <td id="webcamStreams" align="center">-</td><td></td></tr>
  <tr><td></td><td></td><td align=right>Current clients:</td>
      <td id="desktopCurrent" align="center">-</td><td><a href="javascript:request('/video/desktop?resetAll=true');">Reset all</a></td>
      <td id="webcamCurrent" align="center">-</td><td><a href="javascript:request('/video/webcam?resetAll=true');">Reset all</a></td></tr>
<!--  <tr><td></td><td></td><td align=right>Max concurrent:</td>
      <td id="desktopMax" align="center">-</td><td></td>
      <td id="webcamMax" align="center">-</td><td></td></tr> -->
  <tr><td></td><td></td><td align=right>Total clients:</td>
      <td id="desktopTotal" align="center">-</td><td></td>
      <td id="webcamTotal" align="center">-</td><td></td></tr>
  <tr><td></td><td></td><td align=right>Total time:</td>
      <td id="desktopTotalTime" align="center" colspan="2">-</td>
      <td id="webcamTotalTime" align="center" colspan="2">-</td></tr>
  <tr valign="top"><td></td><td></td><td align=right>Connection mode:</td>
      <td id="desktopMode" align="center">-</td><td><a href="javascript:request('/video/desktop?mode=eager');">Eager</a><br/><a href="javascript:request('/video/desktop?mode=lazy');">Lazy</a><br><a href="javascript:request('/video/desktop?mode=lazy_close');">Lazy close</a></td>
      <td id="webcamMode" align="center">-</td><td><a href="javascript:request('/video/webcam?mode=eager');">Eager</a><br/><a href="javascript:request('/video/webcam?mode=lazy');">Lazy</a><br><a href="javascript:request('/video/webcam?mode=lazy_close');">Lazy close</a></td></tr>
</table>

<table> <tr><td></td><td></td><td align=right>Total streams:</td>
      <td id="totalStreams" align="center">-</td></tr>
  <tr><td></td><td></td><td align=right>Current clients:</td>
      <td id="totalCurrent" align="center">-</td></tr>
  <tr><td></td><td></td><td align=right>Max concurrent:</td>
      <td id="totalMax" align="center">-</td></tr>
  <tr><td></td><td></td><td align=right>Total:</td>
      <td id="total" align="center">-</td></tr>
  <tr><td></td><td></td><td align=right>Total time:</td>
      <td id="totalTime" align="center">-</td></tr>
</table>
<p/>

<p><a href="<%= webRoot %>/video/map/desktop">Desktop map</a></p>
<p><a href="<%= webRoot %>/video/map/webcam">Webcam map</a></p>

<h2>Status Key</h2>
<table>
<tr><td bgcolor="DDDDDD">Unknown</td></tr>
<tr><td bgcolor="99FF99">Active</td></tr>
<tr><td bgcolor="FF9999">Failed</td></tr>
</table>
</div>

</body>
</html>