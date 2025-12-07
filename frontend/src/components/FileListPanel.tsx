import React, { useState, useEffect, useCallback, DragEvent } from 'react';
import { getDirectoryFiles, uploadToTarget, downloadFromTranslated, translateTargetFile, deleteFile } from '../services/api';

interface FileListPanelProps {
  onFileSelect?: (fileName: string, type: string) => void;
}

export function FileListPanel({ onFileSelect }: FileListPanelProps): React.ReactElement {
  const [targetFiles, setTargetFiles] = useState<string[]>([]);
  const [translatedFiles, setTranslatedFiles] = useState<string[]>([]);
  const [selectedTranslatedFile, setSelectedTranslatedFile] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [uploading, setUploading] = useState<boolean>(false);
  const [translating, setTranslating] = useState<Record<string, boolean>>({});
  const [error, setError] = useState<string>('');
  const [isDragOver, setIsDragOver] = useState<boolean>(false);

  const ALLOWED_EXTENSIONS = ['.asta', '.astah', '.svg'];

  const fetchFiles = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [target, translated] = await Promise.all([
        getDirectoryFiles('target'),
        getDirectoryFiles('translated'),
      ]);
      setTargetFiles(target);
      setTranslatedFiles(translated);
    } catch (err) {
      setError('파일 목록을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      setLoading(false);
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
      setError('.asta, .svg 파일만 업로드 가능합니다.');
      return;
    }

    if (validFiles.length !== files.length) {
      setError(`${files.length - validFiles.length}개의 파일이 지원되지 않는 형식입니다. .asta, .svg만 허용됩니다.`);
    }

    setUploading(true);

    try {
      for (const file of validFiles) {
        await uploadToTarget(file);
      }
      await fetchFiles();
    } catch (err) {
      setError('파일 업로드에 실패했습니다.');
      console.error(err);
    } finally {
      setUploading(false);
    }
  };

  const handleTranslate = async (fileName: string, e: React.MouseEvent<HTMLButtonElement>): Promise<void> => {
    e.stopPropagation();

    setTranslating(prev => ({ ...prev, [fileName]: true }));
    setError('');

    try {
      await translateTargetFile(fileName);
      await fetchFiles();
    } catch (err) {
      const error = err as { response?: { data?: { error?: string } }; message?: string };
      setError(`번역 실패: ${error.response?.data?.error || error.message}`);
      console.error(err);
    } finally {
      setTranslating(prev => ({ ...prev, [fileName]: false }));
    }
  };

  const handleDownload = async (e?: React.MouseEvent<HTMLButtonElement>): Promise<void> => {
    if (e) {
      e.stopPropagation();
    }

    if (!selectedTranslatedFile) {
      setError('다운로드할 파일을 선택하세요.');
      return;
    }

    try {
      const blob = await downloadFromTranslated(selectedTranslatedFile);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = selectedTranslatedFile;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError('파일 다운로드에 실패했습니다.');
      console.error(err);
    }
  };

  const handleDelete = async (type: string, fileName: string, e: React.MouseEvent<HTMLButtonElement>): Promise<void> => {
    e.stopPropagation();

    if (!confirm(`"${fileName}" 파일을 삭제하시겠습니까?`)) {
      return;
    }

    setError('');

    try {
      await deleteFile(type, fileName);
      if (selectedTranslatedFile === fileName) {
        setSelectedTranslatedFile(null);
      }
      await fetchFiles();
    } catch (err) {
      setError('파일 삭제에 실패했습니다.');
      console.error(err);
    }
  };

  const handleFileClick = (fileName: string, type: string): void => {
    if (type === 'translated') {
      setSelectedTranslatedFile(fileName);
    }

    if (onFileSelect) {
      onFileSelect(fileName, type);
    }
  };

  const renderTargetPanel = (): React.ReactElement => (
    <div
      className={`file-panel ${isDragOver ? 'drop-target' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="file-panel-header">
        <h3>Target (원본)</h3>
        <span className="file-count">{targetFiles.length}개</span>
      </div>
      <div className="file-list">
        {uploading && (
          <p className="uploading-message">업로드 중...</p>
        )}
        {targetFiles.length === 0 && !uploading ? (
          <p className="empty-message">
            .asta, .svg 파일을 여기에 드래그하여 업로드
          </p>
        ) : (
          targetFiles.map((fileName) => (
            <div
              key={fileName}
              className="file-item"
              onClick={() => handleFileClick(fileName, 'target')}
            >
              <span className="file-name">{fileName}</span>
              <div className="file-actions">
                <button
                  className="translate-btn"
                  onClick={(e) => handleTranslate(fileName, e)}
                  disabled={translating[fileName]}
                >
                  {translating[fileName] ? '번역 중...' : '번역'}
                </button>
                <button
                  className="delete-btn"
                  onClick={(e) => handleDelete('target', fileName, e)}
                  title="삭제"
                >
                  ✕
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );

  const renderTranslatedPanel = (): React.ReactElement => (
    <div className="file-panel">
      <div className="file-panel-header">
        <h3>Translated (번역됨)</h3>
        <span className="file-count">{translatedFiles.length}개</span>
      </div>
      <div className="file-list">
        {translatedFiles.length === 0 ? (
          <p className="empty-message">번역된 파일이 없습니다</p>
        ) : (
          translatedFiles.map((fileName) => (
            <div
              key={fileName}
              className={`file-item ${selectedTranslatedFile === fileName ? 'selected' : ''}`}
              onClick={() => handleFileClick(fileName, 'translated')}
            >
              <span className="file-name">{fileName}</span>
              <button
                className="delete-btn"
                onClick={(e) => handleDelete('translated', fileName, e)}
                title="삭제"
              >
                ✕
              </button>
            </div>
          ))
        )}
      </div>
      {translatedFiles.length > 0 && (
        <div className="translated-actions">
          <button
            className="download-btn"
            onClick={handleDownload}
            disabled={!selectedTranslatedFile}
          >
            다운로드
          </button>
        </div>
      )}
    </div>
  );

  return (
    <div className="file-list-container">
      <div className="file-list-header">
        <h2>파일 목록</h2>
        <button onClick={fetchFiles} disabled={loading} className="refresh-btn">
          {loading ? '로딩...' : '새로고침'}
        </button>
      </div>

      {error && <p className="error-message">{error}</p>}

      <div className="file-panels">
        {renderTargetPanel()}
        {renderTranslatedPanel()}
      </div>
    </div>
  );
}
