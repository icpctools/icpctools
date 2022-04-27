<% request.setAttribute("title", "Admin"); %>
<%@ include file="layout/head.jsp" %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/cds.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<div class="container-fluid">
    <div class="row">
        <div class="col-8">
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
          <b><font size="+7"><span id="countdown">unknown</span></font></b><span id="bg-status">&nbsp;</span>
        
           <p>You cannot change time in the final 30s before a contest starts.</p>
                <button id="pause" class="btn btn-secondary" onclick="sendCommand('pause', 'pause')">Pause</button>
                <button id="resume" class="btn btn-secondary" onclick="sendCommand('resume', 'resume')">Resume</button>
                <button id="clear" class="btn btn-secondary" onclick="sendCommand('clear', 'clear')">Clear</button>
                <span id="status">&nbsp;</span>

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
                            <button id="set" class="btn btn-secondary" onclick="sendCommand('set', 'set: ' + $('#timeSelect').children('option:selected').val())">Set</button>
                        </td>
                        <td>
                            <button id="add" class="btn btn-secondary" onclick="sendCommand('add', 'add: ' + $('#timeSelect').children('option:selected').val())">Add</button>
                        </td>
                        <td>
                            <button id="remove" class="btn btn-secondary" onclick="sendCommand('remove', 'remove: ' + $('#timeSelect').children('option:selected').val())">Remove</button>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <input type="text" id="timeSelect2" value="0:01:00" class="form-control"/>
                        </td>
                        <td>
                            <button id="set2" class="btn btn-secondary" onclick="sendCommand('set', 'set: ' + $('#timeSelect2').val())">Set</button>
                        </td>
                        <td>
                            <button id="add2" class="btn btn-secondary" onclick="sendCommand('add', 'add: ' + $('#timeSelect2').val())">Add</button>
                        </td>
                        <td>
                            <button id="remove3" class="btn btn-secondary" onclick="sendCommand('remove', 'remove: ' + $('#timeSelect2').val())">Remove</button>
                        </td>
                    </tr>
                    </tbody>
                </table>
          </div></div>
      </div>

      <div class="col-4">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Start Status</h3>
           </div>
        <div class="card-body p-0">
                <table id="start-status-table" class="table table-sm table-hover table-striped">
                  <thead>
                <tr>
                    <th>Label</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
            </tbody>
                </table>
                <div class="input-group margin">
               <input id="add-status" type="text" class="form-control input-sm"/>
               <span class="input-group-btn">
               <button type="button" class="btn btn-info btn-flat" onclick="addStartStatus($('#add-status').val(), 0)">Add</button>
               </span></div>
            </div>
            </div>
     </div></div>
     
     <div class="row">
        <div class="col-5">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Finalization</h3>
           </div>
        <div class="card-body">
          Finalize (signal end of updates for) the contest.
          <form class="form-inline">
            <div class="form-group">
                <label for="bSelect">B value</label>
                <select id="bSelect" class="form-control">
                    <option value="0">0</option>
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                </select>
                <button id="finalize" class="btn btn-info"
                    onclick="var e = document.getElementById('bSelect'); sendFinalizeCommand('finalize', 'b:' + e.options[e.selectedIndex].value)">Apply</button>
            </div>
          </form>
            <div class="form-group">
                <button id="finalize2" class="btn btn-info" onclick="sendFinalizeCommand('finalize2', 'template')">Apply from Template</button></div>
            <div class="form-group">
                <button id="finalize3" class="btn btn-info" onclick="sendFinalizeCommand('finalize3', 'eou')">End of Updates</button></div>
            <span id="final-status">&nbsp;</span>
        </div>
        </div></div>
        
        <div class="col-7">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Event Feed Reset</h3>
           </div>
        <div class="card-body">
            <div class="box-body">
            If the contest data has changed in an incompatible way that makes past events invalid (e.g. if .tsv config files
            are manually changed after a contest has started), the event feed id can be reset to notify clients that they should
            throw out any cached information and reconnect.</div>
            <div class="box-footer">
              <button id="reset" class="btn btn-danger pull-right" onclick="sendIdResetCommand()">Reset</button>
              <span id="reset-status">&nbsp;</span>
            </div>
        </div>
        </div>
        </div>
    </div>
    
    <div class="row"> 
    <div class="col-7">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Contest Object Control</h3>
           </div>
        <div class="card-body">
            <p>In case of emergency - e.g. a missed event from a CCS - this form can be used to manually add or remove objects from the contest
               event feed. Use very carefully and sparingly! Adding or updating requires all three inputs, removal only requires type and id.</p>
              <div class="box-body">
                <div class="form-group">
                  <label for="input-type" class="col-sm-2 control-label">Type</label>
                  <div class="col-sm-12">
                    <input class="form-control" id="input-type" placeholder="contest type, e.g. 'teams'">
                  </div>
                </div>
                <div class="form-group">
                  <label for="input-id" class="col-sm-2 control-label">Id</label>
                  <div class="col-sm-12">
                    <input class="form-control" id="input-id" placeholder="id">
                  </div>
                </div>
                <div class="form-group">
                  <label for="input-body" class="col-sm-2 control-label">Body</label>
                  <div class="col-sm-12">
                    <textarea class="form-control" rows="3" id="input-body" placeholder="JSON body"></textarea>
                  </div>
                </div>
              </div>
              <div class="box-footer">
                <button type="submit" class="btn btn-danger pull-right" onclick="postContestObject($('#input-type').val(),$('#input-body').val())">Post</button>
                <button type="submit" class="btn btn-danger pull-right" onclick="putContestObject($('#input-type').val(),$('#input-id').val(),$('#input-body').val())">Put</button>
                <button type="submit" class="btn btn-danger pull-right" onclick="patchContestObject($('#input-type').val(),$('#input-id').val(),$('#input-body').val())">Patch</button>
                <button type="submit" class="btn btn-danger pull-right" onclick="deleteContestObject($('#input-type').val(),$('#input-id').val())">Delete</button>
                <span id="object-status">&nbsp;</span>
              </div>
         </div>
         </div>
     </div>
     <div class="col-5">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Balloon Printout</h3>
           </div>
        <div class="card-body">
            <p>Click <a href="<%= webroot %>/balloon">Letter</a> or <a href="<%= webroot %>/balloonA4">A4</a> for a PDF containing balloon labels and colours. Print only the pages you need.</p>
        </div>
        </div>
     
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Resolver Info (Beta)</h3>
           </div>
        <div class="card-body">
          Initializing the resolver does three things:
          <ul>
            <li>Shows the status of the resolution</li>
            <li>Allows you to control the resolution (for extreme circumstances)</li>
            <li>Makes judgements public as they are resolved</li>
          </ul>
            <table id="resolver-table" class="table table-sm table-hover table-striped">
              <tbody>
                <tr>
                  <td>Current pause:</td><td id="resolver-current-pause">-</td>
                  <td>Total pauses:</td><td id="resolver-total-pauses">-</td>
                  <td>Stepping:</td><td id="resolver-stepping">-</td>
                </tr>
              </tbody>
            </table>
            <div class="form-group">
                <button class="btn btn-info" onclick="resolve('init')">Init</button>
                <button class="btn btn-info" onclick="resolve('reset')">Reset</button>
                <button class="btn btn-info" onclick="resolve('fast-rewind')">&lt;&lt;</button>
                <button class="btn btn-info" onclick="resolve('rewind')">&lt;</button>
                <button class="btn btn-info" onclick="resolve('forward')">&gt;</button>
                <button class="btn btn-info" onclick="resolve('fast-forward')">&gt;&gt;</button>
            </div>
        </div>
        </div></div>
    </div>
