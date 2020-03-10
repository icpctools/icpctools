<% request.setAttribute("title", "Admin"); %>
<%@ include file="layout/head.jsp" %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<div class="container-fluid">
    <div class="row">
        <div class="col-8">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Administration</h3>
           </div>
        <div class="card-body">
          Sorry, contest administration is restricted to CDS administrators.
        </div></div>
      </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>