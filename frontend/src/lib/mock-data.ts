/**
 * BuildSync Isolated Mock & Prototyping Data
 * Kept completely isolated from actual backend API services.
 */

export interface Developer {
  id: string;
  name: string;
  username: string;
  avatar: string;
  role: string;
  bio: string;
  location: string;
  skills: string[];
  experienceLevel: 'Junior' | 'Mid-Level' | 'Senior' | 'Lead';
  yearsExperience: number;
  availability: 'Available' | 'Busy' | 'Mentoring';
  githubUrl: string;
  portfolioUrl?: string;
  projectsCount: number;
  connectionsCount: number;
  aboutText: string;
}

export interface Project {
  id: string;
  title: string;
  slug: string;
  description: string;
  detailedDescription: string;
  category: 'Full-Stack' | 'AI & ML' | 'DevOps' | 'Mobile' | 'Web3';
  status: 'In Progress' | 'Planning' | 'Completed';
  technologies: string[];
  creator: {
    name: string;
    username: string;
    avatar: string;
    role: string;
  };
  teamMembers: Array<{
    name: string;
    username: string;
    avatar: string;
    role: string;
  }>;
  githubUrl?: string;
  demoUrl?: string;
  createdAt: string;
  updatedAt: string;
  openRoles: string[];
}

export interface NotificationItem {
  id: string;
  type: 'collaboration' | 'project' | 'message' | 'system';
  title: string;
  description: string;
  timeAgo: string;
  read: boolean;
  avatarUrl?: string;
  link?: string;
}

export interface ChatMessage {
  id: string;
  senderId: string;
  senderName: string;
  senderAvatar: string;
  content: string;
  timestamp: string;
  isCurrentUser: boolean;
}

export interface Conversation {
  id: string;
  recipientId: string;
  recipientName: string;
  recipientUsername: string;
  recipientAvatar: string;
  recipientRole: string;
  isOnline: boolean;
  unreadCount: number;
  lastMessage: string;
  lastMessageTime: string;
  messages: ChatMessage[];
}

export interface CollaborationRequest {
  id: string;
  type: 'received' | 'sent';
  developer: {
    name: string;
    username: string;
    avatar: string;
    role: string;
    skills: string[];
  };
  project: {
    id: string;
    title: string;
    roleNeeded: string;
  };
  message: string;
  timestamp: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
}

export const CURRENT_USER: Developer = {
  id: 'user-bharat-kumar',
  name: 'Bharat Kumar',
  username: 'bharatkumar',
  avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
  role: 'Senior Full-Stack Architect',
  bio: 'Building scalable distributed systems with Next.js, Spring Boot & PostgreSQL. Passionate about developer tooling.',
  location: 'Bengaluru, India',
  skills: ['React', 'Next.js', 'TypeScript', 'Java', 'Spring Boot', 'PostgreSQL', 'Docker', 'Kubernetes'],
  experienceLevel: 'Lead',
  yearsExperience: 7,
  availability: 'Available',
  githubUrl: 'https://github.com/bharatkumar-dev',
  portfolioUrl: 'https://bharatkumar.io',
  projectsCount: 12,
  connectionsCount: 1248,
  aboutText:
    'Senior engineer specializing in high-throughput microservices and performant user interfaces. Leading technical architecture for enterprise collaboration products and mentoring junior developers in full-stack ecosystems.',
};

