/**
 * Mapping from MIME types to file extensions.
 */
const MIME_TO_EXTENSION: Record<string, string> = {
  'image/png': 'png',
  'image/jpeg': 'jpg',
  'image/gif': 'gif',
  'image/webp': 'webp',
  'image/svg+xml': 'svg',
};

/**
 * Allowed image file extensions for upload.
 * Derived from MIME_TO_EXTENSION, with 'jpeg' as an alias for 'jpg'.
 */
export const ALLOWED_IMAGE_EXTENSIONS = [...new Set([...Object.values(MIME_TO_EXTENSION), 'jpeg'])];

/**
 * Create a file with a proper name from clipboard paste.
 * If the file already has a meaningful name with extension, return it as-is.
 * Otherwise, generate a name based on the MIME type.
 * @param file - The file from clipboard
 * @returns A file with a proper name
 */
export function createFileWithName(file: File): File {
  if (file.name && file.name !== 'image.png' && file.name.includes('.')) {
    return file;
  }
  const extension = MIME_TO_EXTENSION[file.type] || 'png';
  const generatedName = `pasted-image.${extension}`;
  return new File([file], generatedName, { type: file.type });
}

/**
 * Check if the file is a valid image file based on extension.
 * @param file - The file to validate
 * @returns true if the file has an allowed image extension
 */
export function isValidImageFile(file: File): boolean {
  const fileName = file.name.toLowerCase();
  const extension = fileName.split('.').pop();
  return extension !== undefined && ALLOWED_IMAGE_EXTENSIONS.includes(extension);
}

/**
 * Generate a unique file name by prepending a timestamp.
 * @param originalName - The original file name
 * @returns A unique file name with timestamp prefix
 */
export function generateUniqueFileName(originalName: string): string {
  const timestamp = Date.now();
  const sanitizedName = originalName.replace(/[^a-zA-Z0-9._-]/g, '_');
  return `${timestamp}_${sanitizedName}`;
}

/**
 * Upload an image to S3 using a presigned URL.
 * @param presignedUrl - The presigned URL for upload
 * @param file - The file to upload
 * @throws Error if the upload fails
 */
export async function uploadImageToS3(presignedUrl: string, file: File): Promise<void> {
  const response = await fetch(presignedUrl, {
    method: 'PUT',
    body: file,
    headers: {
      'Content-Type': file.type,
    },
  });

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.status} ${response.statusText}`);
  }
}

/**
 * Generate markdown image syntax.
 * @param url - The image URL
 * @param altText - Alternative text for the image
 * @returns Markdown image syntax string
 */
export function generateMarkdownImage(url: string, altText: string): string {
  return `![${altText}](${url})`;
}

/**
 * Extract the public URL from a presigned URL by removing query parameters.
 * @param presignedUrl - The presigned URL
 * @returns The public URL without query parameters
 */
export function getPublicUrlFromPresigned(presignedUrl: string): string {
  return presignedUrl.split('?')[0];
}
