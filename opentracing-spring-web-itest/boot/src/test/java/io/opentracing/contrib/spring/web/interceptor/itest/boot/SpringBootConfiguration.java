package io.opentracing.contrib.spring.web.interceptor.itest.boot;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;

import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TestEndpoint;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.WebServiceConfig;
import io.opentracing.contrib.spring.web.ws.TracingClientInterceptorAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
import io.opentracing.contrib.spring.web.interceptor.HandlerInterceptorSpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.ExceptionFilter;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TestController;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TracingBeansConfiguration;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.WebSecurityConfig;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;


/**
 * @author Pavol Loffay
 */
@Configuration
@EnableAsync
@EnableAutoConfiguration
@Import({TracingBeansConfiguration.class,
        WebSecurityConfig.class,
        TestController.class,
        WebServiceConfig.class,
        TestEndpoint.class
})
public class SpringBootConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private List<HandlerInterceptorSpanDecorator> spanDecorators;

    @Autowired
    private Tracer tracer;

    @Bean
    public Filter exceptionFilter() {
        return new ExceptionFilter();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, Tracer tracer) {
        return builder.additionalInterceptors(new TracingRestTemplateInterceptor(tracer))
                .build();
    }

    @Bean
    public WebServiceGatewaySupport webServiceGatewaySupport(Tracer tracer) {
        WeatherWsClient weatherWsClient = new WeatherWsClient();
        TracingClientInterceptorAdapter clientInterceptorAdapter = new TracingClientInterceptorAdapter(tracer);
        weatherWsClient.setInterceptors(new TracingClientInterceptorAdapter[] { clientInterceptorAdapter });
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("io.opentracing.example.test_service");
        weatherWsClient.getWebServiceTemplate().setMarshaller(marshaller);
        weatherWsClient.getWebServiceTemplate().setUnmarshaller(marshaller);
        return weatherWsClient;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TracingHandlerInterceptor(tracer, spanDecorators));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/controllerView")
                .setStatusCode(HttpStatus.OK)
                .setViewName("staticView");
    }

    @Bean
    public FilterRegistrationBean tracingFilter() {
        TracingFilter tracingFilter = new TracingFilter(tracer,
                Collections.singletonList(ServletFilterSpanDecorator.STANDARD_TAGS), Pattern.compile("/health"));

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(tracingFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setAsyncSupported(true);

        return filterRegistrationBean;
    }
}

