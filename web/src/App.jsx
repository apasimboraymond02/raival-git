import React from 'react'
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom'
import ActiveTournamentsGrid from './ActiveTournamentsGrid'
import MatchLobby from './MatchLobby'
import { supabase } from './supabase'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<TournamentsPage />} />
        <Route path="/lobby/:id" element={<MatchLobby />} />
      </Routes>
    </Router>
  )
}

function TournamentsPage() {
  const navigate = useNavigate();

  const handleSelectTournament = (tournament) => {
    // Navigate to the lobby when a tournament is clicked
    navigate(`/lobby/${tournament.id}`, { state: { tournament } });
  }

  return (
    <div className="min-h-screen bg-raival-bg p-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-black text-white mb-2 uppercase tracking-wider">Raival Tournaments</h1>
        <p className="text-neutral-400 mb-8">Select an active tournament to join the match lobby.</p>
        
        <ActiveTournamentsGrid 
          supabase={supabase} 
          onSelectTournament={handleSelectTournament} 
        />
      </div>
    </div>
  )
}

export default App
