package io.opentracing.contrib.spring.web.ws;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.Source;
import java.util.List;

import static org.springframework.ws.test.client.RequestMatchers.payload;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

/**
 * @author Frank Pfleger (trasier.com)
 */
public class TracingWebServiceTemplateTest {

    private final MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager(), MockTracer.Propagator.TEXT_MAP);
    private MockWebServiceServer mockServer;
    private WebServiceTemplate webServiceTemplate;

    public TracingWebServiceTemplateTest() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

        webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setInterceptors(new ClientInterceptor[]{new TracingClientInterceptorAdapter(mockTracer)});
        webServiceTemplate.setMarshaller(marshaller);
        webServiceTemplate.setUnmarshaller(marshaller);

        mockServer = MockWebServiceServer.createServer(webServiceTemplate);
    }

    @Before
    public void before() {
        mockTracer.reset();
    }

    @After
    public void after() {
        mockServer.verify();
    }

    @Test
    public void testStandardTags() {
        Source requestPayload = new StringSource(
                "<ns2:getSomething xmlns:ns2=\"https://trasier.com/something\">" +
                        "<ns2:id>1</ns2:id>" +
                        "</ns2:getSomething>");

        final Source responsePayload = new StringSource(
                "<ns2:getSomethingResponse xmlns:ns2=\"https://trasier.com/something\">" +
                        "<ns2:id>1</ns2:id>" +
                        "<ns2:name>trasier</ns2:name>" +
                        "</ns2:getSomethingResponse>");

        mockServer
                .expect(payload(requestPayload))
                .andRespond(withPayload(responsePayload));

        webServiceTemplate.sendSourceAndReceive("/something", requestPayload, new SourceExtractor<Source>() {
            @Override
            public Source extractData(Source source) {
                return responsePayload;
            }
        });

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("getSomething", mockSpan.operationName());
        Assert.assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
    }

}