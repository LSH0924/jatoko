import { create } from 'zustand';

// State type contains only data (no functions)
type TranslationStore = {
  status: string;
  loading: boolean;
};

// create() returns only initial state (no actions)
const useStore = create<TranslationStore>(() => ({
  status: '',
  loading: false,
}));

// Export both React hook and action object
export const useTranslationStore = useStore;
