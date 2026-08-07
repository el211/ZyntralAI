-- Migrate credit values to half-credit scale (×2).
-- Previously 1 credit was stored as 1; after this migration 1 credit = 2 internal units.
-- This allows fractional credit costs (e.g. 0.5 credits = 1 internal unit).

-- Ledger: double all existing used and limit values
UPDATE ai_credit_ledger
   SET credits_used  = credits_used  * 2,
       credits_limit = credits_limit * 2,
       updated_at    = now()
 WHERE credits_limit > 0;

-- Generation history: double stored credits_cost so history reads correctly
-- (partitioned table — UPDATE applies to all partitions automatically)
UPDATE ai_generations
   SET credits_cost = credits_cost * 2
 WHERE credits_cost > 0;
