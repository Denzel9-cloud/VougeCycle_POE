-- Run after 008_bank_accounts_ledger.sql.
-- Adds balance adjustments and logical account deletion while preserving order history.

alter table public.bank_accounts
    add column if not exists is_active boolean not null default true;

alter table public.account_transactions
    alter column order_id drop not null;

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
    from public.bank_accounts
    where user_id = new.user_id and is_active;
    if account_count >= 3 then
        raise exception 'You can add up to three active bank accounts';
    end if;
    if account_count = 0 then
        new.is_default := true;
    end if;
    return new;
end;
$$;

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
            raise exception 'Choose a bank account for this payment';
        end if;
        select user_id into buyer_account_owner
        from public.bank_accounts
        where id = new.buyer_bank_account_id and is_active;
        if buyer_account_owner is distinct from new.buyer_id then
            raise exception 'The selected payment account does not belong to the buyer';
        end if;
        select id into payout_account
        from public.bank_accounts
        where user_id = new.seller_id and is_default and is_active
        limit 1;
        if payout_account is null then
            raise exception 'The seller needs to add a default payout account';
        end if;
        new.seller_bank_account_id := payout_account;
    end if;
    return new;
end;
$$;

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
    perform set_config('vougecycle.internal_order_ledger', 'on', true);

    update public.bank_accounts
    set balance = balance - buyer_total
    where id = new.buyer_bank_account_id
      and user_id = new.buyer_id
      and is_active
      and balance >= buyer_total;
    if not found then
        raise exception 'The selected demo account does not have enough balance';
    end if;

    update public.bank_accounts
    set balance = balance + new.amount
    where id = new.seller_bank_account_id
      and user_id = new.seller_id
      and is_active;
    if not found then
        raise exception 'The seller payout account could not be updated';
    end if;

    perform set_config('vougecycle.internal_order_ledger', 'off', true);

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

create or replace function public.record_manual_bank_balance_adjustment()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    difference numeric(12,2);
    direction text;
    adjustment_label text;
begin
    if new.balance = old.balance
       or current_setting('vougecycle.internal_order_ledger', true) = 'on' then
        return new;
    end if;

    difference := abs(new.balance - old.balance);
    direction := case when new.balance > old.balance then 'credit' else 'debit' end;
    adjustment_label := case
        when new.balance > old.balance then 'Manual balance increase'
        else 'Manual balance decrease'
    end;

    insert into public.account_transactions
        (account_id, user_id, order_id, transaction_type, amount, payment_reference, description)
    values
        (new.id, new.user_id, null, direction, difference,
         'ADJ-' || upper(replace(gen_random_uuid()::text, '-', '')),
         adjustment_label);

    return new;
end;
$$;

drop trigger if exists record_manual_bank_balance_adjustment_after_update on public.bank_accounts;
create trigger record_manual_bank_balance_adjustment_after_update
after update of balance on public.bank_accounts
for each row execute procedure public.record_manual_bank_balance_adjustment();

create or replace function public.choose_replacement_default_bank_account()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    replacement_id uuid;
begin
    if old.is_active and not new.is_active and old.is_default then
        select id into replacement_id
        from public.bank_accounts
        where user_id = new.user_id and is_active and id <> new.id
        order by created_at
        limit 1;
        if replacement_id is not null then
            update public.bank_accounts
            set is_default = true
            where id = replacement_id and user_id = new.user_id;
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists choose_replacement_default_after_account_archive on public.bank_accounts;
create trigger choose_replacement_default_after_account_archive
after update of is_active on public.bank_accounts
for each row execute procedure public.choose_replacement_default_bank_account();
