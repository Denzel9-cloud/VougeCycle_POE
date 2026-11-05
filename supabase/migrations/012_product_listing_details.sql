-- Optional seller-provided item and delivery information.
alter table public.products
    add column if not exists brand text,
    add column if not exists size text,
    add column if not exists colour text,
    add column if not exists material text,
    add column if not exists seller_delivery_preference text not null default 'Both courier options'
        check (seller_delivery_preference in ('Both courier options', 'Door-to-door only', 'Locker collection only')),
    add column if not exists seller_delivery_notes text;
