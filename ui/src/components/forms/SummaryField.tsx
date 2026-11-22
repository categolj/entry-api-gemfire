import { useState } from 'react';
import { useTenant } from '../../hooks';
import { api, ApiError } from '../../services';
import { Button } from '../common';
import { Textarea } from './Textarea';

interface SummaryFieldProps {
  value: string;
  onChange: (value: string) => void;
  content: string;
  disabled?: boolean;
  onError?: (error: string) => void;
  onLoadingChange?: (loading: boolean) => void;
}

export function SummaryField({
  value,
  onChange,
  content,
  disabled = false,
  onError,
  onLoadingChange,
}: SummaryFieldProps) {
  const { tenant } = useTenant();
  const [isSummarizing, setIsSummarizing] = useState(false);

  const handleAutoSummarize = async () => {
    if (!content.trim()) {
      onError?.('Content is required to generate summary');
      return;
    }

    setIsSummarizing(true);
    onLoadingChange?.(true);

    try {
      const summary = await api.summarize(tenant, content);
      onChange(summary);
    } catch (error) {
      if (error instanceof ApiError) {
        const detail = error.problemDetail?.detail || error.message;
        onError?.(`HTTP ${error.status}: ${detail}`);
      } else {
        onError?.(error instanceof Error ? error.message : 'Failed to generate summary');
      }
    } finally {
      setIsSummarizing(false);
      onLoadingChange?.(false);
    }
  };

  const isDisabled = disabled || isSummarizing;

  return (
    <div>
      <div className="mb-1 flex items-end justify-between gap-2">
        <label className="label mb-0" htmlFor="summary-field">
          Summary
        </label>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => void handleAutoSummarize()}
          disabled={isSummarizing || !content.trim() || disabled}
          loading={isSummarizing}
          className="px-0 py-0 text-[11px]"
        >
          Write with AI
        </Button>
      </div>
      <Textarea
        id="summary-field"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="One or two sentences shown next to the entry."
        rows={3}
        disabled={isDisabled}
      />
    </div>
  );
}
