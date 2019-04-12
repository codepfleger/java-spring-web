package io.opentracing.contrib.spring.web.interceptor.itest.boot;

import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TracingBeansConfiguration;
import io.opentracing.example.test_service.TestTheServiceResponse;
import io.opentracing.mock.MockSpan;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.contrib.spring.web.interceptor.itest.common.AbstractBaseITests;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import java.util.List;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SpringBootConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class BootITest extends AbstractBaseITests {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private WebServiceGatewaySupport webServiceGatewaySupport;

    @LocalServerPort
    private int serverPort;

    @Override
    protected String getUrl(String path) {
        return "http://localhost:" + serverPort + path;
    }

    @Override
    protected TestRestTemplate getRestTemplate() {
        return testRestTemplate;
    }

    private WebServiceGatewaySupport getWebService() {
        return webServiceGatewaySupport;
    }

    @Test
    public void testWS() throws Exception {
        {
            ((WeatherWsClient) getWebService()).getWeather(serverPort, "Bern");
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan childSpan = mockSpans.get(0);
        MockSpan parentSpan = mockSpans.get(1);
        Assert.assertEquals("localSpan", parentSpan.operationName());
        Assert.assertEquals(childSpan.context().traceId(), parentSpan.context().traceId());
        Assert.assertEquals(childSpan.parentId(), parentSpan.context().spanId());

    }
}
