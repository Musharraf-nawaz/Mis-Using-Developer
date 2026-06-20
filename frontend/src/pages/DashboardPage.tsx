import { lazy, Suspense } from 'react';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import {
  Grid,
  Typography,
  Card,
  CardContent,
  Box,
  List,
  ListItem,
  ListItemText,
  Chip,
  Skeleton,
  Avatar,
  Button,
} from '@mui/material';
import {
  Inventory,
  CheckCircle,
  Assignment,
  Folder,
  People,
  Event,
  Schedule,
  AttachMoney,
  Work,
} from '@mui/icons-material';
import { dashboardApi, fileUrl } from '../api/services';
import { useAuth } from '../context/AuthContext';
import StatCard from '../components/common/StatCard';
import DashboardSkeleton from '../components/common/DashboardSkeleton';

const DashboardCharts = lazy(() => import('../components/dashboard/DashboardCharts'));

export default function DashboardPage() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole('ADMIN');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get(),
    placeholderData: keepPreviousData,
    retry: 2,
  });

  const dashboard = data?.data?.data;

  if (isLoading && !dashboard) {
    return <DashboardSkeleton />;
  }

  if (isError || !dashboard) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <Typography color="error" gutterBottom>
          Unable to load dashboard.
        </Typography>
        <Button variant="contained" onClick={() => refetch()}>
          Retry
        </Button>
      </Box>
    );
  }

  const { assetStats, interviewStats } = dashboard;

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        {isAdmin ? 'Admin Dashboard' : 'My Dashboard'}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {isAdmin
          ? 'Organization-wide insights across projects, assets, and interviews'
          : 'Your assigned projects, assets, and upcoming interviews'}
      </Typography>

      {isAdmin && dashboard.projectStats && (
        <>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1.5 }}>
            PROJECT METRICS
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Total Projects" value={dashboard.projectStats.totalProjects} icon={<Folder />} />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Active Projects" value={dashboard.projectStats.activeProjects} icon={<Work />} color="#2E7D32" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Total Budget"
                value={`$${Number(dashboard.projectStats.totalBudget || 0).toLocaleString()}`}
                icon={<AttachMoney />}
                color="#00838F"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard title="Working Candidates" value={dashboard.projectStats.workingCandidates} icon={<People />} />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard title="Interview Candidates" value={dashboard.projectStats.interviewCandidates} icon={<Event />} />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard title="Onboarded Candidates" value={dashboard.projectStats.onboardedCandidates} icon={<CheckCircle />} color="#2E7D32" />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <StatCard title="Scheduled Interviews" value={interviewStats.scheduledInterviews} icon={<Schedule />} color="#F57C00" />
            </Grid>
          </Grid>
        </>
      )}

      {!isAdmin && dashboard.userStats && (
        <>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1.5 }}>
            MY OVERVIEW
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Assigned Projects" value={dashboard.userStats.assignedProjects} icon={<Folder />} />
            </Grid>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Working Candidates" value={dashboard.userStats.workingCandidates} icon={<People />} />
            </Grid>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Onboarded" value={dashboard.userStats.onboardedCandidates} icon={<CheckCircle />} color="#2E7D32" />
            </Grid>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Assigned Assets" value={dashboard.userStats.assignedAssets} icon={<Assignment />} color="#F57C00" />
            </Grid>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Upcoming Interviews" value={dashboard.userStats.upcomingInterviews} icon={<Schedule />} color="#00838F" />
            </Grid>
          </Grid>
        </>
      )}

      {!isAdmin && dashboard.assignedProjects.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={700} gutterBottom>
              My Projects
            </Typography>
            <List dense>
              {dashboard.assignedProjects.map((p) => (
                <ListItem key={p.id} sx={{ flexDirection: 'column', alignItems: 'flex-start', py: 1 }}>
                  <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                    <Typography fontWeight={600}>{p.projectName}</Typography>
                    <Chip label={p.status} size="small" />
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Working: {p.candidateWorkingCount} · Onboarded: {p.onboardedCandidateCount}
                  </Typography>
                  {p.remarks && (
                    <Typography variant="caption" color="text.secondary">
                      Remarks: {p.remarks}
                    </Typography>
                  )}
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      )}

      {!isAdmin && dashboard.assignedAssets.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={700} gutterBottom>
              My Assigned Assets
            </Typography>
            <Grid container spacing={2}>
              {dashboard.assignedAssets.map((a) => (
                <Grid item xs={12} sm={6} md={4} key={a.id}>
                  <Card variant="outlined">
                    <CardContent>
                      {a.photoUrl && (
                        <Box
                          component="img"
                          src={fileUrl(a.photoUrl)}
                          alt={a.assetName}
                          loading="lazy"
                          decoding="async"
                          sx={{ width: '100%', height: 120, objectFit: 'cover', borderRadius: 1, mb: 1 }}
                        />
                      )}
                      <Typography fontWeight={600}>{a.assetName}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        S/N: {a.serialNumber || '—'} · Assigned: {a.assignedDate || '—'}
                      </Typography>
                      <Box sx={{ mt: 0.5 }}>
                        <Chip label={a.status} size="small" />
                      </Box>
                      {a.videoUrl && (
                        <Typography
                          component="a"
                          href={fileUrl(a.videoUrl)}
                          target="_blank"
                          rel="noopener noreferrer"
                          variant="caption"
                          color="primary"
                          sx={{ display: 'block', mt: 1 }}
                        >
                          View asset video
                        </Typography>
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}

      <Card sx={{ mb: 3, border: '1px solid', borderColor: 'divider' }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} gutterBottom>
            Upcoming Interviews
          </Typography>
          {dashboard.upcomingInterviews.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No upcoming interviews.
            </Typography>
          ) : (
            <List dense>
              {dashboard.upcomingInterviews.map((item) => (
                <ListItem
                  key={item.id}
                  disablePadding
                  sx={{
                    py: 0.8,
                    px: 1,
                    mb: 0.5,
                    borderRadius: 1,
                    bgcolor: item.today ? 'warning.light' : 'transparent',
                    border: item.today ? '1px solid' : 'none',
                    borderColor: item.today ? 'warning.main' : 'transparent',
                  }}
                >
                  <ListItemText
                    primary={item.title}
                    secondary={`${item.description} | ${item.interviewDate ?? ''} ${item.interviewTime ?? ''}`.trim()}
                  />
                  {item.today && <Chip label="Today" color="warning" size="small" />}
                </ListItem>
              ))}
            </List>
          )}
        </CardContent>
      </Card>

      {isAdmin && (
        <>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1.5 }}>
            ASSET METRICS
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Total Assets" value={assetStats.totalAssets} icon={<Inventory />} />
            </Grid>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Available" value={assetStats.availableAssets} icon={<CheckCircle />} color="#2E7D32" />
            </Grid>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Assigned" value={assetStats.assignedAssets} icon={<Assignment />} color="#F57C00" />
            </Grid>
            <Grid item xs={12} sm={6} md={2.4}>
              <StatCard title="Damaged" value={assetStats.damagedAssets} icon={<Avatar sx={{ bgcolor: 'error.main', width: 32, height: 32, fontSize: 14 }}>!</Avatar>} color="#C62828" />
            </Grid>
          </Grid>
        </>
      )}

      <Suspense
        fallback={
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}><Skeleton variant="rounded" height={300} /></Grid>
            <Grid item xs={12} md={8}><Skeleton variant="rounded" height={300} /></Grid>
          </Grid>
        }
      >
        <DashboardCharts
          assetStatusDistribution={dashboard.assetStatusDistribution}
          assetAllocationTrends={dashboard.assetAllocationTrends}
          recentAssignments={dashboard.recentAssignments}
          recentReturns={dashboard.recentReturns}
        />
      </Suspense>
    </Box>
  );
}
