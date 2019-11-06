<% request.setAttribute("title", "Contest Comparison"); %>
<!doctype html>
<html>
<%@ include file="layout/head.jsp" %>
<%@page import="org.icpc.tools.contest.model.util.EventFeedUtil.CompareFull"%>
<%@page import="org.icpc.tools.contest.model.util.EventFeedUtil.DiffType"%>
<body>
<%@ include file="layout/contestMenu.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <h1>Contest Comparison</h1>

            <table class="table table-sm table-hover table-striped">
                <tbody>
                <tr>
                    <td>Comparing</td>
                    <td><%= request.getAttribute("a") %>
                    </td>
                </tr>
                <tr>
                    <td class="text-right">to</td>
                    <td><%= request.getAttribute("b") %>
                    </td>
                </tr>
                </tbody>
            </table>
            <p/>

            <table class="table table-sm table-hover table-striped">
                <tr>
                    <td>Contest</td>
                    <td>
                        <%= (String) request.getAttribute("info") %>
                    </td>
                </tr>
                <tr>
                    <td>Languages</td>
                    <td>
                        <%= (String) request.getAttribute("languages") %>
                    </td>
                </tr>
                <tr>
                    <td>Judgement Types</td>
                    <td>
                        <%= (String) request.getAttribute("judgement-types") %>
                    </td>
                </tr>
                <tr>
                    <td>Problems</td>
                    <td>
                        <%= (String) request.getAttribute("problems") %>
                    </td>
                </tr>
                <tr>
                    <td>Groups</td>
                    <td>
                        <%= (String) request.getAttribute("groups") %>
                    </td>
                </tr>
                <tr>
                    <td>Organizations</td>
                    <td>
                        <%= (String) request.getAttribute("organizations") %>
                    </td>
                </tr>
                <tr>
                    <td>Teams</td>
                    <td>
                        <%= (String) request.getAttribute("teams") %>
                    </td>
                </tr>
                <tr>
                    <td>Submissions</td>
                    <td>
                        <%= (String) request.getAttribute("submissions") %>
                    </td>
                </tr>
                <tr>
                    <td>Judgements</td>
                    <td>
                        <%= (String) request.getAttribute("judgements") %>
                    </td>
                </tr>
                <tr>
                    <td>Awards</td>
                    <td>
                        <%= (String) request.getAttribute("awards") %>
                    </td>
                </tr>
            </table>
        </div>
    </div>

<h3>Comparison detail</h3>

<table>
<% org.icpc.tools.contest.model.util.EventFeedUtil.CompareFull fc = (org.icpc.tools.contest.model.util.EventFeedUtil.CompareFull) request.getAttribute("awards2");
int numContests = fc.vals.length;
// header
%><tr><th>Id</th><%
for (int j = 0; j < numContests; j++) {
	%><th>Contest <%= j %></th><%
}%></tr><%

// body
for (int i = 0; i < fc.ids.size(); i++) {
	  %><tr valign=top><td><%= fc.ids.get(i) %></td><%
    for (int j = 0; j < numContests; j++) {
    	%><td><%
    	String[] vars = fc.vals[j][i];
    	org.icpc.tools.contest.model.util.EventFeedUtil.DiffType[] dt = fc.diff[i];
    	if (vars != null) {
    	  for (int k = 0; k < vars.length; k++) {
    		 if (vars[k] != null) {
    	    	if (dt[k] == org.icpc.tools.contest.model.util.EventFeedUtil.DiffType.DIFF_BAD) {
    	    		%><font color="red"><%= vars[k] %></font><br/><%
    	    	} else {
    	    		%><%= vars[k] %><br/><%
    	    	}
    	    }
      	  }
    	}
    	%></td><%
    }
    %></tr><%
  }
%>
</table>
</div>


<%@ include file="layout/footer.jsp" %>
<%@ include file="layout/scripts.jsp" %>
</body>
</html>