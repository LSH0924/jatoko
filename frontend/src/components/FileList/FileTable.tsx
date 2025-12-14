import React, { DragEvent } from 'react';
import dayjs from 'dayjs';
import { FileMetadata } from '../../services/api';

interface FileTableProps {
  fileMetadata: FileMetadata[];
  selectedFiles: Set<string>;
  uploading: boolean;
  isDragOver: boolean;
  onSelectAll: () => void;
  onToggleSelection: (fileName: string) => void;
  onDragOver: (e: DragEvent<HTMLDivElement>) => void;
  onDragLeave: (e: DragEvent<HTMLDivElement>) => void;
  onDrop: (e: DragEvent<HTMLDivElement>) => void;
}

export function FileTable({
  fileMetadata,
  selectedFiles,
  uploading,
  isDragOver,
  onSelectAll,
  onToggleSelection,
  onDragOver,
  onDragLeave,
  onDrop
}: FileTableProps): React.ReactElement {
  
  const formatDate = (dateStr: string | null): string => {
    if (!dateStr) return '-';
    return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss');
  };

  return (
    <>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200">Target 파일 목록</h3>
        <span className="bg-gray-200 dark:bg-gray-600 text-gray-700 dark:text-gray-300 text-xs font-medium px-2.5 py-1 rounded-full">
          {fileMetadata.length}개
        </span>
      </div>

      <div className={`overflow-x-auto ${isDragOver ? 'drop-target' : ''}`}>
        <table
          className="w-full text-sm text-left text-gray-600 dark:text-gray-400"
          onDragOver={onDragOver}
          onDragLeave={onDragLeave}
          onDrop={onDrop}
        >
          <thead className="text-sm text-gray-600 dark:text-gray-300 bg-gray-200 dark:bg-gray-700">
            <tr>
              <th scope="col" className="p-4 w-4">
                <input
                  type="checkbox"
                  checked={selectedFiles.size === fileMetadata.length && fileMetadata.length > 0}
                  onChange={onSelectAll}
                  className="w-4 h-4 text-primary bg-gray-100 border-gray-300 rounded focus:ring-primary dark:focus:ring-offset-gray-800 focus:ring-2 dark:bg-gray-600 dark:border-gray-500"
                />
              </th>
              <th scope="col" className="px-6 py-3 font-medium text-left">파일명</th>
              <th scope="col" className="px-6 py-3 font-medium text-left">번역됨</th>
              <th scope="col" className="px-6 py-3 font-medium text-left">버전</th>
              <th scope="col" className="px-6 py-3 font-medium text-left">업로드 날짜</th>
              <th scope="col" className="px-6 py-3 font-medium text-left">번역된 날짜</th>
            </tr>
          </thead>
          <tbody>
            {uploading && (
              <tr>
                <td colSpan={6} className="px-6 py-3 font-medium text-left uploading-message">
                  업로드 중...
                </td>
              </tr>
            )}

            {fileMetadata.map((file, index) => (
              <tr
                key={file.fileName}
                className={`bg-white dark:bg-gray-800 ${index < fileMetadata.length - 1 ? 'border-b dark:border-gray-700' : ''} hover:bg-gray-50 dark:hover:bg-gray-600`}
              >
                <td className="w-4 p-4">
                  <input
                    type="checkbox"
                    checked={selectedFiles.has(file.fileName)}
                    onChange={() => onToggleSelection(file.fileName)}
                    className="w-4 h-4 text-primary bg-gray-100 border-gray-300 rounded focus:ring-primary dark:focus:ring-offset-gray-800 focus:ring-2 dark:bg-gray-600 dark:border-gray-500"
                  />
                </td>
                <th scope="row" className="px-6 py-4 font-medium text-gray-900 dark:text-white">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="whitespace-nowrap">{file.fileName}</span>
                    {file.outlined && (
                      <span className="inline-flex items-center gap-1 bg-yellow-100 dark:bg-yellow-900/50 text-yellow-800 dark:text-yellow-400 text-xs font-medium px-2 py-0.5 rounded-full whitespace-nowrap">
                        <span className="material-icons text-xs">warning</span>
                        텍스트 추출 불가
                      </span>
                    )}
                  </div>
                </th>
                <td className="px-6 py-4">
                  {file.translated ? (
                    <span className="material-icons text-primary text-xl">check</span>
                  ) : (
                    <span className="text-gray-400">-</span>
                  )}
                </td>
                <td className="px-6 py-4">
                  {file.version !== null ? file.version : '-'}
                </td>
                <td className="px-6 py-4">{formatDate(file.uploadedAt)}</td>
                <td className="px-6 py-4">{formatDate(file.translatedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
