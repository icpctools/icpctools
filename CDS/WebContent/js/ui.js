function sortByColumn(table) {
  $(table).find("th").each(function(index) {
    // don't sort empty column headers (likely logos)
    if ($(this).text() == "")
      return;

    // add cursor and hidden sort icon
    $(this).css('cursor', 'pointer');
    $(this).append('<i class="fa fa-fw fa-sort-up">');
    $(this).children().css('visibility', 'hidden');
    $(this).css('white-space', 'nowrap');
    this.asc = true;
    
    $(this).click(function() {
      var table = $(this).parents('table').eq(0);
      var rows = table.find('tr:gt(0)').toArray().sort(comparer($(this).index()));
      this.asc = !this.asc;
      if (!this.asc)
    	rows = rows.reverse();
     
      for (var i = 0; i < rows.length; i++)
    	 table.append(rows[i]);
 
      // rehide all other columns, then make sure the class is correct and visible for current sort
      $(table).find('th i').css('visibility', 'hidden');
      if (!this.asc) {
        $(this).children().removeClass("fa-sort-down");
        $(this).children().addClass("fa-sort-up");
      } else {
        $(this).children().removeClass("fa-sort-up");
        $(this).children().addClass("fa-sort-down");
      }
      $(this).children().css('visibility', 'visible');
    });
  });

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

function toHtml(templateName, obj) {
  var template = $('#' + templateName).html();
  return Mustache.render(template, obj);
}

function refreshTable(name) {
	console.log("Refreshing: " + name);
	
	// add spinner to the table (will be removed by table update) and call the refresh function
	var table = $("#" + name + "-table");
	table.parent().parent().append('<div id="' + 'temp" class="overlay dark"><i class="fas fa-2x fa-sync-alt fa-spin"></i></div>');
	
	var refreshFunc = new Function(name + 'Refresh()');
	refreshFunc();
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

	var x = $("#" + name + "-refresh");
    if (x != null)
    	x.attr("onclick", 'refreshTable("' + name + '")');

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
	
	$("#temp").remove();
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

function formatContestTime(time) {
	if (time == null)
		return "";

	if (time >= 0 && time < 1000)
		return "0";

	var sb = [];
	if (time < 0) {
		sb.push("-");
		time = -time;
	}
	time2 = Math.floor(time / 1000);

	days = Math.floor(time2 / 86400.0);
	if (days > 0)
		sb.push(days + "d ");

	sb.push(Math.floor(time2 / 3600.0) % 24);

	sb.push(':');
	mins = Math.floor(time2 / 60.0) % 60;
	if (mins < 10)
		sb.push('0');
	sb.push(mins);

	sb.push(':');
	secs = time2 % 60;
	if (secs < 10)
		sb.push('0');
	sb.push(secs);
	return sb.join("");
}

function formatTimestamp(time2) {
	if (time2 == null)
		return "";
	var d = luxon.DateTime.fromISO(time2);
	var now = luxon.DateTime.now();
	if (d.hasSame(now, 'year') && d.hasSame(now, 'month') && d.hasSame(now, 'day'))
		return d.toLocaleString(luxon.DateTime.TIME_WITH_SECONDS);
	return d.toLocaleString(luxon.DateTime.DATETIME_FULL);
}

function getDisplayName(team) {
	if (team.display_name != null)
		return team.display_name;

	return team.name;
}

function getOrganizationName(org) {
	if (org.formal_name != null)
		return org.formal_name;

	return org.name;
}

function getDisplayStr(teamId) {
	if (teamId == null)
		return '';

	team = findById(contest.getTeams(), teamId);
    if (team != null)
		return teamId + ': ' + getDisplayName(team);
	
	return teamId + ': (not found)';
}

function updateContestClock(contest, id) {
    setInterval(() => { $("#" + id).html(contest.getContestTime()) }, 300);
}