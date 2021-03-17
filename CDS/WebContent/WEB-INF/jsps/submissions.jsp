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
                       <span id="queue-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
                    </div>
                </div>
                <div class="card-body p-0">
                    <table id="queue-table" class="table table-sm table-hover table-striped table-head-fixed">
                        <thead>
                            <tr>
                                <th>Id</th>
                                <th class="text-center">Time</th>
                                <th>Problem</th>
                                <th>Language</th>
                                <th>Team</th>
                                <th>Organization</th>
                                <th>Judgements</th>
                            </tr>
                        </thead>
                        <tbody>
                           <tr>
                              <td colspan=7>
                                 <div class="spinner-border"></div>
                              </td>
                           </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Submissions</h3>
                    <div class="card-tools">
                       <span id="submissions-count" data-toggle="tooltip" title="?" class="badge bg-primary">?</span>
                    </div>
                </div>
                <div class="card-body p-0">
                    <table id="submissions-table" class="table table-sm table-hover table-striped table-head-fixed">
                        <thead>
                            <tr>
                                <th>Id</th>
                                <th class="text-center">Time</th>
                                <th>Problem</th>
                                <th>Language</th>
                                <th>Team</th>
                                <th>Organization</th>
                                <th>Judgements</th>
                            </tr>
                        </thead>
                        <tbody>
                           <tr>
                              <td colspan=7>
                                 <div class="spinner-border"></div>
                              </td>
                           </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script type="text/javascript">
    $(document).ready(function () {
    	contest.setContestURL("/api","<%= cc.getId() %>");

        function submissionTd(submission) {
        	var time = '';
        	var problem = '';
        	var lang = '';
        	var team = '';
        	var org = '';
        	var judge = '';
        	var judgeClass = '';
        	if (submission.contest_time != null)
                time = formatTime(parseTime(submission.contest_time));
        	if (submission.problem_id != null) {
                problem = findById(contest.getProblems(), submission.problem_id);
                if (problem != null)
                    problem = problem.label + ' (' + problem.id + ')';
            }
        	if (submission.language_id != null) {
                lang = findById(contest.getLanguages(), submission.language_id);
                if (lang != null)
                	lang = lang.name;
            }
        	if (submission.team_id != null) {
        		team = submission.team_id;
                var team2 = findById(contest.getTeams(), submission.team_id);
                if (team2.organization_id != null) {
                	org = findById(contest.getOrganizations(), team2.organization_id);
                    if (org != null)
                        org = org.name;
                }
                if (team2 != null)
                	team = team2.display_name;
                	if (team == null)
                		team = team2.name;
                	team = team2.id + ": " + team;
            }
        	var judgements = findManyBySubmissionId(contest.getJudgements(), submission.id);
        	if (judgements != null && judgements.length > 0) {
                var first = true;
                for (var j = 0; j < judgements.length; j++) {
                    if (!first)
                    	judge += ', ';
                    var jt = findById(contest.getJudgementTypes(), judgements[j].judgement_type_id);
                    if (jt != null) {
                        judge += jt.name;
                        if (jt.solved) {
                        	if (isFirstToSolve(contest,submission))
                        		judgeClass = "bg-success";
                        	else
                            	judgeClass = "table-success";
                        } else if (jt.penalty)
                            judgeClass = "table-danger";
                    } else {
                        judgeClass = "table-warning";
                        judge += "...";
                    }
                    judge += ' (<a href="' + contest.getURL('judgements', judgements[j].id) + '">' + judgements[j].id + '</a>)';
                    first = false;
                }
            }
            return $('<td><a href="<%= apiRoot %>/submissions/' + submission.id + '">' + submission.id + '</td><td>' + time + '</td><td>'
                + problem + '</td><td>' + lang + '</td><td>' + team + '</td><td>' + org + '</td><td class="' + judgeClass + '">' + judge + '</td>');
        }

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
        	fillContestObjectTable("queue", queue, submissionTd);
        	fillContestObjectTable("submissions", submissions, submissionTd);
        }).fail(function (result) {
        	console.log("Error loading groups: " + result);
        });
    });
</script>
<%@ include file="layout/footer.jsp" %>