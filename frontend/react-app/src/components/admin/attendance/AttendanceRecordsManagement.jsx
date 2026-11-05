import React, { useState, useEffect } from 'react';
import './AttendanceRecordsManagement.css';

const AttendanceRecordsManagement = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fromDate, setFromDate] = useState(() => {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date.toISOString().split('T')[0];
  });
  const [toDate, setToDate] = useState(new Date().toISOString().split('T')[0]);
  const [storeId, setStoreId] = useState(null);
  const [status, setStatus] = useState('');
  const [stores, setStores] = useState([]);
  const [selectedRecord, setSelectedRecord] = useState(null);
  const [showAdjustForm, setShowAdjustForm] = useState(false);
  const [adjustData, setAdjustData] = useState({
    new_check_in: '',
    new_check_out: '',
    reason: ''
  });

  useEffect(() => {
    fetchStores();
    fetchRecords();
  }, []);

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

  const fetchRecords = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        fromDate,
        toDate,
        page: 0,
        size: 50
      });
      if (storeId) params.append('storeId', storeId);
      if (status) params.append('status', status);

      const response = await fetch(`/api/admin/attendance/records?${params}`, {
        credentials: 'include'
      });
      if (response.ok) {
        const data = await response.json();
        setRecords(data.records || []);
      }
    } catch (err) {
      console.error('Error fetching records:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAdjust = async (recordId) => {
    setSelectedRecord(recordId);
    setShowAdjustForm(true);
  };

  const submitAdjustment = async () => {
    try {
      const response = await fetch(`/api/admin/attendance/records/${selectedRecord}/adjust`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(adjustData)
      });

      if (response.ok) {
        alert('Điều chỉnh thành công');
        setShowAdjustForm(false);
        fetchRecords();
      } else {
        const error = await response.json();
        alert('Lỗi: ' + error.message);
      }
    } catch (err) {
      alert('Lỗi khi điều chỉnh: ' + err.message);
    }
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '-';
    return new Date(dateTimeStr).toLocaleString('vi-VN');
  };

  return (
    <div className="attendance-records">
      <h2>Bảng công nhân viên</h2>

      <div className="filter-section">
        <div className="filter-group">
          <label>Từ ngày:</label>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Đến ngày:</label>
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Cửa hàng:</label>
          <select value={storeId || ''} onChange={(e) => setStoreId(e.target.value || null)}>
            <option value="">-- Tất cả --</option>
            {stores.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name}
              </option>
            ))}
          </select>
        </div>
        <div className="filter-group">
          <label>Trạng thái:</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">-- Tất cả --</option>
            <option value="PRESENT">Có mặt</option>
            <option value="ABSENT">Vắng mặt</option>
            <option value="LATE">Đi trễ</option>
          </select>
        </div>
        <button onClick={fetchRecords} className="btn-search" disabled={loading}>
          {loading ? 'Đang tìm...' : 'Tìm kiếm'}
        </button>
      </div>

      <div className="records-table">
        {records.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>Nhân viên</th>
                <th>Ngày</th>
                <th>Check-in</th>
                <th>Check-out</th>
                <th>Thời gian</th>
                <th>Trạng thái</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  <td>{record.employee_name}</td>
                  <td>{record.attendance_date}</td>
                  <td className="time">{formatDateTime(record.check_in_time)}</td>
                  <td className="time">{formatDateTime(record.check_out_time)}</td>
                  <td>{record.total_minutes ? `${Math.floor(record.total_minutes / 60)}h ${record.total_minutes % 60}m` : '-'}</td>
                  <td>
                    <span className={`status status-${record.status.toLowerCase()}`}>
                      {record.status}
                    </span>
                  </td>
                  <td>
                    <button
                      onClick={() => handleAdjust(record.id)}
                      className="btn-small btn-adjust"
                    >
                      Điều chỉnh
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="no-data">Không có dữ liệu</p>
        )}
      </div>

      {showAdjustForm && (
        <div className="modal-overlay">
          <div className="modal">
            <h3>Điều chỉnh công</h3>
            <div className="form-group">
              <label>Check-in mới (nếu cần):</label>
              <input
                type="datetime-local"
                value={adjustData.new_check_in}
                onChange={(e) => setAdjustData({ ...adjustData, new_check_in: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Check-out mới (nếu cần):</label>
              <input
                type="datetime-local"
                value={adjustData.new_check_out}
                onChange={(e) => setAdjustData({ ...adjustData, new_check_out: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Lý do:</label>
              <textarea
                value={adjustData.reason}
                onChange={(e) => setAdjustData({ ...adjustData, reason: e.target.value })}
                rows="4"
              />
            </div>
            <div className="modal-buttons">
              <button onClick={submitAdjustment} className="btn-primary">
                Lưu
              </button>
              <button onClick={() => setShowAdjustForm(false)} className="btn-cancel">
                Hủy
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AttendanceRecordsManagement;
