<%@page import="org.icpc.tools.contest.model.feed.ContestSource.ConnectionState" %>
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
                    String headerClass = "bg-info";
                    if (state.getFinalized() != null) {
                        headerClass = "";
                    } else if (state.getEnded() != null)
                        headerClass = "bg-danger";
                    else if (state.getFrozen() != null) {
                        headerClass = "bg-warning";
                    } else if (state.getStarted() != null)
                        headerClass = "bg-success";
            %>
      <div class="card mb-4">
        <% String webRootH = "/contests/" + cch.getId();
           String apiRootH = "/api/contests/" + cch.getId(); %>
        <div class="card-header <%= headerClass %> default-text-color">
          <a href="<%= webRootH %>">
            <h2 class="card-title default-text-color"><%= contestH.getActualFormalName() != null ? HttpHelper.sanitizeHTML(contestH.getActualFormalName()) : "(unnamed contest)" %></h2>
          </a>
          <div class="card-tools"><a href="<%= apiRootH %>" class="default-text-color">/<%= cch.getId() %></a></div>
        </div>
        <div class="card-body <%= headerClass %>">
          <table class="table table-sm table-fullwidth">
            <thead>
              <tr>
                <% if (state.isRunning()) { %>
                <td colspan="2" width="98%">
                  <div class="progress">
                    <% String progressBg = "bg-warning";
                             if (state.isFrozen())
                            	progressBg = "bg-info"; %>
                    <div class="progress-bar <%= progressBg %>"
                      style="width: <%= Math.max(0, Math.min(99,(long)((System.currentTimeMillis() - state.getStarted()) * contestH.getTimeMultiplier()) * 100 / contestH.getDuration())) %>%;">
                      <%= ContestUtil.formatTime((long) ((System.currentTimeMillis() - state.getStarted()) * contestH.getTimeMultiplier())) %>
                    </div>
                  </div>
                </td>
                <td align="right"><%= ContestUtil.formatDuration(contestH.getDuration()) %></td>
              </tr>
              <% } %>
              <tr>
                <td colspan="3">
                  <% if (state.isRunning()) { %>
                  <% if (state.isFrozen()) { %><a class="default-text-color" href="<%= webRootH %>/freeze">Scoreboard frozen</a>. <% } %>
                  Started at <%= ContestUtil.formatStartTime(contestH) %>
                  <% } else if (state.getFinalized() != null) { %>
                  Finalized. Started at <%= ContestUtil.formatStartTime(contestH) %>
                  <% } else if (state.getEnded() != null) { %>
                  Finished. Started at <%= ContestUtil.formatStartTime(contestH) %>
                  <% } else {
                    Long startStatus = contestH.getStartStatus();
                    if (startStatus == null) { %>
                  No scheduled start time
                  <% } else if (startStatus > 0) { %>
                  Scheduled start at <%= ContestUtil.formatStartTime(contestH) %>
                  (<%= ContestUtil.formatTime(contestH.getStartTime() - System.currentTimeMillis()) %> from now)
                  <% } else { %>
                  Countdown - <%= ContestUtil.formatStartTime(contestH.getStartStatus()) %>
                  <% } %>
                  <% } %>
                  <span class="float-right">
                    <% if (cch.getMode() == Mode.ARCHIVE) { %>Archive
                    <% } else if (cch.getMode() == Mode.LIVE) { %>Live
                    <% } else { %>Playback (<%= cch.getTest().getMultiplier() %>x)<% } %>
                    <%= cch.getError() == null ? "" : " - <span class='text-danger'>" + cch.getError() + "</span>" %>
                  </span></td>
              </tr>
              <tr>
                <td colspan="3"><a href="<%= webRootH %>/details"
                    class="default-text-color"><%= contestH.getNumProblems() %> problems, <%= contestH.getNumTeams() %> teams</a>
                  <span class="float-right"><a href="<%= webRootH %>/scoreboard"
                      class="default-text-color">Scoreboard</a></span></td>
              </tr>
              <tr>
                <td colspan="3"><a href="<%= webRootH %>/submissions"
                    class="default-text-color"><%= contestH.getNumSubmissions() %> submissions</a>
                  <span class="float-right"><a href="<%= webRootH %>/admin" class="default-text-color">Admin</a></span>
                </td>
              </tr>
            </thead>
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