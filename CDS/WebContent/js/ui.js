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
    var x = $("#" + name + "-count");
    if (x != null) {
    	x.attr("title", objs.length);
    	x.html(objs.length);
    }
}

var tagsToReplace = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;'
};

function replaceTag(tag) {
    return tagsToReplace[tag] || tag;
}
	
function sanitizeHTML(str) {
	if (str == null)
		return "";
	return str.replace(/[&<>]/g, replaceTag);
}