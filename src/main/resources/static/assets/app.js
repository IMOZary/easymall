const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];
const state = { user: null, categories: [], products: [], cart: { items: [], totalQuantity: 0, totalAmount: 0 }, coupons: [], categoryId: null, page: 0, totalPages: 1, authMode: 'login' };
const statusText = { PENDING_PAYMENT: '待支付', PAID: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELED: '已取消' };

async function api(url, options = {}, silent401 = false) {
  const response = await fetch(url, { headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }, ...options });
  let body;
  try { body = await response.json(); } catch { body = { success: false, message: '服务返回异常' }; }
  if (!response.ok || !body.success) {
    if (response.status === 401 && !silent401) openAuth();
    throw new Error(body.message || `请求失败 (${response.status})`);
  }
  return body.data;
}

async function init() {
  const [categories, coupons] = await Promise.all([api('/api/categories'), api('/api/coupons')]);
  state.categories = categories;
  state.coupons = coupons;
  renderCategories(); renderCoupons(); await loadProducts();
  try { state.user = await api('/api/auth/me', {}, true); await loadCart(); } catch { state.user = null; }
  renderUser(); bindEvents();
}

function bindEvents() {
  $$('[data-view]').forEach(el => el.addEventListener('click', e => { e.preventDefault(); showView(el.dataset.view); }));
  $('#userButton').addEventListener('click', () => state.user ? userMenu() : openAuth());
  $('#cartButton').addEventListener('click', openCart);
  $('#overlay').addEventListener('click', closeLayers);
  $$('[data-close]').forEach(el => el.addEventListener('click', closeLayers));
  $('.search-toggle').addEventListener('click', () => { showView('home'); setTimeout(() => $('#searchInput').focus(), 50); });
  $('#searchInput').addEventListener('keydown', e => { if (e.key === 'Enter') { state.page = 0; loadProducts(); } });
  $('#clearSearch').addEventListener('click', () => { $('#searchInput').value = ''; state.page = 0; loadProducts(); });
  $('#sortSelect').addEventListener('change', () => { state.page = 0; loadProducts(); });
  $('#moreButton').addEventListener('click', () => { state.page++; loadProducts(true); });
  $('#productGrid').addEventListener('click', e => { const button = e.target.closest('[data-add]'); if (button) addToCart(Number(button.dataset.add)); });
  $('#categoryList').addEventListener('click', e => { const button = e.target.closest('[data-category]'); if (!button) return; state.categoryId = button.dataset.category || null; state.page = 0; renderCategories(); loadProducts(); });
  $('#couponList').addEventListener('click', e => { const code = e.target.closest('[data-code]')?.dataset.code; if (code) { navigator.clipboard?.writeText(code); toast(`优惠码 ${code} 已复制`); } });
  $('#cartItems').addEventListener('click', handleCartClick);
  $('#cartFooter').addEventListener('click', e => { if (e.target.closest('#checkoutButton')) openCheckout(); });
  $$('[data-auth-tab]').forEach(el => el.addEventListener('click', () => switchAuth(el.dataset.authTab)));
  $('#authForm').addEventListener('submit', submitAuth);
  $('#checkoutForm').addEventListener('submit', submitCheckout);
  $('#orderList').addEventListener('click', handleOrderAction);
  $('#adminOrders').addEventListener('click', handleAdminAction);
  $('#newProductButton').addEventListener('click', openProductModal);
  $('#productForm').addEventListener('submit', submitProduct);
}

async function loadProducts(append = false) {
  const query = new URLSearchParams({ page: state.page, size: 8, sort: $('#sortSelect')?.value || 'newest', keyword: $('#searchInput')?.value || '' });
  if (state.categoryId) query.set('categoryId', state.categoryId);
  if (!append) $('#productGrid').innerHTML = '<div class="loading">正在整理好物…</div>';
  try {
    const page = await api(`/api/products?${query}`);
    state.products = append ? [...state.products, ...page.content] : page.content;
    state.totalPages = page.totalPages;
    renderProducts();
  } catch (e) { $('#productGrid').innerHTML = `<div class="empty">${escapeHtml(e.message)}</div>`; }
}

