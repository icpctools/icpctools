<% request.setAttribute("title", "Clarifications"); %>
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
			        <h4 class="card-title">Clarifications</h4>
			        <div class="card-tools">
			            <button id="clarifications-refresh" type="button" class="btn btn-tool" ><i class="fas fa-sync-alt"></i></button>
			            <span id="clarifications-count" title="?" class="badge bg-primary">?</span>
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
			                    <th>To Teams</th>
			                    <th>To Groups</th>
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
    <div class="row" id="submit-clar-ui" style="display: none">
        <div class="col-12">
			<div class="card">
			    <div class="card-header">
			        <h4 class="card-title">Submit Clarification</h4>
			        <div class="card-tools">
			        	<div id="clar-status"></div>
			        </div>
			    </div>
			    <div class="card-body">
			      <div class="form-group row">
                       <label class="col-sm-2 col-form-label">Problem</label>
                       <select id="problem-id" class="col-sm-3 custom-select">
                         <option id="*No Selection*">None</option>
                       </select>
                     </div>
                     <div class="form-group row">
                       <label class="col-sm-2 col-form-label">Clarification</label>  
			        <textarea id="text" class="col-sm-10 form-control" placeholder="Enter clarification..."></textarea>
			      </div>
			    </div>
			    <div class="card-footer">
                  <button type="submit" class="btn btn-primary" onclick="submitClarification()">Submit</button>
                </div>
			</div>
        </div>
    </div>
</div>
<script type="text/html" id="clarifications-template">
  <td><a href="{{api}}">{{id}}</a></td>
  <td class="text-center">{{{time}}}</td>
  <td class="text-center">{{#label}}<span class="badge" style="background-color:{{rgb}}; width:25px; border:1px solid {{border}}"><font color={{fg}}>{{label}}</font></span>{{/label}}</td>
  <td>{{#fromTeam}}{{#logo}}<img src="{{{logo}}}" width="20" height="20"/> {{/logo}}{{label}}: {{name}}{{/fromTeam}}</td>
  <td>{{#toTeams}}{{#array}}{{#logo}}<img src="{{{logo}}}" width="20" height="20"/> {{/logo}}{{label}}: {{name}}{{/array}}{{/toTeams}}</td>
  <td>{{#toGroups}}{{#array}}{{id}}: {{name}}{{/array}}{{/toGroups}}</td>
  <td>{{replyTo}}</td>
  <td class="pre-line">{{{text}}}</td>
</script>
<script type="text/javascript">
contest = new Contest("/api", "<%= cc.getId() %>");
registerContestObjectTable("clarifications");

function clarificationsRefresh() {
	contest.clear();
	$.when(contest.loadClarifications(), contest.loadTeams(), contest.loadGroups(), contest.loadOrganizations(), contest.loadProblems()).done(function () {
        fillContestObjectTable("clarifications", contest.getClarifications());
    }).fail(function (result) {
    	console.log("Error loading clarifications: " + result);
    })
}

$(document).ready(function () {
	clarificationsRefresh();
	
	$.when(contest.loadAccess()).done(function () {
        var access = contest.getAccess();
        if (access.capabilities.some(e => (e === 'team_clar' || e === 'admin_clar')))
	        $("#submit-clar-ui").show();
    })
})

function submitClarification() {
	var obj = { text: $('#text').val() };
	prob = $('#problem-id').children(":selected").attr("id");
	if (prob != '*No Selection*')
		obj.problem_id = $('#problem-id').children(":selected").attr("id");
	contest.postClarification(JSON.stringify(obj), function(body) {
		$('#clar-status').text("Posted successfully");
	}, function(result) {
		$('#clar-status').text("Post failed: " + result.responseText);
	});
}

updateContestClock(contest, "contest-time");
</script>
<%@ include file="layout/footer.jsp" %>