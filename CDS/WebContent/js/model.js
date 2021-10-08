function findById(arr, id) {
  if (arr == null || id == null)
    return null;

  for (var i = 0; i < arr.length; i++) {
    if (id == arr[i].id)
      return arr[i];
  }
  return null;
}

function findManyById(arr, ids) {
  if (arr == null || ids == null || ids.length == 0)
    return null;

  var list = [];
  for (var j = 0; j < ids.length; j++) {
    for (var i = 0; i < arr.length; i++) {
      if (ids[j] == arr[i].id)
        list.push(arr[i]);
    }
  }
  return list;
}

function findManyBySubmissionId(arr, id) {
  if (arr == null || id == null)
    return null;

  var list = [];
  for (var i = 0; i < arr.length; i++) {
    if (arr[i].submission_id == id)
      list.push(arr[i]);
  }
  return list;
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
	
	if (match == null || match.length < 4)
		return null;

	h = parseInt(match[1]);
	m = parseInt(match[2]);
	s = parseInt(match[3]);	
	ms = 0;
	if (match.length == 5)
		ms = parseInt(match[4].substring(1));

	ret = h * 60 * 60 * 1000 + m * 60 * 1000 + s * 1000 + ms;
	if (contestTime.startsWith("-"))
	  return -ret

	return ret;
}

function isFirstToSolve(contest, submission) {
   problem_id = submission.problem_id;
   submissions = contest.getSubmissions();
   for (var i = 0; i < submissions.length; i++) {
      if (parseTime(submissions[i].contest_time) >= 0 && submissions[i].problem_id == problem_id) {
         // TODO: should we check if this is a public team too?
         var judgements = findManyBySubmissionId(contest.getJudgements(), submissions[i].id);
         if (judgements != null && judgements.length > 0) {
            var jt = findById(contest.getJudgementTypes(), judgements[judgements.length - 1].judgement_type_id);
            if (jt != null) {
               if (jt.solved) {
                  if (submission == submissions[i])
                     return true;
                  return false;
               }
            }
         }
      }
   }
   return false;
}

function sortProblems(problems) {
   return problems.sort((a,b) => (a.ordinal > b.ordinal) ? 1 : ((b.ordinal > a.ordinal) ? -1 : 0));
}