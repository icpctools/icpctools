<%@ page isErrorPage="true" %>
<% request.setAttribute("title", "Error"); %>
<!doctype html>
<html>
<%@ include file="WEB-INF/jsps/layout/head.jsp" %>
<body>
<%@ include file="WEB-INF/jsps/layout/baseMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>${pageContext.errorData.statusCode} Error</h1>

            <p>Detail</p>
            <pre>${pageContext.errorData.throwable.message}</pre>

            <p>For up to date information on the services provided, please see the
                <a href="https://clics.ecs.baylor.edu/index.php/CDS" target="_blank">CLICS CDS documentation</a>.</p>

        </div>
    </div>
</div>
</body>
</html>