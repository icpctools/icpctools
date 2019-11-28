<%@page import="java.io.File" %>
<%
    String folder = (String) request.getAttribute("folder");
    File[] files = (File[]) request.getAttribute("files");
%>
<% request.setAttribute("title", "Directory " + folder); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>
                Directory listing for <%= folder %>
            </h1>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Files</th>
                </tr>
                </thead>
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
<%@ include file="layout/footer.jsp" %>