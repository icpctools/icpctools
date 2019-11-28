<div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">Teams</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getNumTeams() %>" class="badge bg-primary"><%= contest.getNumTeams() %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/teams'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
        <div class="card-body p-0">
            <table id="team-table" class="table table-sm table-hover table-striped table-head-fixed">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Name</th>
                    <th>Organization</th>
                    <th></th>
                    <th>Organization (formal name)</th>
                    <th>Group</th>
                    <th>Summary</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td colspan=7><div class="spinner-border"></div></td>
                </tr>
                </tbody>
            </table>
            </div>
         </div>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");

        function fillTable() {
            $("#team-table tbody").find("tr").remove();
            teams = contest.getTeams();
            orgs = contest.getOrganizations();
            groups = contest.getGroups();
            for (var i = 0; i < teams.length; i++) {
                var team = teams[i];
                var org = findById(orgs, team.organization_id);
                var orgName = '';
                var orgFormalName = '';
                var logoSrc = '';
                if (org != null) {
                    orgName = org.name;
                    if (org.formal_name != null)
                        orgFormalName = org.formal_name;
                    var logo = bestSquareLogo(org.logo, 20);
                    if (logo != null)
                        logoSrc = '/api/' + logo.href;
                }
                var groupNames = '';
                var groups2 = findGroups(team.group_ids);
                if (groups2 != null) {
                    var first = true;
                    for (var j = 0; j < groups2.length; j++) {
                        if (!first)
                            groupNames += ', ';
                        groupNames += groups2[j].name;
                        first = false;
                    }
                }

                var col = $('<td><a href="<%= apiRoot %>/teams/' + team.id + '">' + team.id + '</td><td>' + team.name + '</td><td align=center><img src="' + logoSrc + '" height=20/></td><td>' + orgName + '</td><td>' + orgFormalName + '</td><td>' + groupNames + '</td>'
                    + '<td><a href="<%= webroot  %>/teamSummary/' + team.id + '">summary</a></td>');
                var row = $('<tr></tr>');
                row.append(col);
                $('#team-table tbody').append(row);
            }

            if (teams.length === 0) {
                col = $('<td colspan="7">No teams</td>');
                row = $('<tr></tr>');
                row.append(col);
                $('#team-table tbody').append(row);
            }
        }

        sortByColumn($('#org-table'));

        $.when(contest.loadTeams(), contest.loadOrganizations(), contest.loadGroups()).done(function () {
            fillTable()
        }).fail(function (result) {
            alert("Error loading page: " + result);
            console.log(result);
        })

    })
</script>