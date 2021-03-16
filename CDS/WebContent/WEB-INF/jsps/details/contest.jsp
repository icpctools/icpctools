
<div id="accordion">
<div class="card">
    <div class="card-header">
        <h4 class="card-title">Contest</h4>
        <div class="card-tools">
        </div>
    </div>
    <div class="card-body p-0">
        <table class="table table-sm table-hover table-striped table-head-fixed">
            <tbody>
                <tr>
                    <td><b>Start</b></td>
                    <td><%= ContestUtil.formatStartTime(contest) %></td>
                </tr>
                <tr>
                    <td><b>Duration</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getDuration()) %></td>
                </tr>
                <tr>
                    <td><b>Freeze duration</b></td>
                    <td><%= ContestUtil.formatDuration(contest.getFreezeDuration()) %></td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
</div>