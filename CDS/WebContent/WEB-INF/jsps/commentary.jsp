<% request.setAttribute("title", "Commentary"); %>
<%@ include file="layout/head.jsp" %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Commentary</h4>
			        <div class="card-tools">
			            <button id="commentary-refresh" type="button" class="btn btn-tool" ><i class="fas fa-sync-alt"></i></button>
			            <span id="commentary-count" title="?" class="badge bg-primary">?</span>
			            <button id="commentary-api" type="button" class="btn btn-tool">API</button>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="commentary-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th>Id</th>
			                    <th class="text-center">Time</th>
			                    <th class="text-center">Problems</th>
			                    <th>Teams</th>
			                    <th width=50%>Message</th>
			                </tr>
			            </thead>
			            <tbody></tbody>
			        </table>
			    </div>
			</div>
		</div>
    </div>
</div>
<script type="text/html" id="commentary-template">
  <td><a href="{{api}}">{{id}}</a></td>
  <td class="text-center">{{{time}}}</td>
  <td class="text-center">{{#problems}}<span class="badge" style="background-color:{{rgb}}; width:25px; border:1px solid {{border}}"><font color={{fg}}>{{label}}</font></span>{{/problems}}</td>
  <td>{{#teams}}{{#logo}}<img src="{{{logo}}}" width="20" height="20"/> {{/logo}}{{id}}: {{name}}{{/teams}}</td>
  <td class="pre-line">{{{message}}}</td>
</script>
<script type="text/javascript">
contest = new Contest("/api", "<%= cc.getId() %>");
registerContestObjectTable("commentary");

function commentaryRefresh() {
	contest.clear();
	$.when(contest.loadCommentary(), contest.loadTeams(), contest.loadOrganizations(), contest.loadProblems()).done(function () {
        fillContestObjectTable("commentary", contest.getCommentary());
    }).fail(function (result) {
    	console.log("Error loading commentary: " + result);
    })
}

$(document).ready(function () {
	commentaryRefresh();
})

updateContestClock(contest, "contest-time");
</script>
<%@ include file="layout/footer.jsp" %>