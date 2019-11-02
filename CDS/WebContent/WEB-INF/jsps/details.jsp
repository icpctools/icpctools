<%@ page import="java.util.List" %>
<%@ page import="org.icpc.tools.contest.model.*" %>
<% request.setAttribute("title", "Contest Details"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/menu.jsp" %>
<% IState state = contest.getState(); %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Contest Details</h1>

            <h3><a href="<%= apiRoot %>">Contest</a></h3>

            <table class="table table-sm table-hover table-striped">
                <tbody>
                <tr>
                    <td><b>Name:</b></td>
                    <td><%= contest.getName() %>
                    </td>
                    <td><b>Problems:</b></td>
                    <td><%= contest.getNumProblems() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Start:</b></td>
                    <td><%= ContestUtil.formatStartTime(contest) %>
                    </td>
                    <td><b>Organizations:</b></td>
                    <td><%= contest.getNumOrganizations() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Duration:</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getDuration()) %>
                    </td>
                    <td><b>Teams:</b></td>
                    <td><%= contest.getNumTeams() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Freeze duration:</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getFreezeDuration()) %>
                    </td>
                    <td><b>Submissions:</b></td>
                    <td><%= contest.getNumSubmissions() %>
                    </td>
                </tr>
                <tr>
                    <td><b>Last event:</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getContestTimeOfLastEvent()) %>
                    </td>
                    <td><b>Judgements:</b></td>
                    <td><%= contest.getNumJudgements() %>
                    </td>
                </tr>

                <% String validation = "";
                    List<String> validationList = contest.validate();
                    if (validationList == null)
                        validation = "No errors";
                    else if (validationList.size() < 20) {
                        for (String s : validationList)
                            validation += s + "<br/>";
                    } else
                        validation = validationList.size() + " errors"; %>
                <tr>
                    <td><b>Validation:</b></td>
                    <td><a href="<%= webroot %>/validation"><%= validation %>
                    </a></td>
                    <td></td>
                    <td></td>
                </tr>
                <tr>
                    <td class="align-middle"><b>Logo:</b></td>
                    <td class="table-dark" rowspan=2 id="logo"></td>
                    <td class="align-middle"><b>Banner:</b></td>
                    <td class="table-dark" rowspan=2 id="banner"></td>
                </tr>
                </tbody>
            </table>

            <h3><a href="<%= apiRoot %>/state">State</a></h3>

            <table class="table table-sm table-hover table-striped">
                <tbody>
                <tr>
                    <td><b>Started:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getStarted()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Frozen:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getFrozen()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Ended:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getEnded()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Finalized:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getFinalized()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Thawed:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getThawed()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>End of updates:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getEndOfUpdates()) %>
                    </td>
                </tr>
                </tbody>
            </table>

            <h3><a href="<%= apiRoot %>/languages">Languages</a> (<%= contest.getLanguages().length %>)</h3>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Name</th>
                </tr>
                </thead>
                <tbody>
                <% ILanguage[] languages = contest.getLanguages();
                    for (ILanguage language : languages) {
                        List<String> valList = language.validate(contest);
                        String val = null;
                        if (valList != null && !valList.isEmpty()) {
                            val = "";
                            for (String s : valList)
                                val += s + "\n";
                        } %>
                <tr>
                    <td><a href="<%= apiRoot %>/languages/<%= language.getId() %>"><%= language.getId() %>
                    </a>
                        <% if (val != null) { %>
                        <span class="text-danger"><%= val %>
                        </span>
                        <% } %>
                    </td>
                    <td><%= language.getName() %>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>


            <h3><a href="<%= apiRoot %>/judgement-types">Judgement Types</a> (<%= contest.getJudgementTypes().length %>)
            </h3>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Name</th>
                    <th>Penalty</th>
                    <th>Solved</th>
                </tr>
                </thead>
                <tbody>
                <% IJudgementType[] judgementTypes = contest.getJudgementTypes();
                    for (IJudgementType judgementType : judgementTypes) {
                        List<String> valList = judgementType.validate(contest);
                        String val = null;
                        if (valList != null && !valList.isEmpty()) {
                            val = "";
                            for (String s : valList)
                                val += s + "\n";
                        } %>
                <tr>
                    <td>
                        <a href="<%= apiRoot %>/judgement-types/<%= judgementType.getId() %>"><%= judgementType.getId() %>
                        </a>
                        <% if (val != null) { %>
                        <span class="text-danger"><%= val %>
                        </span>
                        <% } %>
                    </td>
                    <td><%= judgementType.getName() %>
                    </td>
                    <td><%= judgementType.isPenalty() + "" %>
                    </td>
                    <td><%= judgementType.isSolved() + "" %>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>


            <h3><a href="<%= apiRoot %>/problems">Problems</a> (<%= contest.getNumProblems() %>)</h3>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Label</th>
                    <th>Name</th>
                    <th>Color</th>
                    <th>RGB</th>
                </tr>
                </thead>
                <tbody>
                <% IProblem[] problems = contest.getProblems();
                    for (IProblem problem : problems) {
                        List<String> valList = problem.validate(contest);
                        String val = null;
                        if (valList != null && !valList.isEmpty()) {
                            val = "";
                            for (String s : valList)
                                val += s + "\n";
                        } %>
                <tr>
                    <td><a href="<%= apiRoot %>/problems/<%= problem.getId() %>"><%= problem.getId() %>
                    </a>
                        <% if (val != null) { %>
                        <span class="text-danger"><%= val %>
                        </span>
                        <% } %>
                    </td>
                    <td><%= problem.getLabel() %>
                    </td>
                    <td><%= problem.getName() %>
                    </td>
                    <td><%= problem.getColor() %>
                    </td>
                    <td><%= problem.getRGB() %>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>


            <h3><a href="<%= apiRoot %>/groups">Groups</a> (<%= contest.getGroups().length %>)</h3>

            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>ICPC Id</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Hidden</th>
                </tr>
                </thead>
                <tbody>
                <% IGroup[] groups = contest.getGroups();
                    for (IGroup group : groups) {
                        List<String> valList = group.validate(contest);
                        String val = null;
                        if (valList != null && !valList.isEmpty()) {
                            val = "";
                            for (String s : valList)
                                val += s + "\n";
                        }
                        String typ = "";
                        if (group.getGroupType() != null)
                            typ = group.getGroupType();
                        String hidden = "";
                        if (group.isHidden())
                            hidden = "true"; %>
                <tr>
                    <td><a href="<%= apiRoot %>/groups/<%= group.getId() %>"><%= group.getId() %>
                    </a>
                        <% if (val != null) { %>
                        <span class="text-danger"><%= val %>
                        </span>
                        <% } %>
                    </td>
                    <td><%= group.getICPCId() %>
                    </td>
                    <td><%= group.getName() %>
                    </td>
                    <td><%= typ %>
                    </td>
                    <td><%= hidden %>
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
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");

        function update() {
            var info = contest.getInfo();
            var logo = bestSquareLogo(info.logo, 50);
            console.log(info.name + " - " + info.logo + " -> " + logo);
            if (logo != null) {
                var elem = document.createElement("img");
                elem.setAttribute("src", "/api/" + logo.href);
                elem.setAttribute("height", "40");
                document.getElementById("logo").appendChild(elem);
            }
            var banner = bestLogo(info.banner, 100, 50);
            console.log(info.name + " - " + info.banner + " -> " + banner);
            if (banner != null) {
                var elem = document.createElement("img");
                elem.setAttribute("src", "/api/" + banner.href);
                elem.setAttribute("height", "40");
                document.getElementById("banner").appendChild(elem);
            }
        }

        $.when(contest.loadInfo()).done(function () {
            update()
        }).fail(function (result) {
            alert("Error loading page: " + result);
        })
    })
</script>
</body>
</html>