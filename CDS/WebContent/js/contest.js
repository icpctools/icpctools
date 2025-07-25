class Contest {
	info;
	access;
	state;
	organizations;
	groups;
	teams;
	persons;
	accounts;
	account;
	languages;
	judgementTypes;
	problems;
	submissions;
	judgements;
	runs;
	clarifications;
	commentary;
	awards;
	startStatus;
	scoreboard;

	timeDelta = [];

	constructor(baseURL, contestId) {
		if (!baseURL.endsWith('/'))
			baseURL += '/';
		this.contestURL = baseURL + 'contests/' + contestId;
		console.log("Contest URL: " + this.contestURL);
	}

	getURL(type, id) {
		if (id == null)
			return this.contestURL + '/' + type;
		return this.contestURL + '/' + type + '/' + id;
	}

	loadObject(type, ok) {
		console.log("Loading contest " + type);
		this.start = Date.now();
		return $.ajax({
			url: this.getURL(type),
			success: (result, status, xhr) => {
				var time = xhr.getResponseHeader("ICPC-Time");
				var d = null;
				if (time == null)
					d = new Date(xhr.getResponseHeader("Date"));
				else
					d = new Date(parseInt(time));
  				
  				this.end = Date.now();
				var serverTime = (Date.now() - d.getTime()) - (this.end - this.start) / 2;
				if (this.timeDelta.length > 4)
					this.timeDelta.shift();
				this.timeDelta.push(serverTime);
				ok(result);
			}
		});
	}

	loadInfo() {
		return this.loadObject('', (result) => { this.info = result });
	}

	loadAccess() {
		if (this.access != null)
			return new $.Deferred().resolve();

		return this.loadObject('access', (result) => { this.access = result });
	}

	loadState() {
		if (this.state != null)
			return new $.Deferred().resolve();

		return this.loadObject('state', (result) => { this.state = result });
	}

	loadStartStatus() {
		if (this.startStatus != null)
			return new $.Deferred().resolve();

		return this.loadObject('start-status', (result) => { this.startStatus = result });
	}

	loadLanguages() {
		if (this.languages != null)
			return new $.Deferred().resolve();

		return this.loadObject('languages', (result) => { this.languages = result });
	}

	loadJudgementTypes() {
		if (this.judgementTypes != null)
			return new $.Deferred().resolve();

		return this.loadObject('judgement-types', (result) => { this.judgementTypes = result });
	}

	loadProblems() {
		if (this.problems != null)
			return new $.Deferred().resolve();

		return this.loadObject('problems', (result) => { this.problems = result });
	}

	loadGroups() {
		if (this.groups != null)
			return new $.Deferred().resolve();

		return this.loadObject('groups', (result) => this.groups = result );
	}

	loadOrganizations() {
		if (this.organizations != null)
			return new $.Deferred().resolve();

		return this.loadObject('organizations', (result) => { this.organizations = result });
	}

	loadTeams() {
		if (this.teams != null)
			return new $.Deferred().resolve();

		return this.loadObject('teams', (result) => {
			var teams2 = result;
			teams2.sort(function(a,b) {
				if (!isNaN(a.id) && !isNaN(b.id))
					return Number(a.id) > Number(b.id);
				else
					return a.id.localeCompare(b.id);
			})
			this.teams = teams2;
		});
	}

	loadPersons() {
		if (this.persons != null)
			return new $.Deferred().resolve();

		return this.loadObject('persons', (result) => { this.persons = result });
	}

	loadAccounts() {
		if (this.accounts != null)
			return new $.Deferred().resolve();

		return this.loadObject('accounts', (result) => { this.accounts = result });
	}

	loadAccount() {
		if (this.accounts != null)
			return new $.Deferred().resolve();

		return this.loadObject('account', (result) => { this.account = result });
	}

	loadSubmissions() {
		if (this.submissions != null)
			return new $.Deferred().resolve();

		return this.loadObject('submissions', (result) => { this.submissions = result });
	}

	loadJudgements() {
		if (this.judgements != null)
			return new $.Deferred().resolve();

		return this.loadObject('judgements', (result) => { this.judgements = result });
	}
	
	loadRuns() {
		if (this.runs != null)
			return new $.Deferred().resolve();

		return this.loadObject('runs', (result) => { this.runs = result });
	}

	loadClarifications() {
		if (this.clarifications != null)
			return new $.Deferred().resolve();

		return this.loadObject('clarifications', (result) => { this.clarifications = result });
	}

	loadCommentary() {
		if (this.commentary != null)
			return new $.Deferred().resolve();

		return this.loadObject('commentary', (result) => { this.commentary = result });
	}

	loadScoreboard() {
		if (this.scoreboard != null)
			return new $.Deferred().resolve();

		return this.loadObject('scoreboard', (result) => { this.scoreboard = result });
	}

	clearScoreboard() {
		this.scoreboard = null;
	}

	loadAwards() {
		if (this.awards != null)
			return new $.Deferred().resolve();

		return this.loadObject('awards', (result) => { this.awards = result });
	}

	getContestURL() { return this.contestURL }
	getInfo() { return this.info }
	getAccess() { return this.access }
	getState() { return this.state }
	getStartStatus() { return this.startStatus }
	getLanguages() { return this.languages }
	getJudgementTypes() { return this.judgementTypes }
	getProblems() { return this.problems }
	getGroups() { return this.groups }
	getTeams() { return this.teams }
	getOrganizations() { return this.organizations }
	getPersons() { return this.persons }
	getAccounts() { return this.accounts }
	getAccount() { return this.account }
	getSubmissions() { return this.submissions }
	getJudgements() { return this.judgements }
	getRuns() { return this.runs }
	getClarifications() { return this.clarifications }
	getCommentary() { return this.commentary }
	getScoreboard() { return this.scoreboard }
	getAwards() { return this.awards }

	getTimeDelta() {
		if (this.timeDelta.length == 0)
			return 0;
		var total = 0;
		this.timeDelta.forEach(function(item) { total += item });
		return total / this.timeDelta.length;
	}

	getContestTimeObj() {
		// returns a single object with contest time/state
		// null - if unscheduled
		// -time - if in countdown
		// "time" - if countdown is paused
		// time - time if past contest start
		if (this.info == null) {
			// contest info wasn't loaded yet - so let's do that in the background in case we're called again
			this.loadInfo();
			return null;
		}

		var m = this.info.time_multiplier;
		if (m == null)
			m = 1;

		if (this.info.start_time == null) {
			if (this.info.countdown_pause_time == null)
				return null;
			else
				return formatTime(parseTime(this.info.countdown_pause_time) * m) + "";
		}

		var d = new Date(this.info.start_time);

		return (Date.now() - d.getTime()) * m - this.getTimeDelta();
	}

	clear() {
		this.startStatus = null;
		this.problems = null;
		this.submissions = null;
		this.judgements = null;
		this.clarifications = null;
		this.commentary = null;
	}

	post(type, body, success, error) {
        console.log("POSTing contest object: " + type);
        return $.ajax({
		    url: this.getURL(type),
		    method: 'POST',
		    headers: { "Accept": "application/json" },
		    contentType: "application/json; charset=utf-8",
		    data: body,
		    success: success,
		    error: function(result, _ajaxOptions, _thrownError) {
		       var obj = jQuery.parseJSON(result.responseText);
		       if (obj != null && obj.message != null) {
		          error(obj.message)
	              return
	           }
		       error(result.responseText)
		    }
		});
	}

	postSubmission(obj, success, error) {
        this.post('submissions', obj, success, error);
	}

	postClarification(obj, success, error) {
        this.post('clarifications', obj, success, error);
	}

	postCommentary(obj, success, error) {
        this.post('commentary', obj, success, error);
	}
}

class Contests {
	contests;
	contestObjs;

	constructor(baseURL) {
		if (!baseURL.endsWith('/'))
			baseURL += '/';
		this.baseURL = baseURL;
		console.log("Base URL: " + this.baseURL);
	}

	loadContests() {
		if (this.contests != null)
			return new $.Deferred().resolve();
			
		console.log("Loading contests");
		var deferred = new $.Deferred();
		return $.ajax({
			url: this.baseURL + 'contests',
			success: (result) => { this.contests = result }
		});
	}

	getBaseURL() { return this.baseURL }
	getContests() { return this.contests }

	getContestObjs() {
		if (this.contestObjs != null)
			return this.contestObjs;

		contests = [];
		for (var i = 0; i < this.contests.length; i++) {
			var c = new Contest(this.baseURL, this.contests[i].id);
			c.info = this.contests[i];
			contests.push(c);
		}
		this.contestObjs = contests;
		return this.contestObjs;
	}

	clear() {
		this.contests = null;
	}
}