<% request.setAttribute("title", "Finalization"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Finalize Control</h3>
           </div>
        <div class="card-body p-0">
          <p class="indent">
            <div class="form-group">
                <label for="bSelect" class="indent">Set value of b:</label>
                <select id="bSelect" class="custom-select indent">
                    <option value="0">0</option>
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                </select>
            
            <button id="set" class="btn btn-primary indent"
                    onclick="var e = document.getElementById('bSelect'); sendCommand('set', 'b:' + e.options[e.selectedIndex].value)">
                Apply!
            </button>

            <span class="indent" id="status"></span>
            </div>
        </div>
        </div></div>
    </div>
</div>
<script>
    function sendCommand(id, command) {
        document.getElementById(id).disabled = true;

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            document.getElementById("status").innerHTML = "";
            if (xmlhttp.readyState == 4) {
                var resp = xmlhttp.responseText;
                if (xmlhttp.status == 200) {
                    if (resp == null || resp.trim().length == 0)
                        targetTime = null;
                    else
                        targetTime = parseInt(resp) / 1000.0;
                } else
                    document.getElementById("status").innerHTML = resp;
                document.getElementById(id).disabled = false;
            }
        };
        xmlhttp.timeout = 10000;
        xmlhttp.ontimeout = function () {
            document.getElementById("status").innerHTML = "Request timed out";
            document.getElementById(id).disabled = false;
        };
        xmlhttp.open("PUT", "<%= webroot %>/finalize/" + command, true);
        xmlhttp.send();
    }
</script>
<%@ include file="layout/footer.jsp" %>