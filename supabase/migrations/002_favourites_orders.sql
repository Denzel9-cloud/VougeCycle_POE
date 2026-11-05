-- Run after 001_initial_schema.sql.
-- Adds working favourites and protected buyer/seller order flows.

create table if not exists public.favourites (
    user_id uuid not null references public.profiles(id) on delete cascade,
    product_id uuid not null references public.products(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, product_id)
);

create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    buyer_id uuid not null references public.profiles(id) on delete restrict,
    seller_id uuid not null references public.profiles(id) on delete restrict,
    product_id uuid not null unique references public.products(id) on delete restrict,
    amount numeric(12,2) not null check (amount > 0),
    status text not null default 'pending' check (status in ('pending', 'paid', 'shipped', 'delivered')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (buyer_id <> seller_id)
);

alter table public.favourites enable row level security;
alter table public.orders enable row level security;

drop policy if exists "Users read their own favourites" on public.favourites;
create policy "Users read their own favourites" on public.favourites for select
using (auth.uid() = user_id);
drop policy if exists "Users add their own favourites" on public.favourites;
create policy "Users add their own favourites" on public.favourites for insert
with check (auth.uid() = user_id);
drop policy if exists "Users remove their own favourites" on public.favourites;
create policy "Users remove their own favourites" on public.favourites for delete
using (auth.uid() = user_id);

drop policy if exists "Buyers and sellers read involved orders" on public.orders;
create policy "Buyers and sellers read involved orders" on public.orders for select
using (auth.uid() = buyer_id or auth.uid() = seller_id);

drop policy if exists "Buyers create valid pending orders" on public.orders;
create policy "Buyers create valid pending orders" on public.orders for insert
with check (
    auth.uid() = buyer_id
    and status = 'pending'
    and exists (
        select 1 from public.products p
        where p.id = product_id
          and p.seller_id = seller_id
          and p.seller_id <> auth.uid()
          and p.price = amount
          and p.is_sold = false
    )
);

drop policy if exists "Sellers update their orders" on public.orders;
create policy "Sellers update their orders" on public.orders for update
using (auth.uid() = seller_id)
with check (auth.uid() = seller_id);

create or replace function public.reserve_product_after_order()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    update public.products
    set is_sold = true
    where id = new.product_id and is_sold = false;
    if not found then
        raise exception 'This product is no longer available';
    end if;
    return new;
end;
$$;

drop trigger if exists reserve_product_on_order on public.orders;
create trigger reserve_product_on_order
after insert on public.orders
for each row execute procedure public.reserve_product_after_order();

create or replace function public.validate_order_update()
returns trigger
language plpgsql
as $$
begin
    if new.buyer_id <> old.buyer_id
       or new.seller_id <> old.seller_id
       or new.product_id <> old.product_id
       or new.amount <> old.amount then
        raise exception 'Order ownership and amount cannot be changed';
    end if;
    if (old.status = 'pending' and new.status <> 'paid')
       or (old.status = 'paid' and new.status <> 'shipped')
       or (old.status = 'shipped' and new.status <> 'delivered')
       or old.status = 'delivered' then
        raise exception 'Invalid order status transition';
    end if;
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists validate_order_status_update on public.orders;
create trigger validate_order_status_update
before update on public.orders
for each row execute procedure public.validate_order_update();

create index if not exists favourites_user_id_idx on public.favourites(user_id);
create index if not exists orders_buyer_id_idx on public.orders(buyer_id, created_at desc);
create index if not exists orders_seller_id_idx on public.orders(seller_id, created_at desc);
