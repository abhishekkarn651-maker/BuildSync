import { SkillCategory } from './profile.types';

export type ProjectStatus =
  | 'IDEA'
  | 'PLANNING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'ON_HOLD'
  | 'ARCHIVED';

export type ProjectVisibility = 'PUBLIC' | 'PRIVATE';

export type ProjectCategory =
  | 'FULL_STACK'
  | 'FRONTEND'
  | 'BACKEND'
  | 'MOBILE'
  | 'AI_ML'
  | 'DEVOPS'
  | 'BLOCKCHAIN'
  | 'CYBERSECURITY'
  | 'OPEN_SOURCE'
  | 'OTHER';

export interface ProjectTechnology {
  id: string;
  name: string;
  category: SkillCategory;
}

export interface ProjectOwner {
  id: string;
  username: string;
  fullName: string;
  avatarUrl?: string | null;
  headline?: string | null;
}

export interface Project {
  id: string;
  slug: string;
  title: string;
  shortDescription: string;
  detailedDescription?: string | null;
  category: ProjectCategory;
  status: ProjectStatus;
  visibility: ProjectVisibility;
  githubUrl?: string | null;
  demoUrl?: string | null;
  startDate?: string | null;
  completionDate?: string | null;
  owner: ProjectOwner;
  technologies: ProjectTechnology[];
  createdAt: string;
  updatedAt: string;
}

export interface ProjectSummary {
  id: string;
  slug: string;
  title: string;
  shortDescription: string;
  category: ProjectCategory;
  status: ProjectStatus;
  visibility: ProjectVisibility;
  githubUrl?: string | null;
  demoUrl?: string | null;
  startDate?: string | null;
  completionDate?: string | null;
  owner: ProjectOwner;
  technologies: ProjectTechnology[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectPayload {
  title: string;
  shortDescription: string;
  detailedDescription?: string;
  category: ProjectCategory;
  status?: ProjectStatus;
  visibility?: ProjectVisibility;
  technologyIds?: string[];
  githubUrl?: string;
  demoUrl?: string;
  startDate?: string | null;
  completionDate?: string | null;
}

export interface UpdateProjectPayload {
  title: string;
  shortDescription: string;
  detailedDescription?: string;
  category: ProjectCategory;
  status: ProjectStatus;
  visibility: ProjectVisibility;
  technologyIds?: string[];
  githubUrl?: string;
  demoUrl?: string;
  startDate?: string | null;
  completionDate?: string | null;
}

export type ProjectSortOption =
  | 'recently_updated'
  | 'newest'
  | 'title_asc'
  | 'title_desc';

export interface ProjectSearchQuery {
  keyword?: string;
  category?: ProjectCategory;
  status?: ProjectStatus;
  technology?: string;
  page?: number;
  size?: number;
  sort?: ProjectSortOption;
}

export const CATEGORY_LABELS: Record<ProjectCategory, string> = {
  FULL_STACK: 'Full-Stack',
  FRONTEND: 'Frontend',
  BACKEND: 'Backend',
  MOBILE: 'Mobile',
  AI_ML: 'AI & ML',
  DEVOPS: 'DevOps & Cloud',
  BLOCKCHAIN: 'Web3 & Blockchain',
  CYBERSECURITY: 'Cybersecurity',
  OPEN_SOURCE: 'Open Source',
  OTHER: 'Other',
};

export const STATUS_LABELS: Record<ProjectStatus, string> = {
  IDEA: 'Idea',
  PLANNING: 'Planning',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  ON_HOLD: 'On Hold',
  ARCHIVED: 'Archived',
};

