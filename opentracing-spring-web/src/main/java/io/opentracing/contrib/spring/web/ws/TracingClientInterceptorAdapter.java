package io.opentracing.contrib.spring.web.ws;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpUrlConnection;

import java.util.Collections;
import java.util.List;

/**
 * @author Frank Pfleger (trasier.com)
 */
public class TracingClientInterceptorAdapter extends ClientInterceptorAdapter {

    private static final String DEFAULT_OPERATION_NAME = "Spring-WS";

    private final Tracer tracer;
    private final List<ClientInterceptorAdapterSpanDecorator> decorators;


    public TracingClientInterceptorAdapter() {
        this(GlobalTracer.get(), Collections.singletonList(ClientInterceptorAdapterSpanDecorator.STANDARD_LOGS));
    }

    public TracingClientInterceptorAdapter(Tracer tracer, List<ClientInterceptorAdapterSpanDecorator> decorators) {
        this.tracer = tracer;
        this.decorators = decorators;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        Scope scope = tracer.buildSpan(DEFAULT_OPERATION_NAME)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .startActive(true);
        Span span = scope.span();

        for (ClientInterceptorAdapterSpanDecorator decorator : decorators) {
            try {
                decorator.onRequest(messageContext, span);
            } catch (RuntimeException e) {
                logger.error("Exception during decorating span", e);
            }
        }

        TransportContext context = TransportContextHolder.getTransportContext();
        if (context.getConnection() instanceof HttpUrlConnection) {
            final HttpUrlConnection httpConnection = (HttpUrlConnection) context.getConnection();
            tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new HttpHeadersCarrier(httpConnection));
        }

        return super.handleRequest(messageContext);
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        Span span = tracer.activeSpan();

        for (ClientInterceptorAdapterSpanDecorator decorator : decorators) {
            try {
                decorator.onResponse(messageContext, span);
            } catch (RuntimeException e) {
                logger.error("Exception during decorating span", e);
            }
        }

        return super.handleResponse(messageContext);
    }

    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        Span span = tracer.activeSpan();

        for (ClientInterceptorAdapterSpanDecorator decorator : decorators) {
            try {
                decorator.onFault(messageContext, span);
            } catch (RuntimeException e) {
                logger.error("Exception during decorating span", e);
            }
        }

        return super.handleFault(messageContext);
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        Span span = tracer.activeSpan();

        for (ClientInterceptorAdapterSpanDecorator decorator : decorators) {
            try {
                decorator.onCompletion(messageContext, span);
            } catch (RuntimeException e) {
                logger.error("Exception during decorating span", e);
            }
        }

        tracer.scopeManager().active().close();

        super.afterCompletion(messageContext, ex);
    }

}