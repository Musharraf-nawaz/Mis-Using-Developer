export type Role = 'ADMIN' | 'HR' | 'EMPLOYEE';
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
  condition: string;
  remarks: string;
  createdAt: string;
  updatedAt: string;
}

export interface Interview {
  id: number;
  candidateName: string;
  candidateEmail: string;
  candidatePhone: string;
  candidateProfile: string;
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
  createdAt: string;
}

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface DashboardData {
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
  };
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
