import { useQuery } from '@tanstack/react-query';
import { Grid, Typography, Card, CardContent, Box, List, ListItem, ListItemText, Chip } from '@mui/material';
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
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { dashboardApi } from '../api/services';
import StatCard from '../components/common/StatCard';

const COLORS = ['#1565C0', '#00838F', '#2E7D32', '#F57C00', '#C62828'];

export default function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => dashboardApi.get(),
  });

  const dashboard = data?.data?.data;

  if (isLoading || !dashboard) {
    return <Typography>Loading dashboard...</Typography>;
  }

  const { assetStats, interviewStats } = dashboard;
  const todayInterviews = dashboard.upcomingInterviews.filter((item) => item.today);

  const pieData = dashboard.assetStatusDistribution.map((d) => ({
    name: d.status,
    value: Number(d.count),
  }));

  const barData = dashboard.assetAllocationTrends.map((d) => ({
    month: d.month,
    assignments: Number(d.count),
  }));

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Dashboard Overview
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Real-time insights across assets and interviews
      </Typography>

      <Card
        sx={{
          mb: 3,
          border: '1px solid',
          borderColor: 'divider',
        }}
      >
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

      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Asset Status Distribution
              </Typography>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                    {pieData.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Asset Allocation Trends
              </Typography>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={barData}>
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="assignments" fill="#1565C0" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Recent Assignments
              </Typography>
              <List dense>
                {dashboard.recentAssignments.map((item) => (
                  <ListItem key={item.id} disablePadding sx={{ py: 0.5 }}>
                    <ListItemText primary={item.title} secondary={item.description} />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Upcoming Interviews
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                {todayInterviews.length} interview(s) today are highlighted
              </Typography>
              <List dense>
                {dashboard.upcomingInterviews.map((item) => (
                  <ListItem
                    key={item.id}
                    disablePadding
                    sx={{
                      py: 0.5,
                      px: 0.75,
                      borderRadius: 1,
                      bgcolor: item.today ? 'warning.light' : 'transparent',
                    }}
                  >
                    <ListItemText
                      primary={item.title}
                      secondary={item.description}
                    />
                    {item.today && <Chip label="Today" size="small" color="warning" variant="filled" />}
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Recent Returns
              </Typography>
              <List dense>
                {dashboard.recentReturns.map((item) => (
                  <ListItem key={item.id} disablePadding sx={{ py: 0.5 }}>
                    <ListItemText primary={item.title} secondary={item.description} />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
