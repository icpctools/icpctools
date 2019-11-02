<%@ page import="java.util.List" %>
<% request.setAttribute("title", "Contest validation"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/contestMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Contest Validation</h1>
            <table class="table table-sm table-hover table-striped">
                <%
                    List<String> validationList = contest.validate();
                    if (validationList == null) { %>
                <tfoot>
                <tr>
                    <td>No errors</td>
                </tr>
                </tfoot>
                <% } else { %>
                <tbody>
                <% for (String s : validationList) { %>
                <tr>
                    <td><%= s %>
                    </td>
                </tr>
                <% } %>
                </tbody>
                <tfoot>
                <tr>
                    <td><%= validationList.size() %> errors.</td>
                </tr>
                </tfoot>
                <% } %>
            </table>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
</body>
</html>
