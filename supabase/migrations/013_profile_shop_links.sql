-- Stores optional public links for a seller's VogueCycle shop profile.
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS shop_links text;
