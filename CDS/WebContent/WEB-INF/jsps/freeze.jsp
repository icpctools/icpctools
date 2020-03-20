<%@ page import="org.icpc.tools.contest.model.*" %>
<% request.setAttribute("title", "Freeze"); %>
<%@ include file="layout/head.jsp" %>
<% IContest contest2 = cc.getContestByRole(Role.BLUE);
    IContest contest1 = cc.getContestByRole(Role.PUBLIC);
    IState state = contest1.getState(); %>
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">State Changes</h3>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover table-striped">
                        <tr>
                            <th>Frozen:</th>
                            <td><%= ContestUtil.formatStartTime(state.getFrozen()) %>
                            </td>
                        </tr>
                        <tr>
                            <th>Ended:</th>
                            <td><%= ContestUtil.formatStartTime(state.getEnded()) %>
                            </td>
                        </tr>
                        <tr>
                            <th>Finalized:</th>
                            <td><%= ContestUtil.formatStartTime(state.getFinalized()) %>
                            </td>
                        </tr>
                        <tr>
                            <th>Thawed:</th>
                            <td><%= ContestUtil.formatStartTime(state.getThawed()) %>
                            </td>
                        </tr>
                    </table>

                </div>
            </div>
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Details</h3>
                </div>
                <div class="card-body p-0">

                <% IJudgement[] juds = contest1.getJudgements();
                int num = juds.length;
                ISubmission s = null;
                String trClass = "table-info";
                if (num > 0) {
                    s = contest1.getSubmissionById(juds[num - 1].getSubmissionId());
                }

                ISubmission t = null;
                IJudgementType jt = null;
                int jaf = 0;
                for (ISubmission sub : contest1.getSubmissions()) {
                    if (!contest1.isBeforeFreeze(sub)) {
                        if (contest1.isJudged(sub))
                            jaf++;
                        if (t == null) {
                            t = sub;
                            jt = contest1.getJudgementType(sub);
                        }
                    }
                }

                IJudgement[] juds2 = contest2.getJudgements();
                int num2 = juds2.length;
                ISubmission s2 = null;
                if (num2 > 0)
                    s2 = contest2.getSubmissionById(juds2[num2 - 1].getSubmissionId());

                ISubmission t2 = null;
                IJudgementType jt2 = null;
                int jaf2 = 0;
                for (ISubmission sub : contest2.getSubmissions()) {
                    if (!contest2.isBeforeFreeze(sub)) {
                        if (contest2.isJudged(sub))
                            jaf2++;
                        if (t2 == null) {
                            t2 = sub;
                            jt2 = contest2.getJudgementType(sub);
                        }
                    }
                }

                if (jt != null || jaf > 0)
                    trClass = "table-danger";
                else if (jt == null && jt2 != null)
                    trClass = "table-success"; %>

                    <table class="table table-sm table-hover table-striped">
                        <thead>
                            <tr>
                                <th></th>
                                <th>Public Contest</th>
                                <th>Full Contest</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td># submissions:</td>
                                <td>
                                    <%= contest1.getNumSubmissions() %>
                                </td>
                                <td>
                                    <%= contest2.getNumSubmissions() %>
                                </td>
                            </tr>
                            <tr>
                                <td># judgements:</td>
                                <td>
                                    <%= num %>
                                </td>
                                <td>
                                    <%= num2 %>
                                </td>
                            </tr>
                            <tr class="<%= trClass %>">
                                <td># of judgements after freeze:</td>
                                <td>
                                    <%= jaf %>
                                </td>
                                <td>
                                    <%= jaf2 %>
                                </td>
                            </tr>
                            <tr>
                                <td>First submission after the freeze:</td>
                                <td>
                                    <%= t == null ? "n/a" : t.getId() %>
                                </td>
                                <td>
                                    <%= t2 == null ? "n/a" : t2.getId() %>
                                </td>
                            </tr>
                            <tr class="<%= trClass %>">
                                <td>Judgement of first submission after the freeze:</td>
                                <td>
                                    <%= jt == null ? "n/a" : jt.getName() %>
                                </td>
                                <td>
                                    <%= jt2 == null ? "n/a" : jt2.getName() %>
                                </td>
                            </tr>
                            <tr>
                                <td>Submission time of most recent judgement:</td>
                                <td>
                                    <%= s == null ? "no judgements" : ContestUtil.formatTime(s.getContestTime()) %>
                                </td>
                                <td>
                                    <%= s2 == null ? "no judgements" : ContestUtil.formatTime(s2.getContestTime()) %>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
<%@ include file="layout/footer.jsp" %>