-- Run this in Supabase SQL Editor (https://supabase.com/dashboard → SQL Editor)

-- Step 1: Disable RLS on users table
ALTER TABLE users DISABLE ROW LEVEL SECURITY;

-- Step 2: Seed test users (IDs auto-generated)
INSERT INTO users (username, fullName, email, phone, bio, paymentMethod, raivalPoints, balance, coinBalance, totalWinnings, tournamentsPlayed, wins, losses, winStreak, dailyLoginClaimedAt, role, status, createdAt, dlsHandle, efootballHandle, discordHandle, xp, level, streakShields, referralCode, unlockedSkills, completedMissions, selectedAvatar, customTitle, clanTag, nameColor, nameGlow, profileTheme, unlockedThemes, unlockedAvatars, welcomeBonusClaimed, socialFbConnected, socialTwConnected, socialAppleConnected, appRated, referredActiveCount, coinsClaimedFromTopUp, ghanaRegion, ghanaHometown, preferredGame)
VALUES
('Admin', 'Raival Admin', 'admin@raival.com', '0240000000', '', 'MTN', 1200, 1000.0, 5000, 0.0, 0, 0, 0, 0, 0, 'player', 'active', 1753718400000, '', '', '', 0, 1, 0, '', '', '', 'avatar_default', '', '', '', false, 'default', 'default', 'avatar_default', false, false, false, false, false, 0, false, 'Greater Accra', 'Accra', 'Dream League Soccer'),
('Akwasi_Gamer', 'Akwasi Mensah', 'akwasi@raival.com', '0241234567', '', 'MTN', 850, 150.0, 1500, 0.0, 0, 0, 0, 0, 0, 'player', 'active', 1753718400000, '', '', '', 0, 1, 0, '', '', '', 'avatar_default', '', '', '', false, 'default', 'default', 'avatar_default', false, false, false, false, false, 0, false, 'Ashanti', 'Kumasi', 'eFootball'),
('GamerPro_23', 'Kofi GamerPro', 'gamerpro@raival.com', '0551112222', '', 'MTN', 980, 100.0, 2000, 0.0, 0, 0, 0, 0, 0, 'player', 'active', 1753718400000, '', '', '', 0, 1, 0, '', '', '', 'avatar_default', '', '', '', false, 'default', 'default', 'avatar_default', false, false, false, false, false, 0, false, 'Greater Accra', 'Tema', 'Dream League Soccer'),
('PlayerX_007', 'Emmanuel PlayerX', 'playerx@raival.com', '0551234567', '', 'MTN', 720, 200.0, 3000, 0.0, 0, 0, 0, 0, 0, 'player', 'active', 1753718400000, '', '', '', 0, 1, 0, '', '', '', 'avatar_default', '', '', '', false, 'default', 'default', 'avatar_default', false, false, false, false, false, 0, false, 'Western', 'Takoradi', 'FC Mobile'),
('SkillKing_1', 'Prince SkillKing', 'skillking@raival.com', '0559998888', '', 'MTN', 1100, 300.0, 4000, 0.0, 0, 0, 0, 0, 0, 'player', 'active', 1753718400000, '', '', '', 0, 1, 0, '', '', '', 'avatar_default', '', '', '', false, 'default', 'default', 'avatar_default', false, false, false, false, false, 0, false, 'Northern', 'Tamale', 'Dream League Soccer');

-- Step 3: Create storage buckets
INSERT INTO storage.buckets (id, name, public) VALUES ('screenshots', 'screenshots', true) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', true) ON CONFLICT DO NOTHING;

-- Step 4: Allow public read/write on storage buckets
CREATE POLICY "Public read access for screenshots" ON storage.objects FOR SELECT USING (bucket_id = 'screenshots');
CREATE POLICY "Public insert access for screenshots" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'screenshots');
CREATE POLICY "Public read access for avatars" ON storage.objects FOR SELECT USING (bucket_id = 'avatars');
CREATE POLICY "Public insert access for avatars" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'avatars');
