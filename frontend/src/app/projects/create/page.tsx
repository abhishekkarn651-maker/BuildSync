'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
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
  ProjectCategory,
  ProjectStatus,
  ProjectVisibility,
  STATUS_LABELS,
} from '@/types/project.types';
import { useAuth } from '@/context/AuthContext';

export default function CreateProjectPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Form states
  const [title, setTitle] = useState('');
  const [shortDescription, setShortDescription] = useState('');
  const [detailedDescription, setDetailedDescription] = useState('');
  const [category, setCategory] = useState<ProjectCategory>('FULL_STACK');
  const [status, setStatus] = useState<ProjectStatus>('PLANNING');
  const [visibility, setVisibility] = useState<ProjectVisibility>('PUBLIC');
  const [githubUrl, setGithubUrl] = useState('');
  const [demoUrl, setDemoUrl] = useState('');
  const [startDate, setStartDate] = useState('');
  const [completionDate, setCompletionDate] = useState('');

  // Skills & technologies
  const [availableSkills, setAvailableSkills] = useState<Skill[]>([]);
  const [selectedSkills, setSelectedSkills] = useState<Skill[]>([]);
  const [skillSearchQuery, setSkillSearchQuery] = useState('');

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Load available skills catalogue
  useEffect(() => {
    let isMounted = true;
    async function loadSkills() {
      try {
        const catalogue = await skillsService.searchCatalogue();
        if (isMounted) {
          setAvailableSkills(catalogue || []);
        }
      } catch (err) {
        console.error('Failed to load master skills:', err);
      }
    }
    loadSkills();
    return () => {
      isMounted = false;
    };
  }, []);

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

    // Basic client validations
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
      const created = await projectService.createProject({
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

      router.push(`/projects/${created.id}`);
    } catch (err: unknown) {
      console.error('Failed to create project:', err);
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to create project. Please verify all fields.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isAuthLoading && !isAuthenticated) {
    return (
      <AppLayout>
        <div className="max-w-xl mx-auto my-12 p-8 rounded-2xl bg-card border border-border text-center space-y-4">
          <FolderKanban className="w-12 h-12 text-[#EF4444] mx-auto" />
          <h2 className="text-xl font-bold text-foreground">Authentication Required</h2>
          <p className="text-sm text-muted">
            You must be signed in to create and manage software projects on BuildSync.
          </p>
          <div className="pt-2">
            <Link href="/login">
              <Button variant="primary" size="md">
                Sign In to Continue
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
            href="/projects"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Projects</span>
          </Link>
        </div>

        {/* Page Header */}
        <div className="border-b border-border pb-5">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Create New Project
          </h1>
          <p className="text-xs sm:text-sm text-muted mt-1">
            Showcase your open source tools, full-stack applications, and collaborate with skilled developers.
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-xs text-red-500">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Project Form */}
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Card: Basic Project Information */}
          <div className="p-6 rounded-2xl bg-card border border-border space-y-5 shadow-sm">
            <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
              <FolderKanban className="w-4 h-4 text-[#EF4444]" />
              Basic Information
            </h2>

            {/* Title */}
            <div>
              <label htmlFor="title" className="block text-xs font-medium text-foreground mb-1.5">
                Project Title <span className="text-[#EF4444]">*</span>
              </label>
              <Input
                id="title"
                placeholder="e.g., BuildSync Platform, CloudScale Engine"
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

            {/* Short Description */}
            <div>
              <label htmlFor="shortDescription" className="block text-xs font-medium text-foreground mb-1.5">
                Short Description <span className="text-[#EF4444]">*</span>
              </label>
              <textarea
                id="shortDescription"
                rows={2}
                placeholder="Brief summary displayed on project discovery cards and search results..."
                value={shortDescription}
                onChange={(e) => setShortDescription(e.target.value)}
                maxLength={300}
                required
                className="w-full rounded-lg bg-surface border border-border p-3 text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/30 transition-colors"
              />
              <div className="flex justify-between text-[11px] text-muted mt-1">
                <span>Recommended: 50-200 characters summarizing the project goal.</span>
                <span>{shortDescription.length}/300</span>
              </div>
            </div>

            {/* Detailed Description */}
            <div>
              <label htmlFor="detailedDescription" className="block text-xs font-medium text-foreground mb-1.5">
                Detailed Description (Optional)
              </label>
              <textarea
                id="detailedDescription"
                rows={5}
                placeholder="Detailed architecture overview, problem statement, core components, and roadmap..."
                value={detailedDescription}
                onChange={(e) => setDetailedDescription(e.target.value)}
                maxLength={5000}
                className="w-full rounded-lg bg-surface border border-border p-3 text-xs text-foreground placeholder:text-muted focus:outline-none focus:border-[#EF4444] focus:ring-1 focus:ring-[#EF4444]/30 transition-colors font-mono text-[12px]"
              />
              <div className="flex justify-between text-[11px] text-muted mt-1">
                <span>Deep dive into the project&apos;s engineering, architecture, and design.</span>
                <span>{detailedDescription.length}/5000</span>
              </div>
            </div>

            {/* Category, Status & Visibility Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              {/* Category */}
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

              {/* Status */}
              <div>
                <label htmlFor="status" className="block text-xs font-medium text-foreground mb-1.5">
                  Status
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

              {/* Visibility */}
              <div>
                <label htmlFor="visibility" className="block text-xs font-medium text-foreground mb-1.5">
                  Visibility
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

          {/* Card: Technology Stack */}
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
            <p className="text-xs text-muted">
              Select languages, frameworks, databases, and infrastructure tools from the verified catalogue.
            </p>

            {/* Selected Skills Pills */}
            <div className="flex flex-wrap gap-2 min-h-[36px] p-2.5 rounded-lg bg-surface border border-border">
              {selectedSkills.length === 0 ? (
                <span className="text-xs text-muted italic">
                  No technologies selected yet. Click from the suggestions or search below.
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

            {/* Search and Suggestion Pills */}
            <div className="space-y-2">
              <div className="relative">
                <Input
                  placeholder="Search and add technologies (e.g., React, Java, Docker)..."
                  value={skillSearchQuery}
                  onChange={(e) => setSkillSearchQuery(e.target.value)}
                  leftIcon={<Search className="w-3.5 h-3.5 text-muted" />}
                />
              </div>

              {/* Suggestions / Available Chips */}
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

          {/* Card: External Links & Schedule */}
          <div className="p-6 rounded-2xl bg-card border border-border space-y-5 shadow-sm">
            <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
              <Globe className="w-4 h-4 text-[#EF4444]" />
              Links & Timeline
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* GitHub Repository */}
              <div>
                <label htmlFor="githubUrl" className="block text-xs font-medium text-foreground mb-1.5">
                  GitHub Repository URL
                </label>
                <Input
                  id="githubUrl"
                  placeholder="https://github.com/username/project"
                  value={githubUrl}
                  onChange={(e) => setGithubUrl(e.target.value)}
                  leftIcon={<GithubIcon className="w-4 h-4 text-muted" />}
                />
              </div>

              {/* Live Demo URL */}
              <div>
                <label htmlFor="demoUrl" className="block text-xs font-medium text-foreground mb-1.5">
                  Live Demo / Deployment URL
                </label>
                <Input
                  id="demoUrl"
                  placeholder="https://myproject.com"
                  value={demoUrl}
                  onChange={(e) => setDemoUrl(e.target.value)}
                  leftIcon={<Globe className="w-4 h-4 text-muted" />}
                />
              </div>

              {/* Start Date */}
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

              {/* Target Completion Date */}
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

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <Link href="/projects">
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
              {isSubmitting ? 'Creating Project...' : 'Publish Project'}
            </Button>
          </div>
        </form>
      </div>
    </AppLayout>
  );
}
