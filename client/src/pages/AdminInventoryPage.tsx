import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

interface Product {
    id: number;
    name: string;
    price: number;
    quantity: number;
    version: number;
}

const AdminInventoryPage = () => {
    const { logout } = useAuth();
    const [products, setProducts] = useState<Product[]>([]);
    const [newProduct, setNewProduct] = useState({ name: '', price: 0, quantity: 10 });

    const fetchInventory = () => {
        api.get<Product[]>('/admin/inventory')
            .then(res => setProducts(res.data))
            .catch(console.error);
    };

    useEffect(() => {
        fetchInventory();
    }, []);

    const handleCreate = async (e: FormEvent) => {
        e.preventDefault();
        try {
            await api.post('/admin/products', newProduct);
            fetchInventory();
            setNewProduct({ name: '', price: 0, quantity: 10 });
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <div>
            <header style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem' }}>
                <h1>Admin Inventory</h1>
                <button onClick={logout} style={{ width: 'auto' }}>Logout</button>
            </header>

            <section style={{ marginBottom: '2rem' }}>
                <h3>Add Product</h3>
                <form onSubmit={handleCreate} style={{ display: 'flex', gap: '1rem', alignItems: 'end' }}>
                    <div>
                        <label>Name</label>
                        <input value={newProduct.name} onChange={e => setNewProduct({ ...newProduct, name: e.target.value })} required />
                    </div>
                    <div>
                        <label>Price</label>
                        <input type="number" value={newProduct.price} onChange={e => setNewProduct({ ...newProduct, price: Number(e.target.value) })} required />
                    </div>
                    <div>
                        <label>Quantity</label>
                        <input type="number" value={newProduct.quantity} onChange={e => setNewProduct({ ...newProduct, quantity: Number(e.target.value) })} required />
                    </div>
                    <button type="submit" style={{ marginBottom: '0.5rem' }}>Add</button>
                </form>
            </section>

            <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '1rem' }}>
                <thead>
                    <tr style={{ background: '#444' }}>
                        <th style={{ padding: '0.5rem' }}>ID</th>
                        <th style={{ padding: '0.5rem' }}>Name</th>
                        <th style={{ padding: '0.5rem' }}>Price</th>
                        <th style={{ padding: '0.5rem' }}>Stock</th>
                        <th style={{ padding: '0.5rem' }}>Version</th>
                    </tr>
                </thead>
                <tbody>
                    {products.map(p => (
                        <tr key={p.id} style={{ borderBottom: '1px solid #444' }}>
                            <td style={{ padding: '0.5rem' }}>{p.id}</td>
                            <td style={{ padding: '0.5rem' }}>{p.name}</td>
                            <td style={{ padding: '0.5rem' }}>${p.price}</td>
                            <td style={{ padding: '0.5rem' }}>{p.quantity}</td>
                            <td style={{ padding: '0.5rem' }}>{p.version}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default AdminInventoryPage;
