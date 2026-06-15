import axios from 'axios';
import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/useAuth';
import { OrderStatus, Role } from '../types/auth';
import type { Order, Page, Product } from '../types/auth';

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

const errorMessage = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message || error.message;
  }
  return 'Unexpected error';
};

const nextOperationalStatuses = (status: OrderStatus) => {
  if (status === OrderStatus.PAID) {
    return [OrderStatus.PROCESSING, OrderStatus.CANCELLED];
  }
  if (status === OrderStatus.PROCESSING) {
    return [OrderStatus.SHIPPED];
  }
  if (status === OrderStatus.SHIPPED) {
    return [OrderStatus.DELIVERED];
  }
  if (status === OrderStatus.CREATED || status === OrderStatus.VALIDATED) {
    return [OrderStatus.CANCELLED];
  }
  return [];
};

const terminalStatuses: OrderStatus[] = [OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.FAILED];

const AdminInventoryPage = () => {
  const { user, logout } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [newProduct, setNewProduct] = useState({ name: '', price: 0, quantity: 10 });
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isAdmin = user?.role === Role.ROLE_ADMIN;

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [inventoryResponse, ordersResponse] = await Promise.all([
        api.get<Product[]>('/admin/inventory'),
        api.get<Page<Order>>('/orders?size=25&sort=createdAt,desc'),
      ]);
      setProducts(inventoryResponse.data);
      setOrders(ordersResponse.data.content);
    } catch (error) {
      setMessage(`Load failed: ${errorMessage(error)}`);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
    const interval = window.setInterval(fetchData, 8000);
    return () => window.clearInterval(interval);
  }, [fetchData]);

  const reservedUnits = useMemo(
    () => products.reduce((total, product) => total + product.reservedQuantity, 0),
    [products],
  );

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await api.post('/admin/products', newProduct);
      setNewProduct({ name: '', price: 0, quantity: 10 });
      setMessage('Product added to inventory.');
      await fetchData();
    } catch (error) {
      setMessage(`Create failed: ${errorMessage(error)}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const updateOrderStatus = async (order: Order, status: OrderStatus) => {
    setIsSubmitting(true);
    try {
      await api.patch(`/orders/${order.id}/status`, {
        status,
        note: `Updated by ${user?.username}`,
      });
      setMessage(`Order #${order.id} moved to ${status}.`);
      await fetchData();
    } catch (error) {
      setMessage(`Status update failed: ${errorMessage(error)}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Operations</p>
          <h1>Inventory and order flow</h1>
          <p className="muted">Signed in as {user?.username} ({user?.role.replace('ROLE_', '')})</p>
        </div>
        <div className="topbar-actions">
          <a className="button secondary" href="/dashboard">Dashboard</a>
          <button className="button ghost" onClick={logout}>Logout</button>
        </div>
      </header>

      {message && <div className="notice">{message}</div>}

      <section className="metric-strip">
        <div>
          <span>Total SKUs</span>
          <strong>{products.length}</strong>
        </div>
        <div>
          <span>Reserved units</span>
          <strong>{reservedUnits}</strong>
        </div>
        <div>
          <span>Open orders</span>
          <strong>{orders.filter((order) => !terminalStatuses.includes(order.status)).length}</strong>
        </div>
      </section>

      <section className="dashboard-grid">
        <div className="panel">
          <div className="section-heading">
            <h2>Inventory</h2>
            <span>{isLoading ? 'Loading' : `${products.length} products`}</span>
          </div>

          {isAdmin && (
            <form className="inline-form" onSubmit={handleCreate}>
              <input
                value={newProduct.name}
                onChange={(event) => setNewProduct({ ...newProduct, name: event.target.value })}
                placeholder="Product name"
                required
              />
              <input
                min="0.01"
                step="0.01"
                type="number"
                value={newProduct.price}
                onChange={(event) => setNewProduct({ ...newProduct, price: Number(event.target.value) })}
                required
              />
              <input
                min="0"
                type="number"
                value={newProduct.quantity}
                onChange={(event) => setNewProduct({ ...newProduct, quantity: Number(event.target.value) })}
                required
              />
              <button className="button primary" disabled={isSubmitting} type="submit">Add</button>
            </form>
          )}

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Price</th>
                  <th>On hand</th>
                  <th>Reserved</th>
                  <th>Available</th>
                  <th>Version</th>
                </tr>
              </thead>
              <tbody>
                {products.map((product) => (
                  <tr key={product.id}>
                    <td>{product.name}</td>
                    <td>{currency.format(product.price)}</td>
                    <td>{product.quantity}</td>
                    <td>{product.reservedQuantity}</td>
                    <td>{product.availableQuantity}</td>
                    <td>{product.version}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="panel">
          <div className="section-heading">
            <h2>Order operations</h2>
            <span>{orders.length} recent</span>
          </div>
          <div className="ops-list">
            {orders.map((order) => (
              <article className="ops-order" key={order.id}>
                <div>
                  <strong>#{order.id} {order.customer}</strong>
                  <span>{currency.format(order.totalAmount)}</span>
                </div>
                <span className={`status ${order.status.toLowerCase()}`}>{order.status}</span>
                <div className="action-row">
                  {nextOperationalStatuses(order.status).map((status) => (
                    <button
                      className={status === OrderStatus.CANCELLED ? 'button secondary' : 'button primary'}
                      disabled={isSubmitting}
                      key={`${order.id}-${status}`}
                      onClick={() => updateOrderStatus(order, status)}
                    >
                      {status}
                    </button>
                  ))}
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
};

export default AdminInventoryPage;
