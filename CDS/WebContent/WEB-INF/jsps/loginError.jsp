<% request.setAttribute("title", "Login unsuccessful"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-8">
        <div class="card">
        <div class="card-body p-0">
            <p class="indent">Invalid user or password</p>
        </div></div>
        </div>
        <div class="col-4">
          <img src="/logo.png" height="240"/>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>