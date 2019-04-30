var targetTime;

var countdown = document.getElementById("countdown");
var contestStatus = document.getElementById("contestStatus");
var running = false;

function toString(seconds_left) {
   var days = parseInt(seconds_left / 86400);
   seconds_left = seconds_left % 86400;
   
   var hours = parseInt(seconds_left / 3600);
   seconds_left = seconds_left % 3600;
   
   var minutes = parseInt(seconds_left / 60);
   var seconds = parseInt(seconds_left % 60);
   
   var text = "";
   if (days > 0)
      text = days + "d ";
   
   if (hours < 10)
      text += "0" + hours;
   else
      text += hours;
   
   text += ":";
   if (minutes < 10)
      text += "0" + minutes;
   else
      text += minutes;
   
   text += ":";
   if (seconds < 10)
      text += "0" + seconds;
   else
      text += seconds;
   
   return text;
}

// update the tag with id "countdown" every 300ms
setInterval(function () {
   if (targetTime == null) {
      countdown.innerHTML = "--:--:--";
      contestStatus.innerHTML = "Start time not set";
      return;
   } else if (targetTime == "") {
	  countdown.innerHTML = "??:??:??";
	  contestStatus.innerHTML = "Start time unknown";
      return;
   } else if (targetTime < 0) {
      countdown.innerHTML = toString(-targetTime);
      contestStatus.innerHTML = "Countdown paused";
      return;
   }
   // find the amount of "seconds" between now and target
   var current_date = new Date().getTime() / 1000.0;
   var seconds_left = (targetTime - current_date);
   
   if (seconds_left < 0) {
	  if (!running) {
	     countdown.innerHTML = "--:--:--";
         contestStatus.innerHTML = "Contest is over";
      } else {
         countdown.innerHTML = toString(-seconds_left);
         contestStatus.innerHTML = "Contest started";
      }
   } else {
      countdown.innerHTML = toString(seconds_left);
      contestStatus.innerHTML = "Contest countdown";
   }
}, 300);

function connectTime(id) {
   contestStatus.innerHTML = "Connecting...";
   var webSocket = new WebSocket("ws://contests/" + id + "/startstatus");
   webSocket.onmessage = function(msg) {
      var d = msg.data;
      if (d == null || d.trim().length == 0)
          targetTime = null;
      else if ("started" == d)
    	  running = true;
      else if ("stopped" == d)
    	  running = false;
      else
          targetTime = parseInt(d) / 1000.0;
   };
}