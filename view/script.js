const API = 'http://localhost:8080/api';

let usuarioAtual = null;
let todasTarefas = [];
let filtroAtual  = 'todas';

document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.form-panel').forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById(tab.dataset.tab + '-form').classList.add('active');
        document.getElementById('login-error').classList.add('hidden');
        document.getElementById('register-error').classList.add('hidden');
    });
});

document.getElementById('nova-desc').addEventListener('keydown', e => { if (e.key === 'Enter') adicionarTarefa(); });
document.getElementById('edit-desc').addEventListener('keydown', e => { if (e.key === 'Enter') salvarEdicao(); });

async function fazerLogin() {
    const email = document.getElementById('login-email').value.trim();
    const senha = document.getElementById('login-senha').value;
    const erro  = document.getElementById('login-error');

    if (!email || !senha) { showError(erro, 'preencha todos os campos.'); return; }

    try {
        const res  = await post('/login', { email, senha });
        const data = await res.json();
        if (!res.ok) { showError(erro, data.erro || 'erro ao fazer login.'); return; }
        usuarioAtual = data;
        entrarNoApp();
    } catch (e) {
        showError(erro, 'não foi possível conectar ao servidor.');
    }
}

async function fazerCadastro() {
    const nome  = document.getElementById('reg-nome').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const senha = document.getElementById('reg-senha').value;
    const erro  = document.getElementById('register-error');

    if (!nome || !email || !senha) { showError(erro, 'preencha todos os campos.'); return; }
    if (senha.length < 4) { showError(erro, 'senha muito curta (mín. 4 caracteres).'); return; }

    try {
        const res  = await post('/register', { nome, email, senha });
        const data = await res.json();
        if (!res.ok) { showError(erro, data.erro || 'erro ao criar conta.'); return; }
        usuarioAtual = data;
        entrarNoApp();
    } catch (e) {
        showError(erro, 'não foi possível conectar ao servidor.');
    }
}

function fazerLogout() {
    usuarioAtual = null;
    todasTarefas = [];
    irParaTela('auth-screen');
    document.getElementById('login-email').value = '';
    document.getElementById('login-senha').value = '';
}

async function entrarNoApp() {
    document.getElementById('user-greeting').textContent = `olá, ${usuarioAtual.nome}`;
    await carregarTarefas();
    irParaTela('app-screen');
}

async function carregarTarefas() {
    try {
        const res  = await fetch(`${API}/tarefas?idUsuario=${usuarioAtual.id}`);
        const data = await res.json();
        todasTarefas = data.filter(t => t.ativo);
        renderTarefas();
    } catch (e) {
        console.error('Erro ao carregar tarefas', e);
    }
}

async function adicionarTarefa() {
    const descricao = document.getElementById('nova-desc').value.trim();
    const dia = parseInt(document.getElementById('nova-dia').value)  || 0;
    const mes = parseInt(document.getElementById('nova-mes').value)  || 0;
    const ano = parseInt(document.getElementById('nova-ano').value)  || 0;

    if (!descricao) { alert('escreva uma descrição.'); return; }

    try {
        const res = await post('/tarefas', { idUsuario: usuarioAtual.id, descricao, dia, mes, ano });
        if (res.ok) {
            document.getElementById('nova-desc').value = '';
            document.getElementById('nova-dia').value  = '';
            document.getElementById('nova-mes').value  = '';
            document.getElementById('nova-ano').value  = '2026';
            await carregarTarefas();
        }
    } catch (e) {
        console.error('Erro ao adicionar tarefa', e);
    }
}

async function toggleConcluida(id) {
    try {
        await post('/tarefas/concluir', { id });
        await carregarTarefas();
    } catch (e) {
        console.error('Erro ao atualizar tarefa', e);
    }
}

function abrirEdicao(id) {
    const tarefa = todasTarefas.find(t => t.id === id);
    if (!tarefa) return;
    document.getElementById('edit-id').value   = id;
    document.getElementById('edit-desc').value = tarefa.descricao;
    document.getElementById('modal-overlay').classList.remove('hidden');
    setTimeout(() => document.getElementById('edit-desc').focus(), 100);
}

async function salvarEdicao() {
    const id        = parseInt(document.getElementById('edit-id').value);
    const descricao = document.getElementById('edit-desc').value.trim();
    if (!descricao) return;

    try {
        await post('/tarefas/editar', { id, descricao });
        fecharModal();
        await carregarTarefas();
    } catch (e) {
        console.error('Erro ao editar tarefa', e);
    }
}

async function deletarTarefa(id) {
    if (!confirm('deletar esta tarefa?')) return;
    try {
        await post('/tarefas/deletar', { id });
        await carregarTarefas();
    } catch (e) {
        console.error('Erro ao deletar tarefa', e);
    }
}

function fecharModal() {
    document.getElementById('modal-overlay').classList.add('hidden');
}

function setFiltro(btn, filtro) {
    document.querySelectorAll('.filter').forEach(f => f.classList.remove('active'));
    btn.classList.add('active');
    filtroAtual = filtro;
    renderTarefas();
}

function renderTarefas() {
    const lista = document.getElementById('task-list');
    const empty = document.getElementById('empty-msg');

    let visiveis = todasTarefas;
    if (filtroAtual === 'pendentes')  visiveis = todasTarefas.filter(t => !t.concluida);
    if (filtroAtual === 'concluidas') visiveis = todasTarefas.filter(t =>  t.concluida);

    document.getElementById('stat-total').textContent   = todasTarefas.length;
    document.getElementById('stat-pending').textContent = todasTarefas.filter(t => !t.concluida).length;
    document.getElementById('stat-done').textContent    = todasTarefas.filter(t =>  t.concluida).length;

    lista.innerHTML = '';

    if (visiveis.length === 0) { empty.classList.remove('hidden'); return; }
    empty.classList.add('hidden');

    visiveis.forEach(t => {
        const li = document.createElement('li');
        li.className = 'task-item' + (t.concluida ? ' done' : '');

        const dataStr = (t.dia && t.mes && t.ano)
            ? `${String(t.dia).padStart(2,'0')}/${String(t.mes).padStart(2,'0')}/${t.ano}`
            : '';

        li.innerHTML = `
            <button class="task-check" onclick="toggleConcluida(${t.id})">
                ${t.concluida ? '✔' : ''}
            </button>
            <div class="task-body">
                <div class="task-desc">${escapeHtml(t.descricao)}</div>
                ${dataStr ? `<div class="task-date">📅 ${dataStr}</div>` : ''}
            </div>
            <div class="task-actions">
                <button class="btn-icon" onclick="abrirEdicao(${t.id})">editar</button>
                <button class="btn-icon del" onclick="deletarTarefa(${t.id})">del</button>
            </div>
        `;
        lista.appendChild(li);
    });
}

function post(path, body) {
    return fetch(API + path, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(body)
    });
}

function irParaTela(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(id).classList.add('active');
}

function showError(el, msg) {
    el.textContent = msg;
    el.classList.remove('hidden');
    setTimeout(() => el.classList.add('hidden'), 3000);
}

function escapeHtml(s) {
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}