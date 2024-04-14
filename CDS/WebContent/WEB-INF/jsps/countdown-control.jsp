<div class="card">
    <div class="card-header">
        <h3 class="card-title">Countdown Control</h3>
        <div class="card-tools">
            <div class="btn-group-toggle" data-toggle="buttons">
                <label class="btn btn-secondary active fa fa-lock" id="locker">
                    <input id="lock" type="checkbox" autocomplete="off">Lock
                </label>
            </div>
        </div>
    </div>
    <div class="card-body" id="lock-group">
        <% if (multiCountdown) { %>
        <% for (IContest countdownContest : countdownContests) { %>
        <% boolean started = countdownContest.getState().getStarted() != null; %>
        <b>
            <font size="+7">
                <span class="btn-group-toggle" data-toggle="buttons">
                    <label class="btn <%= started ? "btn-danger" : "btn-success" %>" style="min-width: 130px;">
                        <input type="checkbox" autocomplete="off" <% if (!started) { %>checked<% } %>
                               data-countdown-contest="<%= countdownContest.getId() %>">
                        <span><%= started ? "Not included" : "Included" %></span>
                    </label>
                </span>
                <%= countdownContest.getName() %>:
                <span id="countdown-<%= countdownContest.getId() %>">&nbsp;</span>
            </font>
        </b>
        <span id="status-<%= countdownContest.getId() %>" class="ml-3">&nbsp;</span>
        <br/>
        <% } %>
        <% } else { %>
        <b><font size="+7"><span id="countdown-<%= countdownContests[0].getId() %>">&nbsp;</span></font></b>
        <span id="status-<%= countdownContests[0].getId() %>" class="ml-3">&nbsp;</span>
        <% } %>

        <% if (multiCountdown) { %>
        <p>Click the buttons above to determine which contests to include in your actions. By default all non-started
            contests are selected.</p>
        <% } %>
        <p>You cannot change time in the final 30s before a contest starts.</p>
        <button id="pause" class="btn btn-secondary" onclick="sendCommand('pause', 'pause')">Pause</button>
        <button id="resume" class="btn btn-secondary" onclick="sendCommand('resume', 'resume')">Resume</button>
        <button id="clear" class="btn btn-secondary" onclick="sendCommand('clear', 'clear')">Clear</button>

        <table class="table table-sm table-hover table-striped">
            <tbody>
            <tr>
                <td>
                    <select id="timeSelect" class="custom-select">
                        <option value="0:00:01">1 second</option>
                        <option value="0:00:05">5 seconds</option>
                        <option value="0:00:15">15 seconds</option>
                        <option value="0:00:30">30 seconds</option>
                        <option value="0:01:00" selected>1 minute</option>
                        <option value="0:05:00">5 minutes</option>
                        <option value="0:15:00">15 minutes</option>
                        <option value="0:30:00">30 minutes</option>
                        <option value="1:00:00">1 hour</option>
                        <option value="2:00:00">2 hours</option>
                    </select>
                </td>
                <td>
                    <button id="set" class="btn btn-secondary"
                            onclick="sendCommand('set', 'set: ' + $('#timeSelect').children('option:selected').val())">
                        Set
                    </button>
                </td>
                <td>
                    <button id="add" class="btn btn-secondary"
                            onclick="sendCommand('add', 'add: ' + $('#timeSelect').children('option:selected').val())">
                        Add
                    </button>
                </td>
                <td>
                    <button id="remove" class="btn btn-secondary"
                            onclick="sendCommand('remove', 'remove: ' + $('#timeSelect').children('option:selected').val())">
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
                            onclick="sendCommand('set', 'set: ' + $('#timeSelect2').val())">Set
                    </button>
                </td>
                <td>
                    <button id="add2" class="btn btn-secondary"
                            onclick="sendCommand('add', 'add: ' + $('#timeSelect2').val())">Add
                    </button>
                </td>
                <td>
                    <button id="remove2" class="btn btn-secondary"
                            onclick="sendCommand('remove', 'remove: ' + $('#timeSelect2').val())">Remove
                    </button>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
<script>
    var requestsDone = 0;

    function sendCommandForContest(id, contestId, command) {
        <% if (multiCountdown) { %>
        var $included = $('[data-countdown-contest="' + contestId + '"]');
        if (!$included.prop('checked')) {
            requestsDone++;
            if (requestsDone == <%= countdownContests.length %>) {
                document.getElementById(id).disabled = false;
            }
            return;
        }
        <% } %>

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            document.getElementById("status-" + contestId).innerHTML = "Sending request...";
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    // Reload the countdown info
                    contests[contestId].info = null;
                    document.getElementById("status-" + contestId).innerHTML = "Request successful";
                } else {
                    document.getElementById("status-" + contestId).innerHTML = xmlhttp.responseText;
                }
                requestsDone++;
                if (requestsDone == <%= countdownContests.length %>) {
                    document.getElementById(id).disabled = false;
                }
                <% if (!multiCountdown) { %>
                updateInfoStartStatus();
                <% } %>
            }
        }
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("status-" + contestId).innerHTML = "Request timed out";
            requestsDone++;
            if (requestsDone == <%= countdownContests.length %>) {
                document.getElementById(id).disabled = false;
            }
        }
        xmlhttp.open("PUT", "<%= request.getContextPath() %>/contests/" + contestId + "/admin/time/" + command, true);
        xmlhttp.send();
    }

    function sendCommand(id, command) {
        if ($("#locker").hasClass('btn-danger'))
            return;

        requestsDone = 0;

        document.getElementById(id).disabled = true;

        <% for (IContest countdownContest : countdownContests) { %>
        sendCommandForContest(id, "<%= countdownContest.getId() %>", command);
        <% } %>
    }

    var contests = {
        <% for (IContest countdownContest : countdownContests) { %>
        "<%= countdownContest.getId()%>": new Contest("/api", "<%= countdownContest.getId() %>"),
        <% } %>
    };

    $(function () {
        $('#lock').change(function () {
            if ($(this).prop('checked')) {
                $("#locker").removeClass('btn-secondary').addClass('btn-danger');
                $("#lock-group").find('*').addClass("disabled");
            } else {
                $("#locker").removeClass('btn-danger').addClass('btn-secondary');
                $("#lock-group").find('*').removeClass("disabled");
            }
        });

        $('[data-countdown-contest]').change(function () {
            var $button = $(this).closest('label');
            var $span = $button.find('span');
            if ($(this).prop('checked')) {
                $span.text('Included');
                $button.removeClass('btn-danger').addClass('btn-success');
            } else {
                $span.text('Not included');
                $button.removeClass('btn-success').addClass('btn-danger');
            }
        });

        <% for (IContest countdownContest : countdownContests) { %>
        updateContestClock(contests["<%= countdownContest.getId() %>"], "countdown-<%= countdownContest.getId() %>", true);
        <% } %>
    });
</script>