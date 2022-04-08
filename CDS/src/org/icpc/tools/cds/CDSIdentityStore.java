package org.icpc.tools.cds;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

import org.icpc.tools.contest.model.IAccount;

@ApplicationScoped
public class CDSIdentityStore implements IdentityStore {
	public CredentialValidationResult validate(UsernamePasswordCredential userCredential) {
		List<IAccount> accounts = CDSConfig.getInstance().getAccounts();
		for (IAccount account : accounts) {
			if (userCredential.compareTo(account.getUsername(), account.getPassword())) {
				return new CredentialValidationResult(account.getUsername(),
						new HashSet<>(Arrays.asList(account.getAccountType())));
			}
		}
		return CredentialValidationResult.INVALID_RESULT;
	}
}