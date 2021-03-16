
<div id="accordion">
<div class="card">
    <div class="card-header">
        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseContest">Contest</a></h4>
        <div class="card-tools">
            <button type="button" class="btn btn-tool" onclick="location.href='<%= apiRoot %>'">API</button>
        </div>
    </div>
    <div id="collapseContest" class="panel-collapse collapse in">
    <div class="card-body p-0">
        <table class="table table-sm table-hover table-striped table-head-fixed">
            <tbody>
                <tr>
                    <td><b>Name:</b></td>
                    <td><%= HttpHelper.sanitizeHTML(contest.getName()) %></td>
                    <td><b>Start:</b></td>
                    <td><%= ContestUtil.formatStartTime(contest) %></td>
                </tr>
                <tr>
                    <td><b>Duration:</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getDuration()) %></td>
                    <td><b>Freeze duration:</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getFreezeDuration()) %></td>
                </tr>
                <tr>
                    <td class="align-middle"><b>Logo:</b></td>
                    <td class="table-dark" rowspan=2 id="logo"></td>
                    <td class="align-middle"><b>Banner:</b></td>
                    <td class="table-dark" rowspan=2 id="banner"></td>
                </tr>
            </tbody>
        </table>
    </div>
    </div>
</div>
</div>
<script type="text/javascript">
    $(document).ready(function () {
        function update() {
            var info = contest.getInfo();
            var logo = bestSquareLogo(info.logo, 50);
            console.log(info.name + " - " + info.logo + " -> " + logo);
            if (logo != null) {
                var elem = document.createElement("img");
                elem.setAttribute("src", "/api/" + logo.href);
                elem.setAttribute("height", "40");
                document.getElementById("logo").appendChild(elem);
            }
            var banner = bestLogo(info.banner, 100, 50);
            console.log(info.name + " - " + info.banner + " -> " + banner);
            if (banner != null) {
                var elem = document.createElement("img");
                elem.setAttribute("src", "/api/" + banner.href);
                elem.setAttribute("height", "40");
                document.getElementById("banner").appendChild(elem);
            }
        }

        $.when(contest.loadInfo()).done(function () {
            update()
        }).fail(function (result) {
            console.log("Error loading page: " + result);
        })
    })
</script>