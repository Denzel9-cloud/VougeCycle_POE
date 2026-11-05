-- Run after 002_favourites_orders.sql.
-- Adds delivery and demo-payment information to orders.

alter table public.orders add column if not exists delivery_name text;
alter table public.orders add column if not exists delivery_phone text;
alter table public.orders add column if not exists delivery_address text;
alter table public.orders add column if not exists payment_method text
    check (payment_method in ('Card', 'Instant EFT', 'PayShap'));
alter table public.orders add column if not exists payment_reference text unique;
alter table public.orders add column if not exists paid_at timestamptz;

drop policy if exists "Buyers create valid pending orders" on public.orders;
drop policy if exists "Buyers create valid orders" on public.orders;
create policy "Buyers create valid orders" on public.orders for insert
with check (
    auth.uid() = buyer_id
    and status in ('pending', 'paid')
    and (
        status <> 'paid'
        or (
            payment_method is not null
            and payment_reference is not null
            and delivery_name is not null
            and delivery_phone is not null
            and delivery_address is not null
        )
    )
    and exists (
        select 1 from public.products p
        where p.id = product_id
          and p.seller_id = seller_id
          and p.seller_id <> auth.uid()
          and p.price = amount
          and p.is_sold = false
    )
);

create or replace function public.set_demo_payment_time()
returns trigger
language plpgsql
as $$
begin
    if new.status = 'paid' and new.paid_at is null then
        new.paid_at = now();
    end if;
    return new;
end;
$$;

drop trigger if exists set_demo_payment_time_on_order on public.orders;
create trigger set_demo_payment_time_on_order
before insert or update on public.orders
for each row execute procedure public.set_demo_payment_time();
