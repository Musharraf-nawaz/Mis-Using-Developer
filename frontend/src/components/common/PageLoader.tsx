import { Box, CircularProgress } from '@mui/material';

export default function PageLoader() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 240 }}>
      <CircularProgress size={36} />
    </Box>
  );
}
