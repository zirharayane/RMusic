-- ========================================================
-- RMusic (RM) — Supabase Database Setup Script
-- Run this in the Supabase Dashboard -> SQL Editor -> Run
-- ========================================================

-- 1. Enable UUID Extension if not enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Create Playlists Table
CREATE TABLE IF NOT EXISTS public.playlists (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    subtitle TEXT DEFAULT '',
    thumbnail TEXT,
    tracks_json TEXT DEFAULT '[]',
    updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- 3. Create Liked Songs Table
CREATE TABLE IF NOT EXISTS public.liked_tracks (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    track_id TEXT NOT NULL,
    title TEXT DEFAULT '',
    artist TEXT DEFAULT '',
    thumbnail TEXT,
    duration_seconds INT DEFAULT 0,
    added_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, track_id)
);

-- 4. Create Favorite Artists Table
CREATE TABLE IF NOT EXISTS public.fav_artists (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    artist_id TEXT NOT NULL,
    name TEXT NOT NULL,
    thumbnail TEXT,
    added_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, artist_id)
);

-- 5. Enable Row Level Security (RLS)
ALTER TABLE public.playlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.liked_tracks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fav_artists ENABLE ROW LEVEL SECURITY;

-- 6. RLS Policies for Playlists
DROP POLICY IF EXISTS "Users can manage their own playlists" ON public.playlists;
CREATE POLICY "Users can manage their own playlists"
    ON public.playlists
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 7. RLS Policies for Liked Tracks
DROP POLICY IF EXISTS "Users can manage their liked tracks" ON public.liked_tracks;
CREATE POLICY "Users can manage their liked tracks"
    ON public.liked_tracks
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 8. RLS Policies for Favorite Artists
DROP POLICY IF EXISTS "Users can manage their favorite artists" ON public.fav_artists;
CREATE POLICY "Users can manage their favorite artists"
    ON public.fav_artists
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 9. Explicit Grants for PostgREST & Auth Users (Fixes 42501 permission denied)
GRANT ALL ON TABLE public.playlists TO authenticated;
GRANT ALL ON TABLE public.liked_tracks TO authenticated;
GRANT ALL ON TABLE public.fav_artists TO authenticated;
GRANT ALL ON TABLE public.playlists TO anon;
GRANT ALL ON TABLE public.liked_tracks TO anon;
GRANT ALL ON TABLE public.fav_artists TO anon;

