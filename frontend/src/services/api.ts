import axios, { AxiosInstance } from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// API 응답 타입 정의

export interface TranslationResponse {
  message: string;
  sessionId: string;
}

export interface FileMetadata {
  fileName: string;
  translated: boolean;
  uploadedAt: string | null;
  translatedAt: string | null;
  outlined: boolean;  // SVG 아웃라인 여부 (텍스트 추출 불가)
}

export interface UploadResponse {
  fileName: string;
  outlined: boolean;
  message: string;
}

export interface BatchTranslationResponse {
  successFiles: string[];
  failedFiles: string[];
  totalCount: number;
  successCount: number;
  failedCount: number;
}

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// --- Generic Helper Functions ---

const uploadFile = async <T>(url: string, file: File): Promise<T> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axios.post<T>(`${API_BASE_URL}${url}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

const postData = async <T>(url: string, data: any): Promise<T> => {
  const response = await api.post<T>(url, data);
  return response.data;
};

const downloadBlob = async (url: string): Promise<Blob> => {
  const response = await axios.get<Blob>(`${API_BASE_URL}${url}`, {
    responseType: 'blob',
  });
  return response.data;
};

const deleteData = async (url: string): Promise<void> => {
  await api.delete(url);
};

// --- Exported Service Functions ---

// 디렉토리 파일 목록 조회
export const getDirectoryFiles = async (directory: string): Promise<string[]> => {
  const response = await api.get<string[]>(`/files/${directory}`);
  return response.data;
};

// Target 디렉토리에 파일 업로드
export const uploadToTarget = async (file: File): Promise<UploadResponse> => {
  return uploadFile<UploadResponse>('/files/target', file);
};

// Translated 디렉토리에서 파일 다운로드
export const downloadFromTranslated = async (fileName: string): Promise<Blob> => {
  return downloadBlob(`/files/translated/${fileName}`);
};

// Target 파일 번역
export const translateTargetFile = async (fileName: string): Promise<TranslationResponse> => {
  return postData<TranslationResponse>('/translate-file', { fileName });
};

// 파일 삭제 (target 또는 translated 디렉토리)
export const deleteFile = async (type: string, fileName: string): Promise<void> => {
  await deleteData(`/files/${type}/${fileName}`);
};

// 파일 메타데이터 조회
export const getFileMetadata = async (): Promise<FileMetadata[]> => {
  const response = await api.get<FileMetadata[]>('/files/metadata');
  return response.data;
};

// 배치 번역
export const translateBatch = async (fileNames: string[]): Promise<BatchTranslationResponse> => {
  return postData<BatchTranslationResponse>('/translate/batch', { fileNames });
};

// 번역된 파일 다운로드 (target 파일명으로 최신 번역 파일 다운로드)
export const downloadTranslatedFile = async (targetFileName: string): Promise<Blob> => {
  return downloadBlob(`/download/translated/${targetFileName}`);
};

// 배치 삭제
export const deleteBatch = async (fileNames: string[]): Promise<void> => {
  await postData<void>('/files/batch-delete', { fileNames });
};

export default api;
