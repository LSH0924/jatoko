import React from 'react';

interface TranslationSectionProps {
  sessionId: string;
  loading: boolean;
  onTranslateAndDownload: () => void;
}

export function TranslationSection({
  sessionId,
  loading,
  onTranslateAndDownload,
}: TranslationSectionProps): React.ReactElement {
  return (
    <section className="step">
      <h2>2단계: 자동 번역 및 다운로드</h2>
      <button onClick={onTranslateAndDownload} disabled={loading || !sessionId}>
        번역 및 다운로드
      </button>
      <p className="info-text">
        일본어 텍스트를 자동으로 추출하고 번역하여 파일을 다운로드합니다.
      </p>
    </section>
  );
}
