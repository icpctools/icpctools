<html>
<head>
  <title>Video Channel Control</title>
  <link rel="stylesheet" href="/cds.css"/>
  <link rel="icon" type="image/png" href="/favicon.png"/>
</head>
<body>

<% String channel2 = (String) request.getAttribute("channel"); %>

<script>
var last;
function sendCommand(id) {
   document.getElementById("team"+id).disabled = true;

   var xmlhttp = new XMLHttpRequest();

   xmlhttp.onreadystatechange = function() {
     document.getElementById("status").innerHTML = "Changing to "+id;
     if (xmlhttp.readyState == 4) {
        if (xmlhttp.status == 200) {
           document.getElementById("status").innerHTML = "Success";
           if (last != null) {
              document.getElementById(last).innerHTML = "-";
           }
           document.getElementById("current"+id).innerHTML = "Now";
           last = "current"+id;
        } else
           document.getElementById("status").innerHTML = xmlhttp.responseText;
        document.getElementById("team"+id).disabled = false;
     }
   };
   
   xmlhttp.open("PUT", "video/channel/<%= channel2 %>/" + id, true);
   xmlhttp.send();
}
</script>

<h1>Video Channel <%= channel2 %></h1>

<table id="stream-table">
<tr>
<th align=left>Stream</th>
<th align=left>Stream</th>
<th align=left>Stream</th>
<th align=left>Stream</th>
<th align=left>Stream</th>
<th align=left>Stream</th>
<th align=left>Stream</th>
<th align=left>Stream</th>
</tr>
</table>

<script src="/js/jquery-3.3.1.min.js"></script>
<script src="/js/model.js"></script>
<script src="/js/ui.js"></script>
<script type="text/javascript">

var curChannel = "${channel}";
var curStream = "${stream}";

function switchToStream(newStream) {
	$.ajax({
		  url: "/video/control/" + curChannel + "/" + newStream,
		  method: "PUT",
		  success: function(result) {
			  curStream = newStream;
			  selectRow(newStream);
		  }
		})
}

var lastSel;
function selectRow(id) {
	if (lastSel != null)
		$('#stream-table td[id=' + lastSel + ']').css('background-color','#FFF');
	$('#stream-table td[id=' + id + ']').css('background-color','#BBB');
	lastSel = id;
}

$(document).ready(function() {

var streams;

function fillTable() {
  if (streams == null)
    return;
  
  // sort
  streams.sort(function(a, b) {
	if (a.order != b.order)
		return a.order - b.order;
	return a.name.localeCompare(b.name);
  });

  var columns = 10;
  var rows = Math.ceil(streams.length / columns);
  for (var i = 0; i < rows; i++) {
	var col = '';
    for (var j = 0; j < columns; j++) {
      if (i + rows * j < streams.length) {
		var stream = streams[i + rows * j];
	    col += '<td id=' + stream.id + '><a href="javascript:switchToStream(' + stream.id + ')">' + stream.name + '</a></td>';
      }
	}
    var row = $('<tr></tr>');
    row.append($(col));
    $('#stream-table').append(row);
  }
  
  selectRow(curStream);
}

var loadStreams=$.ajax({
	  url: "/video",
	  success: function(result) {
	    streams = result;
	  }
	})

$.when(loadStreams).done(function() { fillTable() }).fail(function(result) {
    alert("Could not load page!");
  })
})
</script>

</body>
</html>