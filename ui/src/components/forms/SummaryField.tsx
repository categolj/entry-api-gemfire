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
      <div className="flex items-center justify-between mb-1">
        <label className="block text-sm font-medium text-black">Summary</label>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          onClick={() => void handleAutoSummarize()}
          disabled={isSummarizing || !content.trim() || disabled}
          loading={isSummarizing}
        >
          Auto Generate
        </Button>
      </div>
      <Textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Brief description of the entry"
        rows={2}
        disabled={isDisabled}
      />
    </div>
  );
}
