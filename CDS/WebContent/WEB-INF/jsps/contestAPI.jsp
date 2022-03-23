<% request.setAttribute("title", "Contest API"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Contest API</h1>

            Welcome to the CDS Contest API. You probably want to try <%= webroot %>/api/contests!

            <p/>

            For information on the Contest API, please go
            <a href="https://ccs-specs.icpc.io/master/contest_api" target="_blank">here</a>.

        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>