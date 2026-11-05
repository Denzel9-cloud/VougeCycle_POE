-- Run after 006_profile_images.sql.
-- Adds verified seller reviews for delivered orders.

create table if not exists public.reviews (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null unique references public.orders(id) on delete cascade,
    buyer_id uuid not null references public.profiles(id) on delete cascade,
    seller_id uuid not null references public.profiles(id) on delete cascade,
    product_id uuid not null references public.products(id) on delete cascade,
    rating integer not null check (rating between 1 and 5),
    comment text not null check (char_length(trim(comment)) between 3 and 500),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (buyer_id <> seller_id)
);

alter table public.reviews enable row level security;

drop policy if exists "Reviews are publicly readable" on public.reviews;
create policy "Reviews are publicly readable" on public.reviews for select using (true);

drop policy if exists "Buyers review delivered orders" on public.reviews;
create policy "Buyers review delivered orders" on public.reviews for insert
with check (
    auth.uid() = buyer_id
    and exists (
        select 1 from public.orders o
        where o.id = order_id
          and o.buyer_id = buyer_id
          and o.seller_id = seller_id
          and o.product_id = product_id
          and o.status = 'delivered'
    )
);

drop policy if exists "Buyers update their own reviews" on public.reviews;
create policy "Buyers update their own reviews" on public.reviews for update
using (auth.uid() = buyer_id)
with check (auth.uid() = buyer_id);

drop policy if exists "Buyers delete their own reviews" on public.reviews;
create policy "Buyers delete their own reviews" on public.reviews for delete
using (auth.uid() = buyer_id);

create or replace function public.protect_review_update()
returns trigger
language plpgsql
as $$
begin
    if new.id <> old.id
       or new.order_id <> old.order_id
       or new.buyer_id <> old.buyer_id
       or new.seller_id <> old.seller_id
       or new.product_id <> old.product_id
       or new.created_at <> old.created_at then
        raise exception 'Review ownership and order cannot be changed';
    end if;
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists protect_review_fields on public.reviews;
create trigger protect_review_fields
before update on public.reviews
for each row execute procedure public.protect_review_update();

create index if not exists reviews_seller_idx on public.reviews(seller_id, created_at desc);
create index if not exists reviews_buyer_idx on public.reviews(buyer_id, created_at desc);
