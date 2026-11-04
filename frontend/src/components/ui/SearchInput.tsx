import React, { forwardRef } from 'react';
import { Search, X } from 'lucide-react';

export interface SearchInputProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  onClear?: () => void;
  showKbd?: boolean;
}

export const SearchInput = forwardRef<HTMLInputElement, SearchInputProps>(
  (
    {
      className = '',
      value,
      onChange,
      onClear,
      placeholder = 'Search developers, projects, technologies...',
      showKbd = true,
      ...props
    },
    ref
  ) => {
    return (
      <div className={`relative flex items-center w-full ${className}`}>
        <div className="absolute left-3.5 flex items-center pointer-events-none text-muted">
          <Search className="w-4 h-4" />
        </div>
        <input
          ref={ref}
          type="text"
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          className="w-full h-10 pl-10 pr-20 rounded-lg bg-surface border border-border text-sm text-foreground placeholder:text-muted/70 transition-all focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/40"
          {...props}
        />
        <div className="absolute right-3 flex items-center gap-1.5">
          {value && onClear && (
            <button
              type="button"
              onClick={onClear}
              className="p-1 rounded text-muted hover:text-foreground transition-colors"
              aria-label="Clear search"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
          {showKbd && (
            <kbd className="hidden sm:inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-mono font-medium text-muted bg-card border border-border select-none pointer-events-none">
              Ctrl + K
            </kbd>
          )}
        </div>
      </div>
    );
  }
);

SearchInput.displayName = 'SearchInput';

