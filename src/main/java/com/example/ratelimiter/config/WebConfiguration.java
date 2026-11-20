package com.example.ratelimiter.config;

import com.example.ratelimiter.adapter.in.web.error.ErrorResponses;
import com.example.ratelimiter.adapter.in.web.error.ExceptionLoggingFilter;
import com.example.ratelimiter.adapter.in.web.json.CodedEnumJacksonModule;
import com.example.ratelimiter.adapter.out.http.TargetApiProperties;
import com.fasterxml.jackson.databind.Module;
import java.net.http.HttpClient;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Web infrastructure: JSON enum codes, outermost error filter, HTTP client. */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration {

    @Bean
    public Module codedEnumJacksonModule() {
        return new CodedEnumJacksonModule();
    }

    @Bean
    public FilterRegistrationBean<ExceptionLoggingFilter> exceptionLoggingFilter(
            ErrorResponses errorResponses) {
        FilterRegistrationBean<ExceptionLoggingFilter> registration =
                new FilterRegistrationBean<>(new ExceptionLoggingFilter(errorResponses));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /** Blocking {@code send()} calls run on the caller's (virtual) request thread. */
    @Bean(destroyMethod = "close")
    public HttpClient targetApiHttpClient(TargetApiProperties properties) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
