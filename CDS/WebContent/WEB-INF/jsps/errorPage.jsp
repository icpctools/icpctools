<%@ page isErrorPage="true" %>
<%@page import="org.icpc.tools.cds.util.HttpHelper" %>
<% response.setHeader("X-Frame-Options", "sameorigin");
   request.setAttribute("title", pageContext.getErrorData().getStatusCode() +" Error"); %>

<%@ include file="layout/head.jsp" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Oops! Something went wrong!</h3>
                </div>
                <div class="card-body">
                   All I know is that it has something to do with this: 
                   <pre><%= HttpHelper.sanitize(pageContext.getErrorData().getThrowable().getMessage()) %></pre>

                   <p>Please visit <a href="https://tools.icpc.global" target="_blank">tools.icpc.global</a> for help or go to
                   <a href="https://ccs-specs.icpc.io" target="_blank">clics.ecs.baylor.edu</a> for more info on the Contest API.</p>
                </div>
            </div>
        </div>
    </div>
</div>