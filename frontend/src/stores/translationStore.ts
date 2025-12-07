import { create } from 'zustand';
import { AxiosError } from 'axios';
import { FileType, ApiErrorResponse } from '../types';
import {
  uploadAstaFile,
  applyTranslationIntegrated,
  downloadTranslatedFile,
  uploadSvgFile,
  applySvgTranslationIntegrated,
  downloadTranslatedSvgFile,
} from '../services/api';

// State type contains only data (no functions)
type TranslationStore = {
  sessionId: string;
  selectedFile: File | null;
  fileType: FileType;
  originalFilename: string;
  status: string;
  loading: boolean;
  error: unknown | null;
};

// create() returns only initial state (no actions)
const useStore = create<TranslationStore>(() => ({
  sessionId: '',
  selectedFile: null,
  fileType: '',
  originalFilename: '',
  status: '',
  loading: false,
  error: null,
}));

// Helper function for file download
const downloadFile = (blob: Blob, filename: string): void => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};

// All actions are defined in a separate store object
const store = {
  setSelectedFile: (file: File | null, type: FileType, statusMessage: string) => {
    useStore.setState({
      selectedFile: file,
      fileType: type,
      status: statusMessage,
      error: null,
    });
  },

  uploadFile: async () => {
    const { selectedFile, fileType } = useStore.getState();

    if (!selectedFile) {
      useStore.setState({ status: '파일을 선택해주세요.', error: null });
      return;
    }

    try {
      useStore.setState({ loading: true, status: '파일 업로드 중...', error: null });

      let response;
      if (fileType === 'asta') {
        response = await uploadAstaFile(selectedFile);
      } else if (fileType === 'svg') {
        response = await uploadSvgFile(selectedFile);
      } else {
        useStore.setState({
          status: '파일 타입을 알 수 없습니다.',
          loading: false,
          error: null,
        });
        return;
      }

      useStore.setState({
        sessionId: response.sessionId,
        originalFilename: selectedFile.name,
        status: `업로드 완료! 세션 ID: ${response.sessionId}`,
        error: null,
      });
    } catch (error) {
      const axiosError = error as AxiosError<ApiErrorResponse>;
      useStore.setState({
        status: `업로드 실패: ${axiosError.response?.data?.error || axiosError.message}`,
        error,
      });
    } finally {
      useStore.setState({ loading: false });
    }
  },

  translateAndDownload: async () => {
    const { sessionId, fileType, originalFilename } = useStore.getState();

    if (!sessionId) {
      useStore.setState({ status: '먼저 파일을 업로드해주세요.', error: null });
      return;
    }

    if (!fileType) {
      useStore.setState({ status: '파일 타입을 알 수 없습니다.', error: null });
      return;
    }

    try {
      useStore.setState({ loading: true, error: null });

      useStore.setState({ status: '일본어 추출 중...' });
      await new Promise((resolve) => setTimeout(resolve, 500));

      useStore.setState({ status: '번역 중...' });
      await new Promise((resolve) => setTimeout(resolve, 500));

      useStore.setState({ status: '번역 적용 중...' });

      if (fileType === 'asta') {
        await applyTranslationIntegrated(sessionId);
        useStore.setState({ status: '번역 적용 완료! 파일 다운로드 중...' });
        const fileBlob = await downloadTranslatedFile(sessionId);
        const baseName = originalFilename.replace(/\.(asta|astah)$/i, '');
        downloadFile(fileBlob, `${baseName}_translated.asta`);
      } else if (fileType === 'svg') {
        await applySvgTranslationIntegrated(sessionId);
        useStore.setState({ status: '번역 적용 완료! 파일 다운로드 중...' });
        const fileBlob = await downloadTranslatedSvgFile(sessionId);
        const baseName = originalFilename.replace(/\.svg$/i, '');
        downloadFile(fileBlob, `${baseName}_translated.svg`);
      }

      if (fileType === 'svg') {
        useStore.setState({
          status: '번역된 SVG 파일을 다운로드했습니다! (텍스트에 마우스를 호버하면 번역이 표시됩니다)',
          error: null,
        });
      } else {
        useStore.setState({ status: '번역된 파일을 다운로드했습니다!', error: null });
      }
    } catch (error) {
      const axiosError = error as AxiosError<ApiErrorResponse>;
      useStore.setState({
        status: `번역 실패: ${axiosError.response?.data?.error || axiosError.message}`,
        error,
      });
    } finally {
      useStore.setState({ loading: false });
    }
  },

  reset: () => useStore.setState(useStore.getInitialState()),
};

// Export both React hook and action object
export const useTranslationStore = useStore;
export const translationStore = store;
