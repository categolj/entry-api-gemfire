package am.ik.blog.config;

import am.ik.blog.security.CompositeUserDetailsService;
import am.ik.blog.security.Privilege;
import am.ik.blog.tenant.MethodInvocationTenantAuthorizationManager;
import am.ik.blog.tenant.RequestTenantAuthorizationManager;
import am.ik.blog.tenant.TenantUserDetails;
import am.ik.blog.tenant.TenantUserDetailsService;
import am.ik.blog.tenant.TenantUserProps;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.Advisor;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Role;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.ReflectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableMethodSecurity(prePostEnabled = false)
@ImportRuntimeHints(SecurityConfig.RuntimeHints.class)
public class SecurityConfig {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	Advisor preAuthorize() {
		return AuthorizationManagerBeforeMethodInterceptor
			.preAuthorize(new MethodInvocationTenantAuthorizationManager());
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
			throws Exception {
		var listForTenant = new RequestTenantAuthorizationManager("entry", Privilege.LIST);
		var getForTenant = new RequestTenantAuthorizationManager("entry", Privilege.GET);
		var editForTenant = new RequestTenantAuthorizationManager("entry", Privilege.EDIT);
		var deleteForTenant = new RequestTenantAuthorizationManager("entry", Privilege.DELETE);
		var importForTenant = new RequestTenantAuthorizationManager("entry", Privilege.IMPORT);
		return http
		// @formatter:off
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST,   "/entries").hasAuthority("entry:edit")
				.requestMatchers(HttpMethod.PATCH,  "/entries/**").hasAuthority("entry:edit")
				.requestMatchers(HttpMethod.PUT,    "/entries/**").hasAuthority("entry:edit")
				.requestMatchers(HttpMethod.DELETE, "/entries/**").hasAuthority("entry:delete")
				.requestMatchers(HttpMethod.POST,   "/admin/import").hasAuthority("entry:import")
				.requestMatchers(HttpMethod.POST,   "/tenants/{tenantId}/webhook").permitAll()
				.requestMatchers(HttpMethod.GET,    "/tenants/{tenantId}/entries").access(listForTenant)
				.requestMatchers(HttpMethod.GET,    "/tenants/{tenantId}/categories").access(listForTenant)
				.requestMatchers(HttpMethod.GET,    "/tenants/{tenantId}/tag").access(listForTenant)
				.requestMatchers(HttpMethod.GET,    "/tenants/{tenantId}/entries/**").access(getForTenant)
				.requestMatchers(HttpMethod.POST,   "/tenants/{tenantId}/**").access(editForTenant)
				.requestMatchers(HttpMethod.PATCH,  "/tenants/{tenantId}/**").access(editForTenant)
				.requestMatchers(HttpMethod.PUT,    "/tenants/{tenantId}/**").access(editForTenant)
				.requestMatchers(HttpMethod.DELETE, "/tenants/{tenantId}/**").access(deleteForTenant)
				.requestMatchers(HttpMethod.POST,   "/tenants/{tenantId}/admin/import").access(importForTenant)
				.anyRequest().permitAll())
			// @formatter:on
			.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(new NoPopupBasicAuthenticationEntryPoint()))
			.userDetailsService(userDetailsService)
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.build();
	}

	@SuppressWarnings("deprecation")
	@Bean
	PasswordEncoder passwordEncoder() {
		return new ObservablePasswordEncoder(new DelegatingPasswordEncoder("bcrypt",
				Map.of("bcrypt", new BCryptPasswordEncoder(), "noop", NoOpPasswordEncoder.getInstance())));
	}

	@Bean
	CompositeUserDetailsService compositeUserDetailsService(SecurityProperties properties,
			PasswordEncoder passwordEncoder, TenantUserProps props) {
		InMemoryUserDetailsManager inMemoryUserDetailsManager = inMemoryUserDetailsManager(properties, passwordEncoder);
		TenantUserDetailsService tenantUserDetailsService = tenantUserDetailsService(props);
		return new CompositeUserDetailsService(List.of(inMemoryUserDetailsManager, tenantUserDetailsService));
	}

	InMemoryUserDetailsManager inMemoryUserDetailsManager(SecurityProperties properties,
			PasswordEncoder passwordEncoder) {
		SecurityProperties.User user = properties.getUser();
		List<GrantedAuthority> authorities = user.getRoles()
			.stream()
			.map(Privilege::fromRole)
			.flatMap(Collection::stream)
			.flatMap(p -> Stream.of(p.toAuthority("entry"), p.toAuthority("*", "entry")))
			.toList();
		String password = user.getPassword();
		return new InMemoryUserDetailsManager(User.withUsername(user.getName())
			.password(password.startsWith("{") ? password : passwordEncoder.encode(password))
			.authorities(authorities)
			.build());
	}

	TenantUserDetailsService tenantUserDetailsService(TenantUserProps props) {
		return new TenantUserDetailsService(props);
	}

	public static class RuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(org.springframework.aot.hint.RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
				.registerMethod(
						Objects.requireNonNull(
								ReflectionUtils.findMethod(TenantUserDetails.class, "valueOf", String.class)),
						ExecutableMode.INVOKE);
		}

	}

}
