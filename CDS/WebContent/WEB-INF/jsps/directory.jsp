<%@page import="java.io.File" %>
<%
    String folder = (String) request.getAttribute("folder");
    File[] files = (File[]) request.getAttribute("files");
%>
<% request.setAttribute("title", "Directory listing"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Files in /<%= folder %></h3>
                </div>
                <div class="card-body p-0">
            <table class="table table-sm table-hover table-striped">
                <tbody>
                <% for (File f : files) { %>
                <tr>
                    <td><a href="<%= f.getName() %>"><%= f.getName() %>
                    </a></td>
                </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        </div>
    </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>