<% request.setAttribute("title", "Comparison"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">Contest Comparison</h3>
           </div>
        <div class="card-body p-0">
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
                    <td>Persons</td>
                    <td>
                        <%= (String) request.getAttribute("persons") %>
                    </td>
                </tr>
                <tr>
                    <td>Accounts</td>
                    <td>
                        <%= (String) request.getAttribute("accounts") %>
                    </td>
                </tr>
                <tr>
                    <td>Clarifications</td>
                    <td>
                        <%= (String) request.getAttribute("clarifications") %>
                    </td>
                </tr>
                <tr>
                    <td>Commentary</td>
                    <td>
                        <%= (String) request.getAttribute("commentary") %>
                    </td>
                </tr>
                <tr>
                    <td>Awards</td>
                    <td>
                        <%= (String) request.getAttribute("awards") %>
                    </td>
                </tr>
            </table>
            </div></div>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>