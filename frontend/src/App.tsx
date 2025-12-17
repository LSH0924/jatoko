import React from 'react';
import { FileListPanel } from './components/FileListPanel';

function App(): React.ReactElement {
  return (
    <div className="bg-background-light dark:bg-background-dark font-display text-gray-800 dark:text-gray-200 min-h-screen">
      <div className="container mx-auto p-4 sm:p-6 md:p-8">
        <header className="text-center mb-10">
            <img src="logo.svg" alt="logo" className="block mx-auto py-4"/>
            <p className="text-gray-600 dark:text-gray-400 mt-2 text-lg">
            Astah (.asta) 및 SVG (.svg) 파일 지원
            </p>
            <p className="text-gray-600 dark:text-gray-400 mt-2 text-base">
                같은 파일을 여러 번 업로드 시 기존 버전에서 변경점만 찾아 새로 번역합니다.
            </p>
        </header>

        <main className="max-w-6xl mx-auto">
          <FileListPanel />
        </main>
      </div>
    </div>
  );
}

export default App;
