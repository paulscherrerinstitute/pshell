package ch.psi.pshell.security;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 *
 */
public class LdapAuthenticator implements UserAuthenticator {

    String ldapUrl;
    String domainComponent;
    String organizationalUnit;

    public LdapAuthenticator(String... pars) {
        this.ldapUrl = pars[0];
        this.domainComponent = pars[1];
        this.organizationalUnit = pars[2];
    }

    @Override
    public boolean authenticate(String user, String pwd) {
        if ((pwd == null) || pwd.isEmpty()) {
            return false;
        }
        Hashtable env = new Hashtable();
        StringBuilder sb = new StringBuilder();
        sb.append("cn=" + user);
        if ((organizationalUnit != null) && (!organizationalUnit.isEmpty())) {
            for (String ou : organizationalUnit.split("\\.")) {
                sb.append(",ou=").append(ou);
            }
        }
        if ((domainComponent != null) && (!domainComponent.isEmpty())) {
            for (String dc : domainComponent.split("\\.")) {
                sb.append(",dc=").append(dc);
            }
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, sb.toString());
        env.put(Context.SECURITY_CREDENTIALS, pwd);
        try {
            // Create initial context
            DirContext ctx = new InitialDirContext(env);
            ctx.close();
            return true;
        } catch (NamingException e) {
            return false;
        }
    }

}
