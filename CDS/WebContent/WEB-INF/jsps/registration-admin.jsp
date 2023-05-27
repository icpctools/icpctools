<% request.setAttribute("title", "Registration"); %>
<%@ include file="layout/head.jsp" %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<div class="container-fluid">
    <div class="row">
        <div class="col-7">
            <div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapsePersons">Persons</a></h4>
			        <div class="card-tools">
			            <span id="persons-count" title="?" class="badge bg-primary">?</span>
			            <button id="persons-api" type="button" class="btn btn-tool">API</button>
			        </div>
			    </div>
			    <div id="collapsePersons" class="panel-collapse collapse in">
			    <div class="card-body p-0">
			        <table id="persons-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th>Id</th>
			                    <th>ICPC Id</th>
			                    <th>Name</th>
			                    <th>Title</th>
			                    <th>Email</th>
			                    <th>Sex</th>
			                    <th>Role</th>
			                    <th>Team Id</th>
			                </tr>
			            </thead>
			            <tbody></tbody>
			        </table>
			    </div>
			    </div>
			</div>
			</div>
		</div>
        <div class="col-5">
			<div id="accordion">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseAccounts">Accounts</a></h4>
			        <div class="card-tools">
			            <span id="accounts-count" title="?" class="badge bg-primary">?</span>
			            <button id="accounts-api" type="button" class="btn btn-tool">API</button>
			        </div>
			    </div>
			    <div id="collapseAccounts" class="panel-collapse collapse in">
			    <div class="card-body p-0">
			        <table id="accounts-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th>Id</th>
			                    <th>Username</th>
			                    <th>Name</th>
			                    <th>Type</th>
			                    <th>IP</th>
			                    <th>Team</th>
			                    <th>Person</th>
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
								<th></th>
								<th>Name</th>
								<th>Type</th>
								<th class="text-center">Hidden</th>
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
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Teams</h4>
			        <div class="card-tools">
			            <span id="teams-count" title="?" class="badge bg-primary">?</span>
			            <button id="teams-api" type="button" class="btn btn-tool">API</button>
			        </div>
			    </div>
			    <div class="card-body p-0">
			        <table id="teams-table" class="table table-sm table-hover table-striped table-head-fixed">
			            <thead>
			                <tr>
			                    <th>Id</th>
			                    <th class="text-center">Label</th>
			                    <th></th>
			                    <th>Name</th>
			                    <th>Organization</th>
			                    <th>Group</th>
			                    <th class="text-center">Hidden</th>
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
<script type="text/html" id="groups-template">
  <td><a href="{{api}}">{{id}}</td>
  <td>{{icpc_id}}</td>
  <td style="width: 20px;" class="text-center">{{#logo}}<img src="{{{logo}}}" width="20" height="20"/>{{/logo}}</td>
  <td>{{name}}</td>
  <td>{{type}}</td>
  <td class="text-center">{{#hidden}}<span class="badge badge-info"><i class="fas fa-eye-slash"></i></a>{{/hidden}}</td>
</script>
<script type="text/html" id="organizations-template">
  <td><a href="{{api}}">{{id}}</a></td>
  <td style="width: 20px;" class="text-center">{{#logo}}<img src="{{{logo}}}" width="20" height="20"/>{{/logo}}</td>
  <td>{{name}}</td>
  <td>{{formalName}}</td>
  <td>{{country}}{{#flag}} <img src="{{{flag}}}" width="20" height="20"/>{{/flag}}</td>
</script>
<script type="text/html" id="teams-template">
  <td><a href="{{api}}">{{id}}</a></td>
  <td class="text-center">{{label}}</td>
  <td style="width: 20px;" class="text-center">{{#logo}}<img src="{{{logo}}}" width="20" height="20"/>{{/logo}}</td>
  <td>{{name}}</td>
  <td>{{orgName}}</td>
  <td>{{groupNames}}</td>
  <td class="text-center">{{#hidden}}<span class="badge badge-info"><i class="fas fa-eye-slash"></i></a>{{/hidden}}</td>
  <td><a href="<%= webroot  %>/teamSummary/{{id}}">summary</a></td>
</script>
<script type="text/html" id="persons-template">
  <td><a href="{{api}}">{{id}}</td>
  <td>{{icpc_id}}</td>
  <td>{{name}}</td>
  <td>{{title}}</td>
  <td>{{email}}</td>
  <td>{{sex}}</td>
  <td>{{role}}</td>
  <td>{{team_id}}</td>
</script>
<script type="text/html" id="accounts-template">
  <td><a href="{{api}}">{{id}}</td>
  <td>{{username}}</td>
  <td>{{name}}</td>
  <td>{{type}}</td>
  <td>{{ip}}</td>
  <td>{{team_id}}</td>
  <td>{{person_id}}</td>
</script>
<script type="text/javascript">
contest = new Contest("/api", "<%= cc.getId() %>");

registerContestObjectTable("groups");
registerContestObjectTable("organizations");
registerContestObjectTable("teams");
registerContestObjectTable("persons");
registerContestObjectTable("accounts");

$(document).ready(function () {
    $.when(contest.loadTeams(), contest.loadOrganizations(), contest.loadGroups(), contest.loadPersons(), contest.loadAccounts()).done(function () {
        fillContestObjectTable("teams", contest.getTeams());
        fillContestObjectTable("groups", contest.getGroups());
        fillContestObjectTable("organizations", contest.getOrganizations());
        fillContestObjectTable("persons", contest.getPersons());
        fillContestObjectTable("accounts", contest.getAccounts());
    }).fail(function (result) {
    	console.log("Error loading registration: " + result);
    })
})

updateContestClock(contest, "contest-time");
</script>
<%@ include file="layout/footer.jsp" %>