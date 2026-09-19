-- =============================================================================
-- RAIVAL CANONICAL DATABASE SCHEMA v19
-- =============================================================================
-- Source of truth for both Room (Android) and Supabase backend
-- Run this in Supabase SQL Editor AFTER creating the project
-- =============================================================================

-- =============================================================================
-- 1. EXTENSIONS & RLS ENABLEMENT
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE match_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_verifications ENABLE ROW LEVEL SECURITY;

-- =============================================================================
-- 2. USERS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT UNIQUE,
    bio TEXT DEFAULT '',
    payment_method TEXT DEFAULT 'MTN',
    raival_points INTEGER DEFAULT 100,
    balance DOUBLE PRECISION DEFAULT 50.0,       -- GHS cash balance
    coin_balance INTEGER DEFAULT 200,             -- DLS coins
    total_winnings DOUBLE PRECISION DEFAULT 0.0,
    tournaments_played INTEGER DEFAULT 0,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    win_streak INTEGER DEFAULT 0,
    daily_login_claimed_at INTEGER DEFAULT 0,
    role TEXT DEFAULT 'player',                  -- 'player', 'organizer', 'admin'
    status TEXT DEFAULT 'active',                -- 'active', 'banned'
    created_at INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    dls_handle TEXT DEFAULT '',
    efootball_handle TEXT DEFAULT '',
    discord_handle TEXT DEFAULT '',
    xp INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,
    streak_shields INTEGER DEFAULT 0,
    referral_code TEXT UNIQUE NOT NULL,
    unlocked_skills TEXT DEFAULT '',
    completed_missions TEXT DEFAULT '',
    selected_avatar TEXT DEFAULT 'avatar_default',
    custom_title TEXT DEFAULT '',
    clan_tag TEXT DEFAULT '',
    name_color TEXT DEFAULT '',
    name_glow BOOLEAN DEFAULT FALSE,
    profile_theme TEXT DEFAULT 'default',
    unlocked_themes TEXT DEFAULT 'default',
    unlocked_avatars TEXT DEFAULT 'avatar_default',
    welcome_bonus_claimed BOOLEAN DEFAULT FALSE,
    social_fb_connected BOOLEAN DEFAULT FALSE,
    social_tw_connected BOOLEAN DEFAULT FALSE,
    social_apple_connected BOOLEAN DEFAULT FALSE,
    app_rated BOOLEAN DEFAULT FALSE,
    referred_active_count INTEGER DEFAULT 0,
    coins_claimed_from_top_up BOOLEAN DEFAULT FALSE,
    ghana_region TEXT DEFAULT 'Greater Accra',
    ghana_hometown TEXT DEFAULT 'Accra',
    preferred_game TEXT DEFAULT 'Dream League Soccer',
    password_hash TEXT DEFAULT '',
    password_salt TEXT DEFAULT ''
);

-- RLS Policies for users
CREATE POLICY "Users can read own data" ON users
    FOR SELECT USING (auth.uid()::text = split_part(coalesce(metadata->>'sub'), '@', 1) OR role = 'admin');

CREATE POLICY "Admins can update user roles" ON users
    FOR UPDATE USING (role = 'admin');

-- =============================================================================
-- 3. TOURNAMENTS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS tournaments (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    title TEXT NOT NULL,
    game TEXT DEFAULT 'DLS',
    entry_fee DOUBLE PRECISION DEFAULT 0.0,       -- cash entry fee (GHS)
    coin_entry_fee INTEGER DEFAULT 0,            -- coin entry fee (if cash is 0)
    prize DOUBLE PRECISION DEFAULT 0.0,          -- cash prize (GHS)
    coin_prize INTEGER DEFAULT 0,                -- coin prize
    players INTEGER DEFAULT 0,
    max_players INTEGER DEFAULT 16,
    date TEXT,
    time TEXT,
    status TEXT DEFAULT 'Open',                  -- 'Open', 'Coming Soon', 'Closed'
    description TEXT DEFAULT '',
    mode TEXT DEFAULT '1 vs 1',
    map TEXT DEFAULT 'Dream Arena',
    rules TEXT DEFAULT '',
    schedule TEXT DEFAULT '',
    prize_distribution TEXT DEFAULT '',
    contact TEXT DEFAULT '',
    banner TEXT DEFAULT '',                      -- Sup-assigned vibrant background key
    organizer TEXT DEFAULT 'Raival Admin',
    format TEXT DEFAULT 'Knockout',              -- 'League' or 'Knockout'
    style TEXT DEFAULT 'World Cup Style',        -- 'EPL Style', 'Champions League Style', 'World Cup Style'
    is_hidden BOOLEAN DEFAULT FALSE,
    is_auto_hosted BOOLEAN DEFAULT TRUE
);

