<%@ page import="org.icpc.tools.contest.model.*" %>
<%@ page import="java.util.List" %>
<% request.setAttribute("title", "Team Summary"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/contestMenu.jsp" %>
<% String teamId = (String) request.getAttribute("teamId");
    ITeam team = contest.getTeamById(teamId);
    IStanding st = contest.getStanding(team);
    IOrganization organization = contest.getOrganizationById(team.getOrganizationId());
    IGroup[] groups2 = contest.getGroupsByIds(team.getGroupIds());
    String groupName = "";
    if (groups2 != null) {
        boolean first = true;
        for (IGroup group : groups2) {
            if (!first)
                groupName += ", ";
            groupName += group.getName();
            first = false;
        }
    } %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Team Summary</h1>

            <table class="table table-sm table-hover table-striped">
                <tr>
                    <td><b>Id:</b></td>
                    <td><%= team.getId() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Name:</b></td>
                    <td><%= team.getActualDisplayName() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Group:</b></td>
                    <td><%= groupName %>
                    </td>
                </tr>
                <% if (organization != null) { %>
                <tr>
                    <td><b>Org id:</b></td>
                    <td><%= organization.getId() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Org formal name:</b></td>
                    <td><%= organization.getFormalName() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Org name:</b></td>
                    <td><%= organization.getName() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Country:</b></td>
                    <td><%= organization.getCountry() %>
                    </td>
                </tr>
                <% } %>
            </table>

            <h3>Scoreboard</h3>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th class="text-right">Rank</th>
                    <% int numProblems = contest.getNumProblems(); %>
                    <% for (int j = 0; j < numProblems; j++) {
                        IProblem p = contest.getProblems()[j]; %>
                    <th class="text-center">
                        <%= p.getLabel() %>
                    </th>
                    <% } %>
                    <th class="text-right">Solved</th>
                    <th class="text-right">Time</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="text-right">
                        <%= st.getRank() %>
                    </td>

                    <% for (int j = 0; j < numProblems; j++) {
                        IResult r = contest.getResult(team, j);
                        String time = ContestUtil.getTime(r.getContestTime()); %>

                    <td
                            <% if (r.isFirstToSolve()) { %>
                            class="text-right bg-success"
                            <% } else if (r.getStatus() == Status.SOLVED) { %>
                            class="text-right table-success"
                            <% } else if (r.getStatus() == Status.FAILED) { %>
                            class="text-right table-danger"
                            <% } else if (r.getStatus() == Status.SUBMITTED) { %>
                            class="text-right table-warning"
                            <% } %>
                    ><% if (r.getNumSubmissions() > 0) { %><%= r.getNumSubmissions() + " / " + time %><% } %></td>
                    <% } %>

                    <td class="text-right">
                        <%= st.getNumSolved() %>
                    </td>
                    <td class="text-right">
                        <%= st.getTime() %>
                    </td>
                </tr>
                </tbody>
            </table>

            <h3>Submissions</h3>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th class="text-center">Time</th>
                    <th>Problem</th>
                    <th>Language</th>
                    <th>Judgements</th>
                </tr>
                </thead>
                <tbody>
                <% ISubmission[] subs = contest.getSubmissions();
                    for (ISubmission sub : subs) {
                        String id = sub.getTeamId();
                        if (id.equals(teamId)) {
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
                                            if (jt.isSolved())
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
                                    langStr = "<font color=\"red\">" + id + "</font>";
                            }

                            String probStr = "";
                            id = sub.getProblemId();
                            if (id != null) {
                                IProblem prob = contest.getProblemById(id);
                                if (prob != null)
                                    probStr = id + " (" + prob.getLabel() + ")";
                                else
                                    probStr = "<font color=\"red\">" + id + "</font>";
                            }

                            List<String> valList = sub.validate(contest);
                            String val = null;
                            if (valList != null && !valList.isEmpty()) {
                                val = "";
                                for (String s : valList)
                                    val += s + "\n";
                            }
                %>
                <tr>
                    <td><a href="<%= apiRoot %>/submissions/<%= sub.getId() %>"><%= sub.getId() %>
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
                    <td class="<%= judgeClass %>">
                        <%= judgeStr %>
                    </td>
                </tr>
                <% }
                } %>
                </tbody>
            </table>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
</body>
</html>