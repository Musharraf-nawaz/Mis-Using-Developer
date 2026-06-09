import { useEffect, useState } from 'react';
import {
  Box,
  Stepper,
  Step,
  StepLabel,
  StepContent,
  TextField,
  MenuItem,
  Button,
  Typography,
  Chip,
  CircularProgress,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { interviewApi } from '../../api/services';
import { useAuth } from '../../context/AuthContext';
import type { Interview, InterviewRoundData, RoundStatus } from '../../types';

const ROUND_LABELS = ['Round 1', 'Round 2', 'Round 3 (Optional)'];
const STATUS_OPTIONS: RoundStatus[] = ['SCHEDULED', 'PASSED', 'FAILED', 'CANCELLED'];

const STATUS_COLOR: Record<RoundStatus, 'primary' | 'success' | 'error' | 'default'> = {
  SCHEDULED: 'primary',
  PASSED: 'success',
  FAILED: 'error',
  CANCELLED: 'default',
};

interface Props {
  interview: Interview;
  onClose?: () => void;
}

export default function InterviewStepper({ interview, onClose }: Props) {
  const { hasRole } = useAuth();
  const isAdmin = hasRole('ADMIN');
  const queryClient = useQueryClient();
  const [activeStep, setActiveStep] = useState(0);
  const [forms, setForms] = useState<Record<number, Partial<InterviewRoundData>>>({});

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['interview-rounds', interview.id],
    queryFn: async () => {
      const res = await interviewApi.getRounds(interview.id);
      if (!res.data.data?.length && isAdmin) {
        const init = await interviewApi.initRounds(interview.id);
        return init.data.data;
      }
      return res.data.data;
    },
  });

  const rounds = data ?? [];

  useEffect(() => {
    if (rounds.length) {
      const idx = rounds.findIndex((r) => r.status === 'SCHEDULED' || r.status === 'PASSED');
      setActiveStep(idx >= 0 ? idx : 0);
      const initial: Record<number, Partial<InterviewRoundData>> = {};
      rounds.forEach((r) => {
        initial[r.roundNumber] = { ...r };
      });
      setForms(initial);
    }
  }, [rounds]);

  const updateMutation = useMutation({
    mutationFn: ({ roundNumber, payload }: { roundNumber: number; payload: Record<string, unknown> }) =>
      interviewApi.updateRound(interview.id, roundNumber, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interview-rounds', interview.id] });
      queryClient.invalidateQueries({ queryKey: ['interviews'] });
      refetch();
      toast.success('Round updated');
    },
    onError: () => toast.error('Failed to update round'),
  });

  const updateField = (roundNumber: number, field: string, value: string) => {
    setForms((prev) => ({
      ...prev,
      [roundNumber]: { ...prev[roundNumber], [field]: value },
    }));
  };

  const saveRound = (roundNumber: number) => {
    const form = forms[roundNumber];
    updateMutation.mutate({
      roundNumber,
      payload: {
        interviewLink: form?.interviewLink,
        interviewDate: form?.interviewDate,
        interviewTime: form?.interviewTime,
        companyToRepresent: form?.companyToRepresent,
        interviewer: form?.interviewer,
        status: form?.status,
      },
    });
  };

  if (isLoading) {
    return (
      <Box sx={{ p: 4, textAlign: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', gap: 1, alignItems: 'center' }}>
        <Typography variant="subtitle1" fontWeight={700}>
          {interview.candidateName}
        </Typography>
        {interview.finalStatus && (
          <Chip label={interview.finalStatus} color={interview.finalStatus === 'SELECTED' ? 'success' : 'error'} size="small" />
        )}
      </Box>
      <Stepper activeStep={activeStep} orientation="vertical">
        {ROUND_LABELS.map((label, index) => {
          const roundNumber = index + 1;
          const round = rounds.find((r) => r.roundNumber === roundNumber);
          const available = round?.available ?? roundNumber === 1;
          const form = forms[roundNumber] || round || {};

          return (
            <Step key={label} completed={round?.status === 'PASSED'}>
              <StepLabel
                optional={
                  round ? (
                    <Chip label={round.status} size="small" color={STATUS_COLOR[round.status]} />
                  ) : (
                    <Typography variant="caption">Not initialized</Typography>
                  )
                }
              >
                {label}
                {!available && (
                  <Typography variant="caption" color="text.secondary" display="block">
                    Unlocks when previous round passes
                  </Typography>
                )}
              </StepLabel>
              <StepContent>
                {available ? (
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, pb: 2 }}>
                    <TextField
                      size="small"
                      label="Date"
                      type="date"
                      value={form.interviewDate || ''}
                      onChange={(e) => updateField(roundNumber, 'interviewDate', e.target.value)}
                      InputLabelProps={{ shrink: true }}
                      disabled={!isAdmin}
                    />
                    <TextField
                      size="small"
                      label="Time"
                      type="time"
                      value={form.interviewTime || ''}
                      onChange={(e) => updateField(roundNumber, 'interviewTime', e.target.value)}
                      InputLabelProps={{ shrink: true }}
                      disabled={!isAdmin}
                    />
                    <TextField
                      size="small"
                      label="Interview Link"
                      value={form.interviewLink || ''}
                      onChange={(e) => updateField(roundNumber, 'interviewLink', e.target.value)}
                      disabled={!isAdmin}
                    />
                    <TextField
                      size="small"
                      label="Company To Represent"
                      value={form.companyToRepresent || ''}
                      onChange={(e) => updateField(roundNumber, 'companyToRepresent', e.target.value)}
                      disabled={!isAdmin}
                    />
                    <TextField
                      size="small"
                      label="Interviewer"
                      value={form.interviewer || ''}
                      onChange={(e) => updateField(roundNumber, 'interviewer', e.target.value)}
                      disabled={!isAdmin}
                    />
                    <TextField
                      size="small"
                      select
                      label="Status"
                      value={form.status || 'SCHEDULED'}
                      onChange={(e) => updateField(roundNumber, 'status', e.target.value)}
                      disabled={!isAdmin}
                    >
                      {STATUS_OPTIONS.map((s) => (
                        <MenuItem key={s} value={s}>
                          {s}
                        </MenuItem>
                      ))}
                    </TextField>
                    {isAdmin && (
                      <Button
                        variant="contained"
                        size="small"
                        onClick={() => saveRound(roundNumber)}
                        disabled={updateMutation.isPending}
                      >
                        Save Round {roundNumber}
                      </Button>
                    )}
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    Complete previous round with PASSED status to unlock.
                  </Typography>
                )}
              </StepContent>
            </Step>
          );
        })}
      </Stepper>
      {onClose && (
        <Button onClick={onClose} sx={{ mt: 1 }}>
          Close
        </Button>
      )}
    </Box>
  );
}
