<%@ page import="org.icpc.tools.contest.model.*" %>
<% request.setAttribute("title", "Details"); %>
<%@ include file="layout/head.jsp" %>
<% IState state = contest.getState(); %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<script type="text/javascript">
contest = new Contest("/api", "<%= cc.getId() %>");
</script>
<div class="container-fluid">
    <div class="row">
        <div class="col-7"><%@ include file="details/problems.html" %></div>
        <div class="col-5"><%@ include file="details/contest.html" %><%@ include file="details/languages.html" %></div>
    </div>
    <div class="row">
        <div class="col-7"><%@ include file="details/judgementTypes.html" %></div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>