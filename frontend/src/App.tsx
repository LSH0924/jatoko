import React from 'react';
import './App.css';
import { FileListPanel } from './components/FileListPanel';
import { useTranslationStore } from './stores/translationStore';

function App(): React.ReactElement {
  const { status, loading } = useTranslationStore();

  return (
    <div className="App">
      <header className="App-header">
        <h1>JaToKo - 일본어 번역기</h1>
        <p className="subtitle">Astah (.asta) 및 SVG (.svg) 파일 지원</p>
      </header>

      <main className="App-main">
          <FileListPanel
          onFileSelect={(fileName, type) => {
            useTranslationStore.setState({ status: `선택된 파일: ${fileName} (${type})` });
          }}
        />
          <section className="status">
              {loading && <div className="spinner"></div>}
              <p className={loading ? 'loading' : ''}>{status}</p>
          </section>
      </main>
    </div>
  );
}

export default App;
