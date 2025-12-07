import React from 'react';
import './App.css';
import { FileListPanel } from './components/FileListPanel';

function App(): React.ReactElement {
  return (
    <div className="App">
      <header className="App-header">
        <h1>JaToKo - 일본어 번역기</h1>
        <p className="subtitle">Astah (.asta) 및 SVG (.svg) 파일 지원</p>
      </header>

      <main className="App-main">
        <FileListPanel />
      </main>
    </div>
  );
}

export default App;
