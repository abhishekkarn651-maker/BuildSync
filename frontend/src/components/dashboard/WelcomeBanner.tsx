import React from 'react';

interface WelcomeBannerProps {
  name?: string;
  subtitle?: string;
}

export function WelcomeBanner({
  name = 'Bharat',
  subtitle = 'Discover projects, find teammates, and build something amazing together.',
}: WelcomeBannerProps) {
  return (
    <div className="relative overflow-hidden rounded-2xl bg-card border border-border p-6 sm:p-8 flex items-center justify-between">
      {/* Background subtle ember glow effect */}
      <div className="absolute -right-16 -top-16 w-56 h-56 rounded-full bg-red-600/10 blur-3xl pointer-events-none" />

      {/* Left content */}
      <div className="relative z-10 max-w-xl space-y-1.5">
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground flex items-center gap-2.5">
          <span>Welcome back, {name}!</span>
          <span className="inline-block animate-wave origin-bottom-right" role="img" aria-label="Waving hand">
            👋
          </span>
        </h1>
        <p className="text-xs sm:text-sm text-muted leading-relaxed">
          {subtitle}
        </p>
      </div>

      {/* Right stylized glowing ember-red code emblem matching the mockup */}
      <div className="hidden sm:flex items-center justify-center shrink-0 pr-4 select-none">
        <div className="relative">
          <div className="absolute inset-0 blur-xl bg-red-500/25 rounded-full" />
          <span className="relative font-mono text-5xl sm:text-6xl font-black text-[#EF4444] opacity-90 tracking-tighter drop-shadow-[0_0_20px_rgba(239,68,68,0.35)]">
            &lt;/&gt;
          </span>
        </div>
      </div>
    </div>
  );
}

