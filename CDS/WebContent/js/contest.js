var contest=(function() {
	var info;
	var organizations;
	var groups;
	var teams;
	var languages;
	var judgementTypes;
	var problems;
	var clarifications;
	var awards;
	var startStatus;
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
	
	var loadJudgementTypes = function() {
		console.log("Loading judgement types: " + judgementTypes);
		var deferred = new $.Deferred();
		if (judgementTypes != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'judgement-types',
			  success: function(result) {
				  judgementTypes = result;
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

	var loadStartStatus = function() {
		console.log("Loading start-status: " + startStatus);
		var deferred = new $.Deferred();
		if (startStatus != null) {
			deferred.resolve();
			return deferred;
		} else {
			return $.ajax({
			  url: urlPrefix + 'start-status',
			  success: function(result) {
				  startStatus = result;
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
	var getJudgementTypes = function() {
		return judgementTypes;
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
	var add = function(type, id, body, ok, fail) {
 	    console.log("Adding contest object: " + type + "/" + id + ", " + body);
 	    return $.ajax({
		    url: urlPrefix + type + '/' + id,
		    method: 'PUT',
		    data: body,
		    success: function(result) {
		    	ok(result);
		    },
		    error: function(result) {
			    fail(result);
		    }
		});
    }

	var remove = function(type, id, ok, fail) {
        console.log("Deleting contest object: " + type + "/" + id);
        return $.ajax({
		    url: urlPrefix + type + '/' + id,
		    method: 'DELETE',
		    success: function(result) {
		    	ok(result);
		    },
		    error: function(result) {
			    fail(result);
		    }
		});
	}

	return {
		setContestId: setContestId,
		loadInfo: loadInfo,
		loadOrganizations: loadOrganizations,
		loadGroups: loadGroups,
		loadLanguages: loadLanguages,
		loadJudgementTypes: loadJudgementTypes,
		loadTeams: loadTeams,
		loadProblems: loadProblems,
		loadClarifications: loadClarifications,
		loadAwards: loadAwards,
		loadStartStatus: loadStartStatus,
		loadScoreboard: loadScoreboard,
		getInfo: getInfo,
		getLanguages: getLanguages,
		getJudgementTypes: getJudgementTypes,
		getProblems: getProblems,
		getGroups: getGroups,
		getOrganizations: getOrganizations,
		getTeams: getTeams,
		getClarifications: getClarifications,
		getAwards: getAwards,
		getStartStatus: getStartStatus,
		getScoreboard: getScoreboard,
		getTeamById: getTeamById,
		clear: clear,
		add: add,
		remove: remove
	};
})();