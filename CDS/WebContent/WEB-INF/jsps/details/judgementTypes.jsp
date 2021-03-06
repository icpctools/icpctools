<div id="accordion">
<div class="card">
    <div class="card-header">
        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseJudgementTypes">Judgement Types</a></h4>
        <div class="card-tools">
            <span id="judgement-types-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool"
                onclick="location.href='<%= apiRoot %>/judgement-types'">API</button>
        </div>
    </div>
    <div id="collapseJudgementTypes" class="panel-collapse collapse in">
    <div class="card-body p-0">
        <table id="judgement-types-table" class="table table-sm table-hover table-striped table-head-fixed">
            <thead>
                <tr>
                    <th>Id</th>
                    <th>Name</th>
                    <th>Penalty</th>
                    <th>Solved</th>
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

        function judgementTypeTd(jt) {
            var penalty = jt.penalty;
            var solved = jt.solved;
            return $('<td><a href="<%= apiRoot %>/judgement-types/' + jt.id + '">' + jt.id + '</td><td>' + sanitizeHTML(jt.name) + '</td><td>' + penalty + '</td><td>' + solved + '</td>');
        }

        $.when(contest.loadJudgementTypes()).done(function () {
            fillContestObjectTable("judgement-types", contest.getJudgementTypes(), judgementTypeTd)
        }).fail(function (result) {
        	console.log("Error loading judgement types: " + result);
        });
    });
</script>