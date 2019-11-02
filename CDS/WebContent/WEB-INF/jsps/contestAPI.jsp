<% request.setAttribute("title", "Contest API"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/baseMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Contest API</h1>

            Welcome to the CDS Contest API. You probably want to try <%= webroot %>/api/contests!

            <p/>

            For information on using the Contest API, please go
            <a href="https://clics.ecs.baylor.edu/index.php/Contest_API" target="_blank">here</a>.

        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
</body>
</html>