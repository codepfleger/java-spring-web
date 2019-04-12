package io.opentracing.contrib.spring.web.interceptor.itest.boot;

import io.opentracing.example.test_service.TestTheServiceRequest;
import io.opentracing.example.test_service.TestTheServiceResponse;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

public class WeatherWsClient extends WebServiceGatewaySupport {

    public TestTheServiceResponse getWeather(int serverPort, String cityCode) {
        TestTheServiceRequest request = new TestTheServiceRequest();
        request.setFoo("whatup");
        return (TestTheServiceResponse) getWebServiceTemplate()
                .marshalSendAndReceive("http://localhost:" + serverPort + "/ws/weather", request,
                        new SoapActionCallback("http://opentracing.io/example/test-service/GetWeatherRequest"));
    }
}
