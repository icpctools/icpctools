var search=(function() {
	var results;
	
	var search = function(text) {
		console.log("Searching for: " + text);
		return $.ajax({
		  url: '/search/' + text,
		  success: function(result) {
			  results = result;
		  }
		});
	}
	
	var getResults = function() {
		return results;
	}
	
	return {
		search: search,
		getResults: getResults,
	};
})();