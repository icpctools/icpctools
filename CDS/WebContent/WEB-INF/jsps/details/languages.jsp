<div id="accordion">
<div class="card">
    <div class="card-header">
        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseLanguages">Languages</a></h4>
        <div class="card-tools">
            <span id="languages-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/languages'">API</button>
        </div>
    </div>
    <div id="collapseLanguages" class="panel-collapse collapse in">
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
</div>
</div>
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");

        function langTd(lang) {
            return $('<td><a href="<%= apiRoot %>/languages/' + lang.id + '">' + lang.id + '</td><td>' + sanitizeHTML(lang.name) + '</td>');
        }

        $.when(contest.loadLanguages()).done(function () {
            fillContestObjectTable("languages", contest.getLanguages(), langTd)
        }).fail(function (result) {
        	console.log("Error loading languages: " + result);
        });
    });
</script>