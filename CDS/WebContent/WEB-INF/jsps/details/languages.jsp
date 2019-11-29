<div class="card collapsed-card">
    <div class="card-header">
        <h3 class="card-title">Languages</h3>
        <div class="card-tools">
            <span id="languages-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/languages'">API</button>
            <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
        </div>
    </div>
    <div class="card-body p-0">
        <table id="languages-table" class="table table-sm table-hover table-striped table-head-fixed">
            <thead>
                <tr>
                    <th>Id</th>
                    <th>Name</th>
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

        function langTd(lang) {
            return $('<td><a href="<%= apiRoot %>/languages/' + lang.id + '">' + lang.id + '</td><td>' + lang.name + '</td>');
        }

        $.when(contest.loadLanguages()).done(function () {
            fillContestObjectTable("languages", contest.getLanguages(), langTd)
        }).fail(function (result) {
            alert("Error loading page: " + result);
            console.log(result);
        });
    });
</script>