import React, { useState, useEffect, useCallback, DragEvent } from 'react';
import {
  getFileMetadata,
  uploadToTarget,
  translateBatch,
  downloadTranslatedFile,
  deleteBatch,
} from '../services/api';
import { useTranslationStore, translationStore } from '../stores/translationStore';
import dayjs from 'dayjs';

export function FileListPanel(): React.ReactElement {
  const fileMetadata = useTranslationStore((s) => s.fileMetadata);
  const selectedFiles = useTranslationStore((s) => s.selectedFiles);
  const loading = useTranslationStore((s) => s.loading);
  const error = useTranslationStore((s) => s.error);
  const batchProgress = useTranslationStore((s) => s.batchProgress);

  const [uploading, setUploading] = useState<boolean>(false);
  const [isDragOver, setIsDragOver] = useState<boolean>(false);
  const [outlinedWarning, setOutlinedWarning] = useState<string[]>([]);

  const ALLOWED_EXTENSIONS = ['.asta', '.astah', '.svg'];

  const fetchFiles = useCallback(async () => {
    translationStore.setLoading(true);
    translationStore.clearError();
    try {
      const metadata = await getFileMetadata();
      translationStore.setFileMetadata(metadata);
    } catch (err) {
      translationStore.setError('파일 목록을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      translationStore.setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFiles();
  }, [fetchFiles]);

  const handleDragOver = (e: DragEvent<HTMLDivElement>): void => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>): void => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = async (e: DragEvent<HTMLDivElement>): Promise<void> => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    const files = Array.from(e.dataTransfer.files);
    if (files.length === 0) return;

    const validFiles = files.filter(file => {
      const ext = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
      return ALLOWED_EXTENSIONS.includes(ext);
    });

    if (validFiles.length === 0) {
      translationStore.setError('.asta, .svg 파일만 업로드 가능합니다.');
      return;
    }

    if (validFiles.length !== files.length) {
      translationStore.setError(
        `${files.length - validFiles.length}개의 파일이 지원되지 않는 형식입니다. .asta, .svg만 허용됩니다.`
      );
    }

    setUploading(true);
    setOutlinedWarning([]);

    try {
      const outlinedFiles: string[] = [];
      for (const file of validFiles) {
        const result = await uploadToTarget(file);
        if (result.outlined) {
          outlinedFiles.push(result.fileName);
        }
      }
      if (outlinedFiles.length > 0) {
        setOutlinedWarning(outlinedFiles);
      }
      await fetchFiles();
    } catch (err) {
      translationStore.setError('파일 업로드에 실패했습니다.');
      console.error(err);
    } finally {
      setUploading(false);
    }
  };

  const handleCheckboxChange = (fileName: string): void => {
    translationStore.toggleFileSelection(fileName);
  };

  const handleSelectAll = (): void => {
    if (selectedFiles.size === fileMetadata.length) {
      translationStore.deselectAll();
    } else {
      translationStore.selectAll();
    }
  };

  const handleBatchTranslate = async (): Promise<void> => {
    if (selectedFiles.size === 0) {
      translationStore.setError('번역할 파일을 선택하세요.');
      return;
    }

    // outlined SVG 파일 필터링
    const selectedFileNames = Array.from(selectedFiles);
    const outlinedFiles = selectedFileNames.filter(fileName => {
      const metadata = fileMetadata.find(f => f.fileName === fileName);
      return metadata?.outlined === true;
    });
    const translatableFiles = selectedFileNames.filter(fileName => {
      const metadata = fileMetadata.find(f => f.fileName === fileName);
      return metadata?.outlined !== true;
    });

    // outlined 파일만 선택된 경우
    if (translatableFiles.length === 0) {
      translationStore.setError(
        `선택한 파일이 모두 텍스트가 패스로 변환(아웃라인화)되어 번역할 수 없습니다: ${outlinedFiles.join(', ')}`
      );
      return;
    }

    // 일부 outlined 파일이 있는 경우 경고
    if (outlinedFiles.length > 0) {
      translationStore.setError(
        `다음 파일은 아웃라인화되어 번역에서 제외됩니다: ${outlinedFiles.join(', ')}`
      );
    } else {
      translationStore.clearError();
    }

    const fileNames = translatableFiles;

    try {
      // Progress 초기화
      translationStore.setBatchProgress({
        total: fileNames.length,
        current: 0,
        currentFile: '',
      });

      // 순차적 번역 (Progress 업데이트)
      for (let i = 0; i < fileNames.length; i++) {
        translationStore.setBatchProgress({
          total: fileNames.length,
          current: i,
          currentFile: fileNames[i],
        });

        await translateBatch([fileNames[i]]);
      }

      // 완료
      translationStore.setBatchProgress({
        total: fileNames.length,
        current: fileNames.length,
        currentFile: '',
      });

      await fetchFiles();
    } catch (err) {
      const error = err as { response?: { data?: { error?: string } }; message?: string };
      translationStore.setError(`배치 번역 실패: ${error.response?.data?.error || error.message}`);
      console.error(err);
    } finally {
      translationStore.setBatchProgress(null);
    }
  };

  const handleBatchDownload = async (): Promise<void> => {
    if (selectedFiles.size === 0) {
      translationStore.setError('다운로드할 파일을 선택하세요.');
      return;
    }

    const fileNames = Array.from(selectedFiles);

    // 번역된 파일만 필터링
    const translatedFiles = fileNames.filter(fileName => {
      const metadata = fileMetadata.find(f => f.fileName === fileName);
      return metadata?.translated === true;
    });

    // 번역된 파일이 없는 경우
    if (translatedFiles.length === 0) {
      translationStore.setError('다운로드 할 수 있는 번역 파일이 없습니다.');
      return;
    }

    translationStore.clearError();

    try {
      for (const fileName of translatedFiles) {
        const blob = await downloadTranslatedFile(fileName);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName.replace(/\.(asta|astah|svg)$/, '_translated.$1');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }
    } catch (err) {
      translationStore.setError('파일 다운로드에 실패했습니다.');
      console.error(err);
    }
  };

  const handleBatchDelete = async (): Promise<void> => {
    if (selectedFiles.size === 0) {
      translationStore.setError('삭제할 파일을 선택하세요.');
      return;
    }

    const fileNames = Array.from(selectedFiles);

    if (!confirm(`선택한 ${fileNames.length}개의 파일을 삭제하시겠습니까?`)) {
      return;
    }

    translationStore.clearError();

    try {
      await deleteBatch(fileNames);
      await fetchFiles();
      translationStore.deselectAll();
    } catch (err) {
      const error = err as { response?: { data?: { error?: string } }; message?: string };
      translationStore.setError(`배치 삭제 실패: ${error.response?.data?.error || error.message}`);
      console.error(err);
    }
  };

  const formatDate = (dateStr: string | null): string => {
    if (!dateStr) return '-';
    return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss');
  };

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

      {outlinedWarning.length > 0 && (
        <div className="bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-400 dark:border-yellow-700 text-yellow-800 dark:text-yellow-400 px-4 py-3 rounded mb-4 flex items-start gap-2">
          <span className="material-icons text-xl">warning</span>
          <div>
            <p className="font-medium">텍스트 추출 불가 파일</p>
            <p className="text-sm mt-1">
              다음 SVG 파일은 텍스트가 패스로 변환(아웃라인화)되어 번역할 수 없습니다:
            </p>
            <ul className="text-sm mt-1 list-disc list-inside">
              {outlinedWarning.map(f => <li key={f}>{f}</li>)}
            </ul>
            <button
              onClick={() => setOutlinedWarning([])}
              className="text-sm underline mt-2 hover:opacity-70"
            >
              닫기
            </button>
          </div>
        </div>
      )}

      {batchProgress && (
        <div className="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-4 mb-6">
          <p className="text-sm text-gray-700 dark:text-gray-300 mb-2">
            번역 중: {batchProgress.current}/{batchProgress.total} - {batchProgress.currentFile}
          </p>
          <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
            <div
              className="bg-primary h-2 rounded-full transition-all duration-300"
              style={{ width: `${(batchProgress.current / batchProgress.total) * 100}%` }}
            />
          </div>
        </div>
      )}

      <div className="flex flex-wrap items-center gap-3 mb-6">
        <button
          onClick={handleBatchTranslate}
          disabled={selectedFiles.size === 0 || !!batchProgress}
          className="bg-primary text-white px-4 py-2 rounded-md font-medium text-sm hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
        >
          선택 파일 번역 ({selectedFiles.size})
        </button>
        <button
          onClick={handleBatchDownload}
          disabled={selectedFiles.size === 0}
          className="bg-button-secondary text-line px-4 py-2 rounded-md font-medium text-sm hover:bg-opacity-80 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          번역 파일 다운로드 ({selectedFiles.size})
        </button>
        <button
          onClick={handleBatchDelete}
          disabled={selectedFiles.size === 0}
          className="bg-button-secondary text-line px-4 py-2 rounded-md font-medium text-sm hover:bg-opacity-80 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          선택 파일 삭제 ({selectedFiles.size})
        </button>
      </div>

      <hr className="border-t-2 border-line mb-6"/>

      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200">Target 파일 목록</h3>
        <span className="bg-gray-200 dark:bg-gray-600 text-gray-700 dark:text-gray-300 text-xs font-medium px-2.5 py-1 rounded-full">
          {fileMetadata.length}개
        </span>
      </div>

      <div className={`overflow-x-auto ${isDragOver ? 'drop-target' : ''}`}>
        <table
               className="w-full text-sm text-left text-gray-600 dark:text-gray-400"
               onDragOver={handleDragOver}
               onDragLeave={handleDragLeave}
               onDrop={handleDrop}>
          <thead className="text-sm text-gray-600 dark:text-gray-300 bg-gray-200 dark:bg-gray-700">
            <tr>
              <th scope="col" className="p-4 w-4">
                <input
                  type="checkbox"
                  checked={selectedFiles.size === fileMetadata.length && fileMetadata.length > 0}
                  onChange={handleSelectAll}
                  className="w-4 h-4 text-primary bg-gray-100 border-gray-300 rounded focus:ring-primary dark:focus:ring-offset-gray-800 focus:ring-2 dark:bg-gray-600 dark:border-gray-500"
                />
              </th>
              <th scope="col" className="px-6 py-3 font-medium text-left">파일명</th>
              <th scope="col" className="px-6 py-3 font-medium text-left">번역됨</th>
              <th scope="col" className="px-6 py-3 font-medium text-left">업로드 날짜</th>
              <th scope="col" className="px-6 py-3 font-medium text-left">번역된 날짜</th>
            </tr>
          </thead>
          <tbody>
            {uploading && <tr><td colSpan={5} className="px-6 py-3 font-medium text-left uploading-message">업로드 중...</td></tr>}

            {fileMetadata.map((file, index) => (
              <tr
                key={file.fileName}
                className={`bg-white dark:bg-gray-800 ${index < fileMetadata.length - 1 ? 'border-b dark:border-gray-700' : ''} hover:bg-gray-50 dark:hover:bg-gray-600`}
              >
                <td className="w-4 p-4">
                  <input
                    type="checkbox"
                    checked={selectedFiles.has(file.fileName)}
                    onChange={() => handleCheckboxChange(file.fileName)}
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
                <td className="px-6 py-4">{formatDate(file.uploadedAt)}</td>
                <td className="px-6 py-4">{formatDate(file.translatedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
