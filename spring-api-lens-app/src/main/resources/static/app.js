let workbench = null;
let selectedKey = null;
let currentDetail = null;

const elements = {
  repoPathInput: document.getElementById('repoPathInput'),
  snapshotPathInput: document.getElementById('snapshotPathInput'),
  scanButton: document.getElementById('scanButton'),
  scanStatus: document.getElementById('scanStatus'),
  repoName: document.getElementById('repoName'),
  endpointCount: document.getElementById('endpointCount'),
  callEdgeCount: document.getElementById('callEdgeCount'),
  sqlFragmentCount: document.getElementById('sqlFragmentCount'),
  tableCount: document.getElementById('tableCount'),
  searchInput: document.getElementById('searchInput'),
  methodFilter: document.getElementById('methodFilter'),
  tableInput: document.getElementById('tableInput'),
  authorInput: document.getElementById('authorInput'),
  visibleCount: document.getElementById('visibleCount'),
  endpointList: document.getElementById('endpointList'),
  detailContent: document.getElementById('detailContent'),
  refreshHistoryButton: document.getElementById('refreshHistoryButton'),
  historyList: document.getElementById('historyList'),
  aiConfigStatus: document.getElementById('aiConfigStatus'),
  aiEnabledInput: document.getElementById('aiEnabledInput'),
  aiProviderInput: document.getElementById('aiProviderInput'),
  aiBaseUrlInput: document.getElementById('aiBaseUrlInput'),
  aiModelInput: document.getElementById('aiModelInput'),
  aiApiKeyEnvInput: document.getElementById('aiApiKeyEnvInput'),
  saveAiConfigButton: document.getElementById('saveAiConfigButton')
};

elements.scanButton.addEventListener('click', scanRepository);
elements.refreshHistoryButton.addEventListener('click', loadHistory);
elements.saveAiConfigButton.addEventListener('click', saveAiConfig);
elements.searchInput.addEventListener('input', renderEndpointList);
elements.methodFilter.addEventListener('change', renderEndpointList);
elements.tableInput.addEventListener('input', renderEndpointList);
elements.authorInput.addEventListener('input', renderEndpointList);

loadWorkbench();
loadHistory();
loadAiConfig();

async function loadWorkbench() {
  try {
    const response = await fetch('/api/workbench');
    if (!response.ok) {
      throw new Error(await response.text());
    }
    workbench = await response.json();
    renderSummary();
    renderMethodOptions();
    renderEndpointList();
  } catch (error) {
    setStatus(`加载工作台失败：${error.message}`, true);
  }
}

async function scanRepository() {
  const repoPath = elements.repoPathInput.value.trim();
  const snapshotPath = elements.snapshotPathInput.value.trim();
  if (!repoPath) {
    setStatus('请先输入本地 Git 仓库路径。', true);
    elements.repoPathInput.focus();
    return;
  }

  elements.scanButton.disabled = true;
  setStatus('正在扫描代码仓库...', false);

  try {
    const response = await fetch('/api/scan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ repoPath, snapshotPath })
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || '扫描失败。');
    }
    setStatus(`扫描完成：${body.endpointCount} 个接口，${body.callEdgeCount} 条调用边。`, false);
    selectedKey = null;
    currentDetail = null;
    renderEmptyDetail('选择一个接口查看业务画像和证据。');
    await loadWorkbench();
    await loadHistory();
  } catch (error) {
    setStatus(error.message, true);
  } finally {
    elements.scanButton.disabled = false;
  }
}

async function loadHistory() {
  try {
    const response = await fetch('/api/history');
    const body = await response.json().catch(() => []);
    if (!response.ok) {
      throw new Error(body.message || '扫描历史加载失败。');
    }
    renderHistory(body);
  } catch (error) {
    elements.historyList.className = 'history-list empty-state';
    elements.historyList.textContent = error.message;
  }
}

function renderHistory(history) {
  elements.historyList.innerHTML = '';
  if (!history.length) {
    elements.historyList.className = 'history-list empty-state';
    elements.historyList.textContent = '暂无扫描历史。';
    return;
  }

  elements.historyList.className = 'history-list';
  for (const entry of history) {
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'history-row';
    row.addEventListener('click', () => loadHistoryEntry(entry.id));
    row.innerHTML = `
      <strong>${escapeHtml(entry.repoName || '-')}</strong>
      <span>${escapeHtml(entry.branchName || '-')} · ${escapeHtml(entry.headCommit || '-')}</span>
      <span>${entry.endpointCount || 0} 个接口 · ${formatDate(entry.scannedAt)}</span>
    `;
    elements.historyList.appendChild(row);
  }
}

