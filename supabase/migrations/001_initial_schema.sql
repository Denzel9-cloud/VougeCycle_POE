-- Run this once in the Supabase SQL editor for a new VougeCycle project.
-- It creates the current app schema, profile trigger, storage bucket and RLS policies.

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text not null unique,
    full_name text not null,
    bio text,
    avatar_url text,
    location text,
    created_at timestamptz not null default now()
);

create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    seller_id uuid not null references public.profiles(id) on delete cascade,
    title text not null check (char_length(title) between 1 and 120),
    description text not null check (char_length(description) between 1 and 3000),
    price numeric(12,2) not null check (price > 0),
    category text not null check (category in ('Women', 'Men', 'Eras', 'Vintage', 'Sports')),
    condition text not null check (condition in ('New with tags', 'New without tags', 'Used - Excellent', 'Used - Good', 'Used - Fair')),
    image_url text,
    is_sold boolean not null default false,
    created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.products enable row level security;

drop policy if exists "Profiles are publicly readable" on public.profiles;
create policy "Profiles are publicly readable" on public.profiles for select using (true);
drop policy if exists "Users insert their own profile" on public.profiles;
create policy "Users insert their own profile" on public.profiles for insert with check (auth.uid() = id);
drop policy if exists "Users update their own profile" on public.profiles;
create policy "Users update their own profile" on public.profiles for update using (auth.uid() = id) with check (auth.uid() = id);
drop policy if exists "Users delete their own profile" on public.profiles;
create policy "Users delete their own profile" on public.profiles for delete using (auth.uid() = id);

drop policy if exists "Products are publicly readable" on public.products;
create policy "Products are publicly readable" on public.products for select using (true);
drop policy if exists "Sellers insert their own products" on public.products;
create policy "Sellers insert their own products" on public.products for insert with check (auth.uid() = seller_id);
drop policy if exists "Sellers update their own products" on public.products;
create policy "Sellers update their own products" on public.products for update using (auth.uid() = seller_id) with check (auth.uid() = seller_id);
drop policy if exists "Sellers delete their own products" on public.products;
create policy "Sellers delete their own products" on public.products for delete using (auth.uid() = seller_id);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    insert into public.profiles (id, username, full_name)
    values (
        new.id,
        coalesce(nullif(new.raw_user_meta_data ->> 'username', ''), split_part(new.email, '@', 1) || '_' || substr(new.id::text, 1, 6)),
        coalesce(nullif(new.raw_user_meta_data ->> 'full_name', ''), 'VougeCycle User')
    );
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute procedure public.handle_new_user();

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('product-images', 'product-images', true, 10485760, array['image/jpeg', 'image/png', 'image/webp'])
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Product images are publicly readable" on storage.objects;
create policy "Product images are publicly readable" on storage.objects for select using (bucket_id = 'product-images');
drop policy if exists "Users upload to their own image folder" on storage.objects;
create policy "Users upload to their own image folder" on storage.objects for insert to authenticated
with check (bucket_id = 'product-images' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "Users update their own images" on storage.objects;
create policy "Users update their own images" on storage.objects for update to authenticated
using (bucket_id = 'product-images' and owner_id = auth.uid()::text);
drop policy if exists "Users delete their own images" on storage.objects;
create policy "Users delete their own images" on storage.objects for delete to authenticated
using (bucket_id = 'product-images' and owner_id = auth.uid()::text);

create index if not exists products_created_at_idx on public.products(created_at desc);
create index if not exists products_seller_id_idx on public.products(seller_id);
