<%@page import="org.icpc.tools.contest.model.IContest" %>
<%@page import="org.icpc.tools.contest.model.IState" %>
<%@page import="org.icpc.tools.contest.model.internal.State" %>
<%@page import="org.icpc.tools.contest.model.ContestUtil" %>
<%@page import="org.icpc.tools.cds.CDSConfig" %>
<%@page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@page import="org.icpc.tools.cds.ConfiguredContest.Mode" %>
<%@page import="org.icpc.tools.cds.util.Role" %>
<% request.setAttribute("title", "Contests"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <% ConfiguredContest[] ccsh = CDSConfig.getContests();

                for (ConfiguredContest cch : ccsh) { %>
            <% if (!cch.isHidden() || Role.isAdmin(request) || Role.isBlue(request)) {
                IContest contestH = cch.getContest();
                try {
                    IState state = contestH.getState();
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
                    String webRootH = "/contests/" + cch.getId();
                    String apiRootH = "/api/contests/" + cch.getId(); %>
                <div class="card-header <%= headerClass %>"><%= contestH.getFormalName() %> (<%= cch.getId() %>)</div>

                <div class="card-body">
                    <table class="table table-sm table-hover table-striped table-fullwidth">
                        <thead>
                        <tr>
                            <td>
                                <% if (state.getStarted() == null) {
                                    if (contestH.getStartStatus() == null) { %>Not scheduled
                                <% } else if (contestH.getStartStatus() < 0) {%>Countdown paused
                                <% } else { %>Scheduled<% } %>
                                <% } else if (state.getEnded() != null) { %>Finished
                                <% } else { %>
                                <div class="progress" style="height: 20px;">
                                    <div class="progress-bar progress-bar-striped progress-bar-animated"
                                         style="width:<%= Math.max(0, Math.min(99,(long)((System.currentTimeMillis() - state.getStarted()) * contestH.getTimeMultiplier()) * 100 / contestH.getDuration())) %>%;">
                                        <%= ContestUtil.formatTime((long) ((System.currentTimeMillis() - state.getStarted()) * contestH.getTimeMultiplier())) %>
                                    </div>
                                </div>
                                <% } %></td>
                            <td colspan="3"><%= ContestUtil.formatDuration(contestH.getDuration()) %>
                                starting at <%= ContestUtil.formatStartTime(contestH) %>
                            </td>
                            <td class="text-right">
                                <% if (cch.getMode() == Mode.ARCHIVE) { %>Archive
                                <% } else if (cch.getMode() == Mode.LIVE) { %>Live
                                <% } else { %>Playback (<%= cch.getTest().getMultiplier() %>x)<% } %></td>
                        </tr>
                        </thead>
                        <tbody>
                        <tr class="trcontest">
                            <td><b>Services:</b></td>
                            <td><a href="<%= apiRootH %>/">REST API root</a></td>
                            <td><a href="<%= apiRootH %>/event-feed">Event feed</a></td>
                            <td><a href="<%= apiRootH %>/scoreboard">JSON scoreboard</a></td>
                            <td></td>
                        </tr>

                        <tr class="trcontest">
                            <td><b>Admin:</b></td>
                            <td><a href="<%= webRootH %>">Overview</a></td>
                            <td><a href="<%= webRootH %>/details">Details</a></td>
                            <td><a href="<%= webRootH %>/orgs">Organizations</a></td>
                            <td><a href="<%= webRootH %>/teams">Teams</a></td>
                        </tr>
                        <tr class="trcontest">
                            <td></td>
                            <td><a href="<%= webRootH %>/scoreboard">Scoreboard</a></td>
                            <td><a href="<%= webRootH %>/countdown">Countdown</a></td>
                            <td><a href="<%= webRootH %>/video/status">Video</a></td>
                            <td></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <% } catch (Exception e) { %>
                <div class="card-header">
                    Error loading contest <%= cch.getId() %>
                </div>
                <% } %>
            </div>
            <% }
            } %>
            </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>