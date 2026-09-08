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

const authView = document.getElementById('auth-view');
const dashboardView = document.getElementById('dashboard-view');
let isLoginMode = true;

function showView(viewName) {
    if (viewName === 'auth') {
        authView.style.display = 'block';
        dashboardView.style.display = 'none';
    } else {
        authView.style.display = 'none';
        dashboardView.style.display = 'flex';
    }
}

// ========== Logica Auth ==========

const authForm = document.getElementById('auth-form');
const authSwitchLink = document.getElementById('auth-switch-link');
const authTitle = document.getElementById('auth-title');
const authBtn = document.getElementById('auth-submit-btn');

function setAuthMode(login) {
    isLoginMode = login;
    authTitle.textContent = isLoginMode ? 'Bentornato!' : 'Crea Account';
    authBtn.textContent = isLoginMode ? 'Accedi' : 'Registrati';
    document.getElementById('auth-switch-text').textContent = isLoginMode ? 'Non hai un account? ' : 'Hai già un account? ';
    authSwitchLink.textContent = isLoginMode ? 'Registrati' : 'Accedi';
    const sub = document.getElementById('auth-subtitle');
    if (sub) sub.textContent = isLoginMode ? 'Accedi per gestire le tue finanze' : 'Crea un account per iniziare';
}

authSwitchLink.addEventListener('click', (e) => {
    e.preventDefault();
    setAuthMode(!isLoginMode);
});

authForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    const endpoint = isLoginMode ? '/api/auth/login' : '/api/auth/register';
    
    try {
        const response = await apiFetch(endpoint, {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
        
        if (response.ok) {
            showToast(isLoginMode ? 'Login effettuato' : 'Registrazione completata! Ora puoi accedere.', 'success');
            if (isLoginMode) {
                checkAuth();
            } else {
                setAuthMode(true);
            }
        } else if (isLoginMode && response.status === 404) {
            // Utente inesistente: non fare login, passa alla registrazione col nome già compilato
            showToast('Utente non registrato: completa la registrazione', 'error');
            setAuthMode(false);
            document.getElementById('username').value = username;
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
            document.getElementById('welcome-text').textContent = `Ciao, ${user.username}`;
            showView('dashboard');
            loadDashboardData();
        } else {
            showView('auth');
        }
    } catch (error) {
        showView('auth');
    }
}

// ========== Logica Dashboard ==========

async function loadDashboardData() {
    await loadCategorie();
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

catSelect.addEventListener('change', async (e) => {
    const cat = e.target.value;
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
});

// ========== Form Transazione ==========

const transForm = document.getElementById('transaction-form');
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
    
    try {
        const res = await apiFetch('/api/transazioni', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        
        if (res.ok) {
            const tipoLabel = payload.tipo === 'ENTRATA' ? 'Entrata' : 'Spesa';
            showToast(`${tipoLabel} aggiunta!`, 'success');
            transForm.reset();
            subcatSelect.disabled = true;
            loadCategorie(); // Ricarica le categorie per il tipo di default
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
        const res = await apiFetch('/api/transazioni?size=50');
        if (res.ok) {
            const transazioni = await res.json();
            renderTable(transazioni);
            updateStats(transazioni);
            updateChart(transazioni);
        }
    } catch (e) { console.error(e); }
}

function renderTable(transazioni) {
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
            <td><button class="btn-danger" onclick="deleteTransazione(${t.id})">Elimina</button></td>
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
    CIBO: '#f97316',
    TRASPORTI: '#3b82f6',
    INTRATTENIMENTO: '#a855f7',
    AFFITTO: '#ec4899',
    BOLLETTE: '#eab308',
    SHOPPING: '#14b8a6',
    SPESA: '#06b6d4',
    ALTRO: '#6b7280'
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
                borderColor: 'rgba(15, 23, 42, 0.8)',
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
                        color: '#94a3b8',
                        font: { family: 'Inter', size: 11 },
                        padding: 12,
                        usePointStyle: true,
                        pointStyleWidth: 8
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(30, 41, 59, 0.95)',
                    titleColor: '#f8fafc',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
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
