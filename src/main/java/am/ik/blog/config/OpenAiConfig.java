package am.ik.blog.config;

import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenAiConfig {

	@Bean
	OpenAiHttpClientBuilderCustomizer openAiHttpClientBuilderCustomizer() {
		return builder -> {
			HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
			logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
			builder.interceptor(logging);
		};
	};

}
