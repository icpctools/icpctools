<%@page import="org.icpc.tools.contest.model.IContest" %>
<%@page import="org.icpc.tools.cds.CDSConfig" %>
<%@page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@page import="org.icpc.tools.cds.util.Role" %>
<%@page import="org.icpc.tools.cds.util.HttpHelper" %>
<% ConfiguredContest cc = (ConfiguredContest) request.getAttribute("cc");
    IContest contest = null;
    String webroot = null;
    String apiRoot = null;
    if (cc != null) {
    	contest = cc.getContestByRole(request);
    	webroot = request.getContextPath() + "/contests/" + cc.getId();
    	apiRoot = request.getContextPath() + "/api/contests/" + cc.getId();
    }
    String[] menuPages = {"", "/details", "/submissions", "/scoreboard", "/admin", "/video/status", "/reports"};
    String[] menuTitles = {"Overview", "Details", "Submissions", "Scoreboard", "Admin", "Video", "Reports"};
%>
<!DOCTYPE html>

<html lang="en">
<script src="${pageContext.request.contextPath}/js/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/js/adminlte.min.js"></script>
<script src="${pageContext.request.contextPath}/js/theme.js"></script>

<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <meta http-equiv="x-ua-compatible" content="ie=edge"/>

  <title>CDS</title>

  <link rel="stylesheet" href="${pageContext.request.contextPath}/fontawesome-free/css/all.min.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/adminlte.min.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cds.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/source-sans-pro.css"/>
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
      <form class="form-inline ml-3" action="${pageContext.request.contextPath}/search">
        <div class="input-group input-group-sm">
          <input class="form-control form-control-navbar" type="search" placeholder="Search" aria-label="Search"
            name="value"/>
          <div class="input-group-append">
            <button class="btn btn-navbar" type="submit">
              <i class="fas fa-search"></i>
            </button>
          </div>
        </div>
      </form>

      <!-- Right navbar links -->
      <ul class="navbar-nav ml-auto">
        <li class="nav-item dropdown theme-menu">
          <a class="nav-link" data-toggle="dropdown" href="#">
            <i class="fas fa-user"></i>&nbsp;&nbsp;
            Theme
          </a>
          <div class="dropdown-menu dropdown-menu-lg dropdown-menu-right">
            <a class="dropdown-item" href="#" data-theme="auto">
              <i class="fas fa-check fa-fw"></i>&nbsp;&nbsp;Use device theme
            </a>
            <a class="dropdown-item" href="#" data-theme="light">
              <i class="fas fa-fw"></i>&nbsp;&nbsp;Light theme
            </a>
            <a class="dropdown-item" href="#" data-theme="dark">
              <i class="fas fa-fw"></i>&nbsp;&nbsp;Dark theme
            </a>
          </div>
        </li>
        <% if (request.getRemoteUser() == null) { %>
          <li class="nav-item dropdown">
            <a class="nav-link" href="${pageContext.request.contextPath}/login">
              <i class="fas fa-sign-in-alt"></i>&nbsp;&nbsp;Login
            </a>
          </li>
        <% } else { %>
          <li class="nav-item dropdown">
            <a class="nav-link" data-toggle="dropdown" href="#">
              <i class="fas fa-user"></i>&nbsp;&nbsp;
              <%= request.getRemoteUser() %>
            </a>
            <div class="dropdown-menu dropdown-menu-lg dropdown-menu-right">
              <span class="dropdown-item dropdown-header">Role: <%= ConfiguredContest.getRole(request) %></span>
              <div class="dropdown-divider"></div>
              <a href="${pageContext.request.contextPath}/logout" class="dropdown-item dropdown-footer">
                <i class="fas fa-sign-out-alt"></i>&nbsp;&nbsp;Logout
              </a>
            </div>
          </li>
        <% } %>
      </ul>
    </nav>
    <!-- /.navbar -->

    <!-- Main Sidebar Container -->
    <aside class="main-sidebar sidebar-dark-primary elevation-4">
      <!-- Brand Logo -->
      <a href="/" class="brand-link">
        <img src="${pageContext.request.contextPath}/cdsIcon.png" alt="CDS Logo"
          class="brand-image img-circle elevation-3"/>
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
                IContest contest3 = cc3.getContest();
                if (contest3 == null) {
                  continue;
                }
                String webroot3 = request.getContextPath() + "/contests/" + cc3.getId(); %>

            <li class="nav-item has-treeview menu-<% if (cc == cc3) { %>open<% } else { %>closed<% } %>">
              <a href="#" class="nav-link <% if (cc == cc3) { %>active<% } %>">
                <i class="nav-icon fas fa-tachometer-alt"></i>
                <p>
                  <%= contest3.getName() != null ? contest3.getName() : "(unnamed contest)" %>
                  <i class="right fas fa-angle-left"></i><%= cc3.getError() == null ? "" : "<span class='badge badge-danger'>!</span>" %>
                </p>
              </a>

              <ul class="nav nav-treeview">
                <% for (int i = 0; i < menuPages.length; i++) 
                   if ((i > 0 && i < 4) || Role.isAdmin(request)) { %>
                <li class="nav-item">
                  <a href="${pageContext.request.contextPath}/contests/<%= cc3.getId() %><%= menuPages[i] %>"
                    class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").equals(webroot3 + menuPages[i])) { %> active<% } %>">
                    <i class="far fa-circle nav-icon"></i>
                    <p><%= menuTitles[i] %></p>
                  </a>
                </li>
                <% } %>
              </ul>

            </li>
            <% } } %>
            
            <% if (Role.isPresAdmin(request)) { %>
            <li class="nav-item">
              <a href="${pageContext.request.contextPath}/presentation/admin/web"
                class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("presentation/admin/web")) { %> active<% } %>">
                <i class="nav-icon fas fa-address-card"></i>
                <p>Presentation Admin</p>
              </a>
            </li>
            <% } %>

            <% if (Role.isAdmin(request)) { %>
            <li class="nav-item has-treeview menu-<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("/video/control/")) { %>open<% } else { %>closed<% } %>">
              <a href="#" class="nav-link <% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("/video/control/")) { %>active<% } %>">
                <i class="nav-icon fas fa-video"></i>
                <p>
                  Video
                  <i class="right fas fa-angle-left"></i>
                </p>
              </a>

              <ul class="nav nav-treeview">
                <% for (int v = 1; v < 4; v++) { %>
                <li class="nav-item">
                  <a href="${pageContext.request.contextPath}/video/control/<%= v %>"
                    class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("/video/control/" + v)) { %> active<% } %>">
                    <i class="far fa-circle nav-icon"></i>
                    <p>Channel <%= v %></p>
                  </a>
                </li>
                <% } %>
              </ul>
            </li>
            <% } %>

            <li class="nav-item">
              <a href="${pageContext.request.contextPath}/about"
                class="nav-link<% if (request.getAttribute("javax.servlet.forward.request_uri").toString().contains("/about")) { %> active<% } %>">
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
            <div class="col-sm-10">
              <% String contestName = "";
               if (contest != null && contest.getName() != null)
                   contestName = contest.getName() + " "; %>
              <h1 class="m-0"><%= HttpHelper.sanitizeHTML(contestName) %><%= request.getAttribute("title") %></h1>
            </div><!-- /.col -->
          </div><!-- /.row -->
        </div><!-- /.container-fluid -->
      </div>
      <!-- /.content-header -->

      <!-- Main content -->
      <div class="content">