-- RLS Policies for tournaments
CREATE POLICY "Public can read tournaments" ON tournaments
    FOR SELECT USING (true);

CREATE POLICY "Admins can manage tournaments" ON tournaments
    FOR ALL USING (role = 'admin')
    WITH CHECK (role = 'admin');

-- =============================================================================
-- 4. REGISTRATIONS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS registrations (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tournament_id INTEGER NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    player_name TEXT NOT NULL,
    in_game_name TEXT,
    payment_method TEXT,
    phone TEXT,
    registered_at INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    team_rating INTEGER DEFAULT 0,
    model_team TEXT DEFAULT ''
);

-- RLS Policies for registrations
CREATE POLICY "Users can read own registrations" ON registrations
    FOR SELECT USING (auth.uid()::integer = user_id OR role = 'admin');

CREATE POLICY "Users can insert own registrations" ON registrations
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Admins can manage registrations" ON registrations
    FOR ALL USING (role = 'admin');

-- =============================================================================
-- 5. TRANSACTIONS (MoMo) TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    reference TEXT UNIQUE NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tournament_id INTEGER REFERENCES tournaments(id) ON DELETE SET NULL,
    tournament_title TEXT DEFAULT '',
    amount DOUBLE PRECISION NOT NULL,
    payment_method TEXT DEFAULT 'MTN MoMo',
    phone TEXT,
    status TEXT DEFAULT 'success',              -- 'success', 'failed', 'pending'
    type TEXT DEFAULT 'REGISTRATION',            -- 'REGISTRATION', 'WITHDRAWAL', 'DEPOSIT'
    timestamp INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);

-- RLS Policies for transactions
CREATE POLICY "Users can read own transactions" ON transactions
    FOR SELECT USING (auth.uid()::integer = user_id OR role = 'admin');

CREATE POLICY "Admins can manage transactions" ON transactions
    FOR ALL USING (role = 'admin');

-- =============================================================================
-- 6. TEAMS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS teams (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    member_count INTEGER DEFAULT 1,
    max_members INTEGER DEFAULT 10,
    wins INTEGER DEFAULT 0,
    leader TEXT NOT NULL,
    members TEXT DEFAULT '',          -- comma-separated usernames
    tags TEXT DEFAULT '',             -- comma-separated tags
    created_at INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    is_public BOOLEAN DEFAULT TRUE
);

-- RLS Policies for teams
CREATE POLICY "Public can read teams" ON teams
    FOR SELECT USING (true);

CREATE POLICY "Users can manage own teams" ON teams
    FOR ALL USING (leader = split_part(coalesce(metadata->>'sub'), '@', 1) OR role = 'admin');

-- =============================================================================
-- 7. CHAT MESSAGES TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS chat_messages (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    chat_room_id TEXT NOT NULL,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_username TEXT NOT NULL,
    message TEXT NOT NULL,
    timestamp INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    image_url TEXT DEFAULT ''
);

-- RLS Policies for chat_messages
CREATE POLICY "Users can read own room messages" ON chat_messages
    FOR SELECT USING (
        sender_id = auth.uid()::integer OR
        role = 'admin' OR
        EXISTS (SELECT 1 FROM registrations WHERE tournament_id IN (
            SELECT id FROM tournaments WHERE id = extract(chat_room_id from text)::integer
        ) AND (user_id = auth.uid()::integer OR leader = split_part(coalesce(metadata->>'sub'), '@', 1)))
    );

CREATE POLICY "Users can insert messages" ON chat_messages
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Admins can manage messages" ON chat_messages
    FOR ALL USING (role = 'admin');

