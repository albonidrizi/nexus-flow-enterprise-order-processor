import { useEffect, useState } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

interface Order {
    id: number;
    totalAmount: number;
    status: string;
    createdAt: string;
    items: { product: { name: string }; quantity: number }[];
}

interface Product {
    id: number;
    name: string;
    price: number;
    quantity: number;
}

const DashboardPage = () => {
    const { user, logout } = useAuth();
    const [orders, setOrders] = useState<Order[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [cart, setCart] = useState<{ [key: number]: number }>({}); // productId -> quantity
    const [message, setMessage] = useState('');

    const fetchData = () => {
        api.get<Order[]>('/orders').then(res => setOrders(res.data)).catch(console.error);
        api.get<Product[]>('/products').then(res => setProducts(res.data)).catch(console.error);
    };

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 5000); // Poll every 5s for status updates
        return () => clearInterval(interval);
    }, []);

    const handleQuantityChange = (productId: number, qty: number) => {
        if (qty < 0) return;
        setCart(prev => ({ ...prev, [productId]: qty }));
    };

    const handleOrder = async () => {
        const items = Object.entries(cart)
            .filter(([_, qty]) => qty > 0)
            .map(([productId, qty]) => ({ productId: Number(productId), quantity: qty }));

        if (items.length === 0) {
            setMessage('Cart is empty');
            return;
        }

        try {
            await api.post('/orders', { items });
            setMessage('Order placed successfully!');
            setCart({});
            fetchData();
        } catch (error: any) {
            setMessage('Order failed: ' + (error.response?.data?.error || error.response?.data?.message || 'Unknown error'));
        }
    };

    return (
        <div>
            <header style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem' }}>
                <h1>Welcome, {user?.username}</h1>
                <button onClick={logout} style={{ width: 'auto' }}>Logout</button>
            </header>

            <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
                <section style={{ flex: 1, minWidth: '300px' }}>
                    <h2>Create Order</h2>
                    {message && <p style={{ color: message.includes('failed') ? 'red' : 'green' }}>{message}</p>}
                    <div className="product-list" style={{ display: 'grid', gap: '1rem' }}>
                        {products.map(p => (
                            <div key={p.id} style={{ padding: '1rem', border: '1px solid #444', borderRadius: '4px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div>
                                    <strong>{p.name}</strong> - ${p.price} <br />
                                    <small>Available: {p.quantity}</small>
                                </div>
                                <div>
                                    <input
                                        type="number"
                                        min="0"
                                        max={p.quantity}
                                        value={cart[p.id] || 0}
                                        onChange={(e) => handleQuantityChange(p.id, Number(e.target.value))}
                                        style={{ width: '60px' }}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                    <button onClick={handleOrder} style={{ marginTop: '1rem' }}>Place Order</button>
                </section>

                <section style={{ flex: 1, minWidth: '300px' }}>
                    <h2>My Orders</h2>
                    {orders.length === 0 ? <p>No orders found.</p> : (
                        <div style={{ display: 'grid', gap: '1rem' }}>
                            {orders.map(order => (
                                <div key={order.id} style={{ background: '#333', padding: '1rem', borderRadius: '8px' }}>
                                    <h3 style={{ marginTop: 0 }}>Order #{order.id}</h3>
                                    <p>Status: <strong style={{ color: order.status === 'CONFIRMED' ? '#4caf50' : '#ff9800' }}>{order.status}</strong></p>
                                    <p>Total: ${order.totalAmount}</p>
                                    <ul>
                                        {order.items?.map((item, idx) => (
                                            <li key={idx}>{item.product?.name || 'Item'} x {item.quantity}</li>
                                        ))}
                                    </ul>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
};

export default DashboardPage;
