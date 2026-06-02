import { Box, Typography, Card, CardContent, Grid, Avatar, Chip } from '@mui/material';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} gutterBottom>My Profile</Typography>
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mb: 3 }}>
            <Avatar sx={{ width: 80, height: 80, fontSize: 32, bgcolor: 'primary.main' }}>
              {user.fullName.charAt(0)}
            </Avatar>
            <Box>
              <Typography variant="h6" fontWeight={600}>{user.fullName}</Typography>
              <Typography variant="body2" color="text.secondary">{user.email}</Typography>
              <Chip label={user.role} color="primary" size="small" sx={{ mt: 1 }} />
            </Box>
          </Box>
          <Grid container spacing={2}>
            {[
              ['Department', user.department],
              ['User ID', String(user.userId)],
              ['Role', user.role],
            ].map(([label, value]) => (
              <Grid item xs={12} sm={4} key={label}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="body1" fontWeight={500}>{value || '-'}</Typography>
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>
    </Box>
  );
}
