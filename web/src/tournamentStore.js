/**
 * tournamentStore.js
 * Comprehensive Tournament Data Structure & Store Manager
 * Manages esports tournament information including title, game, entry fee, max number of gamers, prize pool, and active registrations.
 */

class TournamentStore {
    constructor(initialData = []) {
        this.tournaments = [];
        this.listeners = [];
        this.storageKey = 'raival_tournaments_store_v1';

        // Load stored tournaments or populate default seed dataset
        this.init(initialData);
    }

    /**
     * Initialize store with local storage cache or provided initial data
     */
    init(initialData = []) {
        try {
            if (typeof localStorage !== 'undefined') {
                const cached = localStorage.getItem(this.storageKey);
                if (cached) {
                    this.tournaments = JSON.parse(cached);
                    return;
                }
            }
        } catch (e) {
            console.warn('TournamentStore: Local storage read failed, using defaults.', e);
        }

        // Seed default tournament data structure
        const defaultTournaments = initialData.length > 0 ? initialData : [
            {
                id: 101,
                title: "Dream League Ghana Masters",
                game: "Dream League Soccer",
                entryFee: 25,
                maxGamers: 16,
                prizePool: 250,
                gamers: [
                    { id: 1, name: "Akwasi_Gamer", inGameName: "Akwasi DLS", registeredAt: "2026-07-20T10:00:00Z" },
                    { id: 2, name: "FireStriker", inGameName: "Fire DLS", registeredAt: "2026-07-20T11:30:00Z" }
                ],
                status: "Open", // 'Open', 'In Progress', 'Completed', 'Cancelled'
                date: "2026-07-25",
                time: "18:00",
                organizer: "Admin",
                format: "Knockout",
                description: "Premier Ghana DLS tournament with automatic live bracket updates and instant payouts."
            },
            {
                id: 102,
                title: "Accra Esports Championship",
                game: "eFootball 2026",
                entryFee: 50,
                maxGamers: 32,
                prizePool: 500,
                gamers: [
                    { id: 3, name: "Kofi_Pro", inGameName: "Kofi PES", registeredAt: "2026-07-21T09:15:00Z" }
                ],
                status: "Open",
                date: "2026-07-26",
                time: "19:30",
                organizer: "Admin",
                format: "Knockout",
                description: "Flagship 32-player eFootball console & mobile event."
            },
            {
                id: 103,
                title: "FC Mobile Kumasi Clash",
                game: "FC Mobile",
                entryFee: 15,
                maxGamers: 8,
                prizePool: 150,
                gamers: [],
                status: "Open",
                date: "2026-07-28",
                time: "16:00",
                organizer: "Admin",
                format: "Knockout",
                description: "Fast-paced 8-player FC Mobile tournament hosted live for Ashanti gamers."
            },
            {
                id: 104,
                title: "CODM 1v1 Fast Duel",
                game: "Call of Duty: Mobile",
                entryFee: 10,
                maxGamers: 2,
                prizePool: 100,
                gamers: [
                    { id: 4, name: "SniperPro", inGameName: "Sniper_GH", registeredAt: "2026-07-22T08:00:00Z" }
                ],
                status: "Open",
                date: "2026-07-29",
                time: "20:00",
                organizer: "Admin",
                format: "Knockout",
                description: "High-stakes 2-player 1v1 fast showdown."
            },
            {
                id: 105,
                title: "DLS 4-Player Semi Showdown",
                game: "Dream League Soccer",
                entryFee: 20,
                maxGamers: 4,
                prizePool: 200,
                gamers: [],
                status: "Open",
                date: "2026-07-30",
                time: "17:00",
                organizer: "Admin",
                format: "Knockout",
                description: "Quick 4-player knockout tournament starting directly at the Semi-Finals!"
            }
        ];

        this.tournaments = defaultTournaments;
        this.persist();
    }