-- =============================================================================
-- 8. MATCH SESSIONS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS match_sessions (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    room_code TEXT UNIQUE NOT NULL,
    host_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    host_username TEXT NOT NULL,
    guest_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    guest_username TEXT DEFAULT '',
    status TEXT DEFAULT 'Waiting',           -- 'Waiting', 'Ready', 'Playing', 'Verification', 'Disputed', 'Completed'
    half_length INTEGER DEFAULT 6,
    difficulty TEXT DEFAULT 'Legendary',     -- 'Amateur', 'Semi-Pro', 'Professional', 'Legendary'
    stadium TEXT DEFAULT 'Dream Arena',
    host_score INTEGER DEFAULT 0,
    guest_score INTEGER DEFAULT 0,
    winner_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    host_screenshot TEXT DEFAULT '',
    guest_screenshot TEXT DEFAULT '',
    is_friendly BOOLEAN DEFAULT TRUE,
    game_type TEXT DEFAULT 'eFootball',
    timestamp INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);

-- RLS Policies for match_sessions
CREATE POLICY "Public can read match sessions" ON match_sessions
    FOR SELECT USING (true);

CREATE POLICY "Users can manage own sessions" ON match_sessions
    FOR ALL USING (host_id = auth.uid()::integer OR guest_id = auth.uid()::integer);

-- =============================================================================
-- 9. TOURNAMENT MATCHES TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS tournament_matches (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tournament_id INTEGER NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    round TEXT NOT NULL,              -- 'Quarter-finals', 'Semi-finals', 'Final'
    match_index INTEGER NOT NULL,     -- e.g. 0 to 3 for Quarters, 0 to 1 for Semis, 0 for Final
    player1_name TEXT NOT NULL,
    player2_name TEXT NOT NULL,
    player1_score INTEGER DEFAULT NULL,
    player2_score INTEGER DEFAULT NULL,
    status TEXT DEFAULT 'Pending',    -- 'Pending', 'Completed'
    winner_name TEXT DEFAULT NULL,
    match_date TEXT DEFAULT '',
    match_time TEXT DEFAULT '',
    player1_rating INTEGER DEFAULT 0,
    player2_rating INTEGER DEFAULT 0,
    player1_team TEXT DEFAULT '',
    player2_team TEXT DEFAULT '',
    match_code TEXT DEFAULT '',
    player1_checked_in BOOLEAN DEFAULT FALSE,
    player2_checked_in BOOLEAN DEFAULT FALSE,
    check_in_deadline INTEGER DEFAULT 0,
    player1_disqualified BOOLEAN DEFAULT FALSE,
    player2_disqualified BOOLEAN DEFAULT FALSE,
    match_deadline INTEGER DEFAULT 0
);

-- RLS Policies for tournament_matches
CREATE POLICY "Public can read tournament matches" ON tournament_matches
    FOR SELECT USING (true);

CREATE POLICY "Admins can manage tournament matches" ON tournament_matches
    FOR ALL USING (role = 'admin');

-- =============================================================================
-- 10. APP CONFIG TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS app_config (
    id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    economy_mode TEXT DEFAULT 'Coin-Only',           -- 'Coin-Only', 'Real Cash + Coins'
    hubtel_client_id TEXT DEFAULT '',
    hubtel_client_secret TEXT DEFAULT '',
    hubtel_merchant_id TEXT DEFAULT '',
    hubtel_api_url TEXT DEFAULT 'https://api.hubtel.com/v1/',
    hubtel_payment_methods TEXT DEFAULT 'momo, vodafone',
    hubtel_min_amount DOUBLE PRECISION DEFAULT 1.0,
    hubtel_max_amount DOUBLE PRECISION DEFAULT 500.0,
    hubtel_fee_percentage DOUBLE PRECISION DEFAULT 2.0,
    hubtel_auto_confirm BOOLEAN DEFAULT TRUE,
    hubtel_webhook_url TEXT DEFAULT 'https://raival.com/webhook/hubtel',
    coins_reset_to_zero_done BOOLEAN DEFAULT FALSE
);

-- RLS Policies for app_config
CREATE POLICY "Public can read app config" ON app_config
    FOR SELECT USING (true);

CREATE POLICY "Admins can manage app config" ON app_config
    FOR ALL USING (role = 'admin');

