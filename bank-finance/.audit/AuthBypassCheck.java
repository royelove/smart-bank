import com.bank.bankfinance.common.filter.JwtAuthenticationFilter;
import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuthBypassCheck {

    static void setField(Object target, String name, Object value) throws Exception {
        Field f = JwtAuthenticationFilter.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    static String probe(String requestUri, String authHeader) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        // redisUtil is only touched inside the "Bearer " branch; leave it null to prove that branch isn't reached.
        setField(filter, "jwtUtil", null);
        setField(filter, "redisUtil", null);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", requestUri);
        req.setRequestURI(requestUri);
        if (authHeader != null) {
            req.addHeader("Authorization", authHeader);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicBoolean reachedController = new AtomicBoolean(false);
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest r, ServletResponse s) {
                reachedController.set(true);
            }
        };

        String outcome;
        try {
            filter.doFilter(req, res, chain);
            outcome = reachedController.get()
                    ? "REACHED CONTROLLER unauthenticated"
                    : "blocked (no controller call), httpStatus=" + res.getStatus();
        } catch (Throwable t) {
            outcome = "THREW " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
        return String.format("%-28s hdr=%-22s -> status=%s  %s  LoginContext.userId=%s account=%s",
                requestUri, String.valueOf(authHeader), res.getStatus(), outcome,
                LoginContext.getUserId(), LoginContext.getAccount());
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== JwtAuthenticationFilter behaviour ===");
        // not in PUBLIC_PATHS -> should require authentication
        System.out.println(probe("/account/getAccountEntity", null));
        System.out.println(probe("/account/transfer", null));
        System.out.println(probe("/support/admin/idcard/list", null));
        System.out.println(probe("/stock/orders", null));
        System.out.println("--- control: a Bearer token IS present but invalid ---");
        System.out.println(probe("/account/getAccountEntity", "Bearer not.a.real.token"));
        System.out.println(probe("/stock/orders", "Bearer not.a.real.token"));
        System.out.println("--- a PUBLIC_PATH with no token ---");
        System.out.println(probe("/stock/list", null));
    }
}
