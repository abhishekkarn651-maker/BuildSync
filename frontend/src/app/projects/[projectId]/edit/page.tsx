'use client';

import React, { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  ArrowLeft,
  FolderKanban,
  Check,
  X,
  Search,
  Globe,
  Calendar,
  AlertCircle,
  Layers,
  ShieldAlert,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { GithubIcon } from '@/components/ui/Icons';
import { projectService } from '@/services/project.service';
import { skillsService } from '@/services/skills.service';
import { Skill } from '@/types/profile.types';
import {
  CATEGORY_LABELS,
  Project,
  ProjectCategory,
  ProjectStatus,
  ProjectVisibility,
  STATUS_LABELS,
} from '@/types/project.types';
import { useAuth } from '@/context/AuthContext';

export default function EditProjectPage() {
  const params = useParams();
  const router = useRouter();
  const { user, isLoading: isAuthLoading } = useAuth();
  const projectId = params?.projectId as string;

  // Project loading state
  const [project, setProject] = useState<Project | null>(null);
  const [isLoadingProject, setIsLoadingProject] = useState(true);

  // Form states
  const [title, setTitle] = useState('');
  const [shortDescription, setShortDescription] = useState('');
  const [detailedDescription, setDetailedDescription] = useState('');
  const [category, setCategory] = useState<ProjectCategory>('FULL_STACK');
  const [status, setStatus] = useState<ProjectStatus>('IN_PROGRESS');
  const [visibility, setVisibility] = useState<ProjectVisibility>('PUBLIC');
  const [githubUrl, setGithubUrl] = useState('');
  const [demoUrl, setDemoUrl] = useState('');
  const [startDate, setStartDate] = useState('');
  const [completionDate, setCompletionDate] = useState('');

  // Skills states
  const [availableSkills, setAvailableSkills] = useState<Skill[]>([]);
  const [selectedSkills, setSelectedSkills] = useState<Skill[]>([]);
  const [skillSearchQuery, setSkillSearchQuery] = useState('');

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Load project details and available skills
  useEffect(() => {
    let isMounted = true;

    async function loadData() {
      setIsLoadingProject(true);
      try {
        const [projectData, catalogue] = await Promise.all([
          projectService.getProject(projectId),
          skillsService.searchCatalogue(),
        ]);

        if (isMounted) {
          setAvailableSkills(catalogue || []);
          if (projectData) {
            setProject(projectData);
            setTitle(projectData.title);
            setShortDescription(projectData.shortDescription);
            setDetailedDescription(projectData.detailedDescription || '');
            setCategory(projectData.category);
            setStatus(projectData.status);
            setVisibility(projectData.visibility);
            setGithubUrl(projectData.githubUrl || '');
            setDemoUrl(projectData.demoUrl || '');
            setStartDate(projectData.startDate || '');
            setCompletionDate(projectData.completionDate || '');

            // Pre-populate skills
            const initialSkills: Skill[] = (projectData.technologies || []).map((t) => ({
              id: t.id,
              name: t.name,
              category: t.category,
            }));
            setSelectedSkills(initialSkills);
          }
        }
      } catch (err) {
        console.error('Failed to load project for editing:', err);
      } finally {
        if (isMounted) setIsLoadingProject(false);
      }
    }

    if (projectId) {
      loadData();
    }

    return () => {
      isMounted = false;
    };
  }, [projectId]);

  const filteredSkills = availableSkills.filter(
    (skill) =>
      skill.name.toLowerCase().includes(skillSearchQuery.toLowerCase()) &&
      !selectedSkills.some((s) => s.id === skill.id)
  );

  const handleAddSkill = (skill: Skill) => {
    if (!selectedSkills.some((s) => s.id === skill.id)) {
      setSelectedSkills([...selectedSkills, skill]);
      setSkillSearchQuery('');
    }
  };

  const handleRemoveSkill = (skillId: string) => {
    setSelectedSkills(selectedSkills.filter((s) => s.id !== skillId));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (title.trim().length < 3) {
      setErrorMessage('Project title must be at least 3 characters long.');
      return;
    }
    if (shortDescription.trim().length < 5) {
      setErrorMessage('Short description must be at least 5 characters long.');
      return;
    }
    if (startDate && completionDate && completionDate < startDate) {
      setErrorMessage('Completion date cannot be before start date.');
      return;
    }

    setIsSubmitting(true);

    try {
      const updated = await projectService.updateProject(project?.id || projectId, {
        title: title.trim(),
        shortDescription: shortDescription.trim(),
        detailedDescription: detailedDescription.trim() || undefined,
        category,
        status,
        visibility,
        githubUrl: githubUrl.trim() || undefined,
        demoUrl: demoUrl.trim() || undefined,
        startDate: startDate || undefined,
        completionDate: completionDate || undefined,
        technologyIds: selectedSkills.map((s) => s.id),
      });

      router.push(`/projects/${updated.id}`);
    } catch (err: unknown) {
      console.error('Failed to update project:', err);
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to update project. Please verify all fields.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingProject || isAuthLoading) {
    return (
      <AppLayout>
        <div className="max-w-4xl mx-auto space-y-6 animate-pulse">
          <div className="h-6 w-32 bg-surface rounded" />
          <div className="h-10 w-64 bg-surface rounded" />
          <div className="h-80 bg-card rounded-2xl border border-border" />
        </div>
      </AppLayout>
    );
  }

  if (!project) {
    return (
      <AppLayout>
        <div className="max-w-xl mx-auto my-12 p-8 rounded-2xl bg-card border border-border text-center space-y-4">
          <FolderKanban className="w-12 h-12 text-[#EF4444] mx-auto" />
          <h2 className="text-xl font-bold text-foreground">Project Not Found</h2>
          <p className="text-sm text-muted">
            The requested project does not exist or you do not have permission to access it.
          </p>
          <div className="pt-2">
            <Link href="/projects">
              <Button variant="outline" size="md">
                Return to Projects
              </Button>
            </Link>
          </div>
        </div>
      </AppLayout>
    );
  }

  const isOwner = user?.id === project.owner.id;

  if (!isOwner) {
    return (
      <AppLayout>
        <div className="max-w-xl mx-auto my-12 p-8 rounded-2xl bg-card border border-border text-center space-y-4">
          <ShieldAlert className="w-12 h-12 text-[#EF4444] mx-auto" />
          <h2 className="text-xl font-bold text-foreground">Permission Denied</h2>
          <p className="text-sm text-muted">
            Only the owner of this project can modify its details and settings.
          </p>
          <div className="pt-2">
            <Link href={`/projects/${project.id}`}>
              <Button variant="outline" size="md">
                View Project Details
              </Button>
            </Link>
          </div>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className="max-w-4xl mx-auto space-y-6 pb-12">
        {/* Navigation Breadcrumb */}
        <div>
          <Link
            href={`/projects/${project.id}`}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Project</span>
          </Link>
        </div>

        {/* Page Header */}
        <div className="border-b border-border pb-5">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Edit Project: {project.title}
          </h1>
          <p className="text-xs sm:text-sm text-muted mt-1">
            Update your project goals, roadmap, technical stack, and repository links.
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-xs text-red-500">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Info */}
          <div className="p-6 rounded-2xl bg-card border border-border space-y-5 shadow-sm">
            <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
              <FolderKanban className="w-4 h-4 text-[#EF4444]" />
              Basic Information
            </h2>

            <div>
              <label htmlFor="title" className="block text-xs font-medium text-foreground mb-1.5">
                Project Title <span className="text-[#EF4444]">*</span>
              </label>
              <Input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={150}
                required
              />
              <div className="flex justify-between text-[11px] text-muted mt-1">
                <span>Unique, descriptive title for your project.</span>
                <span>{title.length}/150</span>
              </div>
            </div>

            <div>
              <label htmlFor="shortDescription" className="block text-xs font-medium text-foreground mb-1.5">
                Short Description <span className="text-[#EF4444]">*</span>
              </label>
              <textarea
                id="shortDescription"
                rows={2}
                value={shortDescription}
                onChange={(e) => setShortDescription(e.target.value)}
                maxLength={300}
                required
                className="w-full rounded-lg bg-surface border border-border p-3 text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/30 transition-colors"
              />
              <div className="flex justify-between text-[11px] text-muted mt-1">
                <span>Brief overview displayed on search cards.</span>
                <span>{shortDescription.length}/300</span>
              </div>
            </div>

            <div>
              <label htmlFor="detailedDescription" className="block text-xs font-medium text-foreground mb-1.5">
                Detailed Description
              </label>
              <textarea
                id="detailedDescription"
                rows={5}
                value={detailedDescription}
                onChange={(e) => setDetailedDescription(e.target.value)}
                maxLength={5000}
                className="w-full rounded-lg bg-surface border border-border p-3 text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/30 transition-colors font-mono text-[12px]"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              <div>
                <label htmlFor="category" className="block text-xs font-medium text-foreground mb-1.5">
                  Category <span className="text-[#EF4444]">*</span>
                </label>
                <select
                  id="category"
                  value={category}
                  onChange={(e) => setCategory(e.target.value as ProjectCategory)}
                  className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] transition-colors"
                >
                  {(Object.keys(CATEGORY_LABELS) as ProjectCategory[]).map((cat) => (
                    <option key={cat} value={cat}>
                      {CATEGORY_LABELS[cat]}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="status" className="block text-xs font-medium text-foreground mb-1.5">
                  Status <span className="text-[#EF4444]">*</span>
                </label>
                <select
                  id="status"
                  value={status}
                  onChange={(e) => setStatus(e.target.value as ProjectStatus)}
                  className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] transition-colors"
                >
                  {(Object.keys(STATUS_LABELS) as ProjectStatus[]).map((st) => (
                    <option key={st} value={st}>
                      {STATUS_LABELS[st]}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="visibility" className="block text-xs font-medium text-foreground mb-1.5">
                  Visibility <span className="text-[#EF4444]">*</span>
                </label>
                <select
                  id="visibility"
                  value={visibility}
                  onChange={(e) => setVisibility(e.target.value as ProjectVisibility)}
                  className="w-full h-10 rounded-lg bg-surface border border-border px-3 text-xs text-foreground focus:outline-none focus:border-[#EF4444] transition-colors"
                >
                  <option value="PUBLIC">Public (Discoverable)</option>
                  <option value="PRIVATE">Private (Owner Only)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Technology Stack */}
          <div className="p-6 rounded-2xl bg-card border border-border space-y-4 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
                <Layers className="w-4 h-4 text-[#EF4444]" />
                Technology Stack
              </h2>
              <span className="text-xs text-muted">
                {selectedSkills.length} selected
              </span>
            </div>

            <div className="flex flex-wrap gap-2 min-h-[36px] p-2.5 rounded-lg bg-surface border border-border">
              {selectedSkills.length === 0 ? (
                <span className="text-xs text-muted italic">
                  No technologies selected. Add some below.
                </span>
              ) : (
                selectedSkills.map((skill) => (
                  <span
                    key={skill.id}
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-red-500/10 text-red-500 border border-red-500/30 text-xs font-medium"
                  >
                    <span>{skill.name}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveSkill(skill.id)}
                      className="p-0.5 hover:text-red-400 rounded-full cursor-pointer"
                      title="Remove technology"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))
              )}
            </div>

            <div className="space-y-2">
              <Input
                placeholder="Search technologies to add..."
                value={skillSearchQuery}
                onChange={(e) => setSkillSearchQuery(e.target.value)}
                leftIcon={<Search className="w-3.5 h-3.5 text-muted" />}
              />
              <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto p-1 text-xs">
                {filteredSkills.slice(0, 16).map((skill) => (
                  <button
                    key={skill.id}
                    type="button"
                    onClick={() => handleAddSkill(skill)}
                    className="px-2.5 py-1 rounded-md bg-surface text-muted hover:text-foreground hover:bg-surface-hover border border-border transition-colors cursor-pointer text-[11px]"
                  >
                    + {skill.name}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Links & Schedule */}
          <div className="p-6 rounded-2xl bg-card border border-border space-y-5 shadow-sm">
            <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
              <Globe className="w-4 h-4 text-[#EF4444]" />
              Links & Timeline
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="githubUrl" className="block text-xs font-medium text-foreground mb-1.5">
                  GitHub Repository URL
                </label>
                <Input
                  id="githubUrl"
                  value={githubUrl}
                  onChange={(e) => setGithubUrl(e.target.value)}
                  leftIcon={<GithubIcon className="w-4 h-4 text-muted" />}
                />
              </div>

              <div>
                <label htmlFor="demoUrl" className="block text-xs font-medium text-foreground mb-1.5">
                  Live Demo URL
                </label>
                <Input
                  id="demoUrl"
                  value={demoUrl}
                  onChange={(e) => setDemoUrl(e.target.value)}
                  leftIcon={<Globe className="w-4 h-4 text-muted" />}
                />
              </div>

              <div>
                <label htmlFor="startDate" className="block text-xs font-medium text-foreground mb-1.5">
                  Start Date
                </label>
                <Input
                  id="startDate"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  leftIcon={<Calendar className="w-4 h-4 text-muted" />}
                />
              </div>

              <div>
                <label htmlFor="completionDate" className="block text-xs font-medium text-foreground mb-1.5">
                  Target Completion Date
                </label>
                <Input
                  id="completionDate"
                  type="date"
                  value={completionDate}
                  onChange={(e) => setCompletionDate(e.target.value)}
                  leftIcon={<Calendar className="w-4 h-4 text-muted" />}
                />
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <Link href={`/projects/${project.id}`}>
              <Button type="button" variant="outline" size="md">
                Cancel
              </Button>
            </Link>
            <Button
              type="submit"
              variant="primary"
              size="md"
              disabled={isSubmitting}
              leftIcon={<Check className="w-4 h-4" />}
            >
              {isSubmitting ? 'Saving Changes...' : 'Save Changes'}
            </Button>
          </div>
        </form>
      </div>
    </AppLayout>
  );
}
