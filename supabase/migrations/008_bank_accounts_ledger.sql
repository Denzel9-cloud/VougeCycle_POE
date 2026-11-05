-- Demo-only account balances and order transaction ledger.
-- Run after migrations 001 through 007 in the Supabase SQL Editor.

create table if not exists public.bank_accounts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    bank_name text not null check (length(trim(bank_name)) > 0),
    account_name text not null check (length(trim(account_name)) > 0),
    opening_balance numeric(12,2) not null default 0 check (opening_balance >= 0),
    balance numeric(12,2) not null default 0 check (balance >= 0),
    is_default boolean not null default false,
    created_at timestamptz not null default now()
);

create unique index if not exists one_default_bank_account_per_user
    on public.bank_accounts(user_id) where is_default;
create index if not exists bank_accounts_user_id_idx
    on public.bank_accounts(user_id, created_at);

alter table public.bank_accounts enable row level security;

drop policy if exists "Users read their own bank accounts" on public.bank_accounts;
create policy "Users read their own bank accounts" on public.bank_accounts
    for select using (auth.uid() = user_id);
drop policy if exists "Users add their own bank accounts" on public.bank_accounts;
create policy "Users add their own bank accounts" on public.bank_accounts
    for insert with check (auth.uid() = user_id);
drop policy if exists "Users choose their default bank account" on public.bank_accounts;
create policy "Users choose their default bank account" on public.bank_accounts
    for update using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create or replace function public.check_bank_account_limit_and_default()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    account_count integer;
begin
    perform pg_advisory_xact_lock(hashtext(new.user_id::text));
    select count(*) into account_count
    from public.bank_accounts where user_id = new.user_id;
    if account_count >= 3 then
        raise exception 'You can add up to three bank accounts';
    end if;
    if account_count = 0 then
        new.is_default := true;
    end if;
    return new;
end;
$$;

drop trigger if exists enforce_bank_account_limit on public.bank_accounts;
create trigger enforce_bank_account_limit
before insert on public.bank_accounts
for each row execute procedure public.check_bank_account_limit_and_default();

create table if not exists public.account_transactions (
    id uuid primary key default gen_random_uuid(),
    account_id uuid not null references public.bank_accounts(id) on delete restrict,
    user_id uuid not null references public.profiles(id) on delete restrict,
    order_id uuid not null references public.orders(id) on delete restrict,
    transaction_type text not null check (transaction_type in ('debit', 'credit')),
    amount numeric(12,2) not null check (amount > 0),
    payment_reference text not null,
    description text not null,
    created_at timestamptz not null default now(),
    unique (account_id, order_id, transaction_type)
);

create index if not exists account_transactions_account_created_idx
    on public.account_transactions(account_id, created_at desc);
create index if not exists account_transactions_user_created_idx
    on public.account_transactions(user_id, created_at desc);

alter table public.account_transactions enable row level security;
drop policy if exists "Users read their own account transactions" on public.account_transactions;
create policy "Users read their own account transactions" on public.account_transactions
    for select using (auth.uid() = user_id);

alter table public.orders
    add column if not exists buyer_bank_account_id uuid references public.bank_accounts(id) on delete restrict,
    add column if not exists seller_bank_account_id uuid references public.bank_accounts(id) on delete restrict;

create or replace function public.select_order_payout_account()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    buyer_account_owner uuid;
    payout_account uuid;
begin
    if new.status = 'paid' then
        if new.buyer_bank_account_id is null then
            raise exception 'Choose a bank account for this demo payment';
        end if;
        select user_id into buyer_account_owner
        from public.bank_accounts
        where id = new.buyer_bank_account_id;
        if buyer_account_owner is distinct from new.buyer_id then
            raise exception 'The selected payment account does not belong to the buyer';
        end if;
        select id into payout_account
        from public.bank_accounts
        where user_id = new.seller_id and is_default
        limit 1;
        if payout_account is null then
            raise exception 'The seller needs to add a default payout account';
        end if;
        new.seller_bank_account_id := payout_account;
    end if;
    return new;
end;
$$;

drop trigger if exists select_order_payout_account_before_insert on public.orders;
create trigger select_order_payout_account_before_insert
before insert on public.orders
for each row execute procedure public.select_order_payout_account();

create or replace function public.record_order_account_transactions()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    buyer_total numeric(12,2);
begin
    if new.status <> 'paid' then
        return new;
    end if;

    buyer_total := new.amount + coalesce(new.shipping_cost, 0);

    update public.bank_accounts
    set balance = balance - buyer_total
    where id = new.buyer_bank_account_id
      and user_id = new.buyer_id
      and balance >= buyer_total;
    if not found then
        raise exception 'The selected demo account does not have enough balance';
    end if;

    update public.bank_accounts
    set balance = balance + new.amount
    where id = new.seller_bank_account_id
      and user_id = new.seller_id;
    if not found then
        raise exception 'The seller payout account could not be updated';
    end if;

    insert into public.account_transactions
        (account_id, user_id, order_id, transaction_type, amount, payment_reference, description)
    values
        (new.buyer_bank_account_id, new.buyer_id, new.id, 'debit', buyer_total,
         new.payment_reference, 'Demo purchase payment including shipping'),
        (new.seller_bank_account_id, new.seller_id, new.id, 'credit', new.amount,
         new.payment_reference, 'Demo sale proceeds');

    return new;
end;
$$;

drop trigger if exists record_order_account_transactions_after_insert on public.orders;
create trigger record_order_account_transactions_after_insert
after insert on public.orders
for each row execute procedure public.record_order_account_transactions();

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
            and buyer_bank_account_id is not null
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
       or new.delivery_address is distinct from old.delivery_address
       or new.delivery_name is distinct from old.delivery_name
       or new.delivery_phone is distinct from old.delivery_phone
       or new.estimated_delivery is distinct from old.estimated_delivery
       or new.payment_method is distinct from old.payment_method
       or new.payment_reference is distinct from old.payment_reference
       or new.buyer_bank_account_id is distinct from old.buyer_bank_account_id
       or new.seller_bank_account_id is distinct from old.seller_bank_account_id then
        raise exception 'Order ownership, payment and delivery details cannot be changed';
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
