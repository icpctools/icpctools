function findById(arr,id) {
  if (arr == null || id == null)
    return null;

  for (var i = 0; i < arr.length; i++) {
    if (id == arr[i].id)
      return arr[i];
  }
  return null;
}

function findGroups(groups, ids) {
  if (groups == null || ids == null || ids.length == 0)
    return null;

  var grs = [];
  for (var j = 0; j < ids.length; j++) {
    for (var i = 0; i < groups.length; i++) {
      if (ids[j] == groups[i].id)
        grs.push(groups[i]);
    }
  }
  return grs;
}

function bestSquareLogo(logos, size) {
  if (logos == null || size == null)
    return null;

  var best;
  for (var i = 0; i < logos.length; i++) {
    ref = logos[i];
    if (ref.width != ref.height)
      continue;
    if (best == null)
      best = ref;
    else if (best != null) {
      if (best.width < size && best.height < size) {
        // current best image is too small - is this one better (larger than current)?
        if (ref.width > best.width || ref.height > best.height)
          best = ref;
        else if (best.width > size && best.height > size) {
          // current image is too big - is this one better (smaller but still big enough)?
          if (ref.width < best.width || ref.height < best.height) {
            if (ref.width >= size || ref.height >= size)
              best = ref;
          }
        }
      }
	}
  }
  if (best != null)
    return best;
  return bestLogo(logos, size, size);
}

function bestLogo(logos, width, height) {
  if (logos == null || width == null || height == null)
    return null;

  var best;
  for (var i = 0; i < logos.length; i++) {
    ref = logos[i];
    if (best == null)
      best = ref;
    else {
      if (best.width < width && best.height < height) {
        // current best image is too small - is this one better (larger than current)?
        if (ref.width > best.width || ref.height > best.height)
          best = ref;
        else if (best.width > width && best.height > height) {
          // current image is too big - is this one better (smaller but still big enough)?
          if (ref.width < best.width || ref.height < best.height) {
            if (ref.width >= width || ref.height >= height)
              best = ref;
          }
        }
      }
	}
  }
  return best;
}

function parseTime(contestTime) {
	match = contestTime.match("-?([0-9]+):([0-9]{2}):([0-9]{2})(\\.[0-9]{3})?");

	h = parseInt(match[0]);
	m = parseInt(match[1]);
	s = parseInt(match[2]);	
	ms = 0;
	if (match.length == 4)
		ms = parseInt(match[3].substring(1));

	ret = h * 60 * 60 * 1000 + m * 60 * 1000 + s * 1000 + ms;
	if (contestTime.startsWith("-"))
	  return -ret;
	
	return ret;
} 

var isInt = (function() {
	  var re = /^[+-]?\d+$/;
	  var re2 = /\.0+$/;

	  return function(n) {
	    return re.test((''+ n).replace(re2,''));
	  }
	}());