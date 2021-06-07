function parseColor(c) {
	if (c.substr(0, 1) == "#") {
		var len = (c.length - 1) / 3;
		var fact = [17, 1, 0.062272][len - 1];
	    return [
	        Math.round(parseInt(c.substr(1, len),16) * fact),
	        Math.round(parseInt(c.substr(1 + len, len), 16) * fact),
	        Math.round(parseInt(c.substr(1 + 2 * len, len), 16) * fact)
	    ];
    } else
    	return c.split("(")[1].split(")")[0].split(",").map(x=>+x);
}

function componentToHex(c) {
  var hex = c.toString(16);
  return hex.length == 1 ? "0" + hex : hex;
}

function rgbToHex(c) {
  return "#" + componentToHex(c[0]) + componentToHex(c[1]) + componentToHex(c[2]);
}

function addColors(obj, rgb) {
	if (rgb == null) {
		obj.rgb = "#FFFFFF";
		obj.border = "#888888";
		obj.fg = "#000000";
		return obj;
	}

	obj.rgb = rgb;
	
	darker = parseColor(rgb);
	darker[0] = Math.max(darker[0] - 64, 0);
	darker[1] = Math.max(darker[1] - 64, 0);
	darker[2] = Math.max(darker[2] - 64, 0);
	obj.border = rgbToHex(darker);

	foreground = parseColor(rgb);
	if (foreground[0] + foreground[1] + foreground[2] > 450)
		obj.fg = "#000000";
	else
		obj.fg = "#FFFFFF";

	return obj;
}

function languagesTd(lang) {
	var ext = '';
	if (lang.extensions != null)
		ext = lang.extensions.join(', ')
    return { name: lang.name, entry_point_required: lang.entry_point_required, entry_point_name: lang.entry_point_name, extensions: ext };
}

function judgementtypesTd(jt) {
	badge = "badge-info";
    if (jt.solved)
    	 badge = "badge-success";
    else if (jt.penalty)
    	 badge = "badge-danger";
    return { name: jt.name, penalty: jt.penalty, solved: jt.solved, badge: badge };
}

function problemsTd(problem) {
	return addColors({ label: problem.label, name: problem.name, color: problem.color }, problem.rgb);
}

function groupsTd(group) {
	var hidden = "";
    if (group.hidden != null)
        hidden = "true";

   	var logoSrc = '';
    var logo2 = bestSquareLogo(group.logo, 20);
    if (logo2 != null)
        logoSrc = '/api/' + logo2.href;
    return { icpc_id: group.icpc_id, name: group.name, type: group.type, hidden: hidden, logo: logoSrc };
}

function organizationsTd(org) {
	var logoSrc = '';
    var logo2 = bestSquareLogo(org.logo, 20);
    if (logo2 != null)
        logoSrc = '/api/' + logo2.href;

    var flagSrc = '';
    var flag2 = bestSquareLogo(org.country_flag, 20);
    if (flag2 != null)
        flagSrc = '/api/' + flag2.href;

    return { name: org.name, formalName: org.formal_name, country: org.country, logo: logoSrc, flag: flagSrc };
}

function teamsTd(team) {
	if (team == null)
		return null;

	var name = team.display_name;
	if (name == null)
		name = team.name;
    var org = findById(contest.getOrganizations(), team.organization_id);
    var orgName = '';
    var orgFormalName = '';
    var logoSrc = '';
    if (org != null) {
        orgName = getOrganizationName(org);
        var logo = bestSquareLogo(org.logo, 20);
        if (logo != null)
            logoSrc = '/api/' + logo.href;
    }
    var groupNames = '';
    var groups2 = findManyById(contest.getGroups(), team.group_ids);
    if (groups2 != null) {
        var first = true;
        for (var j = 0; j < groups2.length; j++) {
            if (!first)
                groupNames += ', ';
            groupNames += groups2[j].name;
            first = false;
        }
    }
    var hidden = "";
    if (team.hidden != null)
        hidden = "true";

    return { id: team.id, name: name, logo: logoSrc, orgName: orgName, groupNames: groupNames, hidden: hidden };
}

function queueTd(submission) {
	rowObj = submissionsTd(submission);
	rowObj.api = contest.getURL('submissions', id);
	return rowObj;
}

