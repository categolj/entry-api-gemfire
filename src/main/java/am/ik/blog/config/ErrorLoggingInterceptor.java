package am.ik.blog.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Interceptor that logs error responses from HTTP requests.
 */
public class ErrorLoggingInterceptor implements ClientHttpRequestInterceptor {

	private final Logger logger = LoggerFactory.getLogger(ErrorLoggingInterceptor.class);

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		ClientHttpResponse response = execution.execute(request, body);
		HttpStatusCode status = response.getStatusCode();
		if (status.isError() && status.value() != 404) {
			byte[] responseBody = response.getBody().readAllBytes();
			String bodyString = new String(responseBody, StandardCharsets.UTF_8);
			logger.error("action=http_client_error method={} uri={} status={} body={}", request.getMethod(),
					request.getURI(), status, bodyString);
			return new BufferedClientHttpResponse(response, responseBody);
		}
		return response;
	}

	private static class BufferedClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse delegate;

		private final byte[] body;

		BufferedClientHttpResponse(ClientHttpResponse delegate, byte[] body) {
			this.delegate = delegate;
			this.body = body;
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return delegate.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return delegate.getStatusText();
		}

		@Override
		public void close() {
			delegate.close();
		}

		@Override
		public InputStream getBody() throws IOException {
			return new ByteArrayInputStream(body);
		}

		@Override
		public HttpHeaders getHeaders() {
			return delegate.getHeaders();
		}

	}

}
