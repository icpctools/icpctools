<div id="accordion">
<div class="card">
    <div class="card-header">
        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseAwards">Awards</a></h4>
        <div class="card-tools">
            <span id="awards-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/awards'">API</button>
        </div>
    </div>
    <div id="collapseAwards" class="panel-collapse collapse in">
    <div class="card-body p-0">
        <table id="awards-table" class="table table-sm table-hover table-striped table-head-fixed">
            <thead>
                <tr>
                    <th>Id</th>
                    <th>Citation</th>
                    <th>Teams</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td colspan=3>
                        <div class="spinner-border"></div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    </div>
</div>
</div>
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");

        function awardTd(award) {
            var teamsStr = "";
            for (var j = 0; j < award.team_ids.length; j++) {
                if (j > 0)
                    teamsStr += "<br>";
                teamsStr += award.team_ids[j] + ": ";
                var t = contest.getTeamById(award.team_ids[j]);
                if (t != null)
                    teamsStr += t.name;
            }
            return $('<td><a href="<%= apiRoot %>/awards/' + award.id + '">' + award.id + '</td><td>' + sanitizeHTML(award.citation) + '</td><td>' + teamsStr + '</td>');
        }

        $.when(contest.loadAwards(), contest.loadTeams()).done(function () {
            fillContestObjectTable("awards", contest.getAwards(), awardTd)
        }).fail(function (result) {
        	console.log("Error loading awards: " + result);
        });
    });
</script>