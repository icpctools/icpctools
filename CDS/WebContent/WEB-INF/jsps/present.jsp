<% request.setAttribute("title", "Presentation Admin"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Summary</h3>
                </div>
                <div class="card-body p-0">
                    <table id="client-summary-table" class="table table-sm table-hover table-striped">
                        <thead>
                            <tr>
                                <th>Id</th>
                                <th>Display</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                        <tfoot>
                            <tr>
                                <td class="text-right"><b>Total</b></td>
                                <td>
                                    <div class="spinner-border"></div>
                                </td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Presentations</h3>
                </div>
                <div class="card-body p-0">
                    <table id="pres-table" class="table table-sm table-hover table-striped">
                        <thead>
                            <tr>
                                <th>Category</th>
                                <th>Presentation</th>
                                <th>Description</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>

                    <div id="status"></div>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Clients</h3>&nbsp;(<span id="client-count">0</span>)
                </div>
                <div class="card-body p-0">
                    <table id="client-table" class="table table-sm table-hover table-striped">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Display</th>
                                <th>Presentation</th>
                                <th>Actions</th>
                                <th style="width: 75px;"></th>
                                <th>Name</th>
                                <th>Display</th>
                                <th>Presentation</th>
                                <th>Actions</th>
                                <th style="width: 75px;"></th>
                                <th>Name</th>
                                <th>Display</th>
                                <th>Presentation</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script>
    var last;

    function setPresentation(id) {
        //document.getElementById("team"+id).disabled = true;

        var xmlhttp = new XMLHttpRequest();

        xmlhttp.onreadystatechange = function () {
            document.getElementById("status").innerHTML = "Changing to " + id;
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    document.getElementById("status").innerHTML = "Success";
                } else
                    document.getElementById("status").innerHTML = xmlhttp.responseText;
                //document.getElementById("team"+id).disabled = false;
            }
        };

        xmlhttp.open("PUT", "present/" + id, true);
        xmlhttp.send();
    }

    function stop(id) {
        var xmlhttp = new XMLHttpRequest();

        xmlhttp.onreadystatechange = function () {
            document.getElementById("status").innerHTML = "Changing to " + id;
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    document.getElementById("status").innerHTML = "Success";
                } else
                    document.getElementById("status").innerHTML = xmlhttp.responseText;
            }
        };

        xmlhttp.open("PUT", "stop/" + id, true);
        xmlhttp.send();
    }

    function restart(id) {
        var xmlhttp = new XMLHttpRequest();

        xmlhttp.onreadystatechange = function () {
            document.getElementById("status").innerHTML = "Changing to " + id;
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    document.getElementById("status").innerHTML = "Success";
                } else
                    document.getElementById("status").innerHTML = xmlhttp.responseText;
            }
        };

        xmlhttp.open("PUT", "restart/" + id, true);
        xmlhttp.send();
    }


    $(document).ready(function () {
        var pres;
        var clients;

        function fillPresTable() {
            if (pres == null)
                return;

            // sort
            pres.sort(function (a, b) {
                c = a.category.localeCompare(b.category);
                if (c != 0)
                    return c;
                return a.name.localeCompare(b.name);
            });

            var lastCategory;
            for (var i = 0; i < pres.length; i++) {
                var col = '<td>';
                if (pres[i].category != lastCategory) {
                    col += '<b>' + pres[i].category + '</b>';
                    lastCategory = pres[i].category;
                }
                col += '</td><td id=' + pres[i].id + '>' + pres[i].name + '</td><td>';
                if (pres[i].description != null)
                    col += pres[i].description;
                col += '</td><td><a href="javascript:setPresentation(\'' + pres[i].classname + '\')">Apply to all</a></td>';
                var row = $('<tr></tr>');
                row.append($(col));
                $('#pres-table tbody').append(row);
            }
        }

        function fillClientsTable() {
            if (clients == null)
                return;

            $('#client-count').text(clients.length);

            // sort
            clients.sort(function (a, b) {
                return a.name.localeCompare(b.name);
            });

            var map = new Map();
            var count = 0;
            var col = '';
            $('#client-table tbody').find("tr").remove();
            for (var i = 0; i < clients.length; i++) {
                col += '<td id=' + clients[i].uid + '>' + clients[i].name + '</td>';
                if (clients[i].display != null)
                    col += '<td>' + clients[i].display + '</td>';
                else
                    col += '<td></td>';
                if (clients[i].presentation != null) {
                    col += '<td>' + clients[i].presentation + '</td>';
                    var key = clients[i].presentation;
                    if (map.has(key)) {
                        var v = map.get(key);
                        map.set(key, v + 1);
                    } else
                        map.set(key, 1);
                } else
                    col += '<td></td>';
                col += '<td><a href="javascript:restart(\'' + clients[i].uid + '\')">Restart</a>&nbsp;' 
                + '<a href="javascript:stop(\'' + clients[i].uid + '\')">Stop</a></td>';

                count++;
                if (count % 3 == 0 || count == clients.length) {
                    var row = $('<tr></tr>');
                    row.append($(col));
                    $('#client-table tbody').append(row);
                    col = '';
                } else {
                    col += '<td></td>';
                }
            }

            count = 0;
            $('#client-summary-table tbody').find("tr").remove();
            $('#client-summary-table tfoot').find("tr").remove();
            for (var [key, value] of map) {
                var col = '<td>' + key + '</td><td>' + value + '</td>';
                row = $('<tr></tr>');
                row.append($(col));
                $('#client-summary-table tbody').append(row);
                count += value;
            }
            col = '<td class="text-right"><b>Total</b></td><td>' + count + '</td>';
            row = $('<tr></tr>');
            row.append($(col));
            $('#client-summary-table tfoot').append(row);
        }

        var loadPres = $.ajax({
            url: "/presentation/admin/present",
            success: function (result) {
                pres = result;
            }
        })

        function loadClients2() {
            $.ajax({
                url: "/presentation/admin/clients",
                success: function (result) {
                    clients = result;
                }
            })
        }

        $.when(loadPres).done(function () {
            fillPresTable()
        }).fail(function (result) {
            alert("Could not load page! " + result);
        })

        function repeatIt() {
            console.log("repeat!");
            loadClients2();
            fillClientsTable();
            setTimeout(arguments.callee, 2500);
        }

        repeatIt();

        $.when(loadClients2()).done(function () {
            fillClientsTable()
        }).fail(function (result) {
            alert("Could not load page! " + result);
        })
    })
</script>
<%@ include file="layout/footer.jsp" %>