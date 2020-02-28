<div id="accordion">
<div class="card">
    <div class="card-header">
        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseOrganizations">Organizations</a></h4>
        <div class="card-tools">
            <span id="organizations-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
            <button type="button" class="btn btn-tool"
                onclick="location.href='<%= apiRoot %>/organizations'">API</button>
        </div>
    </div>
    <div id="collapseOrganizations" class="panel-collapse collapse in">
    <div class="card-body p-0">
        <table id="organizations-table" class="table table-sm table-hover table-striped table-head-fixed">
            <thead>
                <tr>
                    <th>Id</th>
                    <th></th>
                    <th>Name</th>
                    <th>Formal Name</th>
                    <th>Country</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td colspan=5>
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

        function orgTd(org) {
            var logoSrc = '';
            var logo = bestSquareLogo(org.logo, 20);
            if (logo != null)
                logoSrc = '/api/' + logo.href;

            var formal_name = '';
            if (org.formal_name != null)
                formal_name = org.formal_name;

            var country = '';
            if (org.country != null)
                country = org.country;

            return $('<td><a href="<%= apiRoot %>/organizations/' + org.id + '">' + org.id + '</a></td><td align=middle><img src="' + logoSrc + '" height=20/></td>' +
                '<td>' + org.name + '</td><td>' + formal_name + '</td><td>' + country + '</td>');
        }

        $.when(contest.loadOrganizations()).done(function () {
            fillContestObjectTable("organizations", contest.getOrganizations(), orgTd);
        }).fail(function (result) {
        	console.log("Error loading orgs: " + result);
        })
    })
</script>