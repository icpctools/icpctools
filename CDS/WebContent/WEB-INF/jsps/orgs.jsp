        <div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">Organizations</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getOrganizations().length %>" class="badge bg-primary"><%= contest.getOrganizations().length %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/organizations'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
        <div class="card-body p-0">
            <table id="org-table" class="table table-sm table-hover table-striped table-head-fixed">
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
                    <td colspan=5><div class="spinner-border"></div></td>
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
            $("#org-table tbody").find("tr").remove();
            orgs = contest.getOrganizations();
            for (var i = 0; i < orgs.length; i++) {
                var org = orgs[i];
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

                var col = $('<td><a href="<%= apiRoot %>/organizations/' + org.id + '">' + org.id + '</a></td><td align=middle><img src="' + logoSrc + '" height=20/></td>' +
                    '<td>' + org.name + '</td><td>' + formal_name + '</td><td>' + country + '</td>');
                var row = $('<tr></tr>');
                row.append(col);
                $('#org-table tbody').append(row);
            }
            if (orgs.length === 0) {
                col = $('<td colspan="5">No organizations</td>');
                row = $('<tr></tr>');
                row.append(col);
                $('#org-table tbody').append(row);
            }
        }

        sortByColumn($('#org-table'));

        $.when(contest.loadOrganizations()).done(function () {
            fillTable()
        }).fail(function (result) {
            alert("Error loading page: " + result);
        })
    })
</script>