</div>
<script type="text/html" id="start-status-template">
  <td>{{{label}}}</td>
  <td><div class="btn-group">
    <button type="button" class="btn btn-sm btn-{{#a}}danger{{/a}}{{^a}}default{{/a}}" onclick="updateStartStatus('{{{id}}}',0)">No</button>
    <button type="button" class="btn btn-sm btn-{{#b}}warning{{/b}}{{^b}}default{{/b}}" onclick="updateStartStatus('{{{id}}}',1)">Unknown</button>
    <button type="button" class="btn btn-sm btn-{{#c}}success{{/c}}{{^c}}default{{/c}}" onclick="updateStartStatus('{{{id}}}',2)">Yes</button>
  </div>&nbsp; &nbsp;<button type="button" class="btn btn-sm btn-danger" onclick="removeStartStatus('{{{id}}}')">Remove</button></td>
</script>
<script>
    contest = new Contest("/api", "<%= cc.getId() %>");
	cds.setContestId("<%= cc.getId() %>");
    var targetTime = 0.0;

    // update the tag with id "countdown" every 300ms
    setInterval(function () {
        var countdown = document.getElementById("countdown");
        if (targetTime == null || targetTime == "") {
            countdown.innerHTML = "undefined";
            return;
        } else if (targetTime < 0) {
            countdown.innerHTML = formatContestTime(targetTime, true) + " (paused)";
            return;
        }
        // find the amount of "seconds" between now and target
        var now = new Date().getTime();
        var seconds_left = now - targetTime;

        if (seconds_left > 0)
            countdown.innerHTML = "Contest is started";
        else
            countdown.innerHTML = formatContestTime(seconds_left, true);
    }, 300);

    function sendCommand(id, command) {
    	if ($("#locker").hasClass('btn-danger'))
    		return;
    	
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
                        targetTime = parseInt(resp);
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

    function updateCountdown() {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
                var resp = xmlhttp.responseText;
                if (xmlhttp.status == 200) {
                    if (resp == null || resp.trim().length == 0)
                        targetTime = null;
                    else
                        targetTime = parseInt(resp);
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

    function addStartStatusDefaults() {
    	addStartStatus("Security", 0);
    	addStartStatus("Sysops", 0);
    	addStartStatus("Contest Control", 0);
    	addStartStatus("Judges", 0);
    	addStartStatus("Network Control", 0);
    	addStartStatus("Marshalls", 0);
    	addStartStatus("Operations", 0);
    	addStartStatus("Executive Director", 0);
    	addStartStatus("Contest Director", 0);
    }

    function addStartStatus(text, status) {
    	var id = text.replace(/[^a-zA-Z0-9_.-]+/g, '_');
    	cds.doPut("start-status", id, '{"id":"' + id + '","label":"' + text + '","status":"' + status + '"}', function() { updateStartStatusTable(); });
    }

    function updateStartStatus(id, status) {
    	cds.doPatch("start-status", id, '{"id":"' + id + '","status":"' + status + '"}', function() { updateStartStatusTable(); });
    }

    function removeStartStatus(id) {
    	cds.doDelete("start-status", id, function() { updateStartStatusTable(); });
    }

    function updateStartStatusTable() {
    	contest.clear();
    	$.when(contest.loadStartStatus()).done(function () {
            fillContestObjectTable("start-status", contest.getStartStatus());
            
            if (contest.getStartStatus().length == 0) {
              col = $('<td colspan="2"><button type="button" class="btn btn-sm btn-default" onclick="addStartStatusDefaults()">Add default statuses</button></td>');
              row = $('<tr></tr>');
              row.append(col);
              $("#start-status-table tbody").append(row);
            }
        }).fail(function (result) {
            console.log("Error loading start-status: " + result);
        });
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

    function postContestObject(type, body) {
    	cds.doPost(type, body, function() {
    		$('#object-status').text("Posted successfully");
    	}, function(result) {
    		$('#object-status').text("Post failed: " + result.message);
    	})
    }

    function putContestObject(type, id, body) {
    	cds.doPut(type, id, body, function() {
    		$('#object-status').text(id + " put successfully");
    	}, function(result) {
    		$('#object-status').text("Put failed: " + result.message);
    	})
    }

    function patchContestObject(type, id, body) {
    	cds.doPatch(type, id, body, function() {
    		$('#object-status').text(id + " patched successfully");
    	}, function(result) {
    		$('#object-status').text("Patch failed: " + result.message);
    	})
    }

    function deleteContestObject(type, id) {
    	cds.doDelete(type, id, function() {
    		$('#object-status').text(id + " deleted successfully");
    	}, function(result) {
    		$('#object-status').text("Delete failed: " + result.message);
    	})
    }
    
    function resolve(cmd) {
    	if (cmd == null)
    		return;
    	
    	console.log("Resolve: " + cmd);
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
            	updateResolver();
            }
        };
        xmlhttp.open("PUT", "<%= webroot %>/admin/resolve/" + cmd, true);
        xmlhttp.send();
    }

    function updateResolver() {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
                var resp = xmlhttp.responseText;
                if (xmlhttp.status == 200) {
                   resp = JSON.parse(resp);
                   document.getElementById("resolver-current-pause").innerHTML = resp.pause;
                   document.getElementById("resolver-total-pauses").innerHTML = resp.total_pauses;
                   document.getElementById("resolver-stepping").innerHTML = resp.stepping;
                } else {
                   document.getElementById("resolver-current-pause").innerHTML = "?";
                   document.getElementById("resolver-total-pauses").innerHTML = "?";
                   document.getElementById("resolver-stepping").innerHTML = "?";
                }
            }
        };
        xmlhttp.open("GET", "<%= webroot %>/resolver", true);
        xmlhttp.send();
    }

    function updateInBackground() {
        document.getElementById("bg-status").innerHTML = "Updating status...";
        updateCountdown();
        updateStartStatusTable();
        updateResolver();

        setInterval(updateCountdown, 5000);
        setInterval(updateStartStatusTable, 5000);
        
        $('#lock').change(function() {
        	  if (!$(this).prop('checked')) {
            	  $("#locker").removeClass('btn-secondary').addClass('btn-danger');
            	  $("#lock-group").find('*').addClass("disabled");
        	  } else {
        	      $("#locker").removeClass('btn-danger').addClass('btn-secondary');
        	      $("#lock-group").find('*').removeClass("disabled");
        	  }
        	})
    }

    $(document).ready(updateInBackground);
</script>
<%@ include file="layout/footer.jsp" %>