function sortByColumn(table) {
  $('th').click(function() {
     var table = $(this).parents('table').eq(0);
     var rows = table.find('tr:gt(0)').toArray().sort(comparer($(this).index()));
     this.asc = !this.asc;
     if (!this.asc)
    	rows = rows.reverse();
     
     for (var i = 0; i < rows.length; i++)
    	 table.append(rows[i]);
  })

  function comparer(index) {
     return function(a, b) {
        var valA = getCellValue(a, index);
        var valB = getCellValue(b, index);
        return $.isNumeric(valA) && $.isNumeric(valB) ? valB - valA : -valA.toString().localeCompare(valB);
     }
  }

  function getCellValue(row, index) {
     return $(row).children('td').eq(index).text();
  }
}

function fillContestObjectHeader(name, objs) {
	if (name == null)
		return;

    var x = $("#" + name + "-count");
    if (x != null && objs != null) {
    	x.attr("title", objs.length);
    	x.html(objs.length);
    }
    var x = $("#" + name + "-button");
    if (x != null) {
    	x.attr("onclick", 'location.href="' + contest.getURL(name) + '"');
    }
}

function fillContestObjectTable(name, objs, tdGen) {
	if (name == null || objs == null || tdGen == null)
		return;

	sortByColumn($('#' + name + "-table"));

    $("#" + name + "-table tbody").find("tr").remove();
    for (var i = 0; i < objs.length; i++) {
        var row = $('<tr></tr>');
        row.append(tdGen(objs[i]));
        $("#" + name + "-table tbody").append(row);
    }

    if (objs.length === 0) {
    	var numCols = $("#" + name + "-table thead").find("tr:first th").length;
        col = $('<td colspan="' + numCols + '">None</td>');
        row = $('<tr></tr>');
        row.append(col);
        $("#" + name + "-table tbody").append(row);
    }
    
    fillContestObjectHeader(name, objs);
}

var tagsToReplace = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '\n': '<br/>',
    '\r': ''
};

function replaceTag(tag) {
    return tagsToReplace[tag] || tag;
}
	
function sanitizeHTML(str) {
	if (str == null)
		return "";
	return str.replace(/[&<>\n\r]/g, replaceTag);
}

function formatTime(time2) {	
	if (time2 >= 0 && time2 < 1000)
		return "0s";

	var sb = [];
	if (time2 < 0) {
		sb.push("-");
		time2 = -time2;
	}
	time = Math.floor(time2 / 1000);

	days = Math.floor(time / 86400.0);
	if (days > 0)
		sb.push(days + "d");

	hours = Math.floor(time / 3600.0) % 24;
	if (hours > 0) {
		sb.push(hours + "h");
		if (days > 0)
			return sb.join("");
	}

	mins = Math.floor(time / 60.0) % 60;
	if (mins > 0)
		sb.push(mins + "m");

	secs = time % 60;
	if (secs > 0)
		sb.push(secs + "s");
	return sb.join("");
}

function formatTimestamp(time2) {
	if (time2 == null)
		return "";
	var d = new Date(time2);
	return d.toDateString() + " " + d.toLocaleTimeString();
}