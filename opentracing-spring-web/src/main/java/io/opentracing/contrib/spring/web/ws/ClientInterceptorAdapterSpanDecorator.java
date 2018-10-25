package io.opentracing.contrib.spring.web.ws;

import io.opentracing.Span;
import org.springframework.util.StringUtils;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapMessage;
import org.w3c.dom.Node;

import javax.xml.transform.dom.DOMSource;

/**
 * @author Frank Pfleger (trasier.com)
 */
public interface ClientInterceptorAdapterSpanDecorator {

    void onRequest(MessageContext messageContext, Span span);

    void onResponse(MessageContext messageContext, Span span);

    void onFault(MessageContext messageContext, Span span);

    void onCompletion(MessageContext messageContext, Span span);

    ClientInterceptorAdapterSpanDecorator STANDARD_LOGS = new ClientInterceptorAdapterSpanDecorator() {

        @Override
        public void onRequest(MessageContext messageContext, Span span) {
            String operationName = extractOperationName(messageContext);
            if (operationName != null) {
                span.setOperationName(operationName);
            }
        }

        @Override
        public void onResponse(MessageContext messageContext, Span span) {

        }

        @Override
        public void onFault(MessageContext messageContext, Span span) {

        }

        @Override
        public void onCompletion(MessageContext messageContext, Span span) {

        }

        private String extractOperationName(MessageContext messageContext) {
            if (messageContext.getRequest() instanceof SoapMessage) {
                SoapMessage soapMessage = (SoapMessage) messageContext.getRequest();

                String soapAction = soapMessage.getSoapAction();
                SoapBody body = soapMessage.getSoapBody();
                if (body.getPayloadSource() instanceof DOMSource) {
                    Node node = ((DOMSource) body.getPayloadSource()).getNode();
                    return node.getLocalName();
                } else if (!StringUtils.isEmpty(soapAction)) {
                    soapAction = soapAction.replaceAll("\"", "");
                    String[] soapActionArray = soapAction.split("/");
                    return soapActionArray[soapActionArray.length - 1];
                }
            }

            return null;
        }

    };

}