package net.org.selector.storer.security.xauth;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

/**
 * Filters incoming requests and installs a Spring Security principal
 * if a header corresponding to a valid user is found.
 */
public class XAuthTokenFilter extends GenericFilterBean {

    private final static String XAUTH_TOKEN_HEADER_NAME = "x-auth-token";

    private UserDetailsService detailsService;

    private TokenProvider tokenProvider;

    private final Logger log = LoggerFactory.getLogger(XAuthTokenFilter.class);

    public XAuthTokenFilter(UserDetailsService detailsService, TokenProvider tokenProvider) {
        this.detailsService = detailsService;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String authToken = getAuthToken(httpServletRequest);
            if (StringUtils.isNotBlank(authToken)) {
                String username = this.tokenProvider.getUserNameFromToken(authToken);
                UserDetails details = this.detailsService.loadUserByUsername(username);
                if (this.tokenProvider.validateToken(authToken, details)) {
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(details, details.getPassword(), details.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(token);
                }
            }
        } catch (Exception ex) {
            log.error("set authentication error", ex);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String getAuthToken(HttpServletRequest httpServletRequest) throws UnsupportedEncodingException {
        String authToken = null;
        if (httpServletRequest.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : httpServletRequest.getCookies()) {
            if (Objects.equals(cookie.getName(), XAUTH_TOKEN_HEADER_NAME)) {
                authToken = StringUtils.substringBetween(URLDecoder.decode(cookie.getValue(), "UTF-8"), "\"");
            }
        }
        return authToken;
    }
}