export const MOCK_DEVELOPERS: Developer[] = [
  CURRENT_USER,
  {
    id: 'dev-1',
    name: 'Priya Sharma',
    username: 'priyasharma',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
    role: 'Frontend Engineer & UI Specialist',
    bio: 'Crafting pixel-perfect design systems with React, Tailwind CSS, and Framer Motion.',
    location: 'Mumbai, India',
    skills: ['React', 'Next.js', 'Tailwind', 'TypeScript', 'Figma', 'UI/UX'],
    experienceLevel: 'Senior',
    yearsExperience: 5,
    availability: 'Available',
    githubUrl: 'https://github.com/priyasharma',
    portfolioUrl: 'https://priya.design',
    projectsCount: 8,
    connectionsCount: 640,
    aboutText:
      'Passionate design technologist bridging the divide between Figma designs and accessible production React code.',
  },
  {
    id: 'dev-2',
    name: 'Rohit Verma',
    username: 'rohitverma',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    role: 'Backend & Cloud Systems Engineer',
    bio: 'High-concurrency systems, Spring Cloud, Kafka, and PostgreSQL optimization specialist.',
    location: 'Hyderabad, India',
    skills: ['Java', 'Spring Boot', 'Kafka', 'PostgreSQL', 'Redis', 'AWS'],
    experienceLevel: 'Senior',
    yearsExperience: 6,
    availability: 'Available',
    githubUrl: 'https://github.com/rohitverma',
    projectsCount: 14,
    connectionsCount: 920,
    aboutText:
      'Focused on distributed backend architectures, transaction consistency, and database indexing strategies for enterprise platforms.',
  },
  {
    id: 'dev-3',
    name: 'Alex Chen',
    username: 'alexchen',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
    role: 'Machine Learning & AI Engineer',
    bio: 'Fine-tuning LLMs, building semantic search engines, and deploying inference pipelines.',
    location: 'Singapore',
    skills: ['Python', 'PyTorch', 'FastAPI', 'LangChain', 'Docker', 'PostgreSQL'],
    experienceLevel: 'Mid-Level',
    yearsExperience: 4,
    availability: 'Busy',
    githubUrl: 'https://github.com/alexchen-ai',
    portfolioUrl: 'https://alexchen.dev',
    projectsCount: 9,
    connectionsCount: 450,
    aboutText:
      'Applying transformer models to developer tooling and code recommendation engines. Seeking open-source collaborations.',
  },
  {
    id: 'dev-4',
    name: 'Sarah Jenkins',
    username: 'sarahj',
    avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80',
    role: 'DevOps & Site Reliability Engineer',
    bio: 'Terraform, Kubernetes, GitOps workflows and CI/CD pipeline automation enthusiast.',
    location: 'London, UK',
    skills: ['Kubernetes', 'Docker', 'Terraform', 'GitHub Actions', 'Go', 'Linux'],
    experienceLevel: 'Lead',
    yearsExperience: 8,
    availability: 'Mentoring',
    githubUrl: 'https://github.com/sarahjenkins',
    projectsCount: 18,
    connectionsCount: 1530,
    aboutText:
      'Dedicated to zero-downtime deployments, observable telemetry with OpenTelemetry/Prometheus, and robust infrastructure as code.',
  },
  {
    id: 'dev-5',
    name: 'Arjun Nair',
    username: 'arjunnair',
    avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop&q=80',
    role: 'Mobile & Cross-Platform Developer',
    bio: 'Flutter & React Native architect creating smooth, responsive mobile experiences.',
    location: 'Bengaluru, India',
    skills: ['Flutter', 'React Native', 'TypeScript', 'Dart', 'GraphQL', 'Firebase'],
    experienceLevel: 'Mid-Level',
    yearsExperience: 3,
    availability: 'Available',
    githubUrl: 'https://github.com/arjunnair',
    projectsCount: 6,
    connectionsCount: 310,
    aboutText:
      'Building performant, accessible mobile applications with unified design languages for iOS and Android platforms.',
  },
];