function renderCategories() {
  $('#categoryList').innerHTML = `<button class="${state.categoryId ? '' : 'active'}" data-category="">全部好物</button>` + state.categories.map(c => `<button class="${String(state.categoryId) === String(c.id) ? 'active' : ''}" data-category="${c.id}">${c.icon} ${escapeHtml(c.name)}</button>`).join('');
  $('#productCategory').innerHTML = state.categories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
}

function renderProducts() {
  if (!state.products.length) { $('#productGrid').innerHTML = '<div class="empty"><div><b>没有找到相关好物</b>换个关键词再试试吧</div></div>'; return; }
  $('#productGrid').innerHTML = state.products.map((p, i) => `<article class="product-card">
    <div class="product-visual theme-${p.theme}"><span class="product-badge">${i < 2 && state.page === 0 ? 'POPULAR' : p.stock < 20 ? 'LOW STOCK' : 'EASY PICK'}</span><span class="product-icon">${p.icon}</span><button class="quick-add" data-add="${p.id}" title="加入购物车">＋</button></div>
    <div class="product-info"><span class="product-category">${escapeHtml(p.category.name).toUpperCase()}</span><h3>${escapeHtml(p.name)}</h3><p class="product-subtitle">${escapeHtml(p.subtitle)}</p><div class="price-line"><b class="price">¥${money(p.price)}</b><span class="sales">已售 ${p.sales}</span></div></div>
  </article>`).join('');
  $('#moreButton').classList.toggle('hidden', state.page + 1 >= state.totalPages);
}

function renderCoupons() {
  $('#couponList').innerHTML = state.coupons.slice(0, 3).map(c => `<div class="coupon-card"><b>${c.type === 'FIXED' ? `¥${Number(c.discountValue)}` : `${Number(c.discountValue) * 10}折`}</b><span>${escapeHtml(c.description)}</span><button class="coupon-code" data-code="${c.code}">${c.code} · 复制</button></div>`).join('');
}

function renderUser() {
  $('#userLabel').textContent = state.user ? state.user.nickname : '登录 / 注册';
  $$('.admin-only').forEach(el => el.classList.toggle('hidden', state.user?.role !== 'ADMIN'));
  $('#cartCount').textContent = state.cart.totalQuantity || 0;
}

async function addToCart(productId) {
  if (!state.user) return openAuth();
  try { state.cart = await api('/api/cart', { method: 'POST', body: JSON.stringify({ productId, quantity: 1 }) }); renderCart(); toast('已加入购物袋'); }
  catch (e) { toast(e.message, true); }
}

async function loadCart() {
  if (!state.user) return;
  state.cart = await api('/api/cart'); renderCart();
}

function renderCart() {
  $('#cartCount').textContent = state.cart.totalQuantity || 0;
  $('#drawerCount').textContent = `${state.cart.totalQuantity || 0} 件`;
  if (!state.cart.items?.length) {
    $('#cartItems').innerHTML = '<div class="empty"><div><b>购物袋还是空的</b>去挑一件喜欢的好物吧</div></div>';
    $('#cartFooter').innerHTML = '';
    return;
  }
  $('#cartItems').innerHTML = state.cart.items.map(i => `<div class="cart-item"><div class="cart-thumb theme-${i.product.theme}">${i.product.icon}</div><div class="cart-detail"><b>${escapeHtml(i.product.name)}</b><small>¥${money(i.product.price)}</small><div class="qty-control"><button data-qty="${i.id}" data-value="${i.quantity - 1}">−</button><span>${i.quantity}</span><button data-qty="${i.id}" data-value="${i.quantity + 1}">＋</button></div></div><button class="remove" data-remove="${i.id}">×</button></div>`).join('');
  $('#cartFooter').innerHTML = `<div class="total-line"><span>商品合计</span><b>¥${money(state.cart.totalAmount)}</b></div><button class="primary-button full" id="checkoutButton">去结算 <span>→</span></button>`;
}

async function handleCartClick(e) {
  const qty = e.target.closest('[data-qty]'); const remove = e.target.closest('[data-remove]');
  try {
    if (qty) {
      if (Number(qty.dataset.value) < 1) return;
      state.cart = await api(`/api/cart/${qty.dataset.qty}`, { method: 'PUT', body: JSON.stringify({ quantity: Number(qty.dataset.value) }) });
    } else if (remove) state.cart = await api(`/api/cart/${remove.dataset.remove}`, { method: 'DELETE' });
    renderCart();
  } catch (err) { toast(err.message, true); }
}

