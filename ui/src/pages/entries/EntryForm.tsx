import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link, useLocation } from 'react-router-dom';
import { useTenant, useApi, useDraft } from '../../hooks';
import { api, ApiError } from '../../services';
import { LoadingSpinner, ErrorAlert, Button, DraftBanner } from '../../components/common';
import { Input, Textarea, TagInput, CategoryInput, ImageDropEditor } from '../../components/forms';
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
  const [updateTimestamp, setUpdateTimestamp] = useState(false);
  const [originalMarkdown, setOriginalMarkdown] = useState('');
  const [isRestoredFromPreview, setIsRestoredFromPreview] = useState(false);
  const [isRestoredFromDraft, setIsRestoredFromDraft] = useState(false);
  const [isLoadingExisting, setIsLoadingExisting] = useState(false);
  const [isSummarizing, setIsSummarizing] = useState(false);
  const [isLoadingTemplate, setIsLoadingTemplate] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const isFormBusy = isLoadingExisting || isSummarizing || isLoadingTemplate;

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

  const draftKey = mode === 'create'
    ? `entryDraft_${tenant}_create`
    : `entryDraft_${tenant}_edit_${entryId}`;

  const draft = useDraft({
    key: draftKey,
    enabled: !loadingEntry && !isLoadingExisting,
  });

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
    // Skip if we have restored from preview or draft
    if (isRestoredFromPreview || isRestoredFromDraft) return;

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
  }, [mode, existingEntry, isRestoredFromPreview, isRestoredFromDraft]);

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

  // Restore draft from sessionStorage
  useEffect(() => {
    // Skip if returning from preview (highest priority)
    // Check both the state flag and the actual location.state to handle timing
    const previewState = location.state as PreviewState | undefined;
    if (isRestoredFromPreview || previewState?.formData) return;

    // Skip if still loading in edit mode
    if (mode === 'edit' && loadingEntry) return;

    const restoredDraft = draft.restore();
    if (restoredDraft) {
      setFormData(restoredDraft.formData);
      setEntryIdInput(restoredDraft.entryIdInput || '');
      setMarkdownMode(restoredDraft.markdownMode || false);
      setUpdateTimestamp(restoredDraft.updateTimestamp ?? true);
      setIsRestoredFromDraft(true);
    }
  }, [mode, loadingEntry, isRestoredFromPreview, location.state, draft]);

  // Auto-save draft to sessionStorage
  useEffect(() => {
    if (loadingEntry || isLoadingExisting) return;

    // Skip if form is empty in create mode
    const isEmpty = !formData.title.trim() && !formData.content.trim() && !entryIdInput.trim();
    if (mode === 'create' && isEmpty) return;

    draft.save({
      formData,
      entryIdInput,
      markdownMode,
      updateTimestamp,
    });
  }, [formData, entryIdInput, markdownMode, updateTimestamp, loadingEntry, isLoadingExisting, mode, draft]);

  const resetForm = () => {
    if (mode === 'create') {
      setFormData({
        title: '',
        summary: '',
        categories: [],
        tags: [],
        content: '',
      });
      setEntryIdInput('');
      setMarkdownMode(false);
      setUpdateTimestamp(true);
    } else if (existingEntry) {
      // Reset to existing entry data
      setFormData({
        title: existingEntry.frontMatter.title || '',
        summary: existingEntry.frontMatter.summary || '',
        categories: existingEntry.frontMatter.categories?.map(c => c.name) || [],
        tags: existingEntry.frontMatter.tags?.map(t => t.name) || [],
        content: existingEntry.content || '',
      });
    }
  };

  const handleDiscardDraft = () => {
    draft.clear();
    setIsRestoredFromDraft(false);
    resetForm();
  };

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
      if (error instanceof ApiError) {
        const detail = error.problemDetail?.detail || error.message;
        setSubmitError(`HTTP ${error.status}: ${detail}`);
      } else {
        setSubmitError(`Entry with ID ${targetId} not found`);
      }
    } finally {
      setIsLoadingExisting(false);
    }
  };

  const handleAutoSummarize = async () => {
    if (!formData.content.trim()) {
      setSubmitError('Content is required to generate summary');
      return;
    }

    setIsSummarizing(true);
    setSubmitError(null);

    try {
      const summary = await api.summarize(formData.content);
      handleFieldChange('summary', summary);
    } catch (error) {
      if (error instanceof ApiError) {
        const detail = error.problemDetail?.detail || error.message;
        setSubmitError(`HTTP ${error.status}: ${detail}`);
      } else {
        setSubmitError(error instanceof Error ? error.message : 'Failed to generate summary');
      }
    } finally {
      setIsSummarizing(false);
    }
  };

  const handleLoadTemplate = async () => {
    setIsLoadingTemplate(true);
    setSubmitError(null);

    try {
      const templateMarkdown = await api.getTemplate();
      const { frontMatter, content } = parseMarkdownWithFrontMatter(templateMarkdown);
      setFormData({
        title: frontMatter.title || '',
        summary: frontMatter.summary || '',
        categories: frontMatter.categories?.map(c => c.name) || [],
        tags: frontMatter.tags?.map(t => t.name) || [],
        content: content || '',
      });
      setMarkdownContent(templateMarkdown);
    } catch (error) {
      if (error instanceof ApiError) {
        const detail = error.problemDetail?.detail || error.message;
        setSubmitError(`HTTP ${error.status}: ${detail}`);
      } else {
        setSubmitError(error instanceof Error ? error.message : 'Failed to load template');
      }
    } finally {
      setIsLoadingTemplate(false);
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
    <div>
      {/* Header */}
      <div className="mb-6">
        <nav className="flex" aria-label="Breadcrumb">
          <ol className="flex items-center space-x-2 text-sm">
            <li>
              <Link to={`/console/${tenant}`} className="text-gray-500 hover:text-black">
                Entries
              </Link>
            </li>
            <li>
              <span className="text-gray-300">/</span>
            </li>
            <li>
              <span className="text-gray-500">
                {mode === 'create' ? 'Create New Entry' : `Edit Entry ${entryId}`}
              </span>
            </li>
          </ol>
        </nav>
        <h1 className="mt-2 text-xl font-bold text-black">
          {mode === 'create' ? '## Create New Entry' : '## Edit Entry'}
        </h1>
      </div>

      {/* Mode Toggle */}
      <div className="mb-6 border border-gray-200 p-4">
        <div className="flex items-center space-x-4">
          <span className="text-sm font-medium text-black">Edit Mode:</span>
          <div className="flex border border-gray-300">
            <button
              type="button"
              onClick={() => setMarkdownMode(false)}
              disabled={isFormBusy}
              className={`px-4 py-2 text-sm font-medium disabled:opacity-40 disabled:cursor-not-allowed ${
                !markdownMode
                  ? 'bg-black text-white'
                  : 'bg-white text-black hover:bg-gray-50'
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
              disabled={isFormBusy}
              className={`px-4 py-2 text-sm font-medium border-l border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed ${
                markdownMode
                  ? 'bg-black text-white'
                  : 'bg-white text-black hover:bg-gray-50'
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

      {/* Upload Error Display */}
      {uploadError && (
        <div className="mb-4">
          <ErrorAlert message={uploadError} onDismiss={() => setUploadError(null)} />
        </div>
      )}

      {/* Draft Restored Banner */}
      {draft.status === 'restored' && draft.restoredAt && (
        <DraftBanner
          restoredAt={draft.restoredAt}
          onDiscard={handleDiscardDraft}
          onDismiss={draft.dismiss}
        />
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {markdownMode ? (
          /* Markdown Editor */
          <>
            {mode === 'create' && (
              <div className="border border-gray-200 p-4 mb-4">
                <h3 className="text-base font-medium text-black mb-3">Entry ID</h3>
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
                      disabled={isFormBusy}
                    />
                  </div>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => void handleLoadExisting()}
                    disabled={!entryIdInput.trim() || isFormBusy}
                    loading={isLoadingExisting}
                  >
                    Load
                  </Button>
                </div>
              </div>
            )}
            <div className={`border border-gray-200 p-4 ${isFormBusy ? 'opacity-60 pointer-events-none' : ''}`}>
              <div>
                <label className="block text-sm font-medium text-black mb-2">
                  Markdown Content
                </label>
                <ImageDropEditor
                  value={markdownContent}
                  onChange={handleMarkdownChange}
                  height={600}
                  tenantId={tenant}
                  onUploadError={setUploadError}
                />
              </div>
              <p className="mt-2 text-sm text-gray-500">
                Edit the complete entry including front matter and content in markdown format with live preview. Drag and drop or paste images to upload.
              </p>
            </div>
          </>
        ) : (
          /* Form Editor */
          <>
            {/* Basic Information */}
            <div className="border border-gray-200 p-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-base font-medium text-black">Basic Information</h3>
                {mode === 'create' && (
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => void handleLoadTemplate()}
                    disabled={isFormBusy}
                    loading={isLoadingTemplate}
                  >
                    Load Template
                  </Button>
                )}
              </div>
              <div className="space-y-4">
                <Input
                  label="Title *"
                  type="text"
                  value={formData.title}
                  onChange={(e) => handleFieldChange('title', e.target.value)}
                  placeholder="Enter entry title"
                  required
                  disabled={isFormBusy}
                />
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="block text-sm font-medium text-black">Summary</label>
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={() => void handleAutoSummarize()}
                      disabled={isSummarizing || !formData.content.trim()}
                      loading={isSummarizing}
                    >
                      Auto Generate
                    </Button>
                  </div>
                  <Textarea
                    value={formData.summary}
                    onChange={(e) => handleFieldChange('summary', e.target.value)}
                    placeholder="Brief description of the entry"
                    rows={2}
                    disabled={isFormBusy}
                  />
                </div>
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
                        disabled={isFormBusy}
                      />
                    </div>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => void handleLoadExisting()}
                      disabled={!entryIdInput.trim() || isFormBusy}
                      loading={isLoadingExisting}
                    >
                      Load
                    </Button>
                  </div>
                )}
              </div>
            </div>

            {/* Categories and Tags */}
            <div className="border border-gray-200 p-4">
              <h3 className="text-base font-medium text-black mb-2">Categories and Tags</h3>
              <p className="text-sm text-gray-500 mb-4">
                Organize your entry with categories and tags for better discoverability.
              </p>
              <div className="space-y-4">
                <CategoryInput
                  label="Categories"
                  value={formData.categories}
                  onChange={(categories) => handleFieldChange('categories', categories)}
                  disabled={isFormBusy}
                />
                <TagInput
                  label="Tags"
                  value={formData.tags}
                  onChange={(tags) => handleFieldChange('tags', tags)}
                  disabled={isFormBusy}
                />
              </div>
            </div>

            {/* Content */}
            <div className={`border border-gray-200 p-4 ${isFormBusy ? 'opacity-60 pointer-events-none' : ''}`}>
              <h3 className="text-base font-medium text-black mb-4">Content</h3>
              <div>
                <label className="block text-sm font-medium text-black mb-2">
                  Markdown Content *
                </label>
                <ImageDropEditor
                  value={formData.content}
                  onChange={(val) => handleFieldChange('content', val)}
                  height={400}
                  tenantId={tenant}
                  onUploadError={setUploadError}
                />
              </div>
              <p className="mt-2 text-sm text-gray-500">
                Write your content using Markdown syntax with live preview. Drag and drop or paste images to upload.
              </p>
            </div>
          </>
        )}

        {/* Actions */}
        <div className="flex justify-between items-center pt-4 border-t border-gray-200">
          <div className="flex items-center space-x-4">
            <Link to={`/console/${tenant}`}>
              <Button variant="secondary" type="button">
                Cancel
              </Button>
            </Link>
            {mode === 'edit' && (
              <label className={`flex items-center space-x-2 text-sm text-gray-500 ${isFormBusy ? 'opacity-40 cursor-not-allowed' : ''}`}>
                <input
                  type="checkbox"
                  checked={updateTimestamp}
                  onChange={(e) => setUpdateTimestamp(e.target.checked)}
                  disabled={isFormBusy}
                  className="border-gray-300 text-black focus:ring-black disabled:cursor-not-allowed"
                />
                <span>Update timestamp</span>
              </label>
            )}
          </div>
          <div className="flex items-center space-x-3">
            {draft.status === 'saved' && (
              <span className="inline-flex items-center space-x-1.5 px-3 py-1.5 border border-green-300 bg-green-50 text-green-700 text-sm font-medium">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span>Draft saved</span>
              </span>
            )}
            <Button
              type="submit"
              disabled={!formData.title.trim() || !formData.content.trim() || isFormBusy}
            >
              {mode === 'create' ? 'Preview & Create' : 'Preview & Update'}
            </Button>
          </div>
        </div>
      </form>

    </div>
  );
}