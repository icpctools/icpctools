<html>

<head>
  <title>Contest Details</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>

<body>
<div id="navigation-header">
  <div id="navigation-cds">Search Results</div>
</div>

<div id="main">

<h2>Search</h2>

<input type="text" id="searchText"></input> <button id="search" onclick="searchFor(document.getElementById('searchText').value)">Search</button>

<h3>Results</h3>

<table id="search-table">
<tr><th>Contest</th><th>Type</th><th>Id</th></tr>
</table>

</div>

<script src="/js/jquery-3.3.1.min.js"></script>
<script src="/js/model.js"></script>
<script src="/js/ui.js"></script>
<script src="/js/search.js"></script>
<script type="text/javascript">

function searchFor(text) {
  console.log("search for: " + text);
  $.when(search.search(text)).done(function() {
    fillTable();
  }).fail(function(result) {
    console.log(result);
    alert("Could not perform search (" + result.status + ":" + result.statusText + ")");
  })
}

function fillTable() {
  $("#search-table").find("tr:gt(0)").remove();
  var results = search.getResults();
  var result = results[0];
  result = result.results;
  for (var i = 0; i < result.length; i++) {
    var contestResult = result[i];
    var contestId = contestResult.contest_id;
    var results2 = contestResult.results;
    if (results2.length == 0) {
      var col = $('<td>' + contestId + '</td><td colspan=2>No hits</td>');
      var row = $('<tr></tr>');
      row.append(col);
      $('#search-table').append(row);
    }
    for (var j = 0; j < results2.length; j++) {
      var type = results2[j].type;
      var id = results2[j].id;
      var col = $('<td><a href="/contests/'+contestId+'">' + contestId + '</a></td><td>' + type + '</td>' +
        '<td><a href="/api/contests/'+contestId+'/'+type+'/' + id + '">' + id + '</a></td>');
      var row = $('<tr></tr>');
      row.append(col);
      $('#search-table').append(row);
    }
  }
};
</script>
</body>
</html>