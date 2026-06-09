import { lazy, Suspense } from 'react';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { Grid, Typography, Card, CardContent, Box, List, ListItem, ListItemText, Chip, Skeleton } from '@mui/material';
import {
  Inventory,
  CheckCircle,
  Assignment,
  Undo,
  Warning,
  Event,
  Schedule,
  Done,
  Cancel,
} from '@mui/icons-material';
import { dashboardApi } from '../api/services';
import StatCard from '../components/common/StatCard';
import DashboardSkeleton from '../components/common/DashboardSkeleton';

const DashboardCharts = lazy(() => import('../components/dashboard/DashboardCharts'));

export default function DashboardPage() {
  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get(),
    placeholderData: keepPreviousData,
  });

  const dashboard = data?.data?.data;

  if (isLoading && !dashboard) {
    return <DashboardSkeleton />;
  }

  if (!dashboard) {
    return <Typography>Unable to load dashboard.</Typography>;
  }

  const { assetStats, interviewStats } = dashboard;
  const todayInterviews = dashboard.upcomingInterviews.filter((item) => item.today);

  return (
    <Box sx={{ opacity: isFetching ? 0.85 : 1, transition: 'opacity 0.2s' }}>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Dashboard Overview
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Real-time insights across assets and interviews
      </Typography>

      <Card sx={{ mb: 3, border: '1px solid', borderColor: 'divider' }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} gutterBottom>
            Interview Details
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
          <StatCard title="Returned" value={assetStats.returnedAssets} icon={<Undo />} color="#00838F" />
        </Grid>
        <Grid item xs={12} sm={6} md={2.4}>
          <StatCard title="Damaged" value={assetStats.damagedAssets} icon={<Warning />} color="#C62828" />
        </Grid>
      </Grid>

      <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1.5 }}>
        INTERVIEW METRICS
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Today's Interviews" value={interviewStats.todayInterviews} icon={<Event />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Upcoming" value={interviewStats.upcomingInterviews} icon={<Schedule />} color="#00838F" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Completed" value={interviewStats.completedInterviews} icon={<Done />} color="#2E7D32" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard title="Cancelled" value={interviewStats.cancelledInterviews} icon={<Cancel />} color="#C62828" />
        </Grid>
      </Grid>

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
          upcomingInterviews={dashboard.upcomingInterviews}
          todayInterviewCount={todayInterviews.length}
        />
      </Suspense>
    </Box>
  );
}
