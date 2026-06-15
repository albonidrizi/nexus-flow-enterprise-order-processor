import axios from 'axios';
import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/useAuth';
import { OrderStatus, Role } from '../types/auth';
import type { Order, Page, Product } from '../types/auth';

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

const statusOptions: Array<OrderStatus | 'ALL'> = [
  'ALL',
  OrderStatus.VALIDATED,
  OrderStatus.PAID,
  OrderStatus.PROCESSING,
  OrderStatus.SHIPPED,
  OrderStatus.DELIVERED,
  OrderStatus.CANCELLED,
  OrderStatus.FAILED,
];

const cancellableStatuses: OrderStatus[] = [OrderStatus.CREATED, OrderStatus.VALIDATED, OrderStatus.PAID];

const errorMessage = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message;
  }
  return 'Unexpected error';
};

const DashboardPage = () => {
  const { user, logout } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [cart, setCart] = useState<Record<number, number>>({});
  const [statusFilter, setStatusFilter] = useState<OrderStatus | 'ALL'>('ALL');
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const orderParams = new URLSearchParams({ size: '20', sort: 'createdAt,desc' });
      if (statusFilter !== 'ALL') {
        orderParams.set('status', statusFilter);
      }
      const [ordersResponse, productsResponse] = await Promise.all([
        api.get<Page<Order>>(`/orders?${orderParams.toString()}`),
        api.get<Product[]>('/products'),
      ]);
      setOrders(ordersResponse.data.content);
      setProducts(productsResponse.data);
      setSelectedOrderId((current) => current ?? ordersResponse.data.content[0]?.id ?? null);
    } catch (error) {
      setMessage(`Load failed: ${errorMessage(error)}`);
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchData();
    const interval = window.setInterval(fetchData, 8000);
    return () => window.clearInterval(interval);
  }, [fetchData]);

  const selectedOrder = useMemo(
    () => orders.find((order) => order.id === selectedOrderId) ?? orders[0],
    [orders, selectedOrderId],
  );

  const cartItems = Object.entries(cart)
    .filter(([, quantity]) => quantity > 0)
    .map(([productId, quantity]) => ({ productId: Number(productId), quantity }));

  const cartTotal = cartItems.reduce((total, item) => {
    const product = products.find((candidate) => candidate.id === item.productId);
    return total + (product?.price ?? 0) * item.quantity;
  }, 0);

  const handleQuantityChange = (productId: number, quantity: number) => {
    setCart((current) => ({ ...current, [productId]: Math.max(0, quantity) }));
  };

  const handleCreateOrder = async () => {
    if (cartItems.length === 0) {
      setMessage('Add at least one product before creating an order.');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await api.post<Order>(
        '/orders',
        { items: cartItems },
        { headers: { 'Idempotency-Key': crypto.randomUUID() } },
      );
      setMessage(`Order #${response.data.id} validated and reserved.`);
      setCart({});
      await fetchData();
      setSelectedOrderId(response.data.id);
    } catch (error) {
      setMessage(`Order failed: ${errorMessage(error)}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePayment = async (order: Order) => {
    setIsSubmitting(true);
    try {
      await api.post(
        `/orders/${order.id}/payments`,
        { approved: true },
        { headers: { 'Idempotency-Key': crypto.randomUUID() } },
      );
      setMessage(`Payment captured for order #${order.id}.`);
      await fetchData();
    } catch (error) {
      setMessage(`Payment failed: ${errorMessage(error)}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = async (order: Order) => {
    setIsSubmitting(true);
    try {
      await api.patch(`/orders/${order.id}/status`, {
        status: OrderStatus.CANCELLED,
        note: 'Cancelled from customer dashboard',
      });
      setMessage(`Order #${order.id} cancelled.`);
      await fetchData();
    } catch (error) {
      setMessage(`Cancel failed: ${errorMessage(error)}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const canAccessOperations = user?.role === Role.ROLE_ADMIN || user?.role === Role.ROLE_MANAGER;

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">NexusFlow</p>
          <h1>Order dashboard</h1>
          <p className="muted">Signed in as {user?.username} ({user?.role.replace('ROLE_', '')})</p>
        </div>
        <div className="topbar-actions">
          {canAccessOperations && <a className="button secondary" href="/admin">Operations</a>}
          <button className="button ghost" onClick={logout}>Logout</button>
        </div>
      </header>

      {message && <div className="notice">{message}</div>}

      <section className="dashboard-grid">
        <div className="panel">
          <div className="section-heading">
            <h2>Create order</h2>
            <span>{currency.format(cartTotal)}</span>
          </div>
          {isLoading ? (
            <div className="empty-state">Loading products...</div>
          ) : (
            <div className="product-grid">
              {products.map((product) => (
                <article className="product-card" key={product.id}>
                  <div>
                    <h3>{product.name}</h3>
                    <p>{currency.format(product.price)}</p>
                    <span className={product.availableQuantity > 0 ? 'stock' : 'stock danger'}>
                      {product.availableQuantity} available
                    </span>
                  </div>
                  <input
                    aria-label={`Quantity for ${product.name}`}
                    min="0"
                    max={product.availableQuantity}
                    type="number"
                    value={cart[product.id] ?? 0}
                    onChange={(event) => handleQuantityChange(product.id, Number(event.target.value))}
                  />
                </article>
              ))}
            </div>
          )}
          <button className="button primary" disabled={isSubmitting} onClick={handleCreateOrder}>
            {isSubmitting ? 'Working...' : 'Create validated order'}
          </button>
        </div>

        <div className="panel">
          <div className="section-heading">
            <h2>Orders</h2>
            <select
              className="compact-select"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as OrderStatus | 'ALL')}
            >
              {statusOptions.map((status) => (
                <option value={status} key={status}>{status}</option>
              ))}
            </select>
          </div>
          <div className="order-list">
            {orders.length === 0 ? (
              <div className="empty-state">No orders match this view.</div>
            ) : (
              orders.map((order) => (
                <button
                  className={order.id === selectedOrder?.id ? 'order-row selected' : 'order-row'}
                  key={order.id}
                  onClick={() => setSelectedOrderId(order.id)}
                >
                  <span>#{order.id}</span>
                  <strong>{currency.format(order.totalAmount)}</strong>
                  <span className={`status ${order.status.toLowerCase()}`}>{order.status}</span>
                </button>
              ))
            )}
          </div>
        </div>
      </section>

      {selectedOrder && (
        <section className="panel detail-panel">
          <div className="section-heading">
            <div>
              <h2>Order #{selectedOrder.id}</h2>
              <p className="muted">Correlation: {selectedOrder.correlationId ?? 'n/a'}</p>
            </div>
            <span className={`status ${selectedOrder.status.toLowerCase()}`}>{selectedOrder.status}</span>
          </div>

          <div className="detail-grid">
            <div>
              <h3>Items</h3>
              <div className="line-items">
                {selectedOrder.items.map((item) => (
                  <div className="line-item" key={`${selectedOrder.id}-${item.productId}`}>
                    <span>{item.productName} x {item.quantity}</span>
                    <strong>{currency.format(item.lineTotal)}</strong>
                  </div>
                ))}
              </div>
              <div className="action-row">
                {selectedOrder.status === OrderStatus.VALIDATED && (
                  <button className="button primary" disabled={isSubmitting} onClick={() => handlePayment(selectedOrder)}>
                    Capture payment
                  </button>
                )}
                {cancellableStatuses.includes(selectedOrder.status) && (
                  <button className="button secondary" disabled={isSubmitting} onClick={() => handleCancel(selectedOrder)}>
                    Cancel order
                  </button>
                )}
              </div>
            </div>
            <div>
              <h3>History</h3>
              <ol className="timeline">
                {selectedOrder.history.map((event, index) => (
                  <li key={`${event.createdAt}-${index}`}>
                    <strong>{event.toStatus}</strong>
                    <span>{event.note || event.eventType}</span>
                    <small>{event.actor}</small>
                  </li>
                ))}
              </ol>
            </div>
          </div>
        </section>
      )}
    </main>
  );
};

export default DashboardPage;
