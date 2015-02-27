package net.org.selector.storer.config;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * SLitvinov on 27.02.2015.
 * <p/>
 * Отключаем стандартный механизм обработки multipart запросов
 */
@Configuration
public class MultipartConfiguration {
    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver() {
            @Override
            public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
                return new DefaultMultipartHttpServletRequest(request) {
                    @Override
                    protected MultiValueMap<String, MultipartFile> getMultipartFiles() {
                        return new LinkedMultiValueMap<>(); //stub;
                    }

                    @Override
                    protected Map<String, String[]> getMultipartParameters() {
                        return ImmutableMap.of(); //stub
                    }
                };
            }
        };
    }
}
