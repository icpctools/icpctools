<%@ page import="java.util.List" %>
<% request.setAttribute("title", "Contest validation"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Validation</h3>
           </div>
        <div class="card-body p-0">
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
            </div></div>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>