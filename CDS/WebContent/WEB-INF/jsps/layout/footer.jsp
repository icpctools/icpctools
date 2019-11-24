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
    <!-- To the right -->
    <div class="float-right d-none d-sm-inline">
      ICPC Tools CDS <%= Trace.getVersion() %>
    </div>
    <!-- Default to the left -->
    <strong>Logged in as <%= ConfiguredContest.getUser(request) %></strong>
  </footer>
</div>
<!-- ./wrapper -->


</body>
</html>