import React, { useState, useEffect } from 'react';
import { 
  Trophy, 
  Gamepad2, 
  Coins, 
  Users, 
  Calendar, 
  Clock, 
  Search, 
  Filter, 
  AlertCircle, 
  Loader2, 
  ArrowUpRight 
} from 'lucide-react';

/**
 * ActiveTournamentsGrid - A premium, production-grade React component 
 * that displays a responsive grid of active/open tournaments fetched in real-time 
 * from Supabase.
 * 
 * @param {object} props.supabase - Supabase client instance. If not provided, it falls back to window.supabase.
 * @param {function} props.onSelectTournament - Optional callback when a tournament card is clicked.
 */
export default function ActiveTournamentsGrid({ supabase, onSelectTournament }) {
  const [tournaments, setTournaments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedGame, setSelectedGame] = useState('All');
  const [sortBy, setSortBy] = useState('newest'); // 'newest' | 'prize' | 'entryFee'

  useEffect(() => {
    const activeClient = supabase || window.supabase;

    if (!activeClient) {
      setError("Supabase database client is not connected. Configure your Supabase credentials.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const fetchTournaments = async () => {
      try {
        const { data, error } = await activeClient
          .from('tournaments')
          .select('*')
          .not('status', 'in', '("Closed","Archived")')
          .not('isHidden', 'eq', true);

        if (error) throw error;
        setTournaments(data || []);
        setLoading(false);
      } catch (err) {
        console.error("Supabase Error in ActiveTournamentsGrid:", err);
        setError(`Failed to retrieve tournaments: ${err.message}`);
        setLoading(false);
      }
    };

    fetchTournaments();

    // Realtime subscription
    const channel = activeClient
      .channel('tournaments-grid-changes')
      .on('postgres_changes',
        { event: '*', schema: 'public', table: 'tournaments' },
        (payload) => {
          fetchTournaments();
        }
      )
      .subscribe();

    return () => {
      activeClient.removeChannel(channel);
    };
  }, [supabase]);

  // Extract unique games for the filter pill menu
  const availableGames = ['All', ...new Set(tournaments.map(t => t.game).filter(Boolean))];

  // Filter & sort logic
  const filteredAndSortedTournaments = tournaments
    .filter(t => {
      const matchesSearch = (t.title?.toLowerCase().includes(searchQuery.toLowerCase()) || 
                             t.game?.toLowerCase().includes(searchQuery.toLowerCase()));
      const matchesGame = selectedGame === 'All' || t.game === selectedGame;
      return matchesSearch && matchesGame;
    })
    .sort((a, b) => {
      if (sortBy === 'prize') {
        const prizeA = a.coinPrize || 0;
        const prizeB = b.coinPrize || 0;
        return prizeB - prizeA;
      }
      if (sortBy === 'entryFee') {
        const feeA = a.coinEntryFee || 0;
        const feeB = b.coinEntryFee || 0;
        return feeA - feeB; // Cheapest first
      }
      // default: newest first (or descending ID)
      return b.id - a.id;
    });

  // Dynamic game-specific badge colors
  const getGameTheme = (game = "") => {
    const title = game.toLowerCase();
    if (title.includes("dream league") || title.includes("dls") || title.includes("soccer") || title.includes("football")) {
      return {
        bg: "bg-emerald-500/10 border-emerald-500/25",
        text: "text-emerald-400",
        accent: "from-emerald-500/20 to-transparent"
      };
    }
    if (title.includes("fifa") || title.includes("fc 24") || title.includes("fc 25")) {
      return {
        bg: "bg-blue-500/10 border-blue-500/25",
        text: "text-blue-400",
        accent: "from-blue-500/20 to-transparent"
      };
    }
    if (title.includes("pubg") || title.includes("free fire") || title.includes("cod") || title.includes("shooter")) {
      return {
        bg: "bg-rose-500/10 border-rose-500/25",
        text: "text-rose-400",
        accent: "from-rose-500/20 to-transparent"
      };
    }
    // Default fallback theme
    return {
      bg: "bg-amber-500/10 border-amber-500/25",
      text: "text-amber-400",
      accent: "from-amber-500/20 to-transparent"
    };
  };

  return (
    <div className="w-full text-white font-sans selection:bg-amber-500 selection:text-black">
      
      {/* 🛠️ Filters & Search Bar Section */}
      <div className="mb-8 space-y-4">
        <div className="flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center">
          
          {/* Search Box */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-neutral-400 w-4 h-4" />
            <input
              type="text"
              placeholder="Search active tournaments or games..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-neutral-900 border border-neutral-800 focus:border-amber-500/60 rounded-xl pl-10 pr-4 py-2.5 text-sm text-neutral-200 placeholder-neutral-500 outline-none transition-all"
            />
          </div>

          {/* Sort Controller */}
          <div className="flex items-center gap-3 self-end md:self-auto">
            <span className="text-xs text-neutral-400 font-bold uppercase tracking-wider flex items-center gap-1.5">
              <Filter className="w-3.5 h-3.5 text-amber-500" /> Sort By:
            </span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="bg-neutral-900 border border-neutral-800 text-xs text-neutral-200 rounded-lg px-3 py-2 outline-none focus:border-amber-500/50 cursor-pointer transition-all"
            >
              <option value="newest">Newest Listed</option>
              <option value="prize">Highest Prize Pool</option>
              <option value="entryFee">Lowest Entry Fee</option>
            </select>
          </div>
        </div>

        {/* Game Filters Category Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          {availableGames.map((gameName) => {
            const isSelected = selectedGame === gameName;
            return (
              <button
                key={gameName}
                onClick={() => setSelectedGame(gameName)}
                className={`whitespace-nowrap px-4 py-1.5 rounded-full text-xs font-black uppercase tracking-wider transition-all border ${
                  isSelected 
                    ? 'bg-amber-500 border-amber-500 text-black shadow-lg shadow-amber-500/10' 
                    : 'bg-neutral-900/60 hover:bg-neutral-800 border-neutral-800/80 text-neutral-400 hover:text-white'
                }`}
              >
                {gameName === 'All' ? '🎮 ALL GAMES' : gameName}
              </button>
            );
          })}
        </div>
      </div>

      {/* ⌛ Loading State */}
      {loading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map((idx) => (
            <div 
              key={idx} 
              className="bg-neutral-900/40 border border-neutral-800/60 rounded-2xl p-5 h-[210px] flex flex-col justify-between animate-pulse"
            >
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <div className="h-5 bg-neutral-800 rounded w-24"></div>
                  <div className="h-5 bg-neutral-800 rounded-full w-12"></div>
                </div>
                <div className="h-6 bg-neutral-800 rounded w-3/4"></div>
                <div className="h-4 bg-neutral-800 rounded w-1/2"></div>
              </div>
              <div className="pt-4 border-t border-neutral-800/60 flex justify-between">
                <div className="h-4 bg-neutral-800 rounded w-16"></div>
                <div className="h-4 bg-neutral-800 rounded w-20"></div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ⚠️ Database Connection Error Fallback */}
      {!loading && error && (
        <div className="bg-amber-500/5 border border-amber-500/20 rounded-2xl p-6 text-center max-w-xl mx-auto my-6 space-y-4">
          <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
          <h3 className="font-extrabold text-sm uppercase tracking-wider">Tournament Sync Notification</h3>
          <p className="text-xs text-neutral-400 leading-relaxed">
            {error}
          </p>
          <div className="pt-2">
            <button 
              onClick={() => window.location.reload()}
              className="px-4 py-2 bg-neutral-900 border border-neutral-800 rounded-lg text-xs font-bold hover:bg-neutral-800 transition-all text-neutral-200"
            >
              Retry Connection
            </button>
          </div>
        </div>
      )}

      {/* 📭 Empty State */}
      {!loading && !error && filteredAndSortedTournaments.length === 0 && (
        <div className="bg-neutral-900/30 border border-neutral-800/40 rounded-2xl p-12 text-center max-w-lg mx-auto">
          <Gamepad2 className="w-12 h-12 text-neutral-600 mx-auto mb-4" />
          <h3 className="text-sm font-extrabold text-neutral-300 uppercase tracking-wider mb-2">No Tournaments Found</h3>
          <p className="text-xs text-neutral-500 leading-relaxed mb-6">
            There are currently no active '{selectedGame}' matchups listing in this category. Complete registrations or configure a new live contest from the Administrative terminal!
          </p>
        </div>
      )}

      {/* 🎮 Tournament Cards Grid */}
      {!loading && !error && filteredAndSortedTournaments.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredAndSortedTournaments.map((tournament) => {
            const gameTheme = getGameTheme(tournament.game);
            const currentPlayers = tournament.players || 0;
            const maxPlayers = tournament.maxPlayers || 16;
            const fillPercentage = Math.min(100, (currentPlayers / maxPlayers) * 100);

            return (
              <div
                key={tournament.id}
                onClick={() => onSelectTournament?.(tournament)}
                className="group relative bg-neutral-900 hover:bg-neutral-900/90 border border-neutral-800 hover:border-amber-500/45 rounded-2xl p-5 shadow-xl transition-all duration-300 hover:-translate-y-1.5 flex flex-col justify-between overflow-hidden cursor-pointer"
              >
                {/* Visual gradient accent on top left of card for esports premium look */}
                <div className={`absolute top-0 left-0 w-36 h-36 bg-gradient-to-br ${gameTheme.accent} opacity-30 blur-2xl pointer-events-none group-hover:opacity-50 transition-opacity duration-300`}></div>
                
                <div className="space-y-4 relative z-10">
                  
                  {/* Card Header: Game Title & Mode Badge */}
                  <div className="flex justify-between items-start gap-3">
                    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider border ${gameTheme.bg} ${gameTheme.text}`}>
                      <Gamepad2 className="w-3 h-3" />
                      {tournament.game || "General Mode"}
                    </span>
                    <span className="text-[10px] uppercase font-bold tracking-wider text-neutral-500 bg-neutral-950/60 border border-neutral-800/80 px-2 py-0.5 rounded">
                      {tournament.format || "Knockout"}
                    </span>
                  </div>

                  {/* Card Content: Tournament Title & Description */}
                  <div>
                    <h4 className="text-base font-black tracking-tight text-neutral-100 group-hover:text-amber-400 transition-colors duration-200 line-clamp-2">
                      {tournament.title}
                    </h4>
                    {tournament.description && (
                      <p className="text-[11px] text-neutral-400 mt-1.5 line-clamp-2 leading-relaxed">
                        {tournament.description}
                      </p>
                    )}
                  </div>

                  {/* Player Enrollment Status Meter */}
                  <div className="space-y-1.5">
                    <div className="flex justify-between items-center text-[10px]">
                      <span className="text-neutral-400 flex items-center gap-1">
                        <Users className="w-3 h-3 text-neutral-500" /> Bracket Slots
                      </span>
                      <span className="font-extrabold text-neutral-200">
                        {currentPlayers} / <span className="text-neutral-400">{maxPlayers}</span>
                      </span>
                    </div>
                    {/* Capacity progress bar */}
                    <div className="w-full h-1.5 bg-neutral-950 rounded-full overflow-hidden">
                      <div 
                        className={`h-full bg-amber-500 rounded-full transition-all duration-500`}
                        style={{ width: `${fillPercentage}%` }}
                      ></div>
                    </div>
                  </div>

                </div>

                {/* Card Footer: Entry & Prize Pool */}
                <div className="mt-5 pt-4 border-t border-neutral-800/80 flex items-center justify-between relative z-10">
                  
                  {/* Entry Cost info */}
                  <div>
                    <span className="text-[9px] uppercase font-bold text-neutral-500 block">Entry Fee</span>
                    <div className="text-xs font-black text-neutral-200 flex items-center gap-1 mt-0.5">
                      <Coins className="w-3.5 h-3.5 text-neutral-400" />
                      {tournament.coinEntryFee > 0 ? (
                        <span>{tournament.coinEntryFee} <span className="text-[9px] text-neutral-500 font-bold">Coins</span></span>
                      ) : (
                        <span className="text-emerald-400 uppercase tracking-wide">FREE</span>
                      )}
                    </div>
                  </div>

                  {/* Prize Pool reward info */}
                  <div className="text-right">
                    <span className="text-[9px] uppercase font-black text-amber-500 tracking-wider block">Prize Pool</span>
                    <div className="text-sm font-black text-amber-500 flex items-center gap-1 justify-end mt-0.5">
                      <Trophy className="w-4 h-4 text-amber-500" />
                      <span>{tournament.coinPrize || 0} <span className="text-[10px] font-bold">Coins</span></span>
                    </div>
                  </div>

                </div>

                {/* Interactive slide-in arrow on hover */}
                <div className="absolute bottom-3 right-3 opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-all duration-300 text-amber-500 pointer-events-none">
                  <ArrowUpRight className="w-4 h-4" />
                </div>

              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}