async function loadHistoryEntry(scanId) {
  setStatus('正在加载扫描历史...', false);
  try {
    const response = await fetch(`/api/history/${encodeURIComponent(scanId)}/load`, {
      method: 'POST'
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || '扫描历史加载失败。');
    }
    selectedKey = null;
    currentDetail = null;
    renderEmptyDetail('选择一个接口查看业务画像和证据。');
    await loadWorkbench();
    setStatus(`已加载历史扫描：${body.endpointCount} 个接口。`, false);
  } catch (error) {
    setStatus(error.message, true);
  }
}

async function loadAiConfig() {
  try {
    const response = await fetch('/api/ai-config');
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || 'AI 配置加载失败。');
    }
    renderAiConfig(body);
  } catch (error) {
    elements.aiConfigStatus.textContent = error.message;
    elements.aiConfigStatus.classList.add('error-text');
  }
}

function renderAiConfig(config) {
  elements.aiEnabledInput.checked = Boolean(config.enabled);
  elements.aiProviderInput.value = config.provider || '';
  elements.aiBaseUrlInput.value = config.baseUrl || '';
  elements.aiModelInput.value = config.model || '';
  elements.aiApiKeyEnvInput.value = config.apiKeyEnv || '';
  elements.aiConfigStatus.textContent = config.configured ? '已配置' : translateAiMessage(config.message || '未配置');
  elements.aiConfigStatus.classList.toggle('error-text', !config.configured);
}

async function saveAiConfig() {
  elements.saveAiConfigButton.disabled = true;
  elements.aiConfigStatus.textContent = '正在保存...';
  elements.aiConfigStatus.classList.remove('error-text');

  try {
    const response = await fetch('/api/ai-config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        enabled: elements.aiEnabledInput.checked,
        provider: elements.aiProviderInput.value.trim(),
        baseUrl: elements.aiBaseUrlInput.value.trim(),
        model: elements.aiModelInput.value.trim(),
        apiKeyEnv: elements.aiApiKeyEnvInput.value.trim()
      })
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || 'AI 配置保存失败。');
    }
    renderAiConfig(body);
  } catch (error) {
    elements.aiConfigStatus.textContent = error.message;
    elements.aiConfigStatus.classList.add('error-text');
  } finally {
    elements.saveAiConfigButton.disabled = false;
  }
}

function renderSummary() {
  const repository = workbench?.repository || {};
  const summary = workbench?.summary || {};
  elements.repoName.textContent = repository.repoName || '-';
  elements.endpointCount.textContent = summary.endpointCount ?? 0;
  elements.callEdgeCount.textContent = summary.callEdgeCount ?? 0;
  elements.sqlFragmentCount.textContent = summary.sqlFragmentCount ?? 0;
  elements.tableCount.textContent = summary.tableCount ?? 0;
}

function renderMethodOptions() {
  const current = elements.methodFilter.value;
  const methods = workbench?.filters?.httpMethods || [];
  elements.methodFilter.innerHTML = '<option value="">全部</option>';
  for (const method of methods) {
    const option = document.createElement('option');
    option.value = method;
    option.textContent = method;
    elements.methodFilter.appendChild(option);
  }
  elements.methodFilter.value = methods.includes(current) ? current : '';
}

function filteredEndpoints() {
  const endpoints = workbench?.endpoints || [];
  const query = elements.searchInput.value.trim().toLowerCase();
  const method = elements.methodFilter.value;
  const table = elements.tableInput.value.trim().toLowerCase();
  const author = elements.authorInput.value.trim().toLowerCase();

  return endpoints.filter((endpoint) => {
    const haystack = [
      endpoint.path,
      endpoint.className,
      endpoint.methodName,
      endpoint.requestBodyType,
      endpoint.responseType,
      endpoint.relativeFile
    ].join(' ').toLowerCase();
    const tables = (endpoint.tables || []).join(' ').toLowerCase();
    const authors = (endpoint.authors || []).join(' ').toLowerCase();

    return (!query || haystack.includes(query))
      && (!method || endpoint.httpMethod === method)
      && (!table || tables.includes(table))
      && (!author || authors.includes(author));
  });
}

