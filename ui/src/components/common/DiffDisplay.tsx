import { DiffLine } from '../../utils';

interface DiffDisplayProps {
  diffLines: DiffLine[];
  maxHeight?: string;
  className?: string;
}

/**
 * A reusable component for displaying diff output.
 * Shows line numbers and color-coded additions/deletions.
 */
export function DiffDisplay({ diffLines, maxHeight = 'max-h-96', className = '' }: DiffDisplayProps) {
  return (
    <div className={`bg-gray-50 border border-gray-200 p-4 overflow-y-auto overflow-x-auto ${maxHeight} ${className}`}>
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
  );
}
