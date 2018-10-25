package io.opentracing.contrib.spring.web.starter;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import io.opentracing.contrib.spring.web.ws.ClientInterceptorAdapterSpanDecorator;
import io.opentracing.contrib.spring.web.ws.TracingClientInterceptorAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author Frank Pfleger (trasier.com)
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass(ClientInterceptorAdapter.class)
@ConditionalOnProperty(prefix = ClientInterceptorAdapterTracingProperties.CONFIGURATION_PREFIX, name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@EnableConfigurationProperties(ClientInterceptorAdapterTracingProperties.class)
public class ClientInterceptorAdapterTracingAutoConfiguration {

    private static final Log log = LogFactory.getLog(ClientInterceptorAdapterTracingAutoConfiguration.class);

    @Configuration
    @ConditionalOnMissingBean(ClientInterceptorAdapterSpanDecorator.class)
    public static class StandardTagsConfiguration {
        @Bean
        public ClientInterceptorAdapterSpanDecorator standardTagsClientInterceptorAdapterSpanDecorator() {
            return ClientInterceptorAdapterSpanDecorator.STANDARD_LOGS;
        }
    }

    @Configuration
    @ConditionalOnBean(WebServiceTemplate.class)
    public static class WebServiceTemplatePostProcessingConfiguration {

        private final Tracer tracer;
        private final List<ClientInterceptorAdapterSpanDecorator> decorators;
        private final Set<WebServiceTemplate> templates;

        public WebServiceTemplatePostProcessingConfiguration(Tracer tracer, List<ClientInterceptorAdapterSpanDecorator> decorators, Set<WebServiceTemplate> templates) {
            this.tracer = tracer;
            this.decorators = decorators;
            this.templates = templates;
        }

        @PostConstruct
        public void init() {
            for (WebServiceTemplate template : templates) {
                registerTracingInterceptor(template, tracer, decorators);
            }
        }

    }

    @Configuration
    @ConditionalOnBean(WebServiceGatewaySupport.class)
    public static class WebServiceGatewaySupportPostProcessingConfiguration {

        private final Tracer tracer;
        private final List<ClientInterceptorAdapterSpanDecorator> decorators;
        private final Set<WebServiceGatewaySupport> gatewaySupports;

        public WebServiceGatewaySupportPostProcessingConfiguration(Tracer tracer, List<ClientInterceptorAdapterSpanDecorator> decorators, Set<WebServiceGatewaySupport> gatewaySupports) {
            this.tracer = tracer;
            this.decorators = decorators;
            this.gatewaySupports = gatewaySupports;
        }

        @PostConstruct
        public void init() {
            for (WebServiceGatewaySupport gatewaySupport : gatewaySupports) {
                registerTracingInterceptor(gatewaySupport.getWebServiceTemplate(), tracer, decorators);
            }
        }

    }

    private static void registerTracingInterceptor(WebServiceTemplate template, Tracer tracer, List<ClientInterceptorAdapterSpanDecorator> decorators) {
        ClientInterceptor[] interceptors = template.getInterceptors();

        for (ClientInterceptor interceptor : interceptors) {
            if (interceptor instanceof TracingClientInterceptorAdapter) {
                return;
            }
        }

        log.debug("Adding " + TracingClientInterceptorAdapter.class.getSimpleName() + " to " + template);
        List<ClientInterceptor> newInterceptors = new ArrayList<>();
        newInterceptors.addAll(Arrays.asList(interceptors));
        newInterceptors.add(new TracingClientInterceptorAdapter(tracer, decorators));
        template.setInterceptors(interceptors);
    }

}