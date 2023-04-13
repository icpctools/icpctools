<%@page import="org.icpc.tools.contest.model.ContestUtil" %>
<%@page import="org.icpc.tools.contest.model.IOrganization" %>
<%@page import="org.icpc.tools.contest.model.ITeam" %>
<%@page import="org.icpc.tools.cds.util.HttpHelper" %>
<%@page import="java.util.Arrays" %>
<% request.setAttribute("title", "Video"); %>
<%@ include file="layout/head.jsp" %>
<% int numTeams = contest.getNumTeams();%>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Video Status</h3>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover table-striped table-head-fixed">
                        <thead>
                            <tr>
                                <th>Id</th>
                                <th>Name</th>
                                <th>Organization</th>
                                <th colspan=3>Desktop</th>
                                <th colspan=3>Webcam</th>
                                <th colspan=3>Audio</th>
                            </tr>
                            <tr>
                                <td colspan=3></td>
                                <td colspan=3>Current - Max - Time</td>
                                <td colspan=3>Current - Max - Time</td>
                                <td colspan=3>Current - Max - Time</td>
                            </tr>
                        </thead>
                        <tbody>
                    <% ITeam[] teams = contest.getTeams();
                    teams = Arrays.copyOf(teams, teams.length);
                    ContestUtil.sort(teams);
                    for (int i = 1; i <= numTeams; i++) {
                        ITeam t = teams[i - 1];
                        if (t != null && ConfiguredContest.isTeamOrSpare(contest, t)) {
                            String tId = t.getId();
                            IOrganization org = contest.getOrganizationById(t.getOrganizationId());
                            String orgName = "";
                            if (org != null)
                                orgName = org.getName(); %>
                            <tr>
                                <td><%= t.getLabel() %>
                                </td>
                                <td><%= HttpHelper.sanitizeHTML(t.getActualDisplayName()) %>
                                </td>
                                <td><%= HttpHelper.sanitizeHTML(orgName) %>
                                </td>
                                <td id="desktop-<%= tId %>" class="text-center">-</td>
                                <td id="desktop-<%= tId %>m" class="text-center"></td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=desktop&team=<%= tId %>&action=reset');">Reset</a>
                                </td>
                                <td id="webcam-<%= tId %>" class="text-center">-</td>
                                <td id="webcam-<%= tId %>m" class="text-center"></td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=webcam&team=<%= tId %>&action=reset');">Reset</a>
                                </td>
                                <td id="audio-<%= tId %>" class="text-center">-</td>
                                <td id="audio-<%= tId %>m" class="text-center"></td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=audio&team=<%= tId %>&action=reset');">Reset</a>
                                </td>
                            </tr>
                            <% }
                        } %>
                        </tbody>
                        <tfoot>
                            <tr>
                                <td></td>
                                <td></td>
                                <td class="text-right">Total:</td>
                                <td id="desktopSummary" class="text-center">-</td>
                                <td id="desktopSummary2" class="text-center">-</td>
                                <td><a href="javascript:request('<%= request.getContextPath() %>/stream?type=desktop&action=reset');">Reset all</a></td>
                                <td id="webcamSummary" class="text-center">-</td>
                                <td id="webcamSummary2" class="text-center">-</td>
                                <td><a href="javascript:request('<%= request.getContextPath() %>/stream?type=webcam&action=reset');">Reset all</a></td>
                                <td id="audioSummary" class="text-center">-</td>
                                <td id="audioSummary2" class="text-center">-</td>
                                <td><a href="javascript:request('<%= request.getContextPath() %>/stream?type=audi&action=?reset');">Reset all</a></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td></td>
                                <td class="text-right">Connection mode:</td>
                                <td id="desktopMode" class="text-center">-</td>
                                <td class="text-center">
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=desktop&action=eager');">Eager</a><br /><a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=desktop&action=lazy');">Lazy</a><br><a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=desktop&action=lazy_close');">Lazy
                                        close</a></td>
                                <td></td>
                                <td id="webcamMode" class="text-center">-</td>
                                <td class="text-center">
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=webcam&action=eager');">Eager</a><br /><a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=webcam&actione=lazy');">Lazy</a><br><a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=webcam&action=lazy_close');">Lazy
                                        close</a></td>
                                <td></td>
                                <td id="audioMode" class="text-center">-</td>
                                <td class="text-center">
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=audio&action=eager');">Eager</a><br /><a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=audio&action=lazy');">Lazy</a><br><a
                                        href="javascript:request('<%= request.getContextPath() %>/stream?type=audio&action=lazy_close');">Lazy
                                        close</a></td>
                                <td></td>
                            </tr>
                        </tfoot>
                    </table>

                    <table>
                        <tr>
                            <td></td>
                            <td></td>
                            <td class="text-right">Total streams:</td>
                            <td id="totalStreams" class="text-center">-</td>
                        </tr>
                        <tr>
                            <td></td>
                            <td></td>
                            <td class="text-right">Current clients:</td>
                            <td id="totalCurrent" class="text-center">-</td>
                        </tr>
                        <tr>
                            <td></td>
                            <td></td>
                            <td class="text-right">Max concurrent:</td>
                            <td id="totalMax" class="text-center">-</td>
                        </tr>
                        <tr>
                            <td></td>
                            <td></td>
                            <td class="text-right">Total:</td>
                            <td id="total" class="text-center">-</td>
                        </tr>
                        <tr>
                            <td></td>
                            <td></td>
                            <td class="text-right">Total time:</td>
                            <td id="totalTime" class="text-center">-</td>
                        </tr>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<script>
