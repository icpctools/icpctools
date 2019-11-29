<div class="card collapsed-card">
    <div class="card-header">
        <h3 class="card-title">Groups</h3>
        <div class="card-tools">
            <span id="groups-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/groups'">API</button>
            <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
        </div>
    </div>
    <div class="card-body p-0">
        <table id="groups-table" class="table table-sm table-hover table-striped table-head-fixed">
            <thead>
                <tr>
                    <th>Id</th>
                    <th>ICPC Id</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Hidden</th>
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

        function groupTd(group) {
            var typ = "";
            if (group.type != null)
                typ = group.type;
            var hidden = "";
            if (group.hidden != null)
                hidden = "true";
            return $('<td><a href="<%= apiRoot %>/groups/' + group.id + '">' + group.id + '</td><td>' + group.icpc_id + '</td><td>'
                + group.name + '</td><td>' + typ + '</td><td>' + hidden + '</td>');
        }

        $.when(contest.loadGroups()).done(function () {
            fillContestObjectTable("groups", contest.getGroups(), groupTd)
        }).fail(function (result) {
            alert("Error loading page: " + result);
            console.log(result);
        });
    });
</script>