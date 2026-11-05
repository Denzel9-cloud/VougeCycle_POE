-- Run after 004_messaging.sql.
-- Adds demo courier selection, shipping prices and order tracking.

alter table public.orders add column if not exists courier text;
alter table public.orders add column if not exists delivery_method text;
alter table public.orders add column if not exists shipping_cost numeric(12,2) not null default 0;
alter table public.orders add column if not exists estimated_delivery text;
alter table public.orders add column if not exists tracking_number text;
alter table public.orders add column if not exists shipped_at timestamptz;
alter table public.orders add column if not exists delivered_at timestamptz;

alter table public.orders drop constraint if exists orders_status_check;
alter table public.orders add constraint orders_status_check
check (status in ('pending', 'paid', 'preparing', 'shipped', 'out_for_delivery', 'delivered'));

alter table public.orders drop constraint if exists orders_courier_check;
alter table public.orders add constraint orders_courier_check
check (courier is null or courier in ('Pudo', 'The Courier Guy', 'Buffalo Courier'));

alter table public.orders drop constraint if exists orders_delivery_method_check;
alter table public.orders add constraint orders_delivery_method_check
check (delivery_method is null or delivery_method in ('Door-to-door', 'Locker collection'));

alter table public.orders drop constraint if exists orders_shipping_cost_check;
alter table public.orders add constraint orders_shipping_cost_check check (shipping_cost >= 0);

create unique index if not exists orders_tracking_number_unique
on public.orders(tracking_number) where tracking_number is not null;

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
            and courier is not null
            and delivery_method is not null
            and shipping_cost > 0
            and estimated_delivery is not null
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

create or replace function public.validate_order_update()
returns trigger
language plpgsql
as $$
begin
    if new.buyer_id <> old.buyer_id
       or new.seller_id <> old.seller_id
       or new.product_id <> old.product_id
       or new.amount <> old.amount
       or new.shipping_cost <> old.shipping_cost
       or new.courier is distinct from old.courier
       or new.delivery_method is distinct from old.delivery_method
       or new.delivery_address is distinct from old.delivery_address then
        raise exception 'Order ownership, amount and delivery selection cannot be changed';
    end if;

    if (old.status = 'pending' and new.status <> 'paid')
       or (old.status = 'paid' and new.status <> 'preparing')
       or (old.status = 'preparing' and new.status <> 'shipped')
       or (old.status = 'shipped' and new.status <> 'out_for_delivery')
       or (old.status = 'out_for_delivery' and new.status <> 'delivered')
       or old.status = 'delivered' then
        raise exception 'Invalid order status transition';
    end if;

    if new.status in ('shipped', 'out_for_delivery', 'delivered')
       and new.tracking_number is null then
        raise exception 'A tracking number is required after dispatch';
    end if;
    new.updated_at = now();
    return new;
end;
$$;

create or replace function public.set_shipping_timestamps()
returns trigger
language plpgsql
as $$
begin
    if new.status = 'shipped' and old.status <> 'shipped' then
        new.shipped_at = now();
    end if;
    if new.status = 'delivered' and old.status <> 'delivered' then
        new.delivered_at = now();
    end if;
    return new;
end;
$$;

drop trigger if exists set_shipping_times_on_order on public.orders;
create trigger set_shipping_times_on_order
before update on public.orders
for each row execute procedure public.set_shipping_timestamps();