-- =============================================================================
-- 11. MARKETPLACE LISTINGS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS marketplace_listings (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_name TEXT NOT NULL,
    seller_rating FLOAT DEFAULT 4.8,
    game TEXT NOT NULL,                              -- 'eFootball', 'FC Mobile', 'DLS'
    team_name TEXT,
    ovr_rating INTEGER,
    players TEXT,                                    -- comma or newline separated major players
    special_cards_count INTEGER DEFAULT 5,
    coins INTEGER DEFAULT 150000,
    description TEXT DEFAULT '',
    price DOUBLE PRECISION,                         -- GHS
    listing_type TEXT DEFAULT 'Negotiable',          -- 'Fixed Price', 'Negotiable', 'Auction'
    duration_days INTEGER DEFAULT 7,                 -- 7, 14, 30
    status TEXT DEFAULT 'Active',                    -- 'Active', 'Sold', 'Removed'
    views INTEGER DEFAULT 0,
    listed_at INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    screenshot_url TEXT DEFAULT '',                  -- custom decorative thumbnail/preset image name
    rating_type TEXT DEFAULT 'OVR',                  -- 'OVR', 'Collective Strength', 'Team Rating'
    player_screenshots TEXT DEFAULT ''               -- comma-separated or newline-separated filenames
);

-- RLS Policies for marketplace_listings
CREATE POLICY "Public can read listings" ON marketplace_listings
    FOR SELECT USING (true);

CREATE POLICY "Users can manage own listings" ON marketplace_listings
    FOR ALL USING (seller_id = auth.uid()::integer OR role = 'admin');

-- =============================================================================
-- 12. MARKETPLACE OFFERS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS marketplace_offers (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    listing_id INTEGER NOT NULL REFERENCES marketplace_listings(id) ON DELETE CASCADE,
    buyer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    buyer_name TEXT NOT NULL,
    offer_amount DOUBLE PRECISION NOT NULL,
    status TEXT DEFAULT 'Pending',                  -- 'Chatting', 'Pending', 'Accepted', 'Declined'
    timestamp INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);

-- RLS Policies for marketplace_offers
CREATE POLICY "Public can read offers" ON marketplace_offers
    FOR SELECT USING (true);

CREATE POLICY "Users can manage own offers" ON marketplace_offers
    FOR ALL USING (buyer_id = auth.uid()::integer OR role = 'admin');

-- =============================================================================
-- 13. MARKETPLACE TRANSACTIONS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS marketplace_transactions (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    created_at INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    buyer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    platform_fee DOUBLE PRECISION DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL,
    status TEXT DEFAULT 'Payment Held',             -- 'Payment Held', 'Team Transferred', 'Team Verified', 'Completed', 'Disputed', 'Refunded'
    payment_method TEXT DEFAULT 'MTN MoMo',
    phone TEXT,
    seller_screenshot TEXT DEFAULT null,
    buyer_screenshot TEXT DEFAULT null,
    dispute_reason TEXT DEFAULT null,
    dispute_seller_response TEXT DEFAULT null
);

-- RLS Policies for marketplace_transactions
CREATE POLICY "Users can read own transactions" ON marketplace_transactions
    FOR SELECT USING (buyer_id = auth.uid()::integer OR seller_id = auth.uid()::integer OR role = 'admin');

CREATE POLICY "Admins can manage transactions" ON marketplace_transactions
    FOR ALL USING (role = 'admin');

-- =============================================================================
-- 14. COMMUNITY POSTS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS community_posts (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    user_avatar TEXT DEFAULT 'avatar_default',
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT DEFAULT 'General',                -- 'General', 'Tournaments', 'Stats', 'Team Recruitment'
    timestamp INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    likes_count INTEGER DEFAULT 0,
    liked_by_user_ids TEXT DEFAULT '',             -- comma-separated list of user IDs who liked it
    comments_count INTEGER DEFAULT 0,
    tournament_id INTEGER REFERENCES tournaments(id) ON DELETE SET NULL,
    tournament_title TEXT DEFAULT NULL,
    stat_game TEXT DEFAULT NULL,
    stat_value TEXT DEFAULT NULL,
    stat_label TEXT DEFAULT NULL,
    team_id INTEGER REFERENCES teams(id) ON DELETE SET NULL,
    team_name TEXT DEFAULT NULL
);

