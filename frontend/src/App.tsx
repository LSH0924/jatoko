import React from 'react';
import './App.css';
import { StatusDisplay } from './components/StatusDisplay';
import { FileListPanel } from './components/FileListPanel';
import { useTranslationStore } from './stores/translationStore';

function App(): React.ReactElement {
  const { status, loading } = useTranslationStore();

  return (
    <div className="App">
      <header className="App-header">
        <h1>JPToko - 일본어 번역 도구</h1>
        <p className="subtitle">Astah (.asta) 및 SVG (.svg) 파일 지원</p>
      </header>

      <main className="App-main">
        <FileListPanel
          onFileSelect={(fileName, type) => {
            useTranslationStore.setState({ status: `선택된 파일: ${fileName} (${type})` });
          }}
        />
        <StatusDisplay loading={loading} status={status} />
      </main>
    </div>
  );
}

export default App;
