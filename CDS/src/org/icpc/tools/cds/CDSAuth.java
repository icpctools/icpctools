package org.icpc.tools.cds;

import java.util.Base64;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.CredentialValidationResult.Status;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAccount;

@ApplicationScoped
public class CDSAuth implements HttpAuthenticationMechanism {
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BASIC_PREFIX = "Basic ";
	private static final String RESULT = "result";
	private static final String ACCOUNT = "account";

	@Inject
	private IdentityStoreHandler identityStoreHandler;

	@Override
	public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
			HttpMessageContext context) throws AuthenticationException {
		// check if the user is trying to login via form
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		if (username != null && password != null)
			return validate(username, password, request, context);

		// check if user is trying basic auth (but not for UI)
		String uri = request.getRequestURI();
		if (uri.startsWith("/api") || uri.startsWith("/presentation") || uri.startsWith("/stream")) {
			String authHeader = request.getHeader(AUTHORIZATION_HEADER);
			if (authHeader != null && authHeader.startsWith(BASIC_PREFIX)) {
				String authValue = authHeader.substring(BASIC_PREFIX.length());
				String userAndPassword = new String(Base64.getDecoder().decode(authValue));
				if (!userAndPassword.contains(":"))
					return context.responseUnauthorized();

				username = userAndPassword.substring(0, userAndPassword.indexOf(':'));
				password = userAndPassword.substring(userAndPassword.indexOf(':') + 1);

				if (username != null && password != null)
					return validate(username, password, request, context);
			}
		}

		// check for host-based auto-login
		if (request.getRemoteUser() == null) {
			CDSConfig config = CDSConfig.getInstance();
			username = config.getUserFromHost(request.getRemoteHost());
			if (username == null)
				username = config.getUserFromHost(request.getRemoteAddr());

			if (username != null) {
				password = config.getUserPassword(username);
				if (password != null) {
					Trace.trace(Trace.INFO, "Auto-login user: " + username + " / " + password);
					return validate(username, password, request, context);
				}
			}
		}

		// otherwise check for previous validation
		Object obj = request.getSession().getAttribute(RESULT);
		if (obj != null && obj instanceof CredentialValidationResult)
			return context.notifyContainerAboutLogin((CredentialValidationResult) obj);

		return context.doNothing();
	}

	private AuthenticationStatus validate(String username, String password, HttpServletRequest request,
			HttpMessageContext context) {
		CredentialValidationResult result = identityStoreHandler
				.validate(new UsernamePasswordCredential(username, password));

		HttpSession session = request.getSession();
		session.setAttribute(RESULT, result);

		// find matching account
		session.setAttribute(ACCOUNT, null);
		List<IAccount> accounts = CDSConfig.getInstance().getAccounts();
		for (IAccount account : accounts) {
			if (username.equals(account.getUsername())) {
				session.setAttribute(ACCOUNT, account);
			}
		}

		if (result.getStatus() == Status.VALID)
			return context.notifyContainerAboutLogin(result);

		return context.responseUnauthorized();
	}

	public static IAccount getAccount(HttpServletRequest request) {
		return (IAccount) request.getSession().getAttribute(ACCOUNT);
	}

	public static String getAccountType(HttpServletRequest request) {
		IAccount account = getAccount(request);
		if (account == null)
			return "n/a";
		return account.getAccountType();
	}

	public static boolean isAdmin(HttpServletRequest request) {
		IAccount account = getAccount(request);
		if (account == null)
			return false;
		return IAccount.ADMIN.equals(account.getAccountType());
	}

	public static boolean isPresAdmin(HttpServletRequest request) {
		IAccount account = getAccount(request);
		if (account == null)
			return false;
		return IAccount.PRES_ADMIN.equals(account.getAccountType());
	}

	public static boolean isStaff(HttpServletRequest request) {
		IAccount account = getAccount(request);
		if (account == null)
			return false;
		return IAccount.STAFF.equals(account.getAccountType()) || IAccount.ADMIN.equals(account.getAccountType());
	}

	public static boolean isAnalyst(HttpServletRequest request) {
		IAccount account = getAccount(request);
		if (account == null)
			return false;
		return IAccount.ANALYST.equals(account.getAccountType());
	}
}