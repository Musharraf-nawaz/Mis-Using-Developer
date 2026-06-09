import { Grid, Typography, Card, CardContent, List, ListItem, ListItemText, Chip } from '@mui/material';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import type { ActivityItem } from '../../types';

const COLORS = ['#1565C0', '#00838F', '#2E7D32', '#F57C00', '#C62828'];

interface DashboardChartsProps {
  assetStatusDistribution: { status: string; count: number }[];
  assetAllocationTrends: { month: string; count: number }[];
  recentAssignments: ActivityItem[];
  recentReturns: ActivityItem[];
  upcomingInterviews: ActivityItem[];
  todayInterviewCount: number;
}

export default function DashboardCharts({
  assetStatusDistribution,
  assetAllocationTrends,
  recentAssignments,
  recentReturns,
  upcomingInterviews,
  todayInterviewCount,
}: DashboardChartsProps) {
  const pieData = assetStatusDistribution.map((d) => ({
    name: d.status,
    value: Number(d.count),
  }));

  const barData = assetAllocationTrends.map((d) => ({
    month: d.month,
    assignments: Number(d.count),
  }));

  return (
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
              {recentAssignments.map((item) => (
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
              {todayInterviewCount} interview(s) today are highlighted
            </Typography>
            <List dense>
              {upcomingInterviews.map((item) => (
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
                  <ListItemText primary={item.title} secondary={item.description} />
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
              {recentReturns.map((item) => (
                <ListItem key={item.id} disablePadding sx={{ py: 0.5 }}>
                  <ListItemText primary={item.title} secondary={item.description} />
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
}