function updateStreams(base, type) {
	var d = document.getElementById(base);
	if (d == null || type == null)
		return;

   	var link = "";
   	var streams = type.streams;
   	for (var j = 0; j < streams.length; j++) {
   		if (j > 0)
   			link += "&nbsp;";
   		var stream = streams[j];
		var id = stream.id;
   		var cl = "text-info";
   		if (stream.status == "active")
   			cl = "text-success";
   		else if (stream.status == "failed")
   			cl = "text-danger";
   		link += "<a href='/stream/" + id + "' class="+ cl+">" + id + "</a>";
   	}
  	d.innerHTML =link;
    
    d = document.getElementById(base + "m");
  	d.innerHTML = type.current + " - " + type.total_listeners + " - " + type.total_time;
}

function updateStreamType(base, st) {
	if (base == null || st == null)
		return;
	
	document.getElementById(base + "Summary").innerHTML = st.num_streams;
	document.getElementById(base + "Summary2").innerHTML = st.current + " - " + st.total_listeners + " - " + st.total_time;
    document.getElementById(base + "Mode").innerHTML = st.mode;
}

function updateStatus(st) {
    for (var i = 0; i < st.teams.length; i++) {
        var str = st.teams[i];
        var teamId = str.team_id;
        
        updateStreams("desktop-" + teamId, str.desktop);
        updateStreams("webcam-" + teamId, str.webcam);
        updateStreams("audio-" + teamId, str.audio);
    }
    updateStreamType("desktop", st.desktop);
    updateStreamType("webcam", st.webcam);
    updateStreamType("audio", st.audio);

    document.getElementById("totalStreams").innerHTML = st.num_streams;
    document.getElementById("totalCurrent").innerHTML = st.current;
    document.getElementById("totalMax").innerHTML = st.max_current;
    document.getElementById("total").innerHTML = st.total_listeners;
    document.getElementById("totalTime").innerHTML = st.total_time;
}

function verifyVideo() {
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                updateStatus(JSON.parse(xmlhttp.responseText));
            }
        }
    };

    xmlhttp.open("GET", "<%= webroot %>/video/status");
    xmlhttp.send();
}

function request(url) {
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                //window.location.reload(false);
                verifyVideo();
            } else {
            	console.log("Error checking video - " + xmlhttp.status + ": " + xmlhttp.responseText)
            }
        }
    };

    xmlhttp.open("GET", url);
    xmlhttp.send();
}

$(document).ready(verifyVideo);
</script>
<%@ include file="layout/footer.jsp" %>