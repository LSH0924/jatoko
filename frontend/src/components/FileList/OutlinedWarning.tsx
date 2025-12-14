import React from 'react';

interface OutlinedWarningProps {
  files: string[];
  onClose: () => void;
}

export function OutlinedWarning({ files, onClose }: OutlinedWarningProps): React.ReactElement | null {
  if (files.length === 0) return null;

  return (
    <div className="bg-yellow-50 dark:bg-yellow-900/30 border border-yellow-400 dark:border-yellow-700 text-yellow-800 dark:text-yellow-400 px-4 py-3 rounded mb-4 flex items-start gap-2">
      <span className="material-icons text-xl">warning</span>
      <div>
        <p className="font-medium">텍스트 추출 불가 파일</p>
        <p className="text-sm mt-1">
          다음 SVG 파일은 텍스트가 패스로 변환(아웃라인화)되어 번역할 수 없습니다:
        </p>
        <ul className="text-sm mt-1 list-disc list-inside">
          {files.map(f => <li key={f}>{f}</li>)}
        </ul>
        <button
          onClick={onClose}
          className="text-sm underline mt-2 hover:opacity-70"
        >
          닫기
        </button>
      </div>
    </div>
  );
}
