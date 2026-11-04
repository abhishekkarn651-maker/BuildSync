'use client';

import React, { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  User,
  MapPin,
  Briefcase,
  Calendar,
  Globe,
  UserPlus,
  Send,
  Check,
  CheckCircle2,
  ArrowLeft,
  Clock,
  Edit3,
  GraduationCap,
  Code2,
  MessageSquare,
  Star,
  GitFork,
  ExternalLink,
} from 'lucide-react';
import { AppLayout } from '@/components/layout/AppLayout';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Tabs } from '@/components/ui/Tabs';
import { Modal } from '@/components/ui/Modal';
import { EmptyState } from '@/components/ui/EmptyState';
import { GithubIcon } from '@/components/ui/Icons';
import { SkillsSection } from '@/components/profile/SkillsSection';
import { EducationSection } from '@/components/profile/EducationSection';
import { ExperienceSection } from '@/components/profile/ExperienceSection';
import { MOCK_DEVELOPERS, MOCK_PROJECTS } from '@/lib/mock-data';
import { profileService } from '@/services/profile.service';
import { projectService } from '@/services/project.service';
import { collaborationService } from '@/services/collaboration.service';
import { githubService } from '@/services/github.service';
import { DeveloperProfile } from '@/types/profile.types';
import { ProjectSummary } from '@/types/project.types';
import { GitHubRepositoryResponse } from '@/types/github.types';
import { getAvatarUrl } from '@/config/api.config';
import { useAuth } from '@/context/AuthContext';

