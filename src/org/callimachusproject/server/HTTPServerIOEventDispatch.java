/**
 * 
 */
package org.callimachusproject.server;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.codecs.HttpRequestParser;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HTTPServerIOEventDispatch extends
		DefaultServerIOEventDispatch {
	private Logger logger = LoggerFactory.getLogger(HTTPServerIOEventDispatch.class);
	private final HttpParams params;

	public HTTPServerIOEventDispatch(NHttpServiceHandler handler,
			HttpParams params) {
		super(handler, params);
		this.params = params;
	}

    @Override
    public void inputReady(IOSession session) {
        try {
            super.inputReady(session);
        } catch (RuntimeException ex) {
            session.shutdown();
            logger.error(ex.toString(), ex);
        }
    }

	@Override
	protected NHttpServerIOTarget createConnection(IOSession session) {
		return new DefaultNHttpServerConnection(session,
				createHttpRequestFactory(), createByteBufferAllocator(),
				this.params) {
			@Override
			protected NHttpMessageParser createRequestParser(
					SessionInputBuffer buffer,
					HttpRequestFactory requestFactory, HttpParams params) {
				return new HttpRequestParser(buffer, null,
						requestFactory, params) {
					@Override
					public HttpMessage parse() throws IOException,
							HttpException {
						return removeEntityIfNoContent(super.parse());
					}
				};
			}

			@Override
			public String toString() {
				return super.toString() + session.toString();
			}
		};
	}

	@Override
	protected HttpRequestFactory createHttpRequestFactory() {
		return new HttpRequestFactory() {
			public HttpRequest newHttpRequest(RequestLine requestline)
					throws MethodNotSupportedException {
				return new BasicHttpEntityEnclosingRequest(requestline);
			}

			public HttpRequest newHttpRequest(String method, String uri)
					throws MethodNotSupportedException {
				return new BasicHttpEntityEnclosingRequest(method, uri);
			};
		};
	}

	private HttpMessage removeEntityIfNoContent(HttpMessage msg) {
		if (msg != null) {
			HttpParams params = msg.getParams();
			params.setParameter("http.protocol.scheme", "http");
			msg.setParams(params);
		}
		if (msg instanceof HttpEntityEnclosingRequest
				&& !msg.containsHeader("Content-Length")
				&& !msg.containsHeader("Transfer-Encoding")) {
			HttpEntityEnclosingRequest body = (HttpEntityEnclosingRequest) msg;
			BasicHttpRequest req = new BasicHttpRequest(body.getRequestLine());
			req.setHeaders(body.getAllHeaders());
			req.setParams(msg.getParams());
			return req;
		}
		return msg;
	}
}