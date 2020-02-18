<% request.setAttribute("title", "Admin"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Countdown Control</h3>
           </div>
        <div class="card-body">
          <b><font size="+7"><span id="countdown">unknown</span></font></b><span id="bg-status">&nbsp;</span>
        
           <p>You cannot change time in the final 30s before a contest starts.</p>
                <button id="pause" class="btn btn-secondary" onclick="sendCommand('pause', 'pause')">Pause
                </button>
                <button id="resume" class="btn btn-secondary" onclick="sendCommand('resume', 'resume')">Resume
                </button>
                <button id="clear" class="btn btn-secondary" onclick="sendCommand('clear', 'clear')">Clear
                </button>

                <table class="table table-sm table-hover table-striped">
                    <tbody>
                    <tr>
                        <td>
                            <select id="timeSelect" class="custom-select">
                                <option value="0:00:01">1 second</option>
                                <option value="0:00:05">5 seconds</option>
                                <option value="0:00:15">15 seconds</option>
                                <option value="0:00:30">30 seconds</option>
                                <option value="0:01:00">1 minute</option>
                                <option value="0:05:00">5 minutes</option>
                                <option value="0:15:00">15 minutes</option>
                                <option value="0:30:00">30 minutes</option>
                                <option value="1:00:00">1 hour</option>
                                <option value="2:00:00">2 hours</option>
                            </select>
                        </td>
                        <td>
                            <button id="set" class="btn btn-secondary"
                                    onclick="var e = document.getElementById('timeSelect'); sendCommand('set', 'set: ' + e.options[e.selectedIndex].value)">
                                Set
                            </button>
                        </td>
                        <td>
                            <button id="add" class="btn btn-secondary"
                                    onclick="var e = document.getElementById('timeSelect'); sendCommand('add', 'add: ' + e.options[e.selectedIndex].value)">
                                Add
                            </button>
                        </td>
                        <td>
                            <button id="remove" class="btn btn-secondary"
                                    onclick="var e = document.getElementById('timeSelect'); sendCommand('remove', 'remove: ' + e.options[e.selectedIndex].value)">
                                Remove
                            </button>

                        </td>
                    </tr>
                    <tr>
                        <td>
                            <input type="text" id="timeSelect2" value="0:01:00" class="form-control"/>
                        </td>
                        <td>
                            <button id="set2" class="btn btn-secondary"
                                    onclick="var e = document.getElementById('timeSelect2'); sendCommand('set', 'set: ' + e.value)">
                                Set
                            </button>
                        </td>
                        <td>
                            <button id="add2" class="btn btn-secondary"
                                    onclick="var e = document.getElementById('timeSelect2'); sendCommand('add', 'add: ' + e.value)">
                                Add
                            </button>
                        </td>
                        <td>
                            <button id="remove3" class="btn btn-secondary"
                                    onclick="var e = document.getElementById('timeSelect2'); sendCommand('remove', 'remove: ' + e.value)">
                                Remove
                            </button>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <p/>
            <span id="status">&nbsp;</span>
          </div></div>
      </div>
      </div>
      <div class="row">
      <div class="col-9">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Contest Readiness</h3>
           </div>
        <div class="card-body p-0">
                <table class="table table-sm table-hover table-striped">
                    <tr>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s1" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '1')"/>
                                <label class="custom-control-label" for="s1">Security</label>
                            </div>
                        </td>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s4" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '4')"/>
                                <label class="custom-control-label" for="s4">Judges</label>
                            </div>
                        </td>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s7" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '7')"/>
                                <label class="custom-control-label" for="s7">Operations</label>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s2" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '2')"/>
                                <label class="custom-control-label" for="s2">Sysops</label>
                            </div>
                        </td>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s5" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '5')"/>
                                <label class="custom-control-label" for="s5">Network Control</label>
                            </div>
                        </td>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s8" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '8')"/>
                                <label class="custom-control-label" for="s8">Executive Director</label>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s3" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '3')"/>
                                <label class="custom-control-label" for="s3">Contest Control</label>
                            </div>
                        </td>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s6" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '6')"/>
                                <label class="custom-control-label" for="s6">Marshalls</label>
                            </div>
                        </td>
                        <td>
                            <div class="custom-control custom-checkbox">
                                <input type="checkbox" id="s9" class="custom-control-input"
                                       onclick="sendCountdownStatusCommand(this, '9')"/>
                                <label class="custom-control-label" for="s9">Contest Director</label>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <span id="ready-status">&nbsp;</span>
                        </td>
                        <td>
                            <span id="bg-ready-status">&nbsp;</span>
                        </td>
                    </tr>
                </table>
            </div>
            </div>
            
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Event Feed Reset</h3>
           </div>
        <div class="card-body">
          <p>If the contest data has changed in an incompatible way that makes past events invalid (e.g. if .tsv config files
          are manually changed after a contest has started), the event feed id can be reset to notify clients that they should
          throw out any cached information and reconnect.</p>
          <span id="reset-status">&nbsp;</span>
          <form>
            <div class="form-group">
            <button id="reset" class="btn btn-primary form-control" onclick="sendIdResetCommand()">
                Reset
            </button>
            </div>
            </form>
        </div>
        </div>
        </div>

        <div class="col-3">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Finalization</h3>
           </div>
        <div class="card-body">
          <form>
            <div class="form-group">
                <label for="bSelect">Value of b:</label>
                <select id="bSelect" class="custom-select form-control">
                    <option value="0">0</option>
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                </select>
                
                <p><span id="final-status">&nbsp;</span></p>
            
                <button id="finalize" class="btn btn-primary form-control"
                    onclick="var e = document.getElementById('bSelect'); sendFinalizeCommand('finalize', 'b:' + e.options[e.selectedIndex].value)">
                    Apply
                </button>
                <button id="finalize" class="btn btn-primary form-control"
                    onclick="sendFinalizeCommand('finalize', 'template')">
                    Template
                </button>
            </div>
            </form>
        </div>
        </div></div>
    </div>
</div>
<script>
    var targetTime = 50.0;

    function toString(seconds_left) {
        var days = parseInt(seconds_left / 86400);
        seconds_left = seconds_left % 86400;

        var hours = parseInt(seconds_left / 3600);
        seconds_left = seconds_left % 3600;

        var minutes = parseInt(seconds_left / 60);
        var seconds = parseInt(seconds_left % 60);

        var text = "";
        if (days > 0)
            text = days + "d ";

        if (hours < 10)
            text += "0" + hours;
        else
            text += hours;

        text += ":";
        if (minutes < 10)
            text += "0" + minutes;
        else
            text += minutes;

        text += ":";
        if (seconds < 10)
            text += "0" + seconds;
        else
            text += seconds;

        return text;
    }

    // update the tag with id "countdown" every 300ms
    setInterval(function () {
        var countdown = document.getElementById("countdown");
        if (targetTime == null || targetTime == "") {
            countdown.innerHTML = "undefined";
            return;
        } else if (targetTime < 0) {
            countdown.innerHTML = toString(-targetTime) + " (paused)";
            return;
        }
        // find the amount of "seconds" between now and target
        var current_date = new Date().getTime() / 1000.0;
        var seconds_left = (targetTime - current_date);

        if (seconds_left < 0) {
            countdown.innerHTML = "Contest is started";
        } else {
            countdown.innerHTML = toString(seconds_left);
        }
    }, 300);

    function sendCommand(id, command) {
        document.getElementById(id).disabled = true;

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            document.getElementById("status").innerHTML = "Sending request...";
            if (xmlhttp.readyState == 4) {
                var resp = xmlhttp.responseText;
                if (xmlhttp.status == 200) {
                    if (resp == null || resp.trim().length == 0)
                        targetTime = null;
                    else
                        targetTime = parseInt(resp) / 1000.0;
                    document.getElementById("status").innerHTML = "Request successful";
                } else
                    document.getElementById("status").innerHTML = resp;
                document.getElementById(id).disabled = false;
            }
        }
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("status").innerHTML = "Request timed out";
            document.getElementById(id).disabled = false;
        }
        xmlhttp.open("PUT", "<%= webroot %>/admin/time/" + command, true);
        xmlhttp.send();
    }

    function sendCountdownStatusCommand(checkbox, command) {
        document.getElementById(checkbox.id).disabled = true;

        var s = "";
        for (i = 1; i < command; i++)
            s += "-";

        if (checkbox.checked)
            s += "Y";
        else
            s += "N";

        for (i = command; i < 9; i++)
            s += "-";

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            document.getElementById("ready-status").innerHTML = "Sending request";
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200)
                	document.getElementById("ready-status").innerHTML = "Request successful";
                else
                    document.getElementById("ready-status").innerHTML = xmlhttp.responseText;
                document.getElementById(checkbox.id).disabled = false;
            }
        }
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("ready-status").innerHTML = "Request timed out";
            document.getElementById(checkbox.id).disabled = false;
        }
        xmlhttp.open("PUT", "<%= webroot %>/admin/status/" + s, true);
        xmlhttp.send();
    }

    function updateCountdown() {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
                var resp = xmlhttp.responseText;
                if (xmlhttp.status == 200) {
                    if (resp == null || resp.trim().length == 0)
                        targetTime = null;
                    else
                        targetTime = parseInt(resp) / 1000.0;
                    document.getElementById("bg-status").innerHTML = "";
                } else
                    document.getElementById("bg-status").innerHTML = "Error updating: " + resp;
            }
        }
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("bg-status").innerHTML = "Timed trying to update, may be offline";
        }
        xmlhttp.open("GET", "<%= webroot %>/admin/time", true);
        xmlhttp.send();
    }

    function updateCountdownStatus() {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    var s = xmlhttp.responseText;
                    for (i = 0; i < 9; i++) {
                        if (s.charAt(i) == 'Y')
                            document.getElementById("s" + (i + 1)).checked = true;
                        else
                            document.getElementById("s" + (i + 1)).checked = false;
                    }
                    document.getElementById("bg-ready-status").innerHTML = "";
                } else
                    document.getElementById("bg-ready-status").innerHTML = "Error updating: " + xmlhttp.responseText;
            }
        }
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("bg-ready-status").innerHTML = "Timed trying to update, may be offline";
        }
        xmlhttp.open("GET", "<%= webroot %>/admin/status", true);
        xmlhttp.send();
    }

    function sendFinalizeCommand(id, command) {
        document.getElementById(id).disabled = true;

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            document.getElementById("final-status").innerHTML = "Sending request...";
            if (xmlhttp.readyState == 4) {
                var resp = xmlhttp.responseText;
                if (xmlhttp.status == 200)
                    document.getElementById("final-status").innerHTML = "Request successful";
                else
                    document.getElementById("final-status").innerHTML = resp;
                document.getElementById(id).disabled = false;
            }
        };
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("final-status").innerHTML = "Request timed out";
            document.getElementById(id).disabled = false;
        };
        xmlhttp.open("PUT", "<%= webroot %>/admin/finalize/" + command, true);
        xmlhttp.send();
    }

    function sendIdResetCommand() {
    	var id = "reset";
        document.getElementById(id).disabled = true;

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            document.getElementById("reset-status").innerHTML = "Resetting...";
            if (xmlhttp.readyState == 4) {
                var resp = xmlhttp.responseText;
                if (xmlhttp.status == 200) {
                    if (resp == null || resp.trim().length == 0)
                        targetTime = null;
                    else
                        targetTime = parseInt(resp) / 1000.0;
                    document.getElementById("reset-status").innerHTML = "Event feed successfully reset";
                } else
                    document.getElementById("reset-status").innerHTML = resp;
                document.getElementById(id).disabled = false;
            }
        };
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("reset-status").innerHTML = "Request timed out";
            document.getElementById(id).disabled = false;
        };
        xmlhttp.open("PUT", "<%= webroot %>/admin/reset-feed/", true);
        xmlhttp.send();
    }

    function updateInBackground() {
        document.getElementById("bg_status").innerHTML = "Updating status...";
        updateCountdown();
        updateCountdownStatus();

        setInterval(updateCountdown, 5000);
        setInterval(updateCountdownStatus, 5000);
    }

    $(document).ready(updateInBackground);
</script>
<%@ include file="layout/footer.jsp" %>