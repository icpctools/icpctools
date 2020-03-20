<div id="accordion">
<div class="card">
    <div class="card-header">
        <h4 class="card-title"><a data-toggle="collapse" data-parent="#accordion" href="#collapseState">State</a></h4>
        <div class="card-tools">
            <button type="button" class="btn btn-tool"
                onclick="location.href='<%= apiRoot %>/state'">API</button>
        </div>
    </div>
    <div id="collapseState" class="panel-collapse collapse in">
    <div class="card-body p-0">
        <table class="table table-sm table-hover table-striped table-head-fixed">
            <tbody>
                <tr>
                    <td><b>Started:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getStarted()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Frozen:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getFrozen()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Ended:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getEnded()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Finalized:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getFinalized()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>Thawed:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getThawed()) %>
                    </td>
                </tr>
                <tr>
                    <td><b>End of updates:</b></td>
                    <td><%= ContestUtil.formatStartTime(state.getEndOfUpdates()) %>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    </div>
</div>
</div>