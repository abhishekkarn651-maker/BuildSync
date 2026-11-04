import React from 'react';
import Link from 'next/link';
import { ArrowRight, MoreHorizontal, Code2, GitFork } from 'lucide-react';
import { MOCK_PROJECTS } from '@/lib/mock-data';

export function RecentProjects() {
  const recentList = MOCK_PROJECTS.slice(0, 2);

  return (
    <div className="space-y-4">
      {/* Header with View All -> */}
      <div className="flex items-center justify-between">
        <h2 className="text-base sm:text-lg font-bold text-foreground tracking-tight">
          Recent Projects
        </h2>
        <Link
          href="/projects"
          className="inline-flex items-center gap-1 text-xs font-semibold text-[#EF4444] hover:text-red-400 transition-colors"
        >
          <span>View All</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Projects Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {recentList.map((project) => {
          const isDevCollab = project.id === 'proj-1';

          return (
            <Link
              key={project.id}
              href={`/projects/${project.id}`}
              className="flex flex-col justify-between p-5 rounded-xl bg-card border border-border hover:border-red-500/40 transition-all duration-200 group"
            >
              <div>
                {/* Top row: Icon + Title + Status + More options */}
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 border ${
                        isDevCollab
                          ? 'bg-red-500/10 border-red-500/20 text-[#EF4444]'
                          : 'bg-surface border-border text-foreground'
                      }`}
                    >
                      {isDevCollab ? (
                        <Code2 className="w-5 h-5 animate-pulse" />
                      ) : (
                        <GitFork className="w-5 h-5" />
                      )}
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-foreground group-hover:text-[#EF4444] transition-colors leading-snug">
                        {project.title}
                      </h3>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span
                      className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${
                        project.status === 'In Progress'
                          ? 'bg-red-500/10 text-red-400 border-red-500/30'
                          : 'bg-surface text-muted border-border'
                      }`}
                    >
                      {project.status}
                    </span>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.preventDefault();
                      }}
                      className="text-muted hover:text-foreground p-1 rounded transition-colors"
                      aria-label="Project options"
                    >
                      <MoreHorizontal className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Description */}
                <p className="text-xs text-muted leading-relaxed line-clamp-2 mb-4">
                  {project.description}
                </p>
              </div>

              {/* Technologies pills */}
              <div className="flex flex-wrap items-center gap-1.5 pt-2 border-t border-border/60">
                {project.technologies.map((tech) => (
                  <span
                    key={tech}
                    className="text-[11px] font-medium px-2 py-0.5 rounded-md bg-surface text-muted-foreground border border-border"
                  >
                    {tech}
                  </span>
                ))}
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}

