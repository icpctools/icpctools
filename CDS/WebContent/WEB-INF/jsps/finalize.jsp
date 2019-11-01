<% request.setAttribute("title", "Finalize Control"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<body>
<%@ include file="layout/menu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Finalize Control</h1>
        </div>
    </div>
    <div class="row">
        <div class="col-sm-6 col-md-4">
            <div class="form-group">
                <label for="bSelect">Set value of b:</label>
                <select id="bSelect" class="custom-select">
                    <option value="0">0</option>
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                </select>
            </div>

            <button id="set" class="btn btn-primary"
                    onclick="var e = document.getElementById('bSelect'); sendCommand('set', 'b:' + e.options[e.selectedIndex].value)">
                Apply!
            </button>

            <span id="status"></span>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
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
</body>
</html>