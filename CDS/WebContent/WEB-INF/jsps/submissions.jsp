<%@page import="java.util.List" %>
<% request.setAttribute("title", "Submissions"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Judge Queue</h3>
                    <div class="card-tools">
                       <button id="queue-refresh" type="button" class="btn btn-tool" ><i class="fas fa-sync-alt"></i></button>
                       <span id="queue-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
                    </div>
                </div>
                <div class="card-body p-0">
                    <table id="queue-table" class="table table-sm table-hover table-striped table-head-fixed">
                        <thead>
                            <tr>
                                <th class="text-center">Time</th>
                                <th class="text-center">Problem</th>
                                <th class="text-center">Language</th>
                                <th>Team</th>
                                <th>Organization</th>
                                <th class="text-center">Judgement</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Submissions</h3>
                    <div class="card-tools">
                   	   <button id="submissions-refresh" type="button" class="btn btn-tool" ><i class="fas fa-sync-alt"></i></button>
                       <span id="submissions-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
                    </div>
                </div>
                <div class="card-body p-0">
                    <table id="submissions-table" class="table table-sm table-hover table-striped table-head-fixed">
                        <thead>
                            <tr>
                                <th class="text-center">Time</th>
                                <th class="text-center">Problem</th>
                                <th class="text-center">Language</th>
                                <th>Team</th>
                                <th>Organization</th>
                                <th class="text-center">Judgement</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<script type="text/html" id="queue-template">
  <td class="text-center">{{{time}}}</td>
  <td class="text-center"><span class="badge" style="background-color:{{rgb}}; width:25px; border:1px solid {{border}}"><font color={{fg}}>{{label}}</font></span></td>
  <td class="text-center">{{lang}}</td>
  <td>{{team}}</td>
  <td>{{org}}</td>
  <td align=center>{{{result}}}</td>
</script>
<script type="text/html" id="submissions-template">
  <td class="text-center">{{{time}}}</td>
  <td class="text-center"><span class="badge" style="background-color:{{rgb}}; width:25px; border:1px solid {{border}}"><font color={{fg}}>{{label}}</font></span></td>
  <td class="text-center">{{lang}}</td>
  <td>{{team}}</td>
  <td>{{org}}</td>
  <td align=center>{{{result}}}</td>
</script>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<script type="text/javascript">
contest = new Contest("/api", "<%= cc.getId() %>");

registerContestObjectTable("queue");
registerContestObjectTable("submissions");

function queueRefresh() {
	submissionsRefresh();
}

function submissionsRefresh() {
	contest.clear();
	$.when(contest.loadLanguages(), contest.loadOrganizations(), contest.loadTeams(), contest.loadProblems(), contest.loadSubmissions(), contest.loadJudgements(), contest.loadJudgementTypes()).done(function () {
    	var queue = [];
    	submissions = contest.getSubmissions();
        for (var i = 0; i < submissions.length; i++) {
           var judgements = findManyBySubmissionId(contest.getJudgements(), submissions[i].id);
           var hasJudgement = false;
           if (judgements != null) {
        	  for (var j = 0; j < judgements.length; j++) {
        		 if (judgements[j].judgement_type_id != null)
        			 hasJudgement = true;
        	  }
           }
           if (!hasJudgement)
              queue.push(submissions[i]);
        }
    	fillContestObjectTable("queue", queue);
    	fillContestObjectTable("submissions", submissions);
    }).fail(function (result) {
    	console.log("Error loading submissions: " + result);
    });
}

$(document).ready(function () {
	submissionsRefresh();
});
</script>
<%@ include file="layout/footer.jsp" %>