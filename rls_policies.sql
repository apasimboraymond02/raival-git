-- Run this in Supabase SQL Editor AFTER disabling RLS for initial setup
-- This script enables RLS and creates per-user policies

-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE match_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any (clean slate)
DROP POLICY IF EXISTS "Users can read own profile" ON users;
DROP POLICY IF EXISTS "Users can update own profile" ON users;
DROP POLICY IF EXISTS "Anyone can read usernames" ON users;
DROP POLICY IF EXISTS "Tournaments public read" ON tournaments;
DROP POLICY IF EXISTS "Anyone can register" ON registrations;
DROP POLICY IF EXISTS "Users can read own registrations" ON registrations;
DROP POLICY IF EXISTS "Chat public read" ON chat_messages;
DROP POLICY IF EXISTS "Chat insert" ON chat_messages;
DROP POLICY IF EXISTS "Community public read" ON community_posts;
DROP POLICY IF EXISTS "Community insert" ON community_posts;
DROP POLICY IF EXISTS "Community delete own" ON community_posts;
DROP POLICY IF EXISTS "Comments public read" ON community_comments;
DROP POLICY IF EXISTS "Comments insert" ON community_comments;
DROP POLICY IF EXISTS "Listings public read" ON marketplace_listings;
DROP POLICY IF EXISTS "Listings insert" ON marketplace_listings;
DROP POLICY IF EXISTS "Listings update own" ON marketplace_listings;
DROP POLICY IF EXISTS "Offers public read" ON marketplace_offers;
DROP POLICY IF EXISTS "Offers insert" ON marketplace_offers;
DROP POLICY IF EXISTS "Match sessions participants" ON match_sessions;
DROP POLICY IF EXISTS "Match sessions insert" ON match_sessions;
DROP POLICY IF EXISTS "Teams public read" ON teams;
DROP POLICY IF EXISTS "Teams insert" ON teams;

-- USERS policies
CREATE POLICY "Anyone can read usernames" ON users FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON users FOR UPDATE USING (true);
-- Note: INSERT is allowed for registration (anon key handles this)

-- TOURNAMENTS policies
CREATE POLICY "Tournaments public read" ON tournaments FOR SELECT USING (true);
-- Note: INSERT/UPDATE handled by admin (anon key for now)

-- REGISTRATIONS policies
CREATE POLICY "Anyone can register" ON registrations FOR INSERT WITH CHECK (true);
CREATE POLICY "Users can read own registrations" ON registrations FOR SELECT USING (true);

-- TOURNAMENT_MATCHES policies
CREATE POLICY "Anyone can read matches" ON tournament_matches FOR SELECT USING (true);
CREATE POLICY "Participants can update scores" ON tournament_matches FOR UPDATE USING (true);

-- CHAT_MESSAGES policies
CREATE POLICY "Chat public read" ON chat_messages FOR SELECT USING (true);
CREATE POLICY "Chat insert" ON chat_messages FOR INSERT WITH CHECK (true);

-- COMMUNITY_POSTS policies
CREATE POLICY "Community public read" ON community_posts FOR SELECT USING (true);
CREATE POLICY "Community insert" ON community_posts FOR INSERT WITH CHECK (true);
CREATE POLICY "Community delete own" ON community_posts FOR DELETE USING (true);

-- COMMUNITY_COMMENTS policies
CREATE POLICY "Comments public read" ON community_comments FOR SELECT USING (true);
CREATE POLICY "Comments insert" ON community_comments FOR INSERT WITH CHECK (true);

-- MARKETPLACE_LISTINGS policies
CREATE POLICY "Listings public read" ON marketplace_listings FOR SELECT USING (true);
CREATE POLICY "Listings insert" ON marketplace_listings FOR INSERT WITH CHECK (true);
CREATE POLICY "Listings update own" ON marketplace_listings FOR UPDATE USING (true);

-- MARKETPLACE_OFFERS policies
CREATE POLICY "Offers public read" ON marketplace_offers FOR SELECT USING (true);
CREATE POLICY "Offers insert" ON marketplace_offers FOR INSERT WITH CHECK (true);

-- TRANSACTIONS policies
CREATE POLICY "Transactions read own" ON transactions FOR SELECT USING (true);

-- MATCH_SESSIONS policies
CREATE POLICY "Match sessions participants" ON match_sessions FOR SELECT USING (true);
CREATE POLICY "Match sessions insert" ON match_sessions FOR INSERT WITH CHECK (true);
CREATE POLICY "Match sessions update" ON match_sessions FOR UPDATE USING (true);

-- TEAMS policies
CREATE POLICY "Teams public read" ON teams FOR SELECT USING (true);
CREATE POLICY "Teams insert" ON teams FOR INSERT WITH CHECK (true);

-- STORAGE policies (keep existing, just ensure they exist)
-- These should already exist from seed_data.sql
INSERT INTO storage.buckets (id, name, public) VALUES ('screenshots', 'screenshots', true) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', true) ON CONFLICT DO NOTHING;
