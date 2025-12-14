import React from 'react';

interface FileActionButtonsProps {
  selectedCount: number;
  isProcessing: boolean;
  onTranslate: () => void;
  onDownload: () => void;
  onDelete: () => void;
}

export function FileActionButtons({
  selectedCount,
  isProcessing,
  onTranslate,
  onDownload,
  onDelete
}: FileActionButtonsProps): React.ReactElement {
  return (
    <div className="flex flex-wrap items-center gap-3 mb-6">
      <button
        onClick={onTranslate}
        disabled={selectedCount === 0 || isProcessing}
        className="bg-primary text-white px-4 py-2 rounded-md font-medium text-sm hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
      >
        선택 파일 번역 ({selectedCount})
      </button>
      <button
        onClick={onDownload}
        disabled={selectedCount === 0}
        className="bg-button-secondary text-line px-4 py-2 rounded-md font-medium text-sm hover:bg-opacity-80 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        번역 파일 다운로드 ({selectedCount})
      </button>
      <button
        onClick={onDelete}
        disabled={selectedCount === 0}
        className="bg-button-secondary text-line px-4 py-2 rounded-md font-medium text-sm hover:bg-opacity-80 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        선택 파일 삭제 ({selectedCount})
      </button>
    </div>
  );
}