function renderEndpointList() {
  const endpoints = filteredEndpoints();
  const total = workbench?.summary?.endpointCount ?? (workbench?.endpoints || []).length;
  elements.visibleCount.textContent = `已显示 ${endpoints.length} / 共 ${total} 个`;
  elements.endpointList.innerHTML = '';

  if (endpoints.length === 0) {
    const empty = document.createElement('div');
    empty.className = 'empty-state';
    empty.textContent = workbench?.summary?.endpointCount ? '没有接口匹配当前筛选条件。' : '暂无已扫描接口。';
    elements.endpointList.appendChild(empty);
    return;
  }

  for (const endpoint of endpoints) {
    const row = document.createElement('button');
    row.type = 'button';
    row.className = `endpoint-row ${endpoint.key === selectedKey ? 'selected' : ''}`;
    row.setAttribute('role', 'listitem');
    row.addEventListener('click', () => selectEndpoint(endpoint.key));

    const methodClass = (endpoint.httpMethod || '').toLowerCase();
    row.innerHTML = `
      <div class="endpoint-line">
        <span class="method-badge ${escapeHtml(methodClass)}">${escapeHtml(endpoint.httpMethod || '-')}</span>
        <span class="path-text">${escapeHtml(endpoint.path || '-')}</span>
      </div>
      <div class="endpoint-meta">
        <span>${escapeHtml(endpoint.className || '-')}#${escapeHtml(endpoint.methodName || '-')}</span>
        <span>${escapeHtml(endpoint.requestBodyType || '无请求体')} -> ${escapeHtml(endpoint.responseType || '未知响应')}</span>
        <span>${endpoint.callCount || 0} 条调用</span>
      </div>
      <div class="chip-row">${renderChips(endpoint.tables, '暂无表证据')}${renderAuthorChips(endpoint.authors || [])}</div>
    `;
    elements.endpointList.appendChild(row);
  }
}

async function selectEndpoint(key) {
  selectedKey = key;
  currentDetail = null;
  renderEndpointList();
  renderEmptyDetail('正在加载接口证据...');

  try {
    const response = await fetch(`/api/endpoints/${encodeURIComponent(key)}`);
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || '接口详情加载失败。');
    }
    renderDetail(body);
  } catch (error) {
    renderEmptyDetail(error.message);
  }
}

function renderDetail(detail) {
  currentDetail = detail;
  const endpoint = detail.endpoint || {};
  elements.detailContent.className = 'detail-content';
  elements.detailContent.innerHTML = `
    <section class="detail-section">
      <h3>接口业务画像</h3>
      ${renderBusinessProfile(detail.profile || {})}
    </section>
    <section class="detail-section">
      <h3>基础信息</h3>
      <div class="info-grid">
        <span class="meta-label">接口</span><code>${escapeHtml(endpoint.httpMethod || '-')} ${escapeHtml(endpoint.path || '-')}</code>
        <span class="meta-label">控制器</span><code>${escapeHtml(endpoint.className || '-')}#${escapeHtml(endpoint.methodName || '-')}</code>
        <span class="meta-label">源码位置</span><code>${escapeHtml(endpoint.relativeFile || '-')}:${endpoint.lineStart || '-'}-${endpoint.lineEnd || '-'}</code>
      </div>
    </section>
    <section class="detail-section">
      <h3>请求和响应</h3>
      <div class="info-grid">
        <span class="meta-label">请求参数</span><code>${escapeHtml(endpoint.requestParamsJson || '[]')}</code>
        <span class="meta-label">请求体</span><code>${escapeHtml(endpoint.requestBodyType || '-')}</code>
        <span class="meta-label">响应</span><code>${escapeHtml(endpoint.responseType || '-')}</code>
      </div>
    </section>
    <section class="detail-section">
      <h3>调用链证据</h3>
      ${renderCallEdges(detail.callEdges || [])}
    </section>
    <section class="detail-section">
      <h3>SQL 和数据表</h3>
      <div class="chip-row">${renderChips(detail.tables || [])}</div>
      ${renderSqlFragments(detail.sqlFragments || [])}
    </section>
    <section class="detail-section">
      <h3>入口、调用链及文件历史提交人</h3>
      ${renderAuthors(detail.authors || [])}
    </section>
    <section class="detail-section">
      <div class="section-title-row">
        <h3>AI 分析摘要</h3>
        <button id="aiSummaryButton" class="secondary-button" type="button">生成摘要</button>
      </div>
      <div id="aiSummaryContent" class="ai-summary muted">配置 AI 后，可基于当前证据生成接口分析摘要。</div>
    </section>
  `;
  document.getElementById('aiSummaryButton').addEventListener('click', generateAiSummary);
}

function renderBusinessProfile(profile) {
  return `
    <div class="profile-summary">
      <div>
        <span class="meta-label">接口用途</span>
        <strong>${escapeHtml(profile.purpose || '暂无画像。')}</strong>
      </div>
      <div>
        <span class="meta-label">调用方式</span>
        <strong>${escapeHtml(profile.callGuide || '暂无调用信息。')}</strong>
      </div>
    </div>
    <div class="profile-grid">
      ${renderProfileBlock('业务流程', profile.businessFlow)}
      ${renderProfileBlock('涉及数据表', profile.dataTables)}
      ${renderProfileBlock('入口、调用链及文件历史作者', profile.authorSummary)}
      ${renderProfileBlock('风险点', profile.risks)}
      ${renderProfileBlock('测试建议', profile.testSuggestions)}
    </div>
  `;
}

