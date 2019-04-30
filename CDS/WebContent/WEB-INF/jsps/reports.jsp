<%@page import="org.icpc.tools.cds.ConfiguredContest"%>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
   String webRoot = "/contests/" + cc.getId();
   String apiRoot = "/api/contests/" + cc.getId(); %>
<html>

<head>
  <title>Contest Reports</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>

<div id="navigation-header">
  <div id="navigation-cds">Reports</div>
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
<a href="<%= webRoot %>/finalize">Finalize</a> -
<a href="<%= webRoot %>/awards">Awards</a> -
<a href="<%= webRoot %>/video/status">Video</a> -
Reports

<h3>Languages</h3>

<table id="langs-table">
<tr><td>Loading...</td></tr>
</table>

<h3>Runs</h3>

<table id="runs-table">
<tr><td>Loading...</td></tr>
</table>

<h3>Problems</h3>

<table id="problems-table">
<tr><td>Loading...</td></tr>
</table>

</div>

<script src="/js/jquery-3.3.1.min.js"></script>
<script src="/js/model.js"></script>
<script src="/js/ui.js"></script>
<script type="text/javascript">
$(document).ready(function() {
	console.log("Loading reports");
	$.ajax({
	  url: '<%= apiRoot %>/report/langs',
	  success: function(result) {
	    fillTable('#langs-table', result);
	  },
	  failure: function(result) {
	     alert("Error loading page: " + result);
	  }
	});
	$.ajax({
	  url: '<%= apiRoot %>/report/runs',
	  success: function(result) {
	    fillTable('#runs-table', result);
	  },
	  failure: function(result) {
	     alert("Error loading page: " + result);
	  }
	});
	$.ajax({
	  url: '<%= apiRoot %>/report/problems',
	  success: function(result) {
	    fillTable('#problems-table', result);
	  },
	  failure: function(result) {
	     alert("Error loading page: " + result);
	  }
	});
});

function fillTable(table, result) {
  $(table).find("tr").remove();
  var header = result[0];
  var col = '';
  for (k in header)
    if (k !== 'id')
      col += '<th>'+header[k] +'</th>';
  var row = $('<tr></tr>');
  row.append($(col));
  $(table).append(row);
 
  for (var i = 1; i < result.length; i++) {
    var rep = result[i];
    var col = '';
    for (k in rep)
      if (k !== 'id')
        col += '<td>'+rep[k] +'</td>';
    row = $('<tr></tr>');
    row.append($(col));
    $(table).append(row);
  }
  sortByColumn($(table));
}

</script>

</body>
</html>