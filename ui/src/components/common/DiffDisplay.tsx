import { DiffLine } from '../../utils';

interface DiffDisplayProps {
  diffLines: DiffLine[];
  maxHeight?: string;
  className?: string;
}

/**
 * A reusable component for displaying diff output.
 * Renders the change as a proof sheet with line numbers and blue-pencil style marks.
 */
export function DiffDisplay({ diffLines, maxHeight = 'max-h-96', className = '' }: DiffDisplayProps) {
  const added = diffLines.filter((line) => line.type === 'added').length;
  const deleted = diffLines.filter((line) => line.type === 'deleted').length;

  return (
    <div className={`flex min-h-0 flex-col border border-rule bg-sheet ${className}`}>
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-rule px-4 py-2">
        <span className="font-mono text-[11px] uppercase tracking-wider text-muted">Proof</span>
        <span className="meta-line">
          <span className="text-press">{added} added</span>
          <span className="mx-2 text-rule">/</span>
          <span className="text-redline">{deleted} removed</span>
        </span>
      </div>

      <div className={`overflow-auto ${maxHeight}`}>
        {diffLines.length === 0 ? (
          <p className="meta-line px-4 py-6">No changes to show.</p>
        ) : (
          <div className="py-1">
            {diffLines.map((line, index) => {
              const lineNumber =
                line.type === 'deleted' ? line.oldLine : line.type === 'added' ? line.newLine : line.oldLine;
              const mark = line.type === 'added' ? '+' : line.type === 'deleted' ? '−' : '';
              const rowClasses =
                line.type === 'added'
                  ? 'bg-press-wash/50'
                  : line.type === 'deleted'
                    ? 'bg-redline-wash/50'
                    : '';

              return (
                <div
                  key={index}
                  className={`grid grid-cols-[3.5rem_1.5rem_minmax(0,1fr)] items-start font-mono text-[12px] leading-[1.6] ${rowClasses}`}
                >
                  <span className="select-none pr-3 text-right text-muted/70">{lineNumber}</span>
                  <span
                    className={
                      line.type === 'added'
                        ? 'text-press'
                        : line.type === 'deleted'
                          ? 'text-redline'
                          : 'text-rule'
                    }
                    aria-hidden="true"
                  >
                    {mark}
                  </span>
                  <span className="whitespace-pre-wrap break-words pr-4 text-ink [overflow-wrap:anywhere]">
                    {line.content || <span className="text-muted">(empty line)</span>}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
