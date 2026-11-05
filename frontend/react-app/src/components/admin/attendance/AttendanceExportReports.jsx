import React, { useState } from 'react';
import './AttendanceExportReports.css';

const AttendanceExportReports = () => {
  const [exportType, setExportType] = useState('attendance');
  const [fromDate, setFromDate] = useState(() => {
    const date = new Date();
    date.setDate(date.getDate() - 30);
    return date.toISOString().split('T')[0];
  });
  const [toDate, setToDate] = useState(new Date().toISOString().split('T')[0]);
  const [storeId, setStoreId] = useState('');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [stores, setStores] = useState([]);

  React.useEffect(() => {
    fetchStores();
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

  const handleExport = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        fromDate,
        toDate
      });

      if (storeId) params.append('storeId', storeId);
      if (status && exportType === 'attendance') params.append('status', status);

      const endpoint = exportType === 'attendance'
        ? `/api/admin/attendance/export/records?${params}`
        : `/api/admin/attendance/export/leave-report?${params}`;

      const response = await fetch(endpoint, {
        credentials: 'include'
      });

      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${exportType}_${fromDate}_${toDate}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      } else {
        alert('Lỗi khi xuất file');
      }
    } catch (err) {
      alert('Lỗi: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="export-reports">
      <h2>Xuất báo cáo</h2>

      <div className="export-tabs">
        <button
          className={`tab ${exportType === 'attendance' ? 'active' : ''}`}
          onClick={() => setExportType('attendance')}
        >
          Bảng công
        </button>
        <button
          className={`tab ${exportType === 'leave' ? 'active' : ''}`}
          onClick={() => setExportType('leave')}
        >
          Báo cáo nghỉ phép
        </button>
      </div>

      <div className="export-form">
        <div className="form-group">
          <label>Từ ngày:</label>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label>Đến ngày:</label>
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
          />
        </div>

        {exportType === 'attendance' && (
          <>
            <div className="form-group">
              <label>Cửa hàng:</label>
              <select value={storeId} onChange={(e) => setStoreId(e.target.value)}>
                <option value="">-- Tất cả cửa hàng --</option>
                {stores.map((store) => (
                  <option key={store.id} value={store.id}>
                    {store.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Trạng thái:</label>
              <select value={status} onChange={(e) => setStatus(e.target.value)}>
                <option value="">-- Tất cả --</option>
                <option value="PRESENT">Có mặt</option>
                <option value="ABSENT">Vắng mặt</option>
                <option value="LATE">Đi trễ</option>
              </select>
            </div>
          </>
        )}

        <button
          onClick={handleExport}
          disabled={loading}
          className="btn-export"
        >
          {loading ? 'Đang xuất...' : 'Xuất Excel'}
        </button>
      </div>

      <div className="export-info">
        <h3>Thông tin xuất</h3>
        <ul>
          <li>📊 Dữ liệu được xuất sang định dạng Excel (.xlsx)</li>
          <li>📅 Bạn có thể chọn khoảng thời gian bất kỳ</li>
          <li>🏪 Lọc theo cửa hàng để xuất dữ liệu cụ thể</li>
          <li>✅ Định dạng dữ liệu sẵn sàng để sử dụng với các công cụ khác</li>
        </ul>
      </div>
    </div>
  );
};

export default AttendanceExportReports;
