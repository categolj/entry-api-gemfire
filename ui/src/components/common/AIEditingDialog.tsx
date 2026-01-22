import { useState } from 'react';
import { api, ApiError } from '../../services';
import { Button } from './Button';
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

  const modeLabels: Record<EditMode, { label: string; description: string }> = {
    PROOFREADING: {
      label: 'Proofreading',
      description: 'Fix typos, punctuation, and grammar only',
    },
    COMPLETION: {
      label: 'Completion',
      description: 'Proofreading + fill in missing explanations',
    },
    EXPANSION: {
      label: 'Expansion',
      description: 'Proofreading + completion + continue writing',
    },
  };

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

  // Calculate diff for display
  const originalLines = originalContent.split('\n');
  const newLines = editedContent?.split('\n') || [];

  const calculateLCS = (arr1: string[], arr2: string[]) => {
    const m = arr1.length;
    const n = arr2.length;
    const dp: number[][] = Array(m + 1).fill(0).map(() => Array(n + 1).fill(0) as number[]);

    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        if (arr1[i - 1] === arr2[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }

    const result: { type: string; content: string; oldLine: number | null; newLine: number | null }[] = [];
    let i = m, j = n;

    while (i > 0 || j > 0) {
      if (i > 0 && j > 0 && arr1[i - 1] === arr2[j - 1]) {
        result.unshift({ type: 'unchanged', content: arr1[i - 1], oldLine: i, newLine: j });
        i--;
        j--;
      } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
        result.unshift({ type: 'added', content: arr2[j - 1], oldLine: null, newLine: j });
        j--;
      } else if (i > 0) {
        result.unshift({ type: 'deleted', content: arr1[i - 1], oldLine: i, newLine: null });
        i--;
      }
    }

    return result;
  };

  const diffLines = editedContent !== null ? calculateLCS(originalLines, newLines) : [];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-30 overflow-y-auto h-full w-full z-50">
      <div className="relative top-8 mx-auto p-6 border border-gray-200 max-w-4xl bg-white">
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="mb-4">
            <h3 className="text-lg font-medium text-black">AI Editing</h3>
            <p className="mt-1 text-sm text-gray-500">
              Use AI to proofread and improve your content
            </p>
          </div>

          {/* Mode Selection */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-black mb-2" htmlFor="edit-mode-select">
              Edit Mode
            </label>
            <select
              id="edit-mode-select"
              value={mode}
              onChange={(e) => setMode(e.target.value as EditMode)}
              disabled={isLoading}
              className="w-full px-3 py-2 border border-gray-300 text-sm focus:outline-none focus:border-black disabled:bg-gray-100 disabled:cursor-not-allowed"
            >
              {(Object.keys(modeLabels) as EditMode[]).map((m) => (
                <option key={m} value={m}>
                  {modeLabels[m].label} - {modeLabels[m].description}
                </option>
              ))}
            </select>
          </div>

          {/* Run Button */}
          <div className="mb-4">
            <Button
              type="button"
              onClick={() => void handleEdit()}
              disabled={isLoading || !originalContent.trim()}
              loading={isLoading}
            >
              Run
            </Button>
          </div>

          {/* Error Display */}
          {error && (
            <div className="mb-4">
              <ErrorAlert message={error} onDismiss={() => setError(null)} />
            </div>
          )}

          {/* Loading State */}
          {isLoading && (
            <div className="flex justify-center items-center py-8">
              <LoadingSpinner size="lg" />
              <span className="ml-3 text-sm text-gray-500">Editing content...</span>
            </div>
          )}

          {/* Diff Display */}
          {editedContent !== null && !isLoading && (
            <div className="mb-4 flex-1 overflow-hidden">
              <label className="block text-sm font-medium text-black mb-2">
                Changes Preview
              </label>
              <div className="bg-gray-50 border border-gray-200 p-4 overflow-y-auto overflow-x-auto max-h-96">
                <div className="grid grid-cols-12 gap-0 text-xs font-mono min-w-max">
                  <div className="col-span-1 text-gray-500 font-medium">Line</div>
                  <div className="col-span-11 text-gray-500 font-medium">Content</div>
                </div>

                {diffLines.map((line, index) => (
                  <div
                    key={index}
                    className={`grid grid-cols-12 gap-0 text-xs font-mono py-1 min-w-max ${
                      line.type === 'added'
                        ? 'bg-green-50 text-green-700'
                        : line.type === 'deleted'
                        ? 'bg-red-50 text-red-700'
                        : 'text-black'
                    }`}
                  >
                    <div className="col-span-1 text-gray-400">
                      {line.type === 'deleted' ? line.oldLine : line.type === 'added' ? line.newLine : line.oldLine}
                    </div>
                    <div className="col-span-11 whitespace-pre-wrap break-words overflow-hidden">
                      <span className="inline-block w-4">
                        {line.type === 'added' && <span className="text-green-600">+</span>}
                        {line.type === 'deleted' && <span className="text-red-600">-</span>}
                        {line.type === 'unchanged' && <span className="text-gray-300">&nbsp;</span>}
                      </span>
                      {line.content || <span className="text-gray-400">(empty line)</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200">
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button
              type="button"
              onClick={handleApply}
              disabled={isLoading || editedContent === null}
            >
              Apply Changes
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
