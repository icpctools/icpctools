<%@ page import="org.icpc.tools.contest.model.*" %>
<% request.setAttribute("title", "Clarifications"); %>
<%@ include file="layout/head.jsp" %>
<% IState state = contest.getState(); %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<script type="text/javascript">
    contest.setContestURL("/api","<%= cc.getId() %>");
</script>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
        	<div id="accordion">
				<div class="card">
				    <div class="card-header">
				        <h4 class="card-title">Clarifications</h4>
				        <div class="card-tools">
				            <button id="clarifications-refresh" type="button" class="btn btn-tool" ><i class="fas fa-sync-alt"></i></button>
				            <span id="clarifications-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
				            <button id="clarifications-api" type="button" class="btn btn-tool">API</button>
				        </div>
				    </div>
				    <div class="card-body p-0">
				        <table id="clarifications-table" class="table table-sm table-hover table-striped table-head-fixed">
				            <thead>
				                <tr>
				                    <th>Id</th>
				                    <th class="text-center">Time</th>
				                    <th class="text-center">Problem</th>
				                    <th>From Team</th>
				                    <th>To Team</th>
				                    <th>Reply To</th>
				                    <th>Text</th>
				                </tr>
				            </thead>
				            <tbody></tbody>
				        </table>
				    </div>
				</div>
			</div>
		</div>
    </div>
</div>
<script type="text/html" id="clarifications-template">
  <td><a href="' + contest.getURL('clarifications', {{id}})">{{id}}</a></td>
  <td class="text-center">{{{time}}}</td>
  <td class="text-center">{{#label}}<span class="badge" style="background-color:{{rgb}}; width:25px; border:1px solid {{border}}"><font color={{fg}}>{{label}}</font></span>{{/label}}</td>
  <td>{{fromTeam}}</td>
  <td>{{toTeam}}</td>
  <td>{{replyTo}}</td>
  <td class="pre-line">{{{text}}}</td>
</script>
<script type="text/javascript">
registerContestObjectTable("clarifications");

function clarificationsRefresh() {
	contest.clear();
	$.when(contest.loadClarifications(), contest.loadTeams(), contest.loadProblems()).done(function () {
        fillContestObjectTable("clarifications", contest.getClarifications());
    }).fail(function (result) {
    	console.log("Error loading clarifications: " + result);
    })
}

$(document).ready(function () {
	clarificationsRefresh();
})

function submitClarification(to_team, text) {
	var obj = { to_team_id: to_team, text: text };
	console.log(obj);
	console.log(JSON.stringify(obj));
	contest.postClarification(JSON.stringify(obj), function(body) {
		$('#object-status').text("Posted successfully: " + body);
	}, function(result) {
		$('#object-status').text("Post failed: " + result.responseText);
	})
}
</script>
<%@ include file="layout/footer.jsp" %>