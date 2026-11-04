/**
 * BuildSync Developer Profile TypeScript Types
 * Mirrors the Spring Boot backend profile, skills, education and experience contracts
 */

export type ExperienceLevel = 'JUNIOR' | 'MID_LEVEL' | 'SENIOR' | 'LEAD';

export type AvailabilityStatus = 'AVAILABLE' | 'BUSY' | 'MENTORING' | 'NOT_AVAILABLE';

export type SkillCategory =
  | 'PROGRAMMING_LANGUAGE'
  | 'FRAMEWORK'
  | 'DATABASE'
  | 'DEVOPS'
  | 'TOOL'
  | 'DESIGN'
  | 'OTHER';

export type ProficiencyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';

export type EmploymentType =
  | 'FULL_TIME'
  | 'PART_TIME'
  | 'INTERNSHIP'
  | 'CONTRACT'
  | 'FREELANCE'
  | 'SELF_EMPLOYED';

export interface Skill {
  id: string;
  name: string;
  category: SkillCategory;
}

export interface ProfileSkill {
  id: string;
  skillId: string;
  name: string;
  category: SkillCategory;
  proficiencyLevel: ProficiencyLevel;
  createdAt: string;
  updatedAt: string;
}

export interface AddSkillPayload {
  skillId: string;
  proficiencyLevel: ProficiencyLevel;
}

export interface UpdateSkillProficiencyPayload {
  proficiencyLevel: ProficiencyLevel;
}

export interface Education {
  id: string;
  institution: string;
  degree?: string | null;
  fieldOfStudy?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  currentlyStudying: boolean;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EducationPayload {
  institution: string;
  degree?: string;
  fieldOfStudy?: string;
  startDate?: string | null;
  endDate?: string | null;
  currentlyStudying?: boolean;
  description?: string;
}

export interface WorkExperience {
  id: string;
  jobTitle: string;
  companyName: string;
  employmentType?: EmploymentType | null;
  location?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  currentlyWorking: boolean;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExperiencePayload {
  jobTitle: string;
  companyName: string;
  employmentType?: EmploymentType;
  location?: string;
  startDate?: string | null;
  endDate?: string | null;
  currentlyWorking?: boolean;
  description?: string;
}

export interface DeveloperProfile {
  id: string;
  userId: string;
  username: string;
  fullName: string;
  email: string;
  headline?: string | null;
  bio?: string | null;
  about?: string | null;
  location?: string | null;
  websiteUrl?: string | null;
  githubUrl?: string | null;
  linkedinUrl?: string | null;
  twitterUrl?: string | null;
  avatarUrl?: string | null;
  experienceLevel?: ExperienceLevel | null;
  yearsOfExperience?: number | null;
  availabilityStatus: AvailabilityStatus;
  skills?: ProfileSkill[];
  education?: Education[];
  experience?: WorkExperience[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProfilePayload {
  headline?: string;
  bio?: string;
  about?: string;
  location?: string;
  websiteUrl?: string;
  githubUrl?: string;
  linkedinUrl?: string;
  twitterUrl?: string;
  experienceLevel?: ExperienceLevel;
  yearsOfExperience?: number;
  availabilityStatus?: AvailabilityStatus;
}

export interface UpdateProfilePayload {
  headline?: string;
  bio?: string;
  about?: string;
  location?: string;
  websiteUrl?: string;
  githubUrl?: string;
  linkedinUrl?: string;
  twitterUrl?: string;
  experienceLevel?: ExperienceLevel;
  yearsOfExperience?: number;
  availabilityStatus?: AvailabilityStatus;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface DeveloperSummary {
  id: string;
  userId: string;
  username: string;
  fullName: string;
  headline?: string | null;
  bio?: string | null;
  location?: string | null;
  avatarUrl?: string | null;
  experienceLevel?: ExperienceLevel | null;
  yearsOfExperience?: number | null;
  availabilityStatus: AvailabilityStatus;
  skills?: ProfileSkill[];
  createdAt: string;
  updatedAt: string;
}

export type DeveloperSortOption =
  | 'recently_updated'
  | 'newest'
  | 'name_asc'
  | 'name_desc';

export interface DeveloperSearchQuery {
  keyword?: string;
  skill?: string;
  experienceLevel?: ExperienceLevel;
  availability?: AvailabilityStatus;
  location?: string;
  page?: number;
  size?: number;
  sort?: DeveloperSortOption;
}


