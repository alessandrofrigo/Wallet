// ========== Utilities ==========

function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        if (container.contains(toast)) {
            container.removeChild(toast);
        }
    }, 3500);
}

function getCsrfToken() {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; XSRF-TOKEN=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return '';
}

async function apiFetch(url, options = {}) {
    options.credentials = 'include';
    if (!options.headers) options.headers = {};
    options.headers['Content-Type'] = 'application/json';
    
    if (options.method && ['POST', 'PUT', 'DELETE'].includes(options.method.toUpperCase())) {
        const csrfToken = getCsrfToken();
        if (csrfToken) {
            options.headers['X-XSRF-TOKEN'] = csrfToken;
        }
    }
    
    const response = await fetch(url, options);

    if (response.status === 401 && !url.includes('/auth/login') && !url.includes('/auth/me')) {
        showView('auth');
        showToast('Sessione scaduta, effettua di nuovo il login', 'error');
        throw new Error('Unauthorized');
    }

    return response;
}

// ========== Gestione Viste ==========

const landingView = document.getElementById('landing-view');
const authView = document.getElementById('auth-view');
const dashboardView = document.getElementById('dashboard-view');
let isLoginMode = true;

function showView(viewName) {
    landingView.style.display = viewName === 'landing' ? 'flex' : 'none';
    authView.style.display = viewName === 'auth' ? 'block' : 'none';
    dashboardView.style.display = viewName === 'dashboard' ? 'flex' : 'none';
}

document.querySelectorAll('.landing-login-trigger').forEach(btn => {
    btn.addEventListener('click', () => {
        setAuthMode(true);
        showView('auth');
    });
});

document.querySelectorAll('.landing-register-trigger').forEach(btn => {
    btn.addEventListener('click', () => {
        setAuthMode(false);
        showView('auth');
    });
});

// ========== Logica Auth ==========

const authForm = document.getElementById('auth-form');
const authSwitchLink = document.getElementById('auth-switch-link');
const authTitle = document.getElementById('auth-title');
const authBtn = document.getElementById('auth-submit-btn');
const grpIdentificativo = document.getElementById('grp-identificativo');
const grpUsername = document.getElementById('grp-username');
const grpEmail = document.getElementById('grp-email');
const authHint = document.getElementById('auth-hint');

function setAuthMode(login) {
    isLoginMode = login;
    authTitle.textContent = isLoginMode ? 'Bentornato!' : 'Crea Account';
    authBtn.textContent = isLoginMode ? 'Accedi' : 'Registrati';
    document.getElementById('auth-switch-text').textContent = isLoginMode ? 'Non hai un account? ' : 'Hai già un account? ';
    authSwitchLink.textContent = isLoginMode ? 'Registrati' : 'Accedi';
    const sub = document.getElementById('auth-subtitle');
    if (sub) sub.textContent = isLoginMode ? 'Accedi per gestire le tue finanze' : 'Crea un account per iniziare';

    // Login: un solo campo identificativo. Registrazione: username ed email, almeno uno dei due.
    grpIdentificativo.hidden = !isLoginMode;
    grpUsername.hidden = isLoginMode;
    grpEmail.hidden = isLoginMode;
    authHint.hidden = isLoginMode;
}

authSwitchLink.addEventListener('click', (e) => {
    e.preventDefault();
    setAuthMode(!isLoginMode);
});

authForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const password = document.getElementById('password').value;

    let endpoint, body;
    if (isLoginMode) {
        const identificativo = document.getElementById('identificativo').value.trim();
        endpoint = '/api/auth/login';
        body = { username: identificativo, password };
    } else {
        const regUsername = document.getElementById('reg-username').value.trim();
        const regEmail = document.getElementById('reg-email').value.trim();
        if (!regUsername && !regEmail) {
            showToast('Inserisci uno username o un\'email', 'error');
            return;
        }
        endpoint = '/api/auth/register';
        body = { username: regUsername || null, email: regEmail || null, password };
    }

    try {
        const response = await apiFetch(endpoint, {
            method: 'POST',
            body: JSON.stringify(body)
        });

        if (response.ok) {
            showToast(isLoginMode ? 'Login effettuato' : 'Registrazione completata! Ora puoi accedere.', 'success');
            if (isLoginMode) {
                checkAuth();
            } else {
                authForm.reset();
                setAuthMode(true);
            }
        } else if (isLoginMode && response.status === 404) {
            // Utente inesistente: non fare login, passa alla registrazione con il valore già compilato
            showToast('Utente non registrato: completa la registrazione', 'error');
            const identificativo = body.username;
            setAuthMode(false);
            if (identificativo.includes('@')) {
                document.getElementById('reg-email').value = identificativo;
                document.getElementById('reg-username').value = '';
            } else {
                document.getElementById('reg-username').value = identificativo;
                document.getElementById('reg-email').value = '';
            }
            const pwd = document.getElementById('password');
            pwd.value = '';
            pwd.focus();
        } else {
            const text = await response.text();
            showToast(text || 'Errore durante l\'operazione', 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('Errore di connessione', 'error');
    }
});

