<% String webroot = request.getContextPath(); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cds.css">
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/favicon.png"/>
    <title>
        <%= request.getAttribute("title") %>
    </title>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark fixed-top">
    <a class="navbar-brand" href="/<%= webroot %>">
        <img src="${pageContext.request.contextPath}/cdsIcon.png" alt="CDS" height="30"
             class="d-inline-block align-top">
        Contest Data Server
    </a>
</nav>