function submissionsTd(submission) {
	var sub = new Object();
	if (submission.contest_time != null)
        sub.time = formatTime(parseTime(submission.contest_time));
	if (submission.problem_id != null) {
        problem = findById(contest.getProblems(), submission.problem_id);
        if (problem != null) {
            sub.label = problem.label;;
            addColors(sub, problem.rgb);
        }
    }
	if (submission.language_id != null) {
        lang = findById(contest.getLanguages(), submission.language_id);
        if (lang != null)
        	sub.lang = lang.name;
    }
	sub.team = teamsTd(findById(contest.getTeams(), submission.team_id));
	var judgements = findManyBySubmissionId(contest.getJudgements(), submission.id);
	if (judgements != null && judgements.length > 0) {
        var first = true;
        sub.judge = '';
        sub.result = '';
        for (var j = 0; j < judgements.length; j++) {
            if (!first) {
            	sub.judge += ', ';
            	sub.result += ', ';
            }
            var jt = findById(contest.getJudgementTypes(), judgements[j].judgement_type_id);
            if (jt != null) {
            	sub.judge += judgementBadge(jt);
            	sub.result += judgementBadge(jt);
                if (jt.solved) {
                	if (isFirstToSolve(contest,submission)) {
                		sub.judgeClass = "bg-success";
                		sub.judge += " " + getBadge("FTS", "badge-info");
                		sub.result += " " + getBadge("FTS", "badge-info");
                	} else
                		sub.judgeClass = "table-success";
                } else if (jt.penalty)
                	sub.judgeClass = "table-danger";
            } else {
            	sub.judgeClass = "table-warning";
            	sub.judge += getBadge("...", "badge-warning");
            	sub.result += getBadge("...", "badge-warning");
            }
            sub.judge += ' (<a href="' + contest.getURL('judgements', judgements[j].id) + '">' + judgements[j].id + '</a>)';
            first = false;
        }
    }
    return sub;
}

function judgementBadge(jt) {
	if (jt == null)
		return '';

	badge = "badge-info";
    if (jt.solved)
    	 badge = "badge-success";
    else if (jt.penalty)
    	 badge = "badge-danger";
	return getBadge(jt.id, badge);
}

function getBadge(label,cl) {
	if (label == null)
		return '';

	template = '<span class="badge {{cl}}">{{label}}</span>';
	obj = new Object();
	obj.cl = cl;
	obj.label = label;
	return Mustache.render(template, obj);
}

function clarificationsTd(clar) {
	var c = new Object();
	c.time = formatTime(parseTime(clar.contest_time));
	c.text = sanitizeHTML(clar.text);
	if (clar.problem_id != null) {
        problem = findById(contest.getProblems(), clar.problem_id);
        if (problem != null) {
            c.label = problem.label;
            addColors(c, problem.rgb);
        }
    }
    c.fromTeam = teamsTd(findById(contest.getTeams(), clar.from_team_id));
    c.toTeam = teamsTd(findById(contest.getTeams(), clar.to_team_id));
    c.replyTo = clar.reply_to_id;
    return c;
}

function commentaryTd(comment) {
    problems = [comment.problem_ids];
    for (var j = 0; j < comment.problem_ids.length; j++) {
    	problem = findById(contest.getProblems(), comment.problem_ids[j]);
    	if (problem != null) {
	        problems[j] = { label: problem.label };
	        addColors(problems[j], problem.rgb);
	    }
    }
    teams = [];
    for (var j = 0; j < comment.team_ids.length; j++)
    	teams.push(teamsTd(findById(contest.getTeams(), comment.team_ids[j])));
    
    return { time: formatTime(parseTime(comment.contest_time)), message: sanitizeHTML(comment.message), problems: problems, teams: teams };
}

function awardsTd(award) {
    var teamsStr = "";
    for (var j = 0; j < award.team_ids.length; j++) {
        if (j > 0)
            teamsStr += "<br>";
        teamsStr += getDisplayStr(award.team_ids[j]);
    }
    return { citation: award.citation, teamsStr: teamsStr };
}

function startstatusTd(startStatus) {
	var a = 'default';
	var b = 'default';
	var c = 'default';
	if (startStatus.status == 0)
		a = 'danger';
	else if (startStatus.status == 1)
		b = 'warning';
	else if (startStatus.status == 2)
		c = 'success';
	return { label: startStatus.label, id: startStatus.id, a: a, b: b, c: c };
}