<% request.setAttribute("title", "Organizations"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/contestMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1><a href="<%= apiRoot %>/organizations">Organizations</a> (<%= contest.getOrganizations().length %>)</h1>

            <table id="org-table" class="table table-sm table-hover table-striped">
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
                    <td colspan=5>Loading...</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
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
</body>
</html>