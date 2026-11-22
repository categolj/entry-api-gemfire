import { useEffect, useState } from 'react';
import { api, ApiError } from '../../services';
import { calculateDiff } from '../../utils';
import { Button } from './Button';
import { DiffDisplay } from './DiffDisplay';
import { LoadingSpinner } from './LoadingSpinner';
import { ErrorAlert } from './ErrorAlert';

type EditMode = 'PROOFREADING' | 'COMPLETION' | 'EXPANSION';

interface AIEditingDialogProps {
  originalContent: string;
  onApply: (content: string) => void;
  onClose: () => void;
  tenantId: string;
}

export function AIEditingDialog({
  originalContent,
  onApply,
  onClose,
  tenantId,
}: AIEditingDialogProps) {
  const [mode, setMode] = useState<EditMode>('PROOFREADING');
  const [editedContent, setEditedContent] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const modes: { value: EditMode; label: string; description: string }[] = [
    {
      value: 'PROOFREADING',
      label: 'Proofread',
      description: 'Fix typos, punctuation, and grammar only.',
    },
    {
      value: 'COMPLETION',
      label: 'Complete',
      description: 'Proofread and fill in missing explanations.',
    },
    {
      value: 'EXPANSION',
      label: 'Expand',
      description: 'Proofread, complete, and continue writing.',
    },
  ];

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !isLoading) {
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isLoading, onClose]);

  const handleEdit = async () => {
    setIsLoading(true);
    setError(null);
    setEditedContent(null);

    try {
      const result = await api.edit(tenantId, originalContent, mode);
      setEditedContent(result);
    } catch (err) {
      if (err instanceof ApiError) {
        const detail = err.problemDetail?.detail || err.message;
        setError(`HTTP ${err.status}: ${detail}`);
      } else {
        setError(err instanceof Error ? err.message : 'Failed to edit content');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleApply = () => {
    if (editedContent !== null) {
      onApply(editedContent);
    }
  };

  const diffLines = editedContent !== null ? calculateDiff(originalContent, editedContent) : [];

  return (
    <div
      className="modal-scrim fixed inset-0 z-50 overflow-y-auto"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isLoading) {
          onClose();
        }
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="ai-editing-title"
        className="relative mx-auto my-6 w-[min(56rem,calc(100%-2rem))] border border-rule bg-paper shadow-slip"
      >
        <div className="flex items-start justify-between gap-4 border-b border-rule px-6 py-5">
          <div>
            <p className="font-mono text-[10px] uppercase tracking-[0.14em] text-muted">AI assistant</p>
            <h2 id="ai-editing-title" className="mt-1 font-sans text-2xl font-bold tracking-tightest">
              Proof and revise
            </h2>
            <p className="mt-1 text-sm text-ink-soft">
              Review the proposed changes before anything touches your entry.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="font-mono text-xs text-muted hover:text-ink disabled:opacity-40"
          >
            Close
          </button>
        </div>

        <div className="px-6 py-5">
          <fieldset>
            <legend className="label">Revision kind</legend>
            <div className="grid gap-2 sm:grid-cols-3">
              {modes.map((option) => {
                const id = `ai-mode-${option.value}`;
                const selected = mode === option.value;
                return (
                  <label
                    key={option.value}
                    htmlFor={id}
                    className={`cursor-pointer border px-3 py-3 transition-colors ${
                      selected
                        ? 'border-pencil bg-pencil-wash/50'
                        : 'border-rule bg-sheet hover:border-muted'
                    } ${isLoading ? 'cursor-not-allowed opacity-60' : ''}`}
                  >
                    <span className="flex items-center gap-2">
                      <input
                        id={id}
                        type="radio"
                        name="ai-mode"
                        value={option.value}
                        checked={selected}
                        onChange={() => setMode(option.value)}
                        disabled={isLoading}
                        className="h-3.5 w-3.5 accent-[#2B3FD0]"
                      />
                      <span className="font-sans text-sm font-semibold text-ink">{option.label}</span>
                    </span>
                    <span className="mt-1.5 block text-xs leading-relaxed text-muted">
                      {option.description}
                    </span>
                  </label>
                );
              })}
            </div>
          </fieldset>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <Button
              type="button"
              onClick={() => void handleEdit()}
              disabled={isLoading || !originalContent.trim()}
              loading={isLoading}
            >
              {editedContent !== null ? 'Run again' : 'Run revision'}
            </Button>
            <p className="meta-line">Nothing is applied until you choose Apply changes.</p>
          </div>

          {error && (
            <div className="mt-4">
              <ErrorAlert message={error} onDismiss={() => setError(null)} />
            </div>
          )}

          {isLoading && (
            <div className="mt-6 flex items-center justify-center py-10">
              <LoadingSpinner size="md" color="ink" label="Revising content" />
            </div>
          )}

          {editedContent !== null && !isLoading && (
            <div className="mt-6">
              <p className="label">Proposed changes</p>
              <DiffDisplay diffLines={diffLines} maxHeight="max-h-[30rem]" />
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 border-t border-rule px-6 py-4">
          <Button type="button" variant="secondary" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button type="button" onClick={handleApply} disabled={isLoading || editedContent === null}>
            Apply changes
          </Button>
        </div>
      </div>
    </div>
  );
}
