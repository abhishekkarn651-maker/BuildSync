'use client';

import React, { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { status, isAuthenticated } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === 'unauthenticated') {
      const search = typeof window !== 'undefined' ? window.location.search : '';
      const fullPath = `${pathname}${search}`;
      const loginUrl = `/login?redirect=${encodeURIComponent(fullPath)}`;
      router.replace(loginUrl);
    }
  }, [status, router, pathname]);

  if (status === 'loading') {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-background text-foreground selection:bg-red-500/25 selection:text-red-400">
        <div className="relative mb-5 flex items-center justify-center">
          {/* Subtle Cyber Ember glow */}
          <div className="absolute inset-0 blur-xl bg-red-600/20 rounded-full pointer-events-none" />
          <div className="w-12 h-12 rounded-full border-2 border-border border-t-[#EF4444] animate-spin relative z-10" />
        </div>
        <div className="text-center space-y-1 relative z-10">
          <p className="text-xs font-semibold uppercase tracking-wider text-[#EF4444]">
            BuildSync Security
          </p>
          <p className="text-xs text-muted">
            Verifying authenticated session...
          </p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
