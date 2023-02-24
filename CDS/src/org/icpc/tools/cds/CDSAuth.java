package org.icpc.tools.cds;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.identitystore.CredentialValidationResult;
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

	private List<IAccount> accounts;

	@Override
	public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
			HttpMessageContext context) throws AuthenticationException {
		// check if the user is trying to login via form
		HttpSession session = request.getSession();
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		if (username != null && password != null)
			return validate(username, password, session, context);

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
					return validate(username, password, session, context);
			}
		}

		// otherwise check for previous validation
		Object obj = request.getSession().getAttribute(RESULT);
		if (obj != null && obj instanceof CredentialValidationResult)
			return context.notifyContainerAboutLogin((CredentialValidationResult) obj);

		// check for host-based auto-login
		if (request.getRemoteUser() == null) {
			CDSConfig config = CDSConfig.getInstance();
			IAccount account = config.getAccountFromHost(request.getRemoteHost());
			if (account == null)
				account = config.getAccountFromHost(request.getRemoteAddr());

			if (account != null) {
				Trace.trace(Trace.INFO, "Auto-login " + account.getUsername());
				CredentialValidationResult result = new CredentialValidationResult(account.getUsername(),
						new HashSet<>(Arrays.asList(account.getAccountType())));
				session.setAttribute(ACCOUNT, account);
				session.setAttribute(RESULT, result);
				return context.notifyContainerAboutLogin(result);
			}
		}

		return context.doNothing();
	}

	private AuthenticationStatus validate(String username, String password, HttpSession session,
			HttpMessageContext context) {
		if (username != null && password != null) {
			if (accounts == null)
				accounts = CDSConfig.getInstance().getAccounts();

			for (IAccount account : accounts) {
				if (username.equals(account.getUsername()) && password.equals(account.getPassword())) {
					CredentialValidationResult result = new CredentialValidationResult(account.getUsername(),
							new HashSet<>(Arrays.asList(account.getAccountType())));
					session.setAttribute(ACCOUNT, account);
					session.setAttribute(RESULT, result);
					return context.notifyContainerAboutLogin(result);
				}
			}
		}

		session.setAttribute(ACCOUNT, null);
		session.setAttribute(RESULT, null);

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
		return IAccount.PRES_ADMIN.equals(account.getAccountType()) || IAccount.ADMIN.equals(account.getAccountType());
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
		return IAccount.ANALYST.equals(account.getAccountType()) || IAccount.STAFF.equals(account.getAccountType())
				|| IAccount.ADMIN.equals(account.getAccountType());
	}
}