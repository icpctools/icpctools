<%@ page import="java.util.List" %>
<%@ page import="org.icpc.tools.contest.model.*" %>
<%@ page import="org.icpc.tools.cds.util.HttpHelper" %>
<%@ page import="org.icpc.tools.cds.util.Role" %>
<% request.setAttribute("title", "Details"); %>
<%@ include file="layout/head.jsp" %>
<% IState state = contest.getState(); %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script type="text/javascript">
    $(document).ready(function () {
        contest.setContestId("<%= cc.getId() %>");
    });
</script>
<div class="container-fluid">
    <% if (Role.isAdmin(request)) { %>
    <div class="row">
        <div class="col-9">
            <%@ include file="details/contest-admin.jsp" %>
        </div>
        <div class="col-3">
            <%@ include file="details/state.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-5"><%@ include file="details/languages-admin.html" %></div>
        <div class="col-7"><%@ include file="details/judgementTypes-admin.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/groups-admin.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/problems-admin.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/teams-admin.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/orgs-admin.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/clarifications-admin.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/awards.jsp" %></div>
    </div>
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Submissions</h3>
                    <div class="card-tools">
                        <span data-toggle="tooltip" title="<%= contest.getNumSubmissions() %>"
                            class="badge bg-primary"><%= contest.getNumSubmissions() %></span>
                        <button type="button" class="btn btn-tool"
                            onclick="location.href='<%= apiRoot %>/submissions'">API</button>
                    </div>
                </div>
            </div>
         </div>
    </div>
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Judgements</h3>
                    <div class="card-tools">
                        <span data-toggle="tooltip" title="<%= contest.getNumJudgements() %>"
                            class="badge bg-primary"><%= contest.getNumJudgements() %></span>
                        <button type="button" class="btn btn-tool"
                            onclick="location.href='<%= apiRoot %>/judgements'">API</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Runs</h3>
                    <div class="card-tools">
                        <span data-toggle="tooltip" title="<%= contest.getNumRuns() %>"
                            class="badge bg-primary"><%= contest.getNumRuns() %></span>
                        <button type="button" class="btn btn-tool"
                            onclick="location.href='<%= apiRoot %>/runs'">API</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <% } else { %>
    <div class="row">
        <div class="col-7"><%@ include file="details/problems.html" %></div>
        <div class="col-5"><%@ include file="details/contest.jsp" %><%@ include file="details/languages.html" %></div>
    </div>
    <div class="row">
        <div class="col-7"><%@ include file="details/judgementTypes.html" %></div>
        <div class="col-5"><%@ include file="details/groups.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/teams.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/orgs.html" %></div>
    </div>
    <div class="row">
        <div class="col-12"><%@ include file="details/clarifications.html" %></div>
    </div>
    <% } %>
</div>
<script type="text/javascript">
    $(document).ready(function () {
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
            console.log("Error loading page: " + result);
        })
    })
</script>
<%@ include file="layout/footer.jsp" %>