document.getElementById('logout-btn').addEventListener('click', async () => {
    try {
        await apiFetch('/api/auth/logout', { method: 'POST' });
        showView('auth');
        showToast('Logout effettuato', 'success');
    } catch (error) {
        console.error(error);
    }
});

async function checkAuth() {
    try {
        const response = await apiFetch('/api/auth/me');
        if (response.ok) {
            const user = await response.json();
            document.getElementById('welcome-text').textContent = `Ciao, ${user.username || user.email}`;
            showView('dashboard');
            loadDashboardData();
        } else {
            showView('landing');
        }
    } catch (error) {
        showView('landing');
    }
}

// ========== Logica Dashboard ==========

async function loadDashboardData() {
    await loadCategorie();
    await loadFilterCategorie();
    await loadTransazioni();
}

const catSelect = document.getElementById('categoria');
const subcatSelect = document.getElementById('sottocategoria');
const tipoSelect = document.getElementById('tipo');

// Quando si cambia il Tipo, aggiorniamo le categorie visibili
tipoSelect.addEventListener('change', () => {
    loadCategorie();
    // Reset la sottocategoria
    subcatSelect.innerHTML = '<option value="">Seleziona Categoria prima</option>';
    subcatSelect.disabled = true;
});

async function loadCategorie() {
    try {
        const res = await apiFetch('/api/categorie');
        if (res.ok) {
            const categorie = await res.json();
            const tipoCorrente = tipoSelect.value;
            
            catSelect.innerHTML = '<option value="">Seleziona...</option>';
            categorie.forEach(c => {
                // Se l'utente ha scelto ENTRATA, mostra solo ENTRATE
                // Se ha scelto USCITA, mostra tutto TRANNE ENTRATE
                if (tipoCorrente === 'ENTRATA' && c !== 'ENTRATE') return;
                if (tipoCorrente === 'USCITA' && c === 'ENTRATE') return;
                
                const opt = document.createElement('option');
                opt.value = c;
                opt.textContent = c;
                catSelect.appendChild(opt);
            });
        }
    } catch (e) { console.error(e); }
}

async function loadSottocategorie(cat) {
    subcatSelect.innerHTML = '<option value="">Seleziona...</option>';
    if (!cat) {
        subcatSelect.disabled = true;
        return;
    }

    subcatSelect.disabled = false;
    try {
        const res = await apiFetch(`/api/categorie/${cat}/sottocategorie`);
        if (res.ok) {
            const sottocategorie = await res.json();
            sottocategorie.forEach(sc => {
                const opt = document.createElement('option');
                opt.value = sc;
                opt.textContent = sc;
                subcatSelect.appendChild(opt);
            });
        }
    } catch (e) { console.error(e); }
}

catSelect.addEventListener('change', (e) => loadSottocategorie(e.target.value));

// ========== Filtri, Ordinamento e Paginazione Transazioni ==========

const filterCategoria = document.getElementById('filter-categoria');
const filterTipo = document.getElementById('filter-tipo');
const filterDataDa = document.getElementById('filter-data-da');
const filterDataA = document.getElementById('filter-data-a');
const filterTesto = document.getElementById('filter-testo');
const prevPageBtn = document.getElementById('prev-page-btn');
const nextPageBtn = document.getElementById('next-page-btn');
const pageInfo = document.getElementById('page-info');

const PAGE_SIZE = 20;
let currentPage = 0;
let currentSortBy = 'data';
let currentSortDir = 'desc';

