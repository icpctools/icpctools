<%@ page import="org.icpc.tools.cds.util.Role" %>
<% request.setAttribute("title", "Scoreboard"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Scoreboard</h3>
                    <div class="card-tools"><a href="<%= apiRoot %>/scoreboard">API</a></div>
                </div>
                <div class="card-body p-0">
                    <% if (Role.isBlue(request)) { %>
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
  <th class="text-right">Rank</th><th></th><th>Team</th><th>Organization</th>
</script>
<script type="text/html" id="header-end">
  <th class="text-right">Solved</th><th class="text-right">Time</th>
</script>
<script type="text/html" id="problem">
<th class="text-center"><span class="badge" style="background-color:{{rgb}}; width:25px; border:1px solid {{border}}"><font color={{fg}}>{{label}}</font></span></th>
</script>
<script type="text/html" id="row-start">
  <td class="text-right">{{rank}}</td><td class="text-center"><img src="{{logo}}" height=20/></td><td>{{team}}</td><td>{{org}}</td>
</script>
<script type="text/html" id="row-end">
  <td class="text-right">{{numSolved}}</td><td class="text-right">{{totalTime}}</td>
</script>
<script type="text/html" id="cell">
<td class="text-center {{ scoreClass }}">{{num}}{{#solved}} / {{time}}{{/solved}}</td>
</script>
<script type="text/javascript">
contest.setContestURL("/api","<%= cc.getId() %>");
registerContestObjectTable("score");

$(document).ready(function () {
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

        row.append(toHtml("header-end"));
        $('#score-table thead').append(row);
    }

    function fillTable() {
        $("#score-table tbody").find("tr").remove();
        score = contest.getScoreboard();
        teams = contest.getTeams();
        orgs = contest.getOrganizations();
        problems = contest.getProblems();
        for (var i = 0; i < score.rows.length; i++) {
            var scr = score.rows[i];
            var logoSrc = '';
            var team = '';
            var orgName = '';
            if (scr.team_id != null) {
                team = findById(teams, scr.team_id);
                if (team != null) {
                    var org = findById(orgs, team.organization_id);
                    if (org != null) {
                        var logo = bestSquareLogo(org.logo, 20);
                        if (logo != null)
                            logoSrc = '/api/' + logo.href;
                        orgName = org.name;
                    }
                    if (team.display_name != null)
                        team = team.id + ': ' + team.display_name;
                    else
                        team = team.id + ': ' + team.name;
                }
            }

            obj = { rank: scr.rank, logo: logoSrc, team: team, org: orgName }
            var col = toHtml("row-start", obj);
            var row = $('<tr></tr>');
            row.append(col);
            for (var j = 0; j < problems.length; j++) {
                obj = new Object();
            	
                for (var k = 0; k < scr.problems.length; k++) {
                    var prob = scr.problems[k];
                    if (prob.problem_id == problems[j].id) {
                        if (prob.first_to_solve == true)
                            obj.scoreClass = 'bg-success';
                        else if (prob.solved == true)
                            obj.scoreClass = 'table-success';
                        else if (prob.num_pending > 0)
                            obj.scoreClass = 'table-warning';
                        else
                            obj.scoreClass = 'table-danger';
                        obj.num = prob.num_judged + prob.num_pending;
                        if (prob.solved) {
                        	obj.solved = true;
                            obj.time = prob.time;
                    	}
                    }
                }
                var prb = toHtml("cell", obj);
                row.append(prb);
            }
            obj = { numSolved: scr.score.num_solved, totalTime: scr.score.total_time }
            var col2 = toHtml("row-end", obj);
            row.append(col2);
            $('#score-table tbody').append(row);
        }
    }

    sortByColumn($('#score-table'));

    $.when(contest.loadOrganizations(), contest.loadTeams(), contest.loadProblems(), contest.loadScoreboard()).done(function () {
    	createTableHeader();
        fillTable();
    }).fail(function (result) {
    	console.log("Error loading scoreboard: " + result);
    })
})
</script>
<%@ include file="layout/footer.jsp" %>