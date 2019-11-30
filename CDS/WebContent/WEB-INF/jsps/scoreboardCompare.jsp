<% request.setAttribute("title", "Scoreboard Comparison"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
  <div class="row">
    <div class="col-12">
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">Scoreboard Comparison</h3>
        </div>
        <div class="card-body p-0">
          <table class="table table-sm table-hover table-striped">
            <tbody>
              <tr>
                <td>Comparing</td>
                <td>
                  <%= request.getAttribute("a") %>
                </td>
              </tr>
              <tr>
                <td class="text-right">to</td>
                <td>
                  <%= request.getAttribute("b") %>
                </td>
              </tr>
            </tbody>
          </table>

          <p class="indent"><%= (String) request.getAttribute("compare") %></p>
        </div>
      </div>
    </div>
  </div>
</div>
<%@ include file="layout/footer.jsp" %>