    /**
     * Save current store state to local storage
     */
    persist() {
        try {
            if (typeof localStorage !== 'undefined') {
                localStorage.setItem(this.storageKey, JSON.stringify(this.tournaments));
            }
        } catch (e) {
            console.warn('TournamentStore: Local storage write failed.', e);
        }
        this.notifyListeners();
    }

    /**
     * Subscribe to tournament store updates
     */
    subscribe(listener) {
        if (typeof listener === 'function') {
            this.listeners.push(listener);
        }
        return () => {
            this.listeners = this.listeners.filter(l => l !== listener);
        };
    }

    notifyListeners() {
        this.listeners.forEach(listener => {
            try {
                listener(this.getAllTournaments());
            } catch (e) {
                console.error('TournamentStore listener error:', e);
            }
        });
    }

    /**
     * Get all tournaments
     */
    getAllTournaments() {
        return [...this.tournaments];
    }

    /**
     * Get tournament by ID
     */
    getTournamentById(id) {
        const numericId = Number(id);
        return this.tournaments.find(t => t.id === numericId || t.id === id) || null;
    }

    /**
     * Calculate total prize pool based on entry fee and participant slots
     * @param {number} entryFee - Entry fee per gamer
     * @param {number} participantSlots - Total participant capacity (e.g. 2, 4, 8, 16)
     * @param {number} multiplier - House multiplier (default 1.0 = 100% payout)
     * @returns {number} Calculated total prize pool
     */
    calculatePrizePool(entryFee, participantSlots, multiplier = 1.0) {
        const fee = Math.max(0, Number(entryFee) || 0);
        const slots = Math.max(1, Number(participantSlots) || 16);
        return Math.round(fee * slots * multiplier);
    }

    /**
     * Add a new tournament
     * Required attributes: title, game, entryFee, maxGamers, prizePool
     */
    addTournament({ title, game, entryFee, maxGamers, prizePool, date = '', time = '', format = 'Knockout', description = '', organizer = 'Admin' }) {
        if (!title || !game) {
            throw new Error("Tournament 'title' and 'game' are required fields.");
        }

        const fee = Math.max(0, Number(entryFee) || 0);
        const slots = Math.max(2, Number(maxGamers) || 16);
        const autoPrize = this.calculatePrizePool(fee, slots);

        const newId = Math.floor(100000 + Math.random() * 900000);
        const newTournament = {
            id: newId,
            title: String(title).trim(),
            game: String(game).trim(),
            entryFee: fee,
            maxGamers: slots,
            prizePool: prizePool !== undefined && prizePool !== null && prizePool !== '' ? Math.max(0, Number(prizePool)) : autoPrize,
            gamers: [],
            status: "Open",
            date: date || new Date().toISOString().split('T')[0],
            time: time || "18:00",
            organizer: organizer,
            format: format,
            description: description || `Competitive ${slots}-player tournament.`
        };

        this.tournaments.unshift(newTournament);
        this.persist();
        return newTournament;
    }

    /**
     * Update existing tournament
     */
    updateTournament(id, updatedFields) {
        const numericId = Number(id);
        const index = this.tournaments.findIndex(t => t.id === numericId || t.id === id);
        
        if (index === -1) {
            throw new Error(`Tournament with ID ${id} not found.`);
        }

        const current = this.tournaments[index];
        this.tournaments[index] = {
            ...current,
            ...updatedFields,
            id: current.id, // Immutable ID
            entryFee: updatedFields.entryFee !== undefined ? Math.max(0, Number(updatedFields.entryFee)) : current.entryFee,
            maxGamers: updatedFields.maxGamers !== undefined ? Math.max(2, Number(updatedFields.maxGamers)) : current.maxGamers,
            prizePool: updatedFields.prizePool !== undefined ? Math.max(0, Number(updatedFields.prizePool)) : current.prizePool
        };

        this.persist();
        return this.tournaments[index];
    }

    /**
     * Delete a tournament by ID
     */
    deleteTournament(id) {
        const numericId = Number(id);
        const initialCount = this.tournaments.length;
        this.tournaments = this.tournaments.filter(t => t.id !== numericId && t.id !== id);

        if (this.tournaments.length < initialCount) {
            this.persist();
            return true;
        }
        return false;
    }

