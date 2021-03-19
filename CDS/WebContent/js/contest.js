var contest=(function() {
	var info;
	var state;
	var organizations;
	var groups;
	var teams;
	var languages;
	var judgementTypes;
	var problems;
	var submissions;
	var judgements;
	var runs;
	var clarifications;
	var awards;
	var startStatus;
	var scoreboard;

	var contestURL;

	var setContestURL = function(baseURL, contestId) {
		if (!baseURL.endsWith('/'))
			baseURL += '/';
		contestURL = baseURL + 'contests/' + contestId;
		console.log("Contest URL: " + contestURL);
	}

	var loadObject = function(type, ok) {
		console.log("Loading contest " + type);
		var deferred = new $.Deferred();
		return $.ajax({
			url: getURL(type),
			success: function(result) {
				ok(result);
			}
		});
	}

	var loadInfo = function() {
		if (info != null)
			return new $.Deferred().resolve();

		return loadObject('', function(result) { info = result });
	}

	var loadState = function() {
		if (state != null)
			return new $.Deferred().resolve();

		return loadObject('state', function(result) { state = result });
	}

	var loadLanguages = function() {
		if (languages != null)
			return new $.Deferred().resolve();

		return loadObject('languages', function(result) { languages = result });
	}
	
	var loadJudgementTypes = function() {
		if (judgementTypes != null)
			return new $.Deferred().resolve();

		return loadObject('judgement-types', function(result) { judgementTypes = result });
	}

	var loadProblems = function() {
		if (problems != null)
			return new $.Deferred().resolve();

		return loadObject('problems', function(result) { problems = result });
	}
	
	var loadOrganizations = function() {
		if (organizations != null)
			return new $.Deferred().resolve();

		return loadObject('organizations', function(result) { organizations = result });
	}

	var loadGroups = function() {
		if (groups != null)
			return new $.Deferred().resolve();

		return loadObject('groups', function(result) { groups = result });
	}

	var loadTeams = function() {
		if (teams != null)
			return new $.Deferred().resolve();

		return loadObject('teams', function(result) {
			 var teams2 = result;
			    teams2.sort(function(a,b) {
			    	if (!isNaN(a.id) && !isNaN(b.id))
			    		return Number(a.id) > Number(b.id);
			        else
			    		return a.id.localeCompare(b.id);
				   })
			    teams = teams2;
		});
	}

    var loadSubmissions = function() {
		if (submissions != null)
			return new $.Deferred().resolve();

		return loadObject('submissions', function(result) { submissions = result });
	}

	var loadJudgements = function() {
		if (judgements != null)
			return new $.Deferred().resolve();

		return loadObject('judgements', function(result) { judgements = result });
	}

	var loadRuns = function() {
		if (runs != null)
			return new $.Deferred().resolve();

		return loadObject('runs', function(result) { runs = result });
	}

	var loadClarifications = function() {
		if (clarifications != null)
			return new $.Deferred().resolve();

		return loadObject('clarifications', function(result) { clarifications = result });
	}

	var loadAwards = function() {
		if (awards != null)
			return new $.Deferred().resolve();

		return loadObject('awards', function(result) { awards = result });
	}

	var loadStartStatus = function() {
		if (startStatus != null)
			return new $.Deferred().resolve();

		return loadObject('start-status', function(result) { startStatus = result });
	}

	var loadScoreboard = function() {
		if (scoreboard != null)
			return new $.Deferred().resolve();

		return loadObject('scoreboard', function(result) { scoreboard = result });
	}

	var getContestURL = function() {
		return contestURL;
	}
	var getInfo = function() {
		return info;
	}
	var getState = function() {
		return state;
	}
	var getLanguages = function() {
		return languages;
	}
	var getJudgementTypes = function() {
		return judgementTypes;
	}
	var getProblems = function() {
		return problems;
	}
	var getSubmissions = function() {
		return submissions;
	}
	var getJudgements = function() {
		return judgements;
	}
	var getRuns = function() {
		return runs;
	}
	var getClarifications = function() {
		return clarifications;
	}
	var getScoreboard = function() {
		return scoreboard;
	}
	var getOrganizations = function() {
		return organizations;
	}
	var getGroups = function() {
		return groups;
	}
	var getAwards = function() {
		return awards;
	}
	var getStartStatus = function() {
		return startStatus;
	}
	var getTeams = function() {
		return teams;
	}
	var getTeamById = function(id) {
		for (var i = 0; i < teams.length; i++) {
			if (teams[i].id == id)
				return teams[i];
		}
		return null;
	}
	var clear = function() {
		startStatus = null;
	}

    var post = function(type, body, ok, fail) {
        console.log("Posting (POST) contest object: " + type);
        return $.ajax({
		    url: getURL(type),
		    method: 'POST',
		    headers: { "Accept": "application/json" },
		    data: body,
		    success: function(body) {
		    	ok(body);
		    },
		    error: function(result) {
			    fail(result);
		    }
		});
	}

	var postSubmission = function(obj, ok, fail) {
        post('submissions', obj, ok, fail);
	}

	var postClarification = function(obj, ok, fail) {
        post('clarifications', obj, ok, fail);
	}

	var getURL = function(type, id) {
		if (id == null)
			return contestURL + '/' + type;
		return contestURL + '/' + type + '/' + id;
	}

	return {
		setContestURL: setContestURL,
		loadInfo: loadInfo,
		loadState: loadState,
		loadOrganizations: loadOrganizations,
		loadGroups: loadGroups,
		loadLanguages: loadLanguages,
		loadJudgementTypes: loadJudgementTypes,
		loadTeams: loadTeams,
		loadProblems: loadProblems,
		loadSubmissions: loadSubmissions,
		loadJudgements: loadJudgements,
		loadRuns: loadRuns,
		loadClarifications: loadClarifications,
		loadAwards: loadAwards,
		loadStartStatus: loadStartStatus,
		loadScoreboard: loadScoreboard,
		getContestURL: getContestURL,
		getURL: getURL,
		getInfo: getInfo,
		getState: getState,
		getLanguages: getLanguages,
		getJudgementTypes: getJudgementTypes,
		getProblems: getProblems,
		getGroups: getGroups,
		getOrganizations: getOrganizations,
		getTeams: getTeams,
		getSubmissions: getSubmissions,
		getJudgements: getJudgements,
		getRuns: getRuns,
		getClarifications: getClarifications,
		getAwards: getAwards,
		getStartStatus: getStartStatus,
		getScoreboard: getScoreboard,
		getTeamById: getTeamById,
		clear: clear,
		postSubmission: postSubmission,
		postClarification: postClarification
	};
})();