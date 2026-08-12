import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

function App() {
  const [token, setToken] = useState(localStorage.getItem('paymentToken') || '');
  const [authMode, setAuthMode] = useState('login');
  const [authForm, setAuthForm] = useState({ name: '', email: 'merchant1@example.com', password: 'password' });
  const [paymentForm, setPaymentForm] = useState({
    amount: '1500.00',
    currency: 'INR',
    description: 'Test payment',
    paymentMethod: 'CARD',
    simulationToken: '4242',
    idempotencyKey: crypto.randomUUID()
  });
  const [refundForm, setRefundForm] = useState({ paymentId: '', amount: '500.00', reason: 'Customer requested refund' });
  const [payments, setPayments] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const headers = useMemo(() => ({
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  }), [token]);

  useEffect(() => {
    if (token) loadPayments();
  }, [token, statusFilter]);

  async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, options);
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
      throw new Error(data?.message || `Request failed with ${response.status}`);
    }
    return data;
  }

  async function submitAuth(event) {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      if (authMode === 'register') {
        await request('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(authForm)
        });
        setMessage('Registered. You can log in now.');
        setAuthMode('login');
      } else {
        const data = await request('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: authForm.email, password: authForm.password })
        });
        localStorage.setItem('paymentToken', data.accessToken);
        setToken(data.accessToken);
        setMessage('Logged in.');
      }
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadPayments() {
    setLoading(true);
    try {
      const query = statusFilter ? `?status=${statusFilter}&sort=createdAt,desc` : '?sort=createdAt,desc';
      const data = await request(`/api/payments${query}`, { headers });
      setPayments(data.content || []);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function createPayment(event) {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      await request('/api/payments', {
        method: 'POST',
        headers: { ...headers, 'Idempotency-Key': paymentForm.idempotencyKey },
        body: JSON.stringify({
          amount: Number(paymentForm.amount),
          currency: paymentForm.currency,
          description: paymentForm.description,
          paymentMethod: paymentForm.paymentMethod,
          simulationToken: paymentForm.simulationToken
        })
      });
      setMessage('Payment created.');
      setPaymentForm((form) => ({ ...form, idempotencyKey: crypto.randomUUID() }));
      await loadPayments();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function processPayment(id) {
    setLoading(true);
    setMessage('');
    try {
      await request(`/api/payments/${id}/process`, { method: 'POST', headers });
      setMessage('Payment processing started. Refresh in a moment to see final status.');
      await loadPayments();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function refundPayment(event) {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      await request(`/api/payments/${refundForm.paymentId}/refund`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ amount: Number(refundForm.amount), reason: refundForm.reason })
      });
      setMessage('Refund created.');
      await loadPayments();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    localStorage.removeItem('paymentToken');
    setToken('');
    setPayments([]);
    setMessage('Logged out.');
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <h1>Payment Simulator</h1>
          <p>Simple React client for the Spring Boot API</p>
        </div>
        {token && <button className="secondary" onClick={logout}>Logout</button>}
      </header>

      {message && <div className="notice">{message}</div>}

      {!token ? (
        <section className="panel auth-panel">
          <div className="tabs">
            <button className={authMode === 'login' ? 'active' : ''} onClick={() => setAuthMode('login')}>Login</button>
            <button className={authMode === 'register' ? 'active' : ''} onClick={() => setAuthMode('register')}>Register</button>
          </div>
          <form onSubmit={submitAuth} className="form-grid">
            {authMode === 'register' && (
              <label>Name<input value={authForm.name} onChange={(e) => setAuthForm({ ...authForm, name: e.target.value })} /></label>
            )}
            <label>Email<input type="email" value={authForm.email} onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })} /></label>
            <label>Password<input type="password" value={authForm.password} onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })} /></label>
            <button disabled={loading}>{authMode === 'login' ? 'Login' : 'Register'}</button>
          </form>
        </section>
      ) : (
        <div className="layout">
          <section className="panel">
            <h2>Create Payment</h2>
            <form onSubmit={createPayment} className="form-grid">
              <label>Amount<input value={paymentForm.amount} onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })} /></label>
              <label>Currency
                <select value={paymentForm.currency} onChange={(e) => setPaymentForm({ ...paymentForm, currency: e.target.value })}>
                  <option>INR</option><option>USD</option><option>EUR</option>
                </select>
              </label>
              <label>Method
                <select value={paymentForm.paymentMethod} onChange={(e) => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}>
                  <option>CARD</option><option>UPI</option><option>NET_BANKING</option><option>WALLET</option>
                </select>
              </label>
              <label>Simulation Token<input value={paymentForm.simulationToken} onChange={(e) => setPaymentForm({ ...paymentForm, simulationToken: e.target.value })} /></label>
              <label>Description<input value={paymentForm.description} onChange={(e) => setPaymentForm({ ...paymentForm, description: e.target.value })} /></label>
              <label>Idempotency Key<input value={paymentForm.idempotencyKey} onChange={(e) => setPaymentForm({ ...paymentForm, idempotencyKey: e.target.value })} /></label>
              <button disabled={loading}>Create Payment</button>
            </form>
          </section>

          <section className="panel">
            <h2>Refund</h2>
            <form onSubmit={refundPayment} className="form-grid">
              <label>Payment ID<input value={refundForm.paymentId} onChange={(e) => setRefundForm({ ...refundForm, paymentId: e.target.value })} /></label>
              <label>Amount<input value={refundForm.amount} onChange={(e) => setRefundForm({ ...refundForm, amount: e.target.value })} /></label>
              <label>Reason<input value={refundForm.reason} onChange={(e) => setRefundForm({ ...refundForm, reason: e.target.value })} /></label>
              <button disabled={loading}>Create Refund</button>
            </form>
          </section>

          <section className="panel payments-panel">
            <div className="section-header">
              <h2>Payments</h2>
              <div className="actions">
                <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                  <option value="">All statuses</option>
                  <option>PENDING</option><option>PROCESSING</option><option>SUCCEEDED</option><option>FAILED</option>
                  <option>CANCELLED</option><option>REFUNDED</option><option>PARTIALLY_REFUNDED</option>
                </select>
                <button className="secondary" onClick={loadPayments} disabled={loading}>Refresh</button>
              </div>
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>ID</th><th>Reference</th><th>Amount</th><th>Status</th><th>Method</th><th></th></tr>
                </thead>
                <tbody>
                  {payments.map((payment) => (
                    <tr key={payment.paymentId}>
                      <td className="mono">{payment.paymentId}</td>
                      <td>{payment.paymentReference}</td>
                      <td>{payment.amount} {payment.currency}</td>
                      <td><span className={`status ${payment.status.toLowerCase()}`}>{payment.status}</span></td>
                      <td>{payment.paymentMethod}</td>
                      <td>
                        <button className="secondary" disabled={payment.status !== 'PENDING' || loading} onClick={() => processPayment(payment.paymentId)}>
                          Process
                        </button>
                      </td>
                    </tr>
                  ))}
                  {payments.length === 0 && <tr><td colSpan="6" className="empty">No payments found.</td></tr>}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}
    </main>
  );
}

createRoot(document.getElementById('root')).render(<App />);
