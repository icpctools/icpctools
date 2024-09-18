<%@ page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@ page import="org.icpc.tools.contest.Trace" %>

</div>
<!-- /.content -->
</div>
<!-- /.content-wrapper -->

<!-- Control Sidebar -->
<aside class="control-sidebar control-sidebar-dark">
  <!-- Control sidebar content goes here -->
  <div class="p-3">
    <h5>Title</h5>
    <p>Sidebar content</p>
  </div>
</aside>
<!-- /.control-sidebar -->

<!-- Main Footer -->
<footer class="main-footer">
  <!-- Default to the left -->
  <strong>ICPC Tools CDS <%= Trace.getVersion() %></strong>
</footer>
</div>
<!-- ./wrapper -->

<% ConfiguredContest ccForLogout = (ConfiguredContest) request.getAttribute("cc"); %>
<% if (request.getRemoteUser() != null && ccForLogout != null) { %>
<script>
  $(function() {
    const accountUrl = '/api/contests/<%=ccForLogout.getId()%>/account';
    const checkLoginStatus = function() {
      $.ajax({
        url: accountUrl,
        error: function() {
          console.warn('Not logged in anymore, sending to log in page')
          window.location = '/login';
        }
      })
    };

    setInterval(checkLoginStatus, 30000);
  });
</script>
<% } %>
</body>
</html>
