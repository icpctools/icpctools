<div class="card collapsed-card">
    <div class="card-header">
        <h3 class="card-title">Problems</h3>
        <div class="card-tools">
            <span id="problems-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/problems'">API</button>
            <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
        </div>
    </div>
    <div class="card-body p-0">
        <table id="problems-table" class="table table-sm table-hover table-striped table-head-fixed">
            <thead>
                <tr>
                    <th>Id</th>
                    <th>Label</th>
                    <th>Name</th>
                    <th>Color</th>
                    <th>RGB</th>
                    <th></th>
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

        function problemTd(problem) {
            return $('<td><a href="<%= apiRoot %>/problems/' + problem.id + '">' + problem.id + '</td><td>' + problem.label
                + '</td><td>' + problem.name + '</td><td>' + problem.color + '</td><td>' + problem.rgb + '</td><td><div class="circle" style="background-color:' + problem.rgb + '"></div></td>');
        }

        $.when(contest.loadProblems()).done(function () {
            fillContestObjectTable("problems", contest.getProblems(), problemTd)
        }).fail(function (result) {
            alert("Error loading page: " + result);
            console.log(result);
        });
    });
</script>