async function loadFilterCategorie() {
    try {
        const res = await apiFetch('/api/categorie');
        if (res.ok) {
            const categorie = await res.json();
            filterCategoria.innerHTML = '<option value="">Tutte le categorie</option>';
            categorie.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c;
                opt.textContent = c;
                filterCategoria.appendChild(opt);
            });
        }
    } catch (e) { console.error(e); }
}

function resetPaginaERicarica() {
    currentPage = 0;
    loadTransazioni();
}

[filterCategoria, filterTipo, filterDataDa, filterDataA].forEach(el => {
    el.addEventListener('change', resetPaginaERicarica);
});

let filterTestoTimeout;
filterTesto.addEventListener('input', () => {
    clearTimeout(filterTestoTimeout);
    filterTestoTimeout = setTimeout(resetPaginaERicarica, 400);
});

prevPageBtn.addEventListener('click', () => {
    if (currentPage > 0) {
        currentPage--;
        loadTransazioni();
    }
});

nextPageBtn.addEventListener('click', () => {
    currentPage++;
    loadTransazioni();
});

document.querySelectorAll('#transactions-table th[data-sort]').forEach(th => {
    th.addEventListener('click', () => {
        const colonna = th.dataset.sort;
        if (currentSortBy === colonna) {
            currentSortDir = currentSortDir === 'asc' ? 'desc' : 'asc';
        } else {
            currentSortBy = colonna;
            currentSortDir = 'asc';
        }
        resetPaginaERicarica();
    });
});

function updateSortIndicators() {
    document.querySelectorAll('#transactions-table th[data-sort]').forEach(th => {
        const indicator = th.querySelector('.sort-indicator');
        if (!indicator) return;
        indicator.textContent = th.dataset.sort === currentSortBy ? (currentSortDir === 'asc' ? '▲' : '▼') : '';
    });
}

// ========== Form Transazione (Aggiunta / Modifica) ==========

const transForm = document.getElementById('transaction-form');
const transSubmitBtn = document.getElementById('transaction-submit-btn');
const cancelEditBtn = document.getElementById('cancel-edit-btn');
let editingTransazioneId = null;
let currentTransazioni = [];

function resetTransactionForm() {
    editingTransazioneId = null;
    transForm.reset();
    subcatSelect.disabled = true;
    subcatSelect.innerHTML = '<option value="">Seleziona Categoria prima</option>';
    transSubmitBtn.textContent = 'Aggiungi';
    cancelEditBtn.hidden = true;
    loadCategorie(); // Ricarica le categorie per il tipo di default
}

cancelEditBtn.addEventListener('click', resetTransactionForm);

window.startEditTransazione = async function(id) {
    const t = currentTransazioni.find(x => x.id === id);
    if (!t) return;
    editingTransazioneId = id;

    document.getElementById('tipo').value = t.tipo;
    document.getElementById('descrizione').value = t.descrizione;
    document.getElementById('importo').value = t.importo;
    document.getElementById('data').value = t.data;

    await loadCategorie();
    catSelect.value = t.categoria;
    await loadSottocategorie(t.categoria);
    subcatSelect.value = t.sottocategoria;

    transSubmitBtn.textContent = 'Salva modifiche';
    cancelEditBtn.hidden = false;
    transForm.scrollIntoView({ behavior: 'smooth', block: 'center' });
};

transForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        tipo: document.getElementById('tipo').value,
        descrizione: document.getElementById('descrizione').value,
        importo: parseFloat(document.getElementById('importo').value),
        data: document.getElementById('data').value,
        categoria: document.getElementById('categoria').value,
        sottocategoria: document.getElementById('sottocategoria').value
    };

    const isEditing = editingTransazioneId !== null;
    const url = isEditing ? `/api/transazioni/${editingTransazioneId}` : '/api/transazioni';
    const method = isEditing ? 'PUT' : 'POST';

    try {
        const res = await apiFetch(url, {
            method,
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            const tipoLabel = payload.tipo === 'ENTRATA' ? 'Entrata' : 'Spesa';
            showToast(isEditing ? `${tipoLabel} aggiornata!` : `${tipoLabel} aggiunta!`, 'success');
            resetTransactionForm();
            loadTransazioni();
        } else {
            const text = await res.text();
            showToast(text || 'Errore', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Errore di connessione', 'error');
    }
});

// ========== Caricamento e Rendering Transazioni ==========

async function loadTransazioni() {
    try {
        const params = new URLSearchParams();
        params.set('page', currentPage);
        params.set('size', PAGE_SIZE);
        params.set('sortBy', currentSortBy);
        params.set('sortDir', currentSortDir);
        if (filterCategoria.value) params.set('categoria', filterCategoria.value);
        if (filterTipo.value) params.set('tipo', filterTipo.value);
        if (filterDataDa.value) params.set('dataDa', filterDataDa.value);
        if (filterDataA.value) params.set('dataA', filterDataA.value);
        if (filterTesto.value.trim()) params.set('testo', filterTesto.value.trim());

        const res = await apiFetch(`/api/transazioni?${params.toString()}`);
        if (res.ok) {
            const pagina = await res.json();
            renderTable(pagina.contenuto);
            updateStats(pagina.contenuto);
            updateChart(pagina.contenuto);
            renderPagination(pagina);
            updateSortIndicators();
        }
    } catch (e) { console.error(e); }
}

function renderPagination(pagina) {
    const totalePagine = Math.max(pagina.totalePagine, 1);
    pageInfo.textContent = `Pagina ${pagina.page + 1} di ${totalePagine}`;
    prevPageBtn.disabled = pagina.page <= 0;
    nextPageBtn.disabled = pagina.page >= totalePagine - 1;
}

function renderTable(transazioni) {
    currentTransazioni = transazioni;
    const tbody = document.getElementById('transactions-body');
    tbody.innerHTML = '';

    transazioni.forEach(t => {
        const isEntrata = t.tipo === 'ENTRATA';
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${t.data}</td>
            <td><span class="${isEntrata ? 'badge-entrata' : 'badge-uscita'}">${isEntrata ? '▲ Entrata' : '▼ Uscita'}</span></td>
            <td>${t.descrizione || '-'}</td>
            <td>${t.categoria}</td>
            <td><span class="${isEntrata ? 'amount-income' : 'amount-expense'}">${isEntrata ? '+' : '-'} € ${t.importo.toFixed(2)}</span></td>
            <td>
                <button class="btn-secondary" onclick="startEditTransazione(${t.id})">Modifica</button>
                <button class="btn-danger" onclick="deleteTransazione(${t.id})">Elimina</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function updateStats(transazioni) {
    let totalIncome = 0;
    let totalExpense = 0;
    
    transazioni.forEach(t => {
        if (t.tipo === 'ENTRATA') {
            totalIncome += t.importo;
        } else {
            totalExpense += t.importo;
        }
    });
    
    const balance = totalIncome - totalExpense;
    
    document.getElementById('total-income').textContent = `€ ${totalIncome.toFixed(2)}`;
    document.getElementById('total-expense').textContent = `€ ${totalExpense.toFixed(2)}`;
    
    const balanceEl = document.getElementById('total-balance');
    balanceEl.textContent = `€ ${balance.toFixed(2)}`;
    balanceEl.className = `stat-value ${balance >= 0 ? 'positive' : 'negative'}`;
}

// ========== Chart.js: Grafico a Ciambella ==========

let expensesChartInstance = null;

const chartColors = {
    CIBO: '#f59e0b',
    TRASPORTI: '#6366f1',
    INTRATTENIMENTO: '#c026d3',
    AFFITTO: '#f43f5e',
    BOLLETTE: '#eab308',
    SHOPPING: '#14b8a6',
    SPESA: '#0ea5e9',
    ALTRO: '#64748b'
};

function updateChart(transazioni) {
    // Filtra solo le USCITE per il grafico
    const uscite = transazioni.filter(t => t.tipo !== 'ENTRATA');
    
    // Raggruppa per categoria
    const categorieTotali = {};
    uscite.forEach(t => {
        const cat = t.categoria;
        if (!categorieTotali[cat]) categorieTotali[cat] = 0;
        categorieTotali[cat] += t.importo;
    });
    
    const labels = Object.keys(categorieTotali);
    const data = Object.values(categorieTotali);
    const colors = labels.map(l => chartColors[l] || '#6b7280');
    
    const ctx = document.getElementById('expensesChart').getContext('2d');
    
    // Se il grafico esiste già, distruggilo prima di ricrearlo
    if (expensesChartInstance) {
        expensesChartInstance.destroy();
    }
    
    if (labels.length === 0) {
        // Nessuna spesa: mostra un grafico vuoto/placeholder
        expensesChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Nessuna spesa'],
                datasets: [{
                    data: [1],
                    backgroundColor: ['rgba(100, 116, 139, 0.3)'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: { enabled: false }
                }
            }
        });
        return;
    }
    
    expensesChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors,
                borderColor: 'rgba(15, 10, 26, 0.8)',
                borderWidth: 2,
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        color: '#64748b',
                        font: { family: 'Sora', size: 11 },
                        padding: 12,
                        usePointStyle: true,
                        pointStyleWidth: 8
                    }
                },
                tooltip: {
                    backgroundColor: '#1a1225',
                    titleColor: '#e2e8f0',
                    bodyColor: '#64748b',
                    borderColor: '#2d1f3d',
                    borderWidth: 1,
                    padding: 12,
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const pct = ((context.raw / total) * 100).toFixed(1);
                            return ` € ${context.raw.toFixed(2)} (${pct}%)`;
                        }
                    }
                }
            }
        }
    });
}