-- RLS Policies for community_posts
CREATE POLICY "Public can read posts" ON community_posts
    FOR SELECT USING (true);

CREATE POLICY "Users can manage own posts" ON community_posts
    FOR ALL USING (user_id = auth.uid()::integer OR role = 'admin');

-- =============================================================================
-- 15. COMMUNITY COMMENTS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS community_comments (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    post_id INTEGER NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    user_avatar TEXT DEFAULT 'avatar_default',
    content TEXT NOT NULL,
    timestamp INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);

-- RLS Policies for community_comments
CREATE POLICY "Public can read comments" ON community_comments
    FOR SELECT USING (true);

CREATE POLICY "Users can manage own comments" ON community_comments
    FOR ALL USING (user_id = auth.uid()::integer OR role = 'admin');

-- =============================================================================
-- 16. TOURNAL VERIFICATIONS TABLE (RLS metadata)
-- =============================================================================

CREATE TABLE IF NOT EXISTS tournament_verifications (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    tournament_id INTEGER NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    verified_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    verified_at INTEGER DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    verification_status TEXT DEFAULT 'pending', -- 'pending', 'approved', 'rejected'
    verification_notes TEXT DEFAULT ''
);

-- RLS Policies for tournament_verifications
CREATE POLICY "Admins can manage verifications" ON tournament_verifications
    FOR ALL USING (role = 'admin');

-- =============================================================================
-- 17. ROW LEVEL SECURITY POLICIES SUMMARY
-- =============================================================================

-- Public read access where appropriate (no FOR ALL USING (true) except where explicitly allowed)
-- Admin-only write access via is_admin() JWT claim check
-- User-owned data access via auth.uid() matching

-- =============================================================================
-- 18. MIGRATION v20 — real-account + real-economy columns and tables
-- Run AFTER sections 1-17 (safe to re-run: all statements are idempotent).
-- =============================================================================

-- Anonymous-auth linkage + server-issued Pro license on users
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_uid TEXT DEFAULT '';
ALTER TABLE users ADD COLUMN IF NOT EXISTS pro_license_expires_at BIGINT DEFAULT 0;

-- Server-authoritative economy switch on app_config
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS server_authoritative_economy BOOLEAN DEFAULT FALSE;

-- Predictions: one row per user pick, settled server-side
CREATE TABLE IF NOT EXISTS predictions (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username TEXT NOT NULL DEFAULT '',
    tournament_id INTEGER REFERENCES tournaments(id) ON DELETE CASCADE,
    predicted_winner TEXT NOT NULL DEFAULT '',
    predicted_score1 INTEGER NOT NULL DEFAULT 0,
    predicted_score2 INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending',   -- pending | settled | void
    is_correct BOOLEAN,
    settled_at BIGINT,
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);
ALTER TABLE predictions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can read predictions" ON predictions;
CREATE POLICY "Public can read predictions" ON predictions
    FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can manage own predictions" ON predictions;
CREATE POLICY "Users can manage own predictions" ON predictions
    FOR ALL USING (user_id = auth.uid()::integer OR role = 'admin');

-- Ledger: server-owned money movement (Edge Functions write success rows;
-- clients only ever read their own rows and insert pending requests)
CREATE TABLE IF NOT EXISTS ledger (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind TEXT NOT NULL,                        -- deposit | withdrawal | reward | entry_fee | payout
    amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    coin_amount INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending',    -- pending | success | failed
    reference TEXT UNIQUE NOT NULL,
    provider TEXT DEFAULT '',                  -- hubtel | server
    provider_ref TEXT DEFAULT '',
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);
ALTER TABLE ledger ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users can read own ledger" ON ledger;
CREATE POLICY "Users can read own ledger" ON ledger
    FOR SELECT USING (user_id = auth.uid()::integer OR role = 'admin');
DROP POLICY IF EXISTS "Admins can manage ledger" ON ledger;
CREATE POLICY "Admins can manage ledger" ON ledger
    FOR ALL USING (role = 'admin');

NOTIFY pgrst, 'reload schema';

-- =============================================================================
-- END OF CANONICAL SCHEMA
-- =============================================================================