function renderProfileBlock(title, values) {
  const items = values || [];
  const content = items.length
    ? `<ul>${items.map((value) => `<li>${escapeHtml(value)}</li>`).join('')}</ul>`
    : '<p class="muted">暂无证据。</p>';
  return `
    <div class="profile-block">
      <h4>${escapeHtml(title)}</h4>
      ${content}
    </div>
  `;
}

function renderCallEdges(callEdges) {
  if (!callEdges.length) {
    return '<p class="muted">暂未识别到该接口的确定性调用链证据。</p>';
  }
  return `<div class="evidence-list">${callEdges.map((edge) => `
    <div class="evidence-item">
      <code>${escapeHtml(edge.fromSignature)} -> ${escapeHtml(edge.toSignature)}</code>
      <span class="muted">置信度 ${Number(edge.confidence || 0).toFixed(2)} · ${escapeHtml(edge.evidence || '-')}</span>
    </div>
  `).join('')}</div>`;
}

function renderSqlFragments(sqlFragments) {
  if (!sqlFragments.length) {
    return '<p class="muted">暂未识别到该接口相关 SQL 片段。</p>';
  }
  return `<div class="evidence-list">${sqlFragments.map((fragment) => `
    <div class="evidence-item">
      <strong>${escapeHtml(fragment.operationType || '-')} · ${escapeHtml(fragment.mapperNamespace || '-')}.${escapeHtml(fragment.mapperMethod || '-')}</strong>
      <span class="muted">${escapeHtml(fragment.relativeFile || '-')}</span>
      <pre class="code-block">${escapeHtml(fragment.sqlText || '')}</pre>
    </div>
  `).join('')}</div>`;
}


function renderAuthors(authors) {
  if (!authors.length) {
    return '<p class="muted">暂未识别到该接口入口、调用链及相关文件历史的 Git 作者证据。</p>';
  }
  return `<div class="evidence-list">${authors.map((author) => `
    <div class="evidence-item">
      <div class="author-row">
        <strong>${escapeHtml(author.name || '-')}</strong>
        <span class="muted">${Math.round(Number(author.ratio || 0) * 100)}% · ${author.lineCount || 0} 条证据</span>
      </div>
      <div class="author-bar" aria-hidden="true"><span style="width: ${Math.max(0, Math.min(100, Number(author.ratio || 0) * 100))}%"></span></div>
      <span class="muted">${escapeHtml(author.email || '')}</span>
    </div>
  `).join('')}</div>`;
}

async function generateAiSummary() {
  if (!selectedKey || !currentDetail) {
    return;
  }

  const button = document.getElementById('aiSummaryButton');
  const content = document.getElementById('aiSummaryContent');
  button.disabled = true;
  content.className = 'ai-summary muted';
  content.textContent = '正在根据接口证据生成摘要...';

  try {
    const response = await fetch(`/api/endpoints/${encodeURIComponent(selectedKey)}/ai-summary`, {
      method: 'POST'
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || 'AI 摘要生成失败。');
    }
    renderAiSummary(body);
  } catch (error) {
    content.className = 'ai-summary error';
    content.textContent = error.message;
  } finally {
    button.disabled = false;
  }
}

function renderAiSummary(summary) {
  const content = document.getElementById('aiSummaryContent');
  if (!summary.configured) {
    content.className = 'ai-summary muted';
    content.textContent = translateAiMessage(summary.message || 'AI 未配置。');
    return;
  }

  content.className = 'ai-summary';
  content.innerHTML = `
    <div class="ai-meta">${escapeHtml(summary.provider || 'AI')} · ${escapeHtml(summary.model || '-')}</div>
    <pre class="summary-block">${escapeHtml(summary.content || '')}</pre>
  `;
}

function renderChips(values, emptyText = '暂无证据') {
  const items = values || [];
  if (!items.length) {
    return `<span class="chip">${escapeHtml(emptyText)}</span>`;
  }
  return items.map((value) => `<span class="chip">${escapeHtml(value)}</span>`).join('');
}

function renderAuthorChips(authors) {
  if (!authors.length) {
    return '';
  }
  return authors.map((value) => `<span class="chip author-chip">${escapeHtml(value)}</span>`).join('');
}

function renderEmptyDetail(message) {
  elements.detailContent.className = 'detail-content empty-state';
  elements.detailContent.textContent = message;
}

function setStatus(message, isError) {
  elements.scanStatus.textContent = message;
  elements.scanStatus.classList.toggle('error', Boolean(isError));
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function translateAiMessage(message) {
  const dictionary = {
    'AI is disabled.': 'AI 已禁用。',
    'AI is enabled but baseUrl, model, or API key is missing.': 'AI 已启用，但 Base URL、模型或密钥环境变量缺失。',
    'AI is disabled. Configure .spring-api-lens/ai-config.json to enable summaries.': 'AI 已禁用，请在 AI 配置中启用后再生成摘要。'
  };
  return dictionary[message] || message;
}
