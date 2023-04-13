<% request.setAttribute("title", "Registration"); %>
<%@ include file="layout/head.jsp" %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<div class="container-fluid">
    <div class="row">
        <div class="col-8">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Teams</h4>
			        <div class="card-tools">
			            <span id="teams-count" title="?" class="badge bg-primary">?</span>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="teams-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th class="text-center">Label</th>
			                    <th></th>
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
        <div class="col-4">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Groups</h4>
			        <div class="card-tools">
			            <span id="groups-count" title="?" class="badge bg-primary">?</span>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="groups-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			            	<tr>
			            		<th width=90%>Name</th>
			            		<th></th>
			            	</tr>
			            </thead>
			            <tbody></tbody>
			        </table>
			    </div>
			</div>
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Organizations</h4>
			        <div class="card-tools">
			            <span id="organizations-count" title="?" class="badge bg-primary">?</span>
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
<script type="text/html" id="teams-template">
  <td class="text-right">{{label}}</td>
  <td style="width: 20px;" class="text-center">{{#logo}}<img src="{{{logo}}}" width="20" height="20"/>{{/logo}}</td>
  <td>{{name}}</td>
  <td>{{orgName}}</td>
  <td>{{groupNames}}</td>
</script>
<script type="text/html" id="groups-template">
  <td>{{name}}</td>
  <td style="width: 20px;" class="text-center">{{#logo}}<img src="{{{logo}}}" width="20" height="20"/>{{/logo}}</td>
</script>
<script type="text/html" id="organizations-template">
  <td style="width: 20px;" class="text-center">{{#logo}}<img src="{{{logo}}}" width="20" height="20"/>{{/logo}}</td>
  <td>{{#formalName}}{{formalName}} ({{name}}){{/formalName}}{{^formalName}}{{name}}{{/formalName}}</td>
  <td>{{country}}{{#flag}} <img src="{{{flag}}}" width="20" height="20"/>{{/flag}}</td>
</script>
<script type="text/javascript">
contest = new Contest("/api", "<%= cc.getId() %>");

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

updateContestClock(contest, "contest-time");
</script>
<%@ include file="layout/footer.jsp" %>