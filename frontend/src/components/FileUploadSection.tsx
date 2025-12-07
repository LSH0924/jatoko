import React, { ChangeEvent } from 'react';
import { FileType } from '../types';

interface FileUploadSectionProps {
  selectedFile: File | null;
  fileType: FileType;
  sessionId: string;
  loading: boolean;
  onFileSelect: (e: ChangeEvent<HTMLInputElement>) => void;
  onUpload: () => void;
}

export function FileUploadSection({
  selectedFile,
  fileType,
  sessionId,
  loading,
  onFileSelect,
  onUpload,
}: FileUploadSectionProps): React.ReactElement {
  return (
    <section className="step">
      <h2>1단계: 파일 업로드</h2>
      <div className="file-input-group">
        <input
          type="file"
          accept=".asta,.svg"
          onChange={onFileSelect}
          disabled={loading}
        />
        <button onClick={onUpload} disabled={loading || !selectedFile}>
          업로드
        </button>
      </div>
      {fileType && (
        <p className="file-type-info">
          파일 타입: <strong>{fileType.toUpperCase()}</strong>
        </p>
      )}
      {sessionId && <p className="session-info">세션 ID: {sessionId}</p>}
    </section>
  );
}
