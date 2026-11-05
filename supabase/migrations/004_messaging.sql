-- Run after 003_demo_payments.sql.
-- Adds protected product conversations and messages.

create table if not exists public.conversations (
    id uuid primary key default gen_random_uuid(),
    product_id uuid not null references public.products(id) on delete cascade,
    buyer_id uuid not null references public.profiles(id) on delete cascade,
    seller_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (product_id, buyer_id, seller_id),
    check (buyer_id <> seller_id)
);

create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    sender_id uuid not null references public.profiles(id) on delete cascade,
    receiver_id uuid not null references public.profiles(id) on delete cascade,
    body text not null check (char_length(trim(body)) between 1 and 1000),
    read_at timestamptz,
    created_at timestamptz not null default now(),
    check (sender_id <> receiver_id)
);

alter table public.conversations enable row level security;
alter table public.messages enable row level security;

drop policy if exists "Participants read conversations" on public.conversations;
create policy "Participants read conversations" on public.conversations for select
using (auth.uid() = buyer_id or auth.uid() = seller_id);

drop policy if exists "Buyers start product conversations" on public.conversations;
create policy "Buyers start product conversations" on public.conversations for insert
with check (
    auth.uid() = buyer_id
    and buyer_id <> seller_id
    and exists (
        select 1 from public.products p
        where p.id = product_id and p.seller_id = seller_id
    )
);

drop policy if exists "Participants read messages" on public.messages;
create policy "Participants read messages" on public.messages for select
using (
    exists (
        select 1 from public.conversations c
        where c.id = conversation_id
          and (auth.uid() = c.buyer_id or auth.uid() = c.seller_id)
    )
);

drop policy if exists "Participants send messages" on public.messages;
create policy "Participants send messages" on public.messages for insert
with check (
    auth.uid() = sender_id
    and exists (
        select 1 from public.conversations c
        where c.id = conversation_id
          and (
              (sender_id = c.buyer_id and receiver_id = c.seller_id)
              or (sender_id = c.seller_id and receiver_id = c.buyer_id)
          )
    )
);

drop policy if exists "Receivers mark messages read" on public.messages;
create policy "Receivers mark messages read" on public.messages for update
using (auth.uid() = receiver_id)
with check (auth.uid() = receiver_id and read_at is not null);

create or replace function public.protect_message_update()
returns trigger
language plpgsql
as $$
begin
    if new.id <> old.id
       or new.conversation_id <> old.conversation_id
       or new.sender_id <> old.sender_id
       or new.receiver_id <> old.receiver_id
       or new.body <> old.body
       or new.created_at <> old.created_at then
        raise exception 'Only the read status may be updated';
    end if;
    return new;
end;
$$;

drop trigger if exists protect_message_fields on public.messages;
create trigger protect_message_fields
before update on public.messages
for each row execute procedure public.protect_message_update();

create or replace function public.touch_conversation_after_message()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    update public.conversations set updated_at = now() where id = new.conversation_id;
    return new;
end;
$$;

drop trigger if exists touch_conversation_on_message on public.messages;
create trigger touch_conversation_on_message
after insert on public.messages
for each row execute procedure public.touch_conversation_after_message();

create index if not exists conversations_buyer_idx on public.conversations(buyer_id, updated_at desc);
create index if not exists conversations_seller_idx on public.conversations(seller_id, updated_at desc);
create index if not exists messages_conversation_idx on public.messages(conversation_id, created_at);
create index if not exists messages_receiver_unread_idx on public.messages(receiver_id, read_at);

-- Makes the tables ready for Supabase Realtime subscriptions.
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'messages'
    ) then
        alter publication supabase_realtime add table public.messages;
    end if;
end $$;
