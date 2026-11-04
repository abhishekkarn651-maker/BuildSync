import React from 'react';
import Link from 'next/link';

interface BrandLogoProps {
  size?: 'sm' | 'md' | 'lg';
  href?: string;
  showTagline?: boolean;
  className?: string;
}

export function BrandLogo({
  size = 'md',
  href = '/dashboard',
  showTagline = false,
  className = '',
}: BrandLogoProps) {
  const iconSizeClass =
    size === 'sm'
      ? 'text-lg font-bold'
      : size === 'lg'
      ? 'text-2xl font-black'
      : 'text-xl font-extrabold';

  const textSizeClass =
    size === 'sm'
      ? 'text-base font-semibold'
      : size === 'lg'
      ? 'text-2xl font-bold'
      : 'text-lg font-bold';

  const content = (
    <div className={`inline-flex items-center gap-2 select-none group ${className}`}>
      {/* Code bracket symbol in primary red */}
      <span
        className={`font-mono text-[#EF4444] tracking-tighter ${iconSizeClass} transition-transform duration-200 group-hover:scale-110`}
        aria-hidden="true"
      >
        &lt;/&gt;
      </span>
      <div className="flex flex-col">
        <span className={`tracking-tight text-foreground transition-colors ${textSizeClass}`}>
          BuildSync
        </span>
        {showTagline && (
          <span className="text-[11px] text-muted -mt-0.5 font-normal">
            Bringing Developers and Projects Into Sync.
          </span>
        )}
      </div>
    </div>
  );

  if (href) {
    return (
      <Link href={href} className="inline-flex items-center focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500 rounded-lg">
        {content}
      </Link>
    );
  }

  return content;
}

