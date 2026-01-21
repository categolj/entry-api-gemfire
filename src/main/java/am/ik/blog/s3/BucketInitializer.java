package am.ik.blog.s3;

import am.ik.blog.S3Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

@Component
public class BucketInitializer {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public BucketInitializer(S3Client s3Client, S3Props s3Props) {
		if (s3Props.createBucket()) {
			String bucketName = s3Props.backetName();
			if (bucketExists(s3Client, bucketName)) {
				logger.info("Bucket already exists: {}", bucketName);
				return;
			}
			logger.info("Creating bucket: {}", bucketName);
			// Create bucket
			s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
			// Set bucket policy to allow public read access
			String policyJson = """
					{
					  "Version": "2012-10-17",
					  "Statement": [
					    {
					      "Effect": "Allow",
					      "Principal": {"AWS": ["*"]},
					      "Action": ["s3:GetObject"],
					      "Resource": ["arn:aws:s3:::%s/*"]
					    }
					  ]
					}
					""".formatted(bucketName);
			s3Client.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucketName).policy(policyJson).build());
		}
	}

	private boolean bucketExists(S3Client s3Client, String bucketName) {
		try {
			s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
			return true;
		}
		catch (NoSuchBucketException e) {
			return false;
		}
	}

}
