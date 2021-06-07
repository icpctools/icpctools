<% request.setAttribute("title", "Contests"); %>
<script src="${pageContext.request.contextPath}/js/contest.js"></script>
<script src="${pageContext.request.contextPath}/js/model.js"></script>
<script src="${pageContext.request.contextPath}/js/luxon.min.js"></script>
<script src="${pageContext.request.contextPath}/js/ui.js"></script>
<script src="${pageContext.request.contextPath}/js/types.js"></script>
<script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
  <div class="row">
    <div id="contests" class="col-12">
    </div>
  </div>
</div>
<script type="text/html" id="contest-template">
<div class="card card-widget">
  <div class="card-header {{headerClass}} container-fluid">
    <div class="row">
     {{#logo}}<div class="col-sm-1" style="padding: 0px"><img style="width: 60px; height: 60px" src="{{{logo}}}"/></div>{{/logo}}
     <div class="col-lg">
       <div class="row">
         <div class="col-lg" style="font-size: 1.5rem; font-weight: 500">{{{ name }}}</div>
         <div class="col-sm text-right"><a href="{{{ api }}}" class="default-text-color">/{{{ id }}}</a></div>
       </div>
       <div class="row">
         <div class="col-5">{{{ start }}}</div>
         {{#progress}}
	     <div class="col-6 progress" style="padding: 0px; font-size: 0.75rem; background: #FFFFFF">
            <div class="progress-bar{{{ progressBg }}}" style="width: {{{ progress }}}%">{{{ time }}}</div>
         </div>
         {{/progress}}
         {{^progress}}<div class="col-6"></div>{{/progress}}
	     <div class="col-1 text-right">{{{ len }}}</div>
       </div>
     </div>
   </div>
  </div>
<div class="card-body" style="padding: 0.9rem">
  <div class="row">
<div class="col-sm"><a href="{{{ web }}}/details">
  <div class="small-box bg-info" style="margin-bottom: 0px">
    <div class="inner" style="padding: 6px 10px">
      <h3 id="numProblems{{{ count }}}">?</h3>
      <p style="margin-bottom: 0.75rem">Problems</p>
    </div>
    <div class="icon">
      <i class="fas fa-info"></i>
    </div>
  </div></a>
</div>
<div class="col-sm"><a href="{{{ web }}}/registration">
  <div class="small-box bg-info" style="margin-bottom: 0px">
    <div class="inner" style="padding: 6px 10px">
      <h3 id="numTeams{{{ count }}}">?</h3>
      <p style="margin-bottom: 0.75rem">Teams</p>
    </div>
    <div class="icon">
      <i class="fas fa-users"></i>
    </div>
  </div></a>
</div>
<div class="col-sm"><a href="{{{ web }}}/submissions">
  <div class="small-box bg-info" style="margin-bottom: 0px">
    <div class="inner" style="padding: 6px 10px">
      <h3 id="numSubmissions{{{ count }}}">?</h3>
      <p style="margin-bottom: 0.75rem">Submissions</p>
    </div>
    <div class="icon">
      <i class="fas fa-share"></i>
    </div>
  </div></a>
</div>
<div class="col-sm"><a href="{{{ web }}}/scoreboard">
  <div class="small-box bg-info" style="margin-bottom: 0px">
    <div class="inner" style="padding: 6px 10px">
      <h3>&nbsp;</h3>
      <p style="margin-bottom: 0.75rem">Scoreboard</p>
    </div>
    <div class="icon">
      <i class="fas fa-trophy"></i>
    </div>
  </div></a>
</div>
  </div>
</div>
</script>
<script type="text/javascript">
contests = new Contests("/api");

function loadDetails(contest, i) {
	$.when(contest.loadProblems(), contest.loadTeams(), contest.loadSubmissions()).done(function () {
		$("#numProblems" + i).html(contest.getProblems().length);
		$("#numTeams" + i).html(contest.getTeams().length);
		$("#numSubmissions" + i).html(contest.getSubmissions().length);
	})
}
$(document).ready(function () {
	$.when(contests.loadContests()).done(function () {
		contests = contests.getContestObjs();
		var template = $('#contest-template').html();

		for (var i = 0; i < contests.length; i++) {
			contest = contests[i];
			info = contest.getInfo();
			obj = { id: info.id, name: info.name };
			
			var logo = bestSquareLogo(info.logo, 60);
		    if (logo != null)
		        obj.logo = '/api/' + logo.href;
			obj.api = contest.getContestURL();
			obj.web = '/contests/' + info.id;
			obj.len = formatTime(parseTime(info.duration));
			if (info.time_multiplier != null)
				obj.len += ' (' + info.time_multiplier + 'x)';
			obj.count = i;
			
			time = contest.getContestTimeObj();
			if (time == null) {
				obj.start = "Not scheduled";
			} else if (time < 0) {
				obj.start = "Contest starting at " + formatTimestamp(info.start_time) + " (in " + formatTime(-time) + ")";
				obj.headerClass = 'bg-warning';
			} else if (typeof time == "string") {
				obj.start = "Countdown paused at " + time;
				obj.headerClass = 'bg-warning';
			} else {
				if (time > parseTime(info.duration)) {
					obj.start = "Contest over. Started at " + formatTimestamp(info.start_time);
				} else {
					obj.headerClass = 'bg-success';
					obj.progressBg = " bg-info";
					obj.start = "Contest started at " + formatTimestamp(info.start_time);
					if (info.scoreboard_freeze_duration != null) {
						freeze = parseTime(info.duration) - parseTime(info.scoreboard_freeze_duration);
						if (time > freeze) {
							obj.start += ". Scoreboard frozen";
							obj.progressBg = " bg-warning";
						}
					}
					obj.progress = time * 100 / parseTime(info.duration);
					obj.time = formatTime(time);
				}
			}

			cc = Mustache.render(template, obj);
			$('#contests').append(cc);

			loadDetails(contest, i);
		}
    }).fail(function (result) {
    	console.log("Error loading contests: " + result);
    })
})
</script>
<%@ include file="layout/footer.jsp" %>