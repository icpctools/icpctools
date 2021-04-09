<% request.setAttribute("title", "Details"); %>
<%@ include file="layout/head.jsp" %>
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
        <div class="col-9">
            <%@ include file="details-admin/contest.html" %>
        </div>
        <div class="col-3">
            <%@ include file="details-admin/state.html" %>
        </div>
    </div>
    <div class="row">
        <div class="col-5"><%@ include file="details-admin/languages.html" %></div>
        <div class="col-7"><%@ include file="details-admin/judgementTypes.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details-admin/problems.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details-admin/awards.html" %></div>
    </div>
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Judgements</h3>
                    <div class="card-tools">
                        <span id="judgements-count" title="?" class="badge bg-primary">?</span>
                        <button id="judgements-button" type="button" class="btn btn-tool">API</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Runs</h3>
                    <div class="card-tools">
                        <span id="runs-count" title="?" class="badge bg-primary">?</span>
                        <button id="runs-button" type="button" class="btn btn-tool">API</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script type="text/javascript">
    $(document).ready(function () {
        $.when(contest.loadJudgements()).done(function () {
        	updateContestObjectHeader("judgements", contest.getJudgements());
        }).fail(function (result) {
        	console.log("Error loading judgements: " + result);
        })

        $.when(contest.loadRuns()).done(function () {
        	updateContestObjectHeader("runs", contest.getRuns());
        }).fail(function (result) {
        	console.log("Error loading runs: " + result);
        })
    })
    updateContestClock(contest, "contest-time");
</script>
<%@ include file="layout/footer.jsp" %>