    /**
     * Register a gamer to a tournament
     */
    registerGamer(tournamentId, { gamerId, gamerName, inGameName }) {
        const tournament = this.getTournamentById(tournamentId);
        if (!tournament) {
            throw new Error(`Tournament #${tournamentId} does not exist.`);
        }

        if (tournament.gamers.length >= tournament.maxGamers) {
            throw new Error(`Tournament "${tournament.title}" has reached max gamers limit (${tournament.maxGamers}).`);
        }

        const existing = tournament.gamers.find(g => g.id === gamerId || g.name === gamerName);
        if (existing) {
            throw new Error(`Gamer "${gamerName}" is already registered in this tournament.`);
        }

        const gamerRecord = {
            id: gamerId || Math.floor(Math.random() * 9000) + 1000,
            name: gamerName || "Anonymous Gamer",
            inGameName: inGameName || gamerName || "GamerPro",
            registeredAt: new Date().toISOString()
        };

        tournament.gamers.push(gamerRecord);
        this.persist();
        return gamerRecord;
    }

    /**
     * Remove a gamer from a tournament
     */
    removeGamer(tournamentId, gamerId) {
        const tournament = this.getTournamentById(tournamentId);
        if (!tournament) return false;

        const initialLength = tournament.gamers.length;
        tournament.gamers = tournament.gamers.filter(g => g.id !== gamerId && g.name !== gamerId);

        if (tournament.gamers.length < initialLength) {
            this.persist();
            return true;
        }
        return false;
    }

    /**
     * Filter tournaments by game title
     */
    filterByGame(gameName) {
        if (!gameName) return this.getAllTournaments();
        const search = gameName.toLowerCase();
        return this.tournaments.filter(t => t.game.toLowerCase().includes(search));
    }

    /**
     * Filter tournaments by status ('Open', 'In Progress', 'Completed', etc.)
     */
    filterByStatus(status) {
        if (!status) return this.getAllTournaments();
        return this.tournaments.filter(t => t.status.toLowerCase() === status.toLowerCase());
    }

    /**
     * Get aggregate statistics across all tournaments
     */
    getStats() {
        const totalTournaments = this.tournaments.length;
        const totalPrizePool = this.tournaments.reduce((sum, t) => sum + (Number(t.prizePool) || 0), 0);
        const totalGamersRegistered = this.tournaments.reduce((sum, t) => sum + (t.gamers ? t.gamers.length : 0), 0);
        const totalMaxGamersCapacity = this.tournaments.reduce((sum, t) => sum + (Number(t.maxGamers) || 0), 0);

        return {
            totalTournaments,
            totalPrizePool,
            totalGamersRegistered,
            totalMaxGamersCapacity
        };
    }

    /**
     * Export dataset to JSON string
     */
    exportJSON() {
        return JSON.stringify(this.tournaments, null, 2);
    }

    /**
     * Import dataset from JSON
     */
    importJSON(jsonString) {
        try {
            const parsed = JSON.parse(jsonString);
            if (Array.isArray(parsed)) {
                this.tournaments = parsed;
                this.persist();
                return true;
            }
        } catch (e) {
            console.error("Invalid JSON format for TournamentStore import:", e);
        }
        return false;
    }

