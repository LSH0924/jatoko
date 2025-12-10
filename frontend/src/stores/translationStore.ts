import { create } from 'zustand';
import { FileMetadata } from '../services/api';

// State type contains only data (no functions)
type TranslationStore = {
  fileMetadata: FileMetadata[];
  selectedFiles: Set<string>;
  loading: boolean;
  error: string;
  batchProgress: {
    total: number;
    current: number;
    currentFile: string;
  } | null;
  fileProgress: {
    message: string;
    percentage: number;
  } | null;
};

// create() returns only initial state (no actions)
const useStore = create<TranslationStore>(() => ({
  fileMetadata: [],
  selectedFiles: new Set<string>(),
  loading: false,
  error: '',
  batchProgress: null,
  fileProgress: null,
}));

// Action object separated from state
const store = {
  setFileMetadata: (metadata: FileMetadata[]) => {
    useStore.setState({ fileMetadata: metadata });
  },

  setSelectedFiles: (files: Set<string>) => {
    useStore.setState({ selectedFiles: new Set(files) });
  },

  toggleFileSelection: (fileName: string) => {
    const state = useStore.getState();
    const newSelected = new Set(state.selectedFiles);
    if (newSelected.has(fileName)) {
      newSelected.delete(fileName);
    } else {
      newSelected.add(fileName);
    }
    useStore.setState({ selectedFiles: newSelected });
  },

  selectAll: () => {
    const state = useStore.getState();
    const allFiles = new Set(state.fileMetadata.map(f => f.fileName));
    useStore.setState({ selectedFiles: allFiles });
  },

  deselectAll: () => {
    useStore.setState({ selectedFiles: new Set() });
  },

  setLoading: (loading: boolean) => {
    useStore.setState({ loading });
  },

  setError: (error: string) => {
    useStore.setState({ error });
  },

  setBatchProgress: (progress: { total: number; current: number; currentFile: string } | null) => {
    useStore.setState({ batchProgress: progress });
  },

  setFileProgress: (progress: { message: string; percentage: number } | null) => {
    useStore.setState({ fileProgress: progress });
  },

  clearError: () => {
    useStore.setState({ error: '' });
  },
};

// Export both React hook and action object
export const useTranslationStore = useStore;
export const translationStore = store;
