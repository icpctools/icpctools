<div class="card collapsed-card">
    <div class="card-header">
        <h3 class="card-title">Judgement Types</h3>
        <div class="card-tools">
            <span id="judgement-types-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool"
                onclick="location.href='<%= apiRoot %>/judgement-types'">API</button>
            <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
        </div>
    </div>
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
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");

        function judgementTypeTd(jt) {
            var penalty = jt.penalty;
            var solved = jt.solved;
            return $('<td><a href="<%= apiRoot %>/judgement-types/' + jt.id + '">' + jt.id + '</td><td>' + jt.name + '</td><td>' + penalty + '</td><td>' + solved + '</td>');
        }

        $.when(contest.loadJudgementTypes()).done(function () {
            fillContestObjectTable("judgement-types", contest.getJudgementTypes(), judgementTypeTd)
        }).fail(function (result) {
            alert("Error loading page: " + result);
            console.log(result);
        });
    });
</script>