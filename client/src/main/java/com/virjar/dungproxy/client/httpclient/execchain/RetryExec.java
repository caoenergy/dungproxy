package com.virjar.dungproxy.client.httpclient.execchain;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.NonRepeatableRequestException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.util.Args;

/**
 * 重写{@link org.apache.http.impl.execchain},逻辑未改变.
 * 
 * 重写原因:原始{@link org.apache.http.impl.execchain}中的部分日志级别为info级别,但在爬虫中失败重试很正常,故降低日志级别，避免日志淹没
 * 
 */
public class RetryExec implements ClientExecChain {

    private final Log log = LogFactory.getLog(getClass());

    private final ClientExecChain requestExecutor;
    private final HttpRequestRetryHandler retryHandler;

    public RetryExec(
            final ClientExecChain requestExecutor,
            final HttpRequestRetryHandler retryHandler) {
        Args.notNull(requestExecutor, "HTTP request executor");
        Args.notNull(retryHandler, "HTTP request retry handler");
        this.requestExecutor = requestExecutor;
        this.retryHandler = retryHandler;
    }

    @Override
    public CloseableHttpResponse execute(
            final HttpRoute route,
            final HttpRequestWrapper request,
            final HttpClientContext context,
            final HttpExecutionAware execAware) throws IOException, HttpException {
        Args.notNull(route, "HTTP route");
        Args.notNull(request, "HTTP request");
        Args.notNull(context, "HTTP context");
        final Header[] origheaders = request.getAllHeaders();
        for (int execCount = 1;; execCount++) {
            try {
                return this.requestExecutor.execute(route, request, context, execAware);
            } catch (final IOException ex) {// 抛出异常考虑重试,没有抛出异常也要考虑重试
                if (execAware != null && execAware.isAborted()) {
                    this.log.debug("Request has been aborted");
                    throw ex;
                }
                if (retryHandler.retryRequest(ex, execCount, context)) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("I/O exception ("+ ex.getClass().getName() +
                                ") caught when processing request to "
                                + route +
                                ": "
                                + ex.getMessage());
                    }
                    if (this.log.isDebugEnabled()) {
                        this.log.debug(ex.getMessage(), ex);
                    }
                    if (!RequestEntityProxy.isRepeatable(request)) {
                        this.log.debug("Cannot retry non-repeatable request");
                        throw new NonRepeatableRequestException("Cannot retry request " +
                                "with a non-repeatable request entity", ex);
                    }
                    request.setHeaders(origheaders);
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Retrying request to " + route);
                    }
                } else {
                    if (ex instanceof NoHttpResponseException) {
                        final NoHttpResponseException updatedex = new NoHttpResponseException(
                                route.getTargetHost().toHostString() + " failed to respond");
                        updatedex.setStackTrace(ex.getStackTrace());
                        throw updatedex;
                    } else {
                        throw ex;
                    }
                }
            }
        }
    }
}
