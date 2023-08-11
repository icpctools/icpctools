<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<% request.setAttribute("title", "Multi-contest Countdown Control"); %>
<%@ include file="layout/head.jsp" %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/cds.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<div class="container-fluid">
    <div class="row">
        <div class="col-10">
            <%
                List<IContest> contests = new ArrayList<>();
                for (ConfiguredContest c : CDSConfig.getContests()) {
                    contests.add(c.getContest());
                }
                IContest[] countdownContests = contests.toArray(new IContest[0]);
                boolean multiCountdown = true;
            %>
            <%@ include file="countdown-control.jsp" %>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
