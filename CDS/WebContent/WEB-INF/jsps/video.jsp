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
                                <td colspan=3>Current / Max Current / Total</td>
                                <td colspan=3>Current / Max Current / Total</td>
                                <td colspan=3>Current / Max Current / Total</td>
                            </tr>
                        </thead>
                        <tbody>
                    <% ITeam[] teams = contest.getTeams();
                    teams = Arrays.copyOf(teams, teams.length);
                    ContestUtil.sort(teams);
                    for (int i = 1; i <= numTeams; i++) {
                        ITeam t = teams[i - 1];
                        if (t != null) {
                            String tId = t.getId();
                            IOrganization org = contest.getOrganizationById(t.getOrganizationId());
                            String orgName = "";
                            if (org != null)
                                orgName = org.getActualFormalName(); %>
                            <tr>
                                <td><%= tId %>
                                </td>
                                <td><%= HttpHelper.sanitizeHTML(t.getActualDisplayName()) %>
                                </td>
                                <td><%= HttpHelper.sanitizeHTML(orgName) %>
                                </td>
                                <td id="desktop-<%= tId %>" class="text-center">-</td>
                                <td id="desktop-<%= tId %>m" class="text-center"></td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/video/desktop/<%= tId %>?reset=true');">Reset</a>
                                </td>
                                <td id="webcam-<%= tId %>" class="text-center">-</td>
                                <td id="webcam-<%= tId %>m" class="text-center"></td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/video/webcam/<%= tId %>?reset=true');">Reset</a>
                                </td>
                                <td id="audio-<%= tId %>" class="text-center">-</td>
                                <td id="audio-<%= tId %>m" class="text-center"></td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/video/audio/<%= tId %>?reset=true');">Reset</a>
                                </td>
                            </tr>
                            <% } else { %>
                            <tr>
                                <td>?</td>
                                <td>?</td>
                                <td>?</td>
                                <td id="desktop<%= i %>" class="text-center">-</td>
                                <td></td>
                                <td id="webcam<%= i %>" class="text-center">-</td>
                                <td></td>
                                <td id="audio<%= i %>" class="text-center">-</td>
                                <td></td>
                            </tr>
                            <% }
                        } %>
                        </tbody>
                        <tfoot>
                            <tr>
                                <td></td>
                                <td></td>
                                <td class="text-right">Total streams:</td>
                                <td id="desktopStreams" class="text-center">-</td>
                                <td></td>
                                <td id="webcamStreams" class="text-center">-</td>
                                <td></td>
                                <td id="audioStreams" class="text-center">-</td>
                                <td></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td></td>
                                <td class="text-right">Current clients:</td>
                                <td id="desktopCurrent" class="text-center">-</td>
                                <td><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/desktop?resetAll=true');">Reset
                                        all</a></td>
                                <td id="webcamCurrent" class="text-center">-</td>
                                <td><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/webcam?resetAll=true');">Reset
                                        all</a></td>
                                <td id="audioCurrent" class="text-center">-</td>
                                <td><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/audio?resetAll=true');">Reset
                                        all</a></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td></td>
                                <td class="text-right">Total clients:</td>
                                <td id="desktopTotal" class="text-center">-</td>
                                <td></td>
                                <td id="webcamTotal" class="text-center">-</td>
                                <td></td>
                                <td id="audioTotal" class="text-center">-</td>
                                <td></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td></td>
                                <td class="text-right">Total time:</td>
                                <td id="desktopTotalTime" class="text-center" colspan="2">-</td>
                                <td id="webcamTotalTime" class="text-center" colspan="2">-</td>
                                <td id="audioTotalTime" class="text-center" colspan="2">-</td>
                            </tr>
                            <tr>
                                <td></td>
                                <td></td>
                                <td class="text-right">Connection mode:</td>
                                <td id="desktopMode" class="text-center">-</td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/video/desktop?mode=eager');">Eager</a><br /><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/desktop?mode=lazy');">Lazy</a><br><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/desktop?mode=lazy_close');">Lazy
                                        close</a></td>
                                <td id="webcamMode" class="text-center">-</td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/video/webcam?mode=eager');">Eager</a><br /><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/webcam?mode=lazy');">Lazy</a><br><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/webcam?mode=lazy_close');">Lazy
                                        close</a></td>
                                <td id="audioMode" class="text-center">-</td>
                                <td>
                                    <a
                                        href="javascript:request('<%= request.getContextPath() %>/video/audio?mode=eager');">Eager</a><br /><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/audio?mode=lazy');">Lazy</a><br><a
                                        href="javascript:request('<%= request.getContextPath() %>/video/audio?mode=lazy_close');">Lazy
                                        close</a></td>
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
                    <p />

                    <p>
                        <a href="<%= webroot %>/video/map/desktop">Desktop map</a>
                        <br />
                        <a href="<%= webroot %>/video/map/webcam">Webcam map</a>
                        <br />
                        <a href="<%= webroot %>/video/map/audio">Audio map</a>
                    </p>

                    <h2>Status Key</h2>
                    <table class="table table-sm table-hover table-striped">
                        <tbody>
                            <tr>
                                <td class="table-secondary">Unknown</td>
                            </tr>
                            <tr>
                                <td class="table-success">Active</td>
                            </tr>
                            <tr>
                                <td class="table-danger">Failed</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<script>
    function updateStatus(base, st) {
        for (var i = 0; i < st.streams.length; i++) {
            var str = st.streams[i];
            var id = str.id;
            var d = document.getElementById(base + "-" + id);
            if (d == null)
                continue;

            var link = "<a href='<%= request.getContextPath() %>/video/" + base + "/" + id + "'>link</a>";
            d.innerHTML = str.current + " / " + str.max_current + " / " + str.total_listeners + "  " + link;
            document.getElementById(base + "-" + id + "m").innerHTML = str.mode;

            var stat = str.status;
            var cellClass = "table-info";
            if (stat == "ACTIVE")
                cellClass = "table-success";
            else if (stat == "FAILED")
                cellClass = "table-danger";
            else if (stat == "UNKNOWN")
                cellClass = "table-secondary";
            $(d).removeClass('table-info')
                .removeClass('table-success')
                .removeClass('table-danger')
                .removeClass('table-secondary')
                .addClass(cellClass);
        }
        document.getElementById(base + "Streams").innerHTML = st.streams.length;

        document.getElementById(base + "Current").innerHTML = st.current;
        //document.getElementById(base+"Max").innerHTML = st.max;
        document.getElementById(base + "Total").innerHTML = st.total_listeners;
        document.getElementById(base + "TotalTime").innerHTML = st.total_time;
        //document.getElementById(base+"Mode").innerHTML = st.mode;
    }

    function updateStatus2(st) {
        document.getElementById("totalStreams").innerHTML = st.streams.length;
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
                    updateStatus("desktop", JSON.parse(xmlhttp.responseText));
                }
            }
        };

        xmlhttp.open("GET", "<%= request.getContextPath() %>/video/desktop");
        xmlhttp.send();

        var xmlhttp2 = new XMLHttpRequest();
        xmlhttp2.onreadystatechange = function () {
            if (xmlhttp2.readyState == 4) {
                if (xmlhttp2.status == 200) {
                    updateStatus("webcam", JSON.parse(xmlhttp2.responseText));
                }
            }
        };

        xmlhttp2.open("GET", "<%= request.getContextPath() %>/video/webcam");
        xmlhttp2.send();
        
        var xmlhttp3 = new XMLHttpRequest();
        xmlhttp3.onreadystatechange = function () {
            if (xmlhttp3.readyState == 4) {
                if (xmlhttp3.status == 200) {
                    updateStatus("audio", JSON.parse(xmlhttp3.responseText));
                }
            }
        };

        xmlhttp3.open("GET", "<%= request.getContextPath() %>/video/audio");
        xmlhttp3.send();

        var xmlhttp4 = new XMLHttpRequest();
        xmlhttp4.onreadystatechange = function () {
            if (xmlhttp4.readyState == 4) {
                if (xmlhttp4.status == 200) {
                    updateStatus2(JSON.parse(xmlhttp4.responseText));
                }
            }
        };

        xmlhttp4.open("GET", "<%= request.getContextPath() %>/video");
        xmlhttp4.send();
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