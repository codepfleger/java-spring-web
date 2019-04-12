package io.opentracing.contrib.spring.web.interceptor.itest.common.app;

import io.opentracing.example.test_service.TestTheServiceRequest;
import io.opentracing.example.test_service.TestTheServiceResponse;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class TestEndpoint {

    private static final String NAMESPACE_URI = "http://opentracing.io/example/test-service";

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "testTheServiceRequest")
    @ResponsePayload
    public TestTheServiceResponse testTheService(@RequestPayload TestTheServiceRequest request) {
        TestTheServiceResponse response = new TestTheServiceResponse();
        response.setBar("You said: " + request.getFoo());
        return response;
    }

}