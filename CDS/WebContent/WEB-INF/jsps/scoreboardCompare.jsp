<% request.setAttribute("title", "Scoreboard Comparison"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/menu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Scoreboard Comparison</h1>

            <table class="table table-sm table-hover table-striped">
                <tbody>
                <tr>
                    <td>Comparing</td>
                    <td>
                      <%= request.getAttribute("a") %>
                    </td>
                </tr>
                <tr>
                    <td class="text-right">to</td>
                    <td>
                      <%= request.getAttribute("b") %>
                    </td>
                </tr>
                </tbody>
            </table>

            <%= (String) request.getAttribute("compare") %>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
</body>
</html>