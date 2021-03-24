<%@ page import="org.icpc.tools.contest.model.*" %>
<% request.setAttribute("title", "Registration"); %>
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
        <div class="col-8">
        	<div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Teams</h4>
			        <div class="card-tools">
			            <span id="teams-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="teams-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th colspan=2>#</th>
			                    <th>Name</th>
			                    <th>Organization</th>
			                    <th>Group</th>
			                </tr>
			            </thead>
			            <tbody></tbody>
			        </table>
			    </div>
			</div>
			</div>
		</div>
        <div class="col-4">
			<div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Groups</h4>
			        <div class="card-tools">
			            <span id="groups-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="groups-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			            	<tr>
			            		<th>Name</th>
			            	</tr>
			            </thead>
			            <tbody></tbody>
			        </table>
			    </div>
			</div>
			</div>
			<div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Organizations</h4>
			        <div class="card-tools">
			            <span id="organizations-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="organizations-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th></th>
			                    <th>Name</th>
			                    <th>Country</th>
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
<script type="text/html" id="teams-template">
  <td>{{id}}</td>
  <td style="width: 20px;" align=center>{{{logo}}}</td>
  <td>{{name}}</td>
  <td>{{orgName}}</td>
  <td>{{groupNames}}</td>
</script>
<script type="text/html" id="groups-th-template">
  <th>Name</th>
</script>
<script type="text/html" id="groups-template">
  <td>{{name}}</td>
</script>
<script type="text/html" id="organizations-template">
  <td style="width: 20px;" align=middle>{{{logo}}}</td>
  <td>{{formalName}}</td>
  <td>{{#country}}<img src="/countries/{{country}}.png" height=18/> {{country}}{{/country}}</td>
</script>
<script type="text/javascript">
registerContestObjectTable("teams");
registerContestObjectTable("groups");
registerContestObjectTable("organizations");

$(document).ready(function () {
    $.when(contest.loadTeams(), contest.loadOrganizations(), contest.loadGroups()).done(function () {
        fillContestObjectTable("teams", contest.getTeams());
        fillContestObjectTable("groups", contest.getGroups(), groupsTd);
        fillContestObjectTable("organizations", contest.getOrganizations());
    }).fail(function (result) {
    	console.log("Error loading teams: " + result);
    })
})
</script>
<%@ include file="layout/footer.jsp" %>