<% request.setAttribute("title", "Video Channel Control"); %>
<% String channel2 = (String) request.getAttribute("channel"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Video Channel <%= channel2 %></h3>
           </div>
        <div class="card-body p-0">
            <table id="stream-table" class="table table-sm table-hover table-striped">
                <thead>
                <tr>
                    <th>Stream</th>
                    <th>Stream</th>
                    <th>Stream</th>
                    <th>Stream</th>
                    <th>Stream</th>
                    <th>Stream</th>
                    <th>Stream</th>
                    <th>Stream</th>
                </tr>
                </thead>
                <tbody></tbody>
            </table>
            </div></div>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script>
    var last;

    function sendCommand(id) {
        document.getElementById("team" + id).disabled = true;

        var xmlhttp = new XMLHttpRequest();

        xmlhttp.onreadystatechange = function () {
            document.getElementById("status").innerHTML = "Changing to " + id;
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    document.getElementById("status").innerHTML = "Success";
                    if (last != null) {
                        document.getElementById(last).innerHTML = "-";
                    }
                    document.getElementById("current" + id).innerHTML = "Now";
                    last = "current" + id;
                } else
                    document.getElementById("status").innerHTML = xmlhttp.responseText;
                document.getElementById("team" + id).disabled = false;
            }
        };

        xmlhttp.open("PUT", "video/channel/<%= channel2 %>/" + id, true);
        xmlhttp.send();
    }

    var curChannel = "${channel}";
    var curStream = "${stream}";

    function switchToStream(newStream) {
        $.ajax({
            url: "<%= request.getContextPath() %>/video/control/" + curChannel + "/" + newStream,
            method: "PUT",
            success: function (result) {
                curStream = newStream;
                selectRow(newStream);
            }
        })
    }

    var lastSel;

    function selectRow(id) {
        if (lastSel != null)
            $('#stream-table td[id=' + lastSel + ']').css('background-color', '#FFF');
        $('#stream-table td[id=' + id + ']').css('background-color', '#BBB');
        lastSel = id;
    }

    $(document).ready(function () {

        var streams;

        function fillTable() {
            if (streams == null)
                return;

            // sort
            streams.sort(function (a, b) {
                if (a.order != b.order)
                    return a.order - b.order;
                return a.name.localeCompare(b.name);
            });

            var columns = 10;
            var rows = Math.ceil(streams.length / columns);
            for (var i = 0; i < rows; i++) {
                var col = '';
                for (var j = 0; j < columns; j++) {
                    if (i + rows * j < streams.length) {
                        var stream = streams[i + rows * j];
                        col += '<td id=' + stream.id + '><a href="javascript:switchToStream(' + stream.id + ')">' + stream.name + '</a></td>';
                    }
                }
                var row = $('<tr></tr>');
                row.append($(col));
                $('#stream-table tbody').append(row);
            }

            selectRow(curStream);
        }

        var loadStreams = $.ajax({
            url: "<%= request.getContextPath() %>/video",
            success: function (result) {
                streams = result;
            }
        });

        $.when(loadStreams).done(function () {
            fillTable()
        }).fail(function (result) {
            alert("Could not load page!");
        });
    })
</script>
<%@ include file="layout/footer.jsp" %>