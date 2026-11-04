import React from 'react';
import Image from 'next/image';

export interface AvatarProps {
  src?: string;
  name: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  isOnline?: boolean;
  className?: string;
}

export function Avatar({
  src,
  name,
  size = 'md',
  isOnline,
  className = '',
}: AvatarProps) {
  const sizeMap = {
    xs: { container: 'w-6 h-6 text-[10px]', px: 24, dot: 'w-1.5 h-1.5' },
    sm: { container: 'w-8 h-8 text-xs', px: 32, dot: 'w-2 h-2' },
    md: { container: 'w-10 h-10 text-sm', px: 40, dot: 'w-2.5 h-2.5' },
    lg: { container: 'w-14 h-14 text-base font-medium', px: 56, dot: 'w-3 h-3' },
    xl: { container: 'w-20 h-20 text-xl font-bold', px: 80, dot: 'w-3.5 h-3.5' },
  };

  const getInitials = (n: string) => {
    const parts = n.trim().split(/\s+/);
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return n.slice(0, 2).toUpperCase();
  };

  const current = sizeMap[size];

  return (
    <div className={`relative inline-block shrink-0 select-none ${className}`}>
      <div
        className={`rounded-full overflow-hidden flex items-center justify-center bg-surface border border-border text-foreground font-semibold ${current.container}`}
      >
        {src ? (
          <Image
            src={src}
            alt={name}
            width={current.px}
            height={current.px}
            className="w-full h-full object-cover"
            unoptimized
          />
        ) : (
          <span>{getInitials(name)}</span>
        )}
      </div>
      {typeof isOnline === 'boolean' && (
        <span
          className={`absolute bottom-0 right-0 rounded-full border-2 border-card ${current.dot} ${
            isOnline ? 'bg-emerald-500' : 'bg-zinc-500'
          }`}
          title={isOnline ? 'Online' : 'Offline'}
        />
      )}
    </div>
  );
}

