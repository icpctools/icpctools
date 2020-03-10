<% request.setAttribute("title", "Search for '" + request.getAttribute("value") + "'"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Results</h3>
                </div>
                <div class="card-body p-0">
                    <table id="search-table" class="table table-sm table-hover table-striped">
                        <thead>
                            <tr>
                                <th>Contest</th>
                                <th>Type</th>
                                <th>Id</th>
                                <th>Text</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/search.js"></script>
<script type="text/javascript">
    window.onload = function () {
        $("#search-table tbody").find("tr").remove();
        var results = <%= request.getAttribute("result") %>;
        var result = results[0];
        result = result.results;
        for (var i = 0; i < result.length; i++) {
            var contestResult = result[i];
            var contestId = contestResult.contest_id;
            var results2 = contestResult.results;
            if (results2.length == 0) {
                var col = $('<td>' + contestId + '</td><td colspan=3>No hits</td>');
                var row = $('<tr></tr>');
                row.append(col);
                $('#search-table tbody').append(row);
            }
            for (var j = 0; j < results2.length; j++) {
                var type = results2[j].type;
                var id = results2[j].id;
                var text = results2[j].text;
                if (text.length > 30)
                    text = text.substring(0, 30) + "...";
                var col = $('<td><a href="/contests/' + contestId + '">' + contestId + '</a></td><td>' + type + '</td>' +
                    '<td><a href="/api/contests/' + contestId + '/' + type + '/' + id + '">' + id + '</a></td>' +
                    '<td>' + sanitize(text) + '</td>');
                var row = $('<tr></tr>');
                row.append(col);
                $('#search-table tbody').append(row);
            }
        }
    };
</script>
<%@ include file="layout/footer.jsp" %>