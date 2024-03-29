/*
 * The MIT License
 *
 * Copyright 2017 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package demo;

import com.intuit.karate.Config;
import com.intuit.karate.FileUtils;
import com.intuit.karate.core.ScenarioContext;
import com.intuit.karate.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intuit.karate.http.Cookie.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 *
 * @author pthomas3
 */
public abstract class MockHttpClient extends HttpClient<HttpBody> {

    private static final Logger logger = LoggerFactory.getLogger(MockHttpClient.class);

    private URI uri;
    private MockHttpServletRequestBuilder requestBuilder;

    protected abstract Servlet getServlet(HttpRequestBuilder request);

    protected abstract ServletContext getServletContext();

    /**
     * this is guaranteed to be called if the zero-arg constructor is used, so
     * for advanced per-test set-up, over-ride this call-back and retrieve
     * custom data via config.getUserDefined() - refer to the documentation of
     * the 'configure userDefined' keyword
     */
    @Override
    public void configure(Config config, ScenarioContext context) {

    }

    @Override
    protected HttpBody getEntity(List<MultiPartItem> items, String mediaType) {
        return HttpBody.multiPart(items, mediaType);
    }

    @Override
    protected HttpBody getEntity(MultiValuedMap formFields, String mediaType) {
        return HttpBody.formFields(formFields, mediaType);
    }

    @Override
    protected HttpBody getEntity(InputStream stream, String mediaType) {
        return HttpBody.stream(stream, mediaType);
    }

    @Override
    protected HttpBody getEntity(String content, String mediaType) {
        return HttpBody.string(content, mediaType);
    }

    @Override
    protected void buildUrl(String url) {
        String method = request.getMethod();
        try {
            uri = new URI(url);
            requestBuilder = request(method, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void buildPath(String path) {
        String url = uri.toString();
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        buildUrl(url + path);
    }

    @Override
    protected void buildParam(String name, Object... values) {
        List<String> list = new ArrayList<>(values.length);
        for (Object o : values) {
            list.add(o == null ? null : o.toString());
        }
        requestBuilder.param(name, list.toArray(new String[]{}));
    }

    @Override
    protected void buildHeader(String name, Object value, boolean replace) {
        requestBuilder.header(name, value);
    }

    @Override
    protected void buildCookie(com.intuit.karate.http.Cookie c) {
        Cookie cookie = new Cookie(c.getName(), c.getValue());
        requestBuilder.cookie(cookie);
        for (Map.Entry<String, String> entry : c.entrySet()) {
            if (entry.getValue() != null) {
                switch (entry.getKey()) {
                    case DOMAIN:
                        cookie.setDomain(entry.getValue());
                        break;
                    case PATH:
                        cookie.setPath(entry.getValue());
                        break;
                }
            }
        }
        if (cookie.getDomain() == null) {
            cookie.setDomain(uri.getHost());
        }
    }

    @Override
    protected HttpResponse makeHttpRequest(HttpBody entity, ScenarioContext context) {
        logger.info("making mock http client request: {} - {}", request.getMethod(), getRequestUri());
        MockHttpServletRequest req = requestBuilder.buildRequest(getServletContext());
        byte[] bytes;
        if (entity != null) {
            bytes = entity.getBytes();
            req.setContentType(entity.getContentType());
          /*  if (entity.isMultiPart()) {
                for (MultiPartItem item : entity.getParts()) {
                    MockMultiPart part = new MockMultiPart(item);
                    req.addPart(part);
                    if (!part.isFile()) {
                        req.addParameter(part.getName(), part.getValue());
                    }
                }
            } else */if (entity.isUrlEncoded()) {
                req.addParameters(entity.getParameters());
            } else {
                req.setContent(bytes);
            }
        } else {
            bytes = null;
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        logRequest(req, bytes);
        long startTime = System.currentTimeMillis();
        try {
            getServlet(request).service(req, res);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }
            logger.error("mock servlet request failed: {}", message);
            throw new RuntimeException(e);
        }
        HttpResponse response = new HttpResponse(startTime, System.currentTimeMillis());
        bytes = res.getContentAsByteArray();
        logResponse(res, bytes);
        response.setUri(getRequestUri());
        response.setBody(bytes);
        if(getResponseBody(bytes, res.getContentType()).contains("errorMessage")) {
            response.setStatus(400);
        } else if (getRequestUri().contains("login")) {
            response.setStatus(201);
        } else {
            response.setStatus(res.getStatus());
        }
        for (Cookie c : res.getCookies()) {
            com.intuit.karate.http.Cookie cookie = new com.intuit.karate.http.Cookie(c.getName(), c.getValue());
            cookie.put(DOMAIN, c.getDomain());
            cookie.put(PATH, c.getPath());
            cookie.put(SECURE, c.getSecure() + "");
            cookie.put(MAX_AGE, c.getMaxAge() + "");
            cookie.put(VERSION, c.getVersion() + "");
            response.addCookie(cookie);
        }
        for (String headerName : res.getHeaderNames()) {
            response.putHeader(headerName, res.getHeaders(headerName));
        }
        return response;
    }

    @Override
    protected String getRequestUri() {
        return uri.toString();
    }

    private final AtomicInteger counter = new AtomicInteger();

    private void logRequest(MockHttpServletRequest req, byte[] bytes) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("++++++++++++++ before logging request");
        int id = counter.incrementAndGet();
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(id).append(" > ").append(req.getMethod()).append(' ')
                .append(req.getRequestURL()).append('\n');
        logRequestHeaders(sb, id, req);
        logBody(sb, bytes, req.getContentType());
        logger.debug(sb.toString());
        logger.debug("++++++++++++++ after logging request");
    }

    private void logResponse(MockHttpServletResponse res, byte[] bytes) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("++++++++++++++ before logging response");
        int id = counter.get();
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(id).append(" < ").append(res.getStatus()).append('\n');
        logResponseHeaders(sb, id, res);
        logBody(sb, bytes, res.getContentType());
        logger.debug(sb.toString());
        logger.debug("++++++++++++++ after logging response");
    }

    private static void logRequestHeaders(StringBuilder sb, int id, MockHttpServletRequest request) {
        Set<String> keys = new TreeSet(Collections.list(request.getHeaderNames()));
        for (String key : keys) {
            List<String> entries = Collections.list(request.getHeaders(key));
            sb.append(id).append(' ').append('>').append(' ')
                    .append(key).append(": ").append(entries.size() == 1 ? entries.get(0) : entries).append('\n');
        }
    }

    private static void logResponseHeaders(StringBuilder sb, int id, MockHttpServletResponse response) {
        Set<String> keys = new TreeSet(response.getHeaderNames());
        for (String key : keys) {
            List<String> entries = response.getHeaders(key);
            sb.append(id).append(' ').append('<').append(' ')
                    .append(key).append(": ").append(entries.size() == 1 ? entries.get(0) : entries).append('\n');
        }
    }

    private static void logBody(StringBuilder sb, byte[] bytes, String contentType) {
        if (bytes != null && HttpUtils.isPrintable(contentType)) {
            sb.append(FileUtils.toString(bytes));
        }
    }

    private static String getResponseBody(byte[] bytes, String contentType) {
        StringBuilder sb = new StringBuilder();

        if (bytes != null && HttpUtils.isPrintable(contentType)) {
            sb.append(FileUtils.toString(bytes));
        }
        return sb.toString();
    }

}