function openCart() {
  if (!state.user) return openAuth();
  loadCart().catch(e => toast(e.message, true));
  openLayer($('#cartDrawer'));
}

function openCheckout() {
  if (!state.cart.items?.length) return;
  $('#checkoutAmount').textContent = `¥${money(state.cart.totalAmount)}`;
  $('#cartDrawer').classList.remove('open');
  openLayer($('#checkoutModal'));
}

async function submitCheckout(e) {
  e.preventDefault(); const form = new FormData(e.target); const payload = Object.fromEntries(form.entries());
  payload.idempotencyKey = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
  try {
    const order = await api('/api/orders', { method: 'POST', body: JSON.stringify(payload) });
    state.cart = { items: [], totalQuantity: 0, totalAmount: 0 }; renderCart(); closeLayers(); e.target.reset(); toast(`订单 ${order.orderNo} 创建成功`); showView('orders');
  } catch (err) { toast(err.message, true); }
}

function openAuth() { switchAuth('login'); openLayer($('#authModal')); }
function switchAuth(mode) {
  state.authMode = mode; $$('[data-auth-tab]').forEach(x => x.classList.toggle('active', x.dataset.authTab === mode));
  $$('.register-field').forEach(x => x.classList.toggle('hidden', mode !== 'register'));
  $('[name="nickname"]').required = mode === 'register'; $('#authSubmit').textContent = mode === 'login' ? '登 录' : '注 册';
}

async function submitAuth(e) {
  e.preventDefault(); const payload = Object.fromEntries(new FormData(e.target).entries());
  if (state.authMode === 'login') delete payload.nickname;
  try { state.user = await api(`/api/auth/${state.authMode}`, { method: 'POST', body: JSON.stringify(payload) }, true); closeLayers(); e.target.reset(); await loadCart(); renderUser(); toast(state.authMode === 'login' ? '欢迎回来' : '注册成功，欢迎加入'); }
  catch (err) { toast(err.message, true); }
}

function userMenu() {
  if (confirm(`${state.user.nickname}，是否退出当前账号？`)) api('/api/auth/logout', { method: 'POST' }).then(() => { state.user = null; state.cart = { items: [], totalQuantity: 0, totalAmount: 0 }; renderUser(); showView('home'); toast('已安全退出'); });
}

async function showView(name) {
  if ((name === 'orders' || name === 'admin') && !state.user) { openAuth(); return; }
  if (name === 'admin' && state.user?.role !== 'ADMIN') { toast('仅管理员可访问', true); return; }
  $$('.view').forEach(v => v.classList.toggle('active', v.id === `${name}View`));
  $$('.nav-link').forEach(n => n.classList.toggle('active', n.dataset.view === name));
  window.scrollTo({ top: 0, behavior: 'smooth' });
  if (name === 'orders') await loadOrders();
  if (name === 'admin') await loadAdmin();
}

async function loadOrders() {
  $('#orderList').innerHTML = '<div class="loading">正在查询订单…</div>';
  try {
    const page = await api('/api/orders'); const orders = page.content;
    $('#orderList').innerHTML = orders.length ? orders.map(orderCard).join('') : '<div class="empty"><div><b>还没有订单</b>第一件喜欢的好物正在等你</div></div>';
  } catch (e) { $('#orderList').innerHTML = `<div class="empty">${escapeHtml(e.message)}</div>`; }
}

function orderCard(o) {
  const count = o.items.reduce((sum, i) => sum + i.quantity, 0); const first = o.items[0];
  let actions = ''; if (o.status === 'PENDING_PAYMENT') actions = `<button class="small-button ghost" data-order-action="cancel" data-id="${o.id}">取消订单</button><button class="small-button" data-order-action="pay" data-id="${o.id}">立即支付</button>`;
  else if (o.status === 'PAID') actions = `<button class="small-button ghost" data-order-action="cancel" data-id="${o.id}">申请取消</button>`;
  else if (o.status === 'SHIPPED') actions = `<button class="small-button" data-order-action="complete" data-id="${o.id}">确认收货</button>`;
  return `<article class="order-card"><div class="order-head"><span><b>${o.orderNo}</b>${formatDate(o.createdAt)}</span><span class="status ${o.status}">${statusText[o.status]}</span></div><div class="order-body"><div class="mini-product">${first?.productIcon || '□'}</div><div class="order-products"><b>${escapeHtml(first?.productName || '')}${o.items.length > 1 ? ` 等 ${o.items.length} 种商品` : ''}</b><span>共 ${count} 件 · 收货人 ${escapeHtml(o.receiver)}</span></div><div class="order-amount"><b>¥${money(o.payAmount)}</b>${Number(o.discountAmount) ? `<small>已优惠 ¥${money(o.discountAmount)}</small>` : ''}</div></div>${actions ? `<div class="order-actions">${actions}</div>` : ''}</article>`;
}

