-- Run after 010_realtime_notifications.sql.
-- Stores additional phone numbers and email addresses privately per account.

create table if not exists public.user_contact_details (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    contact_type text not null check (contact_type in ('phone', 'email')),
    contact_value text not null check (char_length(trim(contact_value)) between 1 and 200),
    created_at timestamptz not null default now()
);

alter table public.user_contact_details enable row level security;

grant select, insert, delete on table public.user_contact_details to authenticated;

 drop policy if exists "Users read their own contact details" on public.user_contact_details;
create policy "Users read their own contact details" on public.user_contact_details
for select to authenticated using (auth.uid() = user_id);

drop policy if exists "Users add their own contact details" on public.user_contact_details;
create policy "Users add their own contact details" on public.user_contact_details
for insert to authenticated with check (auth.uid() = user_id);

drop policy if exists "Users delete their own contact details" on public.user_contact_details;
create policy "Users delete their own contact details" on public.user_contact_details
for delete to authenticated using (auth.uid() = user_id);

create unique index if not exists user_contact_details_unique_value_idx
on public.user_contact_details (user_id, contact_type, lower(trim(contact_value)));

create index if not exists user_contact_details_user_id_idx
on public.user_contact_details (user_id, created_at desc);
