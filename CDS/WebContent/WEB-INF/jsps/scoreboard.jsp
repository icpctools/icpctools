<% request.setAttribute("title", "Scoreboard"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Scoreboard</h3>
                    <div class="card-tools"><a href="<%= apiRoot %>/scoreboard">API</a>
                    <button id="score-refresh" type="button" class="btn btn-tool"><i class="fas fa-sync-alt"></i></button>
                    </div>
                </div>
                <div class="card-body p-0">
                    <% if (CDSAuth.isStaff(request)) {%>
                    <p class="indent">
                        Compare to:
                        <% ConfiguredContest[] ccs = CDSConfig.getContests();
                    for (ConfiguredContest cc2 : ccs)
                        if (!cc2.equals(cc)) { %>
                        <a href="<%= webroot%>/scoreboardCompare/<%= cc2.getId() %>"><%= cc2.getId() %>
                        </a>&nbsp;&nbsp;
                        <% } %>

                        <a href="<%= webroot%>/scoreboardCompare/compare2src">source</a>
                    </p>
                    <% } %>

                    <table id="score-table" class="table table-sm table-hover table-striped table-head-fixed">
                        <thead>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<script type="text/html" id="header-start">
  <th class="text-right">Rank</th><th></th><th>Team</th>
</script>
<script type="text/html" id="header-end">
  <th class="text-right">Solved</th><th class="text-right">Time</th>
</script>
<script type="text/html" id="header-end-score">
  <th class="text-right">Score</th><th class="text-right">Time</th>
</script>
<script type="text/html" id="problem">
<th class="text-center"><span class="badge" style="background-color:{{rgb}}; width:25px; border:1px solid {{border}}"><font color={{fg}}>{{label}}</font></span></th>
</script>
<script type="text/html" id="row-start">
  <td class="text-right">{{rank}}</td><td class="text-center"><img src="{{logo}}" height=20/></td><td>{{team}}</td>
</script>
<script type="text/html" id="row-end">
  <td class="text-right">{{numSolved}}</td><td class="text-right">{{totalTime}}</td>
</script>
<script type="text/html" id="row-end-score">
  <td class="text-right">{{score}}</td><td class="text-right">{{time}}</td>
</script>
<script type="text/html" id="cell">
<td class="text-center {{ scoreClass }}">{{num}}{{#solved}} / {{time}}{{/solved}}</td>
</script>
<script type="text/html" id="cell-score">
<td class="text-center {{ scoreClass }}">{{score}}{{#score}} / {{time}}{{/score}}</td>
</script>
<script type="text/javascript">
contest = new Contest("/api", "<%= cc.getId() %>");
registerContestObjectTable("score");

function getRow(scr, type) {
	var row = $('<tr id="team' + scr.team_id + '"></tr>');
    var logoSrc = '';
    var team = '';
    if (scr.team_id != null) {
        team = findById(teams, scr.team_id);
        if (team != null) {
            var org = findById(orgs, team.organization_id);
            if (org != null) {
                var logo = bestSquareLogo(org.logo, 20);
                if (logo != null)
                    logoSrc = '/api/' + logo.href;
            }
            team = getDisplayStr(team.id);
        }
    }

    obj = { rank: scr.rank, logo: logoSrc, team: team }
    row.append(toHtml("row-start", obj));
    for (var j = 0; j < problems.length; j++) {
        obj = new Object();
    	
        for (var k = 0; k < scr.problems.length; k++) {
            var prob = scr.problems[k];
            if (prob.problem_id == problems[j].id) {
                if (prob.first_to_solve == true)
                    obj.scoreClass = 'bg-success';
                else if (prob.solved == true || prob.score > 0)
                    obj.scoreClass = 'table-success';
                else if (prob.num_pending > 0)
                    obj.scoreClass = 'table-warning';
                else
                    obj.scoreClass = 'table-danger';
                obj.num = prob.num_judged + prob.num_pending;
                obj.solved = prob.solved;
                obj.time = prob.time;
                obj.score = prob.score;
            }
        }
        if ("score" == type)
        	row.append(toHtml("cell-score", obj));
        else
        	row.append(toHtml("cell", obj));
    }
    obj = { };
    if (scr.score.num_solved > 0)
    	obj.numSolved = scr.score.num_solved;
    if (scr.score.total_time > 0)
    	obj.totalTime = scr.score.total_time;
    if (scr.score.score != 0)
    	obj.score = scr.score.score;
    if (scr.score.time > 0)
    	obj.time = scr.score.time;
    if ("score" == type)
    	row.append(toHtml("row-end-score", obj));
    else
    	row.append(toHtml("row-end", obj));
    return row;
}

var columnWidths = new Array();

$(document).ready(function () {
	var x = $("#score-refresh");
    if (x != null)
    	x.attr("onclick", 'updateTable()');
    
    function createTableHeader() {
        $("#score-table thead").find("tr").remove();
        var row = $('<tr></tr>');
        row.append(toHtml("header-start"));
   
        problems = contest.getProblems();
        for (var i = 0; i < problems.length; i++) {
        	var p = { label: problems[i].label };
        	p = addColors(p, problems[i].rgb);
        	row.append($(toHtml("problem", p)));
        }

        score = contest.getScoreboard();
        if ("score" == contest.getInfo().scoreboard_type)
        	row.append(toHtml("header-end-score"));
        else
       		row.append(toHtml("header-end"));
        
        $('#score-table thead').append(row);
    }

    function fillTable() {
    	var table = $("#score-table");
        
    	$("#score-table tbody").find("tr").remove();
        score = contest.getScoreboard();
        teams = contest.getTeams();
        orgs = contest.getOrganizations();
        problems = contest.getProblems();
        var type = contest.getInfo().scoreboard_type;
        for (var i = 0; i < score.rows.length; i++) {
        	var row = getRow(score.rows[i], type);
            $('#score-table tbody').append(row);
        }
        
     	// set every td's width
		table.find('tr:first-child th').each(function() {
			columnWidths.push($(this).outerWidth(true));
		});

		table.find('tr td, tr th').each(function() {
			$(this).css( {
				width: columnWidths[$(this).index()]
			} );
		});
		
		// set every row's height and width
		table.find('tr').each(function() {
			$(this).width($(this).outerWidth(true));
			$(this).height($(this).outerHeight(true));
		});
		
		// set the table height and width
		table.height(table.outerHeight()).width(table.outerWidth());
		
		// set the current vertical position
		var y = 0;
		table.find('tr').each(function(index) {
			$(this).css('top', y);
			y += $(this).outerHeight();
		});

		table.css('position', 'relative');
    	table.css('display', 'inline-block');
		
		// make all the tr's absolute
		table.find('tr').each(function(index,el) {
			$(this).css('position', 'absolute');
		});
    }

    $.when(contest.loadInfo(),contest.loadOrganizations(), contest.loadTeams(), contest.loadProblems(), contest.loadScoreboard()).done(function () {
    	createTableHeader();
        fillTable();
    }).fail(function (result) {
    	console.log("Error loading scoreboard: " + result);
    })
    
    updateContestClock(contest, "contest-time");
})

function updateTable() {
	contest.clearScoreboard();
	$.when(contest.loadScoreboard()).done(function () {
		 updateTableImpl();
    }).fail(function (result) {
    	console.log("Error loading scoreboard: " + result);
    })
}
	
function updateTableImpl() {
	var table = $("#score-table");
	var y = table.find('tr:eq(1)').position().top;
	
   	score = contest.getScoreboard();
    for (var i = 0; i < score.rows.length; i++) {
    	var scr = score.rows[i];
    	var oldRow = $('#team' + scr.team_id);
    	var row = getRow(scr);
        
        // replace the content and class of each cell (except logo and team name, which shouldn't change)
        var numTds = row.children('td').length;
        for (var j = 0; j < numTds; j++) {
        	if (j > 0 && j < 3)
        		continue;
        	var td1 = oldRow.find('td').eq(j);
        	var td2 = row.find('td').eq(j);
        	td1.html(td2.html());
        	td1.attr("class", td2.attr("class"));
        }
        
        var oldY = oldRow.position().top;
        if (Math.abs(y - oldY) > 0.5) {
         	if (y > oldY) // slide rows down in 750ms
        		oldRow.stop().animate({ top: y}, 750, 'swing');
        	else {
        		// slide rows going up based on how many rows they have to go
        		// try 750ms + 500ms per log(numRows)
        		var diff = (oldY - y) / oldRow.outerHeight();
        		diff = 750 + Math.log(diff) * 500.0;
        		oldRow.stop().animate({ top: y}, diff, 'swing');
        	}
        }
    	y += oldRow.outerHeight();
    }
}
</script>
<%@ include file="layout/footer.jsp" %>
No newline at end of file
