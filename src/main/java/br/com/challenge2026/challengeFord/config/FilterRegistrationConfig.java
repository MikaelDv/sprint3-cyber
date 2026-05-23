package br.com.challenge2026.challengeFord.config;

import br.com.challenge2026.challengeFord.security.HmacSignatureFilter;
import br.com.challenge2026.challengeFord.security.JwtAuthenticationFilter;
import br.com.challenge2026.challengeFord.security.RateLimitingFilter;
import br.com.challenge2026.challengeFord.security.SecurityHeadersFilter;
import br.com.challenge2026.challengeFord.security.SuspiciousActivityFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> disableJwt(JwtAuthenticationFilter f) {
        return disable(f);
    }

    @Bean
    FilterRegistrationBean<RateLimitingFilter> disableRate(RateLimitingFilter f) {
        return disable(f);
    }

    @Bean
    FilterRegistrationBean<HmacSignatureFilter> disableHmac(HmacSignatureFilter f) {
        return disable(f);
    }

    @Bean
    FilterRegistrationBean<SecurityHeadersFilter> disableHeaders(SecurityHeadersFilter f) {
        return disable(f);
    }

    @Bean
    FilterRegistrationBean<SuspiciousActivityFilter> disableSuspicious(SuspiciousActivityFilter f) {
        return disable(f);
    }

    private <T extends Filter> FilterRegistrationBean<T> disable(T filter) {
        FilterRegistrationBean<T> bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }
}
