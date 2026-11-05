import React, { useState, useEffect } from 'react';
import './AttendanceDashboard.css';

const AttendanceDashboard = () => {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [storeId, setStoreId] = useState(null);
  const [stores, setStores] = useState([]);

  useEffect(() => {
    fetchStores();
    fetchDashboard();
  }, [storeId]);

  const fetchStores = async () => {
    try {
      const response = await fetch('/api/stores?active=true', {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setStores(data.content || []);
      }
    } catch (err) {
      console.error('Error fetching stores:', err);
    }
  };

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (storeId) {
        params.append('storeId', storeId);
      }
      const response = await fetch(`/api/admin/attendance/dashboard?${params}`, {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setDashboard(data);
      }
    } catch (err) {
      console.error('Error fetching dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  const StatCard = ({ title, value, color, icon }) => (
    <div className={`stat-card stat-card-${color}`}>
      <div className="stat-icon">{icon}</div>
      <div className="stat-content">
        <div className="stat-title">{title}</div>
        <div className="stat-value">{value}</div>
      </div>
    </div>
  );

  return (
    <div className="attendance-dashboard">
      <div className="header">
        <h1>Bảng điều khiển chấm công</h1>
        <div className="store-filter">
          <label>Cửa hàng:</label>
          <select value={storeId || ''} onChange={(e) => setStoreId(e.target.value || null)}>
            <option value="">-- Tất cả cửa hàng --</option>
            {stores.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="loading">Đang tải dữ liệu...</div>
      ) : dashboard ? (
        <div className="stats-grid">
          <StatCard title="Tổng nhân viên" value={dashboard.total_employees} color="blue" icon="👥" />
          <StatCard title="Đã check-in" value={dashboard.checked_in_today} color="green" icon="✓" />
          <StatCard title="Chưa check-in" value={dashboard.not_checked_in} color="red" icon="✗" />
          <StatCard title="Đi trễ" value={dashboard.late_arrivals} color="orange" icon="⏰" />
          <StatCard title="Về sớm" value={dashboard.early_departures} color="purple" icon="⏱" />
          <StatCard title="Đã check-out" value={dashboard.checked_out} color="blue" icon="🏁" />
          <StatCard title="Thiếu check-out" value={dashboard.missing_check_out} color="red" icon="⚠" />
          <StatCard title="Đang nghỉ" value={dashboard.on_leave_today} color="gray" icon="🏖" />
          <StatCard title="Chờ duyệt" value={dashboard.pending_approvals} color="yellow" icon="📋" />
        </div>
      ) : null}

      <div className="action-buttons">
        <a href="/admin/attendance/records" className="btn btn-primary">
          Xem bảng công
        </a>
        <a href="/admin/attendance/excuses" className="btn btn-primary">
          Quản lý đơn giải trình
        </a>
        <a href="/admin/attendance/leave" className="btn btn-primary">
          Quản lý đơn xin nghỉ
        </a>
        <a href="/admin/attendance/export" className="btn btn-success">
          Xuất báo cáo
        </a>
      </div>
    </div>
  );
};

export default AttendanceDashboard;
