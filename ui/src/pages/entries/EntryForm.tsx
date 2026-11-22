import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, Link, useLocation } from 'react-router-dom';
import { useTenant, useApi, useDraft } from '../../hooks';
import { api, ApiError } from '../../services';
import { LoadingSpinner, ErrorAlert, Button, DraftBanner, AIEditingDialog } from '../../components/common';
import { Input, TagInput, CategoryInput, ImageDropEditor, SummaryField } from '../../components/forms';
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
  const [showEditingDialog, setShowEditingDialog] = useState(false);
  const titleRef = useRef<HTMLTextAreaElement>(null);

  const isFormBusy = isLoadingExisting || isSummarizing || isLoadingTemplate;

  // Let the title field grow with its content instead of scrolling horizontally.
  // Re-fit on resize and once webfonts settle, since both change the line count.
  useEffect(() => {
    const field = titleRef.current;
    if (!field) return;

    const fit = () => {
      field.style.height = 'auto';
      field.style.height = `${field.scrollHeight}px`;
    };

    fit();
    window.addEventListener('resize', fit);
    void document.fonts?.ready.then(fit);

    return () => window.removeEventListener('resize', fit);
  }, [formData.title, markdownMode]);

  // Load existing entry for edit mode
  const {
    data: existingEntry,
    loading: loadingEntry,
    error: loadError,
  } = useApi(
    () => (mode === 'edit' ? api.getEntry(tenant, entryId) : Promise.resolve(null)),
    [tenant, entryId, mode],
    { immediate: mode === 'edit' }
  );

  const draftKey =
    mode === 'create' ? `entryDraft_${tenant}_create` : `entryDraft_${tenant}_edit_${entryId}`;

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
        categories: existingEntry.frontMatter.categories?.map((c) => c.name) || [],
        tags: existingEntry.frontMatter.tags?.map((t) => t.name) || [],
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
        categories: formData.categories.map((name) => ({ name })),
        tags: formData.tags.map((name) => ({ name })),
        // Only include date if it was explicitly set in the original
        ...(existingEntry.frontMatter.date && { date: existingEntry.frontMatter.date }),
        // Only include updated when explicitly keeping the old timestamp (checkbox unchecked)
        ...(mode === 'edit' &&
          !updateTimestamp &&
          (existingEntry.frontMatter.updated || existingEntry.updated.date) && {
            updated: existingEntry.frontMatter.updated || existingEntry.updated.date,
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
        categories: existingEntry.frontMatter.categories?.map((c) => c.name) || [],
        tags: existingEntry.frontMatter.tags?.map((t) => t.name) || [],
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
    setFormData((prev) => ({ ...prev, [field]: value }));
    setSubmitError(null);

    // Update markdown content when form fields change
    if (!markdownMode) {
      const updatedFormData = { ...formData, [field]: value } as typeof formData;
      const frontMatter: FrontMatter = {
        title: updatedFormData.title,
        summary: updatedFormData.summary || undefined,
        categories: updatedFormData.categories.map((name) => ({ name })),
        tags: updatedFormData.tags.map((name) => ({ name })),
        // Only include date if it was explicitly set in the original
        ...(existingEntry?.frontMatter.date && { date: existingEntry.frontMatter.date }),
        // Only include updated when explicitly keeping the old timestamp (checkbox unchecked)
        ...(mode === 'edit' &&
          !updateTimestamp &&
          (existingEntry?.frontMatter.updated || existingEntry?.updated.date) && {
            updated: existingEntry?.frontMatter.updated || existingEntry?.updated.date,
          }),
      };
      setMarkdownContent(createMarkdownWithFrontMatter(frontMatter, updatedFormData.content));
    }
  };

  const handleMarkdownChange = (value: string) => {
    setMarkdownContent(value);

    // Parse markdown and update form fields
    const { frontMatter, content } = parseMarkdownWithFrontMatter(value);
    setFormData((prev) => ({
      ...prev,
      title: frontMatter.title || prev.title,
      summary: frontMatter.summary || prev.summary,
      categories: frontMatter.categories?.map((c) => c.name) || prev.categories,
      tags: frontMatter.tags?.map((t) => t.name) || prev.tags,
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
        categories: entry.frontMatter.categories?.map((c) => c.name) || [],
        tags: entry.frontMatter.tags?.map((t) => t.name) || [],
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

  const handleLoadTemplate = async () => {
    setIsLoadingTemplate(true);
    setSubmitError(null);

    try {
      const templateMarkdown = await api.getTemplate();
      const { frontMatter, content } = parseMarkdownWithFrontMatter(templateMarkdown);
      setFormData({
        title: frontMatter.title || '',
        summary: frontMatter.summary || '',
        categories: frontMatter.categories?.map((c) => c.name) || [],
        tags: frontMatter.tags?.map((t) => t.name) || [],
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
      originalMarkdown,
    };

    const previewPath =
      mode === 'create'
        ? `/console/${tenant}/entries/new/preview`
        : `/console/${tenant}/entries/${entryId}/edit/preview`;

    navigate(previewPath, { state: previewState });
  };

  if (mode === 'edit' && loadingEntry) {
    return (
      <div className="flex h-64 items-center justify-center">
        <LoadingSpinner size="lg" label="Loading entry" />
      </div>
    );
  }

  if (mode === 'edit' && loadError) {
    return (
      <div>
        <ErrorAlert message={loadError} />
        <div className="mt-4">
          <Link to={`/console/${tenant}`}>
            <Button variant="secondary">Back to entries</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      <nav aria-label="Breadcrumb" className="meta-line">
        <Link to={`/console/${tenant}`} className="hover:text-pencil hover:underline">
          Entries
        </Link>
        <span className="mx-2 text-rule">/</span>
        {mode === 'edit' ? (
          <>
            <Link
              to={`/console/${tenant}/entries/${entryId}`}
              className="hover:text-pencil hover:underline"
            >
              Entry {entryId}
            </Link>
            <span className="mx-2 text-rule">/</span>
            <span>Edit</span>
          </>
        ) : (
          <span>New entry</span>
        )}
      </nav>

      <header className="mt-3 flex flex-wrap items-end justify-between gap-4 border-b border-ink pb-5">
        <div>
          <h1 className="font-sans text-3xl font-bold tracking-tightest sm:text-4xl">
            {mode === 'create' ? 'New entry' : 'Edit entry'}
          </h1>
          <p className="mt-2 text-sm text-ink-soft">
            {mode === 'create'
              ? 'Draft the entry, then review every change before it is sent to the API.'
              : `Editing entry № ${String(entryId).padStart(3, '0')}.`}
          </p>
        </div>

        <div className="flex w-full items-end gap-1 lg:w-auto" role="group" aria-label="Editor mode">
          <button
            type="button"
            onClick={() => setMarkdownMode(false)}
            disabled={isFormBusy}
            aria-pressed={!markdownMode}
            className="tab disabled:opacity-40"
          >
            Form editor
          </button>
          <span className="mx-2 h-4 w-px bg-rule" aria-hidden="true" />
          <button
            type="button"
            onClick={() => {
              setMarkdownMode(true);
              if (!markdownContent) {
                const frontMatter: FrontMatter = {
                  title: formData.title,
                  summary: formData.summary || undefined,
                  categories: formData.categories.map((name) => ({ name })),
                  tags: formData.tags.map((name) => ({ name })),
                };
                setMarkdownContent(createMarkdownWithFrontMatter(frontMatter, formData.content));
              }
            }}
            disabled={isFormBusy}
            aria-pressed={markdownMode}
            className="tab disabled:opacity-40"
          >
            Markdown editor
          </button>
        </div>
      </header>

      {submitError && (
        <div className="mt-5">
          <ErrorAlert message={submitError} onDismiss={() => setSubmitError(null)} />
        </div>
      )}

      {uploadError && (
        <div className="mt-5">
          <ErrorAlert title="Upload failed" message={uploadError} onDismiss={() => setUploadError(null)} />
        </div>
      )}

      {draft.status === 'restored' && draft.restoredAt && (
        <div className="mt-5">
          <DraftBanner
            restoredAt={draft.restoredAt}
            onDiscard={handleDiscardDraft}
            onDismiss={draft.dismiss}
          />
        </div>
      )}

      <form onSubmit={handleSubmit} className="mt-6">
        {markdownMode ? (
          /* Markdown editor */
          <div className="space-y-6">
            {mode === 'create' && (
              <div className="border border-rule bg-sheet p-4">
                <h2 className="label">Entry ID</h2>
                <div className="flex items-end gap-2">
                  <div className="w-full max-w-xs">
                    <Input
                      type="number"
                      value={entryIdInput}
                      onChange={(e) => setEntryIdInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && entryIdInput.trim() && !isLoadingExisting) {
                          e.preventDefault();
                          void handleLoadExisting();
                        }
                      }}
                      placeholder="Optional"
                      disabled={isFormBusy}
                      helpText="Leave empty to auto-generate, or load an existing entry."
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

            <div className={isFormBusy ? 'pointer-events-none opacity-60' : ''}>
              <div className="flex items-baseline justify-between border-b border-rule pb-2">
                <h2 className="label mb-0">Markdown with front matter</h2>
                <span className="meta-line">Live preview</span>
              </div>
              <div className="mt-3">
                <ImageDropEditor
                  value={markdownContent}
                  onChange={handleMarkdownChange}
                  height={600}
                  tenantId={tenant}
                  onUploadError={setUploadError}
                />
              </div>
              <p className="mt-2 text-sm text-ink-soft">
                Edit the whole entry, including front matter, in Markdown. Drag or paste an image to
                upload it.
              </p>
            </div>
          </div>
        ) : (
          /* Form editor */
          <>
            <div>
              <label htmlFor="entry-title" className="label">
                Title
              </label>
              <textarea
                id="entry-title"
                ref={titleRef}
                rows={1}
                value={formData.title}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                onKeyDown={(e) => {
                  // The title is a single logical line; the field only wraps visually.
                  if (e.key === 'Enter') {
                    e.preventDefault();
                  }
                }}
                placeholder="Untitled entry"
                required
                disabled={isFormBusy}
                className="field-title w-full resize-none overflow-hidden"
              />
            </div>

            <div className="mt-8 grid gap-10 lg:grid-cols-[minmax(0,1fr)_17rem]">
              <div className={`min-w-0 ${isFormBusy ? 'pointer-events-none opacity-60' : ''}`}>
                <div className="flex flex-wrap items-baseline justify-between gap-2 border-b border-rule pb-2">
                  <h2 className="label mb-0">Body</h2>
                  <div className="flex items-center gap-4">
                    <span className="meta-line hidden sm:inline">Markdown, live preview</span>
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={() => setShowEditingDialog(true)}
                      disabled={!formData.content.trim() || isFormBusy}
                    >
                      AI revise
                    </Button>
                  </div>
                </div>

                <div className="mt-3">
                  <ImageDropEditor
                    value={formData.content}
                    onChange={(val) => handleFieldChange('content', val)}
                    height={520}
                    tenantId={tenant}
                    onUploadError={setUploadError}
                  />
                </div>
                <p className="mt-2 text-sm text-ink-soft">
                  Drag or paste an image to upload it. The AI assistant proposes changes as a proof
                  before anything is applied.
                </p>
              </div>

              <aside className="min-w-0 self-start lg:sticky lg:top-8">
                <h2 className="label mb-0 border-b border-rule pb-2">Entry settings</h2>
                <div className="space-y-6 pt-4">
                  <SummaryField
                    value={formData.summary}
                    onChange={(value) => handleFieldChange('summary', value)}
                    content={formData.content}
                    disabled={isFormBusy}
                    onError={setSubmitError}
                    onLoadingChange={setIsSummarizing}
                  />

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

                  {mode === 'create' && (
                    <div className="border-t border-rule pt-4">
                      <Input
                        label="Entry ID (optional)"
                        type="number"
                        value={entryIdInput}
                        onChange={(e) => setEntryIdInput(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' && entryIdInput.trim() && !isLoadingExisting) {
                            e.preventDefault();
                            void handleLoadExisting();
                          }
                        }}
                        placeholder="Auto-generate"
                        disabled={isFormBusy}
                        helpText="Leave empty to let the API assign an ID."
                      />
                      <div className="mt-2 flex gap-2">
                        <Button
                          type="button"
                          variant="secondary"
                          size="sm"
                          onClick={() => void handleLoadExisting()}
                          disabled={!entryIdInput.trim() || isFormBusy}
                          loading={isLoadingExisting}
                        >
                          Load entry
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => void handleLoadTemplate()}
                          disabled={isFormBusy}
                          loading={isLoadingTemplate}
                        >
                          Load template
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              </aside>
            </div>
          </>
        )}

        {/* Actions */}
        <div className="mt-10 flex flex-wrap items-center justify-between gap-4 border-t border-ink pt-5">
          <div className="flex flex-wrap items-center gap-4">
            <Link to={`/console/${tenant}`}>
              <Button variant="secondary" type="button">
                Cancel
              </Button>
            </Link>
            {mode === 'edit' && (
              <label
                className={`flex items-center gap-2 font-sans text-sm text-ink-soft ${
                  isFormBusy ? 'cursor-not-allowed opacity-40' : 'cursor-pointer'
                }`}
              >
                <input
                  type="checkbox"
                  checked={updateTimestamp}
                  onChange={(e) => setUpdateTimestamp(e.target.checked)}
                  disabled={isFormBusy}
                  className="h-3.5 w-3.5 accent-[#2B3FD0]"
                />
                <span>Update the timestamp</span>
              </label>
            )}
          </div>
          <div className="flex items-center gap-4">
            {draft.status === 'saved' && (
              <span className="inline-flex items-center gap-2 font-mono text-[11px] text-press">
                <span className="h-1.5 w-1.5 rounded-full bg-press" aria-hidden="true" />
                Draft saved
              </span>
            )}
            <Button
              type="submit"
              disabled={!formData.title.trim() || !formData.content.trim() || isFormBusy}
            >
              {mode === 'create' ? 'Preview and create' : 'Preview and update'}
            </Button>
          </div>
        </div>
      </form>

      {showEditingDialog && (
        <AIEditingDialog
          originalContent={formData.content}
          tenantId={tenant}
          onApply={(content) => {
            handleFieldChange('content', content);
            setShowEditingDialog(false);
          }}
          onClose={() => setShowEditingDialog(false)}
        />
      )}
    </div>
  );
}
