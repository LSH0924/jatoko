import { useState, useCallback, DragEvent } from 'react';
import { useTranslationStore, translationStore } from '../stores/translationStore';
import {
  getFileMetadata,
  uploadToTarget,
  translateTargetFile,
  downloadTranslatedFile,
  deleteBatch,
  generateClientId,
  subscribeToProgress,
  closeEventSource
} from '../services/api';

export function useFileManagement() {
  const fileMetadata = useTranslationStore((s) => s.fileMetadata);
  const selectedFiles = useTranslationStore((s) => s.selectedFiles);
  const loading = useTranslationStore((s) => s.loading);
  const error = useTranslationStore((s) => s.error);
  const batchProgress = useTranslationStore((s) => s.batchProgress);
  const fileProgress = useTranslationStore((s) => s.fileProgress);

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

    const selectedFileNames = Array.from(selectedFiles);
    const outlinedFiles = selectedFileNames.filter(fileName => {
      const metadata = fileMetadata.find(f => f.fileName === fileName);
      return metadata?.outlined === true;
    });
    const translatableFiles = selectedFileNames.filter(fileName => {
      const metadata = fileMetadata.find(f => f.fileName === fileName);
      return metadata?.outlined !== true;
    });

    if (translatableFiles.length === 0) {
      translationStore.setError(
        `선택한 파일이 모두 텍스트가 패스로 변환(아웃라인화)되어 번역할 수 없습니다: ${outlinedFiles.join(', ')}`
      );
      return;
    }

    if (outlinedFiles.length > 0) {
      translationStore.setError(
        `다음 파일은 아웃라인화되어 번역에서 제외됩니다: ${outlinedFiles.join(', ')}`
      );
    } else {
      translationStore.clearError();
    }

    const fileNames = translatableFiles;

    try {
      translationStore.setBatchProgress({
        total: fileNames.length,
        current: 0,
        currentFile: '',
      });

      for (let i = 0; i < fileNames.length; i++) {
        const fileName = fileNames[i];

        translationStore.setBatchProgress({
          total: fileNames.length,
          current: i,
          currentFile: fileName,
        });

        translationStore.setFileProgress({
          message: '번역 준비 중...',
          percentage: 0
        });

        const clientId = generateClientId();
        let eventSource: EventSource | null = null;

        try {
          await new Promise<void>((resolve, reject) => {
            eventSource = subscribeToProgress(
              clientId,
              (event) => {
                const type = event.type;
                if (type === 'progress') {
                  const data = JSON.parse(event.data);
                  translationStore.setFileProgress({
                    message: data.message,
                    percentage: data.percentage
                  });
                } else if (type === 'complete') {
                  resolve();
                } else if (type === 'error') {
                  const data = JSON.parse(event.data);
                  console.error("SSE Error:", data.message);
                  reject(new Error(data.message));
                }
              },
              (error) => {
                console.log("SSE Connection closed or error", error);
              }
            );

            translateTargetFile(fileName, clientId)
              .then(() => {
                // SSE complete 이벤트에서 resolve됨
              })
              .catch((err) => reject(err));
          });

        } catch (e) {
          console.error(`Failed to translate ${fileName}`, e);
          throw e;
        } finally {
          if (eventSource) {
            closeEventSource(eventSource);
          }
          translationStore.setFileProgress(null);
        }
      }

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
      translationStore.setFileProgress(null);
    }
  };

  const handleBatchDownload = async (): Promise<void> => {
    if (selectedFiles.size === 0) {
      translationStore.setError('다운로드할 파일을 선택하세요.');
      return;
    }

    const fileNames = Array.from(selectedFiles);
    const translatedFiles = fileNames.filter(fileName => {
      const metadata = fileMetadata.find(f => f.fileName === fileName);
      return metadata?.translated === true;
    });

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

  return {
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
  };
}
