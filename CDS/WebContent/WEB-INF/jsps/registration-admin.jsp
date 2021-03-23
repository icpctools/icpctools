<%@ page import="org.icpc.tools.contest.model.*" %>
<% request.setAttribute("title", "Registration"); %>
<%@ include file="layout/head.jsp" %>
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
        <div class="col-5">
        	<div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseGroups">Groups</a></h4>
			        <div class="card-tools">
			            <span id="groups-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
			            <button id="groups-api" type="button" class="btn btn-tool">API</button>
			        </div>
			    </div>
			    <div id="collapseGroups" class="panel-collapse collapse in">
			    <div class="card-body p-0">
			        <table id="groups-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			            	<tr>
			            		<th>Id</th>
								<th>ICPC Id</th>
								<th>Name</th>
								<th>Type</th>
								<th>Hidden</th>
			            	</tr>
			            </thead>
			            <tbody></tbody>
			        </table>
			    </div>
			    </div>
			</div>
			</div>
		</div>
        <div class="col-7">
        	<div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseOrganizations">Organizations</a></h4>
			        <div class="card-tools">
			            <span id="organizations-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
			            <button id="organizations-api" type="button" class="btn btn-tool">API</button>
			        </div>
			    </div>
			    <div id="collapseOrganizations" class="panel-collapse collapse in">
			    <div class="card-body p-0">
			        <table id="organizations-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th>Id</th>
			                    <th></th>
			                    <th>Name</th>
			                    <th>Formal Name</th>
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
    <div class="row">
        <div class="col-12">
        <div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Teams</h4>
			        <div class="card-tools">
			            <span id="teams-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
			            <button id="teams-api" type="button" class="btn btn-tool">API</button>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="teams-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th>Id</th>
			                    <th>Name</th>
			                    <th colspan=2>Organization</th>
			                    <th>Organization (formal name)</th>
			                    <th>Group</th>
			                    <th>Summary</th>
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
<script type="text/html" id="groups-template">
  <td><a href="{{api}}">{{id}}</td>
  <td>{{icpc_id}}</td>
  <td>{{name}}</td>
  <td>{{type}}</td>
  <td align=center>{{#hidden}}<span class="badge badge-info"><i class="fas fa-eye-slash"></i></a>{{/hidden}}</td>
</script>
<script type="text/html" id="organizations-template">
  <td><a href="{{api}}">{{id}}</a></td>
  <td style="width: 20px;" align=middle>{{{logo}}}</td>
  <td>{{name}}</td>
  <td>{{formalName}}</td>
  <td>{{country}}</td>
</script>
<script type="text/html" id="teams-template">
  <td><a href="{{api}}">{{id}}</a></td>
  <td>{{name}}</td>
  <td style="width: 20px;" align=middle>{{{logo}}}</td>
  <td>{{orgName}}</td>
  <td>{{orgFormalName}}</td>
  <td>{{groupNames}}</td>
  <td><a href="<%= webroot  %>/teamSummary/{{id}}">summary</a></td>
</script>
<script type="text/javascript">
registerContestObjectTable("groups");
registerContestObjectTable("organizations");
registerContestObjectTable("teams");

$(document).ready(function () {
    $.when(contest.loadTeams(), contest.loadOrganizations(), contest.loadGroups()).done(function () {
        fillContestObjectTable("teams", contest.getTeams());
        fillContestObjectTable("groups", contest.getGroups());
        fillContestObjectTable("organizations", contest.getOrganizations());
    }).fail(function (result) {
    	console.log("Error loading teams: " + result);
    })
})
</script>
<%@ include file="layout/footer.jsp" %>