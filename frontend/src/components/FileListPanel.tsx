import React, { useState, useEffect, useCallback, DragEvent } from 'react';
import {
  getFileMetadata,
  uploadToTarget,
  translateBatch,
  downloadTranslatedFile,
  deleteFile,
} from '../services/api';
import { useTranslationStore, translationStore } from '../stores/translationStore';

export function FileListPanel(): React.ReactElement {
  const fileMetadata = useTranslationStore((s) => s.fileMetadata);
  const selectedFiles = useTranslationStore((s) => s.selectedFiles);
  const loading = useTranslationStore((s) => s.loading);
  const error = useTranslationStore((s) => s.error);
  const batchProgress = useTranslationStore((s) => s.batchProgress);

  const [uploading, setUploading] = useState<boolean>(false);
  const [isDragOver, setIsDragOver] = useState<boolean>(false);

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

    try {
      for (const file of validFiles) {
        await uploadToTarget(file);
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

    const fileNames = Array.from(selectedFiles);
    translationStore.clearError();

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
      translationStore.deselectAll();
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
    translationStore.clearError();

    try {
      for (const fileName of fileNames) {
        const metadata = fileMetadata.find(f => f.fileName === fileName);
        if (!metadata || !metadata.translated) {
          console.warn(`${fileName}은 번역되지 않은 파일입니다. 건너뜁니다.`);
          continue;
        }

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

  const handleDelete = async (fileName: string, e: React.MouseEvent<HTMLButtonElement>): Promise<void> => {
    e.stopPropagation();

    if (!confirm(`"${fileName}" 파일을 삭제하시겠습니까?`)) {
      return;
    }

    translationStore.clearError();

    try {
      await deleteFile('target', fileName);
      await fetchFiles();
    } catch (err) {
      translationStore.setError('파일 삭제에 실패했습니다.');
      console.error(err);
    }
  };

  const formatDate = (dateStr: string | null): string => {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="file-list-container">
      <div className="file-list-header">
        <h2>파일 목록</h2>
        <button onClick={fetchFiles} disabled={loading} className="refresh-btn">
          {loading ? '로딩...' : '새로고침'}
        </button>
      </div>

      {error && <p className="error-message">{error}</p>}

      {batchProgress && (
        <div className="batch-progress">
          <p>
            번역 중: {batchProgress.current}/{batchProgress.total} - {batchProgress.currentFile}
          </p>
          <div className="progress-bar">
            <div
              className="progress-fill"
              style={{ width: `${(batchProgress.current / batchProgress.total) * 100}%` }}
            />
          </div>
        </div>
      )}

      <div className="batch-actions">
        <button onClick={handleBatchTranslate} disabled={selectedFiles.size === 0 || !!batchProgress}>
          선택 파일 번역 ({selectedFiles.size})
        </button>
        <button onClick={handleBatchDownload} disabled={selectedFiles.size === 0}>
          선택 파일 다운로드 ({selectedFiles.size})
        </button>
      </div>

      <div
        className={`file-panel ${isDragOver ? 'drop-target' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <div className="file-panel-header">
          <h3>Target 파일 목록</h3>
          <span className="file-count">{fileMetadata.length}개</span>
        </div>

        {uploading && <p className="uploading-message">업로드 중...</p>}

        {fileMetadata.length === 0 && !uploading ? (
          <p className="empty-message">.asta, .svg 파일을 여기에 드래그하여 업로드</p>
        ) : (
          <table className="file-table">
            <thead>
              <tr>
                <th>
                  <input
                    type="checkbox"
                    checked={selectedFiles.size === fileMetadata.length && fileMetadata.length > 0}
                    onChange={handleSelectAll}
                  />
                </th>
                <th>파일명</th>
                <th>번역됨</th>
                <th>업로드 날짜</th>
                <th>번역된 날짜</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              {fileMetadata.map((file) => (
                <tr key={file.fileName}>
                  <td>
                    <input
                      type="checkbox"
                      checked={selectedFiles.has(file.fileName)}
                      onChange={() => handleCheckboxChange(file.fileName)}
                    />
                  </td>
                  <td>{file.fileName}</td>
                  <td className="translated-status">{file.translated ? '✓' : '✗'}</td>
                  <td>{formatDate(file.uploadedAt)}</td>
                  <td>{formatDate(file.translatedAt)}</td>
                  <td>
                    <button
                      className="delete-btn"
                      onClick={(e) => handleDelete(file.fileName, e)}
                      title="삭제"
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
