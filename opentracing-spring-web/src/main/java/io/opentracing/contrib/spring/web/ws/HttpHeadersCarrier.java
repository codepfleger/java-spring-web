package io.opentracing.contrib.spring.web.ws;

import io.opentracing.propagation.TextMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.transport.http.HttpUrlConnection;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Frank Pfleger (trasier.com)
 */
public class HttpHeadersCarrier implements TextMap {

    private static final Log log = LogFactory.getLog(HttpHeadersCarrier.class);

    private final HttpUrlConnection httpConnection;

    HttpHeadersCarrier(HttpUrlConnection httpConnection) {
        this.httpConnection = httpConnection;
    }

    @Override
    public void put(String key, String value) {
        try {
            httpConnection.addRequestHeader(key, value);
        } catch (IOException e) {
            log.error("Error adding request headers", e);
        }
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("Should be used only with tracer#inject()");
    }

}