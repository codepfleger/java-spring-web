package io.opentracing.contrib.spring.web.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Frank Pfleger (trasier.com)
 */
@ConfigurationProperties(ClientInterceptorAdapterTracingProperties.CONFIGURATION_PREFIX)
public class ClientInterceptorAdapterTracingProperties {

    public static final String CONFIGURATION_PREFIX = WebTracingProperties.CONFIGURATION_PREFIX + ".ws";

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}