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

function registerContestObjectTable(name) {
	if (name == null)
		return;

	// add temporary spinner row, api link, and column sorter
	var table = $("#" + name + "-table tbody");
	if (table != null) {
		var numCols = $("#" + name + "-table thead").find("tr:first th").length;
	    col = $('<td colspan=' + numCols + ' align=middle><div class="spinner-border"></div></td>');
	    row = $('<tr></tr>');
	    row.append(col);
	    $(table).append(row);
	}
    
    var x = $("#" + name + "-api");
    if (x != null)
    	x.attr("onclick", 'location.href="' + contest.getURL(name) + '"');
    
    sortByColumn($('#' + name + "-table"));
}

function updateContestObjectHeader(name, objs) {
	if (name == null)
		return;

    var x = $("#" + name + "-count");
    if (x != null && objs != null) {
    	x.attr("title", objs.length);
    	x.html(objs.length);
    }
}

function toHtml(templateName, obj) {
  var template = $('#' + templateName).html();
  return Mustache.render(template, obj);
}

function fillContestObjectTable(name, objs) {
	if (name == null || objs == null)
		return;

	// remove all rows with no id (spinner and/or 'None' row)
	var table = $("#" + name + "-table tbody");
	$(table).find("tr").not(':has([id])').remove();

	// build list of existing row ids
	var list = [];
	var rows = $(table).find("tr");
	for (var i = 0; i < rows.length; i++)
	   list.push(rows[i].id);

	// load row template
	var template = $('#' + name + '-template').html();

	// walk through all the current objects and add or replace rows as necessary
    for (var i = 0; i < objs.length; i++) {
        id = objs[i].id;
        var rowObj = this[name.replace("-", "")+"Td"](objs[i]);
        rowObj.id = id;
        if (rowObj.api == null)
        	rowObj.api = contest.getURL(name, id);
        rowTDs = Mustache.render(template, rowObj);
        
    	var row = $(table).find("tr #" + id);
    	if (row == null || row.length == 0) {
    		var row = $('<tr id="' + objs[i].id + '"></tr>').append(rowTDs);
        	$(table).append(row);
        } else {
        	list.pop(id);
        	row.replaceWith(rowTDs);
        }
    }

    // remove any rows for objects that don't exist anymore
    for (var i = 0; i < list.length; i++)
	   $(table).find("tr #" + list[i]).remove();

	// add "None" if there are no elements
	if (objs.length === 0) {
    	var numCols = $("#" + name + "-table thead").find("tr:first th").length;
        col = $('<td colspan="' + numCols + '">None</td>');
        row = $('<tr></tr>');
        row.append(col);
        $("#" + name + "-table tbody").append(row);
    }

	updateContestObjectHeader(name, objs);
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