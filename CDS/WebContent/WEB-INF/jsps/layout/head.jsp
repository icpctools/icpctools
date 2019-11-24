<%@page import="org.icpc.tools.contest.model.IContest" %>
<%@page import="org.icpc.tools.cds.CDSConfig" %>
<%@page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@page import="org.icpc.tools.cds.util.Role" %>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
    IContest contest = null;
    String webroot = null;
    String apiRoot = null;
    if (cc != null) {
    	contest = cc.getContestByRole(request);
    	webroot = request.getContextPath() + "/contests/" + cc.getId();
    	apiRoot = request.getContextPath() + "/api/contests/" + cc.getId();
    }
    String[] menuPages = {"", "/details", "/submissions", "/scoreboard", "/countdown", "/finalize", "/video/status", "/reports"};
    String[] menuTitles = {"Overview", "Details", "Submissions", "Scoreboard", "Countdown", "Finalize", "Video", "Reports"};
%>
<!DOCTYPE html>

<html lang="en">
<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/js/adminlte.min.js"></script>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta http-equiv="x-ua-compatible" content="ie=edge">

  <title>CDS</title>

  <link rel="stylesheet" href="${pageContext.request.contextPath}/fontawesome-free/css/all.min.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/adminlte.min.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cds.css"/>
  <!-- Google Font: Source Sans Pro -->
  <link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,400i,700" rel="stylesheet">
</head>
<body class="hold-transition sidebar-mini">
<div class="wrapper">

  <!-- Navbar -->
  <nav class="main-header navbar navbar-expand navbar-white navbar-light">
    <!-- Left navbar links -->
    <ul class="navbar-nav">
      <li class="nav-item">
        <a class="nav-link" data-widget="pushmenu" href="#"><i class="fas fa-bars"></i></a>
      </li>
      <li class="nav-item d-none d-sm-inline-block">
        <a href="/" class="nav-link">Home</a>
      </li>
    </ul>

    <!-- SEARCH FORM -->
    <form class="form-inline ml-3" action="/search">
      <div class="input-group input-group-sm">
        <input class="form-control form-control-navbar" type="search" placeholder="Search" aria-label="Search" name="value">
        <div class="input-group-append">
          <button class="btn btn-navbar" type="submit">
            <i class="fas fa-search"></i>
          </button>
        </div>
      </div>
    </form>
  </nav>
  <!-- /.navbar -->

  <!-- Main Sidebar Container -->
  <aside class="main-sidebar sidebar-dark-primary elevation-4">
    <!-- Brand Logo -->
    <a href="/" class="brand-link">
      <img src="${pageContext.request.contextPath}/logoGear.png" alt="CDS Logo" class="brand-image img-circle elevation-3">
      <span class="brand-text font-weight-light">Contest Data Server</span>
    </a>

    <!-- Sidebar -->
    <div class="sidebar">
      <!-- Sidebar Menu -->
      <nav class="mt-2">
        <ul class="nav nav-pills nav-sidebar flex-column" data-widget="treeview" role="menu" data-accordion="false">
          <!-- Add icons to the links using the .nav-icon class
               with font-awesome or any other icon font library -->

          <% ConfiguredContest[] ccs3 = CDSConfig.getContests();
             for (ConfiguredContest cc3 : ccs3) {
             if (!cc3.isHidden() || Role.isAdmin(request) || Role.isBlue(request)) {
                IContest contest3 = cc3.getContest(); %>

          <li class="nav-item has-treeview menu-<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains(contest3.getId())) { %>open<% } else { %>closed<% } %>">
            <a href="#" class="nav-link">
              <i class="nav-icon fas fa-tachometer-alt"></i>
              <p>
                <%= contest3.getName() %>
                <i class="right fas fa-angle-left"></i>
              </p>
            </a>
            
            
            
            <ul class="nav nav-treeview">
              <% for (int i = 0; i < menuPages.length; i++) { %>
              <li class="nav-item">
                <a href="/contests/<%= contest3.getId() %><%= menuPages[i] %>" class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").equals(webroot + menuPages[i])) { %> active<% } %>">
                  <i class="far fa-circle nav-icon"></i>
                  <p><%= menuTitles[i] %></p>
                </a>
              </li>
              <% } %>
            </ul>
            
            <% } } %>
          </li>
          <li class="nav-item">
            <a href="/presentation/admin/web" class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("presentation/admin/web")) { %> active<% } %>">
              <i class="nav-icon fas fa-address-card"></i>
              <p>Presentation Admin</p>
            </a>
          </li>

          <li class="nav-item has-treeview menu-<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("/video/control/")) { %>open<% } else { %>closed<% } %>">
            <a href="#" class="nav-link">
              <i class="nav-icon fas fa-video"></i>
              <p>
                Video
                <i class="right fas fa-angle-left"></i>
              </p>
            </a>

            <ul class="nav nav-treeview">
            <% for (int v = 1; v < 4; v++) { %>
              <li class="nav-item">
                <a href="/video/control/<%= v %>" class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("/video/control/" + v)) { %> active<% } %>">
                  <i class="far fa-circle nav-icon"></i>
                  <p>Channel <%= v %></p>
                </a>
              </li>
            <% } %>
            </ul>
          </li>

          <li class="nav-item">
            <a href="/about" class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("/about")) { %> active<% } %>">
              <i class="nav-icon fas fa-info"></i>
              <p>About</p>
            </a>
          </li>
        </ul>
      </nav>
      <!-- /.sidebar-menu -->
    </div>
    <!-- /.sidebar -->
  </aside>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <div class="content-header">
      <div class="container-fluid">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0 text-dark"><%= request.getAttribute("title") %></h1>
          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">