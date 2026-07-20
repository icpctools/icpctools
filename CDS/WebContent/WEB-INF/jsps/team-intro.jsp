<% request.setAttribute("title", "Team Intro"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Team Intro</h3>
                </div>
   
    <div class="card-body">
    	<p>Set the team for the team detail presentation.</p>
    	<form id="team-detail-form">
        <div class="box-body">
            <div class="form-group">
                <label for="input-prefix" class="col-sm-4 control-label">Prefix (if multiple screens)</label>
                <div class="col-sm-12">
                    <input class="form-control" id="input-prefix" autocomplete="off" data-1p-ignore placeholder="e.g. 'left'">
                </div>
            </div>
            <div class="form-group">
                <label for="input-team-id" class="col-sm-4 control-label">Team Id or Label</label>
                <div class="col-sm-12">
                    <input class="form-control" id="input-team-id" autocomplete="off" data-1p-ignore placeholder="e.g. '57'">
                </div>
            </div>
        </div>
        <div class="box-footer">
          <button type="submit" class="btn btn-danger pull-right">Set</button>
          <span id="detail-status">&nbsp;</span>
        </div>
        </form>
    </div>
</div>
</div>
</div>
</div>
<script>
    $(function() {
        $('#team-detail-form').on('submit', function() {
            const $teamIdInput = $('#input-team-id')
            let cmd = $teamIdInput.val();

            const $prefixField = $('#input-prefix');
            if ($prefixField.val()) {
                cmd = $prefixField.val() + ':' + cmd;
            }

            console.log("Team detail: " + cmd);
            var xmlhttp = new XMLHttpRequest();
            xmlhttp.onreadystatechange = function () {
                if (xmlhttp.readyState == 4) {
                    if (xmlhttp.status == 200) {
                        document.getElementById("detail-status").innerHTML = "Success";
                    } else
                        document.getElementById("detail-status").innerHTML = xmlhttp.responseText;
                }
            };
            xmlhttp.open("PUT", "/presentation/admin/property/org.icpc.tools.presentation.contest.internal.presentations.TeamDetailPresentation=" + cmd, true);
            xmlhttp.send();

            // Clear the field again and set focus to keep going
            $teamIdInput.val('');
            $teamIdInput.focus();

            return false;
        });

        if (typeof contest !== 'undefined') {
            let teamMapping = [];
            contest.loadTeams().then(function(data) {
                for (let team of data) {
                    teamMapping[team.id] = team.display_name;
                }
            });

            $('#input-team-id').on('keyup', function (e) {
                // Ignore enter
                if (e.keyCode === 13) {
                    return;
                }
                const teamId = $(this).val();
                if (teamMapping[teamId]) {
                    const team = teamMapping[teamId];
                    $('#detail-status').text('Team: ' + team);
                } else {
                    $('#detail-status').text('Team not found');
                }
            })
        }
    });
</script>
