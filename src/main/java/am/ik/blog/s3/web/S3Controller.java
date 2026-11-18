package am.ik.blog.s3.web;

import am.ik.blog.S3Props;
import am.ik.blog.json.JsonNodeReader;
import am.ik.blog.s3.UploadFileName;
import am.ik.yavi.core.ValueValidator;
import io.awspring.cloud.s3.S3Template;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.net.URL;
import java.time.Duration;
import java.util.Set;

@RestController
public class S3Controller {

	private final S3Template s3Template;

	private final String bucketName;

	private final Duration presignedUrlExpiration;

	private final ValueValidator<JsonNode, UploadFileName> fileNameValidator;

	public S3Controller(S3Template s3Template, S3Props s3Props) {
		this.s3Template = s3Template;
		this.bucketName = s3Props.backetName();
		this.presignedUrlExpiration = s3Props.presignedUrlExpiration();
		Set<String> allowedExtensions = Set.copyOf(s3Props.allowedExtensions());
		this.fileNameValidator = UploadFileName.schema(allowedExtensions).readingWith(JsonNodeReader.INSTANCE);
	}

	@PostMapping(path = "/tenants/{tenantId}/s3/presign")
	public PresignResponse presign(@PathVariable String tenantId, @RequestBody JsonNode body) {
		UploadFileName fileName = this.fileNameValidator.validated(body);
		if (!this.s3Template.bucketExists(this.bucketName)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Bucket does not exist: " + this.bucketName);
		}
		String objectKey = tenantId + "/" + fileName.value();
		URL signedURL = this.s3Template.createSignedPutURL(this.bucketName, objectKey, this.presignedUrlExpiration);
		return new PresignResponse(signedURL);
	}

	public record PresignResponse(URL url) {
	}

}
