<%@ page import="org.icpc.tools.cds.ConfiguredContest" %>
<%@ page import="org.icpc.tools.contest.Trace" %>
<p class="footer">
    Logged in as <%= ConfiguredContest.getUser(request) %><br/>
    Powered by
    <a href="https://icpc.baylor.edu/icpctools/" target="_blank">
        ICPC Tools CDS version <%= Trace.getVersion() %>
    </a>
</p>