var cds=(function() {
	var contestURL;

	var setContestId = function(contestId) {
		contestURL = '/api/contests/' + contestId;
		console.log("CDS URL: " + contestURL);
	}

	var getURL = function(type, id) {
		if (id == null)
			return contestURL + '/' + type;
		return contestURL + '/' + type + '/' + id;
	}

	var add = function(type, id, body, ok, fail) {
 	    console.log("Adding (PUT) contest object: " + type + "/" + id + ", " + body);
 	    return $.ajax({
		    url: getURL(type, id),
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

	var update = function(type, id, body, ok, fail) {
        console.log("Updating (PATCH) contest object: " + type + "/" + id);
        return $.ajax({
		    url: getURL(type, id),
		    method: 'PATCH',
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
        console.log("Deleting (DELETE) contest object: " + type + "/" + id);
        return $.ajax({
		    url: getURL(type, id),
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
		add: add,
		update: update,
		remove: remove
	};
})();