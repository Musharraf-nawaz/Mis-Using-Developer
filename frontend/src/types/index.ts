export type Role = 'ADMIN' | 'USER';
export type AssetStatus = 'AVAILABLE' | 'ASSIGNED' | 'RETURNED' | 'DAMAGED' | 'LOST';
export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED';
export type InterviewMode = 'ONLINE' | 'OFFLINE';
export type InterviewRound =
  | 'SCREENING'
  | 'FIRST_ROUND'
  | 'SECOND_ROUND'
  | 'THIRD_ROUND'
  | 'HR'
  | 'FINAL_ROUND';
export type RoundStatus = 'SCHEDULED' | 'PASSED' | 'FAILED' | 'CANCELLED';
export type FinalInterviewStatus = 'SELECTED' | 'REJECTED' | 'ON_HOLD';
export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ON_HOLD' | 'CANCELLED';
export type MediaType = 'PHOTO' | 'VIDEO';
export type UserStatus = 'ACTIVE' | 'INACTIVE';

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  fullName: string;
  email: string;
  role: Role;
  department: string;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  department: string;
  phone: string;
  employeeId: string;
  status: UserStatus;
  createdAt: string;
}

export interface AssetMedia {
  id: number;
  mediaType: MediaType;
  fileUrl: string;
  fileName: string;
  uploadedAt: string;
}

export interface Asset {
  id: number;
  companyName: string;
  assetName: string;
  associatedDeveloper: string;
  projectName: string;
  assetCategory: string;
  assetType: string;
  serialNumber: string;
  assetTag: string;
  purchaseDate: string;
  purchaseCost: number;
  assignedToId: number;
  assignedToName: string;
  assignedDate: string;
  returnDate: string;
  projectOffboarded: boolean;
  status: AssetStatus;
  warrantyExpiryDate: string;
  vendorName: string;
  photoUrl: string;
  videoUrl: string;
  media: AssetMedia[];
  condition: string;
  remarks: string;
  createdAt: string;
  updatedAt: string;
}

export interface InterviewRoundData {
  id: number;
  roundNumber: number;
  interviewLink: string;
  interviewDate: string;
  interviewTime: string;
  companyToRepresent: string;
  interviewer: string;
  status: RoundStatus;
  available: boolean;
}

export interface Interview {
  id: number;
  candidateName: string;
  candidateEmail: string;
  candidatePhone: string;
  candidateProfile: string;
  clientName: string;
  midClientName: string;
  companyToRepresent: string;
  interviewLink: string;
  candidateCvUrl: string;
  finalStatus: FinalInterviewStatus;
  skills: string;
  experience: string;
  interviewerName: string;
  interviewerEmail: string;
  interviewDate: string;
  interviewTime: string;
  interviewMode: InterviewMode;
  interviewRound: InterviewRound;
  interviewStatus: InterviewStatus;
  feedback: string;
  notes: string;
  rounds: InterviewRoundData[];
  createdAt: string;
}

export interface Project {
  id: number;
  projectName: string;
  clientName: string;
  midClientName: string;
  candidateWorkingCount: number;
  interviewCandidateCount: number;
  onboardedCandidateCount: number;
  startDate: string;
  endDate: string;
  budget: number | null;
  status: ProjectStatus;
  remarks: string;
  assignedUserIds: number[];
}

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  entityType?: string;
  entityId?: number;
  read: boolean;
  createdAt: string;
}

export interface DashboardData {
  admin: boolean;
  assetStats: {
    totalAssets: number;
    availableAssets: number;
    assignedAssets: number;
    returnedAssets: number;
    damagedAssets: number;
  };
  interviewStats: {
    todayInterviews: number;
    upcomingInterviews: number;
    completedInterviews: number;
    cancelledInterviews: number;
    scheduledInterviews: number;
  };
  projectStats?: {
    totalProjects: number;
    activeProjects: number;
    totalBudget: number;
    workingCandidates: number;
    interviewCandidates: number;
    onboardedCandidates: number;
  };
  userStats?: {
    assignedProjects: number;
    workingCandidates: number;
    onboardedCandidates: number;
    assignedAssets: number;
    upcomingInterviews: number;
  };
  assignedProjects: {
    id: number;
    projectName: string;
    candidateWorkingCount: number;
    onboardedCandidateCount: number;
    status: ProjectStatus;
    remarks: string;
  }[];
  assignedAssets: {
    id: number;
    assetName: string;
    serialNumber: string;
    assignedDate: string;
    status: AssetStatus;
    photoUrl: string;
    videoUrl: string;
  }[];
  assetStatusDistribution: { status: string; count: number }[];
  monthlyInterviewStats: { month: string; status: string; count: number }[];
  assetAllocationTrends: { month: string; count: number }[];
  recentAssignments: ActivityItem[];
  recentReturns: ActivityItem[];
  upcomingInterviews: ActivityItem[];
}

export interface ActivityItem {
  id: number;
  title: string;
  description: string;
  type: string;
  timestamp: string;
  interviewDate?: string;
  interviewTime?: string;
  today?: boolean;
}
