<% request.setAttribute("title", "Clarifications"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/contestMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1><a href="<%= apiRoot %>/clarifications">Clarifications</a> (<%= contest.getClarifications().length %>)
            </h1>

            <table id="clar-table" class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th class="text-center">Time</th>
                    <th>Problem</th>
                    <th>From Team</th>
                    <th>To Team</th>
                    <th>Text</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td colspan=6>Loading...</td>
                </tr>
                </tbody>
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
            $("#clar-table tbody").find("tr").remove();
            clars = contest.getClarifications();
            teams = contest.getTeams();
            problems = contest.getProblems();
            for (var i = 0; i < clars.length; i++) {
                var clar = clars[i];
                var problem = '';
                var time = '';
                var fromTeam = '';
                var toTeam = '';
                if (clar.contest_time != null) {
                    time = clar.contest_time;
                    if (time != null)
                        time = time;
                }
                if (clar.problem_id != null) {
                    problem = findById(problems, clar.problem_id);
                    if (problem != null)
                        problem = problem.label + ' (' + problem.id + ')';
                }
                if (clar.from_team_id != null) {
                    fromTeam = findById(teams, clar.from_team_id);
                    if (fromTeam != null)
                        fromTeam = fromTeam.id + ' (' + fromTeam.name + ')';
                }
                if (clar.to_team_id != null) {
                    toTeam = findById(teams, clar.to_team_id);
                    if (toTeam != null)
                        toTeam = toTeam.id + ' (' + toTeam.name + ')';
                }

                var col = $('<td><a href="<%= apiRoot %>/clarifications/' + clar.id + '">' + clar.id + '</a></td>' +
                    '<td>' + time + '</td><td>' + problem + '</td><td>' + fromTeam + '</td>' +
                    '<td>' + toTeam + '</td><td class="pre-line">' + clar.text + '</td>');
                var row = $('<tr></tr>');
                row.append(col);
                $('#clar-table tbody').append(row);
            }

            if (clars.length === 0) {
                col = $('<td colspan="6">No clarifications (yet)</td>');
                row = $('<tr></tr>');
                row.append(col);
                $('#clar-table tbody').append(row);
            }
        }

        sortByColumn($('#clar-table'));

        $.when(contest.loadClarifications(), contest.loadTeams(), contest.loadProblems()).done(function () {
            fillTable()
        }).fail(function (result) {
            alert("Error loading page: " + result);
        })
    })
</script>
</body>
</html>