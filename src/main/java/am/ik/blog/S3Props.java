package am.ik.blog;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@ConfigurationProperties(prefix = "blog.s3")
public record S3Props(String backetName, @DefaultValue("false") boolean createBucket,
		@DefaultValue("10m") Duration presignedUrlExpiration, @DefaultValue( {
				"png", "jpg", "jpeg", "gif", "webp" }) List<String> allowedExtensions)
		implements
			Validator{

	@Override
	public boolean supports(Class<?> clazz) {
		return clazz == S3Props.class;
	}

	@Override
	public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "backetName", "backetName.empty", "backetName must not be empty");
	}
}
