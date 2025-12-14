import React, { useEffect } from 'react';
import { useFileManagement } from '../hooks/useFileManagement';
import { FileActionButtons } from './FileList/FileActionButtons';
import { FileProgressBar } from './FileList/FileProgressBar';
import { FileTable } from './FileList/FileTable';
import { OutlinedWarning } from './FileList/OutlinedWarning';

export function FileListPanel(): React.ReactElement {
  const {
    fileMetadata,
    selectedFiles,
    loading,
    error,
    batchProgress,
    fileProgress,
    uploading,
    isDragOver,
    outlinedWarning,
    setOutlinedWarning,
    fetchFiles,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    handleBatchTranslate,
    handleBatchDownload,
    handleBatchDelete,
    handleSelectAll,
    handleCheckboxChange
  } = useFileManagement();

  useEffect(() => {
    fetchFiles();
  }, [fetchFiles]);

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6 md:p-8 border border-gray-200 dark:border-gray-700">
      <div className="flex flex-row justify-between items-center mb-6 gap-4">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">파일 목록</h2>
        <button
          onClick={fetchFiles}
          disabled={loading}
          className="bg-white text-gray-800 px-5 py-2.5 rounded-lg font-semibold text-sm border border-gray-300 hover:bg-gray-50 transition-colors flex items-center gap-2 disabled:opacity-50"
        >
          <span className="material-icons text-base">refresh</span>
          {loading ? '로딩...' : '새로고침'}
        </button>
      </div>

      {error && (
        <div className="bg-red-100 dark:bg-red-900/30 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-400 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      <OutlinedWarning 
        files={outlinedWarning} 
        onClose={() => setOutlinedWarning([])} 
      />

      <FileProgressBar 
        batchProgress={batchProgress} 
        fileProgress={fileProgress} 
      />

      <FileActionButtons
        selectedCount={selectedFiles.size}
        isProcessing={!!batchProgress}
        onTranslate={handleBatchTranslate}
        onDownload={handleBatchDownload}
        onDelete={handleBatchDelete}
      />

      <hr className="border-t-2 border-line mb-6"/>

      <FileTable
        fileMetadata={fileMetadata}
        selectedFiles={selectedFiles}
        uploading={uploading}
        isDragOver={isDragOver}
        onSelectAll={handleSelectAll}
        onToggleSelection={handleCheckboxChange}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      />
    </div>
  );
}