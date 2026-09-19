import React, { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { supabase } from './supabase';
import { 
  Play, Copy, CheckCircle, XCircle, Clock, Users, Shield, ArrowLeft, Share2, Map, Layers, Check
} from 'lucide-react';

export default function MatchLobby() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  
  const [tournament, setTournament] = useState(location.state?.tournament || null);
  const [currentUser, setCurrentUser] = useState('');
  const [isJoined, setIsJoined] = useState(false);
  const [copied, setCopied] = useState(false);
  
  // Real-time players state
  const [players, setPlayers] = useState([]);
  
  // 5 minutes countdown
  const [timeLeft, setTimeLeft] = useState(300);

  // Fetch tournament if not passed in state
  useEffect(() => {
    if (!tournament) {
      const fetchTournament = async () => {
        const { data } = await supabase.from('tournaments').select('*').eq('id', id).single();
        if (data) setTournament(data);
      };
      fetchTournament();
    }
  }, [id, tournament]);

  // Handle Realtime Presence for the Lobby
  useEffect(() => {
    if (!isJoined || !currentUser) return;

    const room = supabase.channel(`lobby:${id}`, {
      config: { presence: { key: currentUser } }
    });

    room
      .on('presence', { event: 'sync' }, () => {
        const state = room.presenceState();
        const activePlayers = [];
        for (const [key, presences] of Object.entries(state)) {
          if (presences.length > 0) {
            activePlayers.push({
              id: key,
              name: key,
              isReady: presences[0].isReady || false,
              isHost: presences[0].isHost || false,
              avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${key}`
            });
          }
        }
        setPlayers(activePlayers);
      })
      .on('broadcast', { event: 'start_match' }, () => {
        alert("The Host has started the match!");
      })
      .subscribe(async (status) => {
        if (status === 'SUBSCRIBED') {
          // Join the room as presence
          await room.track({
            isReady: false,
            isHost: players.length === 0, // First person becomes host
            joinedAt: new Date().toISOString()
          });
        }
      });

    return () => {
      supabase.removeChannel(room);
    };
  }, [id, isJoined, currentUser]); // Removed `players.length` from dependency to avoid re-tracking loop

  // Timer countdown hook
  useEffect(() => {
    if (timeLeft <= 0 || !isJoined) return;
    const interval = setInterval(() => {
      setTimeLeft(prev => prev - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [timeLeft, isJoined]);

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleCopyCode = () => {
    navigator.clipboard.writeText(id);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const toggleReady = async () => {
    const room = supabase.channel(`lobby:${id}`);
    const me = players.find(p => p.id === currentUser);
    if (me) {
      await room.track({
        ...me,
        isReady: !me.isReady
      });
    }
  };

  const handleStartMatch = async () => {
    const room = supabase.channel(`lobby:${id}`);
    await room.send({
      type: 'broadcast',
      event: 'start_match',
      payload: { message: 'Match is starting' }
    });
    alert("Match starting! Notification sent to all players.");
  };

  const handleLeaveMatch = () => {
    navigate('/');
  };

  if (!tournament) {
    return <div className="min-h-screen bg-neutral-950 text-white p-8 flex items-center justify-center">Loading Tournament...</div>;
  }

  // Pre-lobby join screen
  if (!isJoined) {
    return (
      <div className="min-h-screen bg-neutral-950 flex items-center justify-center p-4">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8 max-w-md w-full shadow-2xl">
          <h2 className="text-2xl font-black text-white mb-2">Join Lobby</h2>
          <p className="text-neutral-400 mb-6">{tournament.title}</p>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-neutral-400 uppercase mb-2">Enter Username</label>
              <input 
                type="text" 
                value={currentUser}
                onChange={(e) => setCurrentUser(e.target.value)}
                placeholder="e.g. GamerPro_99"
                className="w-full bg-black/50 border border-neutral-800 rounded-xl px-4 py-3 text-white focus:border-amber-500 outline-none"
              />
            </div>
            <button 
              onClick={() => currentUser && setIsJoined(true)}
              disabled={!currentUser}
              className="w-full bg-amber-500 hover:bg-amber-400 text-black font-black uppercase tracking-wider py-3 rounded-xl disabled:opacity-50 transition-all"
            >
              Enter Match Room
            </button>
            <button 
              onClick={() => navigate('/')}
              className="w-full bg-transparent hover:bg-neutral-800 text-neutral-300 font-bold py-3 rounded-xl transition-all"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    );
  }

  const allReady = players.length > 0 && players.every(p => p.isReady);
  const amIHost = players.find(p => p.id === currentUser)?.isHost || false;
  const amIReady = players.find(p => p.id === currentUser)?.isReady || false;

  return (
    <div className="min-h-screen bg-neutral-950 text-white font-sans antialiased flex flex-col justify-between">
      <header className="px-6 py-4 bg-neutral-900/80 border-b border-neutral-800 backdrop-blur-md sticky top-0 z-50 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button onClick={handleLeaveMatch} className="p-2 hover:bg-neutral-800 rounded-lg text-neutral-400 hover:text-white transition-colors">
            <ArrowLeft size={20} />
          </button>
          <div>
            <span className="text-[10px] uppercase tracking-wider text-amber-500 font-bold">{tournament.game}</span>
            <h1 className="text-base font-extrabold tracking-tight text-neutral-100">{tournament.title}</h1>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 md:p-6 grid grid-cols-1 md:grid-cols-12 gap-6 items-start">
        <section className="md:col-span-7 space-y-6">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 shadow-xl relative overflow-hidden">
            <span className="text-xs uppercase tracking-wider text-neutral-400 font-semibold block mb-2">Match Connection Code</span>
            <div className="flex items-center justify-between gap-4 bg-black/40 border border-neutral-800/80 rounded-xl p-4">
              <span className="text-4xl font-black tracking-widest text-amber-500 select-all font-mono">{id}</span>
              <button onClick={handleCopyCode} className="p-3 bg-neutral-800 hover:bg-amber-500 hover:text-black rounded-lg transition-all">
                {copied ? <Check size={18} /> : <Copy size={18} />}
              </button>
            </div>
            <p className="text-xs text-neutral-400 mt-3 leading-relaxed">
              Share this code with your opponent. Enter this code in the game's local match screen.
            </p>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="bg-neutral-900 border border-neutral-800 p-4 rounded-xl text-center">
              <Map size={18} className="mx-auto mb-2 text-amber-500" />
              <div className="text-[10px] text-neutral-500 font-bold uppercase">Format</div>
              <div className="text-sm font-extrabold">{tournament.format || "Knockout"}</div>
            </div>
            <div className="bg-neutral-900 border border-neutral-800 p-4 rounded-xl text-center">
              <Clock size={18} className="mx-auto mb-2 text-amber-500" />
              <div className="text-[10px] text-neutral-500 font-bold uppercase">Lobby Timer</div>
              <div className="text-sm font-black font-mono text-neutral-100">{formatTime(timeLeft)}</div>
            </div>
            <div className="bg-neutral-900 border border-neutral-800 p-4 rounded-xl text-center">
               <Layers size={18} className="mx-auto mb-2 text-amber-500" />
               <div className="text-[10px] text-neutral-500 font-bold uppercase">Players</div>
               <div className="text-sm font-extrabold">{players.length} / {tournament.maxplayers || tournament.maxPlayers || 2}</div>
            </div>
          </div>
        </section>

        <section className="md:col-span-5 bg-neutral-900 border border-neutral-800 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-neutral-800">
            <div className="flex items-center gap-2">
              <Users size={16} className="text-amber-500" />
              <h2 className="text-sm font-black uppercase tracking-wider text-neutral-200">Lobby Roster</h2>
            </div>
            <span className="text-xs font-bold bg-neutral-800 text-neutral-400 px-2.5 py-1 rounded-full">
              {players.length} Joined
            </span>
          </div>

          <div className="space-y-3 max-h-[320px] overflow-y-auto pr-1">
            {players.map((player) => (
              <div key={player.id} className="flex items-center justify-between p-3 bg-black/30 border border-neutral-800/60 rounded-xl">
                <div className="flex items-center gap-3">
                  <img src={player.avatar} alt={player.name} className="w-9 h-9 rounded-full bg-neutral-800" />
                  <div>
                    <span className="text-sm font-bold text-neutral-100 flex items-center gap-1.5">
                      {player.name} {player.id === currentUser && "(You)"}
                      {player.isHost && <span className="text-[9px] uppercase tracking-wider text-amber-500 bg-amber-500/10 px-1.5 py-0.5 rounded font-black">Host</span>}
                    </span>
                  </div>
                </div>

                <div className="flex items-center">
                  {player.id === currentUser ? (
                     <button 
                       onClick={toggleReady}
                       className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold border transition-all ${
                         amIReady ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-neutral-800 text-neutral-400 border-neutral-800'
                       }`}
                     >
                       {amIReady ? <span>Ready</span> : <span>Click to Ready</span>}
                     </button>
                  ) : (
                    <div className={`px-3 py-1.5 rounded-lg text-xs font-bold border ${player.isReady ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-neutral-800 text-neutral-400 border-neutral-800'}`}>
                      {player.isReady ? 'Ready' : 'Pending'}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>

      <footer className="p-4 bg-neutral-900/95 border-t border-neutral-800 backdrop-blur-md sticky bottom-0 z-50">
        <div className="max-w-4xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className={`w-3.5 h-3.5 rounded-full animate-pulse ${allReady ? 'bg-emerald-500' : 'bg-amber-500'}`}></div>
            <div className="text-xs text-neutral-300">
              {allReady ? <span className="font-semibold text-emerald-400">All players are fully ready to drop!</span> : <span>Waiting for remaining players...</span>}
            </div>
          </div>
          <div className="flex items-center gap-3 w-full sm:w-auto">
            {amIHost && (
              <button 
                onClick={handleStartMatch}
                disabled={!allReady}
                className={`flex-1 sm:flex-initial flex items-center justify-center gap-2 px-8 py-3 rounded-xl text-sm font-black tracking-wide uppercase transition-all shadow-lg active:scale-95 ${
                  allReady ? 'bg-amber-500 hover:bg-amber-400 text-neutral-950 shadow-amber-500/10' : 'bg-neutral-800 text-neutral-600 border border-neutral-800 cursor-not-allowed'
                }`}
              >
                <Play size={16} className="fill-current" />
                <span>Start Match</span>
              </button>
            )}
          </div>
        </div>
      </footer>
    </div>
  );
}
