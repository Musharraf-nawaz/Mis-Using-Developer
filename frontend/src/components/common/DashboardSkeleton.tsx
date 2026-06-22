import { Box, Card, CardContent, Grid, Skeleton } from '@mui/material';

export default function DashboardSkeleton() {
  return (
    <Box>
      <Skeleton width={220} height={36} sx={{ mb: 1 }} />
      <Skeleton width={320} height={20} sx={{ mb: 3 }} />
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Skeleton width={180} height={28} sx={{ mb: 2 }} />
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} height={44} sx={{ mb: 1 }} />
          ))}
        </CardContent>
      </Card>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {[1, 2, 3, 4, 5].map((i) => (
          <Grid item xs={12} sm={6} md={4} lg={2} key={i}>
            <Skeleton variant="rounded" height={96} />
          </Grid>
        ))}
      </Grid>
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Skeleton variant="rounded" height={300} />
        </Grid>
        <Grid item xs={12} md={8}>
          <Skeleton variant="rounded" height={300} />
        </Grid>
      </Grid>
    </Box>
  );
}
