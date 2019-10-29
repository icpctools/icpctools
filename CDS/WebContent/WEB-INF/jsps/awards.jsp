<% request.setAttribute("title", "Awards"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/menu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1><a href="<%= apiRoot %>/awards">Awards</a> (<%= contest.getAwards().length %>)</h1>
            <table id="award-table" class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Citation</th>
                    <th>Teams</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td colspan=3>Loading...</td>
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
            $("#award-table tbody").find("tr").remove();
            var awards = contest.getAwards();
            var col;
            var row;
            for (var i = 0; i < awards.length; i++) {
                var award = awards[i];

                var teamsStr = "";
                for (var j = 0; j < award.team_ids.length; j++) {
                    if (j > 0)
                        teamsStr += "<br>";
                    teamsStr += award.team_ids[j] + ": ";
                    var t = contest.getTeamById(award.team_ids[j]);
                    if (t != null)
                        teamsStr += t.name;
                }
                col = $('<td><a href="<%= apiRoot %>/awards/' + award.id + '">' + award.id + '</td><td>' + award.citation + '</td><td>' + teamsStr + '</td>');
                row = $('<tr></tr>');
                row.append(col);
                $('#award-table tbody').append(row);
            }
            if (awards.length === 0) {
                col = $('<td colspan="3">No awards (yet)</td>');
                row = $('<tr></tr>');
                row.append(col);
                $('#award-table tbody').append(row);
            }
        }

        $.when(contest.loadAwards(), contest.loadTeams()).done(function () {
            fillTable()
        }).fail(function (result) {
            alert("Error loading page: " + result);
            console.log(result);
        });
    });
</script>
</body>
</html>