export const MOCK_PROJECTS: Project[] = [
  {
    id: 'proj-1',
    title: 'DevCollab Platform',
    slug: 'devcollab-platform',
    description: 'A developer collaboration platform to find teammates and sync code activities.',
    detailedDescription:
      'DevCollab is an open-source platform uniting software engineers worldwide. Features intelligent matchmaking algorithms based on programming languages and frameworks, direct real-time project rooms, and automated GitHub status synchronizations.',
    category: 'Full-Stack',
    status: 'In Progress',
    technologies: ['React', 'Spring Boot', 'PostgreSQL'],
    creator: {
      name: 'Bharat Kumar',
      username: 'bharatkumar',
      avatar: CURRENT_USER.avatar,
      role: 'Project Owner',
    },
    teamMembers: [
      { name: 'Bharat Kumar', username: 'bharatkumar', avatar: CURRENT_USER.avatar, role: 'Lead Architect' },
      { name: 'Priya Sharma', username: 'priyasharma', avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80', role: 'Frontend Lead' },
    ],
    githubUrl: 'https://github.com/BuildSync/devcollab',
    demoUrl: 'https://devcollab.buildsync.dev',
    createdAt: '2026-10-15',
    updatedAt: '2026-11-01',
    openRoles: ['Backend Java Engineer', 'PostgreSQL DBA'],
  },
  {
    id: 'proj-2',
    title: 'Open Source Hub',
    slug: 'open-source-hub',
    description: 'A curated platform for open source projects and contributors to connect seamlessly.',
    detailedDescription:
      'Aggregates first-time contributor-friendly issues across major open source repositories. Provides automated mentorship matching and contribution telemetry for maintainers.',
    category: 'Full-Stack',
    status: 'Planning',
    technologies: ['Next.js', 'TypeScript', 'Tailwind'],
    creator: {
      name: 'Priya Sharma',
      username: 'priyasharma',
      avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
      role: 'Project Lead',
    },
    teamMembers: [
      { name: 'Priya Sharma', username: 'priyasharma', avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80', role: 'Lead Designer' },
    ],
    githubUrl: 'https://github.com/priyasharma/opensource-hub',
    createdAt: '2026-10-20',
    updatedAt: '2026-10-31',
    openRoles: ['Frontend Engineer', 'Next.js Specialist'],
  },
  {
    id: 'proj-3',
    title: 'CloudMesh Telemetry',
    slug: 'cloudmesh-telemetry',
    description: 'Real-time distributed tracing and observability daemon for Kubernetes clusters.',
    detailedDescription:
      'Lightweight eBPF based network observability sensor. Monitors latency spikes, dropped packets, and service mesh routing across multi-cloud clusters with minimal CPU overhead.',
    category: 'DevOps',
    status: 'In Progress',
    technologies: ['Go', 'Kubernetes', 'Docker', 'PostgreSQL'],
    creator: {
      name: 'Sarah Jenkins',
      username: 'sarahj',
      avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80',
      role: 'Project Lead',
    },
    teamMembers: [
      { name: 'Sarah Jenkins', username: 'sarahj', avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80', role: 'SRE Lead' },
      { name: 'Rohit Verma', username: 'rohitverma', avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80', role: 'Backend Core' },
    ],
    githubUrl: 'https://github.com/sarahjenkins/cloudmesh',
    demoUrl: 'https://cloudmesh.io',
    createdAt: '2026-09-12',
    updatedAt: '2026-10-28',
    openRoles: ['Kubernetes Operator Dev'],
  },
  {
    id: 'proj-4',
    title: 'Synthetix AI Copilot',
    slug: 'synthetix-ai-copilot',
    description: 'Autonomous documentation and code refactoring agent tailored for microservices.',
    detailedDescription:
      'Leverages local LLM embeddings and AST analysis to generate architectural decision records (ADRs) directly from git commits, ensuring codebase documentation never goes stale.',
    category: 'AI & ML',
    status: 'Completed',
    technologies: ['Python', 'FastAPI', 'PyTorch', 'Next.js'],
    creator: {
      name: 'Alex Chen',
      username: 'alexchen',
      avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
      role: 'Creator',
    },
    teamMembers: [
      { name: 'Alex Chen', username: 'alexchen', avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80', role: 'AI Architect' },
    ],
    githubUrl: 'https://github.com/alexchen-ai/synthetix',
    demoUrl: 'https://synthetix.dev',
    createdAt: '2026-08-01',
    updatedAt: '2026-10-14',
    openRoles: [],
  },
];

export const MOCK_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 'notif-1',
    type: 'collaboration',
    title: 'New collaboration request',
    description: 'Priya sent you a request.',
    timeAgo: '5m ago',
    read: false,
    link: '/collaborations',
  },
  {
    id: 'notif-2',
    type: 'project',
    title: 'Project update',
    description: 'DevCollab Platform has a new update.',
    timeAgo: '28m ago',
    read: false,
    link: '/projects/proj-1',
  },
  {
    id: 'notif-3',
    type: 'message',
    title: 'New message',
    description: 'Rohit: Are you available to join?',
    timeAgo: '1h ago',
    read: false,
    link: '/messages',
  },
  {
    id: 'notif-4',
    type: 'system',
    title: 'Flyway Migration Baseline',
    description: 'Database schema v1 baseline validated successfully.',
    timeAgo: '3h ago',
    read: true,
  },
];

export const MOCK_CONVERSATIONS: Conversation[] = [
  {
    id: 'conv-1',
    recipientId: 'dev-1',
    recipientName: 'Priya Sharma',
    recipientUsername: 'priyasharma',
    recipientAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
    recipientRole: 'Frontend Lead',
    isOnline: true,
    unreadCount: 2,
    lastMessage: 'I reviewed the Figma design tokens for the Cyber Ember Red palette!',
    lastMessageTime: '10:42 AM',
    messages: [
      {
        id: 'msg-1',
        senderId: 'dev-1',
        senderName: 'Priya Sharma',
        senderAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
        content: 'Hey Bharat! Did you get a chance to check the new Cyber Ember theme specifications?',
        timestamp: '10:30 AM',
        isCurrentUser: false,
      },
      {
        id: 'msg-2',
        senderId: 'user-bharat-kumar',
        senderName: 'Bharat Kumar',
        senderAvatar: CURRENT_USER.avatar,
        content: 'Yes! The red accents and charcoal surfaces look extremely sharp and professional.',
        timestamp: '10:36 AM',
        isCurrentUser: true,
      },
      {
        id: 'msg-3',
        senderId: 'dev-1',
        senderName: 'Priya Sharma',
        senderAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
        content: 'I reviewed the Figma design tokens for the Cyber Ember Red palette! Ready whenever you want to merge.',
        timestamp: '10:42 AM',
        isCurrentUser: false,
      },
    ],
  },
  {
    id: 'conv-2',
    recipientId: 'dev-2',
    recipientName: 'Rohit Verma',
    recipientUsername: 'rohitverma',
    recipientAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    recipientRole: 'Backend Engineer',
    isOnline: false,
    unreadCount: 1,
    lastMessage: 'Are you available to join?',
    lastMessageTime: '1h ago',
    messages: [
      {
        id: 'msg-201',
        senderId: 'dev-2',
        senderName: 'Rohit Verma',
        senderAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
        content: 'Hey Bharat, we have a sprint sync for the PostgreSQL schema migrations today.',
        timestamp: '9:15 AM',
        isCurrentUser: false,
      },
      {
        id: 'msg-202',
        senderId: 'dev-2',
        senderName: 'Rohit Verma',
        senderAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
        content: 'Are you available to join?',
        timestamp: '9:18 AM',
        isCurrentUser: false,
      },
    ],
  },
  {
    id: 'conv-3',
    recipientId: 'dev-3',
    recipientName: 'Alex Chen',
    recipientUsername: 'alexchen',
    recipientAvatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
    recipientRole: 'AI Engineer',
    isOnline: true,
    unreadCount: 0,
    lastMessage: 'Sounds great. Let us connect tomorrow.',
    lastMessageTime: 'Yesterday',
    messages: [
      {
        id: 'msg-301',
        senderId: 'user-bharat-kumar',
        senderName: 'Bharat Kumar',
        senderAvatar: CURRENT_USER.avatar,
        content: 'Hey Alex, really liked your Synthetix AI Copilot repository!',
        timestamp: 'Yesterday',
        isCurrentUser: true,
      },
      {
        id: 'msg-302',
        senderId: 'dev-3',
        senderName: 'Alex Chen',
        senderAvatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
        content: 'Thanks Bharat! Appreciate the feedback. Sounds great. Let us connect tomorrow.',
        timestamp: 'Yesterday',
        isCurrentUser: false,
      },
    ],
  },
];

export const MOCK_COLLABORATIONS: CollaborationRequest[] = [
  {
    id: 'collab-1',
    type: 'received',
    developer: {
      name: 'Priya Sharma',
      username: 'priyasharma',
      avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
      role: 'Frontend Engineer',
      skills: ['React', 'Next.js', 'Tailwind', 'TypeScript'],
    },
    project: {
      id: 'proj-1',
      title: 'DevCollab Platform',
      roleNeeded: 'Frontend Design System Engineer',
    },
    message:
      'Hi Bharat! I saw your DevCollab platform architecture and would love to collaborate on standardizing the design system and UI components.',
    timestamp: '5 minutes ago',
    status: 'PENDING',
  },
  {
    id: 'collab-2',
    type: 'received',
    developer: {
      name: 'Sarah Jenkins',
      username: 'sarahj',
      avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80',
      role: 'DevOps / SRE Lead',
      skills: ['Kubernetes', 'Docker', 'Terraform', 'CI/CD'],
    },
    project: {
      id: 'proj-1',
      title: 'DevCollab Platform',
      roleNeeded: 'Infrastructure Architect',
    },
    message:
      'I can help set up the Kubernetes deployment manifests and automated GitHub Actions testing workflows for your Spring Boot service.',
    timestamp: '2 hours ago',
    status: 'PENDING',
  },
  {
    id: 'collab-3',
    type: 'sent',
    developer: {
      name: 'Alex Chen',
      username: 'alexchen',
      avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
      role: 'Machine Learning Engineer',
      skills: ['Python', 'FastAPI', 'PyTorch'],
    },
    project: {
      id: 'proj-4',
      title: 'Synthetix AI Copilot',
      roleNeeded: 'Full-Stack Integration',
    },
    message:
      'Hi Alex, I would love to contribute to the Next.js frontend and Spring Boot API wrapper for Synthetix AI.',
    timestamp: '1 day ago',
    status: 'ACCEPTED',
  },
  {
    id: 'collab-4',
    type: 'received',
    developer: {
      name: 'Arjun Nair',
      username: 'arjunnair',
      avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop&q=80',
      role: 'Mobile Developer',
      skills: ['Flutter', 'React Native'],
    },
    project: {
      id: 'proj-1',
      title: 'DevCollab Platform',
      roleNeeded: 'Mobile Lead',
    },
    message: 'Can I build the Flutter companion mobile app for DevCollab?',
    timestamp: '3 days ago',
    status: 'ACCEPTED',
  },
];