    /**
     * Renders a fully interactive 'Create Tournament' form into a specified DOM container.
     * Features dynamic participant slot selection (2, 4, 8, 16, 32) and real-time auto-calculation of the total prize pool based on entry fee * slots.
     * @param {string|HTMLElement} container - DOM element ID or element instance
     * @param {Function} [onSubmitCallback] - Optional callback triggered when a tournament is created
     */
    renderCreateTournamentForm(container, onSubmitCallback) {
        const target = typeof container === 'string' ? document.getElementById(container) : container;
        if (!target) {
            console.warn(`renderCreateTournamentForm: Container element "${container}" not found.`);
            return null;
        }

        const formId = `form_create_t_${Math.floor(Math.random() * 10000)}`;

        target.innerHTML = `
            <div class="ts-create-form-wrapper bg-black/60 border border-white/10 rounded-2xl p-6 shadow-2xl backdrop-blur-md">
                <div class="flex items-center justify-between border-b border-white/10 pb-4 mb-5">
                    <h3 class="text-base font-black text-white uppercase tracking-wider flex items-center gap-2">
                        <span class="text-raival-primary font-bold">🏆</span> Create Tournament
                    </h3>
                    <span class="text-[11px] font-bold uppercase tracking-widest text-raival-primary bg-raival-primary/10 border border-raival-primary/30 px-3 py-1 rounded-full">
                        Dynamic Auto-Prize Engine
                    </span>
                </div>

                <form id="${formId}" class="grid grid-cols-1 md:grid-cols-3 gap-5">
                    <div class="md:col-span-3">
                        <label class="block text-xs font-bold text-neutral-300 uppercase tracking-wide mb-1.5">Tournament Title</label>
                        <input type="text" name="title" required placeholder="e.g. Dream League Ghana Masters" 
                            class="w-full bg-raival-surfaceLight border border-white/10 rounded-xl p-3 text-sm text-white focus:outline-none focus:border-raival-primary transition-all">
                    </div>

                    <div>
                        <label class="block text-xs font-bold text-neutral-300 uppercase tracking-wide mb-1.5">Game Platform</label>
                        <select name="game" class="w-full bg-raival-surfaceLight border border-white/10 rounded-xl p-3 text-sm text-white focus:outline-none focus:border-raival-primary transition-all">
                            <option value="Dream League Soccer">Dream League Soccer</option>
                            <option value="eFootball 2026">eFootball 2026</option>
                            <option value="FC Mobile">FC Mobile</option>
                            <option value="Call of Duty: Mobile">Call of Duty: Mobile</option>
                        </select>
                    </div>

                    <div>
                        <label class="block text-xs font-bold text-neutral-300 uppercase tracking-wide mb-1.5">Entry Fee (GHS / Coins)</label>
                        <input type="number" name="entryFee" min="0" value="25" required placeholder="25" 
                            class="ts-input-fee w-full bg-raival-surfaceLight border border-white/10 rounded-xl p-3 text-sm text-white font-bold focus:outline-none focus:border-raival-primary transition-all">
                    </div>

                    <div>
                        <label class="block text-xs font-bold text-neutral-300 uppercase tracking-wide mb-1.5">Participant Slots</label>
                        <select name="maxGamers" class="ts-select-slots w-full bg-raival-surfaceLight border border-white/10 rounded-xl p-3 text-sm text-white font-bold focus:outline-none focus:border-raival-primary transition-all">
                            <option value="2">2 Players (Heads Up 1v1)</option>
                            <option value="4">4 Players (Semi-Finals start)</option>
                            <option value="8">8 Players (Quarter-Finals start)</option>
                            <option value="16" selected>16 Players (Round of 16 start)</option>
                            <option value="32">32 Players (Round of 32 start)</option>
                        </select>
                    </div>

                    <div class="md:col-span-3 bg-gradient-to-r from-red-950/40 via-yellow-900/30 to-amber-950/40 border border-yellow-500/30 rounded-xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
                        <div>
                            <div class="text-[11px] font-black uppercase text-yellow-400 tracking-wider">Calculated Total Prize Pool</div>
                            <div class="text-xs text-neutral-300">Auto-calculated: <span class="font-bold text-white">Entry Fee</span> &times; <span class="font-bold text-white">Participant Slots</span></div>
                        </div>
                        <div class="flex items-center gap-2 bg-black/60 border border-yellow-500/40 px-4 py-2 rounded-xl">
                            <span class="text-xl font-black text-yellow-400 ts-prize-display">400 Coins</span>
                            <input type="hidden" name="prizePool" class="ts-input-prize" value="400">
                        </div>
                    </div>

                    <div>
                        <label class="block text-xs font-bold text-neutral-300 uppercase tracking-wide mb-1.5">Date</label>
                        <input type="date" name="date" required value="${new Date().toISOString().split('T')[0]}" 
                            class="w-full bg-raival-surfaceLight border border-white/10 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-raival-primary transition-all">
                    </div>

                    <div>
                        <label class="block text-xs font-bold text-neutral-300 uppercase tracking-wide mb-1.5">Time</label>
                        <input type="time" name="time" required value="18:00" 
                            class="w-full bg-raival-surfaceLight border border-white/10 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-raival-primary transition-all">
                    </div>

                    <div>
                        <label class="block text-xs font-bold text-neutral-300 uppercase tracking-wide mb-1.5">Match Format</label>
                        <select name="format" class="w-full bg-raival-surfaceLight border border-white/10 rounded-xl p-3 text-sm text-white focus:outline-none focus:border-raival-primary transition-all">
                            <option value="Knockout">Knockout Tournament Bracket (Single Elimination)</option>
                            <option value="League">EPL-style Single League Group</option>
                            <option value="UCL">UCL Styled Group & Knockouts</option>
                        </select>
                    </div>

                    <div class="md:col-span-3 text-right pt-2">
                        <button type="submit" class="w-full sm:w-auto bg-gradient-to-r from-yellow-500 to-amber-600 hover:from-yellow-400 hover:to-amber-500 text-black font-black uppercase text-xs tracking-wider px-8 py-3.5 rounded-xl shadow-lg hover:brightness-110 active:scale-95 transition-all">
                            ⚡ Launch & Create Tournament
                        </button>
                    </div>
                </form>
            </div>
        `;

        const formEl = document.getElementById(formId);
        const feeInput = formEl.querySelector('.ts-input-fee');
        const slotsSelect = formEl.querySelector('.ts-select-slots');
        const prizeDisplay = formEl.querySelector('.ts-prize-display');
        const prizeHiddenInput = formEl.querySelector('.ts-input-prize');

        const recalculatePrize = () => {
            const fee = Math.max(0, parseFloat(feeInput.value) || 0);
            const slots = Math.max(1, parseInt(slotsSelect.value) || 16);
            const calculated = this.calculatePrizePool(fee, slots);
            prizeDisplay.textContent = `${calculated} Coins / GHS`;
            prizeHiddenInput.value = calculated;
        };

        // Attach dynamic live prize listeners on fee input and slots change
        feeInput.addEventListener('input', recalculatePrize);
        feeInput.addEventListener('change', recalculatePrize);
        slotsSelect.addEventListener('change', recalculatePrize);

        // Initial calculation call
        recalculatePrize();

        // Form Submit
        formEl.addEventListener('submit', (e) => {
            e.preventDefault();
            const formData = new FormData(formEl);
            const title = formData.get('title');
            const game = formData.get('game');
            const entryFee = parseFloat(formData.get('entryFee')) || 0;
            const maxGamers = parseInt(formData.get('maxGamers')) || 16;
            const prizePool = parseFloat(formData.get('prizePool')) || this.calculatePrizePool(entryFee, maxGamers);
            const date = formData.get('date');
            const time = formData.get('time');
            const format = formData.get('format');

            try {
                const createdTournament = this.addTournament({
                    title,
                    game,
                    entryFee,
                    maxGamers,
                    prizePool,
                    date,
                    time,
                    format
                });

                formEl.reset();
                recalculatePrize();

                if (typeof onSubmitCallback === 'function') {
                    onSubmitCallback(createdTournament);
                }
            } catch (err) {
                alert(`Error creating tournament: ${err.message}`);
            }
        });

        return formEl;
    }
}

// Instantiate global singleton store
const tournamentStore = new TournamentStore();

// Export for ES modules, CommonJS, and Browser Window environments
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { TournamentStore, tournamentStore };
}
if (typeof window !== 'undefined') {
    window.TournamentStore = TournamentStore;
    window.tournamentStore = tournamentStore;
}
