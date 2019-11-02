<%@ page import="org.icpc.tools.cds.util.Role" %>
<%@ page import="org.icpc.tools.cds.CDSConfig" %>
<%@ page import="org.icpc.tools.contest.model.IProblem" %>
<% request.setAttribute("title", "Scoreboard"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/menu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1><a href="<%= apiRoot %>/scoreboard">Scoreboard</a></h1>

            <% if (Role.isBlue(request)) { %>
            <p>
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

            <table id="score-table" class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th class="text-right">Rank</th>
                    <th></th>
                    <th>Team</th>
                    <th>Organization</th>
                    <% int numProblems = contest.getNumProblems();
                        for (int j = 0; j < numProblems; j++) {
                            IProblem p = contest.getProblems()[j]; %>

                    <th class="text-center">
                        <%= p.getLabel() %>
                        <div class="circle" style="background-color: <%= p.getRGB() %>"></div>
                    </th>
                    <% } %>

                    <th class="text-right">Solved</th>
                    <th class="text-right">Time</th>
                </tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");

        function fillTable() {
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
                            org = org.name;
                        }
                        team = team.id + ': ' + team.name;
                    }
                }

                var col = $('<td class="text-right">' + scr.rank + '</td><td class="text-center"><img src="' + logoSrc + '" height=20/></td><td>' + team + '</td><td>' + orgName + '</td>');
                var row = $('<tr></tr>');
                row.append(col);
                for (var j = 0; j < problems.length; j++) {
                    var prb = $('<td  class="text-center"></td>');
                    for (var k = 0; k < scr.problems.length; k++) {
                        var prob = scr.problems[k];
                        var scoreClass;
                        if (prob.problem_id == problems[j].id) {
                            if (prob.first_to_solve == true)
                                scoreClass = 'bg-success';
                            else if (prob.solved == true)
                                scoreClass = 'table-success';
                            else if (prob.num_judged > 0)
                                scoreClass = 'table-danger';
                            else
                                scoreClass = 'table-warning';
                            var p = '<td class="text-center ' + scoreClass + '">' + prob.num_judged;
                            if (prob.solved)
                                p += ' / ' + prob.time;
                            p += '</td>';
                            prb = $(p);
                        }
                    }
                    row.append(prb);
                }
                var col2 = $('<td  class="text-right">' + scr.score.num_solved + '</td><td  class="text-right">' + scr.score.total_time + '</td>');
                row.append(col2);
                $('#score-table tbody').append(row);
            }
        }

        sortByColumn($('#score-table'));

        $.when(contest.loadOrganizations(), contest.loadTeams(), contest.loadProblems(), contest.loadScoreboard()).done(function () {
            fillTable()
        }).fail(function (result) {
            alert("Error loading page: " + result);
        })
    })
</script>
</body>
</html>