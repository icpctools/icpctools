<%@ page import="java.util.List" %>
<%@ page import="org.icpc.tools.contest.model.ContestUtil" %>
<%@ page import="org.icpc.tools.contest.model.IState" %>
<%@ page import="org.icpc.tools.cds.CDSConfig" %>
<% request.setAttribute("title", "Overview"); %>
<%@ include file="layout/head.jsp" %>
<% IState state = contest.getState();
    long[] metrics = cc.getMetrics(); %>
<div class="container-fluid">
    <div class="row">
        <div class="col-9">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Overview</h3>
                </div>
                <div class="card-body p-0">
                    <% String validation = "";
                List<String> validationList = contest.validate();
                if (validationList == null)
                    validation = "No errors";
                else if (validationList.size() < 20) {
                    for (String s : validationList)
                        validation += s + "<br/>";
                } else
                    validation = validationList.size() + " errors"; %>

                    <table class="table table-sm table-hover table-striped">
                        <tbody>
                            <tr>
                                <td><b>Start:</b></td>
                                <td><%= ContestUtil.formatStartTime(contest) %></td>
                                <td><b>Duration:</b></td>
                                <td><%= ContestUtil.formatDuration(contest.getDuration()) %></td>
                            </tr>
                            <tr>
                                <td><b>Current time:</b></td>
                                <td><% if (state.getStarted() == null) { %>Not started
                                    <% } else if (state.getEnded() != null) { %>Finished
                                    <% } else { %><%= ContestUtil.formatTime((long) ((System.currentTimeMillis() - state.getStarted()) * contest.getTimeMultiplier())) %><% } %>
                                </td>
                                <td><b>Freeze duration:</b></td>
                                <td><%= ContestUtil.formatDuration(contest.getFreezeDuration()) %></td>
                            </tr>
                            <tr>
                                <td><b>Validation:</b></td>
                                <td><a href="<%= webroot %>/validation"><%= validation %></a></td>
                                <td><b>Last event:</b></td>
                                <td><%= ContestUtil.formatDuration(contest.getContestTimeOfLastEvent()) %></td>
                            </tr>
                        </tbody>
                    </table>

                    <p class="indent">
                        Compare to:
                        <% ConfiguredContest[] ccs = CDSConfig.getContests();
                    for (ConfiguredContest cc2 : ccs)
                        if (!cc2.equals(cc)) { %>
                        <a href="<%= webroot%>/contestCompare/<%= cc2.getId() %>"><%= cc2.getId() %>
                        </a>&nbsp;&nbsp;
                        <% } %>
                        <a href="<%= webroot%>/contestCompare/compare2cds">CDS awards</a>
                    </p>

                    <p class="indent">Freeze details & verification: <a href="<%= webroot%>/freeze">here</a>.
                    </p>

                </div>
            </div>
        </div>

        <div class="col-3">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Contest API</h3>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover table-striped">
                        <tbody>
                            <tr>
                                <td><a href="<%= apiRoot %>/">REST API root</a></td>
                            </tr>
                            <tr>
                                <td><a href="<%= apiRoot %>/event-feed">Event feed</a></td>
                                </td>
                            </tr>
                            <tr>
                                <td><a href="<%= apiRoot %>/scoreboard">JSON scoreboard</a></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-8">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Configuration</h3>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover table-striped">
                        <tbody>
                            <tr>
                                <td><b>Contest location:</b></td>
                                <td><%= cc.getLocation() %>
                                </td>
                            </tr>
                            <tr>
                                <td><b>CCS:</b></td>
                                <td><%= cc.getCCS() %>
                                </td>
                            </tr>
                            <tr>
                                <td><b>Test:</b></td>
                                <td><%= cc.getTest() %>
                                </td>
                            </tr>
                            <tr>
                                <td><b>Video:</b></td>
                                <td><%= cc.getVideo() %>
                                </td>
                            </tr>
                            <tr>
                                <td><b>Validation:</b></td>
                                <td id="sourceValidation">-</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Clients</h3>
                </div>
                <div class="card-body p-0">
                    <% List<String> efClients = cc.getClients();
                if (efClients == null || efClients.isEmpty()) { %>
                    <ul>
                        <li>None</li>
                    </ul>
                    <% } else {%>
                    <ol>
                        <% for (String s : efClients) { %>
                        <li><%= s %>
                        </li>
                        <% } %>
                    </ol>
                    <% } %>
                </div>
            </div>
        </div>

        <div class="col-4">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Data Requests</h3>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover table-striped">
                        <tbody>
                            <tr>
                                <td>REST:</td>
                                <td><%= metrics[0] %>
                                </td>
                            </tr>
                            <tr>
                                <td>Feed:</td>
                                <td><%= metrics[1] %>
                                </td>
                            </tr>
                            <tr>
                                <td>Websocket:</td>
                                <td><%= metrics[2] %>
                                </td>
                            </tr>
                            <tr>
                                <td>Web:</td>
                                <td><%= metrics[3] %>
                                </td>
                            </tr>
                            <tr>
                                <td>Download:</td>
                                <td><%= metrics[4] %>
                                </td>
                            </tr>
                            <tr>
                                <td>Scoreboard:</td>
                                <td><%= metrics[5] %>
                                </td>
                            </tr>
                            <tr>
                                <td>Desktop:</td>
                                <td><%= metrics[7] %>
                                </td>
                            </tr>
                            <tr>
                                <td>Webcam:</td>
                                <td><%= metrics[8] %>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
</div>
<script>
    function validateSource() {
        var id = "sourceValidation";
        document.getElementById(id).innerHTML = "?";

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200)
                    document.getElementById(id).innerHTML = xmlhttp.responseText;
                else
                    document.getElementById(id).innerHTML = "<font color='red'>FAIL</font>";
            }
        };

        xmlhttp.open("GET", "<%= webroot %>/validate");
        xmlhttp.send();
    }

    $(document).ready(validateSource);
</script>
<%@ include file="layout/footer.jsp" %>