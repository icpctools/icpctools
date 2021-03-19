function languagesTd(lang) {
    return { name: sanitizeHTML(lang.name) };
}

function judgementtypesTd(jt) {
    return { name: sanitizeHTML(jt.name), penalty: jt.penalty ? '<i class="fas fa-times text-danger"></i> Yes' : null,
    	solved: jt.solved ? '<i class="fas fa-check text-success"></i> Yes' : null};
}

function problemsTd(problem) {
	return { label: problem.label, name: problem.name, color: problem.color, rgb: problem.rgb };
}

function groupsTd(group) {
	var hidden = "";
    if (group.hidden != null)
        hidden = "true";
    return { icpc_id: group.icpc_id, name: group.name, type: group.type, hidden: hidden };
}

function organizationsTd(org) {
	var logoSrc = '';
    var logo2 = bestSquareLogo(org.logo, 20);
    if (logo2 != null)
        logoSrc = '/api/' + logo2.href;
    if (logoSrc != null)
    	logoSrc = '<img src="' + logoSrc + '" width="20" height="20"/>';

    return { name: org.name, formalName: org.formal_name, country: org.country, logo: logoSrc };
}

function teamsTd(team) {
	var name = team.display_name;
	if (name == null)
		name = team.name;
    var org = findById(contest.getOrganizations(), team.organization_id);
    var orgName = '';
    var orgFormalName = '';
    var logoSrc = '';
    if (org != null) {
        orgName = org.name;
        if (org.formal_name != null)
            orgFormalName = org.formal_name;
        var logo = bestSquareLogo(org.logo, 20);
        if (logo != null)
            logoSrc = '/api/' + logo.href;
        if (logoSrc != null)
    		logoSrc = '<img src="' + logoSrc + '" width="20" height="20"/>';
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

    return { name: name, logo: logoSrc, orgName: orgName, orgFormalName: orgFormalName,
    	groupNames: groupNames };
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
        if (problem != null)
            sub.problem = problem.label + ' (' + problem.id + ')';
    }
	if (submission.language_id != null) {
        lang = findById(contest.getLanguages(), submission.language_id);
        if (lang != null)
        	sub.lang = lang.name;
    }
	if (submission.team_id != null) {
		team = submission.team_id;
        var team2 = findById(contest.getTeams(), submission.team_id);
        if (team2.organization_id != null) {
        	org = findById(contest.getOrganizations(), team2.organization_id);
            if (org != null)
                sub.org = org.name;
        }
        if (team2 != null) {
        	if (team2.display_name != null)
        	   sub.team = team2.id + ": " +team2.display_name;
        	else
        	   sub.team = team2.id + ": " + team2.name;
		}
    }
	var judgements = findManyBySubmissionId(contest.getJudgements(), submission.id);
	if (judgements != null && judgements.length > 0) {
        var first = true;
        sub.judge = '';
        for (var j = 0; j < judgements.length; j++) {
            if (!first)
            	sub.judge += ', ';
            var jt = findById(contest.getJudgementTypes(), judgements[j].judgement_type_id);
            if (jt != null) {
            	sub.judge += jt.name;
                if (jt.solved) {
                	if (isFirstToSolve(contest,submission))
                		sub.judgeClass = "bg-success";
                	else
                		sub.judgeClass = "table-success";
                } else if (jt.penalty)
                	sub.judgeClass = "table-danger";
            } else {
            	sub.judgeClass = "table-warning";
            	sub.judge += "...";
            }
            sub.judge += ' (<a href="' + contest.getURL('judgements', judgements[j].id) + '">' + judgements[j].id + '</a>)';
            first = false;
        }
    }
    return sub;
}

function clarificationsTd(clar) {
    var problem = '';
    var fromTeam = '';
    var toTeam = '';
    if (clar.problem_id != null) {
        problem = findById(contest.getProblems(), clar.problem_id);
        if (problem != null)
            problem = problem.label + ' (' + problem.id + ')';
    }
    var teams = contest.getTeams();
    if (clar.from_team_id != null) {
        fromTeam = findById(teams, clar.from_team_id);
        if (fromTeam != null)
            fromTeam = fromTeam.id + ' (' + fromTeam.name + ')';
    }
    if (clar.to_team_id != null) {
        toTeam = findById(teams, clar.to_team_id);
        if (toTeam != null)
            toTeam = toTeam.id + ' (' + toTeam.name + ')';
    }

    return { time: formatTime(parseTime(clar.contest_time)), problem: problem, fromTeam: fromTeam, toTeam: toTeam,
    	replyTo: clar.reply_to_id, text: clar.text };
}

function awardsTd(award) {
    var teamsStr = "";
    for (var j = 0; j < award.team_ids.length; j++) {
        if (j > 0)
            teamsStr += "<br>";
        teamsStr += award.team_ids[j] + ": ";
        var t = contest.getTeamById(award.team_ids[j]);
        if (t != null)
            teamsStr += t.name;
    }
    return { citation: award.citation, teamsStr: teamsStr };
}