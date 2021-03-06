<%@ page import="java.util.List" %>
<%@ page import="org.icpc.tools.contest.model.*" %>
<%@ page import="org.icpc.tools.cds.util.HttpHelper" %>
<% request.setAttribute("title", "Details"); %>
<%@ include file="layout/head.jsp" %>
<% IState state = contest.getState(); %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<div class="container-fluid">
    <div class="row">
        <div class="col-9">
            <div id="accordion">
            <div class="card">
                <div class="card-header">
                    <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseContest">Contest</a></h4>
                    <div class="card-tools">
                        <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>'">API</button>
                    </div>
                </div>
                <div id="collapseContest" class="panel-collapse collapse in">
                <div class="card-body p-0">
                    <table class="table table-sm table-hover table-striped table-head-fixed">
                        <tbody>
                            <tr>
                                <td><b>Name:</b></td>
                                <td><%= HttpHelper.sanitizeHTML(contest.getName()) %></td>
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
                </div>
                </div>
            </div>
            </div>
        </div>
        <div class="col-3">
            <%@ include file="details/state.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-5">
            <%@ include file="details/languages.jsp" %>
        </div>
        <div class="col-7">
            <%@ include file="details/judgementTypes.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <%@ include file="details/groups.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <%@ include file="details/problems.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <%@ include file="details/teams.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <%@ include file="details/orgs.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <%@ include file="details/clarifications.jsp" %>
        </div>
    </div>
    <div class="row">
        <div class="col-12">
            <%@ include file="details/awards.jsp" %>
        </div>
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
</div>
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
            console.log("Error loading page: " + result);
        })
    })
</script>
<%@ include file="layout/footer.jsp" %>