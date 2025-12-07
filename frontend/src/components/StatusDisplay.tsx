import React from 'react';

interface StatusDisplayProps {
  loading: boolean;
  status: string;
}

export function StatusDisplay({
  loading,
  status,
}: StatusDisplayProps): React.ReactElement {
  return (
    <section className="status">
      {loading && <div className="spinner"></div>}
      <p className={loading ? 'loading' : ''}>{status}</p>
    </section>
  );
}
