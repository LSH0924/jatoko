import React from 'react';

interface ProgressData {
  total: number;
  current: number;
  currentFile: string;
}

interface FileProgressData {
  message: string;
  percentage: number;
}

interface FileProgressBarProps {
  batchProgress: ProgressData | null;
  fileProgress: FileProgressData | null;
}

export function FileProgressBar({ batchProgress, fileProgress }: FileProgressBarProps): React.ReactElement | null {
  if (!batchProgress) return null;

  return (
    <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-4 mb-6">
      <div className="flex justify-between items-center mb-2">
        <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">
          전체 진행률 ({batchProgress.current}/{batchProgress.total})
        </span>
        <span className="text-xs text-gray-500 dark:text-gray-400">
          현재 파일: {batchProgress.currentFile}
        </span>
      </div>
      <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2.5 mb-4">
        <div
          className="bg-blue-600 h-2.5 rounded-full transition-all duration-300"
          style={{ width: `${(batchProgress.current / batchProgress.total) * 100}%` }}
        />
      </div>

      {fileProgress && (
        <div className="mt-2 pl-4 border-l-2 border-blue-300 dark:border-blue-600">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs text-gray-600 dark:text-gray-400">{fileProgress.message}</span>
            <span className="text-xs font-bold text-blue-600 dark:text-blue-400">{fileProgress.percentage}%</span>
          </div>
          <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-1.5">
            <div
              className="bg-green-500 h-1.5 rounded-full transition-all duration-300"
              style={{ width: `${fileProgress.percentage}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );
}
