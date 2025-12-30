package am.ik.blog;

import java.util.List;
import java.util.Map;
import org.apache.geode.management.GemFireProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "gemfire")
@Validated
public record GemfireProps(List<Endpoint> locators, Map<String, String> properties,
		Endpoint sniProxy) implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return GemFireProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "locators", "locators.empty", "Locators must not be empty");
	}

	public record Endpoint(String host, int port) {

		public static Endpoint valueOf(String hostAndPort) {
			String[] split = hostAndPort.split(":");
			if (split.length != 2) {
				throw new IllegalArgumentException(
						"Invalid locator format: " + hostAndPort + ". Expected format: host:port");
			}
			return new Endpoint(split[0], Integer.parseInt(split[1]));
		}
	}
}