// ========== Profilo Utente ==========

const profileBtn = document.getElementById('profile-btn');
const profileModalOverlay = document.getElementById('profile-modal-overlay');
const profileModalClose = document.getElementById('profile-modal-close');
const profileInfoForm = document.getElementById('profile-info-form');
const profilePasswordForm = document.getElementById('profile-password-form');
const profileDeleteForm = document.getElementById('profile-delete-form');

async function openProfileModal() {
    try {
        const res = await apiFetch('/api/auth/me');
        if (res.ok) {
            const user = await res.json();
            document.getElementById('profile-username').value = user.username || '';
            document.getElementById('profile-email').value = user.email || '';
        }
    } catch (e) { console.error(e); }
    profileModalOverlay.hidden = false;
}

function closeProfileModal() {
    profileModalOverlay.hidden = true;
    profilePasswordForm.reset();
    profileDeleteForm.reset();
}

profileBtn.addEventListener('click', openProfileModal);
profileModalClose.addEventListener('click', closeProfileModal);
profileModalOverlay.addEventListener('click', (e) => {
    if (e.target === profileModalOverlay) closeProfileModal();
});

profileInfoForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        username: document.getElementById('profile-username').value.trim() || null,
        email: document.getElementById('profile-email').value.trim() || null
    };

    try {
        const res = await apiFetch('/api/profilo', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        const text = await res.text();
        if (res.ok) {
            showToast('Profilo aggiornato', 'success');
            checkAuth();
        } else {
            showToast(text || 'Errore durante l\'aggiornamento', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Errore di connessione', 'error');
    }
});

profilePasswordForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        vecchiaPassword: document.getElementById('profile-current-password').value,
        nuovaPassword: document.getElementById('profile-new-password').value
    };

    try {
        const res = await apiFetch('/api/profilo/password', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        const text = await res.text();
        if (res.ok) {
            showToast('Password aggiornata', 'success');
            profilePasswordForm.reset();
        } else {
            showToast(text || 'Errore durante il cambio password', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Errore di connessione', 'error');
    }
});

profileDeleteForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!confirm('Sei sicuro di voler eliminare il tuo account? L\'azione è irreversibile.')) return;

    const payload = {
        password: document.getElementById('profile-delete-password').value
    };

    try {
        const res = await apiFetch('/api/profilo', {
            method: 'DELETE',
            body: JSON.stringify(payload)
        });
        const text = await res.text();
        if (res.ok) {
            closeProfileModal();
            showToast('Account eliminato', 'success');
            showView('landing');
        } else {
            showToast(text || 'Errore durante l\'eliminazione', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Errore di connessione', 'error');
    }
});

// ========== Eliminazione ==========

window.deleteTransazione = async function(id) {
    if (!confirm('Sei sicuro di voler eliminare questa transazione?')) return;
    try {
        const res = await apiFetch(`/api/transazioni/${id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('Eliminata', 'success');
            loadTransazioni();
        }
    } catch (e) { console.error(e); }
}

// ========== Inizializzazione ==========

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
});
