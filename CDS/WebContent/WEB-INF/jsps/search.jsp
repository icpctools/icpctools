<% request.setAttribute("title", "Search"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/baseMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Search</h1>

            <form class="form-inline" onsubmit="searchFor(document.getElementById('searchText').value); return false;">
                <input type="text" class="form-control mr-2" id="searchText" placeholder="Search..."/>
                <button type="submit" id="search" class="btn btn-primary">
                    Search
                </button>
            </form>

            <h3>Results</h3>

            <table id="search-table" class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Contest</th>
                    <th>Type</th>
                    <th>Id</th>
                </tr>
                </thead>
                <tbody></tbody>
            </table>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/search.js"></script>
<script type="text/javascript">

    function searchFor(text) {
        console.log("search for: " + text);
        $.when(search.search(text)).done(function () {
            fillTable();
        }).fail(function (result) {
            console.log(result);
            alert("Could not perform search (" + result.status + ":" + result.statusText + ")");
        })
    }

    function fillTable() {
        $("#search-table tbody").find("tr").remove();
        var results = search.getResults();
        var result = results[0];
        result = result.results;
        for (var i = 0; i < result.length; i++) {
            var contestResult = result[i];
            var contestId = contestResult.contest_id;
            var results2 = contestResult.results;
            if (results2.length == 0) {
                var col = $('<td>' + contestId + '</td><td colspan=2>No hits</td>');
                var row = $('<tr></tr>');
                row.append(col);
                $('#search-table tbody').append(row);
            }
            for (var j = 0; j < results2.length; j++) {
                var type = results2[j].type;
                var id = results2[j].id;
                var col = $('<td><a href="/contests/' + contestId + '">' + contestId + '</a></td><td>' + type + '</td>' +
                    '<td><a href="/api/contests/' + contestId + '/' + type + '/' + id + '">' + id + '</a></td>');
                var row = $('<tr></tr>');
                row.append(col);
                $('#search-table tbody').append(row);
            }
        }
    };
</script>
</body>
</html>