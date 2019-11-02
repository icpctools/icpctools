<%@ page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@ page import="org.icpc.tools.contest.model.IContest" %>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
    IContest contest = cc.getContestByRole(request);
    String webroot = request.getContextPath() + "/contests/" + cc.getId();
    String apiRoot = request.getContextPath() + "/api/contests/" + cc.getId();
    String[] menuPages = {"", "/details", "/orgs", "/teams", "/submissions", "/clarifications", "/scoreboard", "/countdown", "/finalize", "/awards", "/video/status", "/reports"};
    String[] menuTitles = {"Overview", "Details", "Organizations", "Teams", "Submissions", "Clarifications", "Scoreboard", "Countdown", "Finalize", "Awards", "Video", "Reports"};
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cds.css">
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/favicon.png"/>
    <title><%= request.getAttribute("title") %>
    </title>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark fixed-top">
    <a class="navbar-brand" href="<%= webroot %>">
        <img src="${pageContext.request.contextPath}/cdsIcon.png" alt="CDS" height="30"
             class="d-inline-block align-top">
        <%= contest.getFormalName() %>
    </a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#nav" aria-controls="nav"
            aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="nav">
        <ul class="navbar-nav mr-auto">
            <% for (int i = 0; i < menuPages.length; i++) { %>
            <li class="nav-item <% if (request.getAttribute("javax.servlet.forward.request_uri").equals(webroot + menuPages[i])) { %>active<% } %>">
                <a class="nav-link" href="<%= webroot + menuPages[i] %>">
                    <%= menuTitles[i] %>
                </a>
            </li>
            <% } %>
        </ul>
    </div>
</nav>