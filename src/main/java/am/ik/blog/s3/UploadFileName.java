package am.ik.blog.s3;

import am.ik.yavi.input.ObjectInputValidator;
import am.ik.yavi.validator.Yavi;
import java.util.Locale;
import java.util.Set;

/**
 * The name of a file to upload to S3.
 */
public final class UploadFileName {

	public static ObjectInputValidator<UploadFileName> schema(Set<String> allowedExtensions) {
		return Yavi.schema()
			._string("fileName", c -> c.notBlank()
				.message("File name must not be empty")
				.predicate(fileName -> !isPathTraversal(fileName), "s3.fileName.pathTraversal",
						"Invalid file name: path traversal not allowed")
				.predicate(fileName -> hasAllowedExtension(fileName, allowedExtensions), "s3.fileName.extension",
						"Invalid file extension. Allowed extensions: " + allowedExtensions))
			.required("File name must not be empty")
			.fromMap(UploadFileName::new);
	}

	private static boolean isPathTraversal(String fileName) {
		return fileName.contains("..") || fileName.contains("/") || fileName.contains("\\");
	}

	private static boolean hasAllowedExtension(String fileName, Set<String> allowedExtensions) {
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
			return false;
		}
		return allowedExtensions.contains(fileName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT));
	}

	private final String value;

	private UploadFileName(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof UploadFileName that && this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return "UploadFileName[value=" + this.value + "]";
	}

}
