<%@page import="org.icpc.tools.contest.model.IContest" %>
<%@page import="org.icpc.tools.contest.model.IState" %>
<%@page import="org.icpc.tools.contest.model.internal.State" %>
<%@page import="org.icpc.tools.contest.model.ContestUtil" %>
<%@page import="org.icpc.tools.cds.CDSConfig" %>
<%@page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@page import="org.icpc.tools.cds.ConfiguredContest.Mode" %>
<%@page import="org.icpc.tools.cds.util.Role" %>
<% request.setAttribute("title", "Contest Data Server"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/baseMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Contest Data Server</h1>

            <% ConfiguredContest[] ccs = CDSConfig.getContests();

                for (ConfiguredContest cc : ccs) { %>
            <% if (!cc.isHidden() || Role.isAdmin(request) || Role.isBlue(request)) {
                IContest contest = cc.getContest();
                try {
                    IState state = contest.getState();
                    if (state == null)
                        state = new State();
                    String headerClass = "bg-warning text-white";
                    if (state.getEnded() != null) {
                        headerClass = "";
                    } else if (state.getFrozen() != null) {
                        headerClass = "bg-info text-white";
                    } else if (state.getStarted() != null) {
                        headerClass = "bg-success text-white";
                    }
            %>
            <div class="card mb-4">
                <%
                    String webRoot = "/contests/" + cc.getId();
                    String apiRoot = "/api/contests/" + cc.getId(); %>
                <div class="card-header <%= headerClass %>"><%= contest.getFormalName() %> (<%= cc.getId() %>)</div>

                <div class="card-body">
                    <table class="table table-sm table-hover table-striped table-fullwidth">
                        <thead>
                        <tr>
                            <td>
                                <% if (state.getStarted() == null) {
                                    if (contest.getStartStatus() == null) { %>Not scheduled
                                <% } else if (contest.getStartStatus() < 0) {%>Countdown paused
                                <% } else { %>Scheduled<% } %>
                                <% } else if (state.getEnded() != null) { %>Finished
                                <% } else { %>
                                <div class="progress" style="height: 20px;">
                                    <div class="progress-bar progress-bar-striped progress-bar-animated"
                                         style="width:<%= Math.max(0, Math.min(99,(long)((System.currentTimeMillis() - state.getStarted()) * contest.getTimeMultiplier()) * 100 / contest.getDuration())) %>%;">
                                        <%= ContestUtil.formatTime((long) ((System.currentTimeMillis() - state.getStarted()) * contest.getTimeMultiplier())) %>
                                    </div>
                                </div>
                                <% } %></td>
                            <td colspan="3"><%= ContestUtil.formatDuration(contest.getDuration()) %>
                                starting at <%= ContestUtil.formatStartTime(contest) %>
                            </td>
                            <td class="text-right">
                                <% if (cc.getMode() == Mode.ARCHIVE) { %>Archive
                                <% } else if (cc.getMode() == Mode.LIVE) { %>Live
                                <% } else { %>Playback (<%= cc.getTest().getMultiplier() %>x)<% } %></td>
                        </tr>
                        </thead>
                        <tbody>
                        <tr class="trcontest">
                            <td><b>Services:</b></td>
                            <td><a href="<%= apiRoot %>/">REST API root</a></td>
                            <td><a href="<%= apiRoot %>/event-feed">Event feed</a></td>
                            <td><a href="<%= apiRoot %>/scoreboard">JSON scoreboard</a></td>
                            <td></td>
                        </tr>

                        <tr class="trcontest">
                            <td><b>Admin:</b></td>
                            <td><a href="<%= webRoot %>">Overview</a></td>
                            <td><a href="<%= webRoot %>/details">Details</a></td>
                            <td><a href="<%= webRoot %>/orgs">Organizations</a></td>
                            <td><a href="<%= webRoot %>/teams">Teams</a></td>
                        </tr>
                        <tr class="trcontest">
                            <td></td>
                            <td><a href="<%= webRoot %>/scoreboard">Scoreboard</a></td>
                            <td><a href="<%= webRoot %>/countdown">Countdown</a></td>
                            <td><a href="<%= webRoot %>/video/status">Video</a></td>
                            <td></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <% } catch (Exception e) { %>
                <div class="card-header">
                    Error loading contest <%= cc.getId() %>
                </div>
                <% } %>
            </div>
            <% }
            } %>


            <h2>Administration</h2>

            <table class="table table-sm table-hover table-striped mb-4">
                <tbody>
                <tr>
                    <td>Video</td>
                    <td><a href="/video/control/1">Channel 1</a></td>
                    <td><a href="/video/control/2">Channel 2</a></td>
                    <td><a href="/video/control/3">Channel 3</a></td>
                </tr>
                </tbody>
            </table>

            <div class="mb-4">
                <h3><a href="<%= webroot %>/presentation/admin/web">Presentation Admin</a></h3>

                <h3><a href="<%= webroot %>/search">Search</a></h3>
            </div>

            <h3>Links</h3>

            <p>For up to date information on the services provided, please see the
                <a href="https://clics.ecs.baylor.edu/index.php/CDS" target="_blank">CDS documentation</a>.</p>

            <table class="table table-sm table-hover table-striped mb-4">
                <tbody>
                <tr>
                    <td>ICPC Tools</td>
                    <td><a href="https://icpc.baylor.edu/icpctools"
                           target="_blank">https://icpc.baylor.edu/icpctools</a></td>
                </tr>
                <tr>
                    <td>Github</td>
                    <td>
                        <a href="https://github.com/icpctools/icpctools" target="_blank">https://github.com/icpctools/icpctools</a>
                    </td>
                </tr>
                <tr>
                    <td>Latest builds</td>
                    <td>
                        <a href="https://gitlab.com/icpctools/icpctools/pipelines" target="_blank">https://gitlab.com/icpctools/icpctools/pipelines</a>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
</body>
</html>