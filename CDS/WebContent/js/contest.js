var contest=(function() {
	var info;
	var organizations;
	var groups;
	var teams;
	var languages;
	var problems;
	var clarifications;
	var awards;
	var scoreboard;
	
	var urlPrefix;
	
	var setContestId = function(id) {
		urlPrefix = '/api/contests/' + id + '/';
	}

	var loadInfo = function() {
		console.log("Loading info");
		var deferred = new $.Deferred();
		if (info != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix,
			  success: function(result) {
				  info = result;
			  }
			});
		}
	}

	var loadLanguages = function() {
		console.log("Loading languages: " + languages);
		var deferred = new $.Deferred();
		if (languages != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'languages',
			  success: function(result) {
				  languages = result;
			  }
			});
		}
	}

	var loadProblems = function() {
		console.log("Loading problems: " + problems);
		var deferred = new $.Deferred();
		if (problems != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'problems',
			  success: function(result) {
				  problems = result;
			  }
			});
		}
	}
	
	var loadOrganizations = function() {
		console.log("Loading organizations: " + organizations);
		var deferred = new $.Deferred();
		if (organizations != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'organizations',
			  success: function(result) {
				  organizations = result;
			  }
			});
		}
	}

	var loadGroups = function() {
		console.log("Loading groups: " + groups);
		var deferred = new $.Deferred();
		if (groups != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'groups',
			  success: function(result) {
				  groups = result;
			  }
			});
		}
	}

	var loadTeams = function() {
		console.log("Loading teams: " + teams);
		var deferred = new $.Deferred();
		if (teams != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'teams',
			  success: function(result) {
			    var teams2 = result;
			    teams2.sort(function(a,b) {
			    	if (!isNaN(a.id) && !isNaN(b.id))
			    		return Number(a.id) > Number(b.id);
			        else
			    		return a.id.localeCompare(b.id);
				   })
			    teams = teams2;
			  }
			});
		}
	}

	var loadClarifications = function() {
		console.log("Loading clarifications: " + clarifications);
		var deferred = new $.Deferred();
		if (clarifications != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'clarifications',
			  success: function(result) {
				  clarifications = result;
			  }
			});
		}
	}

	var loadAwards = function() {
		console.log("Loading awards: " + awards);
		var deferred = new $.Deferred();
		if (awards != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'awards',
			  success: function(result) {
				  awards = result;
			  }
			});
		}
	}

	var loadScoreboard = function() {
		console.log("Loading scoreboard: " + scoreboard);
		var deferred = new $.Deferred();
		if (scoreboard != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'scoreboard',
			  success: function(result) {
				  scoreboard = result;
			  }
			});
		}
	}
	
	var getInfo = function() {
		return info;
	}
	var getLanguages = function() {
		return languages;
	}
	var getProblems = function() {
		return problems;
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
	
	return {
		setContestId: setContestId,
		loadInfo: loadInfo,
		loadOrganizations: loadOrganizations,
		loadGroups: loadGroups,
		loadLanguages: loadLanguages,
		loadTeams: loadTeams,
		loadProblems: loadProblems,
		loadClarifications: loadClarifications,
		loadAwards: loadAwards,
		loadScoreboard: loadScoreboard,
		getInfo: getInfo,
		getProblems: getProblems,
		getGroups: getGroups,
		getOrganizations: getOrganizations,
		getTeams: getTeams,
		getClarifications: getClarifications,
		getAwards: getAwards,
		getScoreboard: getScoreboard,
		getTeamById: getTeamById
	};
})();