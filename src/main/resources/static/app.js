// ---------------------------------------------------------------------------
// Menu universal: cada perfil tem sua própria lista de itens.
// Quando os perfis de Funcionário do FUSEX e Médico do FUSEX entrarem em
// desenvolvimento (próximas sprints), basta adicionar uma nova chave aqui —
// o restante do código (renderização, navegação) já funciona sem alteração.
// ---------------------------------------------------------------------------

const ICONS = {
  painel: '<svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="8" height="8" rx="1.5"/><rect x="13" y="3" width="8" height="5" rx="1.5"/><rect x="13" y="10" width="8" height="11" rx="1.5"/><rect x="3" y="13" width="8" height="8" rx="1.5"/></svg>',
  encaminhamento: '<svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6M9 17h6"/></svg>',
  preguia: '<svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 5H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3"/><path d="M18 3l3 3-9.5 9.5L8 16l.5-3.5z"/></svg>',
  perfil: '<svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7"/></svg>'
};

const MENUS = {
  BENEFICIARIO: [
    { id: 'painel', label: 'Painel', icon: ICONS.painel },
    { id: 'encaminhamento', label: 'Encaminhamento médico', icon: ICONS.encaminhamento },
    { id: 'pre-guias', label: 'Minhas pré-guias', icon: ICONS.preguia },
    { id: 'perfil', label: 'Meu perfil', icon: ICONS.perfil }
  ]
};

// Em produção, isso viria da sessão/token de autenticação.
// Fica isolado aqui para ser trocado facilmente quando o login estiver pronto.
const currentUser = {
  nome: 'Beneficiário Teste',
  perfil: 'BENEFICIARIO'
};

const state = { activeView: 'painel' };

function initials(nome) {
  return nome.split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
}

function roleLabel(perfil) {
  const labels = { BENEFICIARIO: 'Beneficiário' };
  return labels[perfil] || perfil;
}

function renderSidebar() {
  const list = document.getElementById('menuList');
  const items = MENUS[currentUser.perfil] || [];

  list.innerHTML = items.map(item => `
    <li>
      <button class="sidebar__link ${item.id === state.activeView ? 'is-active' : ''}" data-view="${item.id}">
        ${item.icon}
        <span>${item.label}</span>
      </button>
    </li>
  `).join('');

  list.querySelectorAll('.sidebar__link').forEach(button => {
    button.addEventListener('click', () => navigateTo(button.dataset.view));
  });

  document.getElementById('userName').textContent = currentUser.nome;
  document.getElementById('userRole').textContent = roleLabel(currentUser.perfil);
  document.getElementById('userInitials').textContent = initials(currentUser.nome);
}

const VIEWS = {
  'painel': {
    title: 'Painel',
    render: () => `
      <p style="color: var(--color-text-muted); max-width: 60ch;">
        Acompanhe seu encaminhamento e suas pré-guias, ou inicie uma nova solicitação.
      </p>
      <div class="action-grid">
        <div class="action-card">
          <h3>Anexar encaminhamento</h3>
          <p>Envie o encaminhamento médico digitalizado para liberar a criação da pré-guia.</p>
          <button data-goto="encaminhamento">Anexar agora</button>
        </div>
        <div class="action-card">
          <h3>Criar pré-guia</h3>
          <p>Informe a OCS e os procedimentos a partir do seu encaminhamento já anexado.</p>
          <button data-goto="pre-guias">Nova pré-guia</button>
        </div>
      </div>
    `
  },
  'encaminhamento': {
    title: 'Encaminhamento médico',
    render: () => `
      <div class="empty-state">
        Nenhum encaminhamento anexado ainda.<br>
        Envie o arquivo digitalizado para poder criar sua pré-guia.
      </div>
    `
  },
  'pre-guias': {
    title: 'Minhas pré-guias',
    render: () => `
      <div class="empty-state">
        Você ainda não criou nenhuma pré-guia.
      </div>
    `
  },
  'perfil': {
    title: 'Meu perfil',
    render: () => `
      <p><strong>Nome:</strong> ${currentUser.nome}</p>
      <p><strong>Perfil:</strong> ${roleLabel(currentUser.perfil)}</p>
    `
  }
};

function navigateTo(viewId) {
  if (!VIEWS[viewId]) return;
  state.activeView = viewId;

  document.getElementById('pageTitle').textContent = VIEWS[viewId].title;
  document.getElementById('content').innerHTML = VIEWS[viewId].render();

  renderSidebar();
  closeMobileMenu();

  document.querySelectorAll('[data-goto]').forEach(button => {
    button.addEventListener('click', () => navigateTo(button.dataset.goto));
  });
}

// ---------------------------------------------------------------------------
// Menu mobile (sidebar retrátil)
// ---------------------------------------------------------------------------

function openMobileMenu() {
  document.getElementById('sidebar').classList.add('is-open');
  document.getElementById('overlay').classList.add('is-open');
  document.getElementById('menuToggle').setAttribute('aria-expanded', 'true');
}

function closeMobileMenu() {
  document.getElementById('sidebar').classList.remove('is-open');
  document.getElementById('overlay').classList.remove('is-open');
  document.getElementById('menuToggle').setAttribute('aria-expanded', 'false');
}

document.getElementById('menuToggle').addEventListener('click', () => {
  const isOpen = document.getElementById('sidebar').classList.contains('is-open');
  isOpen ? closeMobileMenu() : openMobileMenu();
});

document.getElementById('overlay').addEventListener('click', closeMobileMenu);

document.getElementById('logoutButton').addEventListener('click', () => {
  // Placeholder: quando a autenticação estiver pronta, limpar sessão/token
  // e redirecionar para a tela de login.
  alert('Logout ainda não implementado — depende da tela de autenticação.');
});

// ---------------------------------------------------------------------------
// Inicialização
// ---------------------------------------------------------------------------

renderSidebar();
navigateTo('painel');
