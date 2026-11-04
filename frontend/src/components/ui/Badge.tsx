import React from 'react';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'neutral' | 'subtle';
  size?: 'sm' | 'md';
}

export function Badge({
  className = '',
  variant = 'default',
  size = 'md',
  children,
  ...props
}: BadgeProps) {
  const base =
    'inline-flex items-center font-medium rounded-full transition-colors select-none';

  const variants = {
    default:
      'bg-surface text-foreground border border-border',
    primary:
      'bg-red-500/10 text-red-500 border border-red-500/30',
    success:
      'bg-emerald-500/10 text-emerald-500 border border-emerald-500/30',
    warning:
      'bg-amber-500/10 text-amber-500 border border-amber-500/30',
    neutral:
      'bg-surface-hover text-muted-foreground border border-border',
    subtle:
      'bg-transparent text-muted border border-border',
  };

  const sizes = {
    sm: 'text-[11px] px-2 py-0.5',
    md: 'text-xs px-2.5 py-1',
  };

  return (
    <span
      className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    >
      {children}
    </span>
  );
}

