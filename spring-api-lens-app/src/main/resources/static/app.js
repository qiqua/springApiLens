let workbench = null;
let selectedKey = null;

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
  detailContent: document.getElementById('detailContent')
};

elements.scanButton.addEventListener('click', scanRepository);
elements.searchInput.addEventListener('input', renderEndpointList);
elements.methodFilter.addEventListener('change', renderEndpointList);
elements.tableInput.addEventListener('input', renderEndpointList);
elements.authorInput.addEventListener('input', renderEndpointList);

loadWorkbench();

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
    setStatus(`Failed to load workbench: ${error.message}`, true);
  }
}

async function scanRepository() {
  const repoPath = elements.repoPathInput.value.trim();
  const snapshotPath = elements.snapshotPathInput.value.trim();
  if (!repoPath) {
    setStatus('Enter a local Git repository path before scanning.', true);
    elements.repoPathInput.focus();
    return;
  }

  elements.scanButton.disabled = true;
  setStatus('Scanning repository...', false);

  try {
    const response = await fetch('/api/scan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ repoPath, snapshotPath })
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || 'Scan failed.');
    }
    setStatus(`Scan complete: ${body.endpointCount} endpoints, ${body.callEdgeCount} call edges.`, false);
    selectedKey = null;
    renderEmptyDetail('Select an endpoint to inspect evidence.');
    await loadWorkbench();
  } catch (error) {
    setStatus(error.message, true);
  } finally {
    elements.scanButton.disabled = false;
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
  elements.methodFilter.innerHTML = '<option value="">All</option>';
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
  elements.visibleCount.textContent = `${endpoints.length} shown`;
  elements.endpointList.innerHTML = '';

  if (endpoints.length === 0) {
    const empty = document.createElement('div');
    empty.className = 'empty-state';
    empty.textContent = workbench?.summary?.endpointCount ? 'No endpoints match the current filters.' : 'No scanned endpoints yet.';
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
        <span>${escapeHtml(endpoint.requestBodyType || 'no body')} -> ${escapeHtml(endpoint.responseType || 'unknown response')}</span>
        <span>${endpoint.callCount || 0} calls</span>
      </div>
      <div class="chip-row">${renderChips(endpoint.tables)}</div>
    `;
    elements.endpointList.appendChild(row);
  }
}

async function selectEndpoint(key) {
  selectedKey = key;
  renderEndpointList();
  renderEmptyDetail('Loading endpoint evidence...');

  try {
    const response = await fetch(`/api/endpoints/${encodeURIComponent(key)}`);
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(body.message || 'Endpoint detail failed to load.');
    }
    renderDetail(body);
  } catch (error) {
    renderEmptyDetail(error.message);
  }
}

function renderDetail(detail) {
  const endpoint = detail.endpoint || {};
  elements.detailContent.className = 'detail-content';
  elements.detailContent.innerHTML = `
    <section class="detail-section">
      <h3>Basic Information</h3>
      <div class="info-grid">
        <span class="meta-label">Endpoint</span><code>${escapeHtml(endpoint.httpMethod || '-')} ${escapeHtml(endpoint.path || '-')}</code>
        <span class="meta-label">Controller</span><code>${escapeHtml(endpoint.className || '-')}#${escapeHtml(endpoint.methodName || '-')}</code>
        <span class="meta-label">Source</span><code>${escapeHtml(endpoint.relativeFile || '-')}:${endpoint.lineStart || '-'}-${endpoint.lineEnd || '-'}</code>
      </div>
    </section>
    <section class="detail-section">
      <h3>Request / Response</h3>
      <div class="info-grid">
        <span class="meta-label">Request params</span><code>${escapeHtml(endpoint.requestParamsJson || '[]')}</code>
        <span class="meta-label">Request body</span><code>${escapeHtml(endpoint.requestBodyType || '-')}</code>
        <span class="meta-label">Response</span><code>${escapeHtml(endpoint.responseType || '-')}</code>
      </div>
    </section>
    <section class="detail-section">
      <h3>Call Evidence</h3>
      ${renderCallEdges(detail.callEdges || [])}
    </section>
    <section class="detail-section">
      <h3>SQL And Tables</h3>
      <div class="chip-row">${renderChips(detail.tables || [])}</div>
      ${renderSqlFragments(detail.sqlFragments || [])}
    </section>
    <section class="detail-section">
      <h3>Authors</h3>
      ${renderAuthors(detail.authors || [])}
    </section>
  `;
}

function renderCallEdges(callEdges) {
  if (!callEdges.length) {
    return '<p class="muted">No deterministic call evidence was found for this endpoint.</p>';
  }
  return `<div class="evidence-list">${callEdges.map((edge) => `
    <div class="evidence-item">
      <code>${escapeHtml(edge.fromSignature)} -> ${escapeHtml(edge.toSignature)}</code>
      <span class="muted">confidence ${Number(edge.confidence || 0).toFixed(2)} · ${escapeHtml(edge.evidence || '-')}</span>
    </div>
  `).join('')}</div>`;
}

function renderSqlFragments(sqlFragments) {
  if (!sqlFragments.length) {
    return '<p class="muted">No related SQL fragment was found for this endpoint.</p>';
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
    return '<p class="muted">Author ratios are not connected to the UI payload yet.</p>';
  }
  return `<div class="evidence-list">${authors.map((author) => `
    <div class="evidence-item">
      <strong>${escapeHtml(author.name || '-')}</strong>
      <span class="muted">${escapeHtml(author.email || '')} · ${Number(author.ratio || 0).toFixed(2)} · ${author.lineCount || 0} lines</span>
    </div>
  `).join('')}</div>`;
}

function renderChips(values) {
  const items = values || [];
  if (!items.length) {
    return '<span class="chip">no table evidence</span>';
  }
  return items.map((value) => `<span class="chip">${escapeHtml(value)}</span>`).join('');
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
