package am.ik.blog.s3.web;

import am.ik.blog.S3Props;
import io.awspring.cloud.s3.S3Template;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.Duration;
import java.util.Set;

@RestController
public class S3Controller {

	private final S3Template s3Template;

	private final String bucketName;

	private final Duration presignedUrlExpiration;

	private final Set<String> allowedExtensions;

	public S3Controller(S3Template s3Template, S3Props s3Props) {
		this.s3Template = s3Template;
		this.bucketName = s3Props.backetName();
		this.presignedUrlExpiration = s3Props.presignedUrlExpiration();
		this.allowedExtensions = Set.copyOf(s3Props.allowedExtensions());
	}

	@PostMapping(path = "/tenants/{tenantId}/s3/presign")
	public PresignResponse presign(@PathVariable String tenantId, @RequestBody PresignRequest request) {
		String fileName = request.fileName();
		validateFileName(fileName);
		if (!this.s3Template.bucketExists(this.bucketName)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Bucket does not exist: " + this.bucketName);
		}
		String objectKey = tenantId + "/" + fileName;
		URL signedURL = this.s3Template.createSignedPutURL(this.bucketName, objectKey, this.presignedUrlExpiration);
		return new PresignResponse(signedURL);
	}

	private void validateFileName(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name must not be empty");
		}
		if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name: path traversal not allowed");
		}
		String extension = extractExtension(fileName);
		if (extension == null || !this.allowedExtensions.contains(extension.toLowerCase())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Invalid file extension. Allowed extensions: " + this.allowedExtensions);
		}
	}

	private String extractExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
			return null;
		}
		return fileName.substring(lastDotIndex + 1);
	}

	public record PresignRequest(String fileName) {
	}

	public record PresignResponse(URL url) {
	}

}
