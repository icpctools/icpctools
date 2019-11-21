<%@page import="org.icpc.tools.contest.model.*" %>
<%@page import="java.util.List" %>
<% request.setAttribute("title", "Submissions"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/contestMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1><a href="<%= apiRoot %>/submissions">Submissions</a> (<%= contest.getNumSubmissions() %>)</h1>
            <h1><a href="<%= apiRoot %>/judgements">Judgements</a> (<%= contest.getNumJudgements() %>)</h1>
            <h1><a href="<%= apiRoot %>/runs">Runs</a> (<%= contest.getNumRuns() %>)</h1>

            <h3>Judge Queue</h3>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th class="text-center">Time</th>
                    <th>Problem</th>
                    <th>Language</th>
                    <th>Team</th>
                    <th>Organization</th>
                </tr>
                </thead>
                <tbody>

                <% ISubmission[] subs = contest.getSubmissions();
                    int numJudging = 0;
                    for (int i = 0; i < subs.length; i++) {
                        String id = subs[i].getTeamId();
                        String teamStr = "";
                        String orgStr = "";
                        if (id != null) {
                            ITeam team = contest.getTeamById(id);
                            if (team != null) {
                                teamStr = id + ": " + team.getActualDisplayName();
                                IOrganization org = contest.getOrganizationById(team.getOrganizationId());
                                if (org != null)
                                    orgStr = org.getName();
                            }
                        }

                        id = subs[i].getId();
                        boolean judged = false;
                        if (id != null) {
                            IJudgement[] jud = contest.getJudgementsBySubmissionId(id);
                            if (jud != null) {
                                for (IJudgement j : jud) {
                                    if (j.getJudgementTypeId() != null)
                                        judged = true;
                                }
                            }
                        }
                        if (judged)
                            continue;
                        numJudging++;

                        String langStr = "";
                        id = subs[i].getLanguageId();
                        if (id != null) {
                            ILanguage lang = contest.getLanguageById(id);
                            if (lang != null)
                                langStr = lang.getName();
                        }

                        String probStr = "";
                        id = subs[i].getProblemId();
                        if (id != null) {
                            IProblem prob = contest.getProblemById(id);
                            if (prob != null)
                                probStr = id + " (" + prob.getLabel() + ")";
                        }
                %>
                <tr>
                    <td><a href="<%= apiRoot %>/submissions/<%= subs[i].getId() %>">
                        <%= subs[i].getId() %>
                    </a></td>
                    <td class="text-center">
                        <%= ContestUtil.formatTime(subs[i].getContestTime()) %>
                    </td>
                    <td>
                        <%= probStr %>
                    </td>
                    <td>
                        <%= langStr %>
                    </td>
                    <td>
                        <%= teamStr %>
                    </td>
                    <td>
                        <%= orgStr %>
                    </td>
                </tr>
                <% } %>
                </tbody>
                <tfoot>
                <tr>
                    <td colspan=6><%= numJudging %> pending judgements</td>
                </tr>
                </tfoot>
            </table>

            <h3>All Submissions</h3>

            <table class="table table-sm table-hover table-striped">
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
                <% for (ISubmission sub : subs) {
                    String id = sub.getTeamId();
                    String teamStr = "";
                    String orgStr = "";
                    if (id != null) {
                        ITeam team = contest.getTeamById(id);
                        if (team != null) {
                            teamStr = id + ": " + team.getActualDisplayName();
                            IOrganization org = contest.getOrganizationById(team.getOrganizationId());
                            if (org != null)
                                orgStr = org.getName();
                        } else
                            teamStr = "<font color=\"red\">" + id + "</font>";
                    }

                    id = sub.getId();
                    String judgeStr = "";
                    String judgeClass = "";
                    if (id != null) {
                        IJudgement[] jud = contest.getJudgementsBySubmissionId(id);
                        if (jud != null) {
                            for (IJudgement j : jud) {
                                IJudgementType jt = contest.getJudgementTypeById(j.getJudgementTypeId());
                                if (jt != null) {
                                    judgeStr += jt.getName();
                                    if (contest.isFirstToSolve(sub))
                                        judgeClass = "bg-success";
                                    else if (jt.isSolved())
                                        judgeClass = "table-success";
                                    else if (jt.isPenalty())
                                        judgeClass = "table-danger";
                                } else {
                                    judgeClass = "table-warning";
                                    judgeStr += "...";
                                }
                                judgeStr += " (<a href=\"" + apiRoot + "/judgements/" + j.getId() + "\">" + j.getId() + "</a>) ";
                       /*IRun[] runs = contest.getRunsByJudgementId(j.getId());
                       if (runs != null) {
                          //judgeStr += runs.length;
                          for (IRun r : runs) {
                             judgeStr += "<a href=\""+ apiRoot + "/runs/" + r.getId() + "\">" +r.getId() + "</a> ";
                          }
                       }
                       judgeStr += "]";*/
                            }
                        }
                    }

                    String langStr = "";
                    id = sub.getLanguageId();
                    if (id != null) {
                        ILanguage lang = contest.getLanguageById(id);
                        if (lang != null)
                            langStr = lang.getName();
                        else
                            langStr = "<span class=\"text-danger\">" + id + "</span>";
                    }

                    String probStr = "";
                    id = sub.getProblemId();
                    if (id != null) {
                        IProblem prob = contest.getProblemById(id);
                        if (prob != null)
                            probStr = id + " (" + prob.getLabel() + ")";
                        else
                            probStr = "<span class=\"text-danger\">" + id + "</span>";
                    }

                    List<String> valList = sub.validate(contest);
                    String val = null;
                    if (valList != null && !valList.isEmpty()) {
                        val = "";
                        for (String s : valList)
                            val += s + "\n";
                    } %>
                <tr>
                    <td>
                        <a href="<%= apiRoot %>/submissions/<%= sub.getId() %>">
                            <%= sub.getId() %>
                        </a>
                        <% if (val != null) { %>
                        <span class="text-danger">
                            <%= val %>
                        </span>
                        <% } %>
                    </td>
                    <td class="text-center">
                        <%= ContestUtil.formatTime(sub.getContestTime()) %>
                    </td>
                    <td>
                        <%= probStr %>
                    </td>
                    <td>
                        <%= langStr %>
                    </td>
                    <td>
                        <%= teamStr %>
                    </td>
                    <td>
                        <%= orgStr %>
                    </td>
                    <td class="<%= judgeClass %>">
                        <%= judgeStr %>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
</body>
</html>