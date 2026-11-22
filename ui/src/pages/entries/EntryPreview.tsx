import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useSWRConfig } from 'swr';
import { useTenant } from '../../hooks';
import { api, ApiError } from '../../services';
import { ErrorAlert, DiffDisplay, Button } from '../../components/common';
import { Entry, FrontMatter, CreateEntryRequest, UpdateEntryRequest, PreviewState } from '../../types';
import { createMarkdownWithFrontMatter, calculateDiff } from '../../utils';

export function EntryPreview() {
  const { tenant } = useTenant();
  const navigate = useNavigate();
  const location = useLocation();
  const { mutate } = useSWRConfig();
  const state = location.state as PreviewState;

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [existingEntryForId, setExistingEntryForId] = useState<Entry | null>(null);
  const [originalMarkdown, setOriginalMarkdown] = useState('');

  // Redirect back if no state
  useEffect(() => {
    if (!state) {
      navigate(`/console/${tenant}/entries/new`);
      return;
    }

    // For create mode with entry ID, check if entry exists
    if (state.mode === 'create' && state.entryIdInput?.trim()) {
      const checkExistingEntry = async () => {
        try {
          const entryId = parseInt(state.entryIdInput!.trim(), 10);
          const existing = await api.getEntry(tenant, entryId);
          setExistingEntryForId(existing);

          const existingMarkdown = createMarkdownWithFrontMatter(existing.frontMatter, existing.content);
          setOriginalMarkdown(existingMarkdown);
        } catch {
          // Entry doesn't exist, which is fine for create with ID
          setExistingEntryForId(null);
          setOriginalMarkdown('');
        }
      };

      void checkExistingEntry();
    } else if (state.mode === 'edit') {
      setOriginalMarkdown(state.originalMarkdown || '');
    }
  }, [state, tenant, navigate]);

  const handleConfirmSubmit = async () => {
    if (!state) return;

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const frontMatter: FrontMatter = {
        title: state.formData.title,
        summary: state.formData.summary || undefined,
        categories: state.formData.categories.map((name) => ({ name })),
        tags: state.formData.tags.map((name) => ({ name })),
        // Include date/updated logic here based on mode and updateTimestamp
        ...(state.existingEntry?.frontMatter.date && { date: state.existingEntry.frontMatter.date }),
        ...(state.mode === 'edit' &&
          !state.updateTimestamp &&
          (state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date) && {
            updated: state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date,
          }),
      };

      if (state.mode === 'create') {
        const request: CreateEntryRequest = {
          frontMatter,
          content: state.formData.content,
        };

        let newEntry;
        if (state.entryIdInput?.trim()) {
          const entryId = parseInt(state.entryIdInput.trim(), 10);
          newEntry = await api.createEntryWithId(tenant, entryId, request);
        } else {
          newEntry = await api.createEntry(tenant, request);
        }
        // Clear draft after successful submission
        sessionStorage.removeItem(`entryDraft_${tenant}_create`);
        // Invalidate all SWR cache to ensure entry list is refreshed
        await mutate(() => true);
        navigate(`/console/${tenant}/entries/${newEntry.entryId}`);
      } else {
        const request: UpdateEntryRequest = {
          frontMatter,
          content: state.formData.content,
        };

        // Extract entry ID from the edit path: /console/{tenant}/entries/{id}/edit/preview
        const pathParts = location.pathname.split('/');
        const idIndex = pathParts.indexOf('entries') + 1;
        const entryId = parseInt(pathParts[idIndex], 10);
        const updatedEntry = await api.updateEntry(tenant, entryId, request);
        // Clear draft after successful submission
        sessionStorage.removeItem(`entryDraft_${tenant}_edit_${entryId}`);
        // Invalidate all SWR cache to ensure entry list is refreshed
        await mutate(() => true);
        navigate(`/console/${tenant}/entries/${updatedEntry.entryId}`);
      }
    } catch (error) {
      if (error instanceof ApiError) {
        const detail = error.problemDetail?.detail || error.message;
        setSubmitError(`HTTP ${error.status}: ${detail}`);
      } else {
        setSubmitError(error instanceof Error ? error.message : 'Failed to save entry');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBack = () => {
    // For edit mode, extract the numeric ID from the path
    // Path format: /console/{tenant}/entries/{id}/edit/preview
    const pathParts = location.pathname.split('/');
    const idIndex = pathParts.indexOf('entries') + 1;
    const entryId = state.mode === 'edit' ? pathParts[idIndex] : null;

    const backPath =
      state.mode === 'edit' ? `/console/${tenant}/entries/${entryId}/edit` : `/console/${tenant}/entries/new`;

    navigate(backPath, { state });
  };

  const getCurrentMarkdown = () => {
    if (!state) return '';

    const frontMatter: FrontMatter = {
      title: state.formData.title,
      summary: state.formData.summary || undefined,
      categories: state.formData.categories.map((name) => ({ name })),
      tags: state.formData.tags.map((name) => ({ name })),
      ...(state.existingEntry?.frontMatter.date && { date: state.existingEntry.frontMatter.date }),
      ...(state.mode === 'edit' &&
        !state.updateTimestamp &&
        (state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date) && {
          updated: state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date,
        }),
    };
    return createMarkdownWithFrontMatter(frontMatter, state.formData.content);
  };

  if (!state) {
    return null;
  }

  const title =
    state.mode === 'create'
      ? existingEntryForId
        ? `Review changes to entry ${state.entryIdInput}`
        : 'Review the new entry'
      : 'Review changes';

  const diffLines = calculateDiff(originalMarkdown, getCurrentMarkdown());

  return (
    <div>
      <nav aria-label="Breadcrumb" className="meta-line">
        <Link to={`/console/${tenant}`} className="hover:text-pencil hover:underline">
          Entries
        </Link>
        <span className="mx-2 text-rule">/</span>
        <button type="button" onClick={handleBack} className="hover:text-pencil hover:underline">
          {state.mode === 'create' ? 'New entry' : 'Edit entry'}
        </button>
        <span className="mx-2 text-rule">/</span>
        <span>Proof</span>
      </nav>

      <header className="mt-3 border-b border-ink pb-5">
        <h1 className="font-sans text-3xl font-bold tracking-tightest sm:text-4xl">{title}</h1>
        <p className="mt-2 text-sm text-ink-soft">
          Saving sends this version to the API immediately. Check the marks before you confirm.
        </p>
      </header>

      {submitError && (
        <div className="mt-6">
          <ErrorAlert message={submitError} onDismiss={() => setSubmitError(null)} />
        </div>
      )}

      <div className="mt-6">
        <DiffDisplay diffLines={diffLines} maxHeight="max-h-[calc(100vh-26rem)] min-h-[18rem]" />
      </div>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-rule pt-5">
        <p className="meta-line">
          {state.mode === 'create' ? 'Creates a new entry.' : 'Updates the existing entry.'}
        </p>
        <div className="flex gap-3">
          <Button type="button" variant="secondary" onClick={handleBack} disabled={isSubmitting}>
            Back to edit
          </Button>
          <Button
            type="button"
            onClick={() => void handleConfirmSubmit()}
            disabled={isSubmitting}
            loading={isSubmitting}
          >
            {isSubmitting ? 'Saving' : state.mode === 'create' ? 'Confirm and create' : 'Confirm and update'}
          </Button>
        </div>
      </div>
    </div>
  );
}
