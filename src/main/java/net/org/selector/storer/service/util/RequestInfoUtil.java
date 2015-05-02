package net.org.selector.storer.service.util;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Selector on 02.05.2015.
 */
public final class RequestInfoUtil {
    public static String getSiteName() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Iterable<String> urlParts = Splitter.on('/').split(request.getServletPath());
        String result;
        if (Objects.equal(Iterables.get(urlParts, 1, ""), "api")) {
            result = Iterables.get(urlParts, 4, "");
        } else {
            result = Iterables.get(urlParts, 2, "");
        }
        if (Strings.isNullOrEmpty(result)) {
            throw new IllegalArgumentException();
        }
        return result;
    }
}
