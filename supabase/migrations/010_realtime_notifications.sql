-- Run after 004_messaging.sql. Creates in-app notifications for orders and messages.

create table if not exists public.notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    actor_id uuid references public.profiles(id) on delete set null,
    title text not null,
    body text not null,
    notification_type text not null,
    related_order_id uuid references public.orders(id) on delete cascade,
    related_message_id uuid references public.messages(id) on delete cascade,
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);

alter table public.notifications enable row level security;

drop policy if exists "Users read their own notifications" on public.notifications;
create policy "Users read their own notifications" on public.notifications
for select using (auth.uid() = user_id);

drop policy if exists "Users mark their own notifications read" on public.notifications;
create policy "Users mark their own notifications read" on public.notifications
for update using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- Only the read flag can be changed by the notification owner.
create or replace function public.protect_notification_update()
returns trigger
language plpgsql
as $$
begin
    if new.id <> old.id
       or new.user_id <> old.user_id
       or new.actor_id is distinct from old.actor_id
       or new.title <> old.title
       or new.body <> old.body
       or new.notification_type <> old.notification_type
       or new.related_order_id is distinct from old.related_order_id
       or new.related_message_id is distinct from old.related_message_id
       or new.created_at <> old.created_at
       or (old.is_read and not new.is_read) then
        raise exception 'Only unread notifications may be marked read';
    end if;
    return new;
end;
$$;

drop trigger if exists protect_notification_fields on public.notifications;
create trigger protect_notification_fields
before update on public.notifications
for each row execute procedure public.protect_notification_update();

-- Add notification rows server-side so clients cannot forge order/message alerts.
create or replace function public.notify_new_order()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare
    product_title text;
begin
    select p.title into product_title from public.products p where p.id = new.product_id;
    insert into public.notifications(user_id, actor_id, title, body, notification_type, related_order_id)
    values (
        new.seller_id,
        new.buyer_id,
        'New order',
        'A customer placed an order for ' || coalesce(product_title, 'your item') || '.',
        'new_order',
        new.id
    );
    return new;
end;
$$;

drop trigger if exists notification_after_new_order on public.orders;
create trigger notification_after_new_order
after insert on public.orders
for each row execute procedure public.notify_new_order();

create or replace function public.notify_order_status_change()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    if new.status is distinct from old.status then
        insert into public.notifications(user_id, actor_id, title, body, notification_type, related_order_id)
        values (
            new.buyer_id,
            new.seller_id,
            'Order update',
            'Your order is now ' || replace(new.status, '_', ' ') || '.',
            'order_' || new.status,
            new.id
        );
    end if;
    return new;
end;
$$;

drop trigger if exists notification_after_order_status_change on public.orders;
create trigger notification_after_order_status_change
after update of status on public.orders
for each row execute procedure public.notify_order_status_change();

create or replace function public.notify_new_message()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    insert into public.notifications(user_id, actor_id, title, body, notification_type, related_message_id)
    values (
        new.receiver_id,
        new.sender_id,
        'New message',
        'New message: ' || left(new.body, 180),
        'new_message',
        new.id
    );
    return new;
end;
$$;

drop trigger if exists notification_after_new_message on public.messages;
create trigger notification_after_new_message
after insert on public.messages
for each row execute procedure public.notify_new_message();

create index if not exists notifications_user_created_idx
    on public.notifications(user_id, created_at desc);

-- Realtime is opt-in for each table in Supabase.
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'notifications'
    ) then
        alter publication supabase_realtime add table public.notifications;
    end if;
end $$;