async function handleOrderAction(e) {
  const button = e.target.closest('[data-order-action]'); if (!button) return;
  const action = button.dataset.orderAction;
  if (action === 'cancel' && !confirm('确定取消订单吗？库存将自动回补。')) return;
  try { await api(`/api/orders/${button.dataset.id}/${action}`, { method: 'POST' }); toast(action === 'pay' ? '模拟支付成功' : action === 'complete' ? '确认收货成功' : '订单已取消'); await loadOrders(); }
  catch (err) { toast(err.message, true); }
}

async function loadAdmin() {
  try {
    const [stats, orders] = await Promise.all([api('/api/admin/dashboard'), api('/api/admin/orders?size=12')]);
    $('#statsGrid').innerHTML = `<div class="stat-card"><span>累计用户</span><b>${stats.users}</b></div><div class="stat-card"><span>在售商品</span><b>${stats.products}</b></div><div class="stat-card"><span>累计订单</span><b>${stats.orders}</b></div><div class="stat-card"><span>成交金额</span><b>¥${money(stats.revenue)}</b></div>`;
    $('#adminOrders').innerHTML = orders.content.length ? orders.content.map(o => `<div class="admin-row"><span><b>${o.orderNo}</b><small>${escapeHtml(o.customer)} · ${formatDate(o.createdAt)}</small></span><span>${escapeHtml(o.items[0]?.productName || '')}${o.items.length > 1 ? ' 等' : ''}</span><b>¥${money(o.payAmount)}</b><span>${o.status === 'PAID' ? `<button class="small-button" data-ship="${o.id}">发货</button>` : `<i class="status ${o.status}">${statusText[o.status]}</i>`}</span></div>`).join('') : '<div class="empty">暂无订单</div>';
  } catch (e) { toast(e.message, true); }
}

async function handleAdminAction(e) {
  const button = e.target.closest('[data-ship]'); if (!button) return;
  try { await api(`/api/admin/orders/${button.dataset.ship}/ship`, { method: 'POST' }); toast('订单已发货'); await loadAdmin(); }
  catch (err) { toast(err.message, true); }
}

function openProductModal() { openLayer($('#productModal')); }
async function submitProduct(e) {
  e.preventDefault(); const payload = Object.fromEntries(new FormData(e.target).entries()); payload.categoryId = Number(payload.categoryId); payload.price = Number(payload.price); payload.stock = Number(payload.stock); payload.status = 'ON_SALE';
  try { await api('/api/admin/products', { method: 'POST', body: JSON.stringify(payload) }); closeLayers(); e.target.reset(); toast('商品创建成功'); state.page = 0; await loadProducts(); await loadAdmin(); }
  catch (err) { toast(err.message, true); }
}

function openLayer(el) { closeLayers(); $('#overlay').classList.add('open'); el.classList.add('open'); document.body.style.overflow = 'hidden'; }
function closeLayers() { $('#overlay').classList.remove('open'); $$('.modal,.drawer').forEach(x => x.classList.remove('open')); document.body.style.overflow = ''; }
function toast(message, error = false) { const el = document.createElement('div'); el.className = `toast${error ? ' error' : ''}`; el.textContent = message; $('#toasts').append(el); setTimeout(() => el.remove(), 3200); }
function money(value) { return Number(value || 0).toFixed(2); }
function formatDate(value) { return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }); }
function escapeHtml(value = '') { return String(value).replace(/[&<>'"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[c])); }

init().catch(e => { console.error(e); toast('应用初始化失败：' + e.message, true); });
