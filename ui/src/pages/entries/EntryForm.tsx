import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link, useLocation } from 'react-router-dom';
import MDEditor from '@uiw/react-md-editor';
import { useTenant, useApi } from '../../hooks';
import { api } from '../../services';
import { LoadingSpinner, ErrorAlert, Button } from '../../components/common';
import { Input, Textarea, TagInput, CategoryInput } from '../../components/forms';
import { FrontMatter, PreviewState } from '../../types';
import { parseMarkdownWithFrontMatter, createMarkdownWithFrontMatter } from '../../utils';

interface EntryFormProps {
  mode: 'create' | 'edit';
}

export function EntryForm({ mode }: EntryFormProps) {
  const { tenant } = useTenant();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const entryId = id ? parseInt(id, 10) : 0;

  const [formData, setFormData] = useState({
    title: '',
    summary: '',
    categories: [] as string[],
    tags: [] as string[],
    content: '',
  });
  
  const [entryIdInput, setEntryIdInput] = useState('');
  
  const [markdownMode, setMarkdownMode] = useState(false);
  const [markdownContent, setMarkdownContent] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [updateTimestamp, setUpdateTimestamp] = useState(true);
  const [originalMarkdown, setOriginalMarkdown] = useState('');
  const [isRestoredFromPreview, setIsRestoredFromPreview] = useState(false);
  const [isLoadingExisting, setIsLoadingExisting] = useState(false);

  // Load existing entry for edit mode
  const {
    data: existingEntry,
    loading: loadingEntry,
    error: loadError,
  } = useApi(
    () => mode === 'edit' ? api.getEntry(tenant, entryId) : Promise.resolve(null),
    [tenant, entryId, mode],
    { immediate: mode === 'edit' }
  );

  // Check if we're returning from preview with state
  useEffect(() => {
    const state = location.state as PreviewState | undefined;
    if (state?.formData) {
      // Restore form data from navigation state
      setFormData(state.formData);
      setEntryIdInput(state.entryIdInput || '');
      setUpdateTimestamp(state.updateTimestamp ?? true);
      setOriginalMarkdown(state.originalMarkdown || '');
      setIsRestoredFromPreview(true);
      
      // Clear the state to prevent re-applying on refresh
      navigate(location.pathname, { replace: true });
    }
  }, [location.state, location.pathname, navigate]);

  // Populate form when editing
  useEffect(() => {
    // Skip if we have restored from preview
    if (isRestoredFromPreview) return;
    
    if (mode === 'edit' && existingEntry) {
      setFormData({
        title: existingEntry.frontMatter.title || '',
        summary: existingEntry.frontMatter.summary || '',
        categories: existingEntry.frontMatter.categories?.map(c => c.name) || [],
        tags: existingEntry.frontMatter.tags?.map(t => t.name) || [],
        content: existingEntry.content || '',
      });
      
      // Create initial markdown content
      const markdown = createMarkdownWithFrontMatter(existingEntry.frontMatter, existingEntry.content);
      setMarkdownContent(markdown);
      setOriginalMarkdown(markdown);
    } else if (mode === 'create') {
      // For new entries, set empty original markdown
      setOriginalMarkdown('');
    }
  }, [mode, existingEntry, isRestoredFromPreview]);

  // Update markdown content when updateTimestamp changes
  useEffect(() => {
    if (!markdownMode && existingEntry) {
      const frontMatter: FrontMatter = {
        title: formData.title,
        summary: formData.summary || undefined,
        categories: formData.categories.map(name => ({ name })),
        tags: formData.tags.map(name => ({ name })),
        // Only include date if it was explicitly set in the original
        ...(existingEntry.frontMatter.date && { date: existingEntry.frontMatter.date }),
        // Only include updated when explicitly keeping the old timestamp (checkbox unchecked)
        ...(mode === 'edit' && !updateTimestamp && (existingEntry.frontMatter.updated || existingEntry.updated.date) && { 
          updated: existingEntry.frontMatter.updated || existingEntry.updated.date 
        }),
      };
      setMarkdownContent(createMarkdownWithFrontMatter(frontMatter, formData.content));
    }
  }, [updateTimestamp, markdownMode, existingEntry, formData, mode]);

  const handleFieldChange = (field: keyof typeof formData, value: string | string[]) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    setSubmitError(null);
    
    // Update markdown content when form fields change
    if (!markdownMode) {
      const updatedFormData = { ...formData, [field]: value } as typeof formData;
      const frontMatter: FrontMatter = {
        title: updatedFormData.title,
        summary: updatedFormData.summary || undefined,
        categories: updatedFormData.categories.map(name => ({ name })),
        tags: updatedFormData.tags.map(name => ({ name })),
        // Only include date if it was explicitly set in the original
        ...(existingEntry?.frontMatter.date && { date: existingEntry.frontMatter.date }),
        // Only include updated when explicitly keeping the old timestamp (checkbox unchecked)
        ...(mode === 'edit' && !updateTimestamp && (existingEntry?.frontMatter.updated || existingEntry?.updated.date) && { 
          updated: existingEntry?.frontMatter.updated || existingEntry?.updated.date 
        }),
      };
      setMarkdownContent(createMarkdownWithFrontMatter(frontMatter, updatedFormData.content));
    }
  };

  const handleMarkdownChange = (value: string) => {
    setMarkdownContent(value);
    
    // Parse markdown and update form fields
    const { frontMatter, content } = parseMarkdownWithFrontMatter(value);
    setFormData(prev => ({
      ...prev,
      title: frontMatter.title || prev.title,
      summary: frontMatter.summary || prev.summary,
      categories: frontMatter.categories?.map(c => c.name) || prev.categories,
      tags: frontMatter.tags?.map(t => t.name) || prev.tags,
      content: content || prev.content,
    }));
  };

  const handleLoadExisting = async () => {
    if (!entryIdInput.trim()) return;

    const targetId = parseInt(entryIdInput.trim(), 10);
    if (isNaN(targetId)) {
      setSubmitError('Invalid Entry ID');
      return;
    }

    setIsLoadingExisting(true);
    setSubmitError(null);

    try {
      const entry = await api.getEntry(tenant, targetId);
      setFormData({
        title: entry.frontMatter.title || '',
        summary: entry.frontMatter.summary || '',
        categories: entry.frontMatter.categories?.map(c => c.name) || [],
        tags: entry.frontMatter.tags?.map(t => t.name) || [],
        content: entry.content || '',
      });

      // Update markdown content as well
      const markdown = createMarkdownWithFrontMatter(entry.frontMatter, entry.content);
      setMarkdownContent(markdown);
      setOriginalMarkdown(markdown);
    } catch (error) {
      setSubmitError(`Entry with ID ${targetId} not found`);
    } finally {
      setIsLoadingExisting(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    // Navigate to preview page
    const previewState = {
      mode,
      formData,
      entryIdInput,
      updateTimestamp,
      existingEntry,
      originalMarkdown
    };

    const previewPath = mode === 'create' 
      ? `/console/${tenant}/entries/new/preview`
      : `/console/${tenant}/entries/${entryId}/edit/preview`;
    
    navigate(previewPath, { state: previewState });
  };


  if (mode === 'edit' && loadingEntry) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (mode === 'edit' && loadError) {
    return (
      <div className="px-4 py-3 sm:px-0">
        <ErrorAlert message={loadError} />
        <div className="mt-3">
          <Link to={`/console/${tenant}`}>
            <Button variant="secondary">Back to Entries</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="px-4 py-3 sm:px-0">
      {/* Header */}
      <div className="mb-4">
        <nav className="flex" aria-label="Breadcrumb">
          <ol className="flex items-center space-x-3">
            <li>
              <Link to={`/console/${tenant}`} className="text-gray-400 hover:text-gray-500 text-sm">
                Entries
              </Link>
            </li>
            <li>
              <span className="text-gray-400">/</span>
            </li>
            <li>
              <span className="text-gray-500 text-sm">
                {mode === 'create' ? 'Create New Entry' : `Edit Entry ${entryId}`}
              </span>
            </li>
          </ol>
        </nav>
        <h1 className="mt-1 text-xl font-bold text-gray-900">
          {mode === 'create' ? 'Create New Entry' : 'Edit Entry'}
        </h1>
      </div>

      {/* Mode Toggle */}
      <div className="mb-4 bg-white shadow rounded-lg p-3">
        <div className="flex items-center space-x-4">
          <span className="text-sm font-medium text-gray-700">Edit Mode:</span>
          <div className="flex rounded-lg border border-gray-300">
            <button
              type="button"
              onClick={() => setMarkdownMode(false)}
              className={`px-4 py-2 text-sm font-medium rounded-l-lg ${
                !markdownMode
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              Form Editor
            </button>
            <button
              type="button"
              onClick={() => {
                setMarkdownMode(true);
                if (!markdownContent) {
                  const frontMatter: FrontMatter = {
                    title: formData.title,
                    summary: formData.summary || undefined,
                    categories: formData.categories.map(name => ({ name })),
                    tags: formData.tags.map(name => ({ name })),
                  };
                  setMarkdownContent(createMarkdownWithFrontMatter(frontMatter, formData.content));
                }
              }}
              className={`px-4 py-2 text-sm font-medium rounded-r-lg border-l border-gray-300 ${
                markdownMode
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              Markdown Editor
            </button>
          </div>
        </div>
      </div>

      {/* Error Display */}
      {submitError && (
        <div className="mb-4">
          <ErrorAlert message={submitError} onDismiss={() => setSubmitError(null)} />
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        {markdownMode ? (
          /* Markdown Editor */
          <>
            {mode === 'create' && (
              <div className="bg-white shadow rounded-lg p-4 mb-4">
                <h3 className="text-lg font-medium text-gray-900 mb-3">Entry ID</h3>
                <div className="flex items-end space-x-2">
                  <div className="w-80">
                    <Input
                      label="Entry ID (Optional)"
                      type="number"
                      value={entryIdInput}
                      onChange={(e) => setEntryIdInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && entryIdInput.trim() && !isLoadingExisting) {
                          e.preventDefault();
                          void handleLoadExisting();
                        }
                      }}
                      placeholder="Leave empty for auto-generated ID"
                    />
                  </div>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => void handleLoadExisting()}
                    disabled={!entryIdInput.trim() || isLoadingExisting}
                    loading={isLoadingExisting}
                  >
                    Load
                  </Button>
                </div>
              </div>
            )}
            <div className="bg-white shadow rounded-lg p-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Markdown Content
                </label>
                <MDEditor
                  value={markdownContent}
                  onChange={(val) => handleMarkdownChange(val || '')}
                  preview="live"
                  height={600}
                  data-color-mode="light"
                />
              </div>
              <p className="mt-2 text-sm text-gray-500">
                Edit the complete entry including front matter and content in markdown format with live preview.
              </p>
            </div>
          </>
        ) : (
          /* Form Editor */
          <>
            {/* Basic Information */}
            <div className="bg-white shadow rounded-lg p-4">
              <h3 className="text-lg font-medium text-gray-900 mb-3">Basic Information</h3>
              <div className="space-y-3">
                <Input
                  label="Title *"
                  type="text"
                  value={formData.title}
                  onChange={(e) => handleFieldChange('title', e.target.value)}
                  placeholder="Enter entry title"
                  required
                />
                <Textarea
                  label="Summary"
                  value={formData.summary}
                  onChange={(e) => handleFieldChange('summary', e.target.value)}
                  placeholder="Brief description of the entry"
                  rows={2}
                />
                {mode === 'create' && (
                  <div className="flex items-end space-x-2">
                    <div className="w-80">
                      <Input
                        label="Entry ID (Optional)"
                        type="number"
                        value={entryIdInput}
                        onChange={(e) => setEntryIdInput(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' && entryIdInput.trim() && !isLoadingExisting) {
                            e.preventDefault();
                            void handleLoadExisting();
                          }
                        }}
                        placeholder="Leave empty for auto-generated ID"
                      />
                    </div>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => void handleLoadExisting()}
                      disabled={!entryIdInput.trim() || isLoadingExisting}
                      loading={isLoadingExisting}
                    >
                      Load
                    </Button>
                  </div>
                )}
              </div>
            </div>

            {/* Categories and Tags */}
            <div className="bg-white shadow rounded-lg p-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">Categories and Tags</h3>
              <p className="text-sm text-gray-600 mb-3">
                Organize your entry with categories and tags for better discoverability.
              </p>
              <div className="space-y-4">
                <CategoryInput
                  label="Categories"
                  value={formData.categories}
                  onChange={(categories) => handleFieldChange('categories', categories)}
                />
                <TagInput
                  label="Tags"
                  value={formData.tags}
                  onChange={(tags) => handleFieldChange('tags', tags)}
                />
              </div>
            </div>

            {/* Content */}
            <div className="bg-white shadow rounded-lg p-4">
              <h3 className="text-lg font-medium text-gray-900 mb-3">Content</h3>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Markdown Content *
                </label>
                <MDEditor
                  value={formData.content}
                  onChange={(val) => handleFieldChange('content', val || '')}
                  preview="live"
                  height={400}
                  data-color-mode="light"
                />
              </div>
              <p className="mt-2 text-sm text-gray-500">
                Write your content using Markdown syntax with live preview.
              </p>
            </div>
          </>
        )}

        {/* Actions */}
        <div className="flex justify-between items-center">
          <div className="flex items-center space-x-4">
            <Link to={`/console/${tenant}`}>
              <Button variant="secondary" type="button">
                Cancel
              </Button>
            </Link>
            {mode === 'edit' && (
              <label className="flex items-center space-x-2 text-sm text-gray-600">
                <input
                  type="checkbox"
                  checked={updateTimestamp}
                  onChange={(e) => setUpdateTimestamp(e.target.checked)}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span>Update timestamp</span>
              </label>
            )}
          </div>
          <div className="flex space-x-3">
            <Button
              type="submit"
              disabled={!formData.title.trim() || !formData.content.trim()}
            >
              {mode === 'create' ? 'Preview & Create' : 'Preview & Update'}
            </Button>
          </div>
        </div>
      </form>

    </div>
  );
}