export default function DeveloperProfilePage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const username = params?.username as string;

  const [realProfile, setRealProfile] = useState<DeveloperProfile | null>(null);
  const [showcasedRepos, setShowcasedRepos] = useState<GitHubRepositoryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [activeTab, setActiveTab] = useState('about');
  const [isConnected, setIsConnected] = useState(false);
  const [isCollabModalOpen, setIsCollabModalOpen] = useState(false);
  const [myProjects, setMyProjects] = useState<ProjectSummary[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('');
  const [collabMessage, setCollabMessage] = useState('');
  const [isCollabSent, setIsCollabSent] = useState(false);
  const [isCollabSubmitting, setIsCollabSubmitting] = useState(false);
  const [collabError, setCollabError] = useState<string | null>(null);
  const [isLoadingProjects, setIsLoadingProjects] = useState(false);

  // Fetch real profile from backend
  useEffect(() => {
    let isMounted = true;

    async function loadProfile() {
      if (!username) return;
      try {
        const [profileData, githubRepos] = await Promise.allSettled([
          profileService.getPublicProfile(username),
          githubService.getPublicShowcase(username),
        ]);

        if (isMounted && profileData.status === 'fulfilled') {
          setRealProfile(profileData.value);
        }
        if (isMounted && githubRepos.status === 'fulfilled') {
          setShowcasedRepos(githubRepos.value);
        }
      } catch {
        // Fallback or not found handled gracefully
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    loadProfile();

    return () => {
      isMounted = false;
    };
  }, [username]);

  // Check mock developers as fallback if not in backend database
  const mockDeveloper = MOCK_DEVELOPERS.find(
    (d) => d.username.toLowerCase() === username?.toLowerCase()
  );

  const isCurrentUser =
    user?.username && username && user.username.toLowerCase() === username.toLowerCase();

  const handleOpenCollabModal = async () => {
    if (!user) {
      router.push('/login');
      return;
    }
    setIsCollabModalOpen(true);
    setIsCollabSent(false);
    setCollabError(null);
    setIsLoadingProjects(true);
    try {
      const projects = await projectService.getMyProjects();
      setMyProjects(projects);
      if (projects.length > 0) {
        setSelectedProjectId(projects[0].id);
      }
    } catch (err: unknown) {
      console.error('Failed to load owned projects for invitation:', err);
      setCollabError('Failed to load your projects. Please try again.');
    } finally {
      setIsLoadingProjects(false);
    }
  };

  const handleSendCollab = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProjectId) {
      setCollabError('Please select a project to invite this developer to.');
      return;
    }
    setIsCollabSubmitting(true);
    setCollabError(null);
    try {
      await collaborationService.inviteDeveloper(selectedProjectId, {
        inviteeUsername: username,
        message: collabMessage.trim() || undefined,
      });
      setIsCollabSent(true);
      setTimeout(() => {
        setIsCollabSent(false);
        setIsCollabModalOpen(false);
        setCollabMessage('');
      }, 2000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to send collaboration invitation';
      setCollabError(msg);
    } finally {
      setIsCollabSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <AppLayout>
        <div className="min-h-[400px] flex flex-col items-center justify-center">
          <div className="w-8 h-8 rounded-full border-2 border-border border-t-[#EF4444] animate-spin mb-3" />
          <p className="text-xs text-muted">Loading developer profile...</p>
        </div>
      </AppLayout>
    );
  }

  // If neither real profile nor mock exists
  if (!realProfile && !mockDeveloper) {
    return (
      <AppLayout>
        <div className="max-w-2xl mx-auto py-12">
          <EmptyState
            icon={<User className="w-6 h-6 text-[#EF4444]" />}
            title="Developer Not Found"
            description={`No developer profile was found matching username "@${username}".`}
            actionLabel="Back to Discovery"
            onAction={() => router.push('/developers')}
          />
        </div>
      </AppLayout>
    );
  }

  // Map real profile or mock profile data
  const name = realProfile?.fullName || mockDeveloper?.name || username;
  const headline = realProfile?.headline || mockDeveloper?.role || 'Developer';
  const bio = realProfile?.bio || mockDeveloper?.bio || '';
  const about = realProfile?.about || mockDeveloper?.aboutText || bio;
  const location = realProfile?.location || mockDeveloper?.location || 'Remote';
  const experienceLevel = realProfile?.experienceLevel || mockDeveloper?.experienceLevel || 'Mid-Level';
  const yearsExperience = realProfile?.yearsOfExperience ?? mockDeveloper?.yearsExperience ?? 3;
  const githubUrl = realProfile?.githubUrl || mockDeveloper?.githubUrl;
  const websiteUrl = realProfile?.websiteUrl || mockDeveloper?.portfolioUrl;
  const availability = realProfile?.availabilityStatus || mockDeveloper?.availability || 'AVAILABLE';

  const developerProjects = MOCK_PROJECTS.filter(
    (p) =>
      p.creator.username.toLowerCase() === username?.toLowerCase() ||
      p.teamMembers.some((m) => m.username.toLowerCase() === username?.toLowerCase())
  );

  const tabsConfig = [
    { id: 'about', label: 'About & Skills', icon: <Code2 className="w-3.5 h-3.5" /> },
    {
      id: 'experience',
      label: `Work Experience (${realProfile?.experience?.length || 0})`,
      icon: <Briefcase className="w-3.5 h-3.5" />,
    },
    {
      id: 'education',
      label: `Education (${realProfile?.education?.length || 0})`,
      icon: <GraduationCap className="w-3.5 h-3.5" />,
    },
    {
      id: 'projects',
      label: `Projects (${developerProjects.length || 0})`,
    },
    {
      id: 'github',
      label: `GitHub (${showcasedRepos.length})`,
      icon: <GithubIcon className="w-3.5 h-3.5" />,
    },
  ];

  return (
    <AppLayout>
      <div className="space-y-6 max-w-5xl mx-auto pb-12">
        {/* Back Link & Edit profile shortcut */}
        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => router.back()}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted hover:text-foreground transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back</span>
          </button>

          {isCurrentUser && (
            <Link href="/profile/edit">
              <Button variant="outline" size="sm" leftIcon={<Edit3 className="w-3.5 h-3.5" />}>
                Edit My Profile
              </Button>
            </Link>
          )}
        </div>

        {/* Profile Header Card */}
        <div className="rounded-2xl bg-card border border-border p-6 sm:p-8 space-y-6 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-80 h-80 bg-red-600/10 blur-[100px] pointer-events-none rounded-full" />

          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-6 relative z-10">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 text-center sm:text-left">
              <Avatar
                name={name}
                src={getAvatarUrl(realProfile?.avatarUrl)}
                size="xl"
                className="ring-4 ring-card text-2xl font-bold"
              />
              <div className="space-y-2">
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                  <h1 className="text-2xl font-bold tracking-tight text-foreground">
                    {name}
                  </h1>
                  <span className="text-xs font-semibold text-muted font-mono">
                    @{username}
                  </span>
                  <Badge variant="primary" size="sm">
                    {experienceLevel}
                  </Badge>
                </div>

                <div className="text-xs sm:text-sm font-medium text-foreground flex items-center justify-center sm:justify-start gap-1.5">
                  <Briefcase className="w-4 h-4 text-[#EF4444]" />
                  <span>{headline}</span>
                </div>

                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 text-xs text-muted pt-1">
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-muted" />
                    {location}
                  </span>
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-muted" />
                    {yearsExperience} years experience
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 text-emerald-500" />
                    <span className="capitalize">{availability.toLowerCase()}</span>
                  </span>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-wrap items-center justify-center sm:justify-end gap-2.5 shrink-0">
              {githubUrl && (
                <a
                  href={githubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="p-2.5 rounded-lg border border-border bg-surface text-muted hover:text-foreground hover:bg-surface-hover transition-colors"
                  title="View GitHub"
                >
                  <GithubIcon className="w-4 h-4" />
                </a>
              )}
              {websiteUrl && (
                <a
                  href={websiteUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="p-2.5 rounded-lg border border-border bg-surface text-muted hover:text-foreground hover:bg-surface-hover transition-colors"
                  title="View Portfolio"
                >
                  <Globe className="w-4 h-4" />
                </a>
              )}

              {!isCurrentUser && (
                <>
                  <Button
                    variant="outline"
                    size="md"
                    className="text-xs"
                    leftIcon={<MessageSquare className="w-3.5 h-3.5 text-[#EF4444]" />}
                    onClick={() => {
                      if (!user) {
                        router.push('/login');
                        return;
                      }
                      router.push(`/messages?user=${encodeURIComponent(username)}`);
                    }}
                  >
                    Message
                  </Button>

                  <Button
                    variant={isConnected ? 'secondary' : 'outline'}
                    size="md"
                    className="text-xs"
                    leftIcon={
                      isConnected ? (
                        <Check className="w-3.5 h-3.5 text-emerald-500" />
                      ) : (
                        <UserPlus className="w-3.5 h-3.5" />
                      )
                    }
                    onClick={() => setIsConnected(!isConnected)}
                  >
                    {isConnected ? 'Connected' : 'Connect'}
                  </Button>

                  <Button
                    variant="primary"
                    size="md"
                    className="text-xs"
                    leftIcon={<Send className="w-3.5 h-3.5" />}
                    onClick={handleOpenCollabModal}
                  >
                    Request Collaboration
                  </Button>
                </>
              )}
            </div>
          </div>

          {/* Quick Metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-4 border-t border-border/80">
            <div className="p-3 rounded-xl bg-surface border border-border/50 text-center">
              <span className="block text-lg font-bold text-foreground">
                {developerProjects.length || 0}
              </span>
              <span className="text-[11px] text-muted">Projects</span>
            </div>
            <div className="p-3 rounded-xl bg-surface border border-border/50 text-center">
              <span className="block text-lg font-bold text-foreground">
                {yearsExperience}
              </span>
              <span className="text-[11px] text-muted">Years Exp.</span>
            </div>
            <div className="p-3 rounded-xl bg-surface border border-border/50 text-center">
              <span className="block text-lg font-bold text-foreground">
                {realProfile?.skills?.length ?? (mockDeveloper?.skills?.length || 0)}
              </span>
              <span className="text-[11px] text-muted">Skills</span>
            </div>
            <div className="p-3 rounded-xl bg-surface border border-border/50 text-center">
              <span className="block text-lg font-bold text-foreground">
                {realProfile?.experience?.length || 0}
              </span>
              <span className="text-[11px] text-muted">Work Records</span>
            </div>
          </div>
        </div>

        {/* Tab Navigation */}
        <Tabs tabs={tabsConfig} activeTab={activeTab} onChange={setActiveTab} />

        {/* Tab Content: About & Skills */}
        {activeTab === 'about' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            <div className="lg:col-span-8 space-y-6">
              {bio && (
                <div className="rounded-2xl bg-card border border-border p-6 space-y-3">
                  <h2 className="text-sm font-bold text-foreground tracking-tight">
                    Bio Summary
                  </h2>
                  <p className="text-xs sm:text-sm text-muted leading-relaxed font-normal">
                    {bio}
                  </p>
                </div>
              )}

              {about && (
                <div className="rounded-2xl bg-card border border-border p-6 space-y-3">
                  <h2 className="text-sm font-bold text-foreground tracking-tight">
                    Detailed Background
                  </h2>
                  <div className="text-xs sm:text-sm text-foreground/80 leading-relaxed whitespace-pre-wrap font-sans">
                    {about}
                  </div>
                </div>
              )}

              {/* Real Skills Section */}
              <div className="rounded-2xl bg-card border border-border p-6">
                <SkillsSection
                  skills={realProfile?.skills || []}
                  editable={false}
                />
              </div>
            </div>

            <div className="lg:col-span-4 space-y-6">
              <div className="rounded-2xl bg-card border border-border p-5 space-y-3">
                <h2 className="text-xs font-bold uppercase tracking-wider text-muted">
                  Technical Details
                </h2>
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between py-1 border-b border-border/50">
                    <span className="text-muted">Status</span>
                    <Badge variant="success" size="sm">{availability}</Badge>
                  </div>
                  <div className="flex justify-between py-1 border-b border-border/50">
                    <span className="text-muted">Experience</span>
                    <span className="font-medium text-foreground">{experienceLevel}</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-border/50">
                    <span className="text-muted">Location</span>
                    <span className="font-medium text-foreground">{location}</span>
                  </div>
                  {githubUrl && (
                    <div className="flex justify-between py-1">
                      <span className="text-muted">GitHub</span>
                      <a href={githubUrl} target="_blank" rel="noreferrer" className="text-[#EF4444] hover:underline">
                        View Profile
                      </a>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab Content: Experience */}
        {activeTab === 'experience' && (
          <div className="rounded-2xl bg-card border border-border p-6">
            <ExperienceSection
              experience={realProfile?.experience || []}
              editable={false}
            />
          </div>
        )}

        {/* Tab Content: Education */}
        {activeTab === 'education' && (
          <div className="rounded-2xl bg-card border border-border p-6">
            <EducationSection
              education={realProfile?.education || []}
              editable={false}
            />
          </div>
        )}

        {/* Tab Content: Projects */}
        {activeTab === 'projects' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {developerProjects.length > 0 ? (
              developerProjects.map((project) => (
                <div
                  key={project.id}
                  className="rounded-2xl bg-card border border-border p-5 space-y-4 hover:border-red-500/40 transition-colors flex flex-col justify-between"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-surface text-muted border border-border">
                        {project.category}
                      </span>
                      <Badge
                        variant={project.status === 'Completed' ? 'success' : 'primary'}
                        size="sm"
                      >
                        {project.status}
                      </Badge>
                    </div>
                    <Link
                      href={`/projects/${project.slug}`}
                      className="text-base font-bold text-foreground hover:text-[#EF4444] transition-colors line-clamp-1"
                    >
                      {project.title}
                    </Link>
                    <p className="text-xs text-muted line-clamp-2">
                      {project.description}
                    </p>
                  </div>

                  <div className="flex flex-wrap gap-1.5 pt-2 border-t border-border">
                    {project.technologies.map((tech) => (
                      <span
                        key={tech}
                        className="text-[10px] font-medium px-2 py-0.5 rounded bg-surface text-muted border border-border/60"
                      >
                        {tech}
                      </span>
                    ))}
                  </div>
                </div>
              ))
            ) : (
              <div className="col-span-full py-8 text-center text-xs text-muted">
                No public projects associated with this developer yet.
              </div>
            )}
          </div>
        )}

        {/* Tab Content: Showcased GitHub Repositories */}
        {activeTab === 'github' && (
          <div className="space-y-4">
            {showcasedRepos.length === 0 ? (
              <div className="p-12 rounded-2xl bg-card border border-border text-center space-y-3">
                <GithubIcon className="w-10 h-10 text-muted mx-auto" />
                <h4 className="text-sm font-semibold text-foreground">No showcased repositories</h4>
                <p className="text-xs text-muted max-w-sm mx-auto">
                  This developer has not selected any public GitHub repositories to showcase yet.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {showcasedRepos.map((repo) => (
                  <div
                    key={repo.id}
                    className="p-5 rounded-2xl bg-card border border-border hover:border-border/80 transition-all flex flex-col justify-between space-y-3"
                  >
                    <div className="space-y-2">
                      <div className="flex items-start justify-between gap-3">
                        <a
                          href={repo.htmlUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="font-bold text-sm text-foreground hover:text-[#EF4444] transition-colors inline-flex items-center gap-1.5 group flex-1 break-all"
                        >
                          <GithubIcon className="w-4 h-4 text-muted group-hover:text-[#EF4444] shrink-0" />
                          <span>{repo.name}</span>
                          <ExternalLink className="w-3 h-3 text-muted opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
                        </a>
                        <Badge variant="success" size="sm" className="text-[10px]">
                          Public
                        </Badge>
                      </div>

                      <p className="text-xs text-muted line-clamp-2 min-h-[32px]">
                        {repo.description || 'No repository description.'}
                      </p>
                    </div>

                    <div className="pt-2 border-t border-border/60 flex items-center justify-between text-xs text-muted">
                      <div className="flex items-center gap-3">
                        {repo.primaryLanguage && (
                          <div className="flex items-center gap-1">
                            <span className="w-2 h-2 rounded-full bg-[#EF4444]" />
                            <span>{repo.primaryLanguage}</span>
                          </div>
                        )}
                        <div className="flex items-center gap-1">
                          <Star className="w-3.5 h-3.5 text-amber-400" />
                          <span>{repo.starsCount}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <GitFork className="w-3.5 h-3.5 text-muted" />
                          <span>{repo.forksCount}</span>
                        </div>
                      </div>

                      {repo.githubPushedAt && (
                        <span className="text-[11px] text-muted/70">
                          Updated {new Date(repo.githubPushedAt).toLocaleDateString()}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Collaboration Request Modal */}
        <Modal
          isOpen={isCollabModalOpen}
          onClose={() => setIsCollabModalOpen(false)}
          title={`Invite ${name} to Collaborate`}
        >
          {isCollabSent ? (
            <div className="py-8 text-center space-y-3">
              <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
              <h3 className="text-base font-bold text-foreground">
                Collaboration Invitation Sent!
              </h3>
              <p className="text-xs text-muted">
                @{username} has been invited and can review the invitation under their Collaborations dashboard.
              </p>
            </div>
          ) : isLoadingProjects ? (
            <div className="py-8 text-center space-y-3">
              <div className="w-6 h-6 rounded-full border-2 border-border border-t-[#EF4444] animate-spin mx-auto" />
              <p className="text-xs text-muted">Loading your projects...</p>
            </div>
          ) : myProjects.length === 0 ? (
            <div className="py-6 text-center space-y-4">
              <p className="text-xs text-muted">
                You do not have any projects yet. You must create a project before inviting collaborators.
              </p>
              <div className="flex justify-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsCollabModalOpen(false)}
                >
                  Close
                </Button>
                <Link href="/projects/create">
                  <Button variant="primary" size="sm">
                    Create Project
                  </Button>
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSendCollab} className="space-y-4">
              {collabError && (
                <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-xs text-red-400">
                  {collabError}
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-foreground mb-1.5">
                  Select Project <span className="text-red-500">*</span>
                </label>
                <select
                  value={selectedProjectId}
                  onChange={(e) => setSelectedProjectId(e.target.value)}
                  className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-xs text-foreground focus:outline-none focus:border-[#EF4444]"
                >
                  {myProjects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title} ({p.category})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-foreground mb-1.5">
                  Invitation Note (Optional)
                </label>
                <textarea
                  rows={4}
                  placeholder={`Hi ${name}, I'd love to invite you to collaborate on our project. Your technical skills would be a great fit...`}
                  value={collabMessage}
                  onChange={(e) => setCollabMessage(e.target.value)}
                  className="w-full rounded-lg bg-surface border border-border px-3.5 py-2 text-xs text-foreground placeholder:text-muted/60 focus:outline-none focus:border-[#EF4444] resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsCollabModalOpen(false)}
                  disabled={isCollabSubmitting}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  disabled={isCollabSubmitting}
                  leftIcon={<Send className="w-3.5 h-3.5" />}
                >
                  {isCollabSubmitting ? 'Sending...' : 'Send Invitation'}
                </Button>
              </div>
            </form>
          )}
        </Modal>
      </div>
    </AppLayout>
  );
}
