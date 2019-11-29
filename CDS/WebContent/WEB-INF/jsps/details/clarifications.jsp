<div class="card collapsed-card">
    <div class="card-header">
        <h3 class="card-title">Clarifications</h3>
        <div class="card-tools">
            <span id="clarifications-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool"
                onclick="location.href='<%= apiRoot %>/clarifications'">API</button>
            <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
        </div>
    </div>
    <div class="card-body p-0">
        <table id="clarifications-table" class="table table-sm table-hover table-striped table-head-fixed">
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
                    <td colspan=6>
                        <div class="spinner-border"></div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");

        function clarTd(clar) {
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
                problem = findById(contest.getProblems(), clar.problem_id);
                if (problem != null)
                    problem = problem.label + ' (' + problem.id + ')';
            }
            var teams = contest.getTeams();
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

            return $('<td><a href="<%= apiRoot %>/clarifications/' + clar.id + '">' + clar.id + '</a></td>' +
                '<td>' + time + '</td><td>' + problem + '</td><td>' + fromTeam + '</td>' +
                '<td>' + toTeam + '</td><td class="pre-line">' + clar.text + '</td>');
        }

        $.when(contest.loadClarifications(), contest.loadTeams(), contest.loadProblems()).done(function () {
            fillContestObjectTable("clarifications", contest.getClarifications(), clarTd)
        }).fail(function (result) {
            alert("Error loading page: " + result);
        })
    })
</script>