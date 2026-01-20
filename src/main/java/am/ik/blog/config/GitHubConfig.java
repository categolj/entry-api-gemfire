package am.ik.blog.config;

import am.ik.blog.GitHubProps;
import am.ik.blog.github.Committer;
import am.ik.blog.github.GitCommit;
import am.ik.blog.github.GitCommitter;
import am.ik.blog.github.GitHubClient;
import am.ik.blog.github.GitHubUserContentClient;
import am.ik.blog.github.Parent;
import am.ik.blog.github.Tree;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration(proxyBeanMethods = false)
@Import(GitHubConfig.GithubTenantsHttpServiceRegistrar.class)
@ImportRuntimeHints(GitHubConfig.RuntimeHints.class)
@ImportHttpServices(group = "github", types = GitHubClient.class)
@ImportHttpServices(group = "githubusercontent", types = GitHubUserContentClient.class)
class GitHubConfig {

	final Predicate<HttpStatusCode> allwaysTrueStatusPredicate = __ -> true;

	final RestClient.ResponseSpec.ErrorHandler noOpErrorHandler = (req, res) -> {
	};

	@Bean
	RestClientHttpServiceGroupConfigurer githubRestClientHttpServiceGroupConfigurer(GitHubProps props) {
		ErrorLoggingInterceptor errorLoggingInterceptor = new ErrorLoggingInterceptor();
		return groups -> {
			groups.filterByName("github").forEachClient((_, builder) -> {
				builder.baseUrl(props.getApiUrl())
					.defaultHeader(HttpHeaders.AUTHORIZATION, "token %s".formatted(props.getAccessToken()))
					.defaultStatusHandler(allwaysTrueStatusPredicate, noOpErrorHandler)
					.requestInterceptor(errorLoggingInterceptor);
			});
			Map<String, GitHubProps> tenants = props.getTenants();
			if (!CollectionUtils.isEmpty(tenants)) {
				tenants.forEach((tenantId, tenantProps) -> {
					groups.filterByName("github.%s".formatted(tenantId)).forEachClient((_, builder) -> {
						builder.baseUrl(props.getApiUrl())
							.defaultHeader(HttpHeaders.AUTHORIZATION,
									"token %s".formatted(tenantProps.getAccessToken()))
							.defaultStatusHandler(allwaysTrueStatusPredicate, noOpErrorHandler)
							.requestInterceptor(errorLoggingInterceptor);
					});
				});
			}
			groups.filterByName("githubusercontent").forEachClient((_, builder) -> {
				builder.baseUrl("https://raw.githubusercontent.com")
					.defaultStatusHandler(allwaysTrueStatusPredicate, noOpErrorHandler)
					.defaultHeader(HttpHeaders.AUTHORIZATION, "token %s".formatted(props.getAccessToken()))
					.requestInterceptor(errorLoggingInterceptor);
			});
		};
	}

	static class RuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(org.springframework.aot.hint.RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
				.registerConstructor(GitCommit.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(GitCommitter.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(Committer.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(Parent.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE)
				.registerConstructor(Tree.class.getDeclaredConstructors()[0], ExecutableMode.INVOKE);
		}

	}

	static class GithubTenantsHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

		private final Logger logger = LoggerFactory.getLogger(getClass());

		@Nullable private Set<String> githubTenantIds;

		@Override
		public void setEnvironment(Environment environment) {
			super.setEnvironment(environment);
			Binder binder = Binder.get(environment);
			var bind = binder.bindOrCreate(ConfigurationPropertyName.of("blog.github.tenants"),
					Bindable.mapOf(String.class, GitHubProps.class), null);
			this.githubTenantIds = bind.keySet();
		}

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata importingClassMetadata) {
			if (CollectionUtils.isEmpty(this.githubTenantIds)) {
				logger.info("No github tenants configured");
				return;
			}
			this.githubTenantIds
				.forEach(tenantId -> registry.forGroup("github.%s".formatted(tenantId)).register(GitHubClient.class));
		}

	}

}
