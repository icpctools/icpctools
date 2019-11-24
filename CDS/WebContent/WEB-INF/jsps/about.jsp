<% request.setAttribute("title", "About"); %>
<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
        <img src="logo.png" height="200"/>
        <div class="card">
           <div class="card-header">
             <h3 class="card-title">About</h3>
           </div>
        <div class="card-body p-0">
            <p class="indent">The latest CDS documentation is available <a href="http://icpctools.org" target="_blank">here</a>.</p>

            <table class="table table-sm table-hover table-striped mb-4">
                <tbody>
                <tr>
                    <td>ICPC Tools</td>
                    <td><a href="https://icpc.baylor.edu/icpctools"
                           target="_blank">https://icpc.baylor.edu/icpctools</a></td>
                </tr>
                <tr>
                    <td>Github</td>
                    <td>
                        <a href="https://github.com/icpctools/icpctools" target="_blank">https://github.com/icpctools/icpctools</a>
                    </td>
                </tr>
                <tr>
                    <td>Latest builds</td>
                    <td>
                        <a href="https://pc2.ecs.csus.edu/icpctools/gitlabbuilds" target="_blank">https://pc2.ecs.csus.edu/icpctools/gitlabbuilds</a>
                    </td>
                </tr>
                </tbody>
            </table>
            </div></div>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>