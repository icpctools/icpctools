<% request.setAttribute("title", "Reports"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Languages</h3>
                </div>
                <div class="card-body p-0">
                    <table id="langs-table" class="table table-sm table-hover table-striped">
                        <thead></thead>
                        <tbody>
                            <tr>
                                <td>
                                    <div class="spinner-border"></div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Runs</h3>
                </div>
                <div class="card-body p-0">
                    <table id="runs-table" class="table table-sm table-hover table-striped">
                        <thead></thead>
                        <tbody>
                            <tr>
                                <td>
                                    <div class="spinner-border"></div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Problems</h3>
                </div>
                <div class="card-body p-0">
                    <table id="problems-table" class="table table-sm table-hover table-striped">
                        <thead></thead>
                        <tbody>
                            <tr>
                                <td>
                                    <div class="spinner-border"></div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script type="text/javascript">
    $(document).ready(function () {
        console.log("Loading reports");
        $.ajax({
            url: '<%= apiRoot %>/report/langs',
            success: function (result) {
                fillTable('#langs-table', result);
            },
            failure: function (result) {
            	console.log("Error loading languages: " + result);
            }
        });
        $.ajax({
            url: '<%= apiRoot %>/report/runs',
            success: function (result) {
                fillTable('#runs-table', result);
            },
            failure: function (result) {
            	console.log("Error loading runs: " + result);
            }
        });
        $.ajax({
            url: '<%= apiRoot %>/report/problems',
            success: function (result) {
                fillTable('#problems-table', result);
            },
            failure: function (result) {
            	console.log("Error loading problems: " + result);
            }
        });
    });

    function fillTable(table, result) {
        $(table).find("tr").remove();
        var header = result[0];
        var col = '';
        for (k in header)
            if (k !== 'id')
                col += '<th>' + header[k] + '</th>';
        var row = $('<tr></tr>');
        row.append($(col));
        $(table).find('thead').append(row);

        for (var i = 1; i < result.length; i++) {
            var rep = result[i];
            var col = '';
            for (k in rep)
                if (k !== 'id')
                    col += '<td>' + sanitizeHTML(rep[k]) + '</td>';
            row = $('<tr></tr>');
            row.append($(col));
            $(table).find('tbody').append(row);
        }
        sortByColumn($(table));
    }
</script>
<%@ include file="layout/footer.jsp" %>