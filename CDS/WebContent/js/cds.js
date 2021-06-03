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

    var getMessage = function(result) {
    	try {
			return JSON.parse(result.responseText);
		} catch (err) {
			return { "code": result.status, "message":"Invalid response: " + result.responseText };
		}
	}

	var doPost = function(type, body, ok, fail) {
 	    console.log("POSTing contest object: " + type + ", " + body);
 	    return $.ajax({
		    url: getURL(type, null),
		    method: 'POST',
		    data: body,
		    success: function(result) {
		    	ok(result);
		    },
		    error: function(result) {
			    fail(getMessage(result));
		    }
		});
    }

	var doPut = function(type, id, body, ok, fail) {
 	    console.log("PUTting contest object: " + type + "/" + id + ", " + body);
 	    return $.ajax({
		    url: getURL(type, id),
		    method: 'PUT',
		    data: body,
		    success: function(result) {
		    	ok(result);
		    },
		    error: function(result) {
			    fail(getMessage(result));
		    }
		});
    }

	var doPatch = function(type, id, body, ok, fail) {
        console.log("PATCHing contest object: " + type + "/" + id);
        return $.ajax({
		    url: getURL(type, id),
		    method: 'PATCH',
		    data: body,
		    success: function(result) {
		    	ok(result);
		    },
		    error: function(result) {
			    fail(getMessage(result));
		    }
		});
	}

	var doDelete = function(type, id, ok, fail) {
        console.log("DELETEing contest object: " + type + "/" + id);
        return $.ajax({
		    url: getURL(type, id),
		    method: 'DELETE',
		    success: function(result) {
		    	ok(result);
		    },
		    error: function(result) {
			    fail(getMessage(result));
		    }
		});
	}

	return {
		setContestId: setContestId,
		doPost: doPost,
		doPut: doPut,
		doPatch: doPatch,
		doDelete: doDelete
	};
})();