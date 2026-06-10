// frontend/src/accounts/TransferForm.tsx
// Step 30 · forms done right — React Hook Form for state/submission + Zod for schema validation (one source of
// truth, typed). On submit it fires the transfer mutation with a fresh Idempotency-Key; the mutation's
// onSuccess invalidates the account/entries queries, so the balance + history above refresh automatically.
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { useTransfer } from './queries';

const transferSchema = z.object({
  from: z.string().min(1, 'From account is required'),
  to: z.string().min(1, 'To account is required'),
  amount: z.coerce.number().positive('Amount must be greater than 0'),
  description: z.string().optional(),
});

export function TransferForm({ defaultFrom = '' }: { defaultFrom?: string }) {
  const transfer = useTransfer();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(transferSchema),
    defaultValues: { from: defaultFrom, to: '', amount: 0, description: '' },
  });

  const onSubmit = handleSubmit((values) => {
    transfer.mutate(
      {
        request: { from: values.from, to: values.to, amount: values.amount, description: values.description },
        idempotencyKey: crypto.randomUUID(),
      },
      { onSuccess: () => reset({ from: values.from, to: '', amount: 0, description: '' }) },
    );
  });

  return (
    <form onSubmit={onSubmit} aria-label="Transfer">
      <h3>Make a transfer</h3>
      <label>
        From
        <input {...register('from')} />
      </label>
      {errors.from && <p role="alert">{errors.from.message}</p>}
      <label>
        To
        <input {...register('to')} />
      </label>
      {errors.to && <p role="alert">{errors.to.message}</p>}
      <label>
        Amount
        <input type="number" step="0.01" {...register('amount')} />
      </label>
      {errors.amount && <p role="alert">{errors.amount.message}</p>}
      <label>
        Description
        <input {...register('description')} />
      </label>
      <button type="submit" disabled={transfer.isPending}>
        {transfer.isPending ? 'Sending…' : 'Send transfer'}
      </button>
      {transfer.isError && <p role="alert">{transfer.error.message}</p>}
      {transfer.isSuccess && <p role="status">Transfer {transfer.data.transactionId} sent ✓</p>}
    </form>
  );
}
