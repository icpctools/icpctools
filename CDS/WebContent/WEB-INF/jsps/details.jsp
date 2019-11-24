<%@ page import="java.util.List" %>
<%@ page import="org.icpc.tools.contest.model.*" %>
<% request.setAttribute("title", "Contest Details"); %>
<%@ include file="layout/head.jsp" %>
<% IState state = contest.getState(); %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">Contest</h3>
             <div class="card-tools">
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
            <div class="card-body p-0">

            <table class="table table-sm table-hover table-striped">
                <tbody>
                <tr>
                    <td><b>Name:</b></td>
                    <td><%= contest.getName() %></td>
                    <td><b>Start:</b></td>
                    <td><%= ContestUtil.formatStartTime(contest) %></td>
                </tr>
                <tr>
                    <td><b>Duration:</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getDuration()) %></td>
                    <td><b>Freeze duration:</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getFreezeDuration()) %></td>
                </tr>
                <tr>
                    <td class="align-middle"><b>Logo:</b></td>
                    <td class="table-dark" rowspan=2 id="logo"></td>
                    <td class="align-middle"><b>Banner:</b></td>
                    <td class="table-dark" rowspan=2 id="banner"></td>
                </tr>
                </tbody>
            </table>
            </div></div>
            
            <div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">State</h3>
             <div class="card-tools">
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/state'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
            <div class="card-body p-0">

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
            </div></div>
            
            <div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">Languages</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getLanguages().length %>" class="badge bg-primary"><%= contest.getLanguages().length %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/languages'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
            <div class="card-body p-0">
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
            </div></div>
            
            <div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">Judgement Types</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getJudgementTypes().length %>" class="badge bg-primary"><%= contest.getJudgementTypes().length %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/judgement-types'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
            <div class="card-body p-0">

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
            </div></div>

            <div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">Problems</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getNumProblems() %>" class="badge bg-primary"><%= contest.getNumProblems() %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/problems'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
            <div class="card-body p-0">
            
            <table class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Label</th>
                    <th>Name</th>
                    <th>Color</th>
                    <th>RGB</th>
                    <th></th>
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
                    <td>
                        <%= problem.getRGB() %>
                    </td>
                    <td>
                        <div class="circle" style="background-color: <%= problem.getRGB() %>"></div>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
            </div></div>
            
            <div class="card collapsed-card">
           <div class="card-header">
             <h3 class="card-title">Groups</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getGroups().length %>" class="badge bg-primary"><%= contest.getGroups().length %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/groups'">API</button>
               <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-plus"></i></button>
             </div>
           </div>
            <div class="card-body p-0">
            
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
            </div></div>
            
            <%@ include file="teams.jsp" %>
            
            <%@ include file="orgs.jsp" %>
            
            <%@ include file="clarifications.jsp" %>
            
            <%@ include file="awards.jsp" %>
            
            <div class="card">
           <div class="card-header">
             <h3 class="card-title">Submissions</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getNumSubmissions() %>" class="badge bg-primary"><%= contest.getNumSubmissions() %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/submissions'">API</button>
             </div>
           </div>
        </div><div class="card">
           <div class="card-header">
             <h3 class="card-title">Judgements</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getNumJudgements() %>" class="badge bg-primary"><%= contest.getNumJudgements() %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/judgements'">API</button>
             </div>
           </div>
        </div><div class="card">
           <div class="card-header">
             <h3 class="card-title">Runs</h3>
             <div class="card-tools">
               <span data-toggle="tooltip" title="<%= contest.getNumRuns() %>" class="badge bg-primary"><%= contest.getNumRuns() %></span>
               <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>/runs'">API</button>
             </div>
           </div>
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
<%@ include file="layout/footer.jsp" %>