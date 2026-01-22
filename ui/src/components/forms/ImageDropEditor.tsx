import React, { useState } from 'react';
import MDEditor from '@uiw/react-md-editor';
import { LoadingSpinner } from '../common';
import { api, ApiError } from '../../services';
import {
  isValidImageFile,
  generateUniqueFileName,
  uploadImageToS3,
  generateMarkdownImage,
  getPublicUrlFromPresigned,
  createFileWithName,
  ALLOWED_IMAGE_EXTENSIONS,
} from '../../utils/imageUpload';

interface ImageDropEditorProps {
  value: string;
  onChange: (value: string) => void;
  height?: number;
  tenantId: string;
  onUploadError?: (error: string) => void;
}

/**
 * MDEditor wrapped with drag & drop image upload functionality.
 * Handles the complete upload flow including validation, S3 upload,
 * and inserting the markdown image at the cursor position.
 */
export function ImageDropEditor({
  value,
  onChange,
  height = 400,
  tenantId,
  onUploadError,
}: ImageDropEditorProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  const handleImageUpload = async (file: File, cursorPosition?: number) => {
    // Validate file type
    if (!isValidImageFile(file)) {
      onUploadError?.(`Invalid file type. Allowed: ${ALLOWED_IMAGE_EXTENSIONS.join(', ')}`);
      return;
    }

    setIsUploading(true);

    try {
      // Generate unique file name
      const uniqueFileName = generateUniqueFileName(file.name);

      // Get presigned URL
      const presignedUrl = await api.getPresignedUrl(tenantId, uniqueFileName);

      // Upload to S3
      await uploadImageToS3(presignedUrl, file);

      // Get public URL (remove query parameters from presigned URL)
      const publicUrl = getPublicUrlFromPresigned(presignedUrl);

      // Generate markdown image syntax
      const markdownImage = '\n' + generateMarkdownImage(publicUrl, file.name) + '\n';

      // Insert markdown at cursor position or at the end
      const pos = cursorPosition !== undefined ? cursorPosition : value.length;
      const newContent = value.slice(0, pos) + markdownImage + value.slice(pos);
      onChange(newContent);
    } catch (error) {
      if (error instanceof ApiError) {
        const detail = error.problemDetail?.detail || error.message;
        onUploadError?.(`Upload failed: HTTP ${error.status}: ${detail}`);
      } else {
        onUploadError?.(error instanceof Error ? error.message : 'Failed to upload image');
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      // Get cursor position from the textarea
      const textarea = e.currentTarget.querySelector<HTMLTextAreaElement>('.w-md-editor-text-input');
      const cursorPosition = textarea?.selectionStart;
      void handleImageUpload(files[0], cursorPosition);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDragEnter = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    // Only set isDragging to false if we're leaving the drop zone entirely
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX;
    const y = e.clientY;
    if (x < rect.left || x >= rect.right || y < rect.top || y >= rect.bottom) {
      setIsDragging(false);
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLDivElement>) => {
    const items = e.clipboardData?.items;
    if (!items) return;

    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      if (item.type.startsWith('image/')) {
        e.preventDefault();
        e.stopPropagation();

        const file = item.getAsFile();
        if (file) {
          const textarea = e.currentTarget.querySelector<HTMLTextAreaElement>('.w-md-editor-text-input');
          const cursorPosition = textarea?.selectionStart;
          const pastedFile = createFileWithName(file);
          void handleImageUpload(pastedFile, cursorPosition);
        }
        return;
      }
    }
  };

  return (
    <div
      className={`relative rounded transition-all duration-200 ${
        isDragging ? 'ring-2 ring-black ring-offset-2 bg-gray-50' : ''
      }`}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onPaste={handlePaste}
    >
      <MDEditor
        value={value}
        onChange={(val) => onChange(val || '')}
        preview="live"
        height={height}
        data-color-mode="light"
      />
      {isDragging && !isUploading && (
        <div className="absolute inset-0 bg-gray-100 bg-opacity-50 flex items-center justify-center z-10 pointer-events-none border-2 border-dashed border-black rounded">
          <div className="flex flex-col items-center space-y-2 text-black">
            <svg className="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <span className="text-sm font-medium">Drop image here to upload</span>
          </div>
        </div>
      )}
      {isUploading && (
        <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-10">
          <div className="flex items-center space-x-2">
            <LoadingSpinner size="sm" />
            <span className="text-sm text-gray-600">Uploading image...</span>
          </div>
        </div>
      )}